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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
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

    @Server("CustomAccessLogFieldsEnv")
    public static LibertyServer envServer;

    @Server("CustomAccessLogFieldsBootstrap")
    public static LibertyServer bootstrapServer;

    @Server("CustomAccessLogFieldsXml")
    public static LibertyServer xmlServer;

    @Server("CustomAccessLogFieldsBadConfigEnv")
    public static LibertyServer badConfigServerEnv;

    @Server("CustomAccessLogFieldsBadConfigBootstrap")
    public static LibertyServer badConfigServerBootstrap;

    @Server("CustomAccessLogFieldsBadConfigXml")
    public static LibertyServer badConfigServerXml;

    @Server("CustomAccessLogFieldsTwoHttpEndpoints")
    public static LibertyServer multipleHttpEndpointServer;

    private final String[] newFields = { "ibm_remoteIP", "ibm_bytesSent", "ibm_cookie", "ibm_requestElapsedTime", "ibm_requestHeader",
                                         "ibm_responseHeader", "ibm_requestFirstLine", "ibm_requestStartTime", "ibm_accessLogDatetime", "ibm_remoteUserID" };

    private final String[] defaultFields = { "ibm_remoteHost", "ibm_requestProtocol", "ibm_requestHost", "ibm_bytesReceived", "ibm_requestMethod", "ibm_requestPort",
                                             "ibm_queryString", "ibm_elapsedTime", "ibm_responseCode", "ibm_uriPath", "ibm_userAgent" };

    private static LibertyServer serverInUse; // hold on to the server currently used so cleanUp knows which server to stop

    @BeforeClass
    public static void initialSetup() throws Exception {
        trustAll();
        envServer.saveServerConfiguration();
        bootstrapServer.saveServerConfiguration();
        xmlServer.saveServerConfiguration();
        badConfigServerEnv.saveServerConfiguration();
        badConfigServerBootstrap.saveServerConfiguration();
        badConfigServerXml.saveServerConfiguration();
        multipleHttpEndpointServer.saveServerConfiguration();
    }

    public void setUp(LibertyServer server) throws Exception {
        serverInUse = server;
        if (server != null && !server.isStarted()) {
            server.restoreServerConfiguration();
            server.startServer();
            server.waitForStringInLog("CWWKT0016I");
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
        hitHttpsEndpointSecure("/metrics", envServer);
        String line = envServer.waitForStringInLog("liberty_accesslog");

        assertTrue("There are fields missing in the output JSON log.", areFieldsPresent(line, newFields));
        assertTrue("There are unexpected fields in the output JSON log.", areFieldsNotPresent(line, defaultFields));
    }

    /*
     * This test sets the "com.ibm.ws.logging.json.access.log.fields" attribute in the bootstrap.properties and verifies the property in the messages.log file.
     */
    @Test
    public void testAccessLogFieldNamesBootstrap() throws Exception {
        setUp(bootstrapServer);
        hitHttpsEndpointSecure("/metrics", bootstrapServer);
        String line = bootstrapServer.waitForStringInLog("liberty_accesslog");

        assertTrue("There are fields missing in the output JSON log.", areFieldsPresent(line, newFields));
        assertTrue("There are unexpected fields in the output JSON log.", areFieldsNotPresent(line, defaultFields));
    }

    /*
     * This test sets the "logFormat" attribute in the server.xml and verifies the property in the messages.log file.
     */
    @Test
    public void testAccessLogFieldNamesXml() throws Exception {
        setUp(xmlServer);
        hitHttpsEndpointSecure("/metrics", xmlServer);
        String line = xmlServer.waitForStringInLog("liberty_accesslog");

        assertTrue("There are fields missing in the output JSON log.", areFieldsPresent(line, newFields));
        assertTrue("There are unexpected fields in the output JSON log.", areFieldsNotPresent(line, defaultFields));
    }

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
        assertNotNull("The error message CWWKG0032W was not sent from faulty configuration in the server env.", lines);
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
        hitHttpEndpoint("", xmlServer);
        String line = xmlServer.waitForStringInLog("liberty_accesslog");
        String[] renamedNames = { "rename_cookie", "rename_requestHeader", "rename_responseHeader" };
        assertTrue("Access log fields were not renamed properly.", areFieldsPresent(line, renamedNames));

    }

    @Test
    public void testOmitAccessLogField() throws Exception {
        setUp(xmlServer);
        setServerConfigurationForRename("ibm_cookie_cookie:,ibm_requestHeader_header:,ibm_responseHeader_Content-Type:",
                                        xmlServer);
        hitHttpEndpoint("", xmlServer);
        String line = xmlServer.waitForStringInLog("liberty_accesslog");
        String[] omittedFields = { "ibm_cookie_cookie", "ibm_requestHeader_header", "ibm_responseHeader_Content-Type" };
        assertTrue("Access log fields were not omitted properly.", areFieldsNotPresent(line, omittedFields));
    }

    @Test
    public void testNullValuesDontPrintInJSON() throws Exception {
        setUp(xmlServer);
        hitHttpEndpoint("", xmlServer);
        xmlServer.setServerConfigurationFile("accessLogging/server-null-values-dont-print.xml");
        waitForConfigUpdate(xmlServer);
        String line = xmlServer.waitForStringInLog("liberty_accesslog");
        String[] nullFields = { "ibm_cookie_invalidcookie", "ibm_requestHeader_invalidheader", "ibm_responseHeader_invalidheader" };
        assertTrue("Null access log fields should not be printed, but null field was found.", areFieldsNotPresent(line, nullFields));
    }

    @Test
    public void testDefaultWillNotPrintNewFields() throws Exception {
        // logFormat = default will not print out any of the new fields
        setUp(xmlServer);
        setServerConfiguration("default", xmlServer);
        hitHttpEndpoint("", xmlServer);
        String line = xmlServer.waitForStringInLog("liberty_accesslog");
        assertTrue("There are unexpected fields in the output JSON log.", areFieldsNotPresent(line, newFields));
    }

    @Test
    public void testFieldsInAccessLogAreSameInJSON() throws Exception {
        // test that the fields in the access log print the same value in the JSON logs
        setUp(xmlServer);
        xmlServer.setMarkToEndOfLog();
        hitHttpsEndpointSecure("/metrics", xmlServer);
        String line = xmlServer.waitForStringInLog("liberty_accesslog");

        // create a map of the strings
        Map<String, String> parsedLine = parseIntoKvp(line);

        // We don't care about the non-access log fields, e.g. type, ibm_userDir, so let's remove them
        Set<String> unwantedFields = new HashSet<String>();
        unwantedFields.add("type");
        unwantedFields.add("host");
        unwantedFields.add("ibm_serverName");
        unwantedFields.add("ibm_userDir");
        unwantedFields.add("ibm_datetime");
        unwantedFields.add("ibm_sequence");
        parsedLine.keySet().removeAll(unwantedFields);

        // unfortunately our JSON log isn't ordered like the http access logs
        // easier to check that the value shows up in the logs. for duplicate values, let's just remove it with each pass-through.
        String accessLogLine = readFile(xmlServer.getServerRoot() + "/logs/http_access.log");
        assertNotNull("The http_access.log file is empty or could not be read.", accessLogLine);
        for (String s : parsedLine.values()) {
            if (accessLogLine.contains(s)) {
                // let's remove it, in case there's a duplicate value for a different field
                accessLogLine.replace(s, "");
            } else {
                // If we didn't find the value, then the JSON log is missing something or has an incorrect value
                fail("There's a value mismatch between the " + s + " http_access.log value and the JSON log value.");
            }
        }
    }

    @Test
    public void testChangeLogFormat() throws Exception {
        setUp(xmlServer);
        hitHttpsEndpointSecure("/metrics", xmlServer);
        String line = xmlServer.waitForStringInLogUsingMark("liberty_accesslog");
        xmlServer.setMarkToEndOfLog();

        assertTrue("There are fields missing in the output JSON log.", areFieldsPresent(line, newFields));
        assertTrue("There are unexpected fields in the output JSON log.", areFieldsNotPresent(line, defaultFields));

        // change to default
        setServerConfiguration("default", xmlServer);
        hitHttpsEndpointSecure("/metrics", xmlServer);
        line = xmlServer.waitForStringInLogUsingMark("liberty_accesslog");
        xmlServer.setMarkToEndOfLog();

        // this time, make sure that the default fields show up but NOT the new fields
        assertTrue("There are unexpected fields in the output JSON log.", areFieldsNotPresent(line, newFields));
        assertTrue("There are fields missing in the output JSON log.", areFieldsPresent(line, defaultFields));

        // change back again to logFormat
        setServerConfiguration("logFormat", xmlServer);
        hitHttpsEndpointSecure("/metrics", xmlServer);
        line = xmlServer.waitForStringInLogUsingMark("liberty_accesslog");
        xmlServer.setMarkToEndOfLog();

        assertTrue("There are fields missing in the output JSON log.", areFieldsPresent(line, newFields));
        assertTrue("There are unexpected fields in the output JSON log.", areFieldsNotPresent(line, defaultFields));
    }

    @Test
    public void testDisableAccessLog() throws Exception {
        // Test that removing the accessLogging attribute will stop JSON access logs too
        setUp(xmlServer);
        // Make sure that it initially prints something out while the accessLogging attribute is still enabled
        hitHttpEndpoint("", xmlServer);
        assertNotNull("No liberty_accesslog found in the messages.log.", xmlServer.waitForStringInLogUsingMark("liberty_accesslog"));
        xmlServer.setMarkToEndOfLog();

        xmlServer.setServerConfigurationFile("accessLogging/server-disable-accesslog.xml");
        waitForConfigUpdate(xmlServer);
        hitHttpEndpoint("", xmlServer);
        assertNull("Disabling the accessLogging attribute did not stop JSON access logs from being printed.", xmlServer.waitForStringInLogUsingMark("liberty_accesslog"));
    }

    @Test
    public void testEnableAccessLog() throws Exception {
        // Test that adding the accessLogging attribute after server started will have JSON access logs print
        setUp(xmlServer);
        xmlServer.setServerConfigurationFile("accessLogging/server-disable-accesslog.xml");
        waitForConfigUpdate(xmlServer);
        // Make sure that it doesn't print access logs without the attribute enabled first
        hitHttpEndpoint("", xmlServer);
        assertNull("There were JSON access logs printed when the accessLogging attribute was disabled.", xmlServer.waitForStringInLogUsingMark("liberty_accesslog"));
        xmlServer.setMarkToEndOfLog();

        // the original configuration actually had accessLogging enabled - so let's revert it back to the original
        xmlServer.restoreServerConfiguration();
        waitForConfigUpdate(xmlServer);
        hitHttpEndpoint("", xmlServer);
        assertNotNull("JSON access logs were not printed after enabling the accessLogging attribute.", xmlServer.waitForStringInLogUsingMark("liberty_accesslog"));
    }

    @Test
    @ExpectedFFDC("java.lang.IllegalArgumentException")
    public void testInvalidTokens() throws Exception {
        setUp(xmlServer);
        xmlServer.setServerConfigurationFile("accessLogging/server-invalid-tokens.xml");
        waitForConfigUpdate(xmlServer);
        hitHttpEndpoint("", xmlServer);
        // An FFDC should be created for the bad token, but we should still have logs printing for the valid ones
        // In this case, our logFormat="%a %b %J" and only %J is invalid, so we're expecting the other tokens to be printed
        String line = xmlServer.waitForStringInLogUsingMark("liberty_accesslog");
        String[] expectedFields = { "ibm_remoteIP", "ibm_bytesSent" };
        assertTrue("The JSON access log was not printed properly and is missing fields.", areFieldsPresent(line, expectedFields));
    }

    @Test
    public void testDuplicateTokensInLogFormat() throws Exception {
        // If there are duplicate tokens in the logFormat, the JSON logs should only print the token once, not multiple times
        setUp(xmlServer);
        xmlServer.setServerConfigurationFile("accessLogging/server-duplicate-keys.xml");
        waitForConfigUpdate(xmlServer);
        hitHttpEndpoint("", xmlServer);
        String line = xmlServer.waitForStringInLogUsingMark("liberty_accesslog");
        String[] expectedFields = { "ibm_remoteIP", "ibm_cookie_cookie" };
        // First, make sure they're even printed at all
        assertTrue("There are fields missing in the output JSON log.", areFieldsPresent(line, expectedFields));
        // Then, make sure they're not printed twice
        assertFalse("The JSON access log was not printed properly and is repeating fields.", areFieldsRepeated(line, expectedFields));
    }

    @Test
    public void testDefaultIsSameAsOriginal() throws Exception {
        setUp(xmlServer);
        // First, let's check that all the fields are present if we manually specify each field in the accessLogging logFormat property
        xmlServer.setServerConfigurationFile("accessLogging/server-default-in-logformat.xml");
        waitForConfigUpdate(xmlServer);
        hitHttpEndpoint("", xmlServer);
        String lineManual = xmlServer.waitForStringInLogUsingMark("liberty_accesslog");
        assertTrue("The JSON access log was not printed properly and is missing fields.", areFieldsPresent(lineManual, defaultFields));

        // Now, switch to jsonAccessLogFields = default and do the same check
        setServerConfiguration("default", xmlServer);
        waitForConfigUpdate(xmlServer);
        hitHttpEndpoint("", xmlServer);
        String lineDefault = xmlServer.waitForStringInLogUsingMark("liberty_accesslog");
        assertTrue("The JSON access log was not printed properly and is missing fields.", areFieldsPresent(lineDefault, defaultFields));

        // Finally, check that they both have the same fields
        // compare lineManual w/ lineDefault
        HashMap<String, String> lineManualMap = parseIntoKvp(lineManual);
        HashMap<String, String> lineDefaultMap = parseIntoKvp(lineDefault);
        assertTrue("There is a mismatch in fields.", lineManualMap.keySet().containsAll(lineDefaultMap.keySet()));
    }

    @Test
    public void testMultipleHttpEndpoints() throws Exception {
        // both have different access log format settings & initialization only happens twice
        // add trace to this initialization
//        setUp(multipleHttpEndpointServer);
//        hitHttpEndpoint("", xmlServer);

    }

    // Helper functions
    public HashMap<String, String> parseIntoKvp(String line) {
        // Parses the JSON log into a HashMap representing the fields and their values without quotes (for simplicity)
        HashMap<String, String> keyValuePairs = new HashMap<String, String>();
        line = line.replaceAll("\"", "").replaceAll("\\\\", "").replace("}", "").replace("{", "");
        String[] keyValueTokens = line.split(",");
        for (String pair : keyValueTokens) {
            keyValuePairs.put(pair.substring(0, pair.indexOf(":")), pair.substring(pair.indexOf(":") + 1));
        }
        return keyValuePairs;
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

    // Rename or omit fields
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

    private String readFile(String file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            line = reader.readLine();
            reader.close();
            return line;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void waitForConfigUpdate(LibertyServer server) {
        String result = server.waitForStringInLog("CWWKG0017I", LOG_TIMEOUT);
        assertNotNull("Wrong number of updates have occurred", result);
    }

    protected static void hitHttpEndpoint(String servletName, LibertyServer server) throws MalformedURLException, IOException, ProtocolException {
        HttpURLConnection con = null;
        try {
            URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + servletName + "/?query");
            con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("cookie", "cookie=cookie");
            con.setRequestProperty("header", "headervalue");

            System.out.println("Before connecting");
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            System.out.println("After connecting");

            String line = null;
            StringBuilder lines = new StringBuilder();
            while ((line = br.readLine()) != null && line.length() > 0) {
                lines.append(line);
            }
            assertNotNull(lines);
            con.disconnect();
        } catch (IOException e) {

        } finally {
            if (con != null)
                con.disconnect();
        }
    }

    protected static void hitHttpsEndpointSecure(String servletName, LibertyServer server) throws MalformedURLException, IOException, ProtocolException {
        HttpsURLConnection con = null;
        try {
            URL url = new URL("https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + servletName + "/?query");
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

            System.out.println("Before connecting");
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            System.out.println("After connecting");

            String line = null;
            StringBuilder lines = new StringBuilder();
            while ((line = br.readLine()) != null && line.length() > 0) {
                lines.append(line);
            }
            System.out.println(lines);
            assertNotNull(lines);
            con.disconnect();
        } catch (IOException e) {

        } finally {
            if (con != null)
                con.disconnect();
        }
    }
}