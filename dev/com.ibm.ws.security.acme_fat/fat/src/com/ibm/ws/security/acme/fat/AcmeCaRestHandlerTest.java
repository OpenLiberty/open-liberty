/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.acme.fat;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.security.KeyPair;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import org.apache.http.Header;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.shredzone.acme4j.util.KeyPairUtils;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.acme.docker.CAContainer;
import com.ibm.ws.security.acme.docker.pebble.PebbleContainer;
import com.ibm.ws.security.acme.internal.web.AcmeCaRestHandler;
import com.ibm.ws.security.acme.utils.AcmeFatUtils;

import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Test the {@link AcmeCaRestHandler} REST endpoint.
 */
@RunWith(FATRunner.class)
@SkipForRepeat({ SkipForRepeat.EE9_FEATURES, SkipForRepeat.EE10_FEATURES }) // No value added
public class AcmeCaRestHandlerTest extends TestContainerSuite {
    @Server("com.ibm.ws.security.acme.fat.rest")
    public static LibertyServer server;

    private static ServerConfiguration ORIGINAL_CONFIG;
    private static final String[] DOMAINS = { "domain1.com", "domain2.com", "domain3.com" };

    private static final String ROOT_ENDPOINT = "/ibm/api" + AcmeCaRestHandler.PATH_ROOT;
    private static final String ACCOUNT_ENDPOINT = "/ibm/api" + AcmeCaRestHandler.PATH_ACCOUNT;
    private static final String CERTIFICATE_ENDPOINT = "/ibm/api" + AcmeCaRestHandler.PATH_CERTIFICATE;

    private static final String ADMIN_USER = AcmeFatUtils.ADMIN_USER;
    private static final String ADMIN_PASS = AcmeFatUtils.ADMIN_PASS;
    private static final String READER_USER = "reader";
    private static final String READER_PASS = "readerpass";
    private static final String UNAUTHORIZED_USER = "unauthorized";
    private static final String UNAUTHORIZED_PASS = "unauthorizedpass";

    @ClassRule
    public static CAContainer pebble = new PebbleContainer();

    private static final String JSON_ACCOUNT_REGEN_KEYPAIR_VALID = "{\"" + AcmeCaRestHandler.OP_KEY + "\":\""
            + AcmeCaRestHandler.OP_RENEW_ACCT_KEY_PAIR + "\"}";
    private static final String JSON_ACCOUNT_REGEN_KEYPAIR_INVALID = "{\"" + AcmeCaRestHandler.OP_KEY
            + "\":\"invalid\"}";
    private static final String JSON_EMPTY = "{}";

    private static final String JSON_CERT_REGEN = "{\"" + AcmeCaRestHandler.OP_KEY + "\":\""
            + AcmeCaRestHandler.OP_RENEW_CERT + "\"}";
    private static final String JSON_CERT_INVALID_OP = "{\"" + AcmeCaRestHandler.OP_KEY + "\":\"invalid\"}";

    private static final String CONTENT_TYPE_HTML = "text/html";
    private static final String CONTENT_TYPE_JSON = "application/json";

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void beforeClass() throws Exception {

        server.addIgnoredErrors(Arrays.asList("CWPKI0063W"));

        ORIGINAL_CONFIG = server.getServerConfiguration();

        /*
         * Make sure the HTTP port is open.
         */
        AcmeFatUtils.checkPortOpen(pebble.getHttpPort(), 60000);

        /*
         * Configure the acmeCA-2.0 feature.
         */
        AcmeFatUtils.configureAcmeCA(server, pebble, ORIGINAL_CONFIG, false, true, DOMAINS);

        server.startServer();
        AcmeFatUtils.waitForAcmeToCreateCertificate(server);
        AcmeFatUtils.waitForSslToCreateKeystore(server);
        AcmeFatUtils.waitForSslEndpoint(server);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        /*
         * Stop the server.
         */
        AcmeFatUtils.stopServer(server, "CWPKI2058W");
    }

    @Test
    public void account_endpoint_get_admin_authorized() throws Exception {
        checkAccountHtml(performGet(ACCOUNT_ENDPOINT, 200, CONTENT_TYPE_HTML, ADMIN_USER, ADMIN_PASS));
    }

    @Test
    public void account_endpoint_get_reader_authorized() throws Exception {
        checkAccountHtml(performGet(ACCOUNT_ENDPOINT, 200, CONTENT_TYPE_HTML, READER_USER, READER_PASS));
    }

