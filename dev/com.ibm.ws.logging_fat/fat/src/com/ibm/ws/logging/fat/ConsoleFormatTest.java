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
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Logging;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 *
 * This FAT tests the different scenarios for the ["dev", "simple"] formats in the consoleFormat attribute for the logging server configuration.
 * Note: The ["json"] format for the consoleFormat attribute is tested in the com.ibm.ws.logging.json_fat project.
 *
 */
@RunWith(FATRunner.class)
public class ConsoleFormatTest {
    private static final Class<?> c = ConsoleFormatTest.class;

    private static final String TRACE_SPEC = "com.ibm.ws.logging.*=all";

    private static final String DEV_FORMAT = "dev";
    private static final String SIMPLE_FORMAT = "simple";
    private static final String DEPRECATED_BASIC_FORMAT = "basic";
    private static final String INVALID_CONSOLE_FORMAT = "simples";

    private static final String SERVER_NAME = "com.ibm.ws.logging.consoleformat";
    private static final String SERVER_ENV_NAME = "com.ibm.ws.logging.consoleformatenv";

    private static final String DEV_FORMAT_REGEX_PATTERN = "([A-Z]{3,}   )"; // Matches with line that has [<LOG_LEVEL>   ] in the beginning. e.g. [AUDIT   ]
    private static final String SIMPLE_FORMAT_REGEX_PATTERN = "(\\[\\d{1,4}.*)"; // Matches with line that [23/02/20 ... ] in the beginning.
    private static final String ISO_8601_REGEX_PATTERN = "(\\d{4})\\-(\\d{2})\\-(\\d{2})T(\\d{2})\\:(\\d{2})\\:(\\d{2})\\.(\\d{3})[+-](\\d{4})"; //ISO 8601 Format : yyyy-MM-dd'T'HH:mm:ss.SSSZ
    private static final String INTERNAL_CLASSES_REGEXP = "at \\[internal classes\\]";

    private static final String[] EXPECTED_FAILURES = { "SRVE0777E", "SRVE0315E", "CWWKG0032W", "CWWKG0075E" };

