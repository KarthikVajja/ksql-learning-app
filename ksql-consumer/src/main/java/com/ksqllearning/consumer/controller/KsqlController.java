package com.ksqllearning.consumer.controller;

import com.ksqllearning.consumer.service.KsqlQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/ksql")
@RequiredArgsConstructor
public class KsqlController {

    @Autowired
    private KsqlQueryService ksqlQueryService;

    @PostMapping("/initialize")
    public CompletableFuture<ResponseEntity<Map<String, String>>> initialize() {
        return ksqlQueryService.initializeKsqlStreamsAndTables()
                .thenApply(v -> ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "KSQL streams and tables initialized"
                )));
    }

    @PostMapping("/query")
    public CompletableFuture<ResponseEntity<List<Map<String, Object>>>> executeCustomQuery(
            @RequestBody Map<String, String> request) {
        String sql = request.get("sql");
        return ksqlQueryService.executeQuery(sql)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/analytics/by-category")
    public CompletableFuture<ResponseEntity<List<Map<String, Object>>>> getSalesByCategory() {
        return ksqlQueryService.getTotalSalesByCategory()
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/analytics/by-region")
    public CompletableFuture<ResponseEntity<List<Map<String, Object>>>> getSalesByRegion() {
        return ksqlQueryService.getTotalSalesByRegion()
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/analytics/high-value")
    public CompletableFuture<ResponseEntity<List<Map<String, Object>>>> getHighValueTransactions(
            @RequestParam(defaultValue = "5000") double minAmount) {
        return ksqlQueryService.getHighValueTransactions(minAmount)
                .thenApply(ResponseEntity::ok);
    }
//    @GetMapping(
//            value = "/analytics/high-value",
//            produces = MediaType.TEXT_EVENT_STREAM_VALUE
//    )
//    public Flux<Map<String, Object>> streamHighValueTransactions(
//            @RequestParam(defaultValue = "5000") double minAmount) {
//
//        return ksqlQueryService.streamHighValueTransactions(minAmount);
//    }

    @GetMapping("/analytics/by-payment-method")
    public CompletableFuture<ResponseEntity<List<Map<String, Object>>>> getTransactionsByPaymentMethod() {
        return ksqlQueryService.getTransactionsByPaymentMethod()
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/analytics/top-products")
    public CompletableFuture<ResponseEntity<List<Map<String, Object>>>> getTopProducts(
            @RequestParam(defaultValue = "10") int limit) {
        return ksqlQueryService.getTopProducts(limit)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