    @Test
    public void account_endpoint_get_unauthorized_user() throws Exception {
        String jsonResponse = performGet(ACCOUNT_ENDPOINT, 403, CONTENT_TYPE_JSON, UNAUTHORIZED_USER,
                UNAUTHORIZED_PASS);
        assertJsonResponse(jsonResponse, 403);
    }

    @Test
    public void account_endpoint_post_admin_authorized() throws Exception {
        /*
         * The administrative user should be able to request an update of
         * account key pairs.
         */
        KeyPair keyPair1 = KeyPairUtils.readKeyPair(
                new FileReader(new File(server.getServerRoot() + "/resources/security/acmeAccountKey.pem")));
        String jsonResponse = performPost(ACCOUNT_ENDPOINT, 200, CONTENT_TYPE_JSON, ADMIN_USER, ADMIN_PASS,
                CONTENT_TYPE_JSON, JSON_ACCOUNT_REGEN_KEYPAIR_VALID);
        assertJsonResponse(jsonResponse, 200);
        KeyPair keyPair2 = KeyPairUtils.readKeyPair(
                new FileReader(new File(server.getServerRoot() + "/resources/security/acmeAccountKey.pem")));
        assertThat("Expected new account key pair in account key pair file.", keyPair2, not(equalTo(keyPair1)));
        File[] files = findFilesThatMatch(server.getServerRoot() + "/resources/security/", ".*-acmeAccountKey.pem");
        assertEquals("Expected that a backup file would be made:" + Arrays.toString(files), 1, files.length);

        /*
         * Do it one more time.
         */
        jsonResponse = performPost(ACCOUNT_ENDPOINT, 200, CONTENT_TYPE_JSON, ADMIN_USER, ADMIN_PASS, CONTENT_TYPE_JSON,
                JSON_ACCOUNT_REGEN_KEYPAIR_VALID);
        assertJsonResponse(jsonResponse, 200);
        KeyPair keyPair3 = KeyPairUtils.readKeyPair(
                new FileReader(new File(server.getServerRoot() + "/resources/security/acmeAccountKey.pem")));
        assertThat("Expected new account key pair in account key pair file.", keyPair3, not(equalTo(keyPair2)));
        files = findFilesThatMatch(server.getServerRoot() + "/resources/security/", ".*-acmeAccountKey.pem");
        assertEquals("Expected that a second backup file would be made:" + Arrays.toString(files), 2, files.length);
    }

    @Test
    public void account_endpoint_post_invalid_content_type() throws Exception {
        String jsonResponse = performPost(ACCOUNT_ENDPOINT, 415, CONTENT_TYPE_JSON, ADMIN_USER, ADMIN_PASS,
                "application/invalid", JSON_ACCOUNT_REGEN_KEYPAIR_VALID);
        assertJsonResponse(jsonResponse, 415);
    }

    @Test
    public void account_endpoint_post_no_content_type() throws Exception {
        String jsonResponse = performPost(ACCOUNT_ENDPOINT, 415, CONTENT_TYPE_JSON, ADMIN_USER, ADMIN_PASS, null,
                JSON_ACCOUNT_REGEN_KEYPAIR_VALID);
        assertJsonResponse(jsonResponse, 415);
    }

    @Test
    public void account_endpoint_post_no_content() throws Exception {
        String jsonResponse = performPost(ACCOUNT_ENDPOINT, 400, CONTENT_TYPE_JSON, ADMIN_USER, ADMIN_PASS,
                CONTENT_TYPE_JSON, null);
        assertJsonResponse(jsonResponse, 400);
    }

    @Test
    public void account_endpoint_post_empty_json() throws Exception {
        String jsonResponse = performPost(ACCOUNT_ENDPOINT, 400, CONTENT_TYPE_JSON, ADMIN_USER, ADMIN_PASS,
                CONTENT_TYPE_JSON, JSON_EMPTY);
        assertJsonResponse(jsonResponse, 400);
    }

    @Test
    public void account_endpoint_post_invalid_operation() throws Exception {
        String jsonResponse = performPost(ACCOUNT_ENDPOINT, 400, CONTENT_TYPE_JSON, ADMIN_USER, ADMIN_PASS,
                CONTENT_TYPE_JSON, JSON_ACCOUNT_REGEN_KEYPAIR_INVALID);
        assertJsonResponse(jsonResponse, 400);
    }

    @Test
    public void account_endpoint_post_reader_unauthorized() throws Exception {
        String jsonResponse = performPost(ACCOUNT_ENDPOINT, 403, CONTENT_TYPE_JSON, READER_USER, READER_PASS,
                CONTENT_TYPE_JSON, JSON_ACCOUNT_REGEN_KEYPAIR_VALID);
        assertJsonResponse(jsonResponse, 403);
    }

