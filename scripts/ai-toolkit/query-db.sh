function cmd_query_db() {
    # Parses DB URI from docker-compose or defaults
    # Uses local `psql` to execute tight JSON query
    
    local query="$1"
    
    if [[ -z "$query" ]]; then
        echo "Error: query-db requires a SQL string."
        echo "Example: ./cli.sh query-db \"SELECT COUNT(*) FROM users;\""
        exit 1
    fi

    # Hardcoded to local docker-compose settings for GSTBuddy
    # To make this dynamic, we could parse the .env file!
    local DB_HOST="localhost"
    local DB_PORT="5432"
    local DB_USER="postgres"
    local DB_PASS="postgres"
    local DB_NAME="gstbuddy"
    
    # Check if psql is installed
    if ! command -v psql &> /dev/null; then
        echo '{"error": "psql command not found. Please install PostgreSQL client tools"}'
        exit 1
    fi

    # Run the query and output JSON (requires postgres 9.2+)
    # By wrapping the user's query in json_agg(row_to_json(t)), we force tight JSON output
    local json_query="SELECT json_agg(row_to_json(t)) FROM ($query) t;"

    PGPASSWORD=$DB_PASS psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -Atc "$json_query" 2>/dev/null || {
        echo '{"error": "Failed to execute database query. Is docker-compose running?"}'
        exit 1
    }
}
