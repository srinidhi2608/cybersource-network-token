package com.example.cybersource.util;

import com.example.cybersource.constants.BillingConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BillingReferenceNumberGenerator.
 */
@ExtendWith(MockitoExtension.class)
class BillingReferenceNumberGeneratorTest {
    
    @Mock
    private MongoTemplate mongoTemplate;
    
    private BillingReferenceNumberGenerator generator;
    
    @BeforeEach
    void setUp() {
        generator = new BillingReferenceNumberGenerator(mongoTemplate);
    }
    
    @Test
    void testGenerateUniqueReferenceNumber_Success() {
        // Arrange
        BillingReferenceNumberGenerator.SequenceDocument sequenceDoc = new BillingReferenceNumberGenerator.SequenceDocument();
        sequenceDoc.setSeq(1L);
        
        when(mongoTemplate.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                any(Class.class),
                eq(BillingConstants.DEFAULT_SEQUENCE_COLLECTION)))
            .thenReturn(sequenceDoc);
        
        // Act
        String referenceNumber = generator.generateUniqueReferenceNumber();
        
        // Assert
        assertNotNull(referenceNumber);
        assertTrue(referenceNumber.startsWith(BillingConstants.BILLING_REF_PREFIX));
        assertTrue(referenceNumber.matches("BILL-\\d+-\\d{8}"));
        
        verify(mongoTemplate).findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                any(Class.class),
                eq(BillingConstants.DEFAULT_SEQUENCE_COLLECTION));
    }
    
    @Test
    void testGenerateUniqueReferenceNumber_MultipleCallsGenerateDifferentNumbers() {
        // Arrange
        AtomicInteger counter = new AtomicInteger(1);
        
        when(mongoTemplate.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                any(Class.class),
                eq(BillingConstants.DEFAULT_SEQUENCE_COLLECTION)))
            .thenAnswer(invocation -> {
                BillingReferenceNumberGenerator.SequenceDocument doc = new BillingReferenceNumberGenerator.SequenceDocument();
                doc.setSeq(counter.getAndIncrement());
                return doc;
            });
        
        // Act
        String ref1 = generator.generateUniqueReferenceNumber();
        String ref2 = generator.generateUniqueReferenceNumber();
        String ref3 = generator.generateUniqueReferenceNumber();
        
        // Assert
        assertNotEquals(ref1, ref2);
        assertNotEquals(ref2, ref3);
        assertNotEquals(ref1, ref3);
        
        verify(mongoTemplate, times(3)).findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                any(Class.class),
                eq(BillingConstants.DEFAULT_SEQUENCE_COLLECTION));
    }
    
    @Test
    void testGenerateUniqueReferenceNumber_NullSequenceDocument_ReturnsFallbackValue() {
        // Arrange
        when(mongoTemplate.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                any(Class.class),
                eq(BillingConstants.DEFAULT_SEQUENCE_COLLECTION)))
            .thenReturn(null);
        
        // Act
        String referenceNumber = generator.generateUniqueReferenceNumber();
        
        // Assert
        assertNotNull(referenceNumber);
        assertTrue(referenceNumber.startsWith(BillingConstants.BILLING_REF_PREFIX));
        assertTrue(referenceNumber.contains("-00000001")); // Fallback value is 1
    }
    
    @Test
    void testGenerateUniqueReferenceNumber_Format() {
        // Arrange
        BillingReferenceNumberGenerator.SequenceDocument sequenceDoc = new BillingReferenceNumberGenerator.SequenceDocument();
        sequenceDoc.setSeq(42L);
        
        when(mongoTemplate.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                any(Class.class),
                eq(BillingConstants.DEFAULT_SEQUENCE_COLLECTION)))
            .thenReturn(sequenceDoc);
        
        // Act
        String referenceNumber = generator.generateUniqueReferenceNumber();
        
        // Assert
        assertTrue(referenceNumber.matches("BILL-\\d+-00000042"));
    }
    
    @Test
    void testGenerateUniqueReferenceNumber_ThreadSafety() throws InterruptedException {
        // This test simulates concurrent calls to ensure thread safety
        // In real scenario, MongoDB's findAndModify ensures atomicity
        
        // Arrange
        int threadCount = 10;
        int callsPerThread = 10;
        AtomicInteger counter = new AtomicInteger(1);
        Set<String> generatedNumbers = new HashSet<>();
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        when(mongoTemplate.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                any(Class.class),
                eq(BillingConstants.DEFAULT_SEQUENCE_COLLECTION)))
            .thenAnswer(invocation -> {
                BillingReferenceNumberGenerator.SequenceDocument doc = new BillingReferenceNumberGenerator.SequenceDocument();
                doc.setSeq(counter.getAndIncrement());
                return doc;
            });
        
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        
        // Act
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < callsPerThread; j++) {
                        String ref = generator.generateUniqueReferenceNumber();
                        synchronized (generatedNumbers) {
                            generatedNumbers.add(ref);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();
        
        // Assert
        assertEquals(threadCount * callsPerThread, generatedNumbers.size(), 
                "All generated reference numbers should be unique");
    }
}
