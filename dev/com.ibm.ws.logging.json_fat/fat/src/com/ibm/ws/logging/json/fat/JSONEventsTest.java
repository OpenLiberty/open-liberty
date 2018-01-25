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
package com.ibm.ws.logging.json.fat;

import static org.junit.Assert.assertNotNull;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ejs.ras.TrLevelConstants;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
import componenttest.topology.impl.LibertyServer;

/**
 * Test liberty_messages, liberty_trace, liberty_accesslog and liberty_ffdc in JSON format
 */
public abstract class JSONEventsTest {

    protected static final Class<?> c = JSONEventsTest.class;

    public static final String APP_NAME = "LogstashApp";

    public abstract LibertyServer getServer();

    public abstract RemoteFile getLogFile() throws Exception;

    @Test
    public void checkMessage() throws Exception {
        final String method = "checkMessage";
        ArrayList<String> messageKeysMandatoryList = new ArrayList<String>(Arrays.asList("ibm_datetime", "type", "host", "ibm_userDir", "ibm_serverName",
                                                                                         "ibm_sequence", "loglevel", "ibm_messageId", "module",
                                                                                         "ibm_threadId", "message"));

        ArrayList<String> messageKeysOptionalList = new ArrayList<String>(Arrays.asList("ibm_className", "ibm_methodName"));

        String line = getServer().waitForStringInLog("\\{.*\"ibm_messageId\":\"CWWKF0011I\".*\\}", getLogFile());
        assertNotNull("Cannot find \"ibm_messageId\":\"CWWKF0011I\" from messages.log", line);
        // Read JSON message
        JsonReader reader = Json.createReader(new StringReader(line));
        JsonObject jsonObj = reader.readObject();
        reader.close();
        if (!checkJsonMessage(jsonObj, messageKeysMandatoryList, messageKeysOptionalList)) {
            Log.info(c, method, "Message line:" + line);
            Assert.fail("Test failed with one or more errors");
        }
    }

    @Test
    public void checkAccessLog() throws Exception {
        final String method = "checkAccessLog";

        ArrayList<String> accessLogKeysMandatoryList = new ArrayList<String>(Arrays.asList("ibm_datetime", "type", "host", "ibm_userDir", "ibm_serverName",
                                                                                           "ibm_sequence", "ibm_requestHost", "ibm_requestPort", "ibm_remoteHost",
                                                                                           "ibm_requestMethod", "ibm_uriPath", "ibm_requestProtocol", "ibm_elapsedTime",
                                                                                           "ibm_responseCode", "ibm_bytesReceived", "ibm_userAgent"));

        ArrayList<String> accessLogKeysOptionalList = new ArrayList<String>(Arrays.asList("ibm_queryString"));

        getServer().addInstalledAppForValidation(APP_NAME);
        TestUtils.runApp(getServer(), "access");

        String line = getServer().waitForStringInLog("\\{.*\"type\":\"liberty_accesslog\".*\\}", getLogFile());
        assertNotNull("Cannot find \"type\":\"liberty_accesslog\" from messages.log", line);

        JsonReader reader = Json.createReader(new StringReader(line));
        JsonObject jsonObj = reader.readObject();
        reader.close();

        if (!checkJsonMessage(jsonObj, accessLogKeysMandatoryList, accessLogKeysOptionalList)) {
            Log.info(c, method, "Message line:" + line);
            Assert.fail("Test failed with one or more errors");
        }

    }

