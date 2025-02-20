package com.example.upbit.util;

import java.math.BigInteger;
import java.security.Key;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.util.StringUtils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JWTUtil {
    
    //================================
    // When API required 
    // GET requeest with query string 
    // or POST request with request body
    // , REST API should set query or request data maked Hash argorithm in JWT Token
    //================================
    public static String makeToken(final String accessKey, final String secretKey, final String queryString){
       
        // 1. Make Hash Key
        Key key = Keys.hmacShaKeyFor(secretKey.getBytes());

        // 2. Set JWT Header
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "HS256");
        headers.put("typ", "JWT");

        // 3. Set Payload JWT
        Map<String, Object> claims = new HashMap<>();
        claims.put("access_key", accessKey);
        claims.put("nonce", UUID.randomUUID().toString());

        if(StringUtils.hasText(queryString)){
            claims.put("query_hash", generateQueryHash(queryString));
            claims.put("query_hash_alg", "SHA512");
        }
        
        // 4. Set Jwts Token
        final String token = Jwts.builder()
                    .setHeader(headers)
                    .setClaims(claims)
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();

        log.info("JWT Token : {}", token);

        return token;
    }

    // Set Hashed query
    private static String generateQueryHash(String query) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(query.getBytes("UTF-8"));
            String queryHash = String.format("%0128x", new BigInteger(1, md.digest()));
            return queryHash;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate query hash", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
