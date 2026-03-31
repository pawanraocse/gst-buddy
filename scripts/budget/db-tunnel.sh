#!/bin/bash
# ============================================================================
# Budget DB Tunnel - SSH tunnel to RDS via EC2 bastion
# ============================================================================
# Opens a local port (5433) forwarding to RDS PostgreSQL.
# Connect pgAdmin/DBeaver/psql to localhost:5433
# ============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/../.."

# Load .env
if [ -f "$PROJECT_ROOT/.env" ]; then
    set -a; source "$PROJECT_ROOT/.env"; set +a
fi

AWS_REGION="${AWS_REGION:-ap-south-1}"
AWS_PROFILE="${AWS_PROFILE:-personal}"
PROJECT_NAME="${PROJECT_NAME:-gstbuddies}"
ENVIRONMENT="budget"
LOCAL_PORT="${1:-5433}"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔗 Budget DB Tunnel"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Fetch connection details from SSM
echo "Fetching connection details from SSM..."
EC2_IP=$(aws ssm get-parameter \
    --name "/$PROJECT_NAME/$ENVIRONMENT/ec2/public_ip" \
    --query "Parameter.Value" --output text --region "$AWS_REGION" --profile "$AWS_PROFILE")

DB_HOST=$(aws ssm get-parameter \
    --name "/$PROJECT_NAME/$ENVIRONMENT/rds/endpoint" \
    --query "Parameter.Value" --output text --region "$AWS_REGION" --profile "$AWS_PROFILE")

DB_NAME=$(aws ssm get-parameter \
    --name "/$PROJECT_NAME/$ENVIRONMENT/rds/database" \
    --query "Parameter.Value" --output text --region "$AWS_REGION" --profile "$AWS_PROFILE")

DB_USER=$(aws ssm get-parameter \
    --name "/$PROJECT_NAME/$ENVIRONMENT/rds/username" \
    --query "Parameter.Value" --output text --region "$AWS_REGION" --profile "$AWS_PROFILE")

SECRET_ARN=$(aws ssm get-parameter \
    --name "/$PROJECT_NAME/$ENVIRONMENT/rds/secret_arn" \
    --query "Parameter.Value" --output text --region "$AWS_REGION" --profile "$AWS_PROFILE")

DB_PASSWORD=$(aws secretsmanager get-secret-value \
    --secret-id "$SECRET_ARN" --query 'SecretString' --output text \
    --region "$AWS_REGION" --profile "$AWS_PROFILE" | python3 -c "import sys,json; print(json.load(sys.stdin)['password'])")

# Auto-detect SSH key
SSH_KEY="${SSH_KEY:-}"
if [ -z "$SSH_KEY" ]; then
    for key in ~/.ssh/id_rsa_personal ~/.ssh/pawankeys ~/.ssh/id_rsa ~/.ssh/id_ed25519; do
        if [ -f "$key" ]; then SSH_KEY="$key"; break; fi
    done
fi

if [ -z "$SSH_KEY" ]; then
    echo "❌ No SSH key found. Set SSH_KEY env var."
    exit 1
fi

echo ""
echo "📋 Connection Details (for pgAdmin/DBeaver):"
echo "  ┌─────────────────────────────────────────────────"
echo "  │ Host:     localhost"
echo "  │ Port:     $LOCAL_PORT"
echo "  │ Database: $DB_NAME"
echo "  │ Username: $DB_USER"
echo "  │ Password: $DB_PASSWORD"
echo "  └─────────────────────────────────────────────────"
echo ""
echo "  Via: ec2-user@$EC2_IP → $DB_HOST:5432"
echo ""
echo "🔌 Tunnel is OPEN on localhost:$LOCAL_PORT"
echo "   Press Ctrl+C to close."
echo ""

ssh -o StrictHostKeyChecking=no -o ServerAliveInterval=60 \
    -i "$SSH_KEY" \
    -L "$LOCAL_PORT:$DB_HOST:5432" \
    -N ec2-user@"$EC2_IP"
