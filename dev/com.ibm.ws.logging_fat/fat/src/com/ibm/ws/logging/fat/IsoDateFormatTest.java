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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Logging;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */
@RunWith(FATRunner.class)
public class IsoDateFormatTest {
    private static final String MESSAGE_LOG = "logs/messages.log";
    private static final String TRACE_LOG = "logs/trace.log";
    private static final String TRACE_SPEC = "com.ibm.ws.logging.*=all";
    private static final String SERVER_NAME = "com.ibm.ws.logging.isodateformat";
    private static final String INVALID_ISO_DATE_FORMAT_SERVER = "server-invalidIsoDateFormat.xml";
    private static final String ISO_8601_REGEX_PATTERN = "(\\d{4})\\-(\\d{2})\\-(\\d{2})T(\\d{2})\\:(\\d{2})\\:(\\d{2})\\.(\\d{3})[+-](\\d{4})"; //ISO 8601 Format : yyyy-MM-dd'T'HH:mm:ss.SSSZ
    private static final int CONN_TIMEOUT = 10;

    private static LibertyServer server;

    @BeforeClass
    public static void initialSetup() throws Exception {
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
        ShrinkHelper.defaultDropinApp(server, "ffdc-servlet", "com.ibm.ws.logging.fat.ffdc.servlet");

        System.out.println("Starting server...");
        server.startServer();
        System.out.println("Started server.");

        // Preserve the original server configuration
        server.saveServerConfiguration();
    }

    @Before
    public void setUp() throws Exception {
        if (server != null && !server.isStarted()) {
            // Restore the original server configuration, before starting the server for each test case.
            server.restoreServerConfiguration();
            server.startServer();
        }
    }

