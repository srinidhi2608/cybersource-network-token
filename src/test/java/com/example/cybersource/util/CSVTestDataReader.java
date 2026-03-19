package com.example.cybersource.util;

import com.example.cybersource.dto.BillingAuditSyncRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Utility class to read test data from CSV files for controller testing.
 * Supports reading both request and response test data.
 */
public class CSVTestDataReader {
    
    private static final Logger logger = LoggerFactory.getLogger(CSVTestDataReader.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    
    /**
     * Represents a test scenario with request and expected response data.
     */
    public static class TestScenario {
        private String scenarioName;
        private BillingAuditSyncRequest request;
        private Map<String, String> expectedResponse;
        private String description;
        
        public TestScenario(String scenarioName, BillingAuditSyncRequest request, 
                           Map<String, String> expectedResponse, String description) {
            this.scenarioName = scenarioName;
            this.request = request;
            this.expectedResponse = expectedResponse;
            this.description = description;
        }
        
        public String getScenarioName() { return scenarioName; }
        public BillingAuditSyncRequest getRequest() { return request; }
        public Map<String, String> getExpectedResponse() { return expectedResponse; }
        public String getDescription() { return description; }
        
        public int getExpectedStatus() {
            return Integer.parseInt(expectedResponse.getOrDefault("expectedStatus", "200"));
        }
        
        public boolean isSuccess() {
            return Boolean.parseBoolean(expectedResponse.getOrDefault("success", "true"));
        }
        
        public boolean hasError() {
            return Boolean.parseBoolean(expectedResponse.getOrDefault("hasError", "false"));
        }
        
        public long getTotalRecordsCreated() {
            return Long.parseLong(expectedResponse.getOrDefault("totalRecordsCreated", "0"));
        }
    }
    
    /**
     * Reads test scenarios from CSV files.
     * Combines request and response CSV data into TestScenario objects.
     *
     * @param requestCsvPath path to the request CSV file
     * @param responseCsvPath path to the response CSV file
     * @return list of test scenarios
     */
    public static List<TestScenario> readTestScenarios(String requestCsvPath, String responseCsvPath) {
        Map<String, BillingAuditSyncRequest> requests = readRequests(requestCsvPath);
        Map<String, RequestMetadata> requestMetadata = readRequestMetadata(requestCsvPath);
        Map<String, Map<String, String>> responses = readResponses(responseCsvPath);
        
        List<TestScenario> scenarios = new ArrayList<>();
        
        for (String scenarioName : requests.keySet()) {
            BillingAuditSyncRequest request = requests.get(scenarioName);
            Map<String, String> response = responses.get(scenarioName);
            RequestMetadata metadata = requestMetadata.get(scenarioName);
            
            if (response != null) {
                scenarios.add(new TestScenario(
                    scenarioName, 
                    request, 
                    response, 
                    metadata != null ? metadata.description : ""
                ));
            } else {
                logger.warn("No response data found for scenario: {}", scenarioName);
            }
        }
        
        return scenarios;
    }
    
    /**
     * Helper class to store request metadata.
     */
    private static class RequestMetadata {
        String description;
        
        RequestMetadata(String description) {
            this.description = description;
        }
    }
    
    /**
     * Reads request data from CSV file.
     */
    private static Map<String, BillingAuditSyncRequest> readRequests(String csvPath) {
        Map<String, BillingAuditSyncRequest> requests = new LinkedHashMap<>();
        
        try (InputStream is = CSVTestDataReader.class.getClassLoader().getResourceAsStream(csvPath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            
            String headerLine = reader.readLine();
            if (headerLine == null) {
                logger.error("Empty CSV file: {}", csvPath);
                return requests;
            }
            
            String[] headers = headerLine.split(",");
            Map<String, Integer> columnIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                columnIndex.put(headers[i].trim(), i);
            }
            
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = splitCSVLine(line);
                
                if (values.length > 0 && !values[0].trim().isEmpty()) {
                    String scenario = values[columnIndex.get("testScenario")].trim();
                    
                    BillingAuditSyncRequest request = BillingAuditSyncRequest.builder()
                        .startTime(parseDateTime(values, columnIndex, "startTime"))
                        .endTime(parseDateTime(values, columnIndex, "endTime"))
                        .forceSync(parseBoolean(values, columnIndex, "forceSync"))
                        .batchSize(parseInteger(values, columnIndex, "batchSize"))
                        .build();
                    
                    requests.put(scenario, request);
                }
            }
            
        } catch (IOException e) {
            logger.error("Error reading request CSV: {}", csvPath, e);
        }
        
        return requests;
    }
    
