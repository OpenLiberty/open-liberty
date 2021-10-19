/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.Logging;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
@RunWith(FATRunner.class)
public class CustomAccessLogFieldsTest {

    private static Class<?> c = CustomAccessLogFieldsTest.class;

    private static final String MESSAGE_LOG = "logs/messages.log";
    private static final long LOG_TIMEOUT = 30 * 1000;
    private static final int WAIT_TIMEOUT = 15 * 1000;

    @Server("CustomAccessLogFieldsEnv")
    public static LibertyServer envServer;

    @Server("CustomAccessLogFieldsBootstrap")
    public static LibertyServer bootstrapServer;

    @Server("CustomAccessLogFieldsXml")
    public static LibertyServer xmlServer;

    // We need a server that has mpMetrics enabled so we can hit the /metrics endpoint for the remoteUserID field
    @Server("CustomAccessLogFieldsXmlWithMetrics")
    public static LibertyServer xmlServerWithMetrics;

    @Server("CustomAccessLogFieldsBadConfigEnv")
    public static LibertyServer badConfigServerEnv;

    @Server("CustomAccessLogFieldsBadConfigBootstrap")
    public static LibertyServer badConfigServerBootstrap;

    @Server("CustomAccessLogFieldsBadConfigXml")
    public static LibertyServer badConfigServerXml;

    @Server("CustomAccessLogFieldsChangeConfig")
    public static LibertyServer changeConfigServer;

    @Server("CustomAccessLogFieldsTwoHttpEndpoints")
    public static LibertyServer multipleHttpEndpointServer;

    private final String[] newFields = { "ibm_remoteIP", "ibm_bytesSent", "ibm_cookie", "ibm_requestElapsedTime", "ibm_requestHeader",
                                         "ibm_responseHeader", "ibm_requestFirstLine", "ibm_requestStartTime", "ibm_accessLogDatetime", "ibm_remoteUserID", "ibm_remotePort" };

    private final String[] defaultFields = { "ibm_remoteHost", "ibm_requestProtocol", "ibm_requestHost", "ibm_bytesReceived", "ibm_requestMethod", "ibm_requestPort",
                                             "ibm_queryString", "ibm_elapsedTime", "ibm_responseCode", "ibm_uriPath", "ibm_userAgent" };

    // We need to verify that the security prerequisites are fulfilled before hitting the /metrics secure endpoint
    // xmlServerWithMetrics is reused multiple times, but we only need to wait for the security pre-reqs once
    private static boolean isFirstTimeUsingXmlServerWithMetrics = true;

    private static LibertyServer serverInUse; // hold on to the server currently used so cleanUp knows which server to stop

    @BeforeClass
    public static void initialSetup() throws Exception {
        trustAll();
        envServer.saveServerConfiguration();
        bootstrapServer.saveServerConfiguration();
        xmlServer.saveServerConfiguration();
        xmlServerWithMetrics.saveServerConfiguration();
        badConfigServerEnv.saveServerConfiguration();
        badConfigServerBootstrap.saveServerConfiguration();
        badConfigServerXml.saveServerConfiguration();
        changeConfigServer.saveServerConfiguration();
        multipleHttpEndpointServer.saveServerConfiguration();
    }

    public void setUp(LibertyServer server) throws Exception {
        serverInUse = server;
        if (server != null && !server.isStarted()) {
            server.restoreServerConfiguration();
            server.startServer();
        }
    }

