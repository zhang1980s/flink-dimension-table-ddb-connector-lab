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
  'rows-per-second' = '1',
  'fields.order_id.kind' = 'sequence',
  'fields.order_id.start' = '1',
  'fields.order_id.end' = '100',
  'fields.product_id.kind' = 'random',
  'fields.product_id.length' = '4',
  'fields.quantity.kind' = 'random',
  'fields.quantity.min' = '1',
  'fields.quantity.max' = '10',
  'fields.customer_id.kind' = 'random',
  'fields.customer_id.length' = '4'
);

-- Create DynamoDB dimension table for product catalog
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
  'connector' = 'dynamodb-lookup',
  'table-name' = 'product-catalog-43c2419',
  'aws.region' = 'ap-southeast-1',
  'lookup.cache.max-rows' = '10000',
  'lookup.cache.ttl' = '300000'
);

-- Create a sink table for filtered orders
CREATE TABLE filtered_orders (
  order_id STRING,
  product_id STRING,
  product_name STRING,
  product_category STRING,
  unit_price DOUBLE,
  quantity INT,
  total_price DOUBLE,
  customer_id STRING,
  order_time TIMESTAMP(3)
) WITH (
  'connector' = 'print'
);

-- Test Case 1: Filtering - Only join with Electronics products
INSERT INTO filtered_orders
SELECT
  o.order_id,
  o.product_id,
  p.name AS product_name,
  p.category AS product_category,
  p.price AS unit_price,
  o.quantity,
  o.quantity * p.price AS total_price,
  o.customer_id,
  o.order_time
FROM order_events o
JOIN product_catalog p ON o.product_id = p.product_id
WHERE p.category = 'Electronics';

-- Create a sink table for projected orders
CREATE TABLE projected_orders (
  order_id STRING,
  product_id STRING,
  product_name STRING,
  unit_price DOUBLE,
  quantity INT,
  total_price DOUBLE,
  order_time TIMESTAMP(3)
) WITH (
  'connector' = 'print'
);

-- Test Case 2: Projection - Select only specific columns from dimension table
INSERT INTO projected_orders
SELECT
  o.order_id,
  o.product_id,
  p.name AS product_name,
  p.price AS unit_price,
  o.quantity,
  o.quantity * p.price AS total_price,
  o.order_time
FROM order_events o
JOIN product_catalog p ON o.product_id = p.product_id;