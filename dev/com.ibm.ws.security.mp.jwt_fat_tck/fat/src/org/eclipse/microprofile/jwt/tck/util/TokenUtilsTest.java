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
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import org.junit.Assert;
import org.junit.Test;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
//import static org.eclipse.microprofile.jwt.tck.TCKConstants.TEST_GROUP_UTILS;

/**
 * Validation of the TokenUtils methods
 */
public class TokenUtilsTest {
    /**
     * Verify the underlying JSONParser used by TokenUtils
     * 
     * @throws Exception
     */
    @Test
    public void testParseRolesEndpoint() throws Exception {
        JSONParser parser = new JSONParser(DEFAULT_PERMISSIVE_MODE);
        InputStream contentIS = TokenUtils.class.getResourceAsStream("/Token1.json");
        byte[] tmp = new byte[4096];
        int length = contentIS.read(tmp);
        byte[] content = new byte[length];
        System.arraycopy(tmp, 0, content, 0, length);
        JSONObject obj = (JSONObject) parser.parse(content);
        System.out.println(obj);

    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void testRolesEndpointToJWTString() throws Exception {
        // Transform the JSON content into a signed JWT
        String jwt = TokenUtils.generateTokenString("/Token1.json");
        System.out.println(jwt);
        /*
         * Note that if you try to validate this token string via jwt.io debugger, you need to take the
         * /publicKey.pem contents, and use
         * -----BEGIN PUBLIC KEY-----
         * ...
         * -----END PUBLIC KEY-----
         * 
         * rather than the:
         * -----BEGIN RSA PUBLIC KEY-----
         * ...
         * -----END RSA PUBLIC KEY-----
         * 
         * in the file.
         */

        // Validate the string via Nimbus
        SignedJWT signedJWT = SignedJWT.parse(jwt);
        PublicKey publicKey = TokenUtils.readPublicKey("/publicKey.pem");
        Assert.assertTrue("publicKey isa RSAPublicKey", publicKey instanceof RSAPublicKey);
        JWSVerifier verifier = new RSASSAVerifier((RSAPublicKey) publicKey);
        Assert.assertTrue(signedJWT.verify(verifier));
    }

    @Test
    //description = "Used to generate initial key testing pair")
    public void testKeyPairGeneration() throws Exception {
        KeyPair keyPair = TokenUtils.generateKeyPair(2048);
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        // extract the encoded private key, this is an unencrypted PKCS#8 private key
        byte[] privateKeyEnc = privateKey.getEncoded();
        byte[] privateKeyPem = Base64.getEncoder().encode(privateKeyEnc);
        String privateKeyPemStr = new String(privateKeyPem);
        System.out.println("-----BEGIN RSA PRIVATE KEY-----");
        int column = 0;
        for (int n = 0; n < privateKeyPemStr.length(); n++) {
            System.out.print(privateKeyPemStr.charAt(n));
            column++;
            if (column == 64) {
                System.out.println();
                column = 0;
            }
        }
        System.out.println("\n-----END RSA PRIVATE KEY-----");

        byte[] publicKeyEnc = publicKey.getEncoded();
        byte[] publicKeyPem = Base64.getEncoder().encode(publicKeyEnc);
        String publicKeyPemStr = new String(publicKeyPem);
        System.out.println("-----BEGIN RSA PUBLIC KEY-----");
        column = 0;
        for (int n = 0; n < publicKeyPemStr.length(); n++) {
            System.out.print(publicKeyPemStr.charAt(n));
            column++;
            if (column == 64) {
                System.out.println();
                column = 0;
            }
        }
        System.out.println("\n-----END RSA PUBLIC KEY-----");
    }

    @Test
    //description = "Test initial key validation")
    public void testReadPrivateKey() throws Exception {
        PrivateKey privateKey = TokenUtils.readPrivateKey("/privateKey.pem");
        System.out.println(privateKey);
    }

    @Test
    //description = "Test initial key validation")
    public void testReadPublicKey() throws Exception {
        RSAPublicKey publicKey = (RSAPublicKey) TokenUtils.readPublicKey("/publicKey.pem");
        System.out.println(publicKey);
        System.out.printf("RSAPublicKey.bitLength: %s\n", publicKey.getModulus().bitLength());
    }
}
