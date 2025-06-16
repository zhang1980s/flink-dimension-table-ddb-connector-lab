#!/bin/bash
# Script to build and deploy the Flink DynamoDB connector

set -e

echo "Building the Flink DynamoDB connector..."

# Build the project
cd flink-job
mvn clean package
cd ..

# Verify the connector JAR was created
if [ ! -f "flink-job/target/ddb-connector-lab-1.0-SNAPSHOT-connector.jar" ]; then
    echo "Error: Connector JAR was not created. Check the Maven build."
    exit 1
fi

echo "Building Docker image..."
docker build -t flink-ddb-connector -f docker/Dockerfile .

echo "Stopping existing containers..."
cd docker
docker-compose down || true
cd ..

echo "Starting Flink containers..."
cd docker
docker-compose up -d
cd ..

echo "Waiting for Flink to start..."
sleep 10

echo "Deployment complete!"
echo "You can access the Flink UI at http://localhost:8081"
echo "Run the SQL examples using: ./run_flink_sql.sh"
