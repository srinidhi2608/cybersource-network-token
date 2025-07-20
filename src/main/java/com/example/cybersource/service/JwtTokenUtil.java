package com.example.cybersource.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.interfaces.RSAPrivateKey;
import java.util.Date;

@Component
public class JwtTokenUtil {

    @Autowired
    private com.example.cybersource.util.KeyLoader keyLoader;

    // You can inject this path via config
    private final String privateKeyPath = "/path/to/your/private_key.pem";

    public String generateJwt(String merchantId, String apiKey, String kid, String resourcePath, String httpMethod) throws Exception {
        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + (5 * 60 * 1000);

        RSAPrivateKey privateKey = keyLoader.loadPrivateKey(java.nio.file.Paths.get(privateKeyPath));
        Algorithm algorithm = Algorithm.RSA256(null, privateKey);

        return JWT.create()
                .withKeyId(kid) // 'kid' header, required by Cybersource
                .withIssuer(merchantId)
                .withSubject(apiKey)
                .withIssuedAt(new Date(nowMillis))
                .withExpiresAt(new Date(expMillis))
                .withClaim("resource", resourcePath)
                .withClaim("method", httpMethod)
                .sign(algorithm);
    }
}