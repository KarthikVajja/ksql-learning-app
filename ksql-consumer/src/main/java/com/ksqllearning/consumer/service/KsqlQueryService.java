package com.ksqllearning.consumer.service;

import io.confluent.ksql.api.client.BatchedQueryResult;
import io.confluent.ksql.api.client.Client;
import io.confluent.ksql.api.client.Row;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class KsqlQueryService {

    @Autowired
    private Client ksqlClient;

    public CompletableFuture<Void> initializeKsqlStreamsAndTables() {
        return CompletableFuture.runAsync(() -> {
            try {
                // First, clean up existing streams and tables
                cleanupExistingObjects();

                // Create stream from Kafka topic using PROTOBUF_NOSR
                String createStreamSql = """
                        CREATE STREAM IF NOT EXISTS sales_stream (
                          transaction_id VARCHAR KEY,
                          customer_id VARCHAR,
                          product_id VARCHAR,
                          product_name VARCHAR,
                          category VARCHAR,
                          price DOUBLE,
                          quantity INT,
                          total_amount DOUBLE,
                          payment_method VARCHAR,
                          region VARCHAR,
                          store VARCHAR,
                          timestamp BIGINT
                        ) WITH (
                          KAFKA_TOPIC='sales-transactions',
                          KEY_FORMAT='KAFKA',
                          VALUE_FORMAT='PROTOBUF_NOSR',
                          PARTITIONS=1,
                          REPLICAS=1
                        );
                    """;


                ksqlClient.executeStatement(createStreamSql).get();
                log.info("Created SALES_STREAM with PROTOBUF_NOSR format");

                // Create sales table
                String salesTableSql = """
                        CREATE TABLE sales_table AS
                           SELECT transaction_id,
                                  SUM(total_amount) AS total_amount,
                                  MAX(timestamp) AS latest_timestamp
                           FROM SALES_STREAM
                           GROUP BY transaction_id;
                    """;

                ksqlClient.executeStatement(salesTableSql).get();
                log.info("Created SALES table");


            } catch (Exception e) {
                log.error("Error initializing KSQL streams/tables", e);
                throw new RuntimeException(e);
            }
        });
    }

    private void cleanupExistingObjects() {
        log.info("Cleaning up existing KSQL objects...");
        
        try {
            // Drop tables first (in reverse order of dependencies)
            dropIfExists("DROP TABLE IF EXISTS sales_table DELETE TOPIC;");
            
            // Then drop streams
            dropIfExists("DROP STREAM IF EXISTS sales_stream DELETE TOPIC;");
            
            // Small delay to ensure cleanup completes
            Thread.sleep(2000);
            
            log.info("Cleanup completed successfully");
        } catch (Exception e) {
            log.warn("Error during cleanup (may be safe to ignore): {}", e.getMessage());
        }
    }

    private void dropIfExists(String dropStatement) {
        try {
            ksqlClient.executeStatement(dropStatement).get();
            log.info("Executed: {}", dropStatement);
        } catch (Exception e) {
            log.debug("Could not execute drop statement (object may not exist): {}", e.getMessage());
        }
    }

    public CompletableFuture<List<Map<String, Object>>> executeQuery(String sql) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Executing query: {}", sql);
                BatchedQueryResult result = ksqlClient.executeQuery(sql);
                
                List<Map<String, Object>> rows = new ArrayList<>();
                List<Row> rowsList = result.get();
                
                for (Row row : rowsList) {
                    Map<String, Object> rowMap = new HashMap<>();
                    for (String columnName : row.columnNames()) {
                        rowMap.put(columnName, row.getValue(columnName));
                    }
                    rows.add(rowMap);
                }
                
                log.info("Query returned {} rows", rows.size());
                return rows;
                
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error executing query", e);
                throw new RuntimeException("Query execution failed: " + e.getMessage(), e);
            }
        });
    }
}
