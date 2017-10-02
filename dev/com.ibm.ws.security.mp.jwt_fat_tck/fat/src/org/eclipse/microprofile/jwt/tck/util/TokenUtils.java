/*
 * Copyright (c) 2016-2017 Contributors to the Eclipse Foundation
 *
 *  See the NOTICE file(s) distributed with this work for additional
 *  information regarding copyright ownership.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.eclipse.microprofile.jwt.tck.util;

import static net.minidev.json.parser.JSONParser.DEFAULT_PERMISSIVE_MODE;

import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.jwt.Claims;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

/**
 * Utiltities for generating a JWT for testing
 */
public class TokenUtils {
    private TokenUtils() {}

    /**
     * Utility method to generate a JWT string from a JSON resource file that is signed by the privateKey.pem
     * test resource key.
     *
     * @param jsonResName - name of test resources file
     * @return the JWT string
     * @throws Exception on parse failure
     */
    public static String generateTokenString(String jsonResName) throws Exception {
        return generateTokenString(jsonResName, Collections.emptySet());
    }

    /**
     * Utility method to generate a JWT string from a JSON resource file that is signed by the privateKey.pem
     * test resource key, possibly with invalid fields.
     *
     * @param jsonResName - name of test resources file
     * @param invalidClaims - the set of claims that should be added with invalid values to test failure modes
     * @return the JWT string
     * @throws Exception on parse failure
     */
    public static String generateTokenString(String jsonResName, Set<InvalidClaims> invalidClaims) throws Exception {
        return generateTokenString(jsonResName, invalidClaims, null);
    }

    /**
     * Utility method to generate a JWT string from a JSON resource file that is signed by the privateKey.pem
     * test resource key, possibly with invalid fields.
     *
     * @param jsonResName - name of test resources file
     * @param invalidClaims - the set of claims that should be added with invalid values to test failure modes
     * @param timeClaims - used to return the exp, iat, auth_time claims
     * @return the JWT string
     * @throws Exception on parse failure
     */
    public static String generateTokenString(String jsonResName, Set<InvalidClaims> invalidClaims, Map<String, Long> timeClaims) throws Exception {
        if (invalidClaims == null) {
            invalidClaims = Collections.emptySet();
        }
        InputStream contentIS = TokenUtils.class.getResourceAsStream(jsonResName);
        byte[] tmp = new byte[4096];
        int length = contentIS.read(tmp);
        byte[] content = new byte[length];
        System.arraycopy(tmp, 0, content, 0, length);

        JSONParser parser = new JSONParser(DEFAULT_PERMISSIVE_MODE);
        JSONObject jwtContent = (JSONObject) parser.parse(content);
        // Change the issuer to INVALID_ISSUER for failure testing if requested
        if (invalidClaims.contains(InvalidClaims.ISSUER)) {
            jwtContent.put(Claims.iss.name(), "INVALID_ISSUER");
        }
        long currentTimeInSecs = currentTimeInSecs();
        long exp = currentTimeInSecs + 300;
        jwtContent.put(Claims.iat.name(), currentTimeInSecs);
        jwtContent.put(Claims.auth_time.name(), currentTimeInSecs);
        // If the exp claim is not updated, it will be an old value that should be seen as expired
        if (!invalidClaims.contains(InvalidClaims.EXP)) {
            jwtContent.put(Claims.exp.name(), exp);
        }
        if (timeClaims != null) {
            timeClaims.put(Claims.iat.name(), currentTimeInSecs);
            timeClaims.put(Claims.auth_time.name(), currentTimeInSecs);
            timeClaims.put(Claims.exp.name(), exp);
        }

        PrivateKey pk;
        if (invalidClaims.contains(InvalidClaims.SIGNER)) {
            // Generate a new random private key to sign with to test invalid signatures
            KeyPair keyPair = generateKeyPair(2048);
            pk = keyPair.getPrivate();
        } else {
            // Use the test private key associated with the test public key for a valid signature
            pk = readPrivateKey("/privateKey.pem");
        }

        // Create RSA-signer with the private key
        JWSSigner signer = new RSASSASigner(pk);
        JWTClaimsSet claimsSet = JWTClaimsSet.parse(jwtContent);
        JWSAlgorithm alg = JWSAlgorithm.RS256;
        if (invalidClaims.contains(InvalidClaims.ALG)) {
            alg = JWSAlgorithm.HS256;
            SecureRandom random = new SecureRandom();
            BigInteger secret = BigInteger.probablePrime(256, random);
            signer = new MACSigner(secret.toByteArray());
        }
        JWSHeader jwtHeader = new JWSHeader.Builder(alg)
                        .keyID("/privateKey.pem")
                        .type(JOSEObjectType.JWT)
                        .build();
        SignedJWT signedJWT = new SignedJWT(jwtHeader, claimsSet);
        signedJWT.sign(signer);
        String jwt = signedJWT.serialize();
        return jwt;
    }

