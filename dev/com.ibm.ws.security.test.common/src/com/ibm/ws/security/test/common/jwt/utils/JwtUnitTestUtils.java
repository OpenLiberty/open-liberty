package com.ibm.ws.security.test.common.jwt.utils;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.ibm.json.java.JSONObject;

public class JwtUnitTestUtils {

    public JSONObject getJwsHeader(String alg) {
        JSONObject header = new JSONObject();
        header.put("typ", "JWT");
        header.put("alg", alg);
        return header;
    }

    public JSONObject getHS256Header() {
        return getJwsHeader("HS256");
    }

    public String getHS256Jws(JSONObject claims, String secret) throws Exception {
        String headerAndPayload = encode(getHS256Header()) + "." + encode(claims);
        String signature = getHS256Signature(headerAndPayload, secret);
        return headerAndPayload + "." + signature;
    }

    public String getHS256Signature(String input, String secret) throws Exception {
        byte[] secretBytes = secret.getBytes("UTF-8");
        Mac hs256Mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secretBytes, "HmacSHA256");
        hs256Mac.init(keySpec);
        byte[] hashBytes = hs256Mac.doFinal(input.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(hashBytes);
    }

    public String encode(Object input) throws UnsupportedEncodingException {
        return Base64.getEncoder().encodeToString(input.toString().getBytes("UTF-8"));
    }

}
