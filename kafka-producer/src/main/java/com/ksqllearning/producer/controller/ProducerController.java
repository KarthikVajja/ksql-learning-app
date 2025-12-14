package com.ksqllearning.producer.controller;

import com.ksqllearning.producer.service.DataGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/producer")
@RequiredArgsConstructor
public class ProducerController {

    @Autowired
    private DataGeneratorService dataGeneratorService;


    @PostMapping("/generate")
    public CompletableFuture<ResponseEntity<Map<String, String>>> generateData(
            @RequestParam(defaultValue = "1000000") int records) {
        
        return dataGeneratorService.generateAndSendData(records)
                .thenApply(result -> ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", result
                )));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
