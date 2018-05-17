/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.json.fat;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;

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
public class JsonConfigTest {

    protected static final Class<?> c = JsonConfigTest.class;

    @Server("com.ibm.ws.logging.json.JsonConfigServer")
    public static LibertyServer server;

    public static final String APP_NAME = "LogstashApp";
    public static final String SERVER_XML_CLLERROR = "consoleLogLevelError.xml";
    public static final String SERVER_XML_CLLWARNING = "consoleLogLevelWarning.xml";
    public static final String SERVER_XML_CLLINFO_BASIC_CONSOLE = "consoleLogLevelInfoBasicConsole.xml";
    public static final String SERVER_XML_BASIC_CONSOLE_JSON_MESSAGE = "basicConsoleJsonMessage.xml";
    public static final String SERVER_XML_JSON_CONSOLE_JSON_MESSAGE = "jsonConsoleJsonMessage.xml";
    public static final String SERVER_XML_BASIC_CONSOLE_BASIC_MESSAGE = "basicConsoleBasicMessage.xml";
    public static final String SERVER_XML_BASIC_CONSOLE_JSON_MESSAGE_MAXFILESIZE = "basicConsoleJsonMessageMaxFileSize.xml";
    public static final String SERVER_XML_BASIC_CONSOLE_JSON_MESSAGE_ACCESSTRACE = "basicConsoleJsonMessageTraceAccess.xml";
    public static final String SERVER_XML_JSON_CONSOLE_JSON_MESSAGE_TRACE_ENABLED = "jsonConsoleJsonMessageTraceEnabled.xml";
    public static final String SERVER_XML_TRACE_CONSOLE_ACCESSTRACE_MESSAGE = "traceConsoleAccessTraceMessage.xml";
    public static final String SERVER_XML_CLEAR_LOGGING_SOURCES = "clearLoggingSources.xml";
    public static final String SERVER_XML_BASIC = "basicServer.xml";
    public static final String SERVER_XML_JSON_SOURCE_MESSAGETRACEACCESS = "jsonSourceMessageTraceAccess.xml";
    public static final String SERVER_XML_JSON_MESSAGE_ACCESS = "jsonMessageSourceAccessLog.xml";

    ArrayList<String> ALL_SOURCE_LIST = new ArrayList<String>(Arrays.asList("message", "trace", "accesslog", "ffdc"));

    String line = null;
    String consoleline = null;

