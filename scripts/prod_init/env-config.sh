#!/bin/bash
# ============================================================================
# Production Environment Config Loader (prod_init)
# ============================================================================
# Single source of truth for all prod_init diagnostic scripts.
# Resolution order:
#   1. Live AWS SSM (most accurate — fetches current EC2 IP after redeploys)
#   2. debug/SKILL.md hardcoded values (offline fallback)
#   3. Built-in defaults below
# ============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SKILL_FILE="$SCRIPT_DIR/../../.agents/skills/debug/SKILL.md"

# ── Shared defaults (overridden by SSM or SKILL.md below) ──────────────────
export AWS_REGION="${AWS_REGION:-ap-south-1}"
export AWS_PROFILE="${AWS_PROFILE:-personal}"
export PROJECT_NAME="${PROJECT_NAME:-gstbuddies}"
export ENVIRONMENT="${ENVIRONMENT:-prod_init}"

# Fallback values from SKILL.md (used if SSM is unavailable)
export EC2_IP="65.1.250.35"
export DB_HOST="gstbuddies-prod-init.czs4w640o6ge.ap-south-1.rds.amazonaws.com"
export DB_NAME="gstbuddies_db"
export DB_USER="postgres"

# ── Function: parse a value from the debug SKILL.md ─────────────────────────
_extract_skill_var() {
    local var_name=$1
    grep -E "^# ?${var_name}=" "$SKILL_FILE" 2>/dev/null | head -n 1 | cut -d'"' -f2
}

# ── Try to resolve values from SKILL.md (overrides built-in defaults) ───────
if [ -f "$SKILL_FILE" ]; then
    v=$(_extract_skill_var "EC2_IP");   [ -n "$v" ] && EC2_IP="$v"
    v=$(_extract_skill_var "DB_HOST");  [ -n "$v" ] && DB_HOST="$v"
    v=$(_extract_skill_var "DB_NAME");  [ -n "$v" ] && DB_NAME="$v"
    v=$(_extract_skill_var "DB_USER");  [ -n "$v" ] && DB_USER="$v"
fi

# ── Try to resolve live values from SSM (overrides everything) ───────────────
if command -v aws &>/dev/null && aws sts get-caller-identity --profile "$AWS_PROFILE" &>/dev/null 2>&1; then
    _ssm_get() {
        aws ssm get-parameter \
            --name "/$PROJECT_NAME/$ENVIRONMENT/$1" \
            --query "Parameter.Value" --output text \
            --region "$AWS_REGION" --profile "$AWS_PROFILE" 2>/dev/null || true
    }

    v=$(_ssm_get "ec2/public_ip");  [ -n "$v" ] && [ "$v" != "None" ] && EC2_IP="$v"
    v=$(_ssm_get "rds/endpoint");   [ -n "$v" ] && [ "$v" != "None" ] && DB_HOST="$v"
    echo "ℹ️  Environment loaded from SSM: EC2=$EC2_IP"
else
    echo "ℹ️  AWS SSM unavailable — using SKILL.md fallback: EC2=$EC2_IP"
fi

export EC2_IP DB_HOST DB_NAME DB_USER
