/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.HttpEndpoint;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.ServerFileUtils;

/**
 * A set of tests to verify the <header> configurations and the corresponding response headers
 */
@RunWith(FATRunner.class)
public class WCResponseHeadersTest {

    private static final Class<?> ME = WCResponseHeadersTest.class;
    private static final String APP_NAME = "ResponseHeadersTest";
    //App used to simulate responses during login process
    private static final String APP_NAME_SECURE_APP = "SameSiteSecurityTest";

    @Server("servlet40_headers")
    public static LibertyServer server;

    private static final ServerFileUtils serverFileUtils = new ServerFileUtils();

    // Tests can use this to indicate they don't make any config updates from the original configuration
    private static boolean restoreSavedConfig = true;

    private static ServerConfiguration savedConfig;

    @Before
    public void setUpBeforeEachTest() throws Exception {
        if (server != null && savedConfig != null) {
            Exception failure = null;
            String consoleLogFileName = WCResponseHeadersTest.class.getSimpleName() + ".log";

            if (!server.isStarted()) {
                server.updateServerConfiguration(savedConfig);
                server.startServer(consoleLogFileName);
                Log.info(ME, "setUpBeforeEachTest", "server started, log file is " + consoleLogFileName);
            } else if (restoreSavedConfig) {
                try {
                    //Allow the warning messages we may have generated on purpose
                    //
                    // W CWWKT0042W: An empty header name was found when the {0} configuration was parsed. This value is ignored.
                    //
                    // W CWWKT0043W: A duplicate header name was found in the [{0}] header using the {1} configuration. All configurations
                    // for the [{0}] header are ignored. Any header that is defined by the remove, add, set, or setIfMissing configurations
                    //must be unique across all configurations.
                    //
                    // W CWWKT0044W: The [{0}] header, which is marked as a duplicate header name, was found in the {1} configuration.
                    // The [{0}] header is ignored. Any header that is defined by the {1} configuration must contain unique header names.

                    server.stopServer("CWWKT0042W", "CWWKT0043W", "CWWKT0044W");
                } catch (Exception e) {
                    failure = e;
                }
                server.updateServerConfiguration(savedConfig);
                server.startServer(consoleLogFileName, true);
                Log.info(getClass(), "setUpBeforeTest", "server restarted, log file is " + consoleLogFileName);
            }
            restoreSavedConfig = true;
            if (failure != null)
                throw failure;
        }
    }

    @BeforeClass
    public static void setUpOnce() throws Exception {
        if (server != null) {
            //Update the bootstrap to include symbolic default variables for
            //the <header> attributes. Will be used in testHeaderBootstrapping()
            writeBootstrapProperty(server, "add.header", "bootstrappedAddHeader:addHeaderValue");
            writeBootstrapProperty(server, "set.header", "bootstrappedSetHeader:setHeaderValue");
            writeBootstrapProperty(server, "set.if.missing.header", "bootstrappedSetIfMissingHeader:setIfMissingValue");
            writeBootstrapProperty(server, "remove.header", "customHeader");

            //Create the ResponseHeadersTest.war application
            ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "headers.servlets");
            savedConfig = server.getServerConfiguration().clone();
            server.setConfigUpdateTimeout(30 * 1000);
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        //Stop the server
        if (server != null) {

            //Allow the warning messages we may have generated on purpose
            //
            // W CWWKT0042W: An empty header name was found when the {0} configuration was parsed. This value is ignored.
            //
            // W CWWKT0043W: A duplicate header name was found in the [{0}] header using the {1} configuration. All configurations
            // for the [{0}] header are ignored. Any header that is defined by the remove, add, set, or setIfMissing configurations
            //must be unique across all configurations.
            //
            // W CWWKT0044W: The [{0}] header, which is marked as a duplicate header name, was found in the {1} configuration.
            // The [{0}] header is ignored. Any header that is defined by the {1} configuration must contain unique header names.
            if (server.isStarted())
                server.stopServer("CWWKT0042W", "CWWKT0043W", "CWWKT0044W");
            server.updateServerConfiguration(savedConfig);
        }

    }

    /**
     * Execute a request/response exchange to the given URL. If the status code is OK (200), return
     * all headers in the response so that the tests can evaluate that all conditions are met.
     *
     * @param url
     * @return
     */
    private static Header[] executeExchangeAndGetHeaders(String url, String testName) throws Exception {
        return executeExchangeAndGetHeaders(url, testName, 200);

    }

    /**
     * Execute a request/response exchange to the given URL. If the status code matches the expected value, return
     * all headers in the response so that the tests can evaluate that all conditions are met.
     *
     * @param url
     * @return
     */
    private static Header[] executeExchangeAndGetHeaders(String url, String testName, int expectedStatusCode) throws Exception {
        Log.info(ME, testName, "url: " + url);

        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        HttpGet getMethod = new HttpGet(url);
        Header[] headers = null;

        try {
            client = HttpClientBuilder.create().build();
            response = client.execute(getMethod);
            String responseText = EntityUtils.toString(response.getEntity());

            Log.info(ME, testName, "\n" + "Response Text:");
            Log.info(ME, testName, "\n" + responseText);

            assertEquals("The expected status code [" + expectedStatusCode + "] was not returned",
                         expectedStatusCode, response.getCode());

            headers = response.getHeaders();

        } finally {
            if (client != null)
                client.close();
            if (response != null)
                response.close();

        }

        return headers;
    }

    /**
     * Utility method to format the various URLs used during the tests
     *
     * @param path
     * @return
     */
    private String generateURL(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + path;
    }

    /**
     * Utility method to write a key-value propery to the server's boostrap.properties file
     *
     * @param server   - server instance
     * @param property - name of the property to add
     * @param value    - value of the property to add
     */
    private static void writeBootstrapProperty(LibertyServer server, String property, String value) throws Exception {

        if (property == null || value == null)
            return;

        String bootstrapFilePath = serverFileUtils.getServerFileLoc(server) + "/bootstrap.properties";
        FileWriter writer = new FileWriter(bootstrapFilePath, true);

        writer.append(System.getProperty("line.separator"));
        writer.append(property + "=" + value);
        writer.append(System.getProperty("line.separator"));

        writer.close();
    }

