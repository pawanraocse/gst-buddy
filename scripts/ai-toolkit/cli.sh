#!/bin/bash
# ═══════════════════════════════════════════════════════════════
# AI Toolkit CLI — Context Window Optimization "Skill + CLI"
# ═══════════════════════════════════════════════════════════════
# This script is designed to be called by AI coding agents (Cursor, Cline)
# to execute complex or high-token-cost tasks locally, returning only
# the summarized data to the LLM context window.

set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Import sub-commands (we'll implement these next)
source "$DIR/query-db.sh"
source "$DIR/filter-logs.sh"

function show_help() {
    cat << EOF
AI Toolkit CLI (v1.0.0)
-----------------------
A utility for AI coding agents to perform local operations without
bloating the context window.

USAGE:
  ./cli.sh <command> [options]

COMMANDS:
  query-db      Query the local PostgreSQL database and return JSON
  filter-logs   Extract only stack traces and ERROR-level logs from a file
  help          Show this message

EXAMPLES:
  ./cli.sh query-db "SELECT * FROM users LIMIT 5"
  ./cli.sh filter-logs /path/to/spring.log

AI AGENT INSTRUCTIONS:
- For multi-step tasks or massive outputs (>2000 lines), DO NOT call these commands directly.
- Instead, write a local Node.js/Python script that calls this CLI, parses the JSON/text output, and prints ONLY the final summarized answer to stdout.
EOF
}

# Main routing
case "$1" in
    query-db)
        shift
        cmd_query_db "$@"
        ;;
    filter-logs)
        shift
        cmd_filter_logs "$@"
        ;;
    help|--help|-h|"")
        show_help
        ;;
    *)
        echo "Unknown command: $1"
        echo "Run './cli.sh help' for usage."
        exit 1
        ;;
esac
