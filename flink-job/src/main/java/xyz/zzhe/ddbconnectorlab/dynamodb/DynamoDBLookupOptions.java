package xyz.zzhe.ddbconnectorlab.dynamodb;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;

/**
 * Options for the DynamoDB lookup source.
 */
public class DynamoDBLookupOptions {

    public static final ConfigOption<String> TABLE_NAME =
            ConfigOptions.key("table-name")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The DynamoDB table name.");

    public static final ConfigOption<String> AWS_REGION =
            ConfigOptions.key("aws.region")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The AWS region where the DynamoDB table is located.");

    public static final ConfigOption<Integer> LOOKUP_CACHE_MAX_ROWS =
            ConfigOptions.key("lookup.cache.max-rows")
                    .intType()
                    .defaultValue(10000)
                    .withDescription("The max number of rows of lookup cache, over this value, the oldest rows will be eliminated.");

    public static final ConfigOption<Long> LOOKUP_CACHE_TTL =
            ConfigOptions.key("lookup.cache.ttl")
                    .longType()
                    .defaultValue(300000L) // 5 minutes by default
                    .withDescription("The cache time to live.");

    public static final ConfigOption<Integer> LOOKUP_MAX_RETRIES =
            ConfigOptions.key("lookup.max-retries")
                    .intType()
                    .defaultValue(3)
                    .withDescription("The max retry times if lookup database failed.");
}