    @Test
    public void account_endpoint_post_unauthorized_user() throws Exception {
        String jsonResponse = performPost(ACCOUNT_ENDPOINT, 403, CONTENT_TYPE_JSON, UNAUTHORIZED_USER,
                UNAUTHORIZED_PASS, CONTENT_TYPE_JSON, JSON_ACCOUNT_REGEN_KEYPAIR_VALID);
        assertJsonResponse(jsonResponse, 403);
    }

    @Test
    public void account_endpoint_delete_method_not_allowed() throws Exception {
        String jsonResponse = performDelete(ACCOUNT_ENDPOINT, 405, CONTENT_TYPE_JSON, ADMIN_USER, ADMIN_PASS);
        assertJsonResponse(jsonResponse, 405);
    }

    @Test
    public void account_endpoint_options_method_not_allowed() throws Exception {
        String jsonResponse = performOptions(ACCOUNT_ENDPOINT, 405, CONTENT_TYPE_JSON, ADMIN_USER, ADMIN_PASS);
        assertJsonResponse(jsonResponse, 405);
    }

    @Test
    public void account_endpoint_put_method_not_allowed() throws Exception {
        String jsonResponse = performPut(ACCOUNT_ENDPOINT, 405, CONTENT_TYPE_JSON, ADMIN_USER, ADMIN_PASS);
        assertJsonResponse(jsonResponse, 405);
    }

    @Test
    public void certificate_endpoint_get_admin_authorized() throws Exception {
        checkCertificateHtml(performGet(CERTIFICATE_ENDPOINT, 200, CONTENT_TYPE_HTML, ADMIN_USER, ADMIN_PASS));
    }

    @Test
    public void certificate_endpoint_get_reader_authorized() throws Exception {
        checkCertificateHtml(performGet(CERTIFICATE_ENDPOINT, 200, CONTENT_TYPE_HTML, READER_USER, READER_PASS));
    }

    @Test
    public void certificate_endpoint_get_unauthorized_user() throws Exception {
        String jsonResponse = performGet(CERTIFICATE_ENDPOINT, 403, CONTENT_TYPE_JSON, UNAUTHORIZED_USER,
                UNAUTHORIZED_PASS);
        assertJsonResponse(jsonResponse, 403);
    }

    @Test
    public void certificate_endpoint_post_renew_certificate() throws Exception {
        /*
         * Get certificate info first.
         */
        String html1 = performGet(CERTIFICATE_ENDPOINT, 200, CONTENT_TYPE_HTML, ADMIN_USER, ADMIN_PASS);

        /*
         * Request a new certificate.
         */
        String jsonResponse = performPost(CERTIFICATE_ENDPOINT, 200, CONTENT_TYPE_JSON, ADMIN_USER, ADMIN_PASS,
                CONTENT_TYPE_JSON, JSON_CERT_REGEN);
        assertJsonResponse(jsonResponse, 200);
        AcmeFatUtils.waitForAcmeToCreateCertificate(server);

        /*
         * Compare the new certificate to the old certificate.
         */
        String html2 = performGet(CERTIFICATE_ENDPOINT, 200, CONTENT_TYPE_HTML, ADMIN_USER, ADMIN_PASS);
        assertNotNull("Should have received an HTML document from REST endpoint.", html2);
        String serial1 = getLeafSerialFromHtml(html1);
        String serial2 = getLeafSerialFromHtml(html2);
        assertThat("Certificates should have been different.", serial2, not(equalTo(serial1)));
    }

    @Test
    public void certificate_endpoint_post_invalid_content_type() throws Exception {
        String jsonResponse = performPost(CERTIFICATE_ENDPOINT, 415, CONTENT_TYPE_JSON, ADMIN_USER, ADMIN_PASS,
                "application/invalid", JSON_CERT_REGEN);
        assertJsonResponse(jsonResponse, 415);
    }

    @Test
    public void certificate_endpoint_post_no_content_type() throws Exception {
        String jsonResponse = performPost(CERTIFICATE_ENDPOINT, 415, CONTENT_TYPE_JSON, ADMIN_USER, ADMIN_PASS, null,
                JSON_CERT_REGEN);
        assertJsonResponse(jsonResponse, 415);
    }

    @Test
    public void certificate_endpoint_post_no_content() throws Exception {
        String jsonResponse = performPost(CERTIFICATE_ENDPOINT, 400, CONTENT_TYPE_JSON, ADMIN_USER, ADMIN_PASS,
                CONTENT_TYPE_JSON, null);
        assertJsonResponse(jsonResponse, 400);
    }

