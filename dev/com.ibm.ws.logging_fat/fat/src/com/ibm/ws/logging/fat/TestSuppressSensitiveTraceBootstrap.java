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
package com.ibm.ws.logging.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.Logging;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 *
 * This FAT tests the setting the supressSensitiveTrace logging property as a bootstrap property.
 *
 */
@RunWith(FATRunner.class)
public class TestSuppressSensitiveTraceBootstrap {
    private static final Class<?> c = TestSuppressSensitiveTraceBootstrap.class;

    private static final String SERVER_NAME = "com.ibm.ws.logging.suppresssensitivetrace";

    private static final String DEV_FORMAT = "dev";

    private static final String[] EXPECTED_FAILURES = { "SRVE0777E", "SRVE0315E", "CWWKG0032W", "CWWKG0083W", "CWWKG0075E" };

    private static RemoteFile bootstrapFile = null;
    private static Properties initialBootstrapProps = null;

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Get the bootstrap.properties file
        bootstrapFile = server.getServerBootstrapPropertiesFile();

        // Set the trace specification in bootstrap.properties before tests begin
        setInBootstrapPropertiesFile(server, bootstrapFile, "com.ibm.ws.logging.trace.specification", "com.ibm.ws.logging*=all:com.ibm.ws.wssecurity.cxf*=all");

        // Store the original content of the bootstrap.properties
        FileInputStream in = getFileInputStreamForRemoteFile(bootstrapFile);
        initialBootstrapProps = loadProperties(in);

