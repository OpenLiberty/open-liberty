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

        ArrayList<String> accessLogKeysOptionalList = new ArrayList<String>();

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
