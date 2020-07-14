/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
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
    public static final String SERVER_XML_JSON_CONSOLE_BASIC_MESSAGE = "jsonConsoleLogServer.xml";
    public static final String SERVER_XML_JSON_CONSOLE_JSON_MESSAGE_TRACE_ENABLED = "jsonConsoleJsonMessageTraceEnabled.xml";
    public static final String SERVER_XML_JSON_MESSAGE_MAXFILESIZE = "jsonMessageMaxFileSize.xml";
    public static final String SERVER_XML_TRACE_CONSOLE_ACCESSTRACE_MESSAGE = "traceConsoleAccessTraceMessage.xml";
    public static final String SERVER_XML_CLEAR_LOGGING_SOURCES = "clearLoggingSources.xml";
    public static final String SERVER_XML_BASIC = "basicServer.xml";
    public static final String SERVER_XML_JSON_SOURCE_MESSAGETRACEACCESS = "jsonSourceMessageTraceAccess.xml";
    public static final String SERVER_XML_JSON_MESSAGE_ACCESS = "jsonMessageSourceAccessLog.xml";
    public static final String SERVER_XML_JSON_CONFIG_FIELD_EXT = "jsonConfigFieldExt.xml";
    public static final String SERVER_XML_APPS_WRITE_JSON_ENABLED = "appsWriteJsonEnabled.xml";
    public static final String SERVER_XML_APPS_WRITE_JSON_DISABLED = "appsWriteJsonDisabled.xml";

    ArrayList<String> ALL_SOURCE_LIST = new ArrayList<String>(Arrays.asList("message", "trace", "accesslog", "ffdc"));

    @BeforeClass
    public static void setUpClass() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "com.ibm.logs");
        server.startServer();

        /* start server */
        Assert.assertNotNull("Test app LogstashApp does not appear to have started.", server.waitForStringInLog("CWWKT0016I:.*LogstashApp"));
        // Preserve the original server configuration
        server.saveServerConfiguration();
    }

    //<logging consoleLogLevel="ERROR" />
    @Test
    public void testError() throws Exception {
        //update file
        setServerConfig(SERVER_XML_CLLERROR);
        //run LogServlet
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        runApplication(consoleLogFile);
        checkMessageLogUpdate(false, ALL_SOURCE_LIST, "");
        //console
        checkConsoleLogUpdate(true, consoleLogFile, "ERROR", ALL_SOURCE_LIST, "");
    }

    //<logging consoleLogLevel="WARNING" />
    @Test
    public void testWarning() throws Exception {
        setServerConfig(SERVER_XML_CLLWARNING);
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        runApplication(consoleLogFile);
        checkMessageLogUpdate(false, ALL_SOURCE_LIST, "");
        //console
        checkConsoleLogUpdate(true, consoleLogFile, "WARNING", ALL_SOURCE_LIST, "");
    }

    //<logging consoleLogLevel="INFO" consoleFormat="dev"/>
    @Test
    public void testInfo() throws Exception {
        setServerConfig(SERVER_XML_CLLINFO_BASIC_CONSOLE);
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        runApplication(consoleLogFile);
        checkMessageLogUpdate(false, ALL_SOURCE_LIST, "");
        checkConsoleLogUpdate(false, consoleLogFile, "INFO", ALL_SOURCE_LIST, "");
    }

    //Test 6 consoleLL="INFO" consoleFormat=dev messageFormat=json
    @Test
    public void testBasicConsoleJsonMessage() throws Exception {
        setServerConfig(SERVER_XML_BASIC_CONSOLE_JSON_MESSAGE);
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        runApplication(consoleLogFile);
        checkMessageLogUpdate(true, ALL_SOURCE_LIST, "");
        //console
        checkConsoleLogUpdate(false, consoleLogFile, "INFO", ALL_SOURCE_LIST, "");
    }

    //<logging consoleLogLevel="INFO" consoleFormat="json" messageFormat="json"/>
    @Test
    public void testJsonConsoleJsonMessage() throws Exception {
        setServerConfig(SERVER_XML_JSON_CONSOLE_JSON_MESSAGE);
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        runApplication(consoleLogFile);
        checkMessageLogUpdate(true, ALL_SOURCE_LIST, "");
        checkConsoleLogUpdate(true, consoleLogFile, "INFO", ALL_SOURCE_LIST, "");
    }

    //<logging consoleLogLevel="INFO" consoleFormat="dev" messageFormat="simple" maxFileSize="100"/>
    @Test
    public void testBasicConsoleBasicMessage() throws Exception {
        setServerConfig(SERVER_XML_BASIC_CONSOLE_BASIC_MESSAGE);
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        runApplication(consoleLogFile);
        checkMessageLogUpdate(false, ALL_SOURCE_LIST, "");
        checkConsoleLogUpdate(false, consoleLogFile, "INFO", ALL_SOURCE_LIST, "");
    }

    //<logging consoleLogLevel="INFO" consoleFormat="dev" messageFormat="json" maxFileSize="100"/>
    @Test
    public void testBasicConsoleJsonMessageMaxFileSize() throws Exception {
        setServerConfig(SERVER_XML_BASIC_CONSOLE_JSON_MESSAGE_MAXFILESIZE);
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        runApplication(consoleLogFile);
        checkMessageLogUpdate(true, ALL_SOURCE_LIST, "");
        checkConsoleLogUpdate(false, consoleLogFile, "INFO", ALL_SOURCE_LIST, "");
    }

    //<logging consoleLogLevel="INFO" consoleFormat="dev" messageFormat="json" messageSource="trace,accesslog" maxFileSize="100"/>
    @Test
    public void testJsonMessageSrcTraceAccess() throws Exception {
        ArrayList<String> messagesourceList = new ArrayList<String>(Arrays.asList("trace", "accesslog"));
        setServerConfig(SERVER_XML_BASIC_CONSOLE_JSON_MESSAGE_ACCESSTRACE);
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        runApplication(consoleLogFile);
        checkMessageLogUpdate(true, messagesourceList, "");
        checkConsoleLogUpdate(false, consoleLogFile, "INFO", ALL_SOURCE_LIST, "");
    }

    //<logging consoleLogLevel="INFO" consoleFormat="json" messageFormat="json" messageSource="trace,accesslog" maxFileSize="100" traceSpecification="com.ibm.logs.LogstashServlet=finest"/>
    @Test
    public void testJsonTraceEnabled() throws Exception {
        ArrayList<String> messagesourceList = new ArrayList<String>(Arrays.asList("trace", "accesslog"));
        setServerConfig(SERVER_XML_JSON_CONSOLE_JSON_MESSAGE_TRACE_ENABLED);
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        runApplication(consoleLogFile);
        checkMessageLogUpdate(true, messagesourceList, "finest");
        checkConsoleLogUpdate(true, consoleLogFile, "INFO", ALL_SOURCE_LIST, "finest");
    }

    //<logging consoleLogLevel="INFO" consoleFormat="dev" consoleSource="trace" messageFormat="json" messageSource="trace,accesslog" maxFileSize="100"/>
    @Test
    public void testTraceConsoleAccessTraceMessage() throws Exception {
        ArrayList<String> messagesourceList = new ArrayList<String>(Arrays.asList("trace", "accesslog"));
        setServerConfig(SERVER_XML_TRACE_CONSOLE_ACCESSTRACE_MESSAGE);
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        runApplication(consoleLogFile);
        checkMessageLogUpdate(true, messagesourceList, "");
        ArrayList<String> consolesourceList = new ArrayList<String>(Arrays.asList("trace"));
        checkConsoleLogUpdate(false, consoleLogFile, "INFO", consolesourceList, "");
    }

    //<logging consoleLogLevel="INFO" consoleFormat="json" consoleSource="trace" messageFormat="json" messageSource="trace,accesslog" maxFileSize="100"/>
    @Test
    public void testClearLoggingSources() throws Exception {
        ArrayList<String> messagesourceList = new ArrayList<String>(Arrays.asList("trace", "accesslog"));
        setServerConfig(SERVER_XML_CLEAR_LOGGING_SOURCES);
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        runApplication(consoleLogFile);
        checkMessageLogUpdate(true, messagesourceList, "");
        ArrayList<String> consolesourceList = new ArrayList<String>(Arrays.asList("trace"));
        checkConsoleLogUpdate(true, consoleLogFile, "INFO", consolesourceList, "");
        //clear server.xml logging element
        setServerConfig(SERVER_XML_BASIC);
        runApplication(consoleLogFile);
        checkMessageLogUpdate(false, ALL_SOURCE_LIST, "");
        checkConsoleLogUpdate(true, consoleLogFile, "INFO", ALL_SOURCE_LIST, "");

    }

    //<logging consoleLogLevel="INFO" consoleFormat="json" messageFormat="json" />
    @Test
    public void testJsonConsoleJsonMessageClearLogging() throws Exception {
        setServerConfig(SERVER_XML_JSON_CONSOLE_JSON_MESSAGE);
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        runApplication(consoleLogFile);
        checkMessageLogUpdate(true, ALL_SOURCE_LIST, "");
        checkConsoleLogUpdate(true, consoleLogFile, "INFO", ALL_SOURCE_LIST, "");
        //clear server.xml logging element
        setServerConfig(SERVER_XML_BASIC);
        runApplication(consoleLogFile);
        checkMessageLogUpdate(false, ALL_SOURCE_LIST, "");
        checkConsoleLogUpdate(true, consoleLogFile, "INFO", ALL_SOURCE_LIST, "");
    }

    //<logging consoleLogLevel="INFO" consoleFormat="json" consoleSource="trace" messageFormat="json" messageSource="message,trace,accesslog" maxFileSize="100"/>
    @Test
    public void testJsonSrcMessageTraceAccess() throws Exception {
        setServerConfig(SERVER_XML_JSON_SOURCE_MESSAGETRACEACCESS);
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        runApplication(consoleLogFile);
        ArrayList<String> messagesourceList = new ArrayList<String>(Arrays.asList("message", "trace", "accesslog"));
        checkMessageLogUpdate(true, messagesourceList, "");
        ArrayList<String> consolesourceList = new ArrayList<String>(Arrays.asList("trace"));
        checkConsoleLogUpdate(true, consoleLogFile, "INFO", consolesourceList, "");
    }

    //<logging messageFormat="json" messageSource="accesslog"/>
    @Test
    public void testJsonMessageAccess() throws Exception {
        setServerConfig(SERVER_XML_JSON_MESSAGE_ACCESS);
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        runApplication(consoleLogFile);
        ArrayList<String> messagesourceList = new ArrayList<String>(Arrays.asList("accesslog"));
        checkMessageLogUpdate(true, messagesourceList, "");
        checkConsoleLogUpdate(true, consoleLogFile, "INFO", ALL_SOURCE_LIST, "");
    }

    @Test
    public void testJsonFieldExtensions() throws Exception {
        //Set jsonFields property in server.xml for extensions
        setServerConfig(SERVER_XML_JSON_CONFIG_FIELD_EXT);
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        server.setMarkToEndOfLog();
        server.setMarkToEndOfLog(consoleLogFile);
        TestUtils.runApp(server, "extension");
        checkLine("\\{.*\"Correct_bool123\".*\\}");
        checkLine("\\{.*\"Correct_string123\".*\\}");

    }

    @Test
    public void testAppsWriteJson() throws Exception {
        //update file
        setServerConfig(SERVER_XML_APPS_WRITE_JSON_ENABLED);
        //run LogServlet
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        runApplication(consoleLogFile);
        //check output are in application's JSON format
        checkLine("\\{\"key\":\"value\"\\}");
        checkLine("\\{\"key\":\"value\",\"loglevel\":\"System.err\"\\}");
        checkLine("\\{\"key\":\"value\"\\}", consoleLogFile);
        checkLine("\\{\"key\":\"value\",\"loglevel\":\"System.err\"\\}", consoleLogFile);
        //update file
        setServerConfig(SERVER_XML_APPS_WRITE_JSON_DISABLED);
        //check output are in liberty JSON format
        runApplication(consoleLogFile);
        checkLine("\\{.*\"message\":\".*key.*value.*\".*\\}");
        checkLine("\\{.*\"message\":\".*key.*value.*,.*loglevel.*System.err.*\".*\\}");
        checkLine("\\{.*\"message\":\".*key.*value.*\".*\\}", consoleLogFile);
        checkLine("\\{.*\"message\":\".*key.*value.*,.*loglevel.*System.err.*\".*\\}", consoleLogFile);

    }

    private void checkMessageLogUpdate(boolean isJson, ArrayList<String> sourceList, String traceSpec) throws Exception {
        if (isJson) {
            if (sourceList.contains("trace") && traceSpec.equals("finest")) {
                checkLine("\\{.*\"loglevel\":\"ENTRY\".*\\}");
                checkLine("\\{.*\"loglevel\":\"CONFIG\".*\\}");
                checkLine("\\{.*\"loglevel\":\"FINE\".*\\}");
                checkLine("\\{.*\"loglevel\":\"FINER\".*\\}");
                checkLine("\\{.*\"loglevel\":\"FINEST\".*\\}");
                checkLine("\\{.*\"loglevel\":\"EXIT\".*\\}");
            }
            if (sourceList.contains("message")) {
                checkLine("\\{.*\"loglevel\":\"SEVERE\".*\\}");
                checkLine("\\{.*\"loglevel\":\"WARNING\".*\\}");
                checkLine("\\{.*\"loglevel\":\"SystemOut\".*\\}");
                checkLine("\\{.*\"loglevel\":\"SystemErr\".*\\}");
            }
            if (sourceList.contains("accesslog")) {
                checkLine("\\{.*\"type\":\"liberty_accesslog\".*\\}");
            }
        } else if (!isJson) {
            checkLine("E severe message");
            checkLine("W warning message");
            checkLine("I info message");
            checkLine("O System.out.println");
            checkLine("R System.err.println");
        }
    }

    private void checkConsoleLogUpdate(boolean isJson, RemoteFile consoleLogFile, String consoleLogLevel, ArrayList<String> sourceList,
                                       String traceSpec) throws Exception {
        //runApp
        if (isJson) {
            if (sourceList.contains("trace") && traceSpec.equals("finest")) {
                checkLine("\\{.*\"loglevel\":\"ENTRY\".*\\}");
            }
            if (sourceList.contains("message")) {
                checkLine("\\{.*\"loglevel\":\"SEVERE\".*\\}", consoleLogFile);
                //check consolelogLevel
                if (consoleLogLevel.equals("WARNING") || consoleLogLevel.equals("INFO")) {
                    checkLine("\\{.*\"loglevel\":\"WARNING\".*\\}", consoleLogFile);
                }
                checkLine("\\{.*\"loglevel\":\"SystemOut\".*\\}", consoleLogFile);
                checkLine("\\{.*\"loglevel\":\"SystemErr\".*\\}", consoleLogFile);
            }
            if (sourceList.contains("trace") && traceSpec.equals("finest")) {
                checkLine("\\{.*\"loglevel\":\"CONFIG\".*\\}");
                checkLine("\\{.*\"loglevel\":\"FINE\".*\\}");
                checkLine("\\{.*\"loglevel\":\"FINER\".*\\}");
                checkLine("\\{.*\"loglevel\":\"FINEST\".*\\}");
                checkLine("\\{.*\"loglevel\":\"EXIT\".*\\}");
            }
            if (sourceList.contains("accesslog")) {
                checkLine("\\{.*\"type\":\"liberty_accesslog\".*\\}", consoleLogFile);
            }
        } else if (!isJson) {
            //loglevel
            checkLine("\\[ERROR   \\] severe message", consoleLogFile);
            //check console log level
            if (consoleLogLevel.equals("WARNING") || consoleLogLevel.equals("INFO")) {
                checkLine("\\[WARNING \\] warning message", consoleLogFile);
            }
            checkLine("\\[INFO    \\] info message", consoleLogFile);
            checkLine("System.out.println", consoleLogFile);
            checkLine("\\[err\\] System.err.println", consoleLogFile);
        }
    }

    private void checkLine(String message, RemoteFile remoteFile) throws Exception {
        String line = server.waitForStringInLog(message, remoteFile);
        assertNotNull("Cannot find" + message + "from messages.log", line);
    }

    private void checkLine(String message) throws Exception {
        String line = server.waitForStringInLog(message, server.getDefaultLogFile());
        assertNotNull("Cannot find" + message + "from JsonConfigTest.log", line);
    }

    private void runApplication(RemoteFile consoleLogFile) throws Exception {
        server.setMarkToEndOfLog(consoleLogFile);
        TestUtils.runApp(server, "logServlet");
    }

    private static String setServerConfig(String fileName) throws Exception {
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