/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.fat.builder.validation;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.commons.codec.binary.Base64;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwx.JsonWebStructure;

import com.gargoylesoftware.htmlunit.Page;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.jwt.JwtTokenForTest;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.fat.common.web.WebResponseUtils;
import com.ibm.ws.security.jwt.fat.builder.JWTBuilderConstants;
import com.ibm.ws.security.jwt.fat.builder.utils.BuilderHelpers;

public class BuilderTestValidationUtils extends TestValidationUtils {

    protected static Class<?> thisClass = BuilderTestValidationUtils.class;

    public void validateSignatureSize(Page endpointOutput, int keySize) throws Exception {

        String thisMethod = "validateSignatureSize";
        loggingUtils.printMethodName(thisMethod, "Start of");

        String jwtToken = WebResponseUtils.getResponseText(endpointOutput);

        Log.info(thisClass, thisMethod, "response value: " + jwtToken);
        JsonObject val = Json.createReader(new StringReader(jwtToken)).readObject();

        String keys = val.get("keys").toString();
        Log.info(thisClass, thisMethod, "keys: " + keys);

        JsonArray keyArrays = val.getJsonArray("keys");
        // We are now generating two jwks again by default like before and should get the latest element
        int cnt = 0;
        for (Object o : keyArrays) {
            JsonObject jsonLineItem = (JsonObject) o;
            if (keyArrays.size() > 1 && cnt < 1) {
                cnt++;
                continue;
            }

            String n = jsonLineItem.getString("n");
            if (n == null) {
                fail("Size of the signature checking failed as the signature (\"n\" value) was missing");
            }
            Log.info(thisClass, thisMethod, "n: " + n);
            String decoded_n = new String(Base64.decodeBase64(n));
            Log.info(thisClass, thisMethod, "raw size of n: " + decoded_n.length());
            int calculatedSize = decoded_n.length() * 8;
            Log.info(thisClass, thisMethod, "Comparing expected size of signature (" + keySize + ") against size (" + calculatedSize + ") calculated from the token");
            assertTrue("The size of the signature in the token (" + calculatedSize + ") did NOT match the expected size (" + keySize + ")", calculatedSize == keySize);

        }

    }

    public void validateCurve(Page endpointOutput, String curve) throws Exception {

        String thisMethod = "validateCurve";
        loggingUtils.printMethodName(thisMethod, "Start of");

        String jwtToken = WebResponseUtils.getResponseText(endpointOutput);

        Log.info(thisClass, thisMethod, "response value: " + jwtToken);
        JsonObject val = Json.createReader(new StringReader(jwtToken)).readObject();

        String keys = val.get("keys").toString();
        Log.info(thisClass, thisMethod, "keys: " + keys);

        JsonArray keyArrays = val.getJsonArray("keys");
        // We are now generating two jwks again by default like before and should get the latest element
        int cnt = 0;
        for (Object o : keyArrays) {
            JsonObject jsonLineItem = (JsonObject) o;
            if (keyArrays.size() > 1 && cnt < 1) {
                cnt++;
                continue;
            }

            String crv = jsonLineItem.getString("crv");
            if (crv == null) {
                fail("Curve checking failed as the curve (\"crv\" value) was missing");
            }
            Log.info(thisClass, thisMethod, "crv: " + crv);
            Log.info(thisClass, thisMethod, "Comparing expected crv (" + curve + ") against actual crv (" + crv + ")");
            assertTrue("The actual crv (" + crv + ") did NOT match the expected curve (" + curve + ")", curve.equals(crv));

        }
    }

    public void validateSigAlg(Page endpointOutput, String sigAlg) throws Exception {

        String thisMethod = "validateSigAlg";
        loggingUtils.printMethodName(thisMethod, "Start of");

        String jwtToken = WebResponseUtils.getResponseText(endpointOutput);

        Log.info(thisClass, thisMethod, "response value: " + jwtToken);
        JsonObject val = Json.createReader(new StringReader(jwtToken)).readObject();

        String keys = val.get("keys").toString();
        Log.info(thisClass, thisMethod, "keys: " + keys);

        JsonArray keyArrays = val.getJsonArray("keys");
        // We are now generating two jwks again by default like before and should get the latest element
        int cnt = 0;
        for (Object o : keyArrays) {
            JsonObject jsonLineItem = (JsonObject) o;
            if (keyArrays.size() > 1 && cnt < 1) {
                cnt++;
                continue;
            }

            String alg = jsonLineItem.getString("alg");
            if (alg == null) {
                fail("Signature Algorithm checking failed as the signature algorithm (\"alg\" value) was missing");
            }
            Log.info(thisClass, thisMethod, "alg: " + alg);
            Log.info(thisClass, thisMethod, "Comparing expected signature algorithm (" + sigAlg + ") against actual alg (" + alg + ")");
            assertTrue("The actual signature algorithm (" + sigAlg + ") did NOT match the expected signature algorithm (" + sigAlg + ")", sigAlg.equals(alg));

        }
    }

