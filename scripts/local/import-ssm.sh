#!/bin/bash
# import-ssm.sh
# Fetches Cognito parameters from AWS SSM and generates terraform/cognito-config.env

set -euo pipefail

# Find project root (2 levels up from scripts/local)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../../" && pwd)"

# Load .env from project root if it exists
if [ -f "$ROOT_DIR/.env" ]; then
    echo "Loading environment variables from .env"
    set -a
    source "$ROOT_DIR/.env"
    set +a
fi

PROFILE="${AWS_PROFILE:-personal}"
REGION="${AWS_REGION:-us-east-1}"

# Match how deploy.sh resolves the project name and environment
PROJECT="${PROJECT_NAME:-gstbuddies}"
TF_ENV="${ENVIRONMENT:-dev}"

PARAM_PATH="/${PROJECT}/${TF_ENV}/cognito"

echo "Fetching SSM parameters from path: ${PARAM_PATH}"
echo "Using AWS Profile: ${PROFILE}, Region: ${REGION}"

cd "$ROOT_DIR"
mkdir -p terraform

# Fetch parameters and format them as ENV vars
aws ssm get-parameters-by-path \
  --path "${PARAM_PATH}" \
  --with-decryption \
  --profile "${PROFILE}" \
  --region "${REGION}" \
  --query "Parameters[*].[Name,Value]" \
  --output text | awk -v path="${PARAM_PATH}/" -F'\t' '{
    if ($1 == "") next;
    # Remove the path prefix from the parameter name
    name = substr($1, length(path) + 1);
    if (name == "") next;
    # Convert to uppercase
    name = toupper(name);
    # Replace any remaining slashes or dashes with underscores (just in case)
    gsub(/[-\/]/, "_", name);
    # Print the export format
    print "export COGNITO_" name "=\"" $2 "\""
  }' > terraform/cognito-config.env

if [ -s terraform/cognito-config.env ]; then
    echo "Successfully generated terraform/cognito-config.env!"
    
    # Load the newly generated vars to update frontend
    source terraform/cognito-config.env
    
    # Update Frontend Environment Files
    FRONTEND_DIR="$ROOT_DIR/frontend"
    if [ -d "$FRONTEND_DIR/src/environments" ]; then
        echo "Updating frontend environment files..."
        
        # Extract the base domain from the auth domain string
        # e.g. from gstbuddies-dev-tgwoncvq turns into auth domain format
        AUTH_DOMAIN="${COGNITO_DOMAIN}.auth.${REGION}.amazoncognito.com"

        # Update development environment
        cat > "$FRONTEND_DIR/src/environments/environment.development.ts" <<EOF
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080', // Gateway URL
  cognito: {
    userPoolId: '${COGNITO_USER_POOL_ID}',
    userPoolWebClientId: '${COGNITO_SPA_CLIENT_ID}',
    region: '${REGION}',
    domain: '${AUTH_DOMAIN}'
  }
};
EOF

        # Update production environment
        cat > "$FRONTEND_DIR/src/environments/environment.ts" <<EOF
export const environment = {
  production: true,
  apiUrl: 'http://localhost:8080', // Gateway URL (update for production)
  cognito: {
    userPoolId: '${COGNITO_USER_POOL_ID}',
    userPoolWebClientId: '${COGNITO_SPA_CLIENT_ID}',
    region: '${REGION}',
    domain: '${AUTH_DOMAIN}'
  }
};
EOF
        echo "Frontend environment files updated successfully!"
    fi
else
    echo "Warning: No parameters were fetched. Please verify the path exists in SSM."
    rm -f terraform/cognito-config.env
fi