    @Test
    @ExpectedFFDC({ "java.lang.NullPointerException" })
    public void checkFfdc() throws Exception {
        final String method = "checkFfdc";

        ArrayList<String> ffdcKeysMandatoryList = new ArrayList<String>(Arrays.asList("ibm_datetime", "type", "host", "ibm_userDir", "ibm_serverName",
                                                                                      "ibm_sequence", "ibm_className", "ibm_exceptionName", "ibm_probeID",
                                                                                      "ibm_threadId", "ibm_stackTrace", "ibm_objectDetails"));

        ArrayList<String> ffdcKeysOptionalList = new ArrayList<String>();

        getServer().addInstalledAppForValidation(APP_NAME);
        TestUtils.runApp(getServer(), "ffdc1");

        String line = getServer().waitForStringInLog("\\{.*\"type\":\"liberty_ffdc\".*\\}", getLogFile());
        assertNotNull("Cannot find \"type\":\"liberty_ffdc\" from messages.log", line);

        JsonReader reader = Json.createReader(new StringReader(line));
        JsonObject jsonObj = reader.readObject();
        reader.close();

        if (!checkJsonMessage(jsonObj, ffdcKeysMandatoryList, ffdcKeysOptionalList)) {
            Log.info(c, method, "Message line:" + line);
            Assert.fail("Test failed with one or more errors");
        }

    }

    @Test
    public void checkTrace() throws Exception {
        final String method = "checkTrace";

        ArrayList<String> traceKeysMandatoryList = new ArrayList<String>(Arrays.asList("ibm_datetime", "type", "host", "ibm_userDir", "ibm_serverName",
                                                                                       "ibm_sequence", "loglevel", "module", "ibm_methodName", "ibm_className",
                                                                                       "ibm_threadId", "message"));

        ArrayList<String> traceKeysOptionalList = new ArrayList<String>();

        getServer().addInstalledAppForValidation(APP_NAME);
        TestUtils.runApp(getServer(), "trace");

        String line = getServer().waitForStringInLog("\\{.*\"ibm_className\":\"com.ibm.logs.TraceServlet\".*\\}", getLogFile());
        assertNotNull("Cannot find \"ibm_className\":\"com.ibm.logs.TraceServlet\" from messages.log", line);

        JsonReader reader = Json.createReader(new StringReader(line));
        JsonObject jsonObj = reader.readObject();
        reader.close();

        if (!checkJsonMessage(jsonObj, traceKeysMandatoryList, traceKeysOptionalList)) {
            Log.info(c, method, "Message line:" + line);
            Assert.fail("Test failed with one or more errors");
        }

    }

    @Test
    public void checkTrInfo() throws Exception {
        final String method = "checkTrInfo";
        ArrayList<String> messageKeysMandatoryList = new ArrayList<String>(Arrays.asList("ibm_datetime", "type", "host", "ibm_userDir", "ibm_serverName",
                                                                                         "ibm_sequence", "loglevel", "ibm_messageId", "module",
                                                                                         "ibm_threadId", "message"));

        ArrayList<String> messageKeysOptionalList = new ArrayList<String>(Arrays.asList("ibm_className", "ibm_methodName"));

        // Tr.info
        TestUtils.runTrWriter(getServer(), TrLevelConstants.TRACE_LEVEL_INFO, "trwriter.info", "Welcome to Analytics team");
        String line = getServer().waitForStringInLog("\\{.*\"ibm_messageId\":\"JSONL0010I\".*\\}", getLogFile());
        assertNotNull("Cannot find \"ibm_messageId\":\"JSONL0010I\" from messages.log", line);
        // Read JSON message
        JsonReader reader = Json.createReader(new StringReader(line));
        JsonObject jsonObj = reader.readObject();
        reader.close();
        if (!checkJsonMessage(jsonObj, messageKeysMandatoryList, messageKeysOptionalList)) {
            Log.info(c, method, "Message line:" + line);
            Assert.fail(method + " failed with one or more errors");
        }

    }

