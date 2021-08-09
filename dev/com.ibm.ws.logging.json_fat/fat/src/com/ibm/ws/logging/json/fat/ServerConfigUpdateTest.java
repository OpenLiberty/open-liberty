/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.json.fat;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Enable JSON logging in messages.log with environment variables in server.env
 */
@RunWith(FATRunner.class)
public class ServerConfigUpdateTest {

    protected static final Class<?> c = ServerConfigUpdateTest.class;

    @Server("com.ibm.ws.logging.json.LibertyServer")
    public static LibertyServer server;

    public static final String APP_NAME = "LogstashApp";
    public static final String SERVER_XML_BASIC = "basicServer.xml";
    public static final String SERVER_XML_JSON_MESSAGES = "jsonMessagesLogServer.xml";
    public static final String SERVER_XML_JSON_CONSOLE = "jsonConsoleLogServer.xml";

    @BeforeClass
    public static void setUpClass() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "com.ibm.logs");
        server.startServer();
        Assert.assertNotNull("Test app LogstashApp does not appear to have started.", server.waitForStringInLog("CWWKT0016I:.*LogstashApp"));

        // Server is start with basic logging
        String line = server.waitForStringInLog("CWWKF0011I");
        Assert.assertNotNull("CWWKF0011I is not found", line);
        Assert.assertFalse("Log is in unexepcted JSON format. line=" + line, line.startsWith("{"));
    }

    @Test
    public void enableAndDisableMessagesLog() throws Exception {

        // Switch to JSON format
        String line = setConfig(SERVER_XML_JSON_MESSAGES);
        Assert.assertNotNull("Both CWWKG0017I and CWWKG0018I are not found", line);
        Assert.assertTrue("Log is in unexpected basic format.  line=" + line, line.startsWith("{"));

        // Switch back to simple (basic) format
        line = setConfig(SERVER_XML_BASIC);
        Assert.assertNotNull("Both CWWKG0017I and CWWKG0018I are not found", line);
        Assert.assertFalse("Log is in unexpected JSON format.  line=" + line, line.startsWith("{"));

    }

    @Test
    public void enableAndDisableConsoleLog() throws Exception {
        // Server is start with simple (basic) logging
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        String line = server.waitForStringInLog("CWWKF0011I", consoleLogFile);
        Assert.assertNotNull("CWWKF0011I is not found", line);
        Assert.assertFalse("Log is in unexepcted JSON format. line=" + line, line.startsWith("{"));

        // Switch to JSON format
        line = setConfig(SERVER_XML_JSON_CONSOLE, consoleLogFile);
        Assert.assertNotNull("Both CWWKG0017I and CWWKG0018I are not found", line);
        Assert.assertTrue("Log is in unexpected basic format.  line=" + line, line.startsWith("{"));

        // Switch back to simple (basic) format
        line = setConfig(SERVER_XML_BASIC);
        Assert.assertNotNull("Both CWWKG0017I and CWWKG0018I are not found", line);
        Assert.assertFalse("Log is in unexpected JSON format.  line=" + line, line.startsWith("{"));

    }

    private static String setConfig(String fileName, RemoteFile remoteFile) throws Exception {
        server.setMarkToEndOfLog(remoteFile);
        server.setServerConfigurationFile(fileName);
        return server.waitForStringInLogUsingMark("CWWKG0017I.*|CWWKG0018I.*", remoteFile);
    }

    private static String setConfig(String fileName) throws Exception {
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(fileName);
        return server.waitForStringInLogUsingMark("CWWKG0017I.*|CWWKG0018I.*");
    }

    @AfterClass
    public static void tearDownClass() {
        if ((server != null) && (server.isStarted())) {
            try {
                server.stopServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
