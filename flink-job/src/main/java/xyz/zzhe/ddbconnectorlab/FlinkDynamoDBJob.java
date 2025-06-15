package xyz.zzhe.ddbconnectorlab;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.factories.FactoryUtil;

import xyz.zzhe.ddbconnectorlab.dynamodb.DynamoDBLookupTableSourceFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Flink job to test DynamoDB dimension table functionality.
 */
public class FlinkDynamoDBJob {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: FlinkDynamoDBJob <sql-file-path>");
            System.exit(1);
        }

        String sqlFilePath = args[0];
        
        // Set up the execution environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        EnvironmentSettings settings = EnvironmentSettings.newInstance().inStreamingMode().build();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        
        // Configure AWS region
        tableEnv.getConfig().getConfiguration().setString("aws.region", "ap-southeast-1");
        
        // Our custom DynamoDB lookup table source factory will be discovered automatically
        // through the Java SPI mechanism (META-INF/services)
        System.out.println("Using custom DynamoDB lookup table source factory: " +
                DynamoDBLookupTableSourceFactory.IDENTIFIER);
        
        // Read and execute SQL statements
        String sqlScript = readSqlScript(sqlFilePath);
        String[] statements = sqlScript.split(";");
        
        for (String statement : statements) {
            String trimmedStatement = statement.trim();
            if (!trimmedStatement.isEmpty()) {
                System.out.println("Executing SQL: " + trimmedStatement);
                tableEnv.executeSql(trimmedStatement);
            }
        }
    }
    
    /**
     * Read SQL script from resources.
     *
     * @param path Path to SQL script
     * @return SQL script content
     */
    private static String readSqlScript(String path) throws IOException {
        try (InputStream inputStream = FlinkDynamoDBJob.class.getClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new IOException("SQL script not found: " + path);
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }
}