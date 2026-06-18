/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer.servlet61.fat.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Test to populate the EncodingUtils localesCache with more than 2000 unique Accept-Language headers
 * and verify the cache behavior.
 *
 * Also test the Accept-Language length over default 4096 is rejected.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ServiceAcceptLanguageTest {

    private static final Logger LOG = Logger.getLogger(ServiceAcceptLanguageTest.class.getName());
    private static final String TEST_APP_NAME = "AcceptLanguageHeaderTest";
    private static final int NUM_REQUESTS = 2010;

    private static String baseURL = null;

    @Server("servlet61_AcceptLanguageHeaderTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : servlet61_AcceptLanguageHeaderTest");
        ShrinkHelper.defaultDropinApp(server, TEST_APP_NAME + ".war", "acceptlanguage.servlets");
        server.startServer(ServiceAcceptLanguageTest.class.getSimpleName() + ".log");
        LOG.info("Setup : startServer, ready for Tests.");
        baseURL = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME;
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");

        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test that makes 2010 requests with unique Accept-Language headers
     * to populate the localesCache in EncodingUtils.
     *
     * Verify that server-side localesCache is bounded to 2000
     */
    @Test
    public void test_PopulateAndVerifyLocalesCache() throws Exception {
        LOG.info("=== Starting test_PopulateAndVerifyLocalesCache - making " + NUM_REQUESTS + " requests with unique Accept-Language headers");
        int successCount = 0;
        int failureCount = 0;
        String responseText = null;
        String acceptLanguage = null;

        String url = baseURL + "/TestAcceptLanguage";
        HttpGet getMethod = new HttpGet(url);

        getMethod.addHeader("runTest", "test_PopulateAndVerifyLocalesCache");

        List<String> acceptLanguageHeaders = generateUniqueAcceptLanguageHeaders(NUM_REQUESTS);

        LOG.info("===== Sending request [" + url + "]");

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            for (int i = 0; i < acceptLanguageHeaders.size(); i++) {
                acceptLanguage = acceptLanguageHeaders.get(i);

                getMethod.setHeader("Accept-Language", acceptLanguage);

                try (final CloseableHttpResponse response = client.execute(getMethod)) {
                    int statusCode = response.getCode();
                    responseText = EntityUtils.toString(response.getEntity());

                    if (statusCode == 200) {
                        successCount++;
                        //show progress every 200 requests
                        if ((i + 1) % 200 == 0) {
                            LOG.info("Progress: " + (i + 1) + "/" + NUM_REQUESTS + " requests completed");
                        }
                    } else {
                        failureCount++;
                        LOG.warning("Request " + (i + 1) + " failed with status: " + statusCode);
                    }

                    // Log first and last requests for verification
                    if (i == 0 || i == NUM_REQUESTS - 1) {
                        LOG.info("\n========== Request " + (i + 1) + " ==========");
                        LOG.info("Accept-Language: " + acceptLanguage);
                        LOG.info("Status Code: " + statusCode);
                        LOG.info("Response snippet: " + responseText.substring(0, Math.min(200, responseText.length())));
                    }

                } catch (Exception e) {
                    failureCount++;
                    LOG.severe("Request " + (i + 1) + " threw exception: " + e.getMessage());
                }
            }

            //Chain request to retrieve the server localesCache size
            LOG.info("========== Locales cache is populated. Check the server localsCache size ======");
            url = baseURL + "/localesCacheSize";

            LOG.info("Request to [" + url + "]");
            try (final CloseableHttpResponse response = client.execute(new HttpGet(url))) {
                responseText = EntityUtils.toString(response.getEntity());

                LOG.info("Response snippet: [" + responseText + "]");

            } catch (Exception e) {
                LOG.severe("Request  threw exception: " + e.getMessage());
            }
        }

        LOG.info("\n========== Test Summary ==========");
        LOG.info("Total Requests: " + NUM_REQUESTS);
        LOG.info("Successful: " + successCount);
        LOG.info("Failed: " + failureCount);
        LOG.info("Server Locales Size: " + responseText);
        LOG.info("==================================\n");

        assertTrue("Expected all " + NUM_REQUESTS + " requests to succeed, but " + failureCount + " failed", successCount == NUM_REQUESTS);

        assertTrue("Expected server Locales Cache size of 2000 but found " + responseText , responseText.contains("2000"));
    }

    /**
     * Verifies Accept-Language headers exceeding EncodingUtils MAX_ACCEPT_LANGUAGE_LENGTH (default 4096)
     * CWWWC0013E is thrown and handled by the application. The exception is included in the response for verification
     */
    @Test
    public void test_AcceptLanguageExceedsMaxLength() throws Exception {
        LOG.info("=== Starting test_AcceptLanguageExceedsMaxLength");

        final String EXPECTED_MESSAGE_CODE = "CWWWC0013E";

        String url = baseURL + "/TestAcceptLanguage";
        HttpGet getMethod = new HttpGet(url);
        getMethod.addHeader("runTest", "test_AcceptLanguageExceedsMaxLength");

        String oversizedAcceptLanguage = generateOversizedAcceptLanguageHeader(4097);

        LOG.info("===== Sending request [" + url + "]");
        LOG.info("===== Accept-Language oversizedAcceptLanguage [" + oversizedAcceptLanguage +"]");
        getMethod.setHeader("Accept-Language", oversizedAcceptLanguage);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                int statusCode = response.getCode();
                String responseText = EntityUtils.toString(response.getEntity());

                LOG.info("\n========== Oversized Accept-Language Response ==========");
                LOG.info("AL Header length sent: " + oversizedAcceptLanguage.length());
                LOG.info("Status Code: " + statusCode);
                LOG.info("Response text: " + responseText);
                LOG.info("========================================================\n");

                assertEquals("Expected oversized Accept-Language request to succeed", 200, statusCode);

                assertTrue("Response should report CWWWC0013E", responseText.contains(EXPECTED_MESSAGE_CODE));
            }
        }
    }

    private String generateOversizedAcceptLanguageHeader(int targetLength) {
        LOG.info("generateOversizedAcceptLanguageHeader");
        StringBuilder header = new StringBuilder("de-US");
        while (header.length() < targetLength) {
            header.append(",fr-CA;q=0.8");
        }
        return header.substring(0, targetLength);
    }

    /**
     * Generate a list of unique Accept-Language headers
     *
     * @param count Number of unique headers to generate
     * @return List of Accept-Language header values
     */
    private List<String> generateUniqueAcceptLanguageHeaders(int count) {
        LOG.info("generateUniqueAcceptLanguageHeaders");
        List<String> headers = new ArrayList<>();
        Random random = new Random(12345); // Fixed seed for reproducibility

        // Common language codes.
        String[] languages = {
            "es", "fr", "de", "it", "pt", "ru", "ja", "ko", "zh",
            "ar", "hi", "bn", "pa", "te", "mr", "ta", "ur", "gu", "kn",
            "ml", "or", "as", "mai", "bho", "awa", "mag", "bgc", "raj", "hne",
            "nl", "pl", "tr", "vi", "th", "id", "ms", "fil", "uk", "ro",
            "el", "cs", "sv", "hu", "fi", "no", "da", "sk", "bg", "hr"
        };

        // Common country codes
        String[] countries = {
            "US", "GB", "CA", "AU", "NZ", "IE", "ZA", "IN", "PK", "BD",
            "ES", "MX", "AR", "CO", "CL", "PE", "VE", "EC", "GT", "CU",
            "FR", "BE", "CH", "LU", "MC", "DE", "AT", "IT", "PT", "BR",
            "RU", "UA", "BY", "KZ", "JP", "KR", "CN", "TW", "HK", "SG",
            "MY", "TH", "VN", "ID", "PH", "TR", "SA", "AE", "EG", "MA"
        };

        // Generate unique combinations
        for (int i = 0; i < count; i++) {
            String lang = languages[i % languages.length];
            String country = countries[(i / languages.length) % countries.length];

            // Create varied Accept-Language headers with quality values
            double quality = 1.0 - (random.nextDouble() * 0.3); // Quality between 0.7 and 1.0

            String header;
            if (i % 3 == 0) {
                // Simple language-country
                header = lang + "-" + country;
            } else if (i % 3 == 1) {
                // Language-country with quality
                header = String.format("%s-%s;q=%.2f", lang, country, quality);
            } else {
                // Multiple languages with different qualities
                String lang2 = languages[(i + 1) % languages.length];
                double quality2 = quality - 0.1;
                header = String.format("%s-%s;q=%.2f, %s;q=%.2f", lang, country, quality, lang2, quality2);
            }

            headers.add(header);
        }

        LOG.info("Generated " + headers.size() + " unique Accept-Language headers");
        LOG.info("Sample headers:");
        for (int i = 0; i < Math.min(5, headers.size()); i++) {
            LOG.info("  " + (i + 1) + ". " + headers.get(i));
        }

        return headers;
    }
}
