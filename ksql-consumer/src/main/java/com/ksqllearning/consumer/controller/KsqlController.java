package com.ksqllearning.consumer.controller;

import com.ksqllearning.consumer.service.KsqlQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
                        "message", "KSQL streams and tables initialized successfully. Created: sales_stream, sales_by_category, sales_by_region, sales_by_payment_method, sales_by_store"
                )))
                .exceptionally(e -> ResponseEntity.status(500).body(Map.of(
                        "status", "error",
                        "message", "Failed to initialize: " + e.getMessage()
                )));
    }

    @PostMapping("/query")
    public CompletableFuture<ResponseEntity<List<Map<String, Object>>>> executeCustomQuery(
            @RequestBody Map<String, String> request) {
        String sql = request.get("sql");
        return ksqlQueryService.executeQuery(sql)
                .thenApply(ResponseEntity::ok)
                .exceptionally(e -> ResponseEntity.status(500).body(List.of(
                        Map.of("error", e.getMessage())
                )));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