    private static void trustAll() throws Exception {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(
                            null,
                            new TrustManager[] {
                                                 new X509TrustManager() {
                                                     @Override
                                                     public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                                                     }

                                                     @Override
                                                     public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                                                     }

                                                     @Override
                                                     public X509Certificate[] getAcceptedIssuers() {
                                                         return null;
                                                     }
                                                 }
                            },
                            new SecureRandom());
            SSLContext.setDefault(sslContext);
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        } catch (Exception e) {
            Log.error(c, "trustAll", e);
        }
    }

    @After
    public void cleanUp() throws Exception {
        if (serverInUse != null && serverInUse.isStarted()) {
            serverInUse.stopServer("com.ibm.ws.logging.fat.ffdc.servlet.FFDCServlet.doGet", "ArithmeticException",
                                   "CWWKG0081E", "CWWKG0083W");
        }
    }

    /*
     * This test sets the "WLP_ENABLE_CUSTOM_ACCESS_LOG_FIELDS" attribute in the server.env and verifies the property in the messages.log file.
     */
    @Test
    public void testAccessLogFieldNamesEnv() throws Exception {
        setUp(envServer);
        waitForSecurityPrerequisites(envServer, WAIT_TIMEOUT);
        waitForMetricsToStart(envServer, WAIT_TIMEOUT);
        hitHttpsEndpointSecure("/metrics", envServer);
        String line = envServer.waitForStringInLog("liberty_accesslog.*/metrics");
        assertNotNull("No liberty_accesslog found in the output JSON log.", line);

        assertTrue("There are fields missing in the output JSON log.", areFieldsPresent(line, newFields));
        assertTrue("There are unexpected fields in the output JSON log.", areFieldsNotPresent(line, defaultFields));
    }

    /*
     * This test sets the "com.ibm.ws.logging.json.access.log.fields" attribute in the bootstrap.properties and verifies the property in the messages.log file.
     */
    @Test
    public void testAccessLogFieldNamesBootstrap() throws Exception {
        setUp(bootstrapServer);
        waitForSecurityPrerequisites(bootstrapServer, WAIT_TIMEOUT);
        waitForMetricsToStart(bootstrapServer, WAIT_TIMEOUT);
        hitHttpsEndpointSecure("/metrics", bootstrapServer);
        String line = bootstrapServer.waitForStringInLog("liberty_accesslog.*/metrics");
        assertNotNull("No liberty_accesslog found in the output JSON log.", line);

        assertTrue("There are fields missing in the output JSON log.", areFieldsPresent(line, newFields));
        assertTrue("There are unexpected fields in the output JSON log.", areFieldsNotPresent(line, defaultFields));
    }

    /*
     * This test sets the "logFormat" attribute in the server.xml and verifies the property in the messages.log file.
     */
    @Test
    public void testAccessLogFieldNamesXml() throws Exception {
        setUp(xmlServerWithMetrics);
        if (isFirstTimeUsingXmlServerWithMetrics) {
            waitForSecurityPrerequisites(xmlServerWithMetrics, WAIT_TIMEOUT);
        }
        waitForMetricsToStart(xmlServerWithMetrics, WAIT_TIMEOUT);

        isFirstTimeUsingXmlServerWithMetrics = false;
        hitHttpsEndpointSecure("/metrics", xmlServer);
        String line = xmlServerWithMetrics.waitForStringInLog("liberty_accesslog.*/metrics");
        assertNotNull("No liberty_accesslog found in the output JSON log.", line);

        assertTrue("There are fields missing in the output JSON log.", areFieldsPresent(line, newFields));
        assertTrue("There are unexpected fields in the output JSON log.", areFieldsNotPresent(line, defaultFields));
    }

    /*
     * This test checks that "CWWKG0032W" is printed if the jsonAccessLogFields property is not set correctly.
     */
    @Test
    public void testAccessLogFaultyConfig() throws Exception {
        // First, server.env
        badConfigServerEnv.startServer();
        List<String> lines = badConfigServerEnv.findStringsInFileInLibertyServerRoot("CWWKG0032W", MESSAGE_LOG);
        assertNotNull("The error message CWWKG0032W was not sent from faulty configuration in the server env.", lines);
        badConfigServerEnv.stopServer();

        // next, bootstrap.properties
        badConfigServerBootstrap.startServer();
        lines = badConfigServerBootstrap.findStringsInFileInLibertyServerRoot("CWWKG0032W", MESSAGE_LOG);
        assertNotNull("The error message CWWKG0032W was not sent from faulty configuration in the bootstrap.properties.", lines);
        badConfigServerBootstrap.stopServer();

        // Finally, server.xml
        badConfigServerXml.startServer();
        lines = badConfigServerXml.findStringsInFileInLibertyServerRoot("CWWKG0032W", MESSAGE_LOG);
        assertNotNull("The error message CWWKG0032W was not sent from faulty configuration in the server xml.", lines);
        badConfigServerXml.stopServer();
    }

    @Test
    public void testRenameAccessLogField() throws Exception {
        setUp(xmlServer);
        setServerConfigurationForRename("ibm_cookie_cookie:rename_cookie,ibm_requestHeader_header:rename_requestHeader,ibm_responseHeader_Content-Type:rename_responseHeader",
                                        xmlServer);
        hitHttpEndpoint(xmlServer);
        String line = xmlServer.waitForStringInLog("liberty_accesslog");
        assertNotNull("No liberty_accesslog found in the output JSON log.", line);

        String[] renamedNames = { "rename_cookie", "rename_requestHeader", "rename_responseHeader" };
        assertTrue("Access log fields were not renamed properly.", areFieldsPresent(line, renamedNames));

    }

    @Test
    public void testOmitAccessLogField() throws Exception {
        setUp(xmlServer);
        setServerConfigurationForRename("ibm_cookie_cookie:,ibm_requestHeader_header:,ibm_responseHeader_Content-Type:",
                                        xmlServer);
        hitHttpEndpoint(xmlServer);
        String line = xmlServer.waitForStringInLog("liberty_accesslog");
        assertNotNull("No liberty_accesslog found in the output JSON log.", line);

        String[] omittedFields = { "ibm_cookie_cookie", "ibm_requestHeader_header", "ibm_responseHeader_Content-Type" };
        assertTrue("Access log fields were not omitted properly.", areFieldsNotPresent(line, omittedFields));
    }

    /*
     * Test that all possible logFormat fields will be printed to JSON logs.
     */
    @Test
    public void testAllFieldsArePrinted() throws Exception {
        setUp(xmlServerWithMetrics);
        xmlServerWithMetrics.setServerConfigurationFile("accessLogging/server-all-fields.xml");
        waitForConfigUpdate(xmlServerWithMetrics);
        if (isFirstTimeUsingXmlServerWithMetrics) {
            waitForSecurityPrerequisites(xmlServerWithMetrics, WAIT_TIMEOUT);
        }
        waitForMetricsToStart(xmlServerWithMetrics, WAIT_TIMEOUT);

        isFirstTimeUsingXmlServerWithMetrics = false;

        hitHttpsEndpointSecure("/metrics", xmlServerWithMetrics);
        String line = xmlServerWithMetrics.waitForStringInLog("liberty_accesslog.*/metrics");
        assertNotNull("No liberty_accesslog found in the output JSON log.", line);

        // Easier to just use two asserts instead of trying to join those two arrays together
        assertTrue("There are fields missing in the output JSON log.", areFieldsPresent(line, newFields));
        assertTrue("There are fields missing in the output JSON log.", areFieldsPresent(line, defaultFields));
    }

    /*
     * Test that null values will not throw an error, but will just print nothing in the JSON logs.
     * Anything that shows up as a "-" in the http_access.log should not print here.
     */
    @Test
    public void testNullValuesDontPrintInJSON() throws Exception {
        setUp(xmlServer);
        hitHttpEndpoint(xmlServer);
        xmlServer.setServerConfigurationFile("accessLogging/server-null-values-dont-print.xml");
        waitForConfigUpdate(xmlServer);
        String line = xmlServer.waitForStringInLog("liberty_accesslog");
        assertNotNull("No liberty_accesslog found in the output JSON log.", line);

        // Since we hit the httpEndpoint and *not* the /metrics endpoint, the value for "remoteUserID" should be null, since we did not login.
        String[] nullFields = { "ibm_cookie_invalidcookie", "ibm_requestHeader_invalidheader", "ibm_responseHeader_invalidheader", "ibm_remoteUserID" };
        assertTrue("Null access log fields should not be printed, but null field was found.", areFieldsNotPresent(line, nullFields));
    }

    /*
     * Test that logFormat = default will not print out any of the new fields
     */
    @Test
    public void testDefaultWillNotPrintNewFields() throws Exception {
        setUp(xmlServer);
        setServerConfiguration("default", xmlServer);
        hitHttpEndpoint(xmlServer);
        String line = xmlServer.waitForStringInLog("liberty_accesslog");
        assertNotNull("No liberty_accesslog found in the output JSON log.", line);

        assertTrue("There are unexpected fields in the output JSON log.", areFieldsNotPresent(line, newFields));
        assertTrue("There are fields missing when logFormat = default.", areFieldsPresent(line, defaultFields));
    }

    /*
     * Test that the fields in the access log print the same value in the JSON logs.
     */
    @Test
    public void testFieldsInAccessLogAreSameInJSON() throws Exception {
        setUp(xmlServerWithMetrics);
        if (isFirstTimeUsingXmlServerWithMetrics) {
            waitForSecurityPrerequisites(xmlServerWithMetrics, WAIT_TIMEOUT);
        }
        waitForMetricsToStart(xmlServerWithMetrics, WAIT_TIMEOUT);

        isFirstTimeUsingXmlServerWithMetrics = false;

        hitHttpsEndpointSecure("/metrics", xmlServerWithMetrics);
        String line = xmlServerWithMetrics.waitForStringInLog("liberty_accesslog.*/metrics");
        assertNotNull("No liberty_accesslog found in the output JSON log.", line);

        // create a map of the strings
        Map<String, String> parsedLine = parseIntoKvp(line);

        // We don't care about the non-access log fields, e.g. type, ibm_userDir, so let's remove them
        // The http_access.log will not have any of these values
        Set<String> unwantedFields = new HashSet<String>();
        unwantedFields.add("type");
        unwantedFields.add("host");
        unwantedFields.add("ibm_serverName");
        unwantedFields.add("ibm_userDir");
        unwantedFields.add("ibm_datetime");
        unwantedFields.add("ibm_sequence");
        // The following two fields are formatted differently in JSON compared to the http_access.log
        unwantedFields.add("ibm_accessLogDatetime");
        unwantedFields.add("ibm_requestStartTime");
        parsedLine.keySet().removeAll(unwantedFields);

        // unfortunately, our JSON log isn't ordered like the http access logs
        // easier to check that the value shows up in the http_access.log at some point; for duplicate values, let's just remove it with each pass-through.

        String accessLog = xmlServerWithMetrics.getServerRoot() + "/logs/http_access.log";
        String accessLogLine = readFile(accessLog);
        // Try to read the file again once every second for WAIT_TIMEOUT if the file is still empty
        long startTime = System.currentTimeMillis();
        while ((accessLogLine == null || accessLogLine.isEmpty()) && ((System.currentTimeMillis() - startTime) < WAIT_TIMEOUT)) {
            Thread.sleep(1000);
            accessLogLine = readFile(accessLog);
        }
        assertNotNull("The http_access.log file is empty or could not be read.", accessLogLine);
        assertFalse("The http_access.log file is empty.", accessLogLine.isEmpty());

        for (String s : parsedLine.values()) {
            if (accessLogLine.contains(s)) {
                // let's remove it, in case there's a duplicate value for a different field
                accessLogLine.replace(s, "");
            } else {
                // If we didn't find the value, then the JSON log is missing something or has an incorrect value
                fail("There's a value mismatch between the " + s + " JSON log value and the http_access.log value.");
            }
        }
    }

    /*
     * Test that we can change the value of jsonAccessLogFields on-the-fly.
     */
    @Test
    public void testChangeJsonAccessLogFieldsConfigValue() throws Exception {
        setUp(changeConfigServer);
        waitForSecurityPrerequisites(changeConfigServer, WAIT_TIMEOUT);
        waitForMetricsToStart(changeConfigServer, WAIT_TIMEOUT);

        hitHttpsEndpointSecure("/metrics", changeConfigServer);
        String line = changeConfigServer.waitForStringInLogUsingMark("liberty_accesslog.*/metrics");
        assertNotNull("No liberty_accesslog found in the output JSON log.", line);

        assertTrue("There are fields missing in the output JSON log.", areFieldsPresent(line, newFields));
        assertTrue("There are unexpected fields in the output JSON log.", areFieldsNotPresent(line, defaultFields));

        // change to default
        setServerConfiguration("default", changeConfigServer);
        changeConfigServer.setMarkToEndOfLog();

        hitHttpsEndpointSecure("/metrics", changeConfigServer);
        line = changeConfigServer.waitForStringInLogUsingMark("liberty_accesslog.*/metrics");
        assertNotNull("No liberty_accesslog found in the output JSON log.", line);
        changeConfigServer.setMarkToEndOfLog();

        // this time, make sure that the default fields show up but NOT the new fields
        assertTrue("There are unexpected fields in the output JSON log.", areFieldsNotPresent(line, newFields));
        assertTrue("There are fields missing in the output JSON log.", areFieldsPresent(line, defaultFields));

        // change back again to logFormat
        setServerConfiguration("logFormat", changeConfigServer);
        hitHttpsEndpointSecure("/metrics", changeConfigServer);
        line = changeConfigServer.waitForStringInLogUsingMark("liberty_accesslog.*/metrics");
        assertNotNull("No liberty_accesslog found in the output JSON log.", line);
        changeConfigServer.setMarkToEndOfLog();

        assertTrue("There are fields missing in the output JSON log.", areFieldsPresent(line, newFields));
        assertTrue("There are unexpected fields in the output JSON log.", areFieldsNotPresent(line, defaultFields));
    }

    /*
     * Test that removing the accessLogging attribute will stop JSON access logs too.
     */
    @Test
    public void testDisableAccessLog() throws Exception {
        setUp(xmlServer);
        // Make sure that it initially prints something out while the accessLogging attribute is still enabled
        hitHttpEndpoint(xmlServer);
        assertNotNull("No liberty_accesslog found in the messages.log.", xmlServer.waitForStringInLogUsingMark("liberty_accesslog"));
        xmlServer.setMarkToEndOfLog();

        xmlServer.setServerConfigurationFile("accessLogging/server-disable-accesslog.xml");
        waitForConfigUpdate(xmlServer);
        hitHttpEndpoint(xmlServer);
        assertNull("Disabling the accessLogging attribute did not stop JSON access logs from being printed.",
                   xmlServer.waitForStringInLogUsingMark("liberty_accesslog", WAIT_TIMEOUT));
    }

    /*
     * Test that adding the accessLogging attribute after server started will have JSON access logs print.
     */
    @Test
    public void testEnableAccessLog() throws Exception {
        setUp(xmlServer);
        // The server should have accessLogging disabled to begin with
        xmlServer.setServerConfigurationFile("accessLogging/server-disable-accesslog.xml");
        waitForConfigUpdate(xmlServer);
        // Make sure that it doesn't print access logs without the attribute enabled first
        hitHttpEndpoint(xmlServer);
        assertNull("There were JSON access logs printed when the accessLogging attribute was disabled.", xmlServer.waitForStringInLogUsingMark("liberty_accesslog", WAIT_TIMEOUT));
        xmlServer.setMarkToEndOfLog();

        // the original configuration actually had accessLogging enabled - so let's revert it back to the original
        xmlServer.restoreServerConfiguration();
        waitForConfigUpdate(xmlServer);
        hitHttpEndpoint(xmlServer);
        assertNotNull("JSON access logs were not printed after enabling the accessLogging attribute.", xmlServer.waitForStringInLogUsingMark("liberty_accesslog"));
    }

    /*
     * Test that an invalid logFormat token will throw an FFDC but not interrupt the processing of correct keys.
     */
    @Test
    @ExpectedFFDC("java.lang.IllegalArgumentException")
    public void testInvalidTokens() throws Exception {
        setUp(xmlServer);
        xmlServer.setServerConfigurationFile("accessLogging/server-invalid-tokens.xml");
        waitForConfigUpdate(xmlServer);
        hitHttpEndpoint(xmlServer);
        // An FFDC should be created for the bad token, but we should still have logs printing for the valid ones
        // In this case, our logFormat="%a %b %J" and only %J is invalid, so we're expecting the other tokens to be printed
        String line = xmlServer.waitForStringInLogUsingMark("liberty_accesslog");
        assertNotNull("No liberty_accesslog found in the output JSON log.", line);

        String[] expectedFields = { "ibm_remoteIP", "ibm_bytesSent" };
        assertTrue("The JSON access log was not printed properly and is missing fields.", areFieldsPresent(line, expectedFields));
    }

    /*
     * Test to check if there are duplicate tokens in the logFormat, the JSON logs only print the token once, not multiple times.
     */
    @Test
    public void testDuplicateTokensInLogFormat() throws Exception {
        setUp(xmlServer);
        xmlServer.setServerConfigurationFile("accessLogging/server-duplicate-keys.xml");
        waitForConfigUpdate(xmlServer);
        hitHttpEndpoint(xmlServer);
        String line = xmlServer.waitForStringInLogUsingMark("liberty_accesslog");
        assertNotNull("No liberty_accesslog found in the output JSON log.", line);

        String[] expectedFields = { "ibm_remoteIP", "ibm_cookie_cookie" };
        // First, make sure they're even printed at all
        assertTrue("There are fields missing in the output JSON log.", areFieldsPresent(line, expectedFields));
        // Then, make sure they're not printed twice
        assertFalse("The JSON access log was not printed properly and is repeating fields.", areFieldsRepeated(line, expectedFields));
    }

    /*
     * Test to verify that jsonAccessLogFields=default exhibits the same behaviour as manually specifying logFormat='%h %H %A %B %m %p %q %{R}W %s %U %{User-Agent}i'.
     */
    @Test
    public void testDefaultIsSameAsOriginal() throws Exception {
        setUp(xmlServer);
        // First, let's check that all the fields are present if we manually specify each field in the accessLogging logFormat property
        xmlServer.setServerConfigurationFile("accessLogging/server-default-in-logformat.xml");
        waitForConfigUpdate(xmlServer);
        hitHttpEndpoint(xmlServer);
        String lineManual = xmlServer.waitForStringInLogUsingMark("liberty_accesslog");
        assertNotNull("No liberty_accesslog found in the output JSON log.", lineManual);

        assertTrue("The JSON access log was not printed properly and is missing fields.", areFieldsPresent(lineManual, defaultFields));

        // Now, switch to jsonAccessLogFields = default and do the same check
        setServerConfiguration("default", xmlServer);
        waitForConfigUpdate(xmlServer);
        hitHttpEndpoint(xmlServer);
        String lineDefault = xmlServer.waitForStringInLogUsingMark("liberty_accesslog");
        assertNotNull("No liberty_accesslog found in the output JSON log.", lineDefault);

        assertTrue("The JSON access log was not printed properly and is missing fields.", areFieldsPresent(lineDefault, defaultFields));

        // Finally, check that they both have the same fields
        HashMap<String, String> lineManualMap = parseIntoKvp(lineManual);
        HashMap<String, String> lineDefaultMap = parseIntoKvp(lineDefault);
        assertTrue("There is a mismatch in fields.", lineManualMap.keySet().containsAll(lineDefaultMap.keySet()));
    }

    /*
     * Test that only one SetterFormatter is created per configuration
     * Each endpoint should have its own SetterFormatter because they both have different logFormats.
     */
    @Test
    public void testMultipleHttpEndpoints() throws Exception {
        setUp(multipleHttpEndpointServer);
        hitHttpEndpoint(multipleHttpEndpointServer);
        assertNotNull("SetterFormatter was not created properly on default http endpoint.",
                      multipleHttpEndpointServer.waitForStringInTraceUsingMark("createSetterFormatter Entry"));
        assertNotNull("SetterFormatter was not created properly on default http endpoint.",
                      multipleHttpEndpointServer.waitForStringInTraceUsingMark("createSetterFormatter Exit"));
        multipleHttpEndpointServer.setTraceMarkToEndOfDefaultTrace();

        // Hit the secondary endpoint and check that the SetterFormatter was created
        hitHttpEndpointSecondary(multipleHttpEndpointServer);
        assertNotNull("SetterFormatter was not created properly on secondary http endpoint.",
                      multipleHttpEndpointServer.waitForStringInTraceUsingMark("createSetterFormatter Entry"));
        assertNotNull("SetterFormatter was not created properly on secondary http endpoint.",
                      multipleHttpEndpointServer.waitForStringInTraceUsingMark("createSetterFormatter Exit"));
        multipleHttpEndpointServer.setTraceMarkToEndOfDefaultTrace();

        // Hit the default endpoint again to make sure the SetterFormatter does not get re-created
        hitHttpEndpoint(multipleHttpEndpointServer);
        assertNull("SetterFormatter was found in trace log for default http endpoint, but should not have been created a second time.",
                   multipleHttpEndpointServer.waitForStringInTraceUsingMark("createSetterFormatter", WAIT_TIMEOUT));
        multipleHttpEndpointServer.setTraceMarkToEndOfDefaultTrace();

        // Hit the secondary endpoint again to make sure the SetterFormatter does not get re-created
        hitHttpEndpoint(multipleHttpEndpointServer);
        assertNull("SetterFormatter was found in trace log for secondary http endpoint, but should not have been created a second time.",
                   multipleHttpEndpointServer.waitForStringInTraceUsingMark("createSetterFormatter", WAIT_TIMEOUT));
    }

    /*
     * Test port specifications such as %p and %{remote}p
     */
    @Test
    public void testDifferentPortSpecifications() throws Exception {
        setUp(xmlServer);

        // Test %p and %{remote}p in both orders
        runPortsTest("accessLogging/server-logformat-ports.xml");
        runPortsTest("accessLogging/server-logformat-ports2.xml");
    }

    /**
     * @throws Exception
     * @throws MalformedURLException
     * @throws IOException
     * @throws ProtocolException
     */
    private void runPortsTest(String configFile) throws Exception, MalformedURLException, IOException, ProtocolException {
        xmlServer.setServerConfigurationFile(configFile);
        waitForConfigUpdate(xmlServer);

        xmlServer.setMarkToEndOfLog(xmlServer.getDefaultLogFile());
        xmlServer.setTraceMarkToEndOfDefaultTrace();

        hitHttpEndpoint(xmlServer);
        String line = xmlServer.waitForStringInLogUsingMark("liberty_accesslog");

        String[] expectedFields = { "ibm_requestPort", "ibm_remotePort" };
        assertTrue("Expecting two different ports.", areFieldsPresent(line, expectedFields));

        String traceLine = xmlServer.waitForStringInTraceUsingMark(Pattern.quote("read (async) requested for local"));
        assertNotNull("Expecting TCP trace statement", traceLine);

        // Example:
        // [8/17/21, 9:31:58:070 PDT] 0000003a id=00000000 com.ibm.ws.tcpchannel.internal.TCPReadRequestContextImpl     1 read (async) requested for local: /127.0.0.1:8010 remote: /127.0.0.1:52709

        int localIndex = traceLine.indexOf("local: ");
        assertTrue(localIndex != -1);

        int remoteIndex = traceLine.indexOf("remote: ");
        assertTrue(remoteIndex != -1 && remoteIndex > localIndex);

        String localPort = traceLine.substring(localIndex, remoteIndex);
        localPort = localPort.substring(localPort.lastIndexOf(':') + 1).trim();

        String remotePort = traceLine.substring(remoteIndex);
        remotePort = remotePort.substring(remotePort.lastIndexOf(':') + 1);

        HashMap<String, String> accessLogResults = parseIntoKvp(line);

        assertEquals("Local port doesn't match", localPort, accessLogResults.get("ibm_requestPort"));
        assertEquals("Remote port doesn't match", remotePort, accessLogResults.get("ibm_remotePort"));
    }

    // *** Helper functions ***
    // We can hit the regular http endpoint for most tests - it's the simplest and fastest one.
    protected static void hitHttpEndpoint(LibertyServer server) throws MalformedURLException, IOException, ProtocolException {
        hitHttpEndpoint("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/?query", server);
    }

    protected static void hitHttpEndpointSecondary(LibertyServer server) throws MalformedURLException, IOException, ProtocolException {
        hitHttpEndpoint("http://" + server.getHostname() + ":" + server.getHttpSecondaryPort() + "/?query", server);
    }

    private static void hitHttpEndpoint(String stringUrl, LibertyServer server) {
        HttpURLConnection con = null;
        try {
            URL url = new URL(stringUrl);
            Log.info(c, "hitHttpEndpoint", "Attempting to connect to " + url);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("cookie", "cookie=cookie");
            con.setRequestProperty("header", "headervalue");
            con.setConnectTimeout(60 * 1000); // Timeout is, by default, infinity - we don't want to waste time if the connection can't be established
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line = null;
            StringBuilder lines = new StringBuilder();
            try {
                while ((line = br.readLine()) != null && line.length() > 0) {
                    lines.append(line);
                }
            } finally {
                br.close();
            }
            Log.info(c, "hitHttpEndpoint", url + " reached successfully.");
            // Just in case - make sure that we got something returned to us
            assertTrue("Nothing was returned from the servlet - there was a problem connecting.", lines.length() > 0);
            con.disconnect();
        } catch (IOException e) {
            //We can still log access log, just the status may return 500 in open liberty image
            Log.info(c, "hitHttpsEndpoint", e.getMessage());
        } finally {
            if (con != null)
                con.disconnect();
        }
    }

    // The /metrics endpoint can supply us all of the possible access logging fields - but is a bit more finnicky to work with because it requires us to wait for security prerequisites
    // Only used for tests where we need to check that *all* possible access log fields show up.
    protected static void hitHttpsEndpointSecure(String servletName, LibertyServer server) throws MalformedURLException, IOException, ProtocolException {
        HttpsURLConnection con = null;
        try {
            URL url = new URL("https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + servletName + "/?query");
            Log.info(c, "hitHttpsEndpointSecure", "Attempting to connect to " + url);
            con = (HttpsURLConnection) url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
            });
            String encoded = "Basic " + Base64.getEncoder().encodeToString(("admin:adminpwd").getBytes(StandardCharsets.UTF_8)); //Java 8
            con.setRequestProperty("Authorization", encoded);
            con.setRequestProperty("cookie", "cookie=cookie");
            con.setRequestProperty("header", "headervalue");
            con.setConnectTimeout(60 * 1000); // Timeout is, by default, infinity - we don't want to waste time if the connection can't be established
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line = null;
            StringBuilder lines = new StringBuilder();
            try {
                while ((line = br.readLine()) != null && line.length() > 0) {
                    lines.append(line);
                }
            } finally {
                br.close();
            }
            Log.info(c, "hitHttpsEndpointSecure", url + " reached successfully.");
            assertTrue("Nothing was returned from the servlet - there was a problem connecting.", lines.length() > 0);
            con.disconnect();
        } catch (IOException e) {
            //We can still log access log, just the status may return 500 in open liberty image
            Log.info(c, "hitHttpsEndpointSecure", e.getMessage());
        } finally {
            if (con != null)
                con.disconnect();
        }
    }

    public boolean areFieldsPresent(String line, String[] fields) {
        boolean allFieldsPresent = true;
        for (String s : fields) {
            if (!line.contains(s)) {
                // Field is not present
                allFieldsPresent = false;
            }
        }
        return allFieldsPresent;
    }

    public boolean areFieldsNotPresent(String line, String[] fields) {
        boolean allFieldsNotPresent = true;
        for (String s : fields) {
            if (line.contains(s)) {
                // Found a field that should not be present
                allFieldsNotPresent = false;
            }
        }
        return allFieldsNotPresent;
    }

    public boolean areFieldsRepeated(String line, String[] fields) {
        boolean repeated = false;
        for (String s : fields) {
            int index = line.indexOf(s);
            int count = 0;
            while (index != -1) {
                count++;
                if (count > 1)
                    return true;
                line = line.substring(index + 1);
                index = line.indexOf(s);
            }
        }
        return repeated;
    }

    // Set server configuration to rename or omit fields
    private static void setServerConfigurationForRename(String newFieldName, LibertyServer server) throws Exception {
        Logging loggingObj;
        ServerConfiguration serverConfig = server.getServerConfiguration();
        loggingObj = serverConfig.getLogging();
        loggingObj.setjsonFields(newFieldName);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(serverConfig);
        server.waitForConfigUpdateInLogUsingMark(null);
    }

    // Switch to a new logFormat value
    private static void setServerConfiguration(String jsonAccessLogFields, LibertyServer server) throws Exception {
        Logging loggingObj;
        ServerConfiguration serverConfig = server.getServerConfiguration();
        loggingObj = serverConfig.getLogging();
        loggingObj.setJsonAccessLogFields(jsonAccessLogFields);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(serverConfig);
        server.waitForConfigUpdateInLogUsingMark(null);
    }

    public HashMap<String, String> parseIntoKvp(String line) {
        // Parses the JSON log into a HashMap representing the fields
        HashMap<String, String> keyValuePairs = new HashMap<String, String>();
        // Remove quotes, backslashes from JSON escaping, and brackets to have a clean map
        line = line.replaceAll("\"", "").replaceAll("\\\\", "").replace("}", "").replace("{", "");
        String[] keyValueTokens = line.split(",");
        for (String pair : keyValueTokens) {
            keyValuePairs.put(pair.substring(0, pair.indexOf(":")), pair.substring(pair.indexOf(":") + 1));
        }
        return keyValuePairs;
    }

    private String readFile(String file) throws Exception {
        File f = new File(file);
        if (!f.exists() || f.isDirectory())
            throw new FileNotFoundException(file);

        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(f));
        try {
            String line;
            while ((line = br.readLine()) != null)
                sb.append(line).append('\n');
        } finally {
            br.close();
        }
        return sb.toString();
    }

    private void waitForConfigUpdate(LibertyServer server) {
        String result = server.waitForStringInLog("CWWKG0017I|CWWKG0018I", LOG_TIMEOUT);
        assertNotNull("Wrong number of updates have occurred", result);
    }

    private void waitForSecurityPrerequisites(LibertyServer server, long logTimeout) {
        // Need to ensure LTPA keys and configuration are created before hitting a secure endpoint
        assertNotNull("LTPA keys are not created within timeout period of " + logTimeout + "ms.", server.waitForStringInLog("CWWKS4104A", logTimeout));
        assertNotNull("LTPA configuration is not ready within timeout period of " + logTimeout + "ms.", server.waitForStringInLog("CWWKS4105I", logTimeout));
    }

    private void waitForMetricsToStart(LibertyServer server, long logTimeout) {
        // Wait for port 8020 to be ready
        assertNotNull("TCP Channel defaultHttpEndpoint-ssl has not started (CWWKO0219I not found)",
                      server.waitForStringInLog("CWWKO0219I.*defaultHttpEndpoint-ssl", logTimeout));
        // Wait for /metrics to be initialized
        assertNotNull("/metrics was not initialized (SRVE0242I not found)",
                      server.waitForStringInLog("SRVE0242I.*/metrics", logTimeout));
    }
}
