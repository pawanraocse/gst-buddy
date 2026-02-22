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
ENVIRONMENT="${ENVIRONMENT:-dev}"
ENV="${ENV:-local}"
AUTH_SERVICE_URL="${AUTH_SERVICE_URL:-http://localhost:8081}"
INTERNAL_API_KEY="${INTERNAL_API_KEY:-}"

SEEDED_EMAIL="system-admin@gst-buddy.local"
POSTGRES_CONTAINER="gst-buddy-postgres-${ENV}"
DB_NAME="${POSTGRES_DB_NAME:-gstbuddy}"
DB_USER="${POSTGRES_USER:-postgres}"

echo "================================================================"
echo "   GST Buddy - System Admin Bootstrap"
echo "================================================================"
echo "Creates a Super Admin user in Cognito and links it to the"
echo "seeded system-admin row in the database."
echo ""
echo "Environment: $ENVIRONMENT | Region: $REGION"
echo ""

# ── 1. Detect User Pool ID ──────────────────────────────────────
USER_POOL_ID=$(aws ssm get-parameter \
    --name "/$PROJECT_NAME/$ENVIRONMENT/cognito/user_pool_id" \
    --query "Parameter.Value" --output text --region "$REGION" 2>/dev/null || echo "")

if [ -z "$USER_POOL_ID" ]; then
    echo "Could not find User Pool ID in SSM."
    echo "  Expected: /$PROJECT_NAME/$ENVIRONMENT/cognito/user_pool_id"
    read -p "  Enter User Pool ID manually: " USER_POOL_ID
    if [ -z "$USER_POOL_ID" ]; then echo "Exiting."; exit 1; fi
fi
echo "User Pool: $USER_POOL_ID"
echo ""

# ── 2. Collect Credentials ──────────────────────────────────────
ADMIN_EMAIL="${1:-}"
ADMIN_PASSWORD="${2:-}"

if [ -z "$ADMIN_EMAIL" ]; then
    read -p "Enter Admin Email [system-admin@gst-buddy.local]: " ADMIN_EMAIL
    ADMIN_EMAIL="${ADMIN_EMAIL:-system-admin@gst-buddy.local}"
fi

if [ -z "$ADMIN_PASSWORD" ]; then
    read -s -p "Enter Admin Password (min 8 chars, upper+lower+number+symbol): " ADMIN_PASSWORD
    echo ""
    if [ -z "$ADMIN_PASSWORD" ]; then echo "Password cannot be empty. Exiting."; exit 1; fi
fi

# ── 3. Create or Update Cognito User ────────────────────────────
echo ""
echo "Step 1/5: Creating Cognito user..."
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

echo "Step 2/5: Setting permanent password..."
aws cognito-idp admin-set-user-password \
    --user-pool-id "$USER_POOL_ID" \
    --username "$ADMIN_EMAIL" \
    --password "$ADMIN_PASSWORD" \
    --permanent \
    --region "$REGION"

echo "Step 3/5: Assigning super-admin attributes..."
aws cognito-idp admin-update-user-attributes \
    --user-pool-id "$USER_POOL_ID" \
    --username "$ADMIN_EMAIL" \
    --user-attributes \
        Name="custom:role",Value="super-admin" \
        Name="custom:tenantId",Value="default" \
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

# ── 5. Update DB Placeholder Email ──────────────────────────────
echo "Step 4/5: Preparing database placeholder..."

if docker ps --format '{{.Names}}' | grep -q "^${POSTGRES_CONTAINER}$"; then
    PLACEHOLDER_EXISTS=$(docker exec "$POSTGRES_CONTAINER" \
        psql -U "$DB_USER" -d "$DB_NAME" -tAc \
        "SELECT COUNT(*) FROM users WHERE user_id = 'SYSTEM_ADMIN_PLACEHOLDER';" 2>/dev/null || echo "0")

    if [ "$PLACEHOLDER_EXISTS" = "1" ] && [ "$ADMIN_EMAIL" != "$SEEDED_EMAIL" ]; then
        docker exec "$POSTGRES_CONTAINER" \
            psql -U "$DB_USER" -d "$DB_NAME" -c \
            "UPDATE users SET email = '${ADMIN_EMAIL}' WHERE user_id = 'SYSTEM_ADMIN_PLACEHOLDER';" > /dev/null
        echo "  Updated placeholder email: $SEEDED_EMAIL -> $ADMIN_EMAIL"
    elif [ "$PLACEHOLDER_EXISTS" = "1" ]; then
        echo "  Placeholder email already matches."
    else
        ALREADY_LINKED=$(docker exec "$POSTGRES_CONTAINER" \
            psql -U "$DB_USER" -d "$DB_NAME" -tAc \
            "SELECT COUNT(*) FROM users WHERE user_id = '${COGNITO_SUB}';" 2>/dev/null || echo "0")
        if [ "$ALREADY_LINKED" = "1" ]; then
            echo "  Admin already linked (user_id = Cognito sub). Skipping bootstrap."
            echo ""
            echo "================================================================"
            echo "System Admin Already Configured!"
            echo "  Email:       $ADMIN_EMAIL"
            echo "  Cognito Sub: $COGNITO_SUB"
            echo "  Login at:    http://localhost:4200"
            echo "================================================================"
            exit 0
        else
            echo "  WARNING: No placeholder row found and user not yet linked."
            echo "  The bootstrap endpoint may fail. User will be linked on first login."
        fi
    fi
else
    echo "  WARNING: Postgres container '$POSTGRES_CONTAINER' not running."
    echo "  Skipping DB email update — bootstrap endpoint will attempt direct match."
fi

# ── 6. Link to Database via Bootstrap Endpoint ──────────────────
echo "Step 5/5: Linking Cognito user to database..."

HTTP_CODE=$(curl -s -o /tmp/bootstrap_response.json -w "%{http_code}" -X POST \
    "${AUTH_SERVICE_URL}/auth/api/v1/admin/bootstrap" \
    -H "Content-Type: application/json" \
    -H "X-Internal-Api-Key: ${INTERNAL_API_KEY}" \
    -d "{\"cognitoSub\": \"${COGNITO_SUB}\", \"email\": \"${ADMIN_EMAIL}\"}")

if [ "$HTTP_CODE" = "200" ]; then
    echo "  Database linked successfully."
elif [ "$HTTP_CODE" = "000" ]; then
    echo "  WARNING: Auth service not reachable at $AUTH_SERVICE_URL"
    echo "  The user will be linked on first login via upsertOnLogin."
else
    BODY=$(cat /tmp/bootstrap_response.json 2>/dev/null || echo "no response body")
    echo "  WARNING: Bootstrap returned HTTP $HTTP_CODE"
    echo "  Response: $BODY"
    echo "  The user will be linked on first login via upsertOnLogin."
fi

rm -f /tmp/bootstrap_response.json

echo ""
echo "================================================================"
echo "System Admin Created Successfully!"
echo "  Email:       $ADMIN_EMAIL"
echo "  Cognito Sub: $COGNITO_SUB"
echo "  Role:        super-admin"
echo "  Login at:    http://localhost:4200"
echo "================================================================"
