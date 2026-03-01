#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/.."

# Load .env from project root
if [ -f "$PROJECT_ROOT/.env" ]; then
    set -a
    source "$PROJECT_ROOT/.env"
    set +a
fi

# Configuration
REGION="${AWS_REGION:-us-east-1}"
PROJECT_NAME="${PROJECT_NAME:-gst-buddy}"
ENVIRONMENT="${ENVIRONMENT:-budget}"
ENV="${ENV:-local}"
AUTH_SERVICE_URL="${AUTH_SERVICE_URL:-http://localhost:8081}"
INTERNAL_API_KEY="${INTERNAL_API_KEY:-}"

echo "================================================================"
echo "   GST Buddy - System Admin Bootstrap"
echo "================================================================"
echo "Creates a Super Admin user in Cognito and links it via API."
echo ""
echo "Environment: $ENVIRONMENT | Region: $REGION"
echo ""

# ── 1. Detect User Pool ID ──────────────────────────────────────
if [ "$ENVIRONMENT" != "local" ]; then
    echo "Fetching configuration from AWS SSM for $ENVIRONMENT environment..."
    USER_POOL_ID=$(aws ssm get-parameter \
        --name "/$PROJECT_NAME/$ENVIRONMENT/cognito/user_pool_id" \
        --query "Parameter.Value" --output text --region "$REGION" 2>/dev/null || echo "")
        
    AUTH_SERVICE_URL=$(aws ssm get-parameter \
        --name "/$PROJECT_NAME/$ENVIRONMENT/api/url" \
        --query "Parameter.Value" --output text --region "$REGION" 2>/dev/null || echo "")
        
    INTERNAL_API_KEY=$(aws ssm get-parameter \
        --name "/$PROJECT_NAME/$ENVIRONMENT/api/internal_key" \
        --with-decryption \
        --query "Parameter.Value" --output text --region "$REGION" 2>/dev/null || echo "")
else
    USER_POOL_ID="${COGNITO_USER_POOL_ID:-}"
fi

if [ -z "$USER_POOL_ID" ]; then
    echo "Could not find User Pool ID in SSM or env."
    read -p "  Enter User Pool ID manually: " USER_POOL_ID
    if [ -z "$USER_POOL_ID" ]; then echo "Exiting."; exit 1; fi
fi
echo "User Pool: $USER_POOL_ID"
echo "Auth URL:  $AUTH_SERVICE_URL"
echo ""

# ── 2. Collect Credentials ──────────────────────────────────────
ADMIN_EMAIL="${1:-}"
ADMIN_PASSWORD="${2:-}"

if [ -z "$ADMIN_EMAIL" ]; then
    read -p "Enter Admin Email [system-admin@gst-buddy.local]: " ADMIN_EMAIL
    ADMIN_EMAIL="${ADMIN_EMAIL:-system-admin@gst-buddy.local}"
fi

if [ -z "$ADMIN_PASSWORD" ]; then
    read -s -p "Enter Admin Password (min 12 chars, upper+lower+number+symbol): " ADMIN_PASSWORD
    echo ""
    if [ -z "$ADMIN_PASSWORD" ]; then echo "Password cannot be empty. Exiting."; exit 1; fi
fi

# ── 3. Create or Update Cognito User ────────────────────────────
echo ""
echo "Step 1/4: Creating Cognito user..."
if aws cognito-idp admin-get-user \
    --user-pool-id "$USER_POOL_ID" \
    --username "$ADMIN_EMAIL" \
    --region "$REGION" > /dev/null 2>&1; then
    echo "  User already exists in Cognito, updating..."
else
    aws cognito-idp admin-create-user \
        --user-pool-id "$USER_POOL_ID" \
        --username "$ADMIN_EMAIL" \
        --user-attributes Name=email,Value="$ADMIN_EMAIL" Name=email_verified,Value=true \
        --message-action SUPPRESS \
        --region "$REGION" > /dev/null
    echo "  User created."
fi

echo "Step 2/4: Setting permanent password..."
aws cognito-idp admin-set-user-password \
    --user-pool-id "$USER_POOL_ID" \
    --username "$ADMIN_EMAIL" \
    --password "$ADMIN_PASSWORD" \
    --permanent \
    --region "$REGION"

echo "Step 3/4: Assigning super-admin attributes..."
aws cognito-idp admin-update-user-attributes \
    --user-pool-id "$USER_POOL_ID" \
    --username "$ADMIN_EMAIL" \
    --user-attributes \
        Name="custom:role",Value="super-admin" \
        Name="custom:tenantId",Value="default" \
        Name="email_verified",Value="true" \
    --region "$REGION"

aws cognito-idp admin-add-user-to-group \
    --user-pool-id "$USER_POOL_ID" \
    --username "$ADMIN_EMAIL" \
    --group-name "admin" \
    --region "$REGION" 2>/dev/null || true

# ── 4. Get Cognito Sub ──────────────────────────────────────────
COGNITO_SUB=$(aws cognito-idp admin-get-user \
    --user-pool-id "$USER_POOL_ID" \
    --username "$ADMIN_EMAIL" \
    --region "$REGION" \
    --query "UserAttributes[?Name=='sub'].Value" \
    --output text)

if [ -z "$COGNITO_SUB" ]; then
    echo "ERROR: Could not retrieve Cognito sub for $ADMIN_EMAIL"
    exit 1
fi
echo "  Cognito Sub: $COGNITO_SUB"

# ── 5. Link to Database via Bootstrap Endpoint ──────────────────
echo "Step 4/4: Linking Cognito user to database via API..."

HTTP_CODE=$(curl -s -w "%{http_code}" -X POST \
    "${AUTH_SERVICE_URL}/auth/api/v1/admin/bootstrap/new-super-admin" \
    -H "Content-Type: application/json" \
    -H "X-Internal-Api-Key: ${INTERNAL_API_KEY}" \
    -d "{\"cognitoSub\": \"${COGNITO_SUB}\", \"email\": \"${ADMIN_EMAIL}\"}" \
    -o /tmp/bootstrap_response.json)

if [ "$HTTP_CODE" = "200" ]; then
    echo "  Database linked successfully."
elif [ "$HTTP_CODE" = "000" ]; then
    echo "  WARNING: Auth service not reachable at $AUTH_SERVICE_URL"
    echo "  Ensure your backend is running."
else
    BODY=$(cat /tmp/bootstrap_response.json 2>/dev/null || echo "no response body")
    echo "  ERROR: Bootstrap returned HTTP $HTTP_CODE"
    echo "  Response: $BODY"
fi

rm -f /tmp/bootstrap_response.json

echo ""
echo "================================================================"
echo "System Admin Created Successfully!"
echo "  Email:       $ADMIN_EMAIL"
echo "  Cognito Sub: $COGNITO_SUB"
echo "  Role:        super-admin"
if [ "$ENVIRONMENT" != "local" ]; then
    echo "  Login at:    ${AUTH_SERVICE_URL}"
else
    echo "  Login at:    http://localhost:4200"
fi
echo "================================================================"
