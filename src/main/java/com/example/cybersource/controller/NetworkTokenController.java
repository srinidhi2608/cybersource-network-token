package com.example.cybersource.controller;

import com.example.cybersource.service.NetworkTokenService;
import com.example.cybersource.service.NetworkTokenService.NetworkTokenResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/network-token")
public class NetworkTokenController {

    @Autowired
    private NetworkTokenService networkTokenService;

    @PostMapping
    public NetworkTokenResult getNetworkToken(@RequestParam String cardNumber) throws Exception {
        return networkTokenService.generateNetworkTokenAndCryptogram(cardNumber);
    }
}