    @BeforeClass
    public static void setUpClass() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "com.ibm.logs");
        server.startServer();
        /* start server */
        Assert.assertNotNull("Test app LogstashApp does not appear to have started.", server.waitForStringInLog("CWWKT0016I:.*LogstashApp"));
    }

    //<logging consoleLogLevel="ERROR" />
    @Test
    public void testError() throws Exception {
        //update file
        line = setConfig(SERVER_XML_CLLERROR);
        Assert.assertNotNull("Both CWWKG0017I and CWWKG0018I are not found", line);
        //run LogServlet
        line = checkMessageLogUpdate(false, ALL_SOURCE_LIST, "");
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        //console
        consoleline = checkConsoleLogUpdate(true, consoleLogFile, "ERROR", ALL_SOURCE_LIST, "");
    }

    //<logging consoleLogLevel="WARNING" />
    @Test
    public void testWarning() throws Exception {
        line = setConfig(SERVER_XML_CLLWARNING);
        Assert.assertNotNull("Both CWWKG0017I and CWWKG0018I are not found", line);
        line = checkMessageLogUpdate(false, ALL_SOURCE_LIST, "");
        //console
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        consoleline = checkConsoleLogUpdate(true, consoleLogFile, "WARNING", ALL_SOURCE_LIST, "");
    }

    //<logging consoleLogLevel="INFO" consoleFormat="basic"/>
    @Test
    public void testInfo() throws Exception {
        line = setConfig(SERVER_XML_CLLINFO_BASIC_CONSOLE);
        Assert.assertNotNull("Both CWWKG0017I and CWWKG0018I are not found", line);
        line = checkMessageLogUpdate(false, ALL_SOURCE_LIST, "");
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        consoleline = checkConsoleLogUpdate(false, consoleLogFile, "INFO", ALL_SOURCE_LIST, "");
    }

    //Test 6 consoleLL="INFO" consoleFormat=basic messageFormat=json
    @Test
    public void testBasicConsoleJsonMessage() throws Exception {
        line = setConfig(SERVER_XML_BASIC_CONSOLE_JSON_MESSAGE);
        Assert.assertNotNull("Both CWWKG0017I and CWWKG0018I are not found", line);
        line = checkMessageLogUpdate(true, ALL_SOURCE_LIST, "");
        //console
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        consoleline = checkConsoleLogUpdate(false, consoleLogFile, "INFO", ALL_SOURCE_LIST, "");
    }

    //<logging consoleLogLevel="INFO" consoleFormat="json" messageFormat="json"/>
    @Test
    public void testJsonConsoleJsonMessage() throws Exception {
        line = setConfig(SERVER_XML_JSON_CONSOLE_JSON_MESSAGE);
        Assert.assertNotNull("Both CWWKG0017I and CWWKG0018I are not found", line);
        line = checkMessageLogUpdate(true, ALL_SOURCE_LIST, "");
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        consoleline = checkConsoleLogUpdate(true, consoleLogFile, "INFO", ALL_SOURCE_LIST, "");
    }

    //<logging consoleLogLevel="INFO" consoleFormat="basic" messageFormat="basic" maxFileSize="100"/>
    @Test
    public void testBasicConsoleBasicMessage() throws Exception {
        line = setConfig(SERVER_XML_BASIC_CONSOLE_BASIC_MESSAGE);
        Assert.assertNotNull("Both CWWKG0017I and CWWKG0018I are not found", line);
        line = checkMessageLogUpdate(false, ALL_SOURCE_LIST, "");
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        consoleline = checkConsoleLogUpdate(false, consoleLogFile, "INFO", ALL_SOURCE_LIST, "");
    }

    //<logging consoleLogLevel="INFO" consoleFormat="basic" messageFormat="json" maxFileSize="100"/>
    @Test
    public void testBasicConsoleJsonMessageMaxFileSize() throws Exception {
        line = setConfig(SERVER_XML_BASIC_CONSOLE_JSON_MESSAGE_MAXFILESIZE);
        Assert.assertNotNull("Both CWWKG0017I and CWWKG0018I are not found", line);
        line = checkMessageLogUpdate(true, ALL_SOURCE_LIST, "");
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        consoleline = checkConsoleLogUpdate(false, consoleLogFile, "INFO", ALL_SOURCE_LIST, "");
    }

    //<logging consoleLogLevel="INFO" consoleFormat="basic" messageFormat="json" messageSource="trace,accesslog" maxFileSize="100"/>
    @Test
    public void testJsonMessageSrcTraceAccess() throws Exception {
        ArrayList<String> messagesourceList = new ArrayList<String>(Arrays.asList("trace", "accesslog"));
        line = setConfig(SERVER_XML_BASIC_CONSOLE_JSON_MESSAGE_ACCESSTRACE);
        line = checkMessageLogUpdate(true, messagesourceList, "");
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        consoleline = checkConsoleLogUpdate(false, consoleLogFile, "INFO", ALL_SOURCE_LIST, "");
    }

    //<logging consoleLogLevel="INFO" consoleFormat="json" messageFormat="json" messageSource="trace,accesslog" maxFileSize="100" traceSpecification="com.ibm.logs.LogstashServlet=finest"/>
    @Test
    public void testJsonTraceEnabled() throws Exception {
        ArrayList<String> messagesourceList = new ArrayList<String>(Arrays.asList("trace", "accesslog"));
        line = setConfig(SERVER_XML_JSON_CONSOLE_JSON_MESSAGE_TRACE_ENABLED);
        line = checkMessageLogUpdate(true, messagesourceList, "finest");
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        consoleline = checkConsoleLogUpdate(false, consoleLogFile, "INFO", ALL_SOURCE_LIST, "finest");
    }

    //<logging consoleLogLevel="INFO" consoleFormat="basic" consoleSource="trace" messageFormat="json" messageSource="trace,accesslog" maxFileSize="100"/>
    @Test
    public void testTraceConsoleAccessTraceMessage() throws Exception {
        ArrayList<String> messagesourceList = new ArrayList<String>(Arrays.asList("trace", "accesslog"));
        line = setConfig(SERVER_XML_TRACE_CONSOLE_ACCESSTRACE_MESSAGE);
        line = checkMessageLogUpdate(true, messagesourceList, "");
        ArrayList<String> consolesourceList = new ArrayList<String>(Arrays.asList("trace"));
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        line = checkConsoleLogUpdate(false, consoleLogFile, "INFO", consolesourceList, "");
    }

    //<logging consoleLogLevel="INFO" consoleFormat="json" consoleSource="trace" messageFormat="json" messageSource="trace,accesslog" maxFileSize="100"/>
    @Test
    public void testClearLoggingSources() throws Exception {
        ArrayList<String> messagesourceList = new ArrayList<String>(Arrays.asList("trace", "accesslog"));
        line = setConfig(SERVER_XML_CLEAR_LOGGING_SOURCES);
        line = checkMessageLogUpdate(true, messagesourceList, "");

        RemoteFile consoleLogFile = server.getConsoleLogFile();
        ArrayList<String> consolesourceList = new ArrayList<String>(Arrays.asList("trace"));
        consoleline = checkConsoleLogUpdate(true, consoleLogFile, "INFO", consolesourceList, "");
        //clear server.xml logging element
        line = setConfig(SERVER_XML_BASIC);
        line = checkMessageLogUpdate(false, ALL_SOURCE_LIST, "");
        consoleline = checkConsoleLogUpdate(true, consoleLogFile, "INFO", ALL_SOURCE_LIST, "");

    }

    //<logging consoleLogLevel="INFO" consoleFormat="json" messageFormat="json" />
    @Test
    public void testJsonConsoleJsonMessageClearLogging() throws Exception {
        line = setConfig(SERVER_XML_JSON_CONSOLE_JSON_MESSAGE);
        Assert.assertNotNull("Both CWWKG0017I and CWWKG0018I are not found", line);
        line = checkMessageLogUpdate(true, ALL_SOURCE_LIST, "");
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        consoleline = checkConsoleLogUpdate(true, consoleLogFile, "INFO", ALL_SOURCE_LIST, "");
        //clear server.xml logging element
        line = setConfig(SERVER_XML_BASIC);
        line = checkMessageLogUpdate(false, ALL_SOURCE_LIST, "");
        consoleline = checkConsoleLogUpdate(true, consoleLogFile, "INFO", ALL_SOURCE_LIST, "");
    }

    //<logging consoleLogLevel="INFO" consoleFormat="json" consoleSource="trace" messageFormat="json" messageSource="message,trace,accesslog" maxFileSize="100"/>
    @Test
    public void testJsonSrcMessageTraceAccess() throws Exception {
        line = setConfig(SERVER_XML_JSON_SOURCE_MESSAGETRACEACCESS);
        Assert.assertNotNull("Both CWWKG0017I and CWWKG0018I are not found", line);
        ArrayList<String> messagesourceList = new ArrayList<String>(Arrays.asList("message", "trace", "accesslog"));
        line = checkMessageLogUpdate(true, messagesourceList, "");
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        ArrayList<String> consolesourceList = new ArrayList<String>(Arrays.asList("trace"));
        consoleline = checkConsoleLogUpdate(true, consoleLogFile, "INFO", consolesourceList, "");
    }

    //<logging messageFormat="json" messageSource="accesslog"/>
    @Test
    public void testJsonMessageAccess() throws Exception {
        line = setConfig(SERVER_XML_JSON_MESSAGE_ACCESS);
        ArrayList<String> messagesourceList = new ArrayList<String>(Arrays.asList("accesslog"));
        line = checkMessageLogUpdate(true, messagesourceList, "");
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        consoleline = checkConsoleLogUpdate(true, consoleLogFile, "INFO", ALL_SOURCE_LIST, "");
    }

    private String checkMessageLogUpdate(boolean isJson, ArrayList<String> sourceList, String traceSpec) {
        String line = null;
        //runApp
        TestUtils.runApp(server, "logServlet");
        if (isJson) {
            if (sourceList.contains("trace") && traceSpec.equals("finest")) {
                line = checkLine("\\{.*\"loglevel\":\"ENTRY\".*\\}");
                line = checkLine("\\{.*\"loglevel\":\"CONFIG\".*\\}");
                line = checkLine("\\{.*\"loglevel\":\"FINE\".*\\}");
                line = checkLine("\\{.*\"loglevel\":\"FINER\".*\\}");
                line = checkLine("\\{.*\"loglevel\":\"FINEST\".*\\}");
                line = checkLine("\\{.*\"loglevel\":\"EXIT\".*\\}");
            }
            if (sourceList.contains("message")) {
                line = checkLine("\\{.*\"loglevel\":\"SEVERE\".*\\}");
                line = checkLine("\\{.*\"loglevel\":\"WARNING\".*\\}");
                line = checkLine("\\{.*\"loglevel\":\"SystemOut\".*\\}");
                line = checkLine("\\{.*\"loglevel\":\"SystemErr\".*\\}");
            }
            if (sourceList.contains("accesslog")) {
                line = checkLine("\\{.*\"type\":\"liberty_accesslog\".*\\}");
            }
        } else if (!isJson) {
            line = checkLine("E severe message");
            line = checkLine("W warning message");
            line = checkLine("I info message");
            line = checkLine("O System.out.println");
            line = checkLine("R System.err.println");
        }
        return line;
    }

    private String checkConsoleLogUpdate(boolean isJson, RemoteFile consoleLogFile, String consoleLogLevel, ArrayList<String> sourceList, String traceSpec) {
        String line = null;
        if (isJson) {
            if (sourceList.contains("trace") && traceSpec.equals("finest")) {
                line = checkLine("\\{.*\"loglevel\":\"ENTRY\".*\\}");
            }
            line = checkLine("\\{.*\"loglevel\":\"SEVERE\".*\\}", consoleLogFile);
            //check consolelogLevel
            if (consoleLogLevel.equals("WARNING") || consoleLogLevel.equals("INFO")) {
                line = checkLine("\\{.*\"loglevel\":\"WARNING\".*\\}", consoleLogFile);
            }
            line = checkLine("\\{.*\"loglevel\":\"SystemOut\".*\\}", consoleLogFile);
            line = checkLine("\\{.*\"loglevel\":\"SystemErr\".*\\}", consoleLogFile);
            if (sourceList.contains("trace") && traceSpec.equals("finest")) {
                line = checkLine("\\{.*\"loglevel\":\"CONFIG\".*\\}");
                line = checkLine("\\{.*\"loglevel\":\"FINE\".*\\}");
                line = checkLine("\\{.*\"loglevel\":\"FINER\".*\\}");
                line = checkLine("\\{.*\"loglevel\":\"FINEST\".*\\}");
                line = checkLine("\\{.*\"loglevel\":\"EXIT\".*\\}");
            }
            if (sourceList.contains("accesslog")) {
                line = checkLine("\\{.*\"type\":\"liberty_accesslog\".*\\}", consoleLogFile);
            }
        } else if (!isJson) {
            //loglevel
            line = checkLine("\\[ERROR   \\] severe message", consoleLogFile);
            //check console log level
            if (consoleLogLevel.equals("WARNING") || consoleLogLevel.equals("INFO")) {
                line = checkLine("\\[WARNING \\] warning message", consoleLogFile);
            }
            line = checkLine("\\[INFO    \\] info message", consoleLogFile);
            line = checkLine("System.out.println", consoleLogFile);
            line = checkLine("\\[err\\] System.err.println", consoleLogFile);
        }
        return line;
    }

    private String checkLine(String message, RemoteFile remoteFile) {
        String line = server.waitForStringInLog(message, remoteFile);
        assertNotNull(message + " is not found", line);
        return line;
    }

    private String checkLine(String message) {
        String line = server.waitForStringInLog(message);
        assertNotNull("Cannot find " + message, line);
        return line;
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
