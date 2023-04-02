/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.logging.json.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Tests if the application name that is registered as a LogRecordContext, correctly
 * appears as a JSON field in the JSON logs, when application logs are present.
 */
@RunWith(FATRunner.class)
public class AppNameExtensionTest {

    @Server("com.ibm.ws.logging.json.AppNameExtensionServer")
    public static LibertyServer server1;

    public static final String LOGSTASH_APP_NAME = "LogstashApp";
    public static final String LOGSTASH_APP_PACKAGE_NAME = "com.ibm.logs";
    public static final String LOGGER_SERVLET_APP_NAME = "LoggerServlet";
    public static final String LOGGER_SERVLET_APP_PACKAGE_NAME = "com.ibm.ws.logging.fat.logger.servlet";

    public static final String SERVER_XML_JSON_MESSAGES = "jsonMessagesLogServer.xml";
    public static final String SERVER_XML_SIMPLE_MESSAGES = "basicServer.xml";

    public static final int CONN_TIMEOUT = 10;

    protected static final Class<?> c = AppNameExtensionTest.class;

    public static void setUpTestCase(LibertyServer server, boolean deployDefaultApp, boolean addMultipleApps) throws Exception {
        if (deployDefaultApp) {
            deployDefaultApplication(server, LOGSTASH_APP_NAME, LOGSTASH_APP_PACKAGE_NAME);
        } else {
            // Start server
            server.startServer();
        }

        if (addMultipleApps) {
            deployDropinsApplication(server, LOGGER_SERVLET_APP_NAME, LOGGER_SERVLET_APP_PACKAGE_NAME);
        }
    }