        // Preserve the original server configuration
        server.saveServerConfiguration();
    }

    @Before
    public void setupTestStart() throws Exception {
        if (server != null && !server.isStarted()) {
            // Restore the original server configuration, with the default settings
            server.restoreServerConfiguration();
            server.startServer(true);
        }
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(EXPECTED_FAILURES);
        }

        // Restore the initial contents of bootstrap.properties
        FileOutputStream out = getFileOutputStreamForRemoteFile(bootstrapFile, false);
        writeProperties(initialBootstrapProps, out);
    }

    /*
     * This test sets the "suppressSensitiveTrace" attribute equivalent property "com.ibm.ws.logging.suppress.sensitive.trace" to true
     * in the bootstrap.properties file and verifies if the logging configuration is set properly.
     */
    @Test
    public void testSupressSensitiveTraceInBootstrapProperties() throws Exception {
        // Set the com.ibm.ws.logging.suppress.sensitive.trace property to true in bootstrap.properties
        setInBootstrapPropertiesFile(server, bootstrapFile, "com.ibm.ws.logging.suppress.sensitive.trace", "true");

        // Set the consoleFormat="dev", to trigger a server configuration update.
        setServerConfiguration(server, DEV_FORMAT);

        // Retrieve the traceLogFile RemoteFile
        RemoteFile traceLogFile = server.getDefaultTraceFile();

        // Verify in the trace.log file to see if the suppressSensitiveTrace bootstrap property was configured and set correctly.
        List<String> lines = server.findStringsInLogs("suppressSensitiveTrace=true", traceLogFile);
        Log.info(c, "testSupressSensitiveTraceInBootstrapProperties", "The following lines were found : " + lines);
        assertTrue("The suppressSensitiveTrace bootstrap property was not set properly.", lines.size() > 0);
    }

    /*
     * This test sets an invalid property name (com.ibm.ws.logging.supress.sensitive.traces) property in the bootstrap.properties file
     * and starts the server and verifies the correct property name (com.ibm.ws.logging.suppress.sensitive.trace) and default (false) value is used, instead.
     */
    @Test
    public void testInvalidSupressSensitiveTracePropNameInBootstrapProperties() throws Exception {
        // Set the invalid com.ibm.ws.logging.supress.sensitive.traces property to true in bootstrap.properties
        setInBootstrapPropertiesFile(server, bootstrapFile, "com.ibm.ws.logging.supress.sensitive.traces", "true");

        // Set the consoleFormat="dev", to trigger a server configuration update.
        setServerConfiguration(server, DEV_FORMAT);

        // Retrieve the traceLogFile RemoteFile
        RemoteFile traceLogFile = server.getDefaultTraceFile();

        // Verify in the trace.log file to see if the suppressSensitiveTrace bootstrap property was set to the correct name with the default value.
        List<String> lines = server.findStringsInLogs("suppressSensitiveTrace=false", traceLogFile);
        Log.info(c, "testSupressSensitiveTraceInBootstrapProperties", "The following lines were found : " + lines);
        assertTrue("The suppressSensitiveTrace bootstrap property was not set properly.", lines.size() > 0);
    }

    /*
     * This test sets the "suppressSensitiveTrace" attribute equivalent property "com.ibm.ws.logging.suppress.sensitive.trace" to an invalid value (flase)
     * in the bootstrap.properties file and starts the server and verifies the correct default (false) value is used.
     */
    @Test
    public void testInvalidSupressSensitiveTraceValueInBootstrapProperties() throws Exception {
        // Set the invalid com.ibm.ws.logging.supress.sensitive.traces property to true in bootstrap.properties
        setInBootstrapPropertiesFile(server, bootstrapFile, "com.ibm.ws.logging.suppress.sensitive.trace", "flase");

        // Verify if the CWWKG0083W WARNING message appeared in the logs, indicating an invalid value is being used.
        String cwwkg0083wLine = server.waitForStringInLog("CWWKG0083W");
        assertNotNull("Warning CWWKG0083W did not appear in the logs.", cwwkg0083wLine);

        // Verify if the CWWKG0075E ERROR message appeared in the logs, indicating the default value will be used, that is set in the metatype.xml.
        String cwwkg0075eLine = server.waitForStringInLog("CWWKG0075E");
        assertNotNull("Warning CWWKG0075E did not appear in the logs.", cwwkg0075eLine);
    }

    private static FileInputStream getFileInputStreamForRemoteFile(RemoteFile bootstrapPropFile) throws Exception {
        FileInputStream input = null;
        try {
            input = (FileInputStream) bootstrapPropFile.openForReading();
        } catch (Exception e) {
            throw new Exception("Error while getting the FileInputStream for the remote bootstrap properties file.");
        }
        return input;
    }

    private static Properties loadProperties(FileInputStream input) throws IOException {
        Properties props = new Properties();
        try {
            props.load(input);
        } catch (IOException e) {

            throw new IOException("Error while loading properties from the remote bootstrap properties file.");
        } finally {
            try {
                input.close();
            } catch (IOException e1) {
                throw new IOException("Error while closing the input stream.");
            }
        }
        return props;
    }

    private static FileOutputStream getFileOutputStreamForRemoteFile(RemoteFile bootstrapPropFile, boolean append) throws Exception {
        // Open the remote file for writing with append as false
        FileOutputStream output = null;
        try {
            output = (FileOutputStream) bootstrapPropFile.openForWriting(append);
        } catch (Exception e) {
            throw new Exception("Error while getting FileOutputStream for the remote bootstrap properties file.");
        }
        return output;
    }

    private static void writeProperties(Properties props, FileOutputStream output) throws Exception {
        // Write the properties to remote bootstrap properties file
        try {
            props.store(output, null);
        } catch (IOException e) {
            throw new Exception("Error while writing to the remote bootstrap properties file.");
        } finally {
            try {
                output.close();
            } catch (IOException e) {
                throw new IOException("Error while closing the output stream.");
            }
        }
    }

    private static void setInBootstrapPropertiesFile(LibertyServer libertyServer, RemoteFile bootstrapFile, String key, String value) throws Exception {
        // Stop server, if running...
        if (libertyServer != null && libertyServer.isStarted()) {
            libertyServer.stopServer(EXPECTED_FAILURES);
        }

        // Update the bootstrap.properties file
        Properties newBootstrapProps = new Properties();
        newBootstrapProps.put(key, value);

        FileOutputStream out = getFileOutputStreamForRemoteFile(bootstrapFile, true);
        writeProperties(newBootstrapProps, out);

        // Start server...
        libertyServer.startServer();
    }

    private static void setServerConfiguration(LibertyServer libertyServer, String consoleFormat) throws Exception {
        // Update the console format server configuration to trigger a logging trace...
        Logging loggingObj;
        ServerConfiguration serverConfig = libertyServer.getServerConfiguration();
        loggingObj = serverConfig.getLogging();
        loggingObj.setConsoleFormat(consoleFormat);
        libertyServer.updateServerConfiguration(serverConfig);
        libertyServer.waitForConfigUpdateInLogUsingMark(null);
    }
}