    /**
     * Read a PEM encoded private key from the classpath
     * 
     * @param pemResName - key file resource name
     * @return PrivateKey
     * @throws Exception on decode failure
     */
    public static PrivateKey readPrivateKey(String pemResName) throws Exception {
        InputStream contentIS = TokenUtils.class.getResourceAsStream(pemResName);
        byte[] tmp = new byte[4096];
        int length = contentIS.read(tmp);
        PrivateKey privateKey = decodePrivateKey(new String(tmp, 0, length));
        return privateKey;
    }

    /**
     * Read a PEM encoded public key from the classpath
     * 
     * @param pemResName - key file resource name
     * @return PublicKey
     * @throws Exception on decode failure
     */
    public static PublicKey readPublicKey(String pemResName) throws Exception {
        InputStream contentIS = TokenUtils.class.getResourceAsStream(pemResName);
        byte[] tmp = new byte[4096];
        int length = contentIS.read(tmp);
        PublicKey publicKey = decodePublicKey(new String(tmp, 0, length));
        return publicKey;
    }

    /**
     * Generate a new RSA keypair.
     * 
     * @param keySize - the size of the key
     * @return KeyPair
     * @throws NoSuchAlgorithmException on failure to load RSA key generator
     */
    public static KeyPair generateKeyPair(int keySize) throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(keySize);
        KeyPair keyPair = keyPairGenerator.genKeyPair();
        return keyPair;
    }

    /**
     * Decode a PEM encoded private key string to an RSA PrivateKey
     * 
     * @param pemEncoded - PEM string for private key
     * @return PrivateKey
     * @throws Exception on decode failure
     */
    public static PrivateKey decodePrivateKey(String pemEncoded) throws Exception {
        pemEncoded = removeBeginEnd(pemEncoded);
        byte[] pkcs8EncodedBytes = Base64.getDecoder().decode(pemEncoded);

        // extract the private key

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey privKey = kf.generatePrivate(keySpec);
        return privKey;
    }

    /**
     * Decode a PEM encoded public key string to an RSA PublicKey
     * 
     * @param pemEncoded - PEM string for private key
     * @return PublicKey
     * @throws Exception on decode failure
     */
    public static PublicKey decodePublicKey(String pemEncoded) throws Exception {
        pemEncoded = removeBeginEnd(pemEncoded);
        byte[] encodedBytes = Base64.getDecoder().decode(pemEncoded);

        X509EncodedKeySpec spec = new X509EncodedKeySpec(encodedBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    private static String removeBeginEnd(String pem) {
        pem = pem.replaceAll("-----BEGIN (.*)-----", "");
        pem = pem.replaceAll("-----END (.*)----", "");
        pem = pem.replaceAll("\r\n", "");
        pem = pem.replaceAll("\n", "");
        return pem.trim();
    }

    /**
     * @return the current time in seconds since epoch
     */
    private static int currentTimeInSecs() {
        long currentTimeMS = System.currentTimeMillis();
        int currentTimeSec = (int) (currentTimeMS / 1000);
        return currentTimeSec;
    }

    /**
     * Enums to indicate which claims should be set to invalid values for testing failure modes
     */
    public enum InvalidClaims {
        ISSUER, // Set an invalid issuer
        EXP, // Set an invalid expiration
        SIGNER, // Sign the token with the incorrect private key
        ALG, // Sign the token with the correct private key, but HS
    }
}