    @After
    public void cleanUpTestCase() {
        if ((server1 != null) && (server1.isStarted())) {
            try {
                server1.stopServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * Tests whether the application name LogRecordContext extension exists as a JSON field ("ext_appName":"LogstashApp")
     * in the JSON application logs.
     */
    @Test
    public void testAppNameExtensionExistsForAppLogs() throws Exception {
        final String method = "testAppNameExtensionExistsForAppLogs";
        setUpTestCase(server1, true, false);
        setServerConfig(server1, SERVER_XML_JSON_MESSAGES);
        TestUtils.runApp(server1, "logServlet");

        String line = server1.waitForStringInLog("\\{.*\"ext_appName\":\"LogstashApp\".*\\}");
        Log.info(c, method, "ext_appName JSON field line : " + line);

        assertNotNull("Cannot find \"ext_appName\":\"LogstashApp\" in messages.log", line);
    }

    /*
     * Tests whether the application name LogRecordContext extension (ext_appName) JSON field does not exist when no application logs
     * are logged in the JSON logs.
     */
    @Test
    public void testAppNameExtensionNotExistsForNoAppLogs() throws Exception {
        final String method = "testAppNameExtensionNotExistsForNoAppLogs";
        setUpTestCase(server1, true, false);
        setServerConfig(server1, SERVER_XML_JSON_MESSAGES);

        String line = server1.waitForStringInLog("\\{.*\"ext_appName\":\"LogstashApp\".*\\}");
        Log.info(c, method, "ext_appName JSON field line : " + line);

        assertNull("Found \"ext_appName\":\"LogstashApp\" in messages.log", line);
    }

    /*
     * Tests whether the application name LogRecordContext extension (ext_appName) JSON field does not exist when no application is
     * deployed in the JSON logs.
     */
    @Test
    public void testAppNameExtensionNotExistsForNoApp() throws Exception {
        final String method = "testAppNameExtensionNotExistsForNoApp";
        setUpTestCase(server1, false, false);
        setServerConfig(server1, SERVER_XML_JSON_MESSAGES);

        String line = server1.waitForStringInLog("\\{.*\"ext_appName\":\"LogstashApp\".*\\}");
        Log.info(c, method, "ext_appName JSON field line : " + line);

        assertNull("Found \"ext_appName\":\"LogstashApp\" in messages.log", line);
    }

    /*
     * Tests whether the application name LogRecordContext extension (ext_appName) JSON field does not exist in the application logs
     * when the messageFormat is simple.
     */
    @Test
    public void testAppNameExtensionNotExistsForSimpleLogs() throws Exception {
        final String method = "testAppNameExtensionNotExistsForSimpleLogs";
        setUpTestCase(server1, true, false);
        setServerConfig(server1, SERVER_XML_SIMPLE_MESSAGES);

        String line = server1.waitForStringInLog("\"ext_appName\":\"LogstashApp\"");
        Log.info(c, method, "ext_appName JSON field line : " + line);

        assertNull("Found \"ext_appName\":\"LogstashApp\" in messages.log", line);
    }

    /*
     * Tests whether the application name LogRecordContext extension exists as a JSON field ("ext_appName":"LogstashApp")
     * in the JSON application logs.
     */
    @Test
    public void testAppNameExtensionExistsForMultipleAppLogs() throws Exception {
        final String method = "testAppNameExtensionExistsForMultipleAppLogs";
        setUpTestCase(server1, true, true);
        setServerConfig(server1, SERVER_XML_JSON_MESSAGES);

        // Check if the LogstashApp application logs contain the appName extension field
        TestUtils.runApp(server1, "logServlet");
        String line1 = server1.waitForStringInLog("\\{.*\"ext_appName\":\"LogstashApp\".*\\}");
        Log.info(c, method, "ext_appName JSON field line : " + line1);
        assertNotNull("Cannot find \"ext_appName\":\"LogstashApp\" in messages.log", line1);

        // Check if the LoggerServlet application logs contain the appName extension field
        hitWebPage(server1, "LoggerServlet", "LoggerServlet", false);
        String line2 = server1.waitForStringInLog("\\{.*\"ext_appName\":\"LoggerServlet\".*\\}");
        Log.info(c, method, "ext_appName JSON field line : " + line2);
        assertNotNull("Cannot find \"ext_appName\":\"LoggerServlet\" in messages.log", line2);
    }

    private static void hitWebPage(LibertyServer server, String contextRoot, String servletName,
                                   boolean failureAllowed) throws MalformedURLException, IOException, ProtocolException {
        try {
            String urlStr = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + contextRoot + "/" + servletName;
            URL url = new URL(urlStr);
            int expectedResponseCode = failureAllowed ? HttpURLConnection.HTTP_INTERNAL_ERROR : HttpURLConnection.HTTP_OK;
            HttpURLConnection con = HttpUtils.getHttpConnection(url, expectedResponseCode, CONN_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            // Make sure the servlet responds correctly
            assertNotNull(line);
            con.disconnect();
        } catch (IOException e) {
            // A message about a 500 code may be fine
            if (!failureAllowed) {
                throw e;
            }
        }
    }

    private static void setServerConfig(LibertyServer server, String fileName) throws Exception {
        RemoteFile log = server.getDefaultLogFile();
        server.setMarkToEndOfLog(log);
        server.setServerConfigurationFile(fileName);
        //Check message.log for CWWKG0017I.*|CWWKG0018I. message, messageSource must have "message"
        Assert.assertNotNull(server.waitForStringInLog("CWWKG0017I.*|CWWKG0018I.*", log));
    }

    private static void deployDefaultApplication(LibertyServer server, String appName, String packageName) throws Exception {
        ShrinkHelper.defaultApp(server, appName, packageName);
        server.startServer();
        Assert.assertNotNull("Test app " + appName + " does not appear to have started.", server.waitForStringInLog("CWWKT0016I:.*" + appName));
    }

    private static void deployDropinsApplication(LibertyServer server, String appName, String packageName) throws Exception {
        RemoteFile log = server.getDefaultLogFile();
        server.setMarkToEndOfLog(log);
        WebArchive app = ShrinkHelper.buildDefaultApp(appName, packageName);
        ShrinkHelper.exportDropinAppToServer(server, app);
        Assert.assertNotNull("Test app " + appName + " does not appear to have started.", server.waitForStringInLog("CWWKT0016I:.*" + appName));
    }

}
