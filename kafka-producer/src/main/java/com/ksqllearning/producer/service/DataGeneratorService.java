package com.ksqllearning.producer.service;

import com.ksqllearning.producer.model.SalesTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataGeneratorService {

    @Autowired
    private KafkaTemplate<String, SalesTransaction> kafkaTemplate;
    private static final String TOPIC = "sales-transactions";
    
    private static final String[] PRODUCTS = {
        "Laptop", "Smartphone", "Tablet", "Headphones", "Smartwatch",
        "Monitor", "Keyboard", "Mouse", "Webcam", "Speaker"
    };
    
    private static final String[] CATEGORIES = {
        "Electronics", "Computers", "Audio", "Accessories", "Wearables"
    };
    
    private static final String[] PAYMENT_METHODS = {
        "Credit Card", "Debit Card", "PayPal", "Cash", "Cryptocurrency"
    };
    
    private static final String[] REGIONS = {
        "North", "South", "East", "West", "Central"
    };
    
    private static final String[] STORES = {
        "Store-001", "Store-002", "Store-003", "Store-004", "Store-005",
        "Store-006", "Store-007", "Store-008", "Store-009", "Store-010"
    };
    
    private final Random random = new Random();

    public DataGeneratorService(KafkaTemplate<String, SalesTransaction> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<String> generateAndSendData(int numberOfRecords) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Starting to generate {} records", numberOfRecords);
            long startTime = System.currentTimeMillis();
            
            ExecutorService executor = Executors.newFixedThreadPool(10);
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            int batchSize = 10000;
            int batches = (numberOfRecords + batchSize - 1) / batchSize;
            
            for (int batch = 0; batch < batches; batch++) {
                final int batchNumber = batch;
                final int recordsInBatch = Math.min(batchSize, numberOfRecords - (batch * batchSize));
                
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    for (int i = 0; i < recordsInBatch; i++) {
                        SalesTransaction transaction = generateRandomTransaction();
                        kafkaTemplate.send(TOPIC, transaction.getTransactionId(), transaction);
                        
                        if ((batchNumber * batchSize + i + 1) % 50000 == 0) {
                            log.info("Sent {} records", batchNumber * batchSize + i + 1);
                        }
                    }
                }, executor);
                
                futures.add(future);
            }
            
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            executor.shutdown();
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            double recordsPerSecond = (numberOfRecords * 1000.0) / duration;
            
            String result = String.format("Successfully sent %d records in %d ms (%.2f records/sec)", 
                numberOfRecords, duration, recordsPerSecond);
            log.info(result);
            
            return result;
        });
    }

    private SalesTransaction generateRandomTransaction() {
        String product = PRODUCTS[random.nextInt(PRODUCTS.length)];
        int quantity = random.nextInt(10) + 1;
        double price = 50 + (random.nextDouble() * 1950); // $50 to $2000
        
        return SalesTransaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .customerId("CUST-" + String.format("%06d", random.nextInt(100000)))
                .productId("PROD-" + String.format("%05d", random.nextInt(50000)))
                .productName(product)
                .category(CATEGORIES[random.nextInt(CATEGORIES.length)])
                .price(Math.round(price * 100.0) / 100.0)
                .quantity(quantity)
                .totalAmount(Math.round(price * quantity * 100.0) / 100.0)
                .paymentMethod(PAYMENT_METHODS[random.nextInt(PAYMENT_METHODS.length)])
                .region(REGIONS[random.nextInt(REGIONS.length)])
                .store(STORES[random.nextInt(STORES.length)])
                .timestamp(LocalDateTime.now().minusSeconds(random.nextInt(86400))) // Random time within last 24 hours
                .build();
    }
}
