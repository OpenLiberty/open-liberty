/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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
package com.ibm.ws.logging.json.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.Assert;
import org.junit.Test;

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
    private static final String EXT_PREFIX = "ext_";

    public abstract LibertyServer getServer();

    public abstract RemoteFile getLogFile() throws Exception;

    @Test
    public void checkMessage() throws Exception {
        ArrayList<String> messageKeysMandatoryList = new ArrayList<String>(Arrays.asList("ibm_datetime", "type", "host", "ibm_userDir", "ibm_serverName",
                                                                                         "ibm_sequence", "loglevel", "ibm_messageId", "module",
                                                                                         "ibm_threadId", "message", "ext_thread"));

        ArrayList<String> messageKeysOptionalList = new ArrayList<String>(Arrays.asList("ibm_className", "ibm_methodName"));

        // Reset the log marks, so the log mark is at the start of the log file, since this test checks for the server
        // started message (CWWKF0011I), it will find the string in the log file, if the tests are not run in order.
        getServer().resetLogMarks();
        String line = getServer().waitForStringInLog("\\{.*\"ibm_messageId\":\"CWWKF0011I\".*\\}", getLogFile());
        assertNotNull("Cannot find \"ibm_messageId\":\"CWWKF0011I\" from messages.log", line);
        checkJsonMessage(line, messageKeysMandatoryList, messageKeysOptionalList);
    }

    @Test
    public void checkAccessLog() throws Exception {
        ArrayList<String> accessLogKeysMandatoryList = new ArrayList<String>(Arrays.asList("ibm_datetime", "type", "host", "ibm_userDir", "ibm_serverName",
                                                                                           "ibm_sequence", "ibm_requestHost", "ibm_requestPort", "ibm_remoteHost",
                                                                                           "ibm_requestMethod", "ibm_uriPath", "ibm_requestProtocol", "ibm_elapsedTime",
                                                                                           "ibm_responseCode", "ibm_bytesReceived", "ibm_userAgent"));

        ArrayList<String> accessLogKeysOptionalList = new ArrayList<String>(Arrays.asList("ibm_queryString"));

        TestUtils.runApp(getServer(), "access");

        String line = getServer().waitForStringInLog("\\{.*\"type\":\"liberty_accesslog\".*\\}", getLogFile());
        assertNotNull("Cannot find \"type\":\"liberty_accesslog\" from messages.log", line);

        checkJsonMessage(line, accessLogKeysMandatoryList, accessLogKeysOptionalList);
    }

    @Test
    @ExpectedFFDC({ "java.lang.NullPointerException" })
    public void checkFfdc() throws Exception {
        ArrayList<String> ffdcKeysMandatoryList = new ArrayList<String>(Arrays.asList("ibm_datetime", "type", "host", "ibm_userDir", "ibm_serverName",
                                                                                      "ibm_sequence", "ibm_className", "ibm_exceptionName", "ibm_probeID",
                                                                                      "ibm_threadId", "ibm_stackTrace", "ibm_objectDetails"));

        // The 'message' key is sometimes included for JDK 15
        ArrayList<String> ffdcKeysOptionalList = new ArrayList<String>(Arrays.asList("message"));

        TestUtils.runApp(getServer(), "ffdc1");

        String line = getServer().waitForStringInLog("\\{.*\"type\":\"liberty_ffdc\".*\\}", getLogFile());
        assertNotNull("Cannot find \"type\":\"liberty_ffdc\" from messages.log", line);

        checkJsonMessage(line, ffdcKeysMandatoryList, ffdcKeysOptionalList);
    }

    @Test
    public void checkTrace() throws Exception {
        ArrayList<String> traceKeysMandatoryList = new ArrayList<String>(Arrays.asList("ibm_datetime", "type", "host", "ibm_userDir", "ibm_serverName",
                                                                                       "ibm_sequence", "loglevel", "module", "ibm_methodName", "ibm_className",
                                                                                       "ibm_threadId", "message", "ext_appName", "ext_thread"));

        ArrayList<String> traceKeysOptionalList = new ArrayList<String>();

        TestUtils.runApp(getServer(), "trace");

        String line = getServer().waitForStringInLog("\\{.*\"ibm_className\":\"com.ibm.logs.TraceServlet\".*\\}", getLogFile());
        assertNotNull("Cannot find \"ibm_className\":\"com.ibm.logs.TraceServlet\" from messages.log", line);

        checkJsonMessage(line, traceKeysMandatoryList, traceKeysOptionalList);
    }

    @Test
    public void checkExtensions() throws Exception {
        TestUtils.runApp(getServer(), "extension");

        String line = getServer().waitForStringInLog("\\{.*\"module\":\"com.ibm.logs.ExtensionServlet\".*\\}", getLogFile());

        assertNotNull("Cannot find \"module\":\"com.ibm.logs.ExtensionServlet\" from messages.log", line);

        checkExtensions(line);
    }

    @Test
    public void checkException() throws Exception {
        TestUtils.runApp(getServer(), "exception");

        ArrayList<String> messageKeysMandatoryList = new ArrayList<String>(Arrays.asList("ibm_datetime", "type", "host", "ibm_userDir", "ibm_serverName",
                                                                                         "ibm_sequence", "loglevel", "module",
                                                                                         "ibm_threadId", "message", "ibm_stackTrace", "ibm_exceptionName", "ext_appName",
                                                                                         "ext_thread"));

        ArrayList<String> messageKeysMandatoryList2 = new ArrayList<String>(messageKeysMandatoryList);
        ArrayList<String> messageKeysMandatoryList3 = new ArrayList<String>(messageKeysMandatoryList);

        ArrayList<String> messageKeysOptionalList = new ArrayList<String>(Arrays.asList("ibm_className", "ibm_methodName"));

        /**
         * The exception servlet emits three exceptions.
         * Given that the current setup of this FAT is using regex to parse the messages
         * and that the JSON fields can be ordered in a varied order, we'll use each message to test an individual field.
         * First assert will just check that the message is there.
         * Second will check that the ibm_exceptionName is present.
         * Third will check that the ibm_stackTrace field is present.
         *
         */

        String line = getServer().waitForStringInLog("\\{.*\"message\":\"exception message\".*\\}", getLogFile());

        //Just checking for first message
        assertNotNull("Cannot find \"message\":\"exception message\" from " + getLogFile().getName(), line);

        checkJsonMessage(line, messageKeysMandatoryList, messageKeysOptionalList);

        //Checking for second  message with ibm_exceptionName field
        line = getServer().waitForStringInLog("\\{.*\"message\":\"second exception message\".*\\}", getLogFile());

        assertNotNull("Cannot find \"message\":\"second exception message\" from " + getLogFile().getName(), line);
        assertTrue(line.contains("\"ibm_exceptionName\":\"java.lang.IllegalArgumentException\""));

        checkJsonMessage(line, messageKeysMandatoryList2, messageKeysOptionalList);

        //Checking for third message with ibm_stackTrace field
        line = getServer().waitForStringInLog("\\{.*\"message\":\"third exception message\".*\\}", getLogFile());

        assertNotNull("Cannot find \"message\":\"third exception message\" from " + getLogFile().getName(), line);
        assertTrue(line.contains("\"ibm_stackTrace\":\"java.lang.IllegalArgumentException: bad"));

        checkJsonMessage(line, messageKeysMandatoryList3, messageKeysOptionalList);
    }

    @Test
    @ExpectedFFDC("java.io.IOException")
    public void checkExceptionExtensions() throws Exception {
        //Set the mark for end of log, so the test finds the correct and latest messageID lines, as there might be multiple occurrences of the same message ID from previous test cases.
        getServer().setMarkToEndOfLog(getLogFile());

        // Test checks if exceptions are thrown, the appropriate LogRecordContext Extensions are logged as well.
        TestUtils.runApp(getServer(), "ExceptionExtURL");

        ArrayList<String> messageKeysMandatoryList = new ArrayList<String>(Arrays.asList("ibm_datetime", "type", "host", "ibm_userDir", "ibm_serverName",
                                                                                         "ibm_sequence", "loglevel", "module",
                                                                                         "ibm_threadId", "message", "ibm_messageId", "ext_appName", "ext_thread",
                                                                                         "ext_testExtensionException"));

        ArrayList<String> messageKeysMandatoryList2 = new ArrayList<String>(messageKeysMandatoryList);

        ArrayList<String> messageKeysOptionalList = new ArrayList<String>(Arrays.asList("ibm_className", "ibm_methodName"));

        String line = getServer().waitForStringInLogUsingMark("\\{.*\"ibm_messageId\":\"SRVE0777E\".*\\}", getLogFile());

        // Check if exception thrown message is logged
        assertNotNull("Cannot find exception message \"ibm_messageId\":\"SRVE0777E\" from " + getLogFile().getName(), line);

        // Check if the exception thrown message event contains all the required JSON fields, including the LogRecordExtensions
        checkJsonMessage(line, messageKeysMandatoryList, messageKeysOptionalList);

        // Check if FFDC incident created message is logged
        assertNotNull("Cannot find FFDC incident created message \"ibm_messageId\":\"FFDC1015I\" from " + getLogFile().getName(), line);

        // Check if the FFDC incident created message event contains all the required JSON fields, including the LogRecordExtensions
        checkJsonMessage(line, messageKeysMandatoryList2, messageKeysOptionalList);
    }

    public void checkExtensions(String line) throws Exception {
        final String method = "checkExtensions";

        JsonReader reader = Json.createReader(new StringReader(line));
        JsonObject jsonObj = reader.readObject();
        reader.close();

        ArrayList<String> extensionKeysMandatoryList = new ArrayList<String>(Arrays.asList("ext_correctBooleanExtension_bool", "ext_correctBooleanExtension2_bool",
                                                                                           "ext_correctIntExtension_int", "ext_correctIntExtension2_int",
                                                                                           "ext_correctStringExtension", "ext_correctFloatExtension_float",
                                                                                           "ext_correctFloatExtension2_float", "ext_thread", "ext_appName"));
        ArrayList<String> invalidFields = new ArrayList<String>();

        String value = "";
        for (String key : jsonObj.keySet()) {
            if (extensionKeysMandatoryList.contains(key)) {
                if (key.equals("ext_correctIntExtension_int")) {
                    if (jsonObj.getInt(key) != 12345) {
                        invalidFields.add(key);
                    }
                } else if (key.equals("ext_correctIntExtension2_int")) {
                    if (jsonObj.getInt(key) != -12345) {
                        invalidFields.add(key);
                    }
                } else if (key.equals("ext_correctBooleanExtension_bool")) {
                    if (jsonObj.getBoolean(key) != true) {
                        invalidFields.add(key);
                    }
                } else if (key.equals("ext_correctBooleanExtension2_bool")) {
                    if (jsonObj.getBoolean(key) != false) {
                        invalidFields.add(key);
                    }
                } else if (key.equals("ext_correctStringExtension")) {
                    if (!jsonObj.getString(key).toString().equals("Testing string 1234")) {
                        invalidFields.add(key);
                    }
                } else if (key.equals("ext_correctFloatExtension_float")) {
                    if ((Float.parseFloat(jsonObj.get(key).toString())) != 100.123f) {
                        invalidFields.add(key);
                    }
                } else if (key.equals("ext_correctFloatExtension2_float")) {
                    if ((Float.parseFloat(jsonObj.get(key).toString())) != -100.123f) {
                        invalidFields.add(key);
                    }
                } else if (key.equals("ext_appName")) {
                    if (!jsonObj.getString(key).toString().equals("LogstashApp")) {
                        invalidFields.add(key);
                    }
                }
                extensionKeysMandatoryList.remove(key);
                value = "" + jsonObj.get(key);
                Log.finer(c, method, "key=" + key + ", value=" + value);
            }

            if (key.startsWith("wrongExtension")) {
                invalidFields.add(key);
            }
        }

        if (extensionKeysMandatoryList.size() > 0) {
            Log.info(c, method, "Mandatory keys missing: " + extensionKeysMandatoryList.toString());
            Assert.fail("Mandatory keys missing: " + extensionKeysMandatoryList.toString() + ". Actual JSON was: " + line);
        }
        if (invalidFields.size() > 0) {
            Log.info(c, method, "Invalid keys found: " + invalidFields.toString());
            Assert.fail("Invalid keys found: " + invalidFields.toString() + ". Actual JSON was: " + line);
        }
    }

    private void checkJsonMessage(String line, ArrayList<String> mandatoryKeyList, ArrayList<String> optionalKeyList) {
        final String method = "checkJsonMessage";

        JsonReader reader = Json.createReader(new StringReader(line));
        JsonObject jsonObj = reader.readObject();
        reader.close();
        String value = null;
        ArrayList<String> invalidFields = new ArrayList<String>();

        for (String key : jsonObj.keySet()) {
            if (mandatoryKeyList.contains(key)) {
                mandatoryKeyList.remove(key);
                value = "" + jsonObj.get(key);
                Log.finer(c, method, "key=" + key + ", value=" + value);
            } else if (optionalKeyList.contains(key)) {
                value = "" + jsonObj.get(key);
                Log.finer(c, method, "key=" + key + ", value=" + value);
            } else if (key.startsWith(EXT_PREFIX)) {
                value = "" + jsonObj.get(key);
                Log.finer(c, method, "key=" + key + ", value=" + value);
            } else {
                invalidFields.add(key);
            }
        }
        if (mandatoryKeyList.size() > 0) {
            Log.info(c, method, "Mandatory keys missing: " + mandatoryKeyList.toString());
            Assert.fail("Mandatory keys missing: " + mandatoryKeyList.toString() + ". Actual JSON was: " + line);
        }
        if (invalidFields.size() > 0) {
            Log.info(c, method, "Invalid keys found: " + invalidFields.toString());
            Assert.fail("Invalid keys found: " + invalidFields.toString() + ". Actual JSON was: " + line);
        }
    }
}
