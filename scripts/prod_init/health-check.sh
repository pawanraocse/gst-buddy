#!/bin/bash
# ============================================================================
# Production Health Check Tool (prod_init)
# ============================================================================
# usage: ./health-check.sh
# ============================================================================

set -euo pipefail

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

echo "🔍 Checking Docker services via SSH..."
ssh -o StrictHostKeyChecking=no -i "$SSH_KEY" ec2-user@"$EC2_IP" "docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'"

echo ""
echo "🔍 Checking RDS connectivity from EC2..."
ssh -o StrictHostKeyChecking=no -i "$SSH_KEY" ec2-user@"$EC2_IP" "timeout 2 bash -c '</dev/tcp/$DB_HOST/5432' && echo '✅ Connected' || echo '❌ Connection Failed'"

echo ""
echo "🔍 Checking Service Health Endpoints..."
    for port in 8081 8082 8083; do
        case $port in 
            8081) name="auth-service";; 
            8082) name="backend-service";; 
            8083) name="gateway-service";; 
        esac
        echo -n "  $name ($port): "
    ssh -o StrictHostKeyChecking=no -i "$SSH_KEY" ec2-user@"$EC2_IP" "curl -s -o /dev/null -w '%{http_code}' http://localhost:$port/actuator/health || echo 'FAILED'"
    echo ""
done
