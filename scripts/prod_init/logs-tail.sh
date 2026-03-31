#!/bin/bash
# ============================================================================
# Production Log Tailing Tool (prod_init)
# ============================================================================
# usage: ./logs-tail.sh auth-service 100
#        ./logs-tail.sh backend-service grep "ERROR"
# ============================================================================

set -euo pipefail

SERVICE="${1:-}"
LINES="${2:-100}"
GREP="${3:-}"

# Default values
AWS_REGION="ap-south-1"
AWS_PROFILE="personal"
PROJECT_NAME="gstbuddies"
ENVIRONMENT="prod_init"

if [ -z "$SERVICE" ]; then
    echo "❌ Usage: ./logs-tail.sh <service-name> [lines] [grep-pattern]"
    echo "Services: auth-service, backend-service, gateway-service, eureka-server"
    exit 1
fi

# Get the script directory and source config
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/env-config.sh"

# Default values if not in skill or env
AWS_REGION="ap-south-1"
AWS_PROFILE="personal"
PROJECT_NAME="gstbuddies"
ENVIRONMENT="prod_init"

echo "ℹ️  Using environment: $ENVIRONMENT (IP: $EC2_IP)"

# Auto-detect SSH key
SSH_KEY=""
for key in ~/.ssh/id_rsa_personal ~/.ssh/pawankeys ~/.ssh/id_rsa ~/.ssh/id_ed25519; do
    if [ -f "$key" ]; then SSH_KEY="$key"; break; fi
done

if [ -z "$SSH_KEY" ]; then
    echo "❌ No SSH key found in ~/.ssh/"
    exit 1
fi

if [ -n "$GREP" ]; then
    echo "📡 Tailing logs for $SERVICE with grep $GREP..."
    ssh -t -o StrictHostKeyChecking=no -i "$SSH_KEY" ec2-user@"$EC2_IP" "docker logs --tail $LINES -f $SERVICE 2>&1 | grep --line-buffered '$GREP'"
else
    echo "📡 Tailing logs for $SERVICE..."
    ssh -t -o StrictHostKeyChecking=no -i "$SSH_KEY" ec2-user@"$EC2_IP" "docker logs --tail $LINES -f $SERVICE"
fi
