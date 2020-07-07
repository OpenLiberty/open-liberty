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
package com.ibm.ws.logstash.collector.tests;

import static org.junit.Assert.assertNotNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public abstract class LogstashCollectorTest {

    private static Class<?> c = LogstashCollectorTest.class;

    // Constants
    public static final String LIBERTY_MESSAGE = "liberty_message";
    public static final String LIBERTY_TRACE = "liberty_trace";
    public static final String LIBERTY_FFDC = "liberty_ffdc";
    public static final String LIBERTY_GC = "liberty_gc";
    public static final String LIBERTY_ACCESSLOG = "liberty_accesslog";
    public static final String LIBERTY_AUDIT = "liberty_audit";
    public static final String NPE = "NullPointerException";
    public static final String AIOB = "ArrayIndexOutOfBoundsException";

    public static final String KEY_TYPE = "type";
    public static final String KEY_TAGS = "tags";
    public static final String KEY_REASON = "reason";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_OBJECTDETAILS = "objectDetails";
    public static final String KEY_URLPATH = "uriPath";
    public static final String KEY_STACKTRACE = "stackTrace";
    public static final String ENTRY = "Entry";
    public static final String EXIT = "Exit";

    protected abstract LibertyServer getServer();

    private static String APP_URL = null;

    protected void setConfig(String conf) throws Exception {
        Log.info(c, "setConfig entry", conf);
        getServer().setMarkToEndOfLog();
        getServer().setServerConfigurationFile(conf);
        assertNotNull("Cannot find CWWKG0016I from messages.log", getServer().waitForStringInLogUsingMark("CWWKG0016I", 10000));
        String line = getServer().waitForStringInLogUsingMark("CWWKG0017I|CWWKG0018I", 10000);
        assertNotNull("Cannot find CWWKG0017I or CWWKG0018I from messages.log", line);
        Log.info(c, "setConfig exit", conf);
    }

    protected void createMessageEvent() {
        createMessageEvent(null);
    }

    protected void createMessageEvent(String id) {
        String url = getAppUrl();
        if (id != null) {
            try {
                url = url + "?id=" + URLEncoder.encode(id, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.error(c, "createMessageEvent", e);
                e.printStackTrace();
            }
        }
        runApp(url);
    }

    protected void createAccessLogEvent() {
        createAccessLogEvent(null);
    }

    protected void createAccessLogEvent(String id) {
        String url = getAppUrl() + "/AccessURL";
        if (id != null) {
            try {
                url = url + "?id=" + URLEncoder.encode(id, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.error(c, "createAccessLogEvent", e);
                e.printStackTrace();
            }
        }
        runApp(url);

    }

    protected void createFFDCEvent(int i) {
        String url = getAppUrl();
        switch (i) {
            case 2:
                url = url + "?secondFFDC=true";
                break;
            case 3:
                url = url + "?thirdFFDC=true";
                break;
            default:
                url = url + "?isFFDC=true";
        }
        runApp(url);
    }

    protected void createTraceEvent() {
        createTraceEvent(null);
    }

    protected void createTraceEvent(String id) {
        String url = getAppUrl() + "/TraceURL";
        if (id != null) {
            try {
                url = url + "?id=" + URLEncoder.encode(id, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.error(c, "createTraceEvent", e);
                e.printStackTrace();
            }
        }
        runApp(url);
    }

    private static void runApp(String url) {
        String method = "runApp";
        Log.info(c, method, "---> Running the application with url : " + url);
        try {
            ValidateHelper.runGetMethod(url);
        } catch (Exception e) {
            Log.info(c, method, " ---> Exception : " + e.getMessage());
        }
    }

    private String getAppUrl() {
        if (APP_URL == null) {
            APP_URL = "http://" + getServer().getHostname() + ":" + getServer().getHttpDefaultPort() + "/LogstashApp";
        }
        return APP_URL;
    }

}
