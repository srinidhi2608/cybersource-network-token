package com.example.cybersource.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Entity for auditing information fetch operations.
 * Records every cryptogram fetch operation and other information retrieval requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "FetchInformationAudits")
public class FetchInformationAudit {
    
    @Id
    private String id;
    
    /**
     * TMS internal payment token ID.
     */
    @Indexed
    private String paymentTokenId;
    
    /**
     * Type of information being fetched.
     * Values: "InstrumentIdentifier", "Cryptogram", "NetworkToken"
     */
    @Indexed
    private String informationType;
    
    /**
     * Event reference categorizing the type of operation.
     * Values: "CreateToken", "FetchCryptogram", "LCM"
     */
    @Indexed
    private String eventReference;
    
    /**
     * Timestamp when the fetch operation occurred.
     */
    @Indexed
    private LocalDateTime timestamp;
    
    /**
     * Flag indicating if the fetch request was completed successfully.
     */
    private Boolean isRequestComplete;
    
    /**
     * External reference or transaction reference number from the request.
     */
    @Indexed
    private String externalReference;
    
    /**
     * Unique trace ID (UUID) generated at insert time for identifying the document.
     */
    @Indexed(unique = true)
    private String traceId;
}