    @Test
    public void checkTrError() throws Exception {
        final String method = "checkTrError";
        ArrayList<String> messageKeysMandatoryList = new ArrayList<String>(Arrays.asList("ibm_datetime", "type", "host", "ibm_userDir", "ibm_serverName",
                                                                                         "ibm_sequence", "loglevel", "ibm_messageId", "module",
                                                                                         "ibm_threadId", "message"));

        ArrayList<String> messageKeysOptionalList = new ArrayList<String>(Arrays.asList("ibm_className", "ibm_methodName"));

        // Tr.error
        TestUtils.runTrWriter(getServer(), TrLevelConstants.TRACE_LEVEL_ERROR, "trwriter.error", "Oh no, build break errors");
        String line = getServer().waitForStringInLog("\\{.*\"ibm_messageId\":\"JSONL0008I\".*\\}", getLogFile());
        assertNotNull("Cannot find \"ibm_messageId\":\"JSONL0008I\" from messages.log", line);
        // Read JSON message
        JsonReader reader = Json.createReader(new StringReader(line));
        JsonObject jsonObj = reader.readObject();
        reader.close();
        if (!checkJsonMessage(jsonObj, messageKeysMandatoryList, messageKeysOptionalList)) {
            Log.info(c, method, "Message line:" + line);
            Assert.fail(method + " failed with one or more errors");
        }
    }

    @Test
    public void checkTrWarning() throws Exception {
        final String method = "checkTr";
        ArrayList<String> messageKeysMandatoryList = new ArrayList<String>(Arrays.asList("ibm_datetime", "type", "host", "ibm_userDir", "ibm_serverName",
                                                                                         "ibm_sequence", "loglevel", "ibm_messageId", "module",
                                                                                         "ibm_threadId", "message"));

        ArrayList<String> messageKeysOptionalList = new ArrayList<String>(Arrays.asList("ibm_className", "ibm_methodName"));

        // Tr.warning
        TestUtils.runTrWriter(getServer(), TrLevelConstants.TRACE_LEVEL_WARNING, "trwriter.warning", "Final warning");
        String line = getServer().waitForStringInLog("\\{.*\"ibm_messageId\":\"JSONL0007I\".*\\}", getLogFile());
        assertNotNull("Cannot find \"ibm_messageId\":\"JSONL0007I\" from messages.log", line);
        // Read JSON message
        JsonReader reader = Json.createReader(new StringReader(line));
        JsonObject jsonObj = reader.readObject();
        reader.close();
        if (!checkJsonMessage(jsonObj, messageKeysMandatoryList, messageKeysOptionalList)) {
            Log.info(c, method, "Message line:" + line);
            Assert.fail("Tr.warning failed with one or more errors");
        }
    }

    @Test
    public void checkTrFatal() throws Exception {
        final String method = "checkTrFatal";
        ArrayList<String> messageKeysMandatoryList = new ArrayList<String>(Arrays.asList("ibm_datetime", "type", "host", "ibm_userDir", "ibm_serverName",
                                                                                         "ibm_sequence", "loglevel", "ibm_messageId", "module",
                                                                                         "ibm_threadId", "message"));

        ArrayList<String> messageKeysOptionalList = new ArrayList<String>(Arrays.asList("ibm_className", "ibm_methodName"));

        // Tr.fatal
        TestUtils.runTrWriter(getServer(), TrLevelConstants.TRACE_LEVEL_FATAL, "trwriter.fatal", "FATAL");
        String line = getServer().waitForStringInLog("\\{.*\"ibm_messageId\":\"JSONL0009I\".*\\}", getLogFile());
        assertNotNull("Cannot find \"ibm_messageId\":\"JSONL0009I\" from messages.log", line);
        // Read JSON message
        JsonReader reader = Json.createReader(new StringReader(line));
        JsonObject jsonObj = reader.readObject();
        reader.close();
        if (!checkJsonMessage(jsonObj, messageKeysMandatoryList, messageKeysOptionalList)) {
            Log.info(c, method, "Message line:" + line);
            Assert.fail(method + " failed with one or more errors");
        }
    }