    /**
     * Header Symbolic Configuration
     *
     * Test that the <headers> element can be configured symbolically.
     * A single bootstrap property for each of the <headers> attributes is
     * writen to the boostrap.properties file when the setUp method is first
     * ran. This test will use those property names within each attribute
     * configuration and test that the response contains all values.
     *
     * To test the "remove" option, ${remove.header} will map to the header
     * [customHeader], which is added by the application by means of the
     * "testCondition=singleHeader" parameter.
     *
     * The application will also add the header [appVerificationHeader]
     *
     * Expectations:
     * Present headers - [bootstrappedAddHeader:addHeaderValue],
     * [bootstrappedSetHeader:setHeaderValue],
     * [bootstrappedSetIfMissingHeader:setIfMissingValue],
     * [appVerificationHeader]
     * Missing headers - [customHeader]
     *
     *
     * @throws Exception
     */
    @Test
    public void testHeaderBootstrapping() throws Exception {

        String testName = "testHeaderBootstrapping";
        String url = generateURL("/ResponseHeadersServlet?testCondition=singleHeader");
        restoreSavedConfig = true;

        ServerConfiguration configuration = server.getServerConfiguration();
        Log.info(ME, testName, "Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getHeaders().setAdd("${add.header}");
        httpEndpoint.getHeaders().setSet("${set.header}");
        httpEndpoint.getHeaders().setSetIfMissing("${set.if.missing.header}");
        httpEndpoint.getHeaders().setRemove("${remove.header}");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*ResponseHeadersTest.*");

        //Send the request and verify the expected headers

        Header[] headers = executeExchangeAndGetHeaders(url, testName);

        HeaderExpectations expectations = new HeaderExpectations();
        expectations.expectPresent("bootstrappedAddHeader", "addHeaderValue");
        expectations.expectPresent("bootstrappedSetHeader", "setHeaderValue");
        expectations.expectPresent("bootstrappedSetIfMissingHeader", "setIfMissingValue");
        expectations.expectPresent("appVerificationHeader", null);
        expectations.expectMissing("customHeader");

        expectations.evaluate(headers);

    }

    /**
     * Header Misconfiguration Series Test 1/4
     *
     * Test the misconfigurations messages of the <headers> element.
     *
     * This test will purposely misconfigure the "add", "set", "setIfMissing", and "remove"
     * attributes by providing empty header names. It is expected that the 'empty header name'
     * CWWKT0042W message will logged on all four configurations.
     *
     * Good configurations should continue working. Therefore, this test will also test correct
     * configurations on all attributes. The test servlet will be invoked with the parameter:
     * [testCondition=singleHeader], which will add the response header: "customHeader:appValue".
     *
     * The "remove" attribute will remove this, so it is expected to not appear on the written
     * response headers.
     *
     * The application will add the header [appVerificationHeader].
     *
     * Expected present headers: [addHeader: addValue], [addHeader: addValue2], [setHeader], [setIfMissingHeader],
     * [appVerificationHeader]
     * Expected removed header - [customHeader]
     *
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testHeaderMisconfiguration_EmptyHeaderName() throws Exception {

        String testName = "testHeaderMisconfiguration_EmptyHeaderName";
        String url = generateURL("/ResponseHeadersServlet?testCondition=singleHeader");
        restoreSavedConfig = true;
        String stringToSearchFor = "CWWKT0042W";

        ServerConfiguration configuration = server.getServerConfiguration();
        Log.info(ME, testName, "Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getHeaders().setAdd(":testValue, addHeader:addValue, addHeader:addValue2");
        httpEndpoint.getHeaders().setSet(":testValue, setHeader");
        httpEndpoint.getHeaders().setSetIfMissing(":testValue, setIfMissingHeader");
        httpEndpoint.getHeaders().setRemove("customHeader, ");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*ResponseHeadersTest.*");

        Log.info(ME, testName, "Updated server configuration: " + configuration);

        List<String> logs = server.findStringsInLogs(stringToSearchFor);

        assertTrue("Expected four occurances of the empty header string but found: " + logs.size(), logs.size() == 4);

        //Send the request and verify the expected headers

        Header[] headers = executeExchangeAndGetHeaders(url, testName);
        HeaderExpectations expectations = new HeaderExpectations();
        expectations.expectPresent("addHeader", "addValue");
        expectations.expectPresent("addHeader", "addValue2");
        expectations.expectPresent("setHeader");
        expectations.expectPresent("setIfMissingHeader");
        expectations.expectPresent("appVerificationHeader");
        expectations.expectMissing("customHeader");

        expectations.evaluate(headers);

    }

    /**
     * Header Misconfiguration Series Test 2/4
     *
     * Test the misconfigurations messages of the <headers> element.
     *
     * This test will purposely misconfigure the "add", "set", "setIfMissing", and "remove"
     * attributes by providing duplicate header names. It is expected that the 'duplicate header
     * name' CWWKT0043W message will logged three times.
     *
     * Good configurations should continue working. Therefore, this test will also test correct
     * configurations on all attributes. The test servlet will be invoked with the parameter:
     * [testCondition=singleHeader], which will add the response header: "customHeader:appValue".
     *
     * The "remove" attribute will remove this, so it is expected to not appear on the written
     * response headers.
     *
     * The application will add the header [appVerificationHeader].
     *
     * Expected bad headers: [badHeader, anotherBadHeader, yetAnotherBadHeader]
     * Expected headers: [addHeader:addValue, addHeader:addValue2 setHeader,
     * setIfMissingHeader, appVerificationHeader]
     * Expected removed header - [customHeader] - from servlet.
     *
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testHeaderMisconfiguration_DuplicateHeaderName() throws Exception {

        String testName = "testHeaderMisconfiguration_DuplicateHeaderName";
        restoreSavedConfig = true;
        String url = generateURL("/ResponseHeadersServlet?testCondition=singleHeader");
        String stringToSearchFor = "CWWKT0043W";

        ServerConfiguration configuration = server.getServerConfiguration();
        Log.info(ME, testName, "Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");

        httpEndpoint.getHeaders().setAdd("addHeader: addValue, addHeader:addValue2, badHeader");
        httpEndpoint.getHeaders().setSet("badHeader, anotherBadHeader, setHeader");
        httpEndpoint.getHeaders().setSetIfMissing("anotherBadHeader, yetAnotherBadHeader, setIfMissingHeader");
        httpEndpoint.getHeaders().setRemove("yetAnotherBadHeader, customHeader");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*ResponseHeadersTest.*");

        Log.info(ME, testName, "Updated server configuration: " + configuration);

        List<String> logs = server.findStringsInLogs(stringToSearchFor);

        assertTrue("Expected three occurances of the duplicate header name string but found: " + logs.size(), logs.size() == 3);

        //Send the request and verify the expected headers

        Header[] headers = executeExchangeAndGetHeaders(url, testName);
        HeaderExpectations expectations = new HeaderExpectations();
        expectations.expectPresent("addHeader", "addValue");
        expectations.expectPresent("addHeader", "addValue2");
        expectations.expectPresent("setHeader");
        expectations.expectPresent("setIfMissingHeader");
        expectations.expectPresent("appVerificationHeader");
        expectations.expectMissing("customHeader");

        expectations.evaluate(headers);

    }

    /**
     * Header Misconfiguration Series Test 3/4
     *
     * Test the misconfigurations messages of the <headers> element.
     *
     * This test will purposely misconfigure the "add", "set", "setIfMissing", and "remove"
     * attributes by providing previously duplicated header names. That is, it will provide
     * three times the same header name. It is expected that the 'previously duplicated
     * header name' CWWKT0044W message will logged two times.
     *
     * Good configurations should continue working. Therefore, this test will also test correct
     * configurations on all attributes. The test servlet will be invoked with the parameter:
     * [testCondition=singleHeader], which will add the response header: "customHeader:appValue".
     *
     * The "remove" attribute will remove this, so it is expected to not appear on the written
     * response headers.
     *
     * The application will add the header [appVerificationHeader].
     *
     * Bad Headers: [badHeader, anotherBadHeader]
     * Expected present headers: [addHeader, setHeader, setIfMissingHeader]
     * Expected removed header - [customHeader]
     *
     * This specifically tests the "remove" configuration with "set-cookie". No "set-cookie" headers are
     * expected.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testHeaderMisconfiguration_PreviouslyDuplicatedHeaderName() throws Exception {

        String testName = "testHeaderMisconfiguration_PreviouslyDuplicatedHeaderName";
        String url = generateURL("/ResponseHeadersServlet?testCondition=singleHeader");
        restoreSavedConfig = true;
        String stringToSearchFor = "CWWKT0044W";

        ServerConfiguration configuration = server.getServerConfiguration();
        Log.info(ME, testName, "Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getHeaders().setAdd("badHeader, addHeader");
        httpEndpoint.getHeaders().setSet("badHeader, badHeader, anotherBadHeader, setHeader");
        httpEndpoint.getHeaders().setSetIfMissing("anotherBadHeader, setIfMissingHeader");
        httpEndpoint.getHeaders().setRemove("anotherBadHeader, customHeader");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*ResponseHeadersTest.*");

        Log.info(ME, testName, "Updated server configuration: " + configuration);

        List<String> logs = server.findStringsInLogs(stringToSearchFor);

        assertTrue("Expected two occurances of the previously duplicated header string but found: " + logs.size(), logs.size() == 2);

        //Send the request and verify the expected headers
        Header[] headers = executeExchangeAndGetHeaders(url, testName);

        HeaderExpectations expectations = new HeaderExpectations();
        expectations.expectPresent("addHeader");
        expectations.expectPresent("setHeader");
        expectations.expectPresent("setIfMissingHeader");
        expectations.expectPresent("appVerificationHeader");
        expectations.expectMissing("customHeader");

        expectations.evaluate(headers);

    }

    /**
     *
     * Header Misconfiguration Series Test 4/4
     *
     * Tests that the configurations of the <headers> element are case insensitive.
     *
     * The test servlet will be invoked with the parameter:
     * [testCondition=singleHeader], which will add the response header: "customHeader:appValue".
     * This will also add a verification header, "appVerificationHeader".
     *
     * The "add" configuration can add multiple of the same name header, so will be ignored
     * for the purpose of this test, as casing is irrelevant.
     *
     * Test Phase 1: Misconfiguration case-insensitive detection
     *
     * This test will misconfigure the "set", "setIfMissing", and "remove" attributes with
     * the same header name with different casing. It expected that this will result with
     * the CWWKT0044W "previously detected duplicate header" message.
     *
     *
     * Test Phase 2: Case Insensitivity when <headers> configuration is consumed by transport
     *
     * The configuration will test the "set" attribute by configuring the servlet header in all
     * capital letters: "CUSTOMHEADER: testValue". It is expected that the resulting response
     * header that is written will be configured "set" header.
     *
     * The "setIfMissing" attribute will be tested by setting the verification header in all
     * capital letters: "APPVERIFICATIONHEADER: badValue". This header is added by the application
     * with no value, so it is expected that no value is found for it.
     *
     * The "remove" attribute will be tested by configuring the date header in all capital
     * letters: "DATE". The response is expected to not contain a "Date" header.
     *
     * Expected bad headers: [badHeader]
     * Expected headers: [CUSTOMHEADER: testValue, appVerificationHeader]
     * Expected removed header - [Date]
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testHeaderMisconfiguration_CaseInsensitivity() throws Exception {
        String testName = "testHeaderCaseInsensitivity";
        String url = generateURL("/ResponseHeadersServlet?testCondition=singleHeader");
        restoreSavedConfig = true;
        String stringToSearchFor = "CWWKT0044W";

        ServerConfiguration configuration = server.getServerConfiguration();
        Log.info(ME, testName, "Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getHeaders().setSet("badHeader, CUSTOMHEADER: testValue");
        httpEndpoint.getHeaders().setSetIfMissing("BaDHeAdEr, APPVERIFICATIONHEADER: testValue");
        httpEndpoint.getHeaders().setRemove("BADHEADER, DATE");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*ResponseHeadersTest.*");

        Log.info(ME, testName, "Updated server configuration: " + configuration);

        List<String> logs = server.findStringsInLogs(stringToSearchFor);

        assertTrue("Expected one occurances of the previously duplicated header string but found: " + logs.size(), logs.size() == 1);

        //Send the request and verify the expected headers
        Header[] headers = executeExchangeAndGetHeaders(url, testName);

        HeaderExpectations expectations = new HeaderExpectations();
        expectations.expectPresent("customHeader", "testValue");
        expectations.expectPresent("appVerificationHeader");
        expectations.expectMissing("date");

        expectations.evaluate(headers);

    }

    /**
     *
     * Simple Configuration Series 1/4: "add" attribute
     *
     * Tests that the "add" configuration of the <headers> element is able
     * to properly append multiple headers to the response that are configured
     * with the same name.
     *
     *
     * The application will add the [appVerificationHeader] header to
     * the response. No further application interaction is expected.
     *
     * Expected headers: [customHeader:testValue], [customHeader:testValue2]
     * [appVerificationHeader]
     *
     * @throws Exception
     */
    @Test
    public void testAddHeader() throws Exception {

        String url = generateURL("/ResponseHeadersServlet");
        String testName = "testAddHeader";
        restoreSavedConfig = true;

        ServerConfiguration configuration = server.getServerConfiguration();
        Log.info(ME, testName, "Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getHeaders().setAdd("customHeader:testValue, customHeader:testValue2");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*ResponseHeadersTest.*");

        Log.info(ME, testName, "Updated server configuration: " + configuration);

        //Send the request and verify the expected headers
        Header[] headers = executeExchangeAndGetHeaders(url, testName);

        HeaderExpectations expectations = new HeaderExpectations();
        expectations.expectPresent("customHeader", "testValue");
        expectations.expectPresent("customHeader", "testValue2");
        expectations.expectPresent("appVerificationHeader");

        expectations.evaluate(headers);
    }

    /**
     *
     * Simple Configuration Series 2/4: "set" attribute
     *
     * Tests that the "set" configuration of the <headers> element is able
     * to properly set the configured header on a response
     *
     *
     * The application will add the [appVerificationHeader] header to
     * the response. No further application interaction is expected.
     *
     * Expected headers: [customHeader:testValue], [appVerificationHeader]
     *
     * @throws Exception
     */
    @Test
    public void testSetHeader() throws Exception {

        String url = generateURL("/ResponseHeadersServlet");
        String testName = "testSetHeader";
        restoreSavedConfig = true;

        ServerConfiguration configuration = server.getServerConfiguration();
        Log.info(ME, testName, "Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getHeaders().setSet("customHeader:testValue");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*ResponseHeadersTest.*");

        Log.info(ME, testName, "Updated server configuration: " + configuration);

        //Send the request and verify the expected headers
        Header[] headers = executeExchangeAndGetHeaders(url, testName);

        HeaderExpectations expectations = new HeaderExpectations();
        expectations.expectPresent("customHeader", "testValue");
        expectations.expectPresent("appVerificationHeader");

        expectations.evaluate(headers);

    }

    /**
     *
     * Simple Configuration Series 3/4: "setIfMissing" attribute
     *
     * Tests that the "setIfMissing" configuration of the <headers> element is able
     * to properly set the configured header on a response when no other header
     * of the same name is present.
     *
     *
     * The application will add the [appVerificationHeader] header to
     * the response. No further application interaction is expected.
     *
     * Expected headers: [customHeader:testValue], [appVerificationHeader]
     *
     * @throws Exception
     */
    @Test
    public void testSetIfMissingHeader() throws Exception {

        String url = generateURL("/ResponseHeadersServlet");
        String testName = "testSetIfMissingHeader";
        restoreSavedConfig = true;

        ServerConfiguration configuration = server.getServerConfiguration();
        Log.info(ME, testName, "Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getHeaders().setSetIfMissing("customHeader:testValue");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*ResponseHeadersTest.*");

        Log.info(ME, testName, "Updated server configuration: " + configuration);

        //Send the request and verify the expected headers
        Header[] headers = executeExchangeAndGetHeaders(url, testName);

        HeaderExpectations expectations = new HeaderExpectations();
        expectations.expectPresent("customHeader", "testValue");
        expectations.expectPresent("appVerificationHeader");

        expectations.evaluate(headers);

    }

    /**
     *
     * Simple Configuration Series 4/4: "remove" attribute
     *
     * Tests that the "remove" configuration of the <headers> element is able
     * to properly remove the configured header from a response.
     *
     * The configuration will be set to remove any header whose name is [customHeader].
     *
     * The test servlet will be invoked with the parameter: [testCondition=singleHeader],
     * which will add the response header: "customHeader:appValue". The application will also
     * add the [appVerificationHeader] header to the response. No further application
     * interaction is expected.
     *
     * Expected headers: [appVerificationHeader]
     *
     * @throws Exception
     */
    @Test
    public void testRemoveHeader() throws Exception {

        String url = generateURL("/ResponseHeadersServlet");
        String testName = "testRemoveHeader";
        restoreSavedConfig = true;

        ServerConfiguration configuration = server.getServerConfiguration();
        Log.info(ME, testName, "Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getHeaders().setRemove("customHeader");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*ResponseHeadersTest.*");

        Log.info(ME, testName, "Updated server configuration: " + configuration);

        //Send the request and verify the expected headers
        Header[] headers = executeExchangeAndGetHeaders(url, testName);

        HeaderExpectations expectations = new HeaderExpectations();
        expectations.expectPresent("appVerificationHeader");
        expectations.expectMissing("customHeader");

        expectations.evaluate(headers);

    }

    /**
     *
     * Tests that the "add" configuration of the <headers> element is able
     * to properly append headers to the response without overwritting
     * existing headers that share the same name.
     *
     * The "testCondition=singleHeader" request parameter will result in the
     * application adding the header [customHeader:appValue].
     *
     * The application will also add the [appVerificationHeader] header to
     * the response.
     *
     * Expected headers: [customHeader:testValue], [customHeader:appValue]
     * [appVerificationHeader]
     *
     * @throws Exception
     */
    @Test
    public void testAddHeaderWithHeaderPresent() throws Exception {

        String url = generateURL("/ResponseHeadersServlet?testCondition=singleHeader");
        String testName = "testAddHeaderWithHeaderPresent";
        restoreSavedConfig = true;

        ServerConfiguration configuration = server.getServerConfiguration();
        Log.info(ME, testName, "Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getHeaders().setAdd("customHeader:testValue");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*ResponseHeadersTest.*");

        Log.info(ME, testName, "Updated server configuration: " + configuration);

        //Send the request and verify the expected headers
        Header[] headers = executeExchangeAndGetHeaders(url, testName);

        HeaderExpectations expectations = new HeaderExpectations();
        expectations.expectPresent("customHeader", "testValue");
        expectations.expectPresent("customHeader", "appValue");
        expectations.expectPresent("appVerificationHeader");

        expectations.evaluate(headers);

    }

    /**
     *
     * Tests that the "set" configuration of the <headers> element is able
     * to properly overwrite an exisiting header that shares its name with a configured
     * "set" header.
     *
     *
     * The configuration will define the header [customHeader:testValue] on the
     * on the "set" attribute.
     *
     * The test servlet will be invoked with the parameter: [testCondition=singleHeader],
     * which will add the response header: "customHeader:appValue". The application will also
     * add the [appVerificationHeader] header to the response. No further application
     * interaction is expected.
     *
     * It is expected that the the app [customHeader] be overwritten by the configuration.
     *
     * Expected headers: [customHeader:testValue], [appVerificationHeader]
     *
     * @throws Exception
     */
    @Test
    public void testSetHeaderWithHeaderPresent() throws Exception {

        String url = generateURL("/ResponseHeadersServlet?testCondition=singleHeader");
        String testName = "testSetHeaderWithHeaderPresent";
        restoreSavedConfig = true;

        ServerConfiguration configuration = server.getServerConfiguration();
        Log.info(ME, testName, "Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getHeaders().setSet("customHeader:testValue");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*ResponseHeadersTest.*");

        Log.info(ME, testName, "Updated server configuration: " + configuration);

        //Send the request and verify the expected headers
        Header[] headers = executeExchangeAndGetHeaders(url, testName);

        HeaderExpectations expectations = new HeaderExpectations();
        expectations.expectPresent("customHeader", "testValue");
        expectations.expectPresent("appVerificationHeader");
        expectations.expectMissing("customHeader", "appValue");

        expectations.evaluate(headers);

    }

    /**
     *
     * Tests that the "setIfMissing" configuration of the <headers> element does not
     * overwrite an exisiting header that shares its name with a configured
     * "setIfMissing" header.
     *
     *
     * The configuration will define the header [customHeader:testValue] on the
     * on the "setIfMissing" attribute.
     *
     * The test servlet will be invoked with the parameter: [testCondition=singleHeader],
     * which will add the response header: "customHeader:appValue". The application will also
     * add the [appVerificationHeader] header to the response. No further application
     * interaction is expected.
     *
     * It is expected that the the app [customHeader] will not be overwritten by the configuration.
     *
     * Expected headers: [customHeader:appValue], [appVerificationHeader]
     *
     * @throws Exception
     */
    @Test
    public void testSetIfMissingHeaderWithHeaderPresent() throws Exception {

        String url = generateURL("/ResponseHeadersServlet?testCondition=singleHeader");
        String testName = "testSetIfMissingHeaderWithHeaderPresent";
        restoreSavedConfig = true;

        ServerConfiguration configuration = server.getServerConfiguration();
        Log.info(ME, testName, "Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getHeaders().setSetIfMissing("customHeader:testValue");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*ResponseHeadersTest.*");

        Log.info(ME, testName, "Updated server configuration: " + configuration);

        //Send the request and verify the expected headers
        Header[] headers = executeExchangeAndGetHeaders(url, testName);

        HeaderExpectations expectations = new HeaderExpectations();
        expectations.expectPresent("customHeader", "appValue");
        expectations.expectPresent("appVerificationHeader");
        expectations.expectMissing("customHeader", "testValue");

        expectations.evaluate(headers);

    }

    /**
     *
     * Tests that the "remove" configuration of the <headers> element removes
     * multiple all instances of the defined header name.
     *
     * The configuration will use the "add" attribute to define two headers with the same name:
     * [customConfigHeader: testValue] and [customConfigHeader: testValue2].
     *
     * The test servlet will be invoked with the parameter: [testCondition=multipleHeaders],
     * which will add the response headers: "customHeader:appValue" and "customHeader:appValue2".
     * The application will also add the [appVerificationHeader] header to the response. No further
     * application interaction is expected.
     *
     *
     * Expected present headers: [appVerificationHeader]
     * Expected missing headers: [customConfigHeader], [customHeader]
     *
     * @throws Exception
     */
    @Test
    public void testRemoveMultipleAppHeader() throws Exception {

        String url = generateURL("/ResponseHeadersServlet?testCondition=multipleHeaders");
        String testName = "testRemoveMultipleAppHeaders";
        restoreSavedConfig = true;

        ServerConfiguration configuration = server.getServerConfiguration();
        Log.info(ME, testName, "Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getHeaders().setAdd("customConfigHeader:testValue, customConfigHeader:testValue2");
        httpEndpoint.getHeaders().setRemove("customHeader, customConfigHeader");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*ResponseHeadersTest.*");

        Log.info(ME, testName, "Updated server configuration: " + configuration);

        //Send the request and verify the expected headers
        Header[] headers = executeExchangeAndGetHeaders(url, testName);

        HeaderExpectations expectations = new HeaderExpectations();
        expectations.expectPresent("appVerificationHeader");
        expectations.expectMissing("customConfigHeader");
        expectations.expectMissing("customHeader");

        expectations.evaluate(headers);
    }

    /**
     *
     * Tests the "remove" configuration of the <headers> element by specifying
     * a header that will not be present in the response.
     *
     * The application will add the [appVerificationHeader] header to the response. No further
     * application interaction is expected.
     *
     * The header [undefinedHeader] will be configured in the remove option, but will not be
     * present on the response. A status 200 is expected with no [undefinedHeader] added
     * to the response.
     *
     * Expected present headers: [appVerificationHeader]
     * Expected missing headers: [undefinedHeader]
     *
     * @throws Exception
     */
    @Test
    public void testRemoveMissingHeader() throws Exception {

        String url = generateURL("/ResponseHeadersServlet");
        String testName = "testRemoveMissingHeader";
        restoreSavedConfig = true;

        ServerConfiguration configuration = server.getServerConfiguration();
        Log.info(ME, testName, "Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getHeaders().setRemove("undefinedHeader");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*ResponseHeadersTest.*");

        Log.info(ME, testName, "Updated server configuration: " + configuration);

        //Send the request and verify the expected headers
        Header[] headers = executeExchangeAndGetHeaders(url, testName);

        HeaderExpectations expectations = new HeaderExpectations();
        expectations.expectPresent("appVerificationHeader");
        expectations.expectMissing("undefinedHeader");

        expectations.evaluate(headers);
    }

    /**
     *
     * Cookie Header Test Series Test 1/4 : "add" attribute configuration
     *
     * Test the interactions of the <headers> configuration with response Cookie objects and "Set-Cookie"
     * headers.
     *
     * This test will set a request parameter [testCondition=testCookies], which will instruct the
     * servlet to add the following "Set-Cookie" header and Cookie object to the response:
     * response.addHeader("set-cookie", "chocolate=chip; SameSite=None");
     * response.addCookie(new Cookie("vanilla","sugar"));
     *
     * Cookie objects should be converted into "Set-Cookie" headers by the transport.
     *
     * The application will also add the header [appVerificationHeader].
     *
     * This test specifically tests the "add" configuration with a "set-cookie: oatmeal" header.
     * It expected that the response will write out all three chocolate, vanilla, and oatmeal cookies in the
     * form of "set-cookie" headers.
     *
     * Expected headers: [set-cookie: chocolate=chip; SameSite=None], [set-cookie: vanilla=sugar],
     * [set-cookie: oatmeal], [appVerificationHeader]
     *
     * @throws Exception
     */
    @Test
    public void testCookieHeaders_Add() throws Exception {

        String url = generateURL("/ResponseHeadersServlet?testCondition=testCookies");
        String testName = "testCookieHeaders_Add";
        restoreSavedConfig = true;

        //FIRST REQUEST - <add> configuration

        ServerConfiguration configuration = server.getServerConfiguration();
        Log.info(ME, testName, "Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getHeaders().setAdd("set-cookie:oatmeal");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*ResponseHeadersTest.*");

        Log.info(ME, testName, "Updated server configuration: " + configuration);

        //Send the request and verify the expected headers
        Header[] headers = executeExchangeAndGetHeaders(url, testName);

        HeaderExpectations expectations = new HeaderExpectations();
        expectations.expectPresent("set-cookie", "chocolate=chip; SameSite=None");
        expectations.expectPresent("set-cookie", "vanilla=sugar");
        expectations.expectPresent("set-cookie", "oatmeal");
        expectations.expectPresent("appVerificationHeader");

        expectations.evaluate(headers);

    }

    /**
     * Cookie Header Test Series Test 2/4 - "set" attribute configuration
     *
     * Test the interactions of the <headers> configuration with response Cookie objects and "Set-Cookie"
     * headers.
     *
     * This test will set a request parameter [testCondition=testCookies], which will instruct the
     * servlet to add the following "Set-Cookie" header and Cookie object to the response:
     * response.addHeader("set-cookie", "chocolate=chip; SameSite=None");
     * response.addCookie(new Cookie("vanilla","sugar"));
     *
     * Cookie objects should be converted into "Set-Cookie" headers by the transport.
     *
     * The application will also add the header [appVerificationHeader].
     *
     * This test specifically tests the "set" configuration with the "set-cookie: oatmeal" header. It is expected
     * that the servlet chocolate and vanilla values be overwriten.
     *
     * Expected present headers: [set-cookie: oatmeal], [appVerificationHeader]
     * Expected missing headers: [set-cookie: chocolate=chip; SameSite=None], [set-cookie: vanilla=sugar]
     *
     *
     * @throws Exception
     */
    @Test
    public void testCookieHeaders_Set() throws Exception {

        String url = generateURL("/ResponseHeadersServlet?testCondition=testCookies");
        String testName = "testCookieHeaders_Set";
        restoreSavedConfig = true;

        ServerConfiguration configuration = server.getServerConfiguration();
        Log.info(ME, testName, "Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getHeaders().setSet("set-cookie:oatmeal");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*ResponseHeadersTest.*");

        Log.info(ME, testName, "Updated server configuration: " + configuration);

        //Send the request and verify the expected headers
        Header[] headers = executeExchangeAndGetHeaders(url, testName);

        HeaderExpectations expectations = new HeaderExpectations();
        expectations.expectMissing("set-cookie", "chocolate=chip; SameSite=None");
        expectations.expectMissing("set-cookie", "vanilla=sugar");
        expectations.expectPresent("set-cookie", "oatmeal");
        expectations.expectPresent("appVerificationHeader");

        expectations.evaluate(headers);
    }

    /**
     * Cookie Header Test Series Test 3/4 - "setIfMissing" attribute configuration
     *
     * Test the interactions of the <headers> configuration with response Cookie objects and "Set-Cookie"
     * headers.
     *
     * This test will set a request parameter [testCondition=testCookies], which will instruct the
     * servlet to add the following "Set-Cookie" header and Cookie object to the response:
     * response.addHeader("set-cookie", "chocolate=chip; SameSite=None");
     * response.addCookie(new Cookie("vanilla","sugar"));
     *
     * Cookie objects should be converted into "Set-Cookie" headers by the transport.
     *
     * The application will also add the header [appVerificationHeader].
     *
     *
     * This test specifically tests the "setIfMissing" configuration with the "set-cookie: oatmeal" header. It is
     * expected that the response will be written out with both chocolate and vanilla "set-cookie" headers.
     *
     * Expected present headers: [set-cookie: chocolate=chip; SameSite=None], [set-cookie: vanilla=sugar],
     * [appVerificationHeader]
     * Expected missing headers: [set-cookie: oatmeal]
     *
     *
     * @throws Exception
     */
    @Test
    public void testCookieHeaders_SetIfMissing() throws Exception {

        String url = generateURL("/ResponseHeadersServlet?testCondition=testCookies");
        String testName = "testCookieHeaders_SetIfMissing";
        restoreSavedConfig = true;

        ServerConfiguration configuration = server.getServerConfiguration();
        Log.info(ME, testName, "Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getHeaders().setSetIfMissing("set-cookie:oatmeal");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*ResponseHeadersTest.*");
        Log.info(ME, testName, "Updated server configuration: " + configuration);

        //Send the request and verify the expected headers
        Header[] headers = executeExchangeAndGetHeaders(url, testName);

        HeaderExpectations expectations = new HeaderExpectations();
        expectations.expectPresent("set-cookie", "chocolate=chip; SameSite=None");
        expectations.expectPresent("set-cookie", "vanilla=sugar");
        expectations.expectMissing("set-cookie", "oatmeal");
        expectations.expectPresent("appVerificationHeader");

        expectations.evaluate(headers);

    }

    /**
     * Cookie Header Test Series Test 4/4 - "remove" attribute configuration
     *
     * Test the interactions of the <headers> configuration with response Cookie objects and "Set-Cookie"
     * headers.
     *
     * This test will set a request parameter [testCondition=testCookies], which will instruct the
     * servlet to add the following "Set-Cookie" header and Cookie object to the response:
     * response.addHeader("set-cookie", "chocolate=chip; SameSite=None");
     * response.addCookie(new Cookie("vanilla","sugar"));
     *
     * Cookie objects should be converted into "Set-Cookie" headers by the transport.
     *
     * The application will also add the header [appVerificationHeader].
     *
     * This specifically tests the "remove" configuration with "set-cookie". No "set-cookie" headers are
     * expected.
     *
     * Expected present headers: [appVerificationHeader]
     * Expected missing headers: [set-cookie: chocolate=chip; SameSite=None], [set-cookie: vanilla=sugar],
     * [set-cookie: oatmeal]
     *
     * @throws Exception
     */
    @Test
    public void testCookieHeaders_Remove() throws Exception {

        String url = generateURL("/ResponseHeadersServlet?testCondition=testCookies");
        String testName = "testCookieHeaders_Remove";
        restoreSavedConfig = true;

        ServerConfiguration configuration = server.getServerConfiguration();
        Log.info(ME, testName, "Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getHeaders().setRemove("set-cookie");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*ResponseHeadersTest.*");

        Log.info(ME, testName, "Updated server configuration: " + configuration);

        //Send the request and verify the expected headers
        Header[] headers = executeExchangeAndGetHeaders(url, testName);

        HeaderExpectations expectations = new HeaderExpectations();
        expectations.expectMissing("set-cookie");
        expectations.expectPresent("appVerificationHeader");

        expectations.evaluate(headers);

    }

    /**
     * Tests that the configuration is applied to all responses in the process of authenticating
     * an end-user to a secure application. The server configuration will configure the "add" attribute
     * with the header [foo:bar].
     *
     * First Request:
     * Expected response code: 302
     * Expected response header: [foo:bar]
     *
     * Second Request: Login Page
     * Expected response code: 200
     * Expected response header: [foo:bar]
     *
     * Third Request: Perform Login
     * Expected response code: 302
     * Expected response header: [foo:bar]
     *
     * Fourth Request: Application Servlet
     * Expected response code: 200
     * Expected response header: [foo:bar]
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testHeadersDuringLogin() throws Exception {

        String testName = "testHeadersDuringLogin";
        restoreSavedConfig = true;
        String expectedResponse = "Welcome to the SameSiteSecurityServlet!";
        Header expectedHeader = null;

        // Build and deploy the application that we need for this test
        ShrinkHelper.defaultApp(server, APP_NAME_SECURE_APP + ".war", "samesite.security.servlet");

        // Use the necessary server.xml for this test.
        ServerConfiguration configuration = server.getServerConfiguration();
        Log.info(ME, testName, "Server configuration that was saved: " + configuration);
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("serverConfigs/ResponseHeadersServer.xml");
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SECURE_APP), true, "CWWKT0016I:.*SameSiteSecurityTest.*");

        // Wait for LTPA key to be available to avoid CWWKS4000E
        // CWWKS4105I: LTPA configuration is ready after x seconds
        assertNotNull("CWWKS4105I LTPA configuration message not found.",
                      server.waitForStringInLogUsingMark("CWWKS4105I.*"));

        // CWWKO0219I: TCP Channel defaultHttpEndpoint-ssl has been started and is now listening for
        // requests on host * (IPv6) port 8020.
        assertNotNull("CWWKO0219I: TCP Channel defaultHttpEndpoint-ssl message was not found",
                      server.waitForStringInLogUsingMark("CWWKO0219I:.*defaultHttpEndpoint-ssl"));

        configuration = server.getServerConfiguration();
        Log.info(ME, testName, "Updated server configuration: " + configuration);

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME_SECURE_APP + "/SameSiteSecurityServlet";
        String userName = "user1";
        String password = "user1Login";
        String location;

        HttpGet getMethod = new HttpGet(url);

        // Drive the initial request.
        Log.info(ME, testName, "Initial Request: url = " + getMethod.getUri().toString() + " request method = " + getMethod);
        try (final CloseableHttpClient client = HttpClientBuilder.create().disableRedirectHandling().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                Log.info(ME, testName, "Initial request result: " + response.getReasonPhrase());
                Log.info(ME, testName, "Initial request page status code: " + response.getCode());
                assertEquals("The expected status code for the initial request was not returned: ",
                             302, response.getCode());

                String content = EntityUtils.toString(response.getEntity());

                Log.info(ME, testName, "Initial request content: " + content);

                expectedHeader = response.getFirstHeader("foo");
                assertNotNull("Initial request did not contain the configured header", expectedHeader);
                Log.info(ME, testName, "Initial request header name: " + expectedHeader.getName());
                Log.info(ME, testName, "Initial request header value: " + expectedHeader.getValue());

                assertTrue("Initial request did not have expected header", "foo".equalsIgnoreCase(expectedHeader.getName()) && "bar".equalsIgnoreCase(expectedHeader.getValue()));

                EntityUtils.consume(response.getEntity());

                // The initial request should result in a 302 status code so we need to
                // find where we're being redirected to and drive a request to that location
                // since we have disableRedirectHandling enabled. We should arrive at the login page.
                location = response.getHeader("location").getValue();
                Log.info(ME, testName, "Redirect to : " + location);
                getMethod = new HttpGet(location);
                try (final CloseableHttpResponse responseRedirect = client.execute(getMethod)) {
                    Log.info(ME, testName, "Form login page result: " + responseRedirect.getReasonPhrase());
                    Log.info(ME, testName, "Form login page status code: " + responseRedirect.getCode());
                    String contentRedirect = EntityUtils.toString(responseRedirect.getEntity());
                    Log.info(ME, testName, "Form login page content: " + contentRedirect);
                    EntityUtils.consume(responseRedirect.getEntity());

                    // Verify we get the form login JSP
                    assertEquals("The expected status code for the form login page was not returned: ",
                                 200, responseRedirect.getCode());
                    assertTrue("Did not find expected form login page: " + "Form Login Page",
                               contentRedirect.contains("Form Login Page"));

                    //Test header was added for Login Request
                    expectedHeader = null;
                    expectedHeader = responseRedirect.getFirstHeader("foo");
                    assertNotNull("Initial header did not contain the configured header", expectedHeader);
                    Log.info(ME, testName, "JSP Form Login request header name: " + expectedHeader.getName());
                    Log.info(ME, testName, "JSP Form Login request header value: " + expectedHeader.getValue());
                    assertTrue("JSP Form Login request did not have custom header",
                               "foo".equalsIgnoreCase(expectedHeader.getName()) && "bar".equalsIgnoreCase(expectedHeader.getValue()));

                }
            }

            // Perform the login now.
            Log.info(ME, testName, "Perform FormLogin: url=" + url +
                                   " user=" + userName + " password=" + password);

            // Post method to login
            HttpPost postMethod = new HttpPost(url + "/j_security_check");
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("j_username", userName));
            nvps.add(new BasicNameValuePair("j_password", password));
            postMethod.setEntity(new UrlEncodedFormEntity(nvps));

            try (final CloseableHttpResponse response = client.execute(postMethod)) {
                Log.info(ME, testName, "Post Method response code: " + response.getCode());
                assertEquals("The expected form login status code was not returned: ", 302,
                             response.getCode());

                //Test header was added for Login Request
                expectedHeader = null;
                expectedHeader = response.getFirstHeader("foo");
                assertNotNull("Initial header did not contain the configured header", expectedHeader);
                Log.info(ME, testName, "Login request header name: " + expectedHeader.getName());
                Log.info(ME, testName, "Login request header value: " + expectedHeader.getValue());
                assertTrue("Login request did not have custom header", "foo".equalsIgnoreCase(expectedHeader.getName()) && "bar".equalsIgnoreCase(expectedHeader.getValue()));

                // The Post request should result in a redirect to the servlet. Save the location Header
                // so we can drive the final authenticated request to the servlet.
                Header header = response.getFirstHeader("Location");
                location = header.getValue();
                Log.info(ME, testName, "Redirect location: " + location);

                EntityUtils.consume(response.getEntity());

                assertEquals("Redirect location was not the original URL: ",
                             url, location);
            }

            // Drive the request to the Servlet.
            getMethod = new HttpGet(location);

            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                Log.info(ME, testName, "getMethod status: " + response.getReasonPhrase());
                assertEquals("The expected status code was not returned: ",
                             200, response.getCode());

                //Test header was added for Servlet request
                expectedHeader = null;
                expectedHeader = response.getFirstHeader("foo");
                assertNotNull("Servlet request did not contain the configured header", expectedHeader);
                Log.info(ME, testName, "Servlet reques header name: " + expectedHeader.getName());
                Log.info(ME, testName, "Servlet request header value: " + expectedHeader.getValue());
                assertTrue("Servlet request did not have custom header", "foo".equalsIgnoreCase(expectedHeader.getName()) && "bar".equalsIgnoreCase(expectedHeader.getValue()));

                String content = EntityUtils.toString(response.getEntity());
                Log.info(ME, testName, "Servlet content: " + content);

                EntityUtils.consume(response.getEntity());

                assertTrue("Response did not contain expected response: " + expectedResponse,
                           content.equals(expectedResponse));
            } finally {
                server.removeInstalledAppForValidation(APP_NAME_SECURE_APP);
            }
        }

    }

    /**
     * Tests that the configuration is applied to a response that results on a 404 status due to
     * the context root not being found. The configuration will define a single [addHeader] under the
     * "add" option.
     *
     * Expected present header: [addHeader]
     * Expected response status: 404
     *
     * @throws Exception
     */
    @Test
    public void testHeadersNoContextRoot() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/NoContextRoot";
        String testName = "testHeadersNoContextRoot";
        restoreSavedConfig = true;

        ServerConfiguration configuration = server.getServerConfiguration();
        Log.info(ME, testName, "Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getHeaders().setAdd("addHeader");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*ResponseHeadersTest.*");

        Log.info(ME, testName, "Updated server configuration: " + configuration);

        //Send the request and verify the expected headers
        Header[] headers = executeExchangeAndGetHeaders(url, testName, 404);

        HeaderExpectations expectations = new HeaderExpectations();
        expectations.expectPresent("addHeader");

        expectations.evaluate(headers);
    }

    /**
     * Utility class to quickly add header expecations throughout the FAT
     */
    private class HeaderExpectations {

        //Hashcode representation of a header key-value pair that is expected
        //to be present.
        List<Integer> expectedPresentHeaders = new ArrayList<Integer>();
        //Hashcode representation of a header key-value pair that is expected
        //to not be present.
        List<Integer> expectedMissingHeaders = new ArrayList<Integer>();
        //Haschode representation of a header name that is expected to
        //be not be present.
        List<Integer> expectedMissingHeaderNames = new ArrayList<Integer>();

        private HeaderExpectations() {
        };

        private void expectPresent(String headerName) {
            expectPresent(headerName, null);
        }

        /**
         * Used to define a response header key-value pair that must be present
         * on the list of response headers. The header name is case-insensitive;
         * the value is case-sensitive.
         */
        private void expectPresent(String headerName, String headerValue) {
            if (headerName == null || headerName.isEmpty()) {
                return;
            }
            if (headerValue == null) {
                headerValue = "";
            }

            String normalizedHeader = headerName.toLowerCase().trim() + ": " + headerValue.trim();
            int headerHash = normalizedHeader.hashCode();

            expectedPresentHeaders.add(headerHash);

        }

        /**
         * Used to define a response header name that must not be present on the list
         * of response headers, regardless of what value the header has. The header name is
         * case-insensitive.
         */
        private void expectMissing(String headerName) {
            if (headerName == null || headerName.isEmpty()) {
                return;
            }

            int headerHash = headerName.toLowerCase().trim().hashCode();

            if (!expectedMissingHeaderNames.contains(headerHash)) {
                expectedMissingHeaderNames.add(headerHash);
            }
        }

        /**
         * Used to define a response header key-value pair that is expected to not
         * be present on the list of response headers. The header name is case-insensitive;
         * the value is case-sensitive.
         *
         * For instance, expectMissing("foo", "bar") implies that the header
         * [foo] or [foo:anotherBar] may be present in the list of response headers.
         *
         * Not to be confused with expectMissing(headerName), which expects
         * no header, disregarding the value, that has the configured name to
         * be present on the list of response headers.
         *
         */
        private void expectMissing(String headerName, String headerValue) {
            if (headerName == null || headerName.isEmpty()) {
                return;
            }

            if (headerValue == null) {
                headerValue = "";
            }

            String normalizedHeader = headerName.toLowerCase().trim() + ": " + headerValue.trim();
            int headerHash = normalizedHeader.hashCode();

            if (!expectedMissingHeaders.contains(headerHash)) {
                expectedMissingHeaders.add(headerHash);
            }
        }

        /**
         * Evaluates that all expecations are met
         */
        private void evaluate(Header[] headers) {

            String testName = "HeaderExpectations.evaluate";
            String formattedHeader = null;
            int headerHash = -1;

            for (Header h : headers) {
                formattedHeader = h.getName().toLowerCase().trim() + ": " + h.getValue().trim();
                headerHash = formattedHeader.hashCode();

                //Assert this is not a header name that is expected to be missing
                assertTrue("Found header that was expected to be missing: [" + h.getName() + "]", !expectedMissingHeaderNames.contains(headerHash));
                //Assert this is not a header key-value pair that is expected to be missing
                assertTrue("Found header that was expected to be missing: [" + h.getName() + ": " + h.getValue() + "]", !expectedMissingHeaders.contains(headerHash));

                if (expectedPresentHeaders.contains(headerHash)) {
                    Log.info(ME, testName, "Found expected header [" + h.getName() + ": " + h.getValue() + "]");
                    expectedPresentHeaders.remove(Integer.valueOf(headerHash));
                }

                //For debugging, trace untracked headers
                else {
                    Log.info(ME, testName, "Found untracked header [" + h.getName() + ": " + h.getValue() + "]");
                }
            }

            //If all expected headers were found, the expectations list should be empty
            assertTrue("Not all expected headers were found.", expectedPresentHeaders.isEmpty());

        }

    }

}
