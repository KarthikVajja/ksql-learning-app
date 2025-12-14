package com.ksqllearning.producer.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesTransaction {
    private String transactionId;
    private String customerId;
    private String productId;
    private String productName;
    private String category;
    private Double price;
    private Integer quantity;
    private Double totalAmount;
    private String paymentMethod;
    private String region;
    private String store;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
}