    private static final int CONN_TIMEOUT = 10;

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @Server(SERVER_ENV_NAME)
    public static LibertyServer serverEnv;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "logger-servlet", "com.ibm.ws.logging.fat.logger.servlet");
        ShrinkHelper.defaultDropinApp(server, "broken-servlet", "com.ibm.ws.logging.fat.broken.servlet");
        server.startServer();

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
    }

    /*
     * This test dynamically changes the consoleFormat from dev to simple and vice-versa, and verifies if new messages are in the new correct format.
     */
    @Test
    public void testDynamicSimpleFormat() throws Exception {
        // Retrieve the consoleLogFile RemoteFile
        RemoteFile consoleLogFile = server.getConsoleLogFile();

        // Verify if the console logging format is in the default dev format
        List<String> lines = server.findStringsInLogs(DEV_FORMAT_REGEX_PATTERN, consoleLogFile);
        assertTrue("The console log is not in dev format.", lines.size() > 0);

        // Set the consoleFormat="simple" and traceSpec=off in server.xml
        setServerConfiguration(server, SIMPLE_FORMAT, false, false, consoleLogFile);

        // Verify if the server was successfully updated
        String line = server.waitForStringInLogUsingMark("CWWKG0017I", consoleLogFile);
        Log.info(c, "testDynamicSimpleConsoleFormat", "The simple console formatted line : " + line);
        assertNotNull("Message CWWKG0017I not appeared or appeared more than once ", line);

        // Verify if the console.log file is in the simple format
        assertTrue("The console.log file was not formatted to the simple format.", isStringinSimpleFormat(line));

        // Set the consoleFormat="dev" and traceSpec=off in server.xml
        setServerConfiguration(server, DEV_FORMAT, false, false, consoleLogFile);

        // Verify if the server was successfully updated again.
        line = server.waitForStringInLogUsingMark("CWWKG0017I", consoleLogFile);
        Log.info(c, "testDynamicSimpleConsoleFormat", "The default dev console formatted line : " + line);
        assertNotNull("Message CWWKG0017I not appeared or appeared more than once ", line);

        // Verify if the console log is back to the default dev format, by getting the latest server update success message (CWWKG0017I)
        assertTrue("The console.log file was not formatted to the default dev format.", isStringinDevFormat(line));
    }

    /*
     * This test sets the "consoleFormat" attribute to an invalid value and verifies if the appropriate warning message is displayed
     * and checks that the default format is applied.
     */
    @Test
    public void testInvalidConsoleFormat() throws Exception {
        // Retrieve the consoleLogFile RemoteFile
        RemoteFile consoleLogFile = server.getConsoleLogFile();

        // Set the consoleFormat="simples" and traceSpec=off in server.xml
        setServerConfiguration(server, INVALID_CONSOLE_FORMAT, false, false, consoleLogFile);

        // Verify if the WARNING message appeared in the logs.
        String line = server.waitForStringInLogUsingMark("CWWKG0032W", consoleLogFile);
        Log.info(c, "testInvalidConsoleFormat", "The invalid console format warning message is : " + line);
        assertNotNull("Warning CWWKG0032W did not appear in console.log", line);

        // Verify if the server was successfully updated again.
        line = server.waitForStringInLogUsingMark("CWWKG0017I", consoleLogFile);
        Log.info(c, "testInvalidConsoleFormat", "The default dev console formatted line : " + line);

        // Verify if the console log is back to the default dev format, by getting the latest message.
        assertTrue("The console.log file was not formatted to the default dev format.", isStringinDevFormat(line));
    }

    /*
     * This test sets the "consoleFormat" attribute to the deprecated basic console format and verifies if the appropriate warning message is displayed
     * and checks that the default format is applied.
     */
    @Test
    public void testDeprecatedBasicConsoleFormat() throws Exception {
        // Retrieve the consoleLogFile RemoteFile
        RemoteFile consoleLogFile = server.getConsoleLogFile();

        // Set the consoleFormat="basic" and traceSpec=off in server.xml
        setServerConfiguration(server, DEPRECATED_BASIC_FORMAT, false, false, consoleLogFile);

        // Verify if the WARNING message appeared in the logs.
        String line = server.waitForStringInLogUsingMark("CWWKG0032W", consoleLogFile);
        Log.info(c, "testDeprecatedBasicConsoleFormat", "The invalid console format warning message is : " + line);
        assertNotNull("Warning CWWKG0032W did not appear in console.log", line);

        // Verify if the server was successfully updated again.
        line = server.waitForStringInLogUsingMark("CWWKG0017I", consoleLogFile);
        Log.info(c, "testDeprecatedBasicConsoleFormat", "The default dev console formatted line : " + line);

        // Verify if the console log is back to the default dev format, by getting the latest message.
        assertTrue("The console.log file was not formatted to the default dev format.", isStringinDevFormat(line));
    }

    /*
     * This test sets the "consoleFormat" attribute to simple in the bootstrap.properties and verifies the correct format in the console.log file.
     */
    @Test
    public void testSimpleFormatSetInBootstrapProperties() throws Exception {
        // Get the bootstrap.properties file and store the original content
        RemoteFile bootstrapFile = server.getServerBootstrapPropertiesFile();
        FileInputStream in = getFileInputStreamForRemoteFile(bootstrapFile);
        Properties initialBootstrapProps = loadProperties(in);

        try {
            // Set the consoleFormat to simple in bootstrap.properties
            setInBootstrapPropertiesFile(server, bootstrapFile, "com.ibm.ws.logging.console.format", SIMPLE_FORMAT);

            // Retrieve the consoleLogFile RemoteFile
            RemoteFile consoleLogFile = server.getConsoleLogFile();

            // Check in console.log file to see if the message is formatted in the simple console format
            List<String> lines = server.findStringsInLogs(SIMPLE_FORMAT_REGEX_PATTERN, consoleLogFile);
            Log.info(c, "testSimpleFormatSetInBootstrapProperties", "The simple console formatted lines : " + lines);
            assertTrue("The console log is not in simple format.", lines.size() > 0);

        } finally {
            // Restore the initial contents of bootstrap.properties
            FileOutputStream out = getFileOutputStreamForRemoteFile(bootstrapFile, false);
            writeProperties(initialBootstrapProps, out);
        }
    }

    /*
     * This test sets the "consoleFormat" attribute to an invalid format in the bootstrap.properties and starts the server and verifies the correct default dev format in the
     * console.log file.
     */
    @Test
    public void testInvalidConsoleFormatSetInBootstrapProperties() throws Exception {
        // Get the bootstrap.properties file and store the original content
        RemoteFile bootstrapFile = server.getServerBootstrapPropertiesFile();
        FileInputStream in = getFileInputStreamForRemoteFile(bootstrapFile);
        Properties initialBootstrapProps = loadProperties(in);

        try {
            // Set the consoleFormat to simples in bootstrap.properties
            setInBootstrapPropertiesFile(server, bootstrapFile, "com.ibm.ws.logging.console.format", INVALID_CONSOLE_FORMAT);

            // Retrieve the consoleLogFile RemoteFile
            RemoteFile consoleLogFile = server.getConsoleLogFile();

            // Verify if the WARNING message appeared in the logs.
            String line = server.waitForStringInLogUsingMark("CWWKG0032W", consoleLogFile);
            assertNotNull("Warning CWWKG0032W did not appear in console.log", line);

            // Verify if the WARNING message appeared in the logs.
            line = server.waitForStringInLogUsingMark("CWWKG0075E", consoleLogFile);
            assertNotNull("Error CWWKG0075E did not appear in console.log", line);

            // Check in console.log file to see if the message is formatted in the default dev console format
            List<String> lines = server.findStringsInLogs(DEV_FORMAT_REGEX_PATTERN, consoleLogFile);
            Log.info(c, "testSimpleFormatSetInBootstrapProperties", "The default console formatted lines : " + lines);
            assertFalse("The console log is in simple format.", lines.isEmpty());

        } finally {
            // Restore the initial contents of bootstrap.properties
            FileOutputStream out = getFileOutputStreamForRemoteFile(bootstrapFile, false);
            writeProperties(initialBootstrapProps, out);
        }
    }

    /*
     * This test sets both simple console format and isoDateFormat=true and verifies if the message is formatted correctly.
     */
    @Test
    public void testSimpleConsoleFormatWithIsoDateFormat() throws Exception {
        // Retrieve the consoleLogFile RemoteFile
        RemoteFile consoleLogFile = server.getConsoleLogFile();

        // Set the consoleFormat="simple", traceSpec=off, isoDateFormat=true in server.xml
        setServerConfiguration(server, SIMPLE_FORMAT, false, true, consoleLogFile);

        // Verify if the server was successfully updated
        String line = server.waitForStringInLogUsingMark("CWWKG0017I", consoleLogFile);
        Log.info(c, "testSimpleConsoleFormatWithIsoDateFormat", "The simple console formatted lines : " + line);
        assertNotNull("Message CWWKG0017I not appeared or appeared more than once ", line);

        // Verify if the console.log file has the date and time stamps formatted in ISO-8601 format.
        assertTrue("The console.log file was not formatted to the simple format.", isStringinSimpleFormat(line));
        assertFalse("The date and time stamps in the console.log file was not formatted in ISO-8601 format.", line.matches(ISO_8601_REGEX_PATTERN));

    }

    /*
     * This test sets consoleFormat=simple and verifies if SystemOut and SystemErr messages are formatted correctly.
     */
    @Test
    public void testSimpleConsoleFormatWithSysOutSysErrMsgs() throws Exception {
        // Retrieve the consoleLogFile RemoteFile
        RemoteFile consoleLogFile = server.getConsoleLogFile();

        // Set the consoleFormat="simple", traceSpec=off, isoDateFormat=false in server.xml
        setServerConfiguration(server, SIMPLE_FORMAT, false, false, consoleLogFile);

        // Verify if the server was successfully updated
        String line = server.waitForStringInLogUsingMark("CWWKG0017I", consoleLogFile);
        assertNotNull("Message CWWKG0017I did not appear.", line);

        // Run application to generate SystemOut and SystemErr messages
        hitWebPage("logger-servlet", "LoggerServlet", false, null);

        // Verify the SystemOut message
        line = server.waitForStringInLog("Hello, this is just a SystemOut message!", consoleLogFile);
        Log.info(c, "testSimpleConsoleFormatWithSysOutSysErrMsgs", "The SystemOut message in simple format : " + line);
        assertNotNull("The SystemOut message did not appear in the console.log file", line);
        assertTrue("The SystemOut message is not in the simple console format.", isStringinSimpleFormat(line));

        // Verify the SystemErr message
        line = server.waitForStringInLog("Bye, this is just a SystemErr message!", consoleLogFile);
        Log.info(c, "testSimpleConsoleFormatWithSysOutSysErrMsgs", "The SystemErr message in simple format : " + line);
        assertNotNull("The SystemErr message did not appear in the console.log file", line);
        assertTrue("The SystemErr message is not in the simple console format.", isStringinSimpleFormat(line));

    }

    /*
     * This test sets consoleFormat=simple and generates an exception and verifies if the exception is not trimmed/suppressed are formatted correctly.
     */
    @Test
    @AllowedFFDC
    public void testSimpleConsoleFormatWithException() throws Exception {
        // Retrieve the consoleLogFile RemoteFile
        RemoteFile consoleLogFile = server.getConsoleLogFile();

        // Set the consoleFormat="simple", traceSpec=off, isoDateFormat=false in server.xml
        setServerConfiguration(server, SIMPLE_FORMAT, false, false, consoleLogFile);

        // Verify if the server was successfully updated
        String line = server.waitForStringInLogUsingMark("CWWKG0017I", consoleLogFile);
        assertNotNull("Message CWWKG0017I did not appear.", line);

        // Run application to generate SystemOut and SystemErr messages
        hitWebPage("broken-servlet", "BrokenWithABadlyWrittenThrowableServlet", true, null);

        // Verify if the exception appeared and is in the simple format
        line = server.waitForStringInLog("An exception occurred: java.lang.Throwable:", consoleLogFile);
        Log.info(c, "testSimpleConsoleFormatWithException", "The exception message in simple format : " + line);
        assertNotNull("The exception message did not appear in the console.log file", line);
        assertTrue("The exception message is not in the simple console format.", isStringinSimpleFormat(line));

        // Verify if the exception is complete, and not trimmed and/or suppressed
        List<String> lines = server.findStringsInLogs(INTERNAL_CLASSES_REGEXP, consoleLogFile);
        assertTrue("The SystemErr message is not in the  simple console format.", lines.isEmpty());
    }

    /*
     * This test sets the "consoleFormat" attribute to simple as Environment Variables via the server.env file and verifies the correct format in the console.log file.
     */
    @Test
    public void testSimpleFormatSetInEnv() throws Exception {
        // Stop the default server, if running...
        if (server != null && server.isStarted()) {
            server.stopServer(EXPECTED_FAILURES);
        }

        // Start the server with the server.env file configured with the consoleFormat=simple
        serverEnv.startServer();

        // Retrieve the consoleLogFile RemoteFile
        RemoteFile consoleLogFile = serverEnv.getConsoleLogFile();

        // Verify if the console logging format is not in the default dev format, and is in the simple format
        List<String> lines = serverEnv.findStringsInLogs(SIMPLE_FORMAT_REGEX_PATTERN, consoleLogFile);
        assertTrue("The console log is not in simple format.", lines.size() > 0);

        // Stop the serverEnv
        if (serverEnv != null && serverEnv.isStarted()) {
            serverEnv.stopServer(EXPECTED_FAILURES);
        }
    }

    /*
     * This test verifies consoleFormat when the attribute is set in server.env, bootstrap.properties and server.xml
     * server.env: WLP_LOGGING_CONSOLE_FORMAT=simple
     * bootstrap.properties: com.ibm.ws.logging.console.format=dev
     * server.xml: <logging consoleFormat="simple"/>
     */
    @Test
    public void testConsoleFormatInEnvPropertiesXML() throws Exception {

        // Stop the default server, if running...
        if (server != null && server.isStarted()) {
            server.stopServer(EXPECTED_FAILURES);
        }

        // Start the server with the server.env file configured with the consoleFormat=simple
        serverEnv.startServer();

        // Get the bootstrap.properties file and store the original content
        RemoteFile bootstrapFile = serverEnv.getServerBootstrapPropertiesFile();
        FileInputStream in = getFileInputStreamForRemoteFile(bootstrapFile);
        Properties initialBootstrapProps = loadProperties(in);

        try {

            // Set the messageFormat to basic in bootstrap.properties
            // Start the server with the server.env file configured with the consoleFormat=simple
            setInBootstrapPropertiesFile(serverEnv, bootstrapFile, "com.ibm.ws.logging.console.format", DEV_FORMAT);

            RemoteFile consoleLogFile = serverEnv.getConsoleLogFile();

            // Check in console.log file to see if the message is formatted in the simple console format
            List<String> lines = serverEnv.findStringsInLogs(DEV_FORMAT_REGEX_PATTERN, consoleLogFile);
            Log.info(c, "testConsoleFormatInEnvPropertiesXML", "The dev console formatted lines : " + lines);
            assertTrue("The console log is not in dev format.", lines.size() > 0);

            // Set the consoleFormat="simple" and traceSpec=off in server.xml
            setServerConfiguration(serverEnv, SIMPLE_FORMAT, false, false, consoleLogFile);

            // Verify if the console.log file is in the simple format
            String line = serverEnv.waitForStringInLogUsingMark("CWWKG0017I", consoleLogFile);
            Log.info(c, "testBootstrapServerXMLConsoleFormat", "The simple console formatted line : " + line);
            assertTrue("The console.log file was not formatted to the simple format.", isStringinSimpleFormat(line));

        } finally {
            // Restore the initial contents of bootstrap.properties
            FileOutputStream out = getFileOutputStreamForRemoteFile(bootstrapFile, false);
            writeProperties(initialBootstrapProps, out);
        }

        // Stop the serverEnv
        if (serverEnv != null && serverEnv.isStarted()) {
            serverEnv.stopServer(EXPECTED_FAILURES);
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

    private static void setServerConfiguration(LibertyServer Server, String consoleFormat, boolean useTraceSpec, boolean useIsoDateFormat,
                                               RemoteFile consoleLogFile) throws Exception {
        Logging loggingObj;
        ServerConfiguration serverConfig = Server.getServerConfiguration();
        loggingObj = serverConfig.getLogging();
        if (useTraceSpec) {
            loggingObj.setTraceSpecification(TRACE_SPEC);
        }
        if (useIsoDateFormat) {
            loggingObj.setIsoDateFormat(useIsoDateFormat);
        }
        loggingObj.setConsoleFormat(consoleFormat);
        Server.setMarkToEndOfLog(consoleLogFile);
        Server.updateServerConfiguration(serverConfig);
        Server.waitForConfigUpdateInLogUsingMark(null);
    }

    private static boolean isStringinDevFormat(String text) {
        return Pattern.compile(DEV_FORMAT_REGEX_PATTERN).matcher(text).find();
    }

    private static boolean isStringinSimpleFormat(String text) {
        return Pattern.compile(SIMPLE_FORMAT_REGEX_PATTERN).matcher(text).find();
    }

    private void setInBootstrapPropertiesFile(LibertyServer Server, RemoteFile bootstrapFile, String key, String value) throws Exception {
        // Stop server, if running...
        if (Server != null && Server.isStarted()) {
            Server.stopServer(EXPECTED_FAILURES);
        }

        // Update bootstrap.properties file with consoleFormat=simple
        Properties newBootstrapProps = new Properties();
        newBootstrapProps.put(key, value);

        FileOutputStream out = getFileOutputStreamForRemoteFile(bootstrapFile, true);
        writeProperties(newBootstrapProps, out);

        // Start server...
        Server.startServer();
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