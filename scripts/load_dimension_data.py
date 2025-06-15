#!/usr/bin/env python3
"""
Script to load sample product catalog data into DynamoDB.
"""

import json
import boto3
import os
import sys
from decimal import Decimal

def load_data(table_name, data_file):
    """
    Load data from JSON file into DynamoDB table.
    
    Args:
        table_name (str): Name of the DynamoDB table
        data_file (str): Path to JSON data file
    """
    # Initialize DynamoDB client
    dynamodb = boto3.resource('dynamodb', region_name='ap-southeast-1')
    table = dynamodb.Table(table_name)
    
    # Read data from JSON file
    try:
        with open(data_file, 'r') as file:
            items = json.load(file, parse_float=Decimal)
    except Exception as e:
        print(f"Error reading data file: {e}")
        sys.exit(1)
    
    # Load data into DynamoDB
    print(f"Loading {len(items)} items into {table_name}...")
    
    for item in items:
        try:
            table.put_item(Item=item)
            print(f"Added item: {item['product_id']}")
        except Exception as e:
            print(f"Error adding item {item.get('product_id', 'unknown')}: {e}")
    
    print("Data loading complete!")

def main():
    """Main function."""
    if len(sys.argv) < 3:
        print("Usage: load_dimension_data.py <table_name> <data_file>")
        sys.exit(1)
    
    table_name = sys.argv[1]
    data_file = sys.argv[2]
    
    if not os.path.exists(data_file):
        print(f"Data file not found: {data_file}")
        sys.exit(1)
    
    load_data(table_name, data_file)

if __name__ == "__main__":
    main()