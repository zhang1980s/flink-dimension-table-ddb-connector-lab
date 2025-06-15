# Flink DynamoDB Dimension Table Lab

This lab demonstrates how to use Amazon DynamoDB as a dimension table in Apache Flink 1.16 using the flink-connector-dynamodb.

## Overview

In this lab, we'll set up a complete environment to test the DynamoDB connector in Flink 1.16, focusing on dimension table functionality. We'll create the necessary AWS infrastructure using Pulumi with Golang, set up a Flink environment on an EC2 instance, and implement test cases to verify the connector's functionality.

## Architecture

The lab consists of the following components:

1. **AWS Infrastructure** (deployed in ap-southeast-1 region):
   - VPC with public subnet
   - EC2 instance running Amazon Linux 2023
   - DynamoDB table for dimension data
   - Security groups and networking components

2. **Flink Environment**:
   - Flink 1.16.x running in Docker
   - DynamoDB connector v4.0.0-1.16
   - Sample Flink SQL jobs

3. **Test Scenario**:
   - Product catalog dimension table in DynamoDB
   - Streaming order events
   - Dimension table lookups and joins

## Project Structure

```
flink-dimension-table-ddb-connector-lab/
├── README.md                      # Lab introduction and guide
├── infrastructure/                # Pulumi code for AWS resources
│   ├── go.mod                     # Go module definition
│   ├── go.sum                     # Go dependencies
│   └── main.go                    # Pulumi program for AWS resources
├── flink-job/                     # Flink application code
│   ├── pom.xml                    # Maven project definition
│   └── src/                       # Source code
│       └── main/
│           ├── java/
│           │   └── xyz/zzhe/ddb-connector-lab/   # Java code if needed
│           └── resources/
│               └── flink-sql/     # SQL scripts for testing
├── docker/                        # Docker setup for Flink
│   ├── Dockerfile                 # Custom Flink image if needed
│   └── docker-compose.yml         # Compose file for Flink services
└── data/                          # Sample data for testing
    ├── dimension-data.json        # DDB dimension table data
    └── stream-data.json           # Streaming data for joins
```

## Implementation Plan

### 1. AWS Infrastructure with Pulumi (Go)

We'll create the following AWS resources in the ap-southeast-1 region:

- VPC with CIDR block 10.0.0.0/16
- Public subnet with CIDR block 10.0.1.0/24
- Internet Gateway for public internet access
- Security Group allowing SSH (22), Flink UI (8081), and other necessary ports
- EC2 instance (t3.medium) with Amazon Linux 2023 in the public subnet
- DynamoDB table with appropriate schema for dimension data

- Go version: 1.24.4 linux/arm64

### 2. DynamoDB Table Design

We'll create a product catalog dimension table:

- Table name: `product_catalog`
- Schema:
  - product_id (primary key, String)
  - category (String)
  - name (String)
  - description (String)
  - price (Number)
  - inventory_status (String)
  - supplier_id (String)
  - last_updated (String - ISO timestamp)

### 3. Flink Environment Setup

We'll use the official Flink Docker image and extend it to include:

- Flink 1.16.x
- The DynamoDB connector v4.0.0-1.16
- AWS SDK dependencies
- Configuration for AWS credentials and region
- Amazon Corretto 11 as the Java runtime

### 4. Flink SQL Test Cases

We'll implement the following test cases:

1. **Basic Lookup Join**: Join a stream of order events with product catalog dimension data
2. **Temporal Join**: Test time-based joins if applicable
3. **Filtering**: Apply filters on dimension table data (e.g., only join with products from certain categories)
4. **Projection**: Select specific columns from the dimension table

### 5. Example Flink SQL

#### DynamoDB Connector Configuration

```sql
CREATE TABLE product_catalog (
  product_id STRING,
  category STRING,
  name STRING,
  description STRING,
  price DOUBLE,
  inventory_status STRING,
  supplier_id STRING,
  last_updated STRING,
  PRIMARY KEY (product_id) NOT ENFORCED
) WITH (
  'connector' = 'dynamodb',
  'table-name' = 'product_catalog',
  'aws.region' = 'ap-southeast-1',
  'scan.parallelism' = '4',
  'lookup.cache.max-rows' = '10000',
  'lookup.cache.ttl' = '300000'
);
```

