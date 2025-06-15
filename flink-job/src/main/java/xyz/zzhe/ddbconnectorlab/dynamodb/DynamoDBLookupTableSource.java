package xyz.zzhe.ddbconnectorlab.dynamodb;

import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.connector.source.LookupTableSource;
import org.apache.flink.table.connector.source.TableFunctionProvider;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.LogicalTypeRoot;
import org.apache.flink.util.Preconditions;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * A {@link LookupTableSource} for DynamoDB.
 */
public class DynamoDBLookupTableSource implements LookupTableSource {

    private final DataType physicalRowDataType;
    private final String tableName;
    private final String regionName;
    private final String[] fieldNames;
    private final String[] keyNames;
    private final long cacheMaxSize;
    private final long cacheExpireMs;
    private final int maxRetryTimes;

    public DynamoDBLookupTableSource(
            DataType physicalRowDataType,
            String tableName,
            String regionName,
            String[] keyNames,
            long cacheMaxSize,
            long cacheExpireMs,
            int maxRetryTimes) {
        this.physicalRowDataType = physicalRowDataType;
        this.tableName = tableName;
        this.regionName = regionName;
        this.fieldNames = DataType.getFieldNames(physicalRowDataType).toArray(new String[0]);
        this.keyNames = keyNames;
        this.cacheMaxSize = cacheMaxSize;
        this.cacheExpireMs = cacheExpireMs;
        this.maxRetryTimes = maxRetryTimes;
        
        // Validate that key fields exist in the table schema
        List<String> fieldNameList = Arrays.asList(fieldNames);
        for (String keyName : keyNames) {
            Preconditions.checkArgument(
                    fieldNameList.contains(keyName),
                    "Key field '%s' not found in table schema",
                    keyName);
        }
    }

    @Override
    public LookupRuntimeProvider getLookupRuntimeProvider(LookupContext context) {
        // Get the indices of the key fields in the table schema
        int[] keyIndices = Arrays.stream(keyNames)
                .mapToInt(keyName -> {
                    for (int i = 0; i < fieldNames.length; i++) {
                        if (fieldNames[i].equals(keyName)) {
                            return i;
                        }
                    }
                    throw new IllegalArgumentException("Key field '" + keyName + "' not found in table schema");
                })
                .toArray();

        // Create and return a table function
        return TableFunctionProvider.of(
                new DynamoDBLookupFunction(
                        tableName,
                        regionName,
                        fieldNames,
                        keyNames,
                        keyIndices,
                        cacheMaxSize,
                        cacheExpireMs,
                        maxRetryTimes));
    }

    @Override
    public DynamicTableSource copy() {
        return new DynamoDBLookupTableSource(
                physicalRowDataType,
                tableName,
                regionName,
                keyNames,
                cacheMaxSize,
                cacheExpireMs,
                maxRetryTimes);
    }

    @Override
    public String asSummaryString() {
        return "DynamoDB Table Source";
    }
}