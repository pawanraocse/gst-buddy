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

# Load cognito-config.env from terraform if it exists
if [ -f "$PROJECT_ROOT/terraform/cognito-config.env" ]; then
    set -a
    source "$PROJECT_ROOT/terraform/cognito-config.env"
    set +a
fi

# Configuration
REGION="${AWS_REGION:-us-east-1}"
PROJECT_NAME="${PROJECT_NAME:-gstbuddies}"
ENVIRONMENT="${3:-${ENVIRONMENT:-budget}}"
ENV="${ENV:-local}"
AUTH_SERVICE_URL="${AUTH_SERVICE_URL:-http://localhost:8081}"
INTERNAL_API_KEY="${INTERNAL_API_KEY:-}"

echo "================================================================"
echo "   gstbuddies - System Admin Bootstrap"
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
    read -p "Enter Admin Email [system-admin@gstbuddies.local]: " ADMIN_EMAIL
    ADMIN_EMAIL="${ADMIN_EMAIL:-system-admin@gstbuddies.local}"
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
echo "Step 4/4: Linking Cognito user to database..."

API_LINKED=false

# Both local and remote environments use the /auth prefix 
# because auth-service server.servlet.context-path is /auth
BOOTSTRAP_URL="${AUTH_SERVICE_URL}/auth/api/v1/admin/bootstrap/new-super-admin"

echo "  Trying API: $BOOTSTRAP_URL"
HTTP_CODE=$(curl -s -w "%{http_code}" -X POST \
    "${BOOTSTRAP_URL}" \
    -H "Content-Type: application/json" \
    -H "X-Internal-Api-Key: ${INTERNAL_API_KEY}" \
    -d "{\"cognitoSub\": \"${COGNITO_SUB}\", \"email\": \"${ADMIN_EMAIL}\"}" \
    -o /tmp/bootstrap_response.json 2>/dev/null || echo "000")

if [ "$HTTP_CODE" = "200" ]; then
    echo "  ✅ Database linked via API."
    API_LINKED=true
else
    echo "  ⚠️  API returned HTTP $HTTP_CODE — falling back to direct DB insert."
fi

rm -f /tmp/bootstrap_response.json

# ── 5b. Fallback: Direct DB insert via SSH to EC2 ───────────────
if [ "$API_LINKED" = "false" ] && [ "$ENVIRONMENT" != "local" ]; then
    echo ""
    echo "  Attempting direct DB bootstrap via EC2..."

    # Fetch DB + EC2 connection details from SSM
    EC2_IP=$(aws ssm get-parameter \
        --name "/$PROJECT_NAME/$ENVIRONMENT/ec2/public_ip" \
        --query "Parameter.Value" --output text --region "$REGION" 2>/dev/null || echo "")

    DB_HOST=$(aws ssm get-parameter \
        --name "/$PROJECT_NAME/$ENVIRONMENT/rds/endpoint" \
        --query "Parameter.Value" --output text --region "$REGION" 2>/dev/null || echo "")

    DB_NAME=$(aws ssm get-parameter \
        --name "/$PROJECT_NAME/$ENVIRONMENT/rds/database" \
        --query "Parameter.Value" --output text --region "$REGION" 2>/dev/null || echo "")

    DB_USER=$(aws ssm get-parameter \
        --name "/$PROJECT_NAME/$ENVIRONMENT/rds/username" \
        --query "Parameter.Value" --output text --region "$REGION" 2>/dev/null || echo "")

    SECRET_ARN=$(aws ssm get-parameter \
        --name "/$PROJECT_NAME/$ENVIRONMENT/rds/secret_arn" \
        --query "Parameter.Value" --output text --region "$REGION" 2>/dev/null || echo "")

    DB_PASSWORD=$(aws secretsmanager get-secret-value \
        --secret-id "$SECRET_ARN" --query 'SecretString' --output text \
        --region "$REGION" 2>/dev/null | python3 -c "import sys,json; print(json.load(sys.stdin)['password'])" 2>/dev/null || echo "")

    # Auto-detect SSH key
    SSH_KEY="${SSH_KEY:-}"
    if [ -z "$SSH_KEY" ]; then
        for key in ~/.ssh/id_rsa_personal ~/.ssh/pawankeys ~/.ssh/id_rsa ~/.ssh/id_ed25519; do
            if [ -f "$key" ]; then SSH_KEY="$key"; break; fi
        done
    fi

    if [ -z "$EC2_IP" ] || [ -z "$DB_HOST" ] || [ -z "$DB_PASSWORD" ] || [ -z "$SSH_KEY" ]; then
        echo "  ❌ Cannot fallback: missing EC2_IP, DB details, or SSH key."
        echo "     EC2_IP=$EC2_IP DB_HOST=$DB_HOST SSH_KEY=$SSH_KEY"
        echo "     You may need to manually insert the user and role into the database."
    else
        echo "  EC2: $EC2_IP | DB: $DB_HOST/$DB_NAME"

        ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 -i "$SSH_KEY" ec2-user@"$EC2_IP" \
            "PGPASSWORD='$DB_PASSWORD' psql -h '$DB_HOST' -U '$DB_USER' -d '$DB_NAME' -c \"
                -- Insert user if not exists
                INSERT INTO users (user_id, tenant_id, email, name, status, source, first_login_at, last_login_at, created_at, updated_at)
                VALUES ('$COGNITO_SUB', 'default', '$ADMIN_EMAIL', 'Super Admin', 'ACTIVE', 'COGNITO', NOW(), NOW(), NOW(), NOW())
                ON CONFLICT (user_id) DO UPDATE SET name = 'Super Admin', status = 'ACTIVE', updated_at = NOW();

                -- Assign super-admin role if not exists
                INSERT INTO user_roles (tenant_id, user_id, role_id, assigned_by)
                VALUES ('default', '$COGNITO_SUB', 'super-admin', 'SYSTEM_BOOTSTRAP')
                ON CONFLICT DO NOTHING;

                -- Ensure credit wallet exists
                INSERT INTO user_credit_wallets (user_id, tenant_id, total_credits, consumed_credits, has_used_trial, version, created_at, updated_at)
                VALUES ('$COGNITO_SUB', 'default', 2, 0, true, 0, NOW(), NOW())
                ON CONFLICT DO NOTHING;

                -- Clean up seeded placeholder (no longer needed)
                DELETE FROM user_roles WHERE user_id = 'SYSTEM_ADMIN_PLACEHOLDER';
                DELETE FROM user_credit_wallets WHERE user_id = 'SYSTEM_ADMIN_PLACEHOLDER';
                DELETE FROM users WHERE user_id = 'SYSTEM_ADMIN_PLACEHOLDER';
            \"" 2>/dev/null

        if [ $? -eq 0 ]; then
            echo "  ✅ Database linked via direct DB insert."
            API_LINKED=true
        else
            echo "  ❌ Direct DB insert failed. Check SSH/DB connectivity."
        fi
    fi
fi

echo ""
echo "================================================================"
if [ "$API_LINKED" = "true" ]; then
    echo "✅ System Admin Created Successfully!"
else
    echo "⚠️  System Admin Created (Cognito OK, DB linking needs attention)"
fi
echo "  Email:       $ADMIN_EMAIL"
echo "  Cognito Sub: $COGNITO_SUB"
echo "  Role:        super-admin"
if [ "$ENVIRONMENT" != "local" ]; then
    echo "  Login at:    ${AUTH_SERVICE_URL}"
else
    echo "  Login at:    http://localhost:4200"
fi
echo "================================================================"
