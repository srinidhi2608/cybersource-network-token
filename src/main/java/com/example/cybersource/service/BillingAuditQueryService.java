package com.example.cybersource.service;

import com.example.cybersource.dto.TokenInfo;
import com.example.cybersource.entity.BillingAudit;
import com.example.cybersource.repository.BillingAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for querying billing audit data.
 * Provides methods to retrieve merchant lists and token event information by month and year.
 */
@Service
public class BillingAuditQueryService {
    
    private static final Logger logger = LoggerFactory.getLogger(BillingAuditQueryService.class);
    
    private final BillingAuditRepository billingAuditRepository;
    
    public BillingAuditQueryService(BillingAuditRepository billingAuditRepository) {
        this.billingAuditRepository = billingAuditRepository;
    }
    
    /**
     * Gets a list of unique merchant token registration IDs for a given month and year.
     * Queries BillingAudit collection filtering by eventTimeStamp.
     *
     * @param month the month (1-12)
     * @param year the year (e.g., 2025)
     * @return list of distinct merchant token registration IDs
     * @throws IllegalArgumentException if month or year is invalid
     */
    public List<String> getMerchantListByMonthAndYear(int month, int year) {
        logger.info("Fetching merchant list for month: {}, year: {}", month, year);
        
        validateMonthAndYear(month, year);
        
        DateRange dateRange = getDateRangeForMonthAndYear(month, year);
        
        List<BillingAudit> billingAudits = billingAuditRepository
                .findByEventTimeStampBetweenWithMerchantIdOnly(dateRange.getStartTime(), dateRange.getEndTime());
        
        // Extract distinct merchant IDs
        List<String> merchantIds = billingAudits.stream()
                .map(BillingAudit::getMerchantTokenRegistrationId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        
        logger.info("Found {} unique merchants for month: {}, year: {}", merchantIds.size(), month, year);
        return merchantIds;
    }
    
    /**
     * Gets a list of token events for a specific merchant and month/year.
     * Returns TokenInfo objects with event details.
     *
     * @param merchantTokenRegistrationId the merchant token registration ID
     * @param month the month (1-12)
     * @param year the year (e.g., 2025)
     * @return list of TokenInfo objects containing event details
     * @throws IllegalArgumentException if month or year is invalid
     */
    public List<TokenInfo> getTokenEventsByMerchant(String merchantTokenRegistrationId, int month, int year) {
        logger.info("Fetching token events for merchant: {}, month: {}, year: {}",
                merchantTokenRegistrationId, month, year);
        
        if (merchantTokenRegistrationId == null || merchantTokenRegistrationId.isBlank()) {
            throw new IllegalArgumentException("Merchant token registration ID cannot be null or empty");
        }
        
        validateMonthAndYear(month, year);
        
        DateRange dateRange = getDateRangeForMonthAndYear(month, year);
        
        List<BillingAudit> billingAudits = billingAuditRepository
                .findByMerchantTokenRegistrationIdAndEventTimeStampBetween(
                        merchantTokenRegistrationId,
                        dateRange.getStartTime(),
                        dateRange.getEndTime()
                );
        
        List<TokenInfo> tokenInfoList = billingAudits.stream()
                .map(this::mapToTokenInfo)
                .collect(Collectors.toList());
        
        logger.info("Found {} token events for merchant: {}, month: {}, year: {}",
                tokenInfoList.size(), merchantTokenRegistrationId, month, year);
        
        return tokenInfoList;
    }
    
    /**
     * Maps a BillingAudit entity to a TokenInfo DTO.
     *
     * @param billingAudit the billing audit entity
     * @return TokenInfo DTO
     */
    private TokenInfo mapToTokenInfo(BillingAudit billingAudit) {
        return TokenInfo.builder()
                .traceId(billingAudit.getTraceIdReference())
                .transactionType(billingAudit.getTraceIdEvent())
                .timestamp(billingAudit.getEventTimeStamp())
                .eventStatus(isSuccessEvent(billingAudit.getEventType()))
                .build();
    }
    
    /**
     * Determines if an event type indicates success.
     *
     * @param eventType the event type string
     * @return true if event is successful, false otherwise
     */
    private Boolean isSuccessEvent(String eventType) {
        if (eventType == null) {
            return null;
        }
        return "Success".equalsIgnoreCase(eventType);
    }
    
    /**
     * Validates month and year parameters.
     *
     * @param month the month (1-12)
     * @param year the year
     * @throws IllegalArgumentException if parameters are invalid
     */
    private void validateMonthAndYear(int month, int year) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }
        if (year < 2020 || year > 2100) {
            throw new IllegalArgumentException("Year must be between 2020 and 2100");
        }
    }
    
    /**
     * Calculates the date range for a given month and year.
     * Start time: first day of month at 00:00:00
     * End time: last day of month at 23:59:59.999999999
     *
     * @param month the month (1-12)
     * @param year the year
     * @return DateRange object with start and end times
     */
    private DateRange getDateRangeForMonthAndYear(int month, int year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        
        LocalDateTime startTime = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endTime = yearMonth.atEndOfMonth().atTime(23, 59, 59, 999999999);
        
        return new DateRange(startTime, endTime);
    }
    
    /**
     * Inner class representing a date range.
     */
    private static class DateRange {
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        
        public DateRange(LocalDateTime startTime, LocalDateTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }
        
        public LocalDateTime getStartTime() {
            return startTime;
        }
        
        public LocalDateTime getEndTime() {
            return endTime;
        }
    }
}
