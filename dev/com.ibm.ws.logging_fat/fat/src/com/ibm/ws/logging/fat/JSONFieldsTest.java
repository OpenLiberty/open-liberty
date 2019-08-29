/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.fat;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.Logging;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
@RunWith(FATRunner.class)
public class JSONFieldsTest {
    private static final String MESSAGE_LOG = "logs/messages.log";
    private static final String SERVER_NAME_XML = "com.ibm.ws.logging.fieldnamexml";
    private static final String SERVER_NAME_ENV = "com.ibm.ws.logging.fieldnameenv";
    private static final String SERVER_NAME_BOOTSTRAP = "com.ibm.ws.logging.fieldnamebootstrap";

    private static LibertyServer server_xml;
    private static LibertyServer server_env;
    private static LibertyServer server_bootstrap;

    @BeforeClass
    public static void initialSetup() throws Exception {
        server_xml = LibertyServerFactory.getLibertyServer(SERVER_NAME_XML);
        server_env = LibertyServerFactory.getLibertyServer(SERVER_NAME_ENV);
        server_bootstrap = LibertyServerFactory.getLibertyServer(SERVER_NAME_BOOTSTRAP);

        // Preserve the original server configuration
        server_xml.saveServerConfiguration();
        server_env.saveServerConfiguration();
        server_bootstrap.saveServerConfiguration();
    }

    public void setUp(LibertyServer server) throws Exception {
        if (server != null && !server.isStarted()) {
            // Restore the original server configuration, before starting the server for each test case.
            server.restoreServerConfiguration();
            server.startServer();
        }
    }

//    @After
    public void cleanUp(LibertyServer server) throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("com.ibm.ws.logging.fat.ffdc.servlet.FFDCServlet.doGet", "ArithmeticException",
                              "CWWKG0081E", "CWWKG0083W");
        }
    }

    /*
     * This test sets the "WLP_LOGGING_JSON_FIELDS" attribute in the server.env and verifies the property in the messages.log file.
     */
    @Test
    public void testMessageFieldNamesEnv() throws Exception {
        setUp(server_env);
        List<String> lines = server_env.findStringsInFileInLibertyServerRoot("log3", MESSAGE_LOG);
        assertTrue("The message field name was not formatted in the new configuration in messages.log.", lines.size() > 0);
    }

    /*
     * This test sets the "com.ibm.ws.logging.json.fields" attribute in the bootstrap.properties and verifies the property in the messages.log file.
     */
    @Test
    public void testMessageFieldNamesBootstrap() throws Exception {
        setUp(server_bootstrap);
        List<String> lines = server_bootstrap.findStringsInFileInLibertyServerRoot("log2", MESSAGE_LOG);
        assertTrue("The message field name was not formatted in the new configuration in messages.log.", lines.size() > 0);
    }

    /*
     * This test sets the "jsonFields" attribute in the server.xml and verifies the property in the messages.log file.
     */
    @Test
    public void testMessageFieldNamesXML() throws Exception {
        setUp(server_xml);
        setServerConfiguration(true, "message:test1", server_xml);

        List<String> lines = server_xml.findStringsInFileInLibertyServerRoot("test1", MESSAGE_LOG);
        assertTrue("The message field name was not formatted in the new configuration in messages.log.", lines.size() > 0);
        setServerConfiguration(true, "message:test2", server_xml);
        lines = server_xml.findStringsInFileInLibertyServerRoot("test2", MESSAGE_LOG);
        assertTrue("The message field name was not updated in messages.log.", lines.size() > 0);
    }

    @Test
    public void jsonFieldsEmptyMap() throws Exception {
        // Set jsonFields property in server.xml
        setUp(server_xml);
        //map the ibm_datetime field to blank field name
        setServerConfiguration(true, "ibm_datetime:", server_xml);

        //the default field name should be used
        List<String> lines = server_xml.findStringsInFileInLibertyServerRoot("ibm_datetime", MESSAGE_LOG);
        assertTrue("The default field name was not returned", lines.size() > 0);
    }

    @Test
    public void jsonFieldsUnknownKey() throws Exception {
        // Set jsonFields property in server.xml
        setUp(server_xml);
        //provide an unknown fieldname
        setServerConfiguration(true, "testing:error", server_xml);

        //a warning should be given when a non-recognized key is provided
        List<String> lines = server_xml.findStringsInFileInLibertyServerRoot("TRAS3009W", MESSAGE_LOG);
        assertTrue("The default field name was not returned", lines.size() > 0);
    }

    @Test
    public void jsonFieldsTooManyTokens() throws Exception {
        // Set jsonFields property in server.xml
        setUp(server_xml);
        //provide an unknown fieldname
        setServerConfiguration(true, "provide:too:many:tokens", server_xml);

        //a warning should be given when an entry with too many or too few tokens is provided
        List<String> lines = server_xml.findStringsInFileInLibertyServerRoot("TRAS3008W", MESSAGE_LOG);
        assertTrue("The default field name was not returned", lines.size() > 0);
    }

    @Test
    public void jsonFieldsWrongEventType() throws Exception {
        // Set jsonFields property in server.xml
        setUp(server_xml);
        //provide an unknown fieldname
        setServerConfiguration(true, "notevent:message:log", server_xml);

        //a warning should be given when an entry with too many or too few tokens is provided
        List<String> lines = server_xml.findStringsInFileInLibertyServerRoot("TRAS3010W", MESSAGE_LOG);
        assertTrue("The default field name was not returned", lines.size() > 0);
    }

    private static void setServerConfiguration(boolean isjsonFields, String newFieldName, LibertyServer server) throws Exception {
        Logging loggingObj;
        ServerConfiguration serverConfig = server.getServerConfiguration();
        loggingObj = serverConfig.getLogging();
        if (isjsonFields) {
            loggingObj.setjsonFields(newFieldName);
        }
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(serverConfig);
        server.waitForConfigUpdateInLogUsingMark(null);
    }
}
