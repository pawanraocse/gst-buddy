#!/bin/bash
# ============================================================================
# Production Environment Config Loader (prod_init)
# ============================================================================
# This script extracts configuration values from the "debug" skill's SKILL.md.
# It ensures that SKILL.md is the single source of truth for all tools.
# ============================================================================

# Get the script and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SKILL_FILE="$SCRIPT_DIR/../../.agents/skills/debug/SKILL.md"

if [ ! -f "$SKILL_FILE" ]; then
    echo "⚠️ Warning: debug/SKILL.md not found at $SKILL_FILE. Falling back to defaults."
    return 0
fi

# Function to extract a value from the SKILL.md's production profile section
extract_var() {
    local var_name=$1
    # Grep the line, take the value in quotes
    grep -E "^# ?$var_name=" "$SKILL_FILE" | head -n 1 | cut -d'"' -f2
}

# Export the variables found in the skill
FOUND_IP=$(extract_var "EC2_IP")
FOUND_HOST=$(extract_var "DB_HOST")
FOUND_NAME=$(extract_var "DB_NAME")
FOUND_USER=$(extract_var "DB_USER")

if [ -n "$FOUND_IP" ]; then export EC2_IP="$FOUND_IP"; fi
if [ -n "$FOUND_HOST" ]; then export DB_HOST="$FOUND_HOST"; fi
if [ -n "$FOUND_NAME" ]; then export DB_NAME="$FOUND_NAME"; fi
if [ -n "$FOUND_USER" ]; then export DB_USER="$FOUND_USER"; fi

# Notify user of source
if [ -n "$FOUND_IP" ]; then
    echo "ℹ️  Environment loaded from debug skill: $EC2_IP"
else
    echo "⚠️  Warning: Could not parse environment details from SKILL.md"
fi