#### Example Join Query

```sql
-- Create a source table for order events
CREATE TABLE order_events (
  order_id STRING,
  product_id STRING,
  quantity INT,
  customer_id STRING,
  order_time TIMESTAMP(3),
  WATERMARK FOR order_time AS order_time - INTERVAL '5' SECOND
) WITH (
  'connector' = 'datagen',
  -- other connector properties
);

-- Create a sink table for enriched orders
CREATE TABLE enriched_orders (
  order_id STRING,
  product_id STRING,
  product_name STRING,
  product_category STRING,
  unit_price DOUBLE,
  quantity INT,
  total_price DOUBLE,
  inventory_status STRING,
  customer_id STRING,
  order_time TIMESTAMP(3)
) WITH (
  'connector' = 'print'
);

-- Join query with dimension table lookup
INSERT INTO enriched_orders
SELECT
  o.order_id,
  o.product_id,
  p.name AS product_name,
  p.category AS product_category,
  p.price AS unit_price,
  o.quantity,
  o.quantity * p.price AS total_price,
  p.inventory_status,
  o.customer_id,
  o.order_time
FROM order_events o
JOIN product_catalog p ON o.product_id = p.product_id;
```

## Lab Guide

### Prerequisites

- AWS account with appropriate permissions
- Pulumi CLI installed
- Docker and Docker Compose installed
- Amazon Corretto 11 (Java 11) and Maven for Flink job development

### Part 1: Infrastructure Setup

1. Verify Pulumi (v3.177.0) and Go (v1.24.4 linux/arm64) are installed on your EC2 instance. Make sure that Pulumi is logged in via:
   ```bash
   pulumi login
   ```

2. Configure Pulumi to use the EC2 instance profile credentials by running:
   ```bash
   pulumi config set aws:useInstanceProfile true
   ```
   This ensures Pulumi will leverage the IAM role attached to your EC2 instance for AWS resource provisioning instead of using explicit credentials.

3. Configure Pulumi to use the ap-southeast-1 region:
   ```bash
   pulumi config set aws:region ap-southeast-1
   ```

4. Deploy the infrastructure using Pulumi:
   ```bash
   pulumi up
   ```
   Note: The pre-defined key pair 'keypair-sandbox0-sin-mymac.pem' will be used to access the EC2 instance created by Pulumi.

4. Verify the resources are created correctly

### Part 2: Flink Environment Setup

1. Connect to the EC2 instance
2. Install Amazon Corretto 11 using `sudo dnf install -y java-11-amazon-corretto-devel`
3. Install Docker and Docker Compose
4. Set up the Flink environment using Docker
5. Configure AWS credentials for Flink

### Part 3: DynamoDB Table Setup

1. Create the product catalog table in DynamoDB
2. Populate the table with sample data
3. Verify the data is accessible

### Part 4: Flink SQL Examples

1. Create and run the Flink SQL examples
2. Verify the dimension table lookups work correctly
3. Experiment with different connector configurations

### Part 5: Testing and Verification

1. Run the test cases
2. Verify the results
3. Troubleshoot any issues

## Conclusion

This lab demonstrates how to use DynamoDB as a dimension table in Flink SQL using the flink-connector-dynamodb. By completing this lab, you'll understand how to:

1. Set up the necessary AWS infrastructure
2. Configure the DynamoDB connector in Flink
3. Perform dimension table lookups and joins
4. Optimize the connector configuration for performance

## References

- [Apache Flink Documentation](https://nightlies.apache.org/flink/flink-docs-release-1.16/)
- [Flink DynamoDB Connector](https://github.com/apache/flink-connector-aws/tree/v4.0/flink-connector-dynamodb)
- [Amazon DynamoDB Documentation](https://docs.aws.amazon.com/dynamodb/)
- [Pulumi Documentation](https://www.pulumi.com/docs/)
