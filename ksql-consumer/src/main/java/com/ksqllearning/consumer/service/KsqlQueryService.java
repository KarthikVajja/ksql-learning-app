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
                // Create stream from Kafka topic
                String createStreamSql = """
                    CREATE STREAM IF NOT EXISTS sales_stream (
                        transactionId VARCHAR KEY,
                        customerId VARCHAR,
                        productId VARCHAR,
                        productName VARCHAR,
                        category VARCHAR,
                        price DOUBLE,
                        quantity INT,
                        totalAmount DOUBLE,
                        paymentMethod VARCHAR,
                        region VARCHAR,
                        store VARCHAR,
                        timestamp VARCHAR
                    ) WITH (
                        KAFKA_TOPIC='sales-transactions',
                        VALUE_FORMAT='JSON',
                        TIMESTAMP='timestamp',
                        TIMESTAMP_FORMAT='yyyy-MM-dd''T''HH:mm:ss'
                    );
                    """;

                ksqlClient.executeStatement(createStreamSql).get();
                log.info("Created SALES_STREAM");

                // Create table for aggregations
                String createTableSql = """
                    CREATE TABLE IF NOT EXISTS sales_by_category AS
                    SELECT 
                        category,
                        COUNT(*) AS transaction_count,
                        SUM(totalAmount) AS total_revenue,
                        AVG(totalAmount) AS avg_transaction_value,
                        MIN(totalAmount) AS min_transaction,
                        MAX(totalAmount) AS max_transaction
                    FROM sales_stream
                    GROUP BY category;
                    """;
//                String createTableSql = """
//                    CREATE TABLE IF NOT EXISTS sales_by_category AS
//                    SELECT
//                        category,
//                        COUNT(*) AS transaction_count,
//                        SUM(totalAmount) AS total_revenue,
//                        AVG(totalAmount) AS avg_transaction_value,
//                        MIN(totalAmount) AS min_transaction,
//                        MAX(totalAmount) AS max_transaction
//                    FROM sales_stream
//                    GROUP BY category
//                    EMIT CHANGES;
//                    """;

                try {
                    ksqlClient.executeStatement(createTableSql).get();
                    log.info("Created SALES_BY_CATEGORY table");
                } catch (Exception e) {
                    log.info("SALES_BY_CATEGORY table may already exist");
                }

                // Create another useful table - sales by region
                String createRegionTableSql = """
                    CREATE TABLE IF NOT EXISTS sales_by_region AS
                    SELECT 
                        region,
                        COUNT(*) AS transaction_count,
                        SUM(totalAmount) AS total_revenue
                    FROM sales_stream
                    GROUP BY region;
                    """;
//                String createRegionTableSql = """
//                    CREATE TABLE IF NOT EXISTS sales_by_region AS
//                    SELECT
//                        region,
//                        COUNT(*) AS transaction_count,
//                        SUM(totalAmount) AS total_revenue
//                    FROM sales_stream
//                    GROUP BY region
//                    EMIT CHANGES;
//                    """;

                try {
                    ksqlClient.executeStatement(createRegionTableSql).get();
                    log.info("Created SALES_BY_REGION table");
                } catch (Exception e) {
                    log.info("SALES_BY_REGION table may already exist");
                }

            } catch (Exception e) {
                log.error("Error initializing KSQL streams/tables", e);
                throw new RuntimeException(e);
            }
        });
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

    public CompletableFuture<List<Map<String, Object>>> getTotalSalesByCategory() {
        String query = "SELECT * FROM sales_by_category;";
        return executeQuery(query);
    }

    public CompletableFuture<List<Map<String, Object>>> getTotalSalesByRegion() {
        String query = "SELECT * FROM sales_by_region;";
        return executeQuery(query);
    }

    public CompletableFuture<List<Map<String, Object>>> getHighValueTransactions(double minAmount) {
        String query = String.format(
            "SELECT transactionId, customerId, productName, totalAmount, region, timestamp " +
            "FROM sales_stream " +
            "WHERE totalAmount > %.2f " +
//            "EMIT CHANGES " +
            "LIMIT 100;", minAmount);
        return executeQuery(query);
    }

    public CompletableFuture<List<Map<String, Object>>> getTransactionsByPaymentMethod() {
        String query = """
            SELECT 
                paymentMethod,
                COUNT(*) AS count,
                SUM(totalAmount) AS total_revenue
            FROM sales_stream
            GROUP BY paymentMethod
            """;
//        String query = """
//            SELECT
//                paymentMethod,
//                COUNT(*) AS count,
//                SUM(totalAmount) AS total_revenue
//            FROM sales_stream
//            GROUP BY paymentMethod
//            EMIT CHANGES;
//            """;
        return executeQuery(query);
    }

    public CompletableFuture<List<Map<String, Object>>> getTopProducts(int limit) {
        String query = String.format("""
            SELECT 
                productName,
                COUNT(*) AS purchase_count,
                SUM(totalAmount) AS total_revenue
            FROM sales_stream
            GROUP BY productName
            LIMIT %d;
            """, limit);
        return executeQuery(query);
//        String query = String.format("""
//            SELECT
//                productName,
//                COUNT(*) AS purchase_count,
//                SUM(totalAmount) AS total_revenue
//            FROM sales_stream
//            GROUP BY productName
//            EMIT CHANGES
//            LIMIT %d;
//            """, limit);
//        return executeQuery(query);
    }
}
