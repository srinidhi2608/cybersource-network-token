package com.srinidhi.oq.cashflows.cybersourceCashflows;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Requires MongoDB instance - enabled for integration testing only")
class CybersourceCashflowsApplicationTests {

	@Test
	void contextLoads() {
		// Basic context load test - security and filters should be loaded
		// This test requires MongoDB to be running
	}

}
