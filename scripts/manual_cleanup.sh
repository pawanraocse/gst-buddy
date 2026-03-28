#!/bin/bash
# ============================================================================
# Manual Cleanup Script for gst-buddy-dev
# ============================================================================
# usage: ./manual_cleanup.sh [aws-profile]

set -e

PROJECT="gst-buddy"
ENV="dev"
REGION="us-east-1"
PROFILE="${1:-default}"

echo "Using AWS Profile: $PROFILE"
echo "Region: $REGION"

# Helper function
delete_resource() {
    local cmd="$1"
    local id="$2"
    local type="$3"
    
    echo "Deleting $type: $id..."
    if $cmd --profile "$PROFILE" --region "$REGION" >/dev/null 2>&1; then
        echo "✅ Deleted $type: $id"
    else
        echo "⚠️  Failed to delete $type: $id (or it may not exist)"
    fi
}

echo "======================================================================"
echo "Starting Cleanup for $PROJECT-$ENV"
echo "======================================================================"

# 1. Cognito User Pool
echo "🔍 Finding User Pool..."
POOL_NAME="${PROJECT}-${ENV}-user-pool"
POOL_ID=$(aws cognito-idp list-user-pools --max-results 20 --profile "$PROFILE" --region "$REGION" --query "UserPools[?Name=='$POOL_NAME'].Id" --output text)

if [ "$POOL_ID" == "None" ] || [ -z "$POOL_ID" ]; then
    echo "⚠️  User Pool '$POOL_NAME' not found."
else
    echo "Found User Pool ID: $POOL_ID"
    
    # Check for Domain
    echo "🔍 Checking for User Pool Domain..."
    DOMAIN_DESC=$(aws cognito-idp describe-user-pool-domain --domain "${PROJECT}-${ENV}" --profile "$PROFILE" --region "$REGION" 2>/dev/null || true)
    
    # We construct the domain prefix to try deleting it just in case
    # The terraform uses randomized suffix, so we rely on the pool deletion 
    # identifying if a domain is attached.
    # Actually, describe-user-pool returns the domain info.
    # We'll rely on 'delete-user-pool-domain' if we can find the full domain name, 
    # but the domain is usually "prefix-{random}".
    
    # However, 'delete-user-pool' often handles managed domains. 
    # Let's try to find the custom domain prefix if it exists.
    
    # Delete User Pool
    echo "🗑️  Deleting User Pool '$POOL_NAME' ($POOL_ID)..."
    # This deletes clients, groups, and the pool itself.
    delete_resource "aws cognito-idp delete-user-pool --user-pool-id $POOL_ID" "$POOL_ID" "User Pool"
fi

# 2. Lambda Functions
echo "----------------------------------------------------------------------"
echo "🔍 Cleaning up Lambda Functions..."

LAMBDAS=(
    "${PROJECT}-${ENV}-post-confirmation"
    "${PROJECT}-${ENV}-pre-token-generation"
)

for func in "${LAMBDAS[@]}"; do
    delete_resource "aws lambda delete-function --function-name $func" "$func" "Lambda Function"
done

# 3. SSM Parameters
echo "----------------------------------------------------------------------"
echo "🔍 Cleaning up SSM Parameters..."

PARAM_PATH="/${PROJECT}/${ENV}"
PARAMS=$(aws ssm get-parameters-by-path --path "$PARAM_PATH" --recursive --query "Parameters[*].Name" --output text --profile "$PROFILE" --region "$REGION" || true)

if [ -z "$PARAMS" ]; then
    echo "⚠️  No SSM parameters found under $PARAM_PATH"
else
    for param in $PARAMS; do
        delete_resource "aws ssm delete-parameter --name $param" "$param" "SSM Parameter"
    done
fi

echo "======================================================================"
echo "✅ Cleanup sequence completed!"
echo "Please verify in AWS Console if any resources remain."