    @Test
    public void certificate_endpoint_post_empty_json() throws Exception {
        String jsonResponse = performPost(CERTIFICATE_ENDPOINT, 400, CONTENT_TYPE_JSON, ADMIN_USER, ADMIN_PASS,
                CONTENT_TYPE_JSON, JSON_EMPTY);
        assertJsonResponse(jsonResponse, 400);
    }

    @Test
    public void certificate_endpoint_post_invalid_operation() throws Exception {
        String jsonResponse = performPost(CERTIFICATE_ENDPOINT, 400, CONTENT_TYPE_JSON, ADMIN_USER, ADMIN_PASS,
                CONTENT_TYPE_JSON, JSON_CERT_INVALID_OP);
        assertJsonResponse(jsonResponse, 400);
    }

    @Test
    public void certificate_endpoint_post_reader_unauthorized() throws Exception {
        String jsonResponse = performPost(CERTIFICATE_ENDPOINT, 403, CONTENT_TYPE_JSON, READER_USER, READER_PASS,
                CONTENT_TYPE_JSON, JSON_CERT_REGEN);
        assertJsonResponse(jsonResponse, 403);
    }

    @Test
    public void certificate_endpoint_post_unauthorized_user() throws Exception {
        String jsonResponse = performPost(CERTIFICATE_ENDPOINT, 403, CONTENT_TYPE_JSON, UNAUTHORIZED_USER,
                UNAUTHORIZED_PASS, CONTENT_TYPE_JSON, JSON_CERT_REGEN);
        assertJsonResponse(jsonResponse, 403);
    }

    @Test
    public void certificate_endpoint_delete_method_not_allowed() throws Exception {
        String jsonResponse = performDelete(CERTIFICATE_ENDPOINT, 405, CONTENT_TYPE_JSON, ADMIN_USER, ADMIN_PASS);
        assertJsonResponse(jsonResponse, 405);
    }

    @Test
    public void certificate_endpoint_options_method_not_allowed() throws Exception {
        String jsonResponse = performOptions(CERTIFICATE_ENDPOINT, 405, CONTENT_TYPE_JSON, ADMIN_USER, ADMIN_PASS);
        assertJsonResponse(jsonResponse, 405);
    }

    @Test
    public void certificate_endpoint_put_method_not_allowed() throws Exception {
        String jsonResponse = performPut(CERTIFICATE_ENDPOINT, 405, CONTENT_TYPE_JSON, ADMIN_USER, ADMIN_PASS);
        assertJsonResponse(jsonResponse, 405);
    }

    @Test
    public void root_endpoint_get_admin_authorized() throws Exception {
        String html = performGet(ROOT_ENDPOINT, 200, CONTENT_TYPE_HTML, ADMIN_USER, ADMIN_PASS);
        checkAccountHtml(html);
        checkCertificateHtml(html);
    }

    @Test
    public void root_endpoint_get_reader_authorized() throws Exception {
        String html = performGet(ROOT_ENDPOINT, 200, CONTENT_TYPE_HTML, READER_USER, READER_PASS);
        checkAccountHtml(html);
        checkCertificateHtml(html);
    }

    @Test
    public void root_endpoint_get_unauthorized_user() throws Exception {
        String jsonResponse = performGet(ROOT_ENDPOINT, 403, CONTENT_TYPE_JSON, UNAUTHORIZED_USER, UNAUTHORIZED_PASS);
        assertJsonResponse(jsonResponse, 403);
    }

    @Test
    public void root_endpoint_delete_method_not_allowed() throws Exception {
        String jsonResponse = performDelete(ROOT_ENDPOINT, 405, CONTENT_TYPE_JSON, ADMIN_USER, ADMIN_PASS);
        assertJsonResponse(jsonResponse, 405);
    }

    @Test
    public void root_endpoint_options_method_not_allowed() throws Exception {
        String jsonResponse = performOptions(ROOT_ENDPOINT, 405, CONTENT_TYPE_JSON, ADMIN_USER, ADMIN_PASS);
        assertJsonResponse(jsonResponse, 405);
    }

    @Test
    public void root_endpoint_put_method_not_allowed() throws Exception {
        String jsonResponse = performPut(ROOT_ENDPOINT, 405, CONTENT_TYPE_JSON, ADMIN_USER, ADMIN_PASS);
        assertJsonResponse(jsonResponse, 405);
    }

