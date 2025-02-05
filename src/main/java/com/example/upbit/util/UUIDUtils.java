package com.example.upbit.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class UUIDUtils {
    public static String createUUID(){
        String oiginUUID = UUID.randomUUID().toString();

        byte[]originBytes = oiginUUID.getBytes(StandardCharsets.UTF_8);
        byte[]hashBytes = {};

        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            hashBytes = messageDigest.digest(originBytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(String.format("%02x", hashBytes[i]));
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        System.out.println("ID : " + createUUID());
    }
}