    @Test
    public void checkTrDump() throws Exception {
        final String method = "checkTrDump";
        ArrayList<String> messageKeysMandatoryList = new ArrayList<String>(Arrays.asList("ibm_datetime", "type", "host", "ibm_userDir", "ibm_serverName",
                                                                                         "ibm_sequence", "loglevel", "module",
                                                                                         "ibm_threadId", "message"));

        ArrayList<String> messageKeysOptionalList = new ArrayList<String>(Arrays.asList("ibm_className", "ibm_methodName"));

        // Tr.error
        long ts = System.nanoTime();
        TestUtils.runTrWriter(getServer(), TrLevelConstants.TRACE_LEVEL_DUMP, "trwriter.dump", Long.toString(ts));
        String line = getServer().waitForStringInLog("\\{.*\"message\":\"Dump: trwriter.dump.*\\}", getLogFile());
        assertNotNull("Cannot find \"message\":\"Dump: trwriter.dump...\" from messages.log", line);
        // Read JSON message
        JsonReader reader = Json.createReader(new StringReader(line));
        JsonObject jsonObj = reader.readObject();
        reader.close();
        if (!checkJsonMessage(jsonObj, messageKeysMandatoryList, messageKeysOptionalList)) {
            Log.info(c, method, "Message line:" + line);
            Assert.fail(method + " failed with one or more errors");
        }
    }

    @Test
    public void checkTrDebug() throws Exception {
        final String method = "checkTrDebug";
        ArrayList<String> messageKeysMandatoryList = new ArrayList<String>(Arrays.asList("ibm_datetime", "type", "host", "ibm_userDir", "ibm_serverName",
                                                                                         "ibm_sequence", "loglevel", "module",
                                                                                         "ibm_threadId", "message"));

        ArrayList<String> messageKeysOptionalList = new ArrayList<String>(Arrays.asList("ibm_className", "ibm_methodName"));

        // Tr.error
        long ts = System.nanoTime();
        TestUtils.runTrWriter(getServer(), TrLevelConstants.TRACE_LEVEL_DEBUG, "trwriter.debug", Long.toString(ts));
        String line = getServer().waitForStringInLog("\\{.*\"message\":\"trwriter.debug.*\\}", getLogFile());
        assertNotNull("Cannot find \"message\":\"trwriter.debug...\" from messages.log", line);
        // Read JSON message
        JsonReader reader = Json.createReader(new StringReader(line));
        JsonObject jsonObj = reader.readObject();
        reader.close();
        if (!checkJsonMessage(jsonObj, messageKeysMandatoryList, messageKeysOptionalList)) {
            Log.info(c, method, "Message line:" + line);
            Assert.fail(method + " failed with one or more errors");
        }
    }

    @Test
    public void checkTrEntryExit() throws Exception {
        final String method = "checkTrEntryExit";
        ArrayList<String> messageKeysMandatoryList = new ArrayList<String>(Arrays.asList("ibm_datetime", "type", "host", "ibm_userDir", "ibm_serverName",
                                                                                         "ibm_sequence", "loglevel", "module",
                                                                                         "ibm_threadId", "message"));

        ArrayList<String> messageKeysOptionalList = new ArrayList<String>(Arrays.asList("ibm_className", "ibm_methodName"));

        // Tr.entry/exit
        long ts = System.nanoTime();
        TestUtils.runTrWriter(getServer(), TrLevelConstants.TRACE_LEVEL_ENTRY_EXIT, "trwriter.entryexit", Long.toString(ts));
        String line = getServer().waitForStringInLog("\\{.*\"message\":\"Entry.*\\}", getLogFile());
        assertNotNull("Cannot find \"message\":\"Entry...\" from messages.log", line);
        line = getServer().waitForStringInLog("\\{.*\"message\":\"Exit.*\\}", getLogFile());
        assertNotNull("Cannot find \"message\":\"Exit...\" from messages.log", line);
        // Read JSON message
        JsonReader reader = Json.createReader(new StringReader(line));
        JsonObject jsonObj = reader.readObject();
        reader.close();
        if (!checkJsonMessage(jsonObj, messageKeysMandatoryList, messageKeysOptionalList)) {
            Log.info(c, method, "Message line:" + line);
            Assert.fail(method + " failed with one or more errors");
        }
    }

