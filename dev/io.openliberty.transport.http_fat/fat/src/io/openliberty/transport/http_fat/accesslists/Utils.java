/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.n
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.transport.http_fat.accesslists;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.NoHttpResponseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.Assert;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.HttpEndpoint;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.TcpOptions;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.topology.impl.LibertyServer;

/**
 * A place to store useful code that might get reused
 */
public class Utils {

    private static final String WEB_APPLICATION_AVAILABLE_MSG_CWWKT0016I = "CWWKT0016I";
    private static final String NO_UPDATE_DETECTED_CWWKG0018I = "CWWKG0018I";
    private static final String READY = WEB_APPLICATION_AVAILABLE_MSG_CWWKT0016I + "|" + NO_UPDATE_DETECTED_CWWKG0018I;

    /**
     * Do a simple http get to a server uri with trace
     *
     * @param server                target server
     * @param reqURI                'ContextRoot/path'
     * @param resExpectedStatusCode
     * @param resExpectedText
     * @param resNotExpectedText    will fail if this is in what is returned
     * @return the response text
     * @throws Exception
     */
    public static String get(LibertyServer server,
                             String reqURI, String resExpectedStatusCode, String resExpectedText, String resNotExpectedText) throws Exception {

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + reqURI;
        debug("Expecting response text [" + resExpectedText + "]");
        debug("Expecting NO response text [" + resNotExpectedText + "]");
        debug("Expecting status code [" + resExpectedStatusCode + "]");
        debug("Sending --> GET" + " [" + url + "]");

        HttpUriRequestBase method = new HttpGet(url);

        String result = null;
        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(method)) {
                String responseText = EntityUtils.toString(response.getEntity());
                String responseCode = String.valueOf(response.getCode());

                debug("\n" + "##### Response Text ##### \n[" + responseText + "]");
                debug("##### Response Code ###### [" + responseCode + "]");

                if (resExpectedStatusCode != null && !resExpectedStatusCode.isEmpty())
                    assertTrue("The response did not contain the status code " + resExpectedStatusCode, responseCode.equals(resExpectedStatusCode));

                if (resExpectedText != null && !resExpectedText.isEmpty())
                    assertTrue("The response did not contain the following text: " + resExpectedText + " it was " + responseText, responseText.contains(resExpectedText));

                if (resNotExpectedText != null && !resNotExpectedText.isEmpty())
                    assertFalse("The response did not contain the following text: " + resNotExpectedText, responseText.contains(resNotExpectedText));
                result = responseText;
            }
        }

        return result;
    }

    /**
     * Do a simple http get to a server uri with trace and expect it to work
     *
     * @param server                target server
     * @param reqURI                'ContextRoot/path'
     * @param resExpectedStatusCode
     * @param resExpectedText
     * @param resNotExpectedText    will fail if this is in what is returned
     * @return the response text
     * @throws Exception
     */
    public static String accessAllowed(LibertyServer server,
                                       String reqURI, String resExpectedStatus, String resExpectedText) throws Exception {
        // We can pass this straight through, it will fail if the expected text is not retrieved
        try {
            return get(server, reqURI, resExpectedStatus, resExpectedText, "");
        } catch (Throwable t) {
            // We have failed unexpectedly, why?
            debug("Unexpected server access problem for " + reqURI + "server config: " + server.getServerConfiguration());
            t.printStackTrace();
            throw t;
        }
    }

    /**
     * Do a simple http get to a server uri with trace and expect it to fail
     *
     * @param server                target server
     * @param reqURI                'ContextRoot/path'
     * @param resExpectedStatusCode
     * @param resExpectedText
     * @param resNotExpectedText    will fail if this is in what is returned
     * @return the response text
     * @throws Exception
     */
    public static String accessDenied(LibertyServer server,
                                      String reqURI, String resExpectedStatus, String resNotExpectedText) throws Exception {

        String result = "";
        try {
            result = get(server, reqURI, resExpectedStatus, "", "");
            Assert.fail("Expected exception but got: " + result);
        } catch (SocketException | NoHttpResponseException e) {
            result = "OK, denied with: " + e.getClass().getSimpleName() + " " + e.getMessage();
        } catch (Throwable t) {
            result = "Expected HttpHostConnectException, NoHttpResponseException or Socket Exception but got: " + t.getMessage();
            Assert.fail(result);
        }
        debug(result);
        return result;
    }

    /**
     * Place a string in the log, change fine to info for default output
     *
     * @param string
     */
    public static void debug(String string) {
        AccessListsTests.LOG.info(string);
    }

    /**
     * Set server.xml to the test's customized config,
     * for example, publish/files/AccessListsTests_testA11_server.xml
     *
     * @param server
     * @param test   used to get the specific file name and app
     * @throws InterruptedException
     */
    public static void setUpServer(LibertyServer server, HttpTest test) {
        String testMethod = test.toString();
        if (testMethod.endsWith(RepeatTestFilter.getRepeatActionsAsString())) {
            testMethod = testMethod.replace(RepeatTestFilter.getRepeatActionsAsString(), "");
        }
        String serverXml = testMethod + "_server.xml";
        String customized = customize(server, serverXml, test.clientDetails);

        // We are not testing server startup glitches in the test cloud infrastructure here
        // so have some tolerance for when there is a temporary problem in starting
        // the server on time (due to cloud CPU resources usually).
        final int SLOW_SERVER_START_TOLERANCE = 5;
        for (int i = 1; i <= SLOW_SERVER_START_TOLERANCE; i++) {
            try {
                // We will wait for the app to come up
                replaceServerXmlContents(server, customized, test.app, READY);
                // We will have thrown an exception on having a problem if not we are finished
                return;
            } catch (Exception e) {
                debug(e.getMessage());
                if (i >= SLOW_SERVER_START_TOLERANCE) {
                    Assert.fail("Failed to start server promptly " + SLOW_SERVER_START_TOLERANCE + " times." + e.getMessage());
                }
                try {
                    Thread.sleep(i * 1000);
                } catch (InterruptedException ie) {
                    continue; // we will be finished soon
                }
            }
        }
    }

    /**
     * Replace a servers server.xml contents with the contents of a String
     *
     * @param serverXml the new config (contents not filename)
     * @throws Exception
     */
    static void replaceServerXmlContents(LibertyServer server, String serverXml, String app, String... additionalMsgs) throws Exception {
        RemoteFile srvXml;
        try {
            srvXml = server.getServerConfigurationFile();
            debug("Replacing server.xml for server " + server.getServerName() + " with: ");
            debug(serverXml);
            server.setMarkToEndOfLog();
            Files.write(Paths.get(srvXml.getAbsolutePath()), serverXml.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(app), additionalMsgs);

            if (serverXml.contains("tcpOptions")) {
                ServerConfiguration c = server.getServerConfiguration();
                ConfigElementList<HttpEndpoint> h = c.getHttpEndpoints();
                for (HttpEndpoint httpEndpoint : h) {
                    TcpOptions tcpo = httpEndpoint.getTcpOptions();
                    debug("TcpOptions have been set to be: " + tcpo);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            debug(e.getMessage());
            throw e;
        }
    }

    /**
     * Complete a template with property values
     *
     * @param clientInfoServer
     * @param serverXml
     */
    static String customize(LibertyServer server, String file, Properties p) {
        String custom = "empty";
        String src = server.pathToAutoFVTTestFiles + file;

        try (Stream<String> strm = new BufferedReader(new FileReader(new File(src))).lines()) {
            custom = strm.collect(Collectors.joining("\n"));
            final StringBuilder sb = new StringBuilder(custom);
            // Substitute any keys for values from Properties - short and simple (prefixes etc. not needed)
            if (p != null) {
                p.forEach((k, v) -> sb.replace(0, sb.length(), sb.toString().replaceAll((String) k, (String) v)));
            }
            custom = sb.toString();
            return custom;
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("failed to customize " + src + " from " + p + " due to " + e.getMessage());
            return null;
        }
    }

    /**
     * Taken as is from tWAS test code
     *
     * @param s
     * @param place
     * @param wildString
     * @return
     */
    public static String putInWildCards(String s, int place, String wildString) {

        int currentPlace = 1;
        int startIndex = 0;
        int endIndex = 0;
        String separator = ".";
        boolean found = false;

        StringBuffer ret = new StringBuffer(s);
        if (s.indexOf(":") != -1) {
            separator = ":";
        }

        endIndex = s.indexOf(separator);

        while (endIndex != -1) {

            if (currentPlace == place) {
                ret.replace(startIndex, endIndex, wildString);
                found = true;
                break;
            }

            startIndex = endIndex + 1;
            endIndex = s.indexOf(separator, startIndex);
            currentPlace++;
        }

        // check if we are on the last place
        if ((found == false) && (currentPlace == place)) {
            ret.replace(startIndex, s.length(), wildString);
        }

        return ret.toString();
    }

    /**
     * Taken as is from tWAS test code
     *
     * @param ip4
     * @return
     */
    public static String convertIP4toIP6(String ip4) {
        String ip6 = null;

        // if already in ipv6, then return it
        if (ip4.indexOf(":") != -1) {
            return ip4;
        }

        int startIndex = 0;
        int endIndex = ip4.indexOf(".");
        String ip41 = Integer.toHexString(Integer.parseInt(ip4.substring(startIndex, endIndex)));

        startIndex = endIndex + 1;
        endIndex = ip4.indexOf(".", endIndex + 1);
        String ip42 = Integer.toHexString(Integer.parseInt(ip4.substring(startIndex, endIndex)));

        startIndex = endIndex + 1;
        endIndex = ip4.indexOf(".", endIndex + 1);
        String ip43 = Integer.toHexString(Integer.parseInt(ip4.substring(startIndex, endIndex)));

        startIndex = endIndex + 1;
        endIndex = ip4.length();
        String ip44 = Integer.toHexString(Integer.parseInt(ip4.substring(startIndex, endIndex)));

        ip6 = "0:0:0:0:" + ip41 + ":" + ip42 + ":" + ip43 + ":" + ip44;

        return ip6;
    }
}
