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

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.MinimumJavaLevel;
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
    @MaximumJavaLevel(javaLevel = 14)
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
    @ExpectedFFDC({ "java.lang.NullPointerException" })
    @MinimumJavaLevel(javaLevel = 15)
    public void checkFfdcJava15() throws Exception {
        final String method = "checkFfdc";

        //JDK 15 has message as an additional field in ffdc events
        ArrayList<String> ffdcKeysMandatoryList = new ArrayList<String>(Arrays.asList("ibm_datetime", "type", "host", "ibm_userDir", "ibm_serverName",
                                                                                      "ibm_sequence", "ibm_className", "ibm_exceptionName", "ibm_probeID",
                                                                                      "ibm_threadId", "ibm_stackTrace", "ibm_objectDetails", "message"));

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
    public void checkExtensions() throws Exception {
        final String method = "checkExt";
        getServer().addInstalledAppForValidation(APP_NAME);
        TestUtils.runApp(getServer(), "extension");

        String line = getServer().waitForStringInLog("\\{.*\"module\":\"com.ibm.logs.ExtensionServlet\".*\\}", getLogFile());

        assertNotNull("Cannot find \"module\":\"com.ibm.logs.ExtensionServlet\" from messages.log", line);

        JsonReader reader = Json.createReader(new StringReader(line));
        JsonObject jsonObj = reader.readObject();
        reader.close();

        if (!checkExtensions(jsonObj)) {
            Log.info(c, method, "Message line:" + line);
            Assert.fail("Test failed with one or more extension related errors");
        }
    }

    public boolean checkExtensions(JsonObject jsonObj) throws Exception {
        final String method = "checkExtensions";

        boolean isValid = true;

        ArrayList<String> extensionKeysMandatoryList = new ArrayList<String>(Arrays.asList("ext_correctBooleanExtension_bool", "ext_correctBooleanExtension2_bool",
                                                                                           "ext_correctIntExtension_int", "ext_correctIntExtension2_int",
                                                                                           "ext_correctStringExtension", "ext_correctFloatExtension_float",
                                                                                           "ext_correctFloatExtension2_float"));
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
            isValid = false;
            Log.info(c, method, "Mandatory keys missing:" + extensionKeysMandatoryList.toString());
        }
        if (invalidFields.size() > 0) {
            isValid = false;
            Log.info(c, method, "Invalid keys found:" + invalidFields.toString());
        }

        return isValid;
    }

    private boolean checkJsonMessage(JsonObject jsonObj, ArrayList<String> mandatoryKeyList, ArrayList<String> optionalKeyList) {
        final String method = "checkJsonMessage";

        String value = null;
        ArrayList<String> invalidFields = new ArrayList<String>();

        for (String key : jsonObj.keySet()) {
            if (mandatoryKeyList.contains(key)) {
                mandatoryKeyList.remove(key);
                value = "" + jsonObj.get(key);
                Log.finer(c, method, "key=" + key + ", value=" + value);
            } else if (optionalKeyList.contains(key)) {
                optionalKeyList.remove(key);
                value = "" + jsonObj.get(key);
                Log.finer(c, method, "key=" + key + ", value=" + value);
            } else if (key.startsWith(EXT_PREFIX)) {
                value = "" + jsonObj.get(key);
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
