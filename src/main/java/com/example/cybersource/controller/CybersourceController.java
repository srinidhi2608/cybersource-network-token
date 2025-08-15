package com.example.cybersource.controller;

import com.example.cybersource.service.InstrumentIdentifierService;
import com.example.cybersource.service.PaymentCredentialsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cybersource")
public class CybersourceController {

    @Autowired
    private InstrumentIdentifierService instrumentIdentifierService;

    @Autowired
    private PaymentCredentialsService paymentCredentialsService;

    @PostMapping("/instrument-identifier")
    public String createInstrumentIdentifier(
            @RequestParam String cardNumber,
            @RequestParam String merchantId) throws Exception {
        return instrumentIdentifierService.createInstrumentIdentifier(cardNumber, merchantId);
    }

    @GetMapping("/instrument-identifier/{tokenId}")
    public String getInstrumentIdentifier(
            @PathVariable String tokenId,
            @RequestParam String merchantId) throws Exception {
        return instrumentIdentifierService.getInstrumentIdentifier(tokenId, merchantId);
    }

    @GetMapping("/payment-credentials/{tokenId}")
    public String getPaymentCredentials(
            @PathVariable String tokenId,
            @RequestParam String merchantId) throws Exception {
        return paymentCredentialsService.getPaymentCredentials(tokenId, merchantId);
    }
}