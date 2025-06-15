package xyz.zzhe.ddbconnectorlab.dynamodb;

import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.functions.FunctionContext;
import org.apache.flink.table.functions.LookupFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A lookup function for DynamoDB.
 */
public class DynamoDBLookupFunction extends LookupFunction {

    private static final Logger LOG = LoggerFactory.getLogger(DynamoDBLookupFunction.class);
    private static final long serialVersionUID = 1L;

    private final String tableName;
    private final String regionName;
    private final String[] fieldNames;
    private final String[] keyNames;
    private final int[] keyIndices;
    private final long cacheMaxSize;
    private final long cacheExpireMs;
    private final int maxRetryTimes;

    private transient DynamoDbClient dynamoDbClient;
    private transient ConcurrentHashMap<String, Map<String, AttributeValue>> cache;

    public DynamoDBLookupFunction(
            String tableName,
            String regionName,
            String[] fieldNames,
            String[] keyNames,
            int[] keyIndices,
            long cacheMaxSize,
            long cacheExpireMs,
            int maxRetryTimes) {
        this.tableName = tableName;
        this.regionName = regionName;
        this.fieldNames = fieldNames;
        this.keyNames = keyNames;
        this.keyIndices = keyIndices;
        this.cacheMaxSize = cacheMaxSize;
        this.cacheExpireMs = cacheExpireMs;
        this.maxRetryTimes = maxRetryTimes;
    }

    @Override
    public void open(FunctionContext context) throws Exception {
        dynamoDbClient = DynamoDbClient.builder()
                .region(Region.of(regionName))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        
        // Initialize cache
        cache = new ConcurrentHashMap<>();
        
        LOG.info("DynamoDB lookup function opened. Table: {}, Region: {}", tableName, regionName);
    }

    @Override
    public Collection<RowData> lookup(RowData keyRow) throws IOException {
        // Extract key value from the lookup key
        String keyValue = keyRow.getString(0).toString();
        
        // Check cache first
        Map<String, AttributeValue> item = cache.get(keyValue);
        
        if (item == null) {
            // Key not in cache, query DynamoDB
            try {
                item = queryDynamoDB(keyValue);
                if (item != null && !item.isEmpty()) {
                    // Add to cache
                    cache.put(keyValue, item);
                    
                    // Simple cache size control
                    if (cache.size() > cacheMaxSize) {
                        // Remove a random entry if cache is too large
                        // In a production system, you'd use a proper LRU cache
                        String keyToRemove = cache.keySet().iterator().next();
                        cache.remove(keyToRemove);
                    }
                }
            } catch (Exception e) {
                LOG.error("Error querying DynamoDB", e);
                return Collections.emptyList();
            }
        }
        
        if (item == null || item.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Convert DynamoDB item to RowData
        RowData rowData = convertToRowData(item);
        return Collections.singletonList(rowData);
    }

    private Map<String, AttributeValue> queryDynamoDB(String keyValue) {
        // Create key condition
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(keyNames[0], AttributeValue.builder().s(keyValue).build());
        
        // Create request
        GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build();
        
        // Execute request with retry logic
        for (int i = 0; i <= maxRetryTimes; i++) {
            try {
                GetItemResponse response = dynamoDbClient.getItem(request);
                if (response.hasItem()) {
                    return response.item();
                }
                return Collections.emptyMap();
            } catch (Exception e) {
                if (i == maxRetryTimes) {
                    LOG.error("Failed to query DynamoDB after {} retries", maxRetryTimes, e);
                    throw e;
                }
                LOG.warn("Error querying DynamoDB, retrying ({}/{})", i + 1, maxRetryTimes, e);
                try {
                    Thread.sleep(1000 * (i + 1)); // Simple backoff strategy
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        return Collections.emptyMap();
    }

    private RowData convertToRowData(Map<String, AttributeValue> item) {
        GenericRowData row = new GenericRowData(fieldNames.length);
        
        for (int i = 0; i < fieldNames.length; i++) {
            String fieldName = fieldNames[i];
            AttributeValue value = item.get(fieldName);
            
            if (value == null) {
                row.setField(i, null);
                continue;
            }
            
            // Handle different types of AttributeValue
            if (value.s() != null) {
                row.setField(i, StringData.fromString(value.s()));
            } else if (value.n() != null) {
                try {
                    row.setField(i, Double.parseDouble(value.n()));
                } catch (NumberFormatException e) {
                    row.setField(i, null);
                }
            } else if (value.bool() != null) {
                row.setField(i, value.bool());
            } else {
                // For other types, convert to string
                row.setField(i, StringData.fromString(value.toString()));
            }
        }
        
        return row;
    }

    @Override
    public void close() throws Exception {
        if (dynamoDbClient != null) {
            dynamoDbClient.close();
            dynamoDbClient = null;
        }
        
        if (cache != null) {
            cache.clear();
            cache = null;
        }
    }
}