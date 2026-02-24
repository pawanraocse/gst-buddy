function cmd_filter_logs() {
    local log_file="$1"
    local lines="${2:-500}" # Default to last 500 lines

    if [[ -z "$log_file" ]]; then
        echo "Error: filter-logs requires a log file path."
        echo "Example: ./cli.sh filter-logs backend-service/target/spring.log"
        exit 1
    fi

    if [[ ! -f "$log_file" ]]; then
        echo "Error: File not found: $log_file"
        exit 1
    fi

    # 1. Grab the last N lines
    # 2. Extract lines containing ERROR or WARN, and stack traces (lines starting with whitespace following an error)
    # Using awk to keep context of stacktraces
    
    echo "--- Filtered Errors & Warnings (Last $lines lines) ---"
    tail -n "$lines" "$log_file" | awk '
        /ERROR|WARN|Exception/ {
            print $0
            in_trace = 1
            next
        }
        in_trace && /^[[:space:]]+at / {
            print $0
            next
        }
        {
            in_trace = 0
        }
    '
}
