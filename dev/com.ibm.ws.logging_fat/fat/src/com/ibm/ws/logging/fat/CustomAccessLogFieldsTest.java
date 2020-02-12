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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */
@RunWith(FATRunner.class)
public class CustomAccessLogFieldsTest {
    private static final String MESSAGE_LOG = "logs/messages.log";

    @Server("CustomAccessLogFieldsEnv")
    public static LibertyServer envServer;

    @Server("CustomAccessLogFieldsBootstrap")
    public static LibertyServer bootstrapServer;

    @Server("CustomAccessLogFieldsXml")
    public static LibertyServer xmlServer;

    @Server("CustomAccessLogFieldsBadConfig")
    public static LibertyServer badConfigServer;

    private static final String SERVER_NAME_ENV = "CustomAccessLogFieldsEnv";
    private static final String SERVER_NAME_BOOTSTRAP = "CustomAccessLogFieldsBootstrap";
    private static final String SERVER_NAME_XML = "CustomAccessLogFieldsXml";
    private static final String SERVER_NAME_BAD_CONFIG = "CustomAccessLogFieldsBadConfig";

    // variable naming convention?
    private final String[] newFields = { "ibm_remoteIP", "ibm_bytesSent", "ibm_cookie", "ibm_requestElapsedTime", "ibm_requestHeader",
                                         "ibm_responseHeader", "ibm_requestFirstLine", "ibm_requestStartTime", "ibm_accessLogDatetime", "ibm_remoteUserID" };

    private static LibertyServer serverInUse; // hold on to the server currently used so cleanUp knows which server to stop

    @BeforeClass
    public static void initialSetup() throws Exception {
        envServer = LibertyServerFactory.getLibertyServer(SERVER_NAME_ENV);
        bootstrapServer = LibertyServerFactory.getLibertyServer(SERVER_NAME_BOOTSTRAP);
        xmlServer = LibertyServerFactory.getLibertyServer(SERVER_NAME_XML);
        badConfigServer = LibertyServerFactory.getLibertyServer(SERVER_NAME_BAD_CONFIG);

        // Preserve the original server configuration
        envServer.saveServerConfiguration();
        bootstrapServer.saveServerConfiguration();
        xmlServer.saveServerConfiguration();
        badConfigServer.saveServerConfiguration();
    }

    public void setUp(LibertyServer server) throws Exception {
        serverInUse = server;
        if (server != null && !server.isStarted()) {
            // Restore the original server configuration, before starting the server for each test case.
            server.restoreServerConfiguration();
            server.startServer();
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
        hitWebPage("", "", envServer);
        List<String> lines = envServer.findStringsInFileInLibertyServerRoot("liberty_accesslog", MESSAGE_LOG);
        System.out.println(lines.get(0));

        assertTrue("There are fields missing in the output JSON log.", areAllFieldsPresent(lines));
    }

    /*
     * This test sets the "WLP_ENABLE_CUSTOM_ACCESS_LOG_FIELDS" attribute in the server.env and verifies the property in the messages.log file.
     */
    @Test
    public void testAccessLogFieldNamesBootstrap() throws Exception {
        setUp(bootstrapServer);
        hitWebPage("", "", bootstrapServer);
        List<String> lines = bootstrapServer.findStringsInFileInLibertyServerRoot("liberty_accesslog", MESSAGE_LOG);
        System.out.println(lines.get(0));

        assertTrue("There are fields missing in the output JSON log.", areAllFieldsPresent(lines));
    }

    /*
     * This test sets the "WLP_ENABLE_CUSTOM_ACCESS_LOG_FIELDS" attribute in the server.env and verifies the property in the messages.log file.
     */
    @Test
    public void testAccessLogFieldNamesXml() throws Exception {
        setUp(xmlServer);
        hitWebPage("", "", xmlServer);
        List<String> lines = xmlServer.findStringsInFileInLibertyServerRoot("liberty_accesslog", MESSAGE_LOG);
        System.out.println(lines.get(0));

        assertTrue("There are fields missing in the output JSON log.", areAllFieldsPresent(lines));
    }

    @Test
    public void testAccessLogFaultyConfig() throws Exception {
        // Should we have 3 tests to test each version? Can one test stop and start server 3x?
        setUp(badConfigServer);
        List<String> lines = badConfigServer.findStringsInFileInLibertyServerRoot("TRAS3012W", MESSAGE_LOG);

        assertNotNull("The error message was not sent with a bad configuration.", lines);
    }

    @Test
    public void testRenameAccessLogField() throws Exception {
        // rename header, cookie <-- doesnt work yet
        // rename broadly
    }

    @Test
    public void testOmitAccessLogField() throws Exception {
        // omit specific header, cookie <-- doesnt work yet
        // omit broadly
    }

    @Test
    public void testNullValuesDontPrintInJSON() throws Exception {
        // in the access log, it'll print `-` but in the JSON log it shouldn't print at all
        // test w/ header, cookie
    }

    @Test
    public void testFieldsInAccessLogAreSameInJSON() throws Exception {
        // test that the fields in the access log print the same value in the JSON logs
    }

    @Test
    public void testOnlyUnchangingField() throws Exception {
        // come up with better test name
        // test that specifying `= logFormat` doesn't print out the original set of fields unless specified
        // %h %H %A %B %m %p %q %R{W} %s %U <- should not be printed out
    }

    public boolean areAllFieldsPresent(List<String> lines) {
        for (String s : newFields) {
            if (!lines.get(0).contains(s)) {
                System.out.println(s);
                return false;
            }
        }
        return true;
    }

    protected static void hitWebPage(String contextRoot, String servletName,
                                     LibertyServer server) throws MalformedURLException, IOException, ProtocolException {
        try {
            String cookie = "cookie=cookie";
            URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + contextRoot + "" + servletName);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("cookie", cookie);
            con.setRequestProperty("header", "headervalue");
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            // Make sure the server gave us something back
            assertNotNull(line);
            con.disconnect();
        } catch (IOException e) {

        }

    }
}
