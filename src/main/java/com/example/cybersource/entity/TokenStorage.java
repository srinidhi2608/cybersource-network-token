package com.example.cybersource.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "TokenStorage")
public class TokenStorage {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String paymentTokenId;
    
    private String cryptogram;
    
    private String merchantId;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private Map<String, Object> metadata;
    
    public TokenStorage(String paymentTokenId, String cryptogram, String merchantId, Map<String, Object> metadata) {
        this.paymentTokenId = paymentTokenId;
        this.cryptogram = cryptogram;
        this.merchantId = merchantId;
        this.metadata = metadata;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}