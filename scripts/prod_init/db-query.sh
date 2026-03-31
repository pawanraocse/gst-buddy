#!/bin/bash
# ============================================================================
# Production DB Query Tool (prod_init)
# ============================================================================
# usage: ./db-query.sh "SELECT * FROM users LIMIT 10;"
#        ./db-query.sh  (interactive mode)
# ============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/../.."

# Default values
AWS_REGION="ap-south-1"
AWS_PROFILE="personal"
PROJECT_NAME="gstbuddies"
ENVIRONMENT="prod_init"

SQL_QUERY="${1:-}"

# Get the script directory and source config
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/env-config.sh"

# Default values if not in skill or env
AWS_REGION="ap-south-1"
AWS_PROFILE="personal"
PROJECT_NAME="gstbuddies"
ENVIRONMENT="prod_init"
SECRET_ARN=""
DB_PASSWORD=""

echo "ℹ️  Using environment: $ENVIRONMENT (IP: $EC2_IP)"

# Optional: Attempt to fetch latest from AWS if credentials available
if command -v aws &> /dev/null && aws sts get-caller-identity --profile "$AWS_PROFILE" &> /dev/null; then
    echo "🔍 AWS detected, refreshing credentials from SSM..."
    EC2_IP=$(aws ssm get-parameter --name "/$PROJECT_NAME/$ENVIRONMENT/ec2/public_ip" --query "Parameter.Value" --output text --region "$AWS_REGION" --profile "$AWS_PROFILE")
    DB_HOST=$(aws ssm get-parameter --name "/$PROJECT_NAME/$ENVIRONMENT/rds/endpoint" --query "Parameter.Value" --output text --region "$AWS_REGION" --profile "$AWS_PROFILE")
    SECRET_ARN=$(aws ssm get-parameter --name "/$PROJECT_NAME/$ENVIRONMENT/rds/secret_arn" --query "Parameter.Value" --output text --region "$AWS_REGION" --profile "$AWS_PROFILE")
    DB_PASSWORD=$(aws secretsmanager get-secret-value --secret-id "$SECRET_ARN" --query 'SecretString' --output text --region "$AWS_REGION" --profile "$AWS_PROFILE" | python3 -c "import sys,json; print(json.load(sys.stdin)['password'])")
fi

if [ -z "$DB_PASSWORD" ]; then
    echo -n "🔑 Enter DB Password for $DB_USER: "
    read -s DB_PASSWORD
    echo ""
fi

# Auto-detect SSH key
SSH_KEY=""
for key in ~/.ssh/id_rsa_personal ~/.ssh/pawankeys ~/.ssh/id_rsa ~/.ssh/id_ed25519; do
    if [ -f "$key" ]; then SSH_KEY="$key"; break; fi
done

if [ -z "$SSH_KEY" ]; then
    echo "❌ No SSH key found in ~/.ssh/"
    exit 1
fi

if [ -z "$SQL_QUERY" ]; then
    echo "⌨️  Entering interactive psql via SSH..."
    ssh -t -o StrictHostKeyChecking=no -i "$SSH_KEY" ec2-user@"$EC2_IP" "PGPASSWORD='$DB_PASSWORD' psql -h $DB_HOST -U $DB_USER -d $DB_NAME"
else
    echo "🚀 Executing query..."
    ssh -o StrictHostKeyChecking=no -i "$SSH_KEY" ec2-user@"$EC2_IP" "PGPASSWORD='$DB_PASSWORD' psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c \"$SQL_QUERY\""
fi
