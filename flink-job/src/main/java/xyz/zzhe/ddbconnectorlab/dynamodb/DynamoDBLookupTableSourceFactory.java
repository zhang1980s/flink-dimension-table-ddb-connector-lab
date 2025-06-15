package xyz.zzhe.ddbconnectorlab.dynamodb;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.table.catalog.Column;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.factories.DynamicTableSourceFactory;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.flink.table.types.DataType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Factory for creating {@link DynamoDBLookupTableSource}.
 */
public class DynamoDBLookupTableSourceFactory implements DynamicTableSourceFactory {

    public static final String IDENTIFIER = "dynamodb-lookup";

    @Override
    public DynamicTableSource createDynamicTableSource(Context context) {
        // Use helper provided by Flink to validate options
        final FactoryUtil.TableFactoryHelper helper = FactoryUtil.createTableFactoryHelper(this, context);
        
        // Validate all options
        helper.validate();
        
        // Get the validated options
        final ReadableConfig options = helper.getOptions();
        
        // Get the table schema
        ResolvedSchema schema = context.getCatalogTable().getResolvedSchema();
        DataType physicalRowDataType = schema.toPhysicalRowDataType();
        
        // Get primary key names
        List<String> primaryKeyNames = schema.getPrimaryKey()
                .map(pk -> pk.getColumns())
                .orElse(Arrays.asList("product_id")); // Default to product_id if no primary key is defined
        
        // Extract configuration options
        String tableName = options.get(DynamoDBLookupOptions.TABLE_NAME);
        String regionName = options.get(DynamoDBLookupOptions.AWS_REGION);
        long cacheMaxSize = options.get(DynamoDBLookupOptions.LOOKUP_CACHE_MAX_ROWS);
        long cacheExpireMs = options.get(DynamoDBLookupOptions.LOOKUP_CACHE_TTL);
        int maxRetryTimes = options.get(DynamoDBLookupOptions.LOOKUP_MAX_RETRIES);
        
        // Create and return the table source
        return new DynamoDBLookupTableSource(
                physicalRowDataType,
                tableName,
                regionName,
                primaryKeyNames.toArray(new String[0]),
                cacheMaxSize,
                cacheExpireMs,
                maxRetryTimes);
    }

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        Set<ConfigOption<?>> options = new HashSet<>();
        options.add(DynamoDBLookupOptions.TABLE_NAME);
        options.add(DynamoDBLookupOptions.AWS_REGION);
        return options;
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        Set<ConfigOption<?>> options = new HashSet<>();
        options.add(DynamoDBLookupOptions.LOOKUP_CACHE_MAX_ROWS);
        options.add(DynamoDBLookupOptions.LOOKUP_CACHE_TTL);
        options.add(DynamoDBLookupOptions.LOOKUP_MAX_RETRIES);
        return options;
    }
}