    /**
     * Reads request metadata from CSV file.
     */
    private static Map<String, RequestMetadata> readRequestMetadata(String csvPath) {
        Map<String, RequestMetadata> metadata = new LinkedHashMap<>();
        
        try (InputStream is = CSVTestDataReader.class.getClassLoader().getResourceAsStream(csvPath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            
            String headerLine = reader.readLine();
            if (headerLine == null) return metadata;
            
            String[] headers = headerLine.split(",");
            Map<String, Integer> columnIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                columnIndex.put(headers[i].trim(), i);
            }
            
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = splitCSVLine(line);
                
                if (values.length > 0 && !values[0].trim().isEmpty()) {
                    String scenario = values[columnIndex.get("testScenario")].trim();
                    String description = getString(values, columnIndex, "description");
                    
                    metadata.put(scenario, new RequestMetadata(description));
                }
            }
            
        } catch (IOException e) {
            logger.error("Error reading request metadata CSV: {}", csvPath, e);
        }
        
        return metadata;
    }
    
    /**
     * Reads response data from CSV file.
     */
    private static Map<String, Map<String, String>> readResponses(String csvPath) {
        Map<String, Map<String, String>> responses = new LinkedHashMap<>();
        
        try (InputStream is = CSVTestDataReader.class.getClassLoader().getResourceAsStream(csvPath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            
            String headerLine = reader.readLine();
            if (headerLine == null) {
                logger.error("Empty CSV file: {}", csvPath);
                return responses;
            }
            
            String[] headers = headerLine.split(",");
            
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = splitCSVLine(line);
                
                if (values.length > 0 && !values[0].trim().isEmpty()) {
                    String scenario = values[0].trim();
                    Map<String, String> responseData = new HashMap<>();
                    
                    for (int i = 0; i < Math.min(headers.length, values.length); i++) {
                        responseData.put(headers[i].trim(), values[i].trim());
                    }
                    
                    responses.put(scenario, responseData);
                }
            }
            
        } catch (IOException e) {
            logger.error("Error reading response CSV: {}", csvPath, e);
        }
        
        return responses;
    }
    
    /**
     * Splits CSV line handling quoted values.
     */
    private static String[] splitCSVLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        
        return result.toArray(new String[0]);
    }
    
    /**
     * Parses LocalDateTime from CSV value.
     */
    private static LocalDateTime parseDateTime(String[] values, Map<String, Integer> columnIndex, String columnName) {
        Integer index = columnIndex.get(columnName);
        if (index != null && index < values.length) {
            String value = values[index].trim();
            if (!value.isEmpty()) {
                try {
                    return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
                } catch (Exception e) {
                    logger.warn("Failed to parse datetime: {}", value);
                }
            }
        }
        return null;
    }
    
    /**
     * Parses Boolean from CSV value.
     */
    private static Boolean parseBoolean(String[] values, Map<String, Integer> columnIndex, String columnName) {
        Integer index = columnIndex.get(columnName);
        if (index != null && index < values.length) {
            String value = values[index].trim();
            if (!value.isEmpty()) {
                return Boolean.parseBoolean(value);
            }
        }
        return null;
    }
    
    /**
     * Parses Integer from CSV value.
     */
    private static Integer parseInteger(String[] values, Map<String, Integer> columnIndex, String columnName) {
        Integer index = columnIndex.get(columnName);
        if (index != null && index < values.length) {
            String value = values[index].trim();
            if (!value.isEmpty()) {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    logger.warn("Failed to parse integer: {}", value);
                }
            }
        }
        return null;
    }
    
    /**
     * Gets String from CSV value.
     */
    private static String getString(String[] values, Map<String, Integer> columnIndex, String columnName) {
        Integer index = columnIndex.get(columnName);
        if (index != null && index < values.length) {
            return values[index].trim();
        }
        return "";
    }
}