    @After
    public void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("com.ibm.ws.logging.fat.ffdc.servlet.FFDCServlet.doGet", "ArithmeticException", // testIsoDateFormatInFFDC
                              "CWWKG0081E", "CWWKG0083W"); // testInvalidIsoDateFormatAttributeValue
        }
    }

    /*
     * This test sets the "isoDateFormat" attribute to true and verifies the ISO-8601 date format in the messages.log file.
     */
    @Test
    public void testIsoDateFormatInMessagesLog() throws Exception {
        // Set isoDateFormat=true and traceSpec=off in server.xml
        setServerConfiguration(true, false);

        List<String> lines = server.findStringsInFileInLibertyServerRoot(ISO_8601_REGEX_PATTERN, MESSAGE_LOG);
        assertTrue("The date and time was not formatted in ISO-8601 format in messages.log.", lines.size() > 0);
    }

    /*
     * This test sets the "isoDateFormat" attribute to true and verifies the ISO-8601 date format in the trace.log file.
     */
    @Test
    public void testIsoDateFormatInTraceLog() throws Exception {
        // Set isoDateFormat=true and traceSpec=on in server.xml
        setServerConfiguration(true, true);

        List<String> lines = server.findStringsInFileInLibertyServerRoot(ISO_8601_REGEX_PATTERN, TRACE_LOG);
        assertTrue("The date and time was not formatted in ISO-8601 format in trace.log.", lines.size() > 0);
    }

    /*
     * This test sets the "isoDateFormat" attribute to true and verifies the ISO-8601 date format in the FFDC log and summary files.
     */
    @Test
    public void testIsoDateFormatInFFDC() throws Exception {
        // Set isoDateFormat = true and traceSpec=off in server.xml
        setServerConfiguration(true, false);

        // Run application to generate FFDC
        hitWebPage("ffdc-servlet", "FFDCServlet", true, "?generateFFDC=true");

        // Get latest FFDC file
        ArrayList<String> ffdcFiles = server.listFFDCFiles(SERVER_NAME);
        RemoteFile ffdcFile = server.getFFDCLogFile(ffdcFiles.get(ffdcFiles.size() - 1)); //Gets the latest FFDC file.

        List<String> lines = server.findStringsInFileInLibertyServerRoot(ISO_8601_REGEX_PATTERN, "logs/ffdc/" + ffdcFile.getName());
        assertTrue("The date and time was not formatted in ISO-8601 format in FFDC file.", lines.size() > 0);

        // Get latest FFDC Summary file
        ArrayList<String> ffdcSummaryFiles = server.listFFDCSummaryFiles(SERVER_NAME);
        RemoteFile ffdcSummaryFile = server.getFFDCSummaryFile(ffdcSummaryFiles.get(ffdcSummaryFiles.size() - 1)); //Gets the latest FFDC Summary file.

        lines = server.findStringsInFileInLibertyServerRoot(ISO_8601_REGEX_PATTERN, "logs/ffdc/" + ffdcSummaryFile.getName());
        assertTrue("The date and time was not formatted in ISO-8601 format in FFDC Summary file.", lines.size() > 0);
    }

    /*
     * This test sets the "isoDateFormat" attribute to an invalid value and verifies if the appropriate warning message is displayed
     * and checks that the previously configured date format is applied.
     */
    @Test
    public void testInvalidIsoDateFormatAttributeValue() throws Exception {
        // Set the invalid true value for attribute. e.g. "isoDateFormat=ture"
        server.setServerConfigurationFile(INVALID_ISO_DATE_FORMAT_SERVER);
        server.waitForConfigUpdateInLogUsingMark(null);

        List<String> lines = server.findStringsInFileInLibertyServerRoot("CWWKG0081E", MESSAGE_LOG);
        assertEquals("Error CWWKG0081E did not appear in messages.log", 1, lines.size());

        lines = server.findStringsInFileInLibertyServerRoot("CWWKG0083W", MESSAGE_LOG);
        assertEquals("Error CWWKG0083W did not appear in messages.log", 1, lines.size());

        // Verify that the ISO-8601 date format is not set, and the previously configured Locale date format is being used.
        lines = server.findStringsInFileInLibertyServerRoot(ISO_8601_REGEX_PATTERN, MESSAGE_LOG);
        assertFalse("The date and time is being formatted in ISO-8601 format, instead of the default Locale format.", lines.size() > 0);
    }

    /*
     * This test sets the "isoDateFormat" attribute to true in the bootstrap.properties and verifies the ISO-8601 date format in the FFDC log and summary files.
     */
    @Test
    public void testIsoDateFormatSetInBootstrapProperties() throws Exception {
        // Stop server, if running...
        if (server != null && server.isStarted()) {
            server.stopServer();
        }

        // Get the bootstrap.properties file and store the original content
        RemoteFile bootstrapFile = server.getServerBootstrapPropertiesFile();

        FileInputStream in = getFileInputStreamForRemoteFile(bootstrapFile);
        Properties initialBootstrapProps = loadProperties(in);

        try {
            // Update bootstrap.properties file with isoDateFormat = true
            Properties newBootstrapProps = new Properties();
            newBootstrapProps.put("com.ibm.ws.logging.isoDateFormat", "true");

            FileOutputStream out = getFileOutputStreamForRemoteFile(bootstrapFile, true);
            writeProperties(newBootstrapProps, out);

            // Start server...
            server.startServer();

            // Check in messages.log file to see if the date and time is formatted in ISO-8601 format
            List<String> lines = server.findStringsInFileInLibertyServerRoot(ISO_8601_REGEX_PATTERN, MESSAGE_LOG);
            assertTrue("The date and time was not formatted in ISO-8601 format in messages.log.", lines.size() > 0);
        } finally {
            // Restore the initial contents of bootstrap.properties
            FileOutputStream out = getFileOutputStreamForRemoteFile(bootstrapFile, false);
            writeProperties(initialBootstrapProps, out);
        }
    }

    /*
     * This test dynamically changes the isoDateFormat attribute to true and false, and verifies if the date and time are formatted accordingly each time.
     */
    @Test
    public void testDynamicallyUpdatingIsoDateFormatAttribute() throws Exception {
        // Verify if the date and time are in Locale format
        List<String> lines = server.findStringsInFileInLibertyServerRoot(ISO_8601_REGEX_PATTERN, MESSAGE_LOG);
        assertFalse("The date and time is being formatted in ISO-8601 format, instead of the default Locale format.", lines.size() > 0);

        // Set the isoDateFormat=true and traceSpec=off in server.xml
        setServerConfiguration(true, false);

        // Verify if the server was successfully updated
        lines = server.findStringsInFileInLibertyServerRoot("CWWKG0017I", MESSAGE_LOG);
        assertEquals("Message CWWKG0017I not appeared or appeared more than once ", 1, lines.size());

        // Verify the date and time are in ISO-8601 format
        lines = server.findStringsInFileInLibertyServerRoot(ISO_8601_REGEX_PATTERN, MESSAGE_LOG);
        assertTrue("The date and time was not formatted in ISO-8601 format in messages.log.", lines.size() > 0);

        // Set the isoDateFormat=false and traceSpec=off in server.xml
        setServerConfiguration(false, false);

        // Verify if the server was successfully updated again.
        lines = server.findStringsInFileInLibertyServerRoot("CWWKG0017I", MESSAGE_LOG);
        assertEquals("Message CWWKG0017I not appeared or appeared more than twice ", 2, lines.size());

        // Verify if the date and time are in Locale format, by getting the latest server update success message (CWWKG0017I)
        // and verifying if the string does not contain the ISO-8601 date format
        String latestServerUpdateSuccessMsg = lines.get(1);
        assertFalse("The date and time is being formatted in ISO-8601 format, instead of the default Locale format.", latestServerUpdateSuccessMsg.matches(ISO_8601_REGEX_PATTERN));
    }

    /*
     * This test verifies isoDateFormat for date and time when the attribute is set in both bootstrap.properties and server.xml
     */
    @Test
    public void testIsoDateFormatInPropertiesXML() throws Exception {

        // Stop server, if running...
        if (server != null && server.isStarted()) {
            server.stopServer();
        }

        // Get the bootstrap.properties file and store the original content
        RemoteFile bootstrapFile = server.getServerBootstrapPropertiesFile();

        FileInputStream in = getFileInputStreamForRemoteFile(bootstrapFile);
        Properties initialBootstrapProps = loadProperties(in);

        try {
            // Update bootstrap.properties file with isoDateFormat = true
            Properties newBootstrapProps = new Properties();
            newBootstrapProps.put("com.ibm.ws.logging.isoDateFormat", "false");

            FileOutputStream out = getFileOutputStreamForRemoteFile(bootstrapFile, true);
            writeProperties(newBootstrapProps, out);

            // Start server...
            server.startServer();

            // isoDateFormat=true and traceSpec=off
            setServerConfiguration(true, false);

            // Check in messages.log file to see if the date and time is formatted in ISO-8601 format
            List<String> lines = server.findStringsInFileInLibertyServerRoot(ISO_8601_REGEX_PATTERN, MESSAGE_LOG);
            assertTrue("The date and time was not formatted in ISO-8601 format in messages.log.", lines.size() > 0);
        } finally {
            // Restore the initial contents of bootstrap.properties
            FileOutputStream out = getFileOutputStreamForRemoteFile(bootstrapFile, false);
            writeProperties(initialBootstrapProps, out);
        }
    }

    private FileInputStream getFileInputStreamForRemoteFile(RemoteFile bootstrapPropFile) throws Exception {
        FileInputStream input = null;
        try {
            input = (FileInputStream) bootstrapPropFile.openForReading();
        } catch (Exception e) {
            throw new Exception("Error while getting the FileInputStream for the remote bootstrap properties file.");
        }
        return input;
    }

    private Properties loadProperties(FileInputStream input) throws IOException {
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

    private FileOutputStream getFileOutputStreamForRemoteFile(RemoteFile bootstrapPropFile, boolean append) throws Exception {
        // Open the remote file for writing with append as false
        FileOutputStream output = null;
        try {
            output = (FileOutputStream) bootstrapPropFile.openForWriting(append);
        } catch (Exception e) {
            throw new Exception("Error while getting FileOutputStream for the remote bootstrap properties file.");
        }
        return output;
    }

    private void writeProperties(Properties props, FileOutputStream output) throws Exception {
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

    private static void setServerConfiguration(boolean useIsoDateFormat, boolean useTraceSpec) throws Exception {
        Logging loggingObj;
        ServerConfiguration serverConfig = server.getServerConfiguration();
        loggingObj = serverConfig.getLogging();
        if (useTraceSpec) {
            loggingObj.setTraceSpecification(TRACE_SPEC);
        }
        loggingObj.setIsoDateFormat(useIsoDateFormat);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(serverConfig);
        server.waitForConfigUpdateInLogUsingMark(null);
    }

    private static void hitWebPage(String contextRoot, String servletName, boolean failureAllowed, String params) throws MalformedURLException, IOException, ProtocolException {
        try {
            String urlStr = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + contextRoot + "/" + servletName;
            urlStr = params != null ? urlStr + params : urlStr;
            URL url = new URL(urlStr);
            int expectedResponseCode = failureAllowed ? HttpURLConnection.HTTP_INTERNAL_ERROR : HttpURLConnection.HTTP_OK;
            HttpURLConnection con = HttpUtils.getHttpConnection(url, expectedResponseCode, CONN_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            // Make sure the server gave us something back
            assertNotNull(line);
            con.disconnect();
        } catch (IOException e) {
            // A message about a 500 code may be fine
            if (!failureAllowed) {
                throw e;
            }
        }
    }
}