    @Test
    public void root_endpoint_post_method_not_allowed() throws Exception {
        String jsonResponse = performPost(ROOT_ENDPOINT, 405, CONTENT_TYPE_JSON, ADMIN_USER, ADMIN_PASS,
                CONTENT_TYPE_JSON, JSON_ACCOUNT_REGEN_KEYPAIR_VALID);
        assertJsonResponse(jsonResponse, 405);
    }

    private static void checkAccountHtml(String html) {
        assertNotNull("Should have received an HTML document from REST endpoint.", html);
        assertThat("Expected account header.", html, containsString("ACME CA Account Details"));
        assertThat("Expected account status.", html, containsString("Status:"));
    }

    private static void checkCertificateHtml(String html) {
        assertNotNull("Should have received an HTML document from REST endpoint.", html);
        assertThat("Expected certificate header.", html, containsString("Active Certificate Chain"));
        assertThat("Expected certificate with signature algorithm.", html, containsString("Signature Algorithm:"));
    }

    /**
     * Make a GET call to the REST endpoint. This will return the currently
     * configured certificate.
     * 
     * @param endpoint
     *            The endpoint to call.
     * @param expectedStatus
     *            The expected HTTP return code.
     * @param expectedContentType
     *            The expected response content type.
     * @param user
     *            The user to make the call with.
     * @param password
     *            The password to make the call with.
     * @return the response in the form of a string.
     * @throws Exception
     *             If the call failed.
     */
    private static String performGet(String endpoint, int expectedStatus, String expectedContentType, String user,
            String password) throws Exception {
        String methodName = "performRestGet()";

        try (CloseableHttpClient httpclient = AcmeFatUtils.getInsecureHttpsClient()) {

            /*
             * Create a GET request to the Liberty server.
             */
            HttpGet httpGet = new HttpGet("https://localhost:" + server.getHttpDefaultSecurePort() + endpoint);
            httpGet.setHeader("Authorization",
                    "Basic " + DatatypeConverter.printBase64Binary((user + ":" + password).getBytes()));

            /*
             * Send the GET request and process the response.
             */
            try (final CloseableHttpResponse response = httpclient.execute(httpGet)) {
                AcmeFatUtils.logHttpResponse(PebbleContainer.class, methodName, httpGet, response);

                StatusLine statusLine = response.getStatusLine();
                assertEquals("Unexpected status code response.", expectedStatus, statusLine.getStatusCode());

                /*
                 * Check content type header.
                 */
                if (expectedContentType != null) {
                    Header[] headers = response.getHeaders("content-type");
                    assertNotNull("Expected content type header.", headers);
                    assertEquals("Expected 1 content type header.", 1, headers.length);
                    assertEquals("Unexpected content type.", expectedContentType, headers[0].getValue());
                }

                String contentString = EntityUtils.toString(response.getEntity());
                Log.info(AcmeCaRestHandlerTest.class, methodName, "HTTP GET contents: \n" + contentString);

                return contentString;
            }
        }
    }

    /**
     * Make a PUT call to the REST endpoint. This will drive a certificate
     * refresh.
     * 
     * @param endpoint
     *            The endpoint to call.
     * @param expectedStatus
     *            The expected HTTP return code.
     * @param expectedContentType
     *            The expected response content type.
     * @param user
     *            The user to make the call with.
     * @param password
     *            The password to make the call with.
     * @return the response in the form of a string.
     * @throws Exception
     *             If the call failed.
     */
    private static String performPut(String endpoint, int expectedStatus, String expectedContentType, String user,
            String password) throws Exception {
        String methodName = "performPut()";

        try (CloseableHttpClient httpclient = AcmeFatUtils.getInsecureHttpsClient()) {

            /*
             * Create a PUT request to the Liberty server.
             */
            HttpPut httpPut = new HttpPut("https://localhost:" + server.getHttpDefaultSecurePort() + endpoint);
            httpPut.setHeader("Authorization",
                    "Basic " + DatatypeConverter.printBase64Binary((user + ":" + password).getBytes()));

            /*
             * Send the PUT request and process the response.
             */
            try (final CloseableHttpResponse response = httpclient.execute(httpPut)) {
                AcmeFatUtils.logHttpResponse(PebbleContainer.class, methodName, httpPut, response);

                StatusLine statusLine = response.getStatusLine();
                assertEquals("Unexpected status code response.", expectedStatus, statusLine.getStatusCode());

                /*
                 * Check content type header.
                 */
                if (expectedContentType != null) {
                    Header[] headers = response.getHeaders("content-type");
                    assertNotNull("Expected content type header.", headers);
                    assertEquals("Expected 1 content type header.", 1, headers.length);
                    assertEquals("Unexpected content type.", expectedContentType, headers[0].getValue());
                }

                String contentString = EntityUtils.toString(response.getEntity());
                Log.info(AcmeCaRestHandlerTest.class, methodName, "HTTP put contents: \n" + contentString);

                return contentString;
            }
        }
    }

