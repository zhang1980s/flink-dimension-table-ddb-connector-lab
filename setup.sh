#!/bin/bash
# Setup script for Flink DynamoDB Dimension Table Lab

set -e

echo "Setting up Flink DynamoDB Dimension Table Lab..."

# Check if AWS CLI is installed
if ! command -v aws &> /dev/null; then
    echo "AWS CLI is not installed. Installing..."
    curl "https://awscli.amazonaws.com/awscli-exe-linux-aarch64.zip" -o "awscliv2.zip"
    unzip awscliv2.zip
    sudo ./aws/install
    rm -rf aws awscliv2.zip
fi

# Check if Python is installed
if ! command -v python3 &> /dev/null; then
    echo "Python 3 is not installed. Installing..."
    sudo dnf install -y python3 python3-pip
fi

# Install Python dependencies
echo "Installing Python dependencies..."
pip3 install boto3 --user

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "Docker is not installed. Installing..."
    sudo dnf install -y docker
    sudo systemctl enable docker
    sudo systemctl start docker
    sudo usermod -aG docker $USER
    echo "Please log out and log back in for Docker group changes to take effect."
fi

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo "Docker Compose is not installed. Installing..."
    sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
fi

# Check if Java 11 is installed
if ! command -v java &> /dev/null || [[ $(java -version 2>&1) != *"11"* ]]; then
    echo "Java 11 is not installed. Installing Amazon Corretto 11..."
    sudo dnf install -y java-11-amazon-corretto-devel
fi

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Maven is not installed. Installing..."
    sudo dnf install -y maven
fi

# Check if Pulumi is installed
if ! command -v pulumi &> /dev/null; then
    echo "Pulumi is not installed. Installing..."
    curl -fsSL https://get.pulumi.com | sh
    export PATH=$PATH:$HOME/.pulumi/bin
fi

echo "Setting up Pulumi for AWS..."
pulumi config set aws:region ap-southeast-1
pulumi config set aws:useInstanceProfile true

echo "Setup complete!"
echo ""
echo "Next steps:"
echo "1. Deploy AWS infrastructure: cd infrastructure && pulumi up"
echo "2. Load sample data: python3 scripts/load_dimension_data.py product_catalog data/dimension-data.json"
echo "3. Start Flink environment: cd docker && docker-compose up -d"
echo "4. Access Flink UI at http://localhost:8081"
echo "5. Run Flink SQL examples using the Flink SQL Client"
echo ""