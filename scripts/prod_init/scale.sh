#!/bin/bash
# =============================================================================
# Production Scaling & Cost Management Tool
# =============================================================================
# Allows resizing the EC2 instance (vertical scaling) and stopping/starting
# to save costs during maintenance.
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/../.."
TERRAFORM_DIR="$PROJECT_ROOT/terraform/envs/prod_init"

# Load environment variables
if [ -f "$PROJECT_ROOT/.env" ]; then
    set -a
    source "$PROJECT_ROOT/.env"
    set +a
fi

AWS_REGION="${AWS_REGION:-ap-south-1}"
AWS_PROFILE="${AWS_PROFILE:-personal}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Branch Guard
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "prod" ]; then
    log_error "This script MUST be run from the 'prod' branch."
    exit 1
fi

usage() {
    echo "Usage: $0 [up|down|stop|start|status]"
    echo ""
    echo "Commands:"
    echo "  up      Scale up to t3.large (8GB RAM)"
    echo "  down    Scale down to t3.medium (4GB RAM)"
    echo "  stop    Stop the production instance (save money)"
    echo "  start   Start the production instance"
    echo "  status  Check current instance status and type"
    exit 1
}

if [ $# -lt 1 ]; then
    usage
fi

COMMAND=$1

# Get Instance ID from Terraform
cd "$TERRAFORM_DIR"
INSTANCE_ID=$(terraform output -raw ec2_instance_id 2>/dev/null || echo "")

if [ -z "$INSTANCE_ID" ]; then
    log_error "Could not find EC2 instance ID. Has the infrastructure been deployed?"
    exit 1
fi

get_status() {
    aws ec2 describe-instances \
        --instance-ids "$INSTANCE_ID" \
        --profile "$AWS_PROFILE" \
        --region "$AWS_REGION" \
        --query 'Reservations[0].Instances[0].{Status:State.Name,Type:InstanceType}' \
        --output table
}

wait_for_state() {
    local target_state=$1
    log_info "Waiting for instance to reach '$target_state' state..."
    aws ec2 wait "instance-$target_state" \
        --instance-ids "$INSTANCE_ID" \
        --profile "$AWS_PROFILE" \
        --region "$AWS_REGION"
    log_success "Instance is now $target_state"
}

case $COMMAND in
    status)
        get_status
        ;;
    stop)
        log_info "Stopping production instance: $INSTANCE_ID"
        aws ec2 stop-instances --instance-ids "$INSTANCE_ID" --profile "$AWS_PROFILE" --region "$AWS_REGION"
        wait_for_state "stopped"
        ;;
    start)
        log_info "Starting production instance: $INSTANCE_ID"
        aws ec2 start-instances --instance-ids "$INSTANCE_ID" --profile "$AWS_PROFILE" --region "$AWS_REGION"
        wait_for_state "running"
        ;;
    up|down)
        TARGET_TYPE="t3.medium"
        [ "$COMMAND" == "up" ] && TARGET_TYPE="t3.large"
        
        log_info "Resizing production instance to: $TARGET_TYPE"
        
        # 1. Stop instance
        log_info "Step 1: Stopping instance..."
        aws ec2 stop-instances --instance-ids "$INSTANCE_ID" --profile "$AWS_PROFILE" --region "$AWS_REGION"
        wait_for_state "stopped"
        
        # 2. Modify type
        log_info "Step 2: Modifying instance type to $TARGET_TYPE..."
        aws ec2 modify-instance-attribute \
            --instance-id "$INSTANCE_ID" \
            --instance-type "{\"Value\": \"$TARGET_TYPE\"}" \
            --profile "$AWS_PROFILE" \
            --region "$AWS_REGION"
            
        # 3. Start instance
        log_info "Step 3: Starting instance..."
        aws ec2 start-instances --instance-ids "$INSTANCE_ID" --profile "$AWS_PROFILE" --region "$AWS_REGION"
        wait_for_state "running"
        
        log_success "Scaling successful! Instance is now $TARGET_TYPE"
        get_status
        ;;
    *)
        usage
        ;;
esac
