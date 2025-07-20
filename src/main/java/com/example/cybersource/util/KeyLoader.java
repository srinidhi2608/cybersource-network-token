package com.example.cybersource.util;

import java.io.FileReader;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.Security;
import java.security.interfaces.RSAPrivateKey;

import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.stereotype.Component;

@Component
public class KeyLoader {
    public RSAPrivateKey loadPrivateKey(Path pemPath) throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        try (PEMParser pemParser = new PEMParser(new FileReader(pemPath.toFile()))) {
            Object object = pemParser.readObject();
            if (object instanceof PEMKeyPair) {
                return (RSAPrivateKey) new JcaPEMKeyConverter().getPrivateKey(((PEMKeyPair) object).getPrivateKeyInfo());
            } else {
                throw new IllegalArgumentException("Invalid PEM file: no key pair found.");
            }
        }
    }
}