    /**
     * Validate that we have a JWE and not a JWS.
     * Validate that we can decrypt the token properly
     *
     * @param response
     *            - the test app response (the jwt token will be found in the response)
     * @param keyMgmtKeyAlg
     *            - the key management key algorithm used to decrypt the token
     * @param privateKey
     *            - the private key used to decrypt the token
     * @param contentEncryptAlg
     *            - the content encryption algorithm used to decrypt the token
     * @return - returns a JwtTokenForTest - which has the JWE Header, JWS Header and payload objects in case the caller needs to
     *         do more validation of the token content
     * @throws Exception
     */
    public JwtTokenForTest validateJWSToken(Page response) throws Exception {

        String thisMethod = "validateJWSToken";

        String jwtTokenString = BuilderHelpers.extractJwtTokenFromResponse(response, JWTBuilderConstants.BUILT_JWT_TOKEN);

        Log.info(thisClass, thisMethod, "");

        if (jwtTokenString == null) {
            fail("Test failed to find the JWT in the builder response");
        }

        JsonWebStructure joseObject = JsonWebStructure.fromCompactSerialization(jwtTokenString);

        // we should have a JWE, not a JWS
        if (joseObject instanceof JsonWebEncryption) {
            fail("Token is encrypted");
        }

        JwtTokenForTest tokenForTest = new JwtTokenForTest(jwtTokenString);
        JsonObject jweHeader = tokenForTest.getJsonJWEHeader();

        if (jweHeader != null) {
            fail("Token was not a proper JWS - JWE Header was populated");
        }

        Log.info(thisClass, thisMethod, "Validation of the JWS was successful");
        return tokenForTest;
    }

    /**
     * Validate that we have a JWE and not a JWS.
     * Validate that we can decrypt the token properly
     *
     * @param response
     *            - the test app response (the jwt token will be found in the response)
     * @param keyMgmtKeyAlg
     *            - the key management key algorithm used to decrypt the token
     * @param privateKey
     *            - the private key used to decrypt the token
     * @param contentEncryptAlg
     *            - the content encryption algorithm used to decrypt the token
     * @return - returns a JwtTokenForTest - which has the JWE Header, JWS Header and payload objects in case the caller needs to
     *         do more validation of the token content
     * @throws Exception
     */
    public JwtTokenForTest validateJWEToken(Page response, String keyMgmtKeyAlg, String privateKey, String contentEncryptAlg) throws Exception {

        String thisMethod = "validateEncryptedToken";

        String jwtTokenString = BuilderHelpers.extractJwtTokenFromResponse(response, JWTBuilderConstants.BUILT_JWT_TOKEN);

        Log.info(thisClass, thisMethod, "keyMgmtAlg: " + keyMgmtKeyAlg + " ContentEncryptionAlg: " + contentEncryptAlg);

        if (jwtTokenString == null) {
            fail("Test failed to find the JWT in the builder response");
        }

        JsonWebStructure joseObject = JsonWebStructure.fromCompactSerialization(jwtTokenString);

        // we should have a JWS, not a JWE
        if (joseObject instanceof JsonWebSignature) {
            fail("Token is NOT encrypted");
        }

        JwtTokenForTest tokenForTest = new JwtTokenForTest(jwtTokenString, keyMgmtKeyAlg, privateKey, contentEncryptAlg);
        JsonObject jweHeader = tokenForTest.getJsonJWEHeader();

        if (jweHeader == null) {
            fail("Token was not a proper JWE - JWE Header was not populated");
        }

        Log.info(thisClass, thisMethod, "Validation of the JWE was successful");
        return tokenForTest;
    }
}