    /**
     * Make a POST call to the REST endpoint.
     * 
     * @param endpoint
     *            The endpoint to call.
     * @param expectedStatus
     *            The expected HTTP return code.
     * @param expectedContentType
     *            The expected response content type.
     * @param user
     *            The user to make the call with.
     * @param password
     *            The password to make the call with.
     * @return the response in the form of a string.
     * @throws Exception
     *             If the call failed.
     */
    private static String performPost(String endpoint, int expectedStatus, String expectedContentType, String user,
            String password, String contentType, String content) throws Exception {
        String methodName = "performPost()";

        try (CloseableHttpClient httpclient = AcmeFatUtils.getInsecureHttpsClient()) {

            /*
             * Create a POST request to the Liberty server.
             */
            HttpPost httpPost = new HttpPost("https://localhost:" + server.getHttpDefaultSecurePort() + endpoint);
            httpPost.setHeader("Authorization",
                    "Basic " + DatatypeConverter.printBase64Binary((user + ":" + password).getBytes()));
            if (contentType != null && !contentType.isEmpty()) {
                httpPost.setHeader("Content-Type", contentType);
            }
            if (content != null && !content.isEmpty()) {
                httpPost.setEntity(new StringEntity(content));
            }

            /*
             * Send the POST request and process the response.
             */
            try (final CloseableHttpResponse response = httpclient.execute(httpPost)) {
                AcmeFatUtils.logHttpResponse(PebbleContainer.class, methodName, httpPost, response);

                StatusLine statusLine = response.getStatusLine();
                assertEquals("Unexpected status code response.", expectedStatus, statusLine.getStatusCode());

                /*
                 * Check content type header.
                 */
                if (expectedContentType != null) {
                    Header[] headers = response.getHeaders("content-type");
                    assertNotNull("Expected content type header.", headers);
                    assertEquals("Expected 1 content type header.", 1, headers.length);
                    assertEquals("Unexpected content type.", expectedContentType, headers[0].getValue());
                }

                String contentString = EntityUtils.toString(response.getEntity());
                Log.info(AcmeCaRestHandlerTest.class, methodName, "HTTP post contents: \n" + contentString);

                return contentString;
            }
        }
    }

    /**
     * Make a DELETE call to the REST endpoint.
     * 
     * @param endpoint
     *            The endpoint to call.
     * @param expectedStatus
     *            The expected HTTP return code.
     * @param expectedContentType
     *            The expected response content type.
     * @param user
     *            The user to make the call with.
     * @param password
     *            The password to make the call with.
     * @return the response in the form of a string.
     * @throws Exception
     *             If the call failed.
     */
    private static String performDelete(String endpoint, int expectedStatus, String expectedContentType, String user,
            String password) throws Exception {
        String methodName = "performPut()";

        try (CloseableHttpClient httpclient = AcmeFatUtils.getInsecureHttpsClient()) {

            /*
             * Create a DELETE request to the Liberty server.
             */
            HttpDelete httpDelete = new HttpDelete("https://localhost:" + server.getHttpDefaultSecurePort() + endpoint);
            httpDelete.setHeader("Authorization",
                    "Basic " + DatatypeConverter.printBase64Binary((user + ":" + password).getBytes()));

            /*
             * Send the DELETE request and process the response.
             */
            try (final CloseableHttpResponse response = httpclient.execute(httpDelete)) {
                AcmeFatUtils.logHttpResponse(PebbleContainer.class, methodName, httpDelete, response);

                StatusLine statusLine = response.getStatusLine();
                assertEquals("Unexpected status code response.", expectedStatus, statusLine.getStatusCode());

                /*
                 * Check content type header.
                 */
                if (expectedContentType != null) {
                    Header[] headers = response.getHeaders("content-type");
                    assertNotNull("Expected content type header.", headers);
                    assertEquals("Expected 1 content type header.", 1, headers.length);
                    assertEquals("Unexpected content type.", expectedContentType, headers[0].getValue());
                }

                String contentString = EntityUtils.toString(response.getEntity());
                Log.info(AcmeCaRestHandlerTest.class, methodName, "HTTP delete contents: \n" + contentString);

                return contentString;
            }
        }
    }

    /**
     * Make a DELETE call to the REST endpoint.
     * 
     * @param endpoint
     *            The endpoint to call.
     * @param expectedStatus
     *            The expected HTTP return code.
     * @param expectedContentType
     *            The expected response content type.
     * @param user
     *            The user to make the call with.
     * @param password
     *            The password to make the call with.
     * @return the response in the form of a string.
     * @throws Exception
     *             If the call failed.
     */
    private static String performOptions(String endpoint, int expectedStatus, String expectedContentType, String user,
            String password) throws Exception {
        String methodName = "performPut()";

        try (CloseableHttpClient httpclient = AcmeFatUtils.getInsecureHttpsClient()) {

            /*
             * Create a OPTIONS request to the Liberty server.
             */
            HttpOptions httpOptions = new HttpOptions(
                    "https://localhost:" + server.getHttpDefaultSecurePort() + endpoint);
            httpOptions.setHeader("Authorization",
                    "Basic " + DatatypeConverter.printBase64Binary((user + ":" + password).getBytes()));

            /*
             * Send the OPTIONS request and process the response.
             */
            try (final CloseableHttpResponse response = httpclient.execute(httpOptions)) {
                AcmeFatUtils.logHttpResponse(PebbleContainer.class, methodName, httpOptions, response);

                StatusLine statusLine = response.getStatusLine();
                assertEquals("Unexpected status code response.", expectedStatus, statusLine.getStatusCode());

                /*
                 * Check content type header.
                 */
                if (expectedContentType != null) {
                    Header[] headers = response.getHeaders("content-type");
                    assertNotNull("Expected content type header.", headers);
                    assertEquals("Expected 1 content type header.", 1, headers.length);
                    assertEquals("Unexpected content type.", expectedContentType, headers[0].getValue());
                }

                String contentString = EntityUtils.toString(response.getEntity());
                Log.info(AcmeCaRestHandlerTest.class, methodName, "HTTP options contents: \n" + contentString);

                return contentString;
            }
        }
    }

    /**
     * Get the leaf certificates serial number from the HTML page returned from
     * the REST client.
     * 
     * @param htmlCertChain
     *            the HTML content from a GET request.
     * @return The serial number, or null if one was not found.
     */
    private static String getLeafSerialFromHtml(String htmlCertChain) {
        String serial = null;

        Matcher m = Pattern.compile(".*SerialNumber:\\s+\\s*(.*).*").matcher(htmlCertChain);
        if (m.find()) {
            serial = m.group(1);
            // Before Java 23, the format was SerialNumber: [number].  Starting in Java 23, the brackets are not used.
            // The regex above was adjusted to remove the brackets, so we need to strip off the brackets now.
            if (serial.startsWith("[")) {
                serial = serial.substring(1);
            }
            if (serial.endsWith("]")) {
                serial= serial.substring(0, serial.length()  -1);
            }
        }

        Log.info(AcmeCaRestHandlerTest.class, "getLeafSerialFromHtml(String)", serial);
        return serial;
    }

    /**
     * Find all files in the directory whose file name matches the given
     * pattern.
     * 
     * @param dir
     *            The directory to search.
     * @param pattern
     *            The pattern to check file names against.
     * @return The array of files that match.
     */
    public static File[] findFilesThatMatch(String dir, final String pattern) {
        File searchDir = new File(dir);

        return searchDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().matches(pattern);
            }
        });
    }

    /**
     * Assert that the expected JSON response is returned.
     * 
     * @param jsonResponse
     *            The actual JSON response.
     * @param expectedStatusCode
     *            The expected status code.
     */
    private static void assertJsonResponse(String jsonResponse, int expectedStatusCode) {
        assertThat("Unexpected HTTP status code returned in JSON response.", jsonResponse,
                containsString("\"httpCode\":" + expectedStatusCode));
        if (expectedStatusCode == 200) {
            assertThat("Expected no error message in JSON response.", jsonResponse,
                    not(containsString("\"message\":")));
        } else {
            assertThat("Expected error message in JSON response.", jsonResponse, containsString("\"message\":"));
        }
    }

    /**
     * Make sure we are blocked when we do back to back renew requests.
     * 
     * @throws Exception
     *             If the test failed for some reason.
     */
    @Test
    @CheckForLeakedPasswords(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD)
    public void certificate_endpoint_post_repeated_renew() throws Exception {
        final String methodName = "certificate_endpoint_post_repeated_renew";
        Certificate[] startingCertificateChain = null, endingCertificateChain = null;

        /*
         * Enable the minimum renew window
         * 
         */

        ServerConfiguration clone = server.getServerConfiguration().clone();
        clone.getAcmeCA().setDisableMinRenewWindow(false);
        clone.getAcmeCA().setRenewCertMin(30000);

        /***********************************************************************
         * 
         * The server will request a certificate successfully, then request
         * more back to back, which should fail. Then we'll sleep and should
         * have a successful request again.
         * 
         **********************************************************************/
        try {
            Log.info(this.getClass(), methodName, "TEST Run back to back REST renew requests");

            AcmeFatUtils.updateConfigDynamically(server, clone);

            /*
             * First renew request should update.
             */
            startingCertificateChain = AcmeFatUtils.assertAndGetServerCertificate(server, pebble);

            long startRequest = System.currentTimeMillis();
            String jsonResponse = performPost(AcmeCaRestHandlerTest.CERTIFICATE_ENDPOINT, 200, AcmeCaRestHandlerTest.CONTENT_TYPE_JSON, AcmeCaRestHandlerTest.ADMIN_USER, AcmeCaRestHandlerTest.ADMIN_PASS,
                    AcmeCaRestHandlerTest.CONTENT_TYPE_JSON, AcmeCaRestHandlerTest.JSON_CERT_REGEN);
            long endTime = System.currentTimeMillis();
            long requestTime = endTime - startRequest;
            Log.info(this.getClass(), methodName, "Total Certificate request time was: " + requestTime);
            assertJsonResponse(jsonResponse, 200);
            AcmeFatUtils.waitForNewCert(server, pebble, startingCertificateChain);

            /*
             * Do back to back renew requests, we should be blocked from renewing
             * 
             */

            if (requestTime < clone.getAcmeCA().getRenewCertMin()) {

                Log.info(this.getClass(), testName.getMethodName(), "Renew, expect failure:");

                startingCertificateChain = AcmeFatUtils.assertAndGetServerCertificate(server, pebble);

                jsonResponse = performPost(AcmeCaRestHandlerTest.CERTIFICATE_ENDPOINT, 429, AcmeCaRestHandlerTest.CONTENT_TYPE_JSON, AcmeCaRestHandlerTest.ADMIN_USER, AcmeCaRestHandlerTest.ADMIN_PASS,
                        AcmeCaRestHandlerTest.CONTENT_TYPE_JSON, AcmeCaRestHandlerTest.JSON_CERT_REGEN);
                assertThat("Unexpected HTTP status code returned in JSON response.", jsonResponse,
                        containsString("\"httpCode\":" + 429));
                assertThat("Expected error message in JSON response.", jsonResponse, containsString("\"message\":"));

                Log.info(this.getClass(), methodName, "Response received: " + jsonResponse);

                endingCertificateChain = AcmeFatUtils.assertAndGetServerCertificate(server, pebble);

                assertEquals("The certificate should not renew after REST request.",
                        ((X509Certificate) startingCertificateChain[0]).getSerialNumber(),
                        ((X509Certificate) endingCertificateChain[0]).getSerialNumber());

                /*
                 * Allow the minimum time to expire, next request should be successful
                 */
                Log.info(this.getClass(), methodName, "Start sleep to expire the renewCertMin time");
                Thread.sleep(clone.getAcmeCA().getRenewCertMin() + 2000 - (System.currentTimeMillis() - endTime));
                Log.info(this.getClass(), methodName, "End sleep to expire the renewCertMin time");
            } else {
                /*
                 * Had some test failures where the revoke took a long time and ate up the minRenew window for us, making the
                 * renewCertMin longer.
                 */
                fail("Certificate request took longer than the renewCertMin is configured for, update test again: " + requestTime);
            }

            startingCertificateChain = AcmeFatUtils.assertAndGetServerCertificate(server, pebble);

            jsonResponse = performPost(AcmeCaRestHandlerTest.CERTIFICATE_ENDPOINT, 200, AcmeCaRestHandlerTest.CONTENT_TYPE_JSON, AcmeCaRestHandlerTest.ADMIN_USER, AcmeCaRestHandlerTest.ADMIN_PASS,
                    AcmeCaRestHandlerTest.CONTENT_TYPE_JSON, AcmeCaRestHandlerTest.JSON_CERT_REGEN);
            assertJsonResponse(jsonResponse, 200);

            AcmeFatUtils.waitForNewCert(server, pebble, startingCertificateChain);

        } finally {
            /*
             * Disable min renew so the rest of the tests can pass without extra time
             * spent sleeping
             */
            clone = server.getServerConfiguration().clone();
            clone.getAcmeCA().setDisableMinRenewWindow(true);
            AcmeFatUtils.updateConfigDynamically(server, clone);
        }
    }
}
