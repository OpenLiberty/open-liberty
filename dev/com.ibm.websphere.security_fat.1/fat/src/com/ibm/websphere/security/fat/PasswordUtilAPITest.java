/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.security.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class PasswordUtilAPITest {

    private final Class<?> thisClass = PasswordUtilAPITest.class;
    protected DefaultHttpClient client = new DefaultHttpClient();

    @Rule
    public TestName name = new TestName();

    /*
     * test PasswordUtil class is not visible from the application when
     * passwordUtilities-1.0 feature does not exist. (the negative test is done by PublicAPITestSecurityDisabled class
     */
    @Test
    public void testPasswordUtilApiDisabled() throws Exception {
        String method = name.getMethodName();
        Log.info(thisClass, method, "Entering test " + method);
        LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.websphere.security.fat.passwordutil");
        SecurityFatUtils.transformApps(myServer, "PasswordUtilAPI.war");

        try {
            myServer.startServer(true);
            String urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort() + "/PasswordUtilAPI";
            String className = "PasswordUtil";
            String encodeInput = "&input=sensitiveText";
            String decodeInput = "&input=%7bxor%7dLDoxLDYrNik6CzonKw%3d%3d";
            String expectedOutput = "NoClassDefFoundError: ";

            invokeServlet(urlBase, className, "encode", encodeInput, 200, expectedOutput);
            invokeServlet(urlBase, className, "decode", decodeInput, 200, expectedOutput);
        } finally {
            myServer.stopServer();
        }
        Log.info(thisClass, method, "Exiting test " + method);
    }

    /*
     * test PasswordUtil class is visible from the application when
     * passwordUtilities-1.0 feature exists. (the negative test is done by PublicAPITestSecurityDisabled class
     */
    @Test
    public void testPasswordUtilApiEnabled_passwordUtilities10() throws Exception {
        String method = name.getMethodName();
        Log.info(thisClass, method, "Entering test " + method);
        // stop server, and then update the feature.
        LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.websphere.security.fat.passwordutil.api");
        SecurityFatUtils.transformApps(myServer, "PasswordUtilAPI.war");

        try {
            myServer.startServer(true);
            String urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort() + "/PasswordUtilAPI";
            String className = "PasswordUtil";
            String encodeInput = "&input=sensitiveText";
            String decodeInput = "&input=%7bxor%7dLDoxLDYrNik6CzonKw%3d%3d";
            String expectedEncodeOutput = "encode output is: {xor}LDoxLDYrNik6CzonKw==";
            String expectedDecodeOutput = "decode output is: sensitiveText";

            invokeServlet(urlBase, className, "encode", encodeInput, 200, expectedEncodeOutput);
            invokeServlet(urlBase, className, "decode", decodeInput, 200, expectedDecodeOutput);

            runAesScenarios(urlBase, className);

            /*
             * The feature passwordUtilties-1.0 starts federatedRegistry-1.0, except when using EE9 since then
             * appSecurity-1.0 (which brings in federatedRegistry-1.0) is no longer compatible.
             */
            if (!JakartaEEAction.isEE9OrLaterActive()) {
                assertFalse("Federated registry feature (federatedRegistry-1.0) should have started.", myServer.findStringsInLogs("federatedRegistry-1.0").isEmpty());
            } else {
                assertTrue("Federated registry feature (federatedRegistry-1.0) should not have started.", myServer.findStringsInLogs("federatedRegistry-1.0").isEmpty());
            }
        } finally {
            // CWWKS1864W: expected warning when decoding AES-128 (aes_v0) passwords
            // CWWKS1856E: expected errors from negative-test decode failures (key mismatch scenarios)
            myServer.stopServer("CWWKS1864W", "CWWKS1856E");
        }

        Log.info(thisClass, method, "Exiting test " + method);

    }

    /*
     * test PasswordUtil class is visible from the application when
     * passwordUtilities-1.1 feature exists. (the negative test is done by PublicAPITestSecurityDisabled class
     */
    @Test
    public void testPasswordUtilApiEnabled_passwordUtilities11() throws Exception {
        String method = name.getMethodName();
        Log.info(thisClass, method, "Entering test " + method);
        // stop server, and then update the feature.
        LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.websphere.security.fat.passwordutil.api");
        ServerConfiguration config = myServer.getServerConfiguration().clone();
        config.getFeatureManager().getFeatures().remove("passwordUtilities-1.0");
        config.getFeatureManager().getFeatures().remove("passwordutilities-1.0"); // JakartaEE9Action lower-cases feature names
        config.getFeatureManager().getFeatures().add("passwordUtilities-1.1");
        myServer.updateServerConfiguration(config);
        SecurityFatUtils.transformApps(myServer, "PasswordUtilAPI.war");

        try {
            myServer.startServer(true);
            String urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort() + "/PasswordUtilAPI";
            String className = "PasswordUtil";
            String encodeInput = "&input=sensitiveText";
            String decodeInput = "&input=%7bxor%7dLDoxLDYrNik6CzonKw%3d%3d";
            String expectedEncodeOutput = "encode output is: {xor}LDoxLDYrNik6CzonKw==";
            String expectedDecodeOutput = "decode output is: sensitiveText";

            invokeServlet(urlBase, className, "encode", encodeInput, 200, expectedEncodeOutput);
            invokeServlet(urlBase, className, "decode", decodeInput, 200, expectedDecodeOutput);

            runAesScenarios(urlBase, className);

            /*
             * The feature passwordUtilties-1.1 does not start federatedRegistry-1.0.
             */
            assertTrue("Federated registry feature (federatedRegistry-1.0) should not have started.", myServer.findStringsInLogs("federatedRegistry-1.0").isEmpty());
        } finally {
            // CWWKS1864W: expected warning when decoding AES-128 (aes_v0) passwords
            // CWWKS1856E: expected errors from negative-test decode failures (key mismatch scenarios)
            myServer.stopServer("CWWKS1864W", "CWWKS1856E");
        }

        Log.info(thisClass, method, "Exiting test " + method);

    }

    private void runAesScenarios(String urlBase, String className) throws Exception {
        String plaintext = "LibertyAesPassword!456";

        // Scenario 1: AES_V0 (AES-128 Default Key)
        String respV0 = invokeServlet(urlBase, className, "encode", "aesVersion=v0&input=" + plaintext, 200, "encode output is: {aes}");
        String encV0 = extractCiphertext(respV0);
        invokeServlet(urlBase, className, "decode", "input=" + URLEncoder.encode(encV0, StandardCharsets.UTF_8.name()), 200, "decode output is: " + plaintext);

        // Scenario 2: AES_V0 (AES-128 Mismatch Custom Key -> decode fails with InvalidPasswordDecodingException)
        // The encode uses a custom cryptoKey not known to the server, so decode is expected to fail.
        Log.info(thisClass, name.getMethodName(), "Scenario 2: Expecting decode failure (InvalidPasswordDecodingException) - encoded with a custom key not present in server config.");
        String respV0Custom = invokeServlet(urlBase, className, "encode", "aesVersion=v0&cryptoKey=MismatchSecretKey123&input=" + plaintext, 200, "encode output is: {aes}");
        String encV0Custom = extractCiphertext(respV0Custom);
        invokeServlet(urlBase, className, "decode", "input=" + URLEncoder.encode(encV0Custom, StandardCharsets.UTF_8.name()), 200, "Unexpected Exception during processing: aes");

        // Scenario 3: AES_V1 (AES-256 Server wlp.password.encryption.key)
        String respV1 = invokeServlet(urlBase, className, "encode", "aesVersion=v1&input=" + plaintext, 200, "encode output is: {aes}");
        String encV1 = extractCiphertext(respV1);
        invokeServlet(urlBase, className, "decode", "input=" + URLEncoder.encode(encV1, StandardCharsets.UTF_8.name()), 200, "decode output is: " + plaintext);

        // Scenario 4: AES_V1 (AES-256 Mismatch Custom Key -> decode fails with InvalidPasswordDecodingException)
        // The encode uses a custom cryptoKey not known to the server, so decode is expected to fail.
        Log.info(thisClass, name.getMethodName(), "Scenario 4: Expecting decode failure (InvalidPasswordDecodingException) - encoded with a custom key not present in server config.");
        String respV1Custom = invokeServlet(urlBase, className, "encode", "aesVersion=v1&cryptoKey=MismatchSecretKey123&input=" + plaintext, 200, "encode output is: {aes}");
        String encV1Custom = extractCiphertext(respV1Custom);
        invokeServlet(urlBase, className, "decode", "input=" + URLEncoder.encode(encV1Custom, StandardCharsets.UTF_8.name()), 200, "Unexpected Exception during processing: aes");

        // Scenario 5: AES_V2 (AES-256 Server wlp.aes.encryption.key)
        String respV2 = invokeServlet(urlBase, className, "encode", "aesVersion=v2&input=" + plaintext, 200, "encode output is: {aes}");
        String encV2 = extractCiphertext(respV2);
        invokeServlet(urlBase, className, "decode", "input=" + URLEncoder.encode(encV2, StandardCharsets.UTF_8.name()), 200, "decode output is: " + plaintext);

        // Scenario 6: AES_V2 (AES-256 Matching Base64 Key)
        String serverAesKey = "prnwy497AdQ8Mr7BFlfxsEz8xOxowwC5sjBvnNXwoaM="; // pragma: allowlist secret
        String respV2Explicit = invokeServlet(urlBase, className, "encode", "aesVersion=v2&aesKey=" + URLEncoder.encode(serverAesKey, StandardCharsets.UTF_8.name()) + "&input=" + plaintext, 200, "encode output is: {aes}");
        String encV2Explicit = extractCiphertext(respV2Explicit);
        invokeServlet(urlBase, className, "decode", "input=" + URLEncoder.encode(encV2Explicit, StandardCharsets.UTF_8.name()), 200, "decode output is: " + plaintext);

        // Scenario 7: AES_V2 (AES-256 Mismatching Base64 Key -> decode fails with InvalidPasswordDecodingException)
        // The encode uses an aesKey that does not match the server's wlp.aes.encryption.key, so decode is expected to fail.
        Log.info(thisClass, name.getMethodName(), "Scenario 7: Expecting decode failure (InvalidPasswordDecodingException) - encoded with an aesKey not matching the server's wlp.aes.encryption.key.");
        String mismatchAesKey = "MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=";
        String respV2Mismatch = invokeServlet(urlBase, className, "encode", "aesVersion=v2&aesKey=" + URLEncoder.encode(mismatchAesKey, StandardCharsets.UTF_8.name()) + "&input=" + plaintext, 200, "encode output is: {aes}");
        String encV2Mismatch = extractCiphertext(respV2Mismatch);
        invokeServlet(urlBase, className, "decode", "input=" + URLEncoder.encode(encV2Mismatch, StandardCharsets.UTF_8.name()), 200, "Unexpected Exception during processing: aes");

        // Scenario 8: Generic algorithm=aes (AES-256 V1 by default)
        String respAlgoAes = invokeServlet(urlBase, className, "encode", "algorithm=aes&input=" + plaintext, 200, "encode output is: {aes}");
        String encAlgoAes = extractCiphertext(respAlgoAes);
        invokeServlet(urlBase, className, "decode", "input=" + URLEncoder.encode(encAlgoAes, StandardCharsets.UTF_8.name()), 200, "decode output is: " + plaintext);
    }

    private String extractCiphertext(String response) {
        String prefix = "encode output is: ";
        int idx = response.indexOf(prefix);
        if (idx >= 0) {
            String sub = response.substring(idx + prefix.length()).trim();
            int newlineIdx = sub.indexOf('\n');
            if (newlineIdx >= 0) {
                return sub.substring(0, newlineIdx).trim();
            }
            return sub;
        }
        return response.trim();
    }

    protected String invokeServlet(String urlBase, String className, String methodName, String parms,
                                   int expectedStatusCode, String expectedResponseText) {
        String method = name.getMethodName();
        String url = urlBase + "?class=" + className + "&method=" + methodName;
        if (parms != null && !parms.isEmpty())
            url = url + "&" + parms;
        String response = invokeServlet(url, expectedStatusCode);
        Log.info(thisClass, method, "response = " + response);
        assertNotNull("response was null", response);
        assertTrue("Expected: " + expectedResponseText + ", in response text: " + response,
                   response.indexOf(expectedResponseText) >= 0);
        return response;
    }

    protected String invokeServlet(String url, int expectedStatusCode) {
        String method = name.getMethodName();
        String content = null;
        Log.info(thisClass, method, "url=" + url + " expectedStatusCode=" + expectedStatusCode);
        try {
            HttpGet getMethod = new HttpGet(url);
            HttpResponse response = client.execute(getMethod);
            Log.info(thisClass, method, "response = " + response.toString());
            HttpEntity entity = response.getEntity();
            content = EntityUtils.toString(entity);
            Log.info(thisClass, method, "Servlet response: " + content);
            EntityUtils.consume(entity);

            assertEquals("Expected status code: " + expectedStatusCode + ", received: " + response.getStatusLine().getStatusCode(),
                         expectedStatusCode, response.getStatusLine().getStatusCode());
            return content;
        } catch (Exception e) {
            Log.info(thisClass, method, "Caught unexpected exception: " + e);
            return null;
        }
    }

}