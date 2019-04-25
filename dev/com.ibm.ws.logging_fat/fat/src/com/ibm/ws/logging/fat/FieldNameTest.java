/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
public class FieldNameTest {
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

//        System.out.println("Starting server...");
//        server_xml.startServer();
//        System.out.println("Started server.");

        // Preserve the original server configuration
        server_xml.saveServerConfiguration();
        server_env.saveServerConfiguration();
        server_bootstrap.saveServerConfiguration();
    }

//    @Before
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
            server.stopServer("com.ibm.ws.logging.fat.ffdc.servlet.FFDCServlet.doGet", "ArithmeticException", // testIsoDateFormatInFFDC
                              "CWWKG0081E", "CWWKG0083W"); // testInvalidIsoDateFormatAttributeValue
        }
    }

    /*
     * This test sets the "messageFields" attribute in the server.env and verifies the property in the messages.log file.
     */
    @Test
    public void testMessageFieldNamesEnv() throws Exception {
        setUp(server_env);
        List<String> lines = server_env.findStringsInFileInLibertyServerRoot("log3", MESSAGE_LOG);
        assertTrue("The message field name was not formatted in the new configuration in messages.log.", lines.size() > 0);
    }

    /*
     * This test sets the "messageFields" attribute in the bootstrap.properties and verifies the property in the messages.log file.
     */
    @Test
    public void testMessageFieldNamesBootstrap() throws Exception {
//        RemoteFile bootstrapFile = server.getServerBootstrapPropertiesFile();
//        // Update bootstrap.properties file with messageFields property
//        Properties newBootstrapProps = new Properties();
//        newBootstrapProps.put("com.ibm.ws.logging.message.fields", "message:log2");
//
//        System.out.println("****New props: " + newBootstrapProps);
//        FileOutputStream out = getFileOutputStreamForRemoteFile(bootstrapFile, true);
//        writeProperties(newBootstrapProps, out);
        setUp(server_bootstrap);
        List<String> lines = server_bootstrap.findStringsInFileInLibertyServerRoot("log2", MESSAGE_LOG);
        assertTrue("The message field name was not formatted in the new configuration in messages.log.", lines.size() > 0);

        //now restore original bootstrap properties?
    }

    /*
     * This test sets the "messageFields" attribute in the server.xml and verifies the property in the messages.log file.
     */
    @Test
    public void testMessageFieldNamesXML() throws Exception {
        // Set messageFields property in server.xml
        setUp(server_xml);
        setServerConfiguration(true, "message:test1", server_xml);

        List<String> lines = server_xml.findStringsInFileInLibertyServerRoot("test1", MESSAGE_LOG);
        assertTrue("The message field name was not formatted in the new configuration in messages.log.", lines.size() > 0);
        setServerConfiguration(true, "message:test2", server_xml);
        lines = server_xml.findStringsInFileInLibertyServerRoot("test2", MESSAGE_LOG);
        assertTrue("The message field name was not updated in messages.log.", lines.size() > 0);
    }

    @Test
    public void messageFieldsErrorChecking() throws Exception {
        //server_xml.restoreServerConfiguration();
        // Set messageFields property in server.xml
        setUp(server_xml);
        //map the ibm_datetime field to blank field name
        setServerConfiguration(true, "ibm_datetime:", server_xml);

        //the default field name should be used
        List<String> lines = server_xml.findStringsInFileInLibertyServerRoot("ibm_datetime", MESSAGE_LOG);
        assertTrue("The default field name was not returned", lines.size() > 0);
    }

//    private FileInputStream getFileInputStreamForRemoteFile(RemoteFile bootstrapPropFile) throws Exception {
//        FileInputStream input = null;
//        try {
//            input = (FileInputStream) bootstrapPropFile.openForReading();
//        } catch (Exception e) {
//            throw new Exception("Error while getting the FileInputStream for the remote bootstrap properties file.");
//        }
//        return input;
//    }
//
//    private Properties loadProperties(FileInputStream input) throws IOException {
//        Properties props = new Properties();
//        try {
//            props.load(input);
//        } catch (IOException e) {
//
//            throw new IOException("Error while loading properties from the remote bootstrap properties file.");
//        } finally {
//            try {
//                input.close();
//            } catch (IOException e1) {
//                throw new IOException("Error while closing the input stream.");
//            }
//        }
//        return props;
//    }
//
//    private FileOutputStream getFileOutputStreamForRemoteFile(RemoteFile bootstrapPropFile, boolean append) throws Exception {
//        // Open the remote file for writing with append as false
//        FileOutputStream output = null;
//        try {
//            output = (FileOutputStream) bootstrapPropFile.openForWriting(append);
//        } catch (Exception e) {
//            throw new Exception("Error while getting FileOutputStream for the remote bootstrap properties file.");
//        }
//        return output;
//    }
//
//    private void writeProperties(Properties props, FileOutputStream output) throws Exception {
//        // Write the properties to remote bootstrap properties file
//        try {
//            props.store(output, null);
//            System.out.println("****Written to the output stream");
//        } catch (IOException e) {
//            throw new Exception("Error while writing to the remote bootstrap properties file.");
//        } finally {
//            try {
//                output.close();
//            } catch (IOException e) {
//                throw new IOException("Error while closing the output stream.");
//            }
//        }
//    }

    private static void setServerConfiguration(boolean isMessageFields, String newFieldName, LibertyServer server) throws Exception {
        Logging loggingObj;
        ServerConfiguration serverConfig = server.getServerConfiguration();
        loggingObj = serverConfig.getLogging();
        if (isMessageFields) {
            loggingObj.setMessageFields(newFieldName);
        }
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(serverConfig);
        server.waitForConfigUpdateInLogUsingMark(null);
    }
}