    @Test
    public void checkTrEvent() throws Exception {
        final String method = "checkTrEvent";
        ArrayList<String> messageKeysMandatoryList = new ArrayList<String>(Arrays.asList("ibm_datetime", "type", "host", "ibm_userDir", "ibm_serverName",
                                                                                         "ibm_sequence", "loglevel", "module",
                                                                                         "ibm_threadId", "message"));

        ArrayList<String> messageKeysOptionalList = new ArrayList<String>(Arrays.asList("ibm_className", "ibm_methodName"));

        // Tr.event
        long ts = System.nanoTime();
        TestUtils.runTrWriter(getServer(), TrLevelConstants.TRACE_LEVEL_EVENT, "trwriter.event", Long.toString(ts));
        String line = getServer().waitForStringInLog("\\{.*\"message\":\"trwriter.event.*\\}", getLogFile());
        assertNotNull("Cannot find \"message\":\"trwriter.event...\" from messages.log", line);
        // Read JSON message
        JsonReader reader = Json.createReader(new StringReader(line));
        JsonObject jsonObj = reader.readObject();
        reader.close();
        if (!checkJsonMessage(jsonObj, messageKeysMandatoryList, messageKeysOptionalList)) {
            Log.info(c, method, "Message line:" + line);
            Assert.fail(method + " failed with one or more errors");
        }
    }

    @Test
    public void checkTrConfig() throws Exception {
        final String method = "checkTrConfig";
        ArrayList<String> messageKeysMandatoryList = new ArrayList<String>(Arrays.asList("ibm_datetime", "type", "host", "ibm_userDir", "ibm_serverName",
                                                                                         "ibm_sequence", "loglevel", "ibm_messageId", "module",
                                                                                         "ibm_threadId", "message"));

        ArrayList<String> messageKeysOptionalList = new ArrayList<String>(Arrays.asList("ibm_className", "ibm_methodName"));

        // Tr.error
        TestUtils.runTrWriter(getServer(), TrLevelConstants.TRACE_LEVEL_CONFIG, "trwriter.config", "Config");
        String line = getServer().waitForStringInLog("\\{.*\"ibm_messageId\":\"JSONL0005I\".*\\}", getLogFile());
        assertNotNull("Cannot find \"ibm_messageId\":\"JSONL0005I\" from messages.log", line);
        // Read JSON message
        JsonReader reader = Json.createReader(new StringReader(line));
        JsonObject jsonObj = reader.readObject();
        reader.close();
        if (!checkJsonMessage(jsonObj, messageKeysMandatoryList, messageKeysOptionalList)) {
            Log.info(c, method, "Message line:" + line);
            Assert.fail(method + " failed with one or more errors");
        }
    }

    private boolean checkJsonMessage(JsonObject jsonObj, ArrayList<String> mandatoryKeyList, ArrayList<String> optionalKeyList) {
        final String method = "checkJsonMessage";

        String value = null;
        ArrayList<String> invalidFields = new ArrayList<String>();

        for (String key : jsonObj.keySet()) {
            if (mandatoryKeyList.contains(key)) {
                mandatoryKeyList.remove(key);
                value = jsonObj.getString(key);
                Log.finer(c, method, "key=" + key + ", value=" + value);
            } else if (optionalKeyList.contains(key)) {
                optionalKeyList.remove(key);
                value = jsonObj.getString(key);
                Log.finer(c, method, "key=" + key + ", value=" + value);
            } else {
                invalidFields.add(key);
            }
        }
        boolean isSucceeded = true;
        if (mandatoryKeyList.size() > 0) {
            isSucceeded = false;
            Log.info(c, method, "Mandatory keys missing:" + mandatoryKeyList.toString());
        }
        if (invalidFields.size() > 0) {
            isSucceeded = false;
            Log.info(c, method, "Invalid keys found:" + invalidFields.toString());
        }
        return isSucceeded;
    }
}
