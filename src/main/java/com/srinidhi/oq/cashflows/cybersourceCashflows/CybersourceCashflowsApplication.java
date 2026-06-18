package com.srinidhi.oq.cashflows.cybersourceCashflows;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
		"com.srinidhi.oq.cashflows.cybersourceCashflows",
		"com.example.cybersource"
})
public class CybersourceCashflowsApplication {

	public static void main(String[] args) {
		SpringApplication.run(CybersourceCashflowsApplication.class, args);
	}

}
