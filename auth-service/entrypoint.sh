#!/bin/sh
set -e

# Source global .env if present (project root)
set -a
[ -f /app/../.env ] && . /app/../.env
set +a

# AWS Region
export AWS_REGION=${AWS_REGION:-us-east-1}

# Fix for empty AWS_PROFILE causing CLI errors
if [ -z "$AWS_PROFILE" ]; then
  unset AWS_PROFILE
fi

export HOME=/home/spring

# SSM Parameter Path Prefix
PROJECT_NAME=${PROJECT_NAME:-gstbuddies}
SSM_PREFIX="/${PROJECT_NAME}/dev/cognito"

echo "[INFO] Starting Auth Service Entrypoint"
echo "[INFO] Region: $AWS_REGION"
echo "[INFO] SSM Prefix: $SSM_PREFIX"

echo "[INFO] HOME is: $HOME"
ls -la $HOME/.aws || echo "Could not ls .aws"
cat $HOME/.aws/credentials || echo "Could not read credentials"

# Verify AWS Identity (useful for troubleshooting but less verbose)
IDENTITY=$(aws sts get-caller-identity --query "Arn" --output text 2>/dev/null || echo "Unknown/None")
echo "[INFO] AWS Identity: $IDENTITY"

# Function to fetch SSM parameter with error handling
fetch_ssm_param() {
  local param_name=$1
  local param_path="$SSM_PREFIX/$param_name"
  local decrypt_flag=$2

  local result
  if [ "$decrypt_flag" = "decrypt" ]; then
    result=$(aws ssm get-parameter \
      --name "$param_path" \
      --region "$AWS_REGION" \
      --with-decryption \
      --query "Parameter.Value" \
      --output text 2>&1)
  else
    result=$(aws ssm get-parameter \
      --name "$param_path" \
      --region "$AWS_REGION" \
      --query "Parameter.Value" \
      --output text 2>&1)
  fi

  if [ $? -ne 0 ]; then
    echo "[ERROR] Failed to fetch SSM parameter: $param_path" >&2
    echo "[ERROR] $result" >&2
    return 1
  fi

  echo "$result"
}

# Fetch SSM parameters
echo "[INFO] Fetching SSM parameters..."
export COGNITO_USER_POOL_ID=$(fetch_ssm_param "user_pool_id") || echo "Failed"
export COGNITO_CLIENT_ID=$(fetch_ssm_param "client_id") || echo "Failed"
export COGNITO_CLIENT_SECRET=$(fetch_ssm_param "client_secret" "decrypt") || echo "Failed"
export COGNITO_ISSUER_URI=$(fetch_ssm_param "issuer_uri") || echo "Failed"
export COGNITO_JWKS_URI=$(fetch_ssm_param "jwks_uri") || echo "Failed"
export COGNITO_DOMAIN=$(fetch_ssm_param "domain") || echo "Failed"
export COGNITO_REDIRECT_URI=$(fetch_ssm_param "callback_url") || echo "Failed"
export COGNITO_LOGOUT_REDIRECT_URL=$(fetch_ssm_param "logout_redirect_url") || echo "Failed"

# Validate required parameters
if [ -z "$COGNITO_USER_POOL_ID" ] || [ -z "$COGNITO_CLIENT_ID" ] || [ "$COGNITO_USER_POOL_ID" = "Failed" ]; then
  echo "[ERROR] Missing required Cognito configuration."
  sleep 60
  exit 1
fi

echo "[INFO] ✓ Cognito Configuration Loaded (Pool ID: $COGNITO_USER_POOL_ID)"
echo "[INFO] Starting Spring Boot application..."
exec java -jar /app/app.jar
