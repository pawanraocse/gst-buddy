#!/bin/bash
# ============================================================================
# Sync Local Environment from S3 Remote State
# ============================================================================
# Fetches outputs from the existing remote Terraform state and updates your
# local environment.ts and cognito-config.env files without running apply.
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="$SCRIPT_DIR/../../terraform"
cd "$TERRAFORM_DIR"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 1. Init to connect to S3 Backend
log_info "Initializing Terraform to connect to remote S3 State..."
terraform init -upgrade > /dev/null

# 2. Extract outputs
log_info "Fetching Terraform outputs..."
USER_POOL_ID=$(terraform output -raw user_pool_id 2>/dev/null || echo "")
CLIENT_ID=$(terraform output -raw client_id 2>/dev/null || echo "")
SPA_CLIENT_ID=$(terraform output -raw spa_client_id 2>/dev/null || echo "")
CLIENT_SECRET=$(terraform output -raw client_secret 2>/dev/null || echo "")
ISSUER_URI=$(terraform output -raw issuer_uri 2>/dev/null || echo "")
JWKS_URI=$(terraform output -raw jwks_uri 2>/dev/null || echo "")
CALLBACK_URL=$(terraform output -raw callback_url 2>/dev/null || echo "")
LOGOUT_REDIRECT_URL=$(terraform output -raw logout_redirect_url 2>/dev/null || echo "")
DOMAIN=$(terraform output -raw cognito_domain 2>/dev/null || echo "")
AWS_REGION=$(grep 'aws_region' terraform.tfvars 2>/dev/null | cut -d'"' -f2 || echo "ap-south-1")

if [ -z "$USER_POOL_ID" ]; then
    log_error "No outputs found! Is the infrastructure deployed?"
    exit 1
fi

# 3. Update cognito-config.env
OUTPUT_FILE="cognito-config.env"
cat > "$OUTPUT_FILE" <<EOF
COGNITO_USER_POOL_ID=$USER_POOL_ID
COGNITO_CLIENT_ID=$CLIENT_ID
COGNITO_CLIENT_SECRET=$CLIENT_SECRET
COGNITO_SPA_CLIENT_ID=$SPA_CLIENT_ID
COGNITO_ISSUER_URI=$ISSUER_URI
COGNITO_JWKS_URI=$JWKS_URI
COGNITO_DOMAIN=$DOMAIN
COGNITO_REDIRECT_URI=$CALLBACK_URL
COGNITO_LOGOUT_REDIRECT_URL=$LOGOUT_REDIRECT_URL
AWS_REGION=$AWS_REGION
EOF
log_info "Config saved to $OUTPUT_FILE"

# 4. Update Frontend Environment Files
FRONTEND_DIR="$SCRIPT_DIR/../../frontend"
if [ -d "$FRONTEND_DIR/src/environments" ]; then
    log_info "Updating frontend environment files..."
    
    cat > "$FRONTEND_DIR/src/environments/environment.development.ts" <<EOF
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  cognito: {
    userPoolId: '$USER_POOL_ID',
    userPoolWebClientId: '$SPA_CLIENT_ID',
    region: '$AWS_REGION',
    domain: '$DOMAIN.auth.$AWS_REGION.amazoncognito.com'
  }
};
EOF
    cat > "$FRONTEND_DIR/src/environments/environment.ts" <<EOF
export const environment = {
  production: true,
  apiUrl: 'http://localhost:8080',
  cognito: {
    userPoolId: '$USER_POOL_ID',
    userPoolWebClientId: '$SPA_CLIENT_ID',
    region: '$AWS_REGION',
    domain: '$DOMAIN.auth.$AWS_REGION.amazoncognito.com'
  }
};
EOF
    log_info "Frontend env files updated successfully!"
fi

echo "✅ Environment sync complete! You can now run docker-compose up."
