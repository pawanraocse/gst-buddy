#!/bin/bash
# ============================================================================
# Agent State Synchronization Utility
# ============================================================================
# Syncs Git-ignored state (.memory, .agents, .env) across machines via S3.
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../../" && pwd)"
TERRAFORM_DIR="$ROOT_DIR/terraform"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 1. Auto-detect S3 bucket from terraform/main.tf
BUCKET=$(grep "bucket" "$TERRAFORM_DIR/main.tf" | head -n 1 | cut -d'"' -f2 || echo "")

if [ -z "$BUCKET" ]; then
    log_error "Could not detect S3 bucket from $TERRAFORM_DIR/main.tf"
    exit 1
fi

S3_PATH="s3://$BUCKET/agent-state"

usage() {
    echo "Usage: $0 [push|pull|status|cleanup] [--dryrun]"
    echo ""
    echo "Commands:"
    echo "  push      Upload local state to S3"
    echo "  pull      Download state from S3 to local"
    echo "  status    Show local vs remote diffs"
    echo "  cleanup   Delete state from S3"
    exit 1
}

if [ $# -lt 1 ]; then usage; fi

COMMAND=$1
DRY_RUN=""
if [[ "${2:-}" == "--dryrun" ]]; then
    DRY_RUN="--dryrun"
    log_info "Running in DRY RUN mode"
fi

# Define filter arguments for aws s3 sync
# Note: We exclude everything first, then include specific paths
SYNC_FILTERS=(
    --exclude "*"
    --include ".memory/"
    --include ".memory/**"
    --include ".agents/"
    --include ".agents/**"
    --include ".agent-skills"
    --include ".env"
    --include ".env.local"
    --include ".env.*.local"
    --include "terraform/cognito-config.env"
)

case "$COMMAND" in
    push)
        log_info "Pushing local state to $S3_PATH..."
        aws s3 sync "$ROOT_DIR" "$S3_PATH" "${SYNC_FILTERS[@]}" $DRY_RUN
        log_info "✅ Push complete."
        ;;
    
    pull)
        log_info "Pulling state from $S3_PATH to local..."
        aws s3 sync "$S3_PATH" "$ROOT_DIR" $DRY_RUN
        log_info "✅ Pull complete."
        ;;
    
    status)
        log_info "Checking state in $S3_PATH..."
        aws s3 ls "$S3_PATH" --recursive
        ;;
    
    cleanup)
        read -p "Are you sure you want to delete all agent state from $S3_PATH? (y/N) " confirm
        if [[ $confirm == [yY] || $confirm == [yY][eE][sS] ]]; then
            log_info "Cleaning up $S3_PATH..."
            aws s3 rm "$S3_PATH" --recursive $DRY_RUN
            log_info "✅ Cleanup complete."
        else
            log_info "Cleanup cancelled."
        fi
        ;;
    
    *)
        usage
        ;;
esac
