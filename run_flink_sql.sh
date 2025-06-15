#!/bin/bash
# Script to run Flink SQL examples

set -e

# Check if Docker is running
if ! docker info &> /dev/null; then
    echo "Docker is not running. Please start Docker first."
    exit 1
fi

# Check if Flink containers are running
if ! docker ps | grep -q "flink-jobmanager"; then
    echo "Flink containers are not running. Starting them..."
    cd docker && docker-compose up -d
    cd ..
    
    # Wait for Flink to start
    echo "Waiting for Flink to start..."
    sleep 10
fi

# Function to run a SQL script
run_sql_script() {
    local script_name=$1
    local script_path="flink-sql/$script_name"
    
    echo "Running SQL script: $script_name"
    
    # Copy the SQL script to the Flink container
    docker cp "flink-job/src/main/resources/$script_path" flink-jobmanager:/opt/flink/sql/
    
    # Execute the SQL script using Flink SQL Client
    docker exec -it flink-jobmanager bash -c "/opt/flink/bin/sql-client.sh -f /opt/flink/sql/$script_name"
    
    echo "SQL script execution completed."
}

# Main menu
show_menu() {
    echo "Flink SQL Examples:"
    echo "1. Basic Dimension Table Lookup Join"
    echo "2. Filtering and Projection Test"
    echo "3. Run All Examples"
    echo "4. Exit"
    echo ""
    read -p "Select an option (1-4): " option
    
    case $option in
        1)
            run_sql_script "dimension-table-test.sql"
            ;;
        2)
            run_sql_script "filtering-projection-test.sql"
            ;;
        3)
            run_sql_script "dimension-table-test.sql"
            run_sql_script "filtering-projection-test.sql"
            ;;
        4)
            echo "Exiting..."
            exit 0
            ;;
        *)
            echo "Invalid option. Please try again."
            show_menu
            ;;
    esac
    
    # Return to menu after execution
    echo ""
    read -p "Press Enter to return to the menu..."
    show_menu
}

# Show the menu
echo "Flink DynamoDB Dimension Table Lab"
echo "=================================="
echo ""
show_menu