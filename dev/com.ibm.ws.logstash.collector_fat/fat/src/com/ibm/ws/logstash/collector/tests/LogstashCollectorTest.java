/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
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
package com.ibm.ws.logstash.collector.tests;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.ClassRule;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

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
    public static final String MESSAGE_PREFIX = "Test Logstash Message";
    public static final String PATH_TO_AUTOFVT_TESTFILES = "lib/LibertyFATTestFiles/";
    public static final int DEFAULT_TIMEOUT = 40 * 1000; // 40 seconds

    protected abstract LibertyServer getServer();

    private static String APP_URL = null;
    private static String buffer;
    private static CopyOnWriteArrayList<String> logstashOutput = new CopyOnWriteArrayList<String>();

    protected void setConfig(String conf) throws Exception {
        Log.info(c, "setConfig entry", conf);
        getServer().setMarkToEndOfLog();
        getServer().setServerConfigurationFile(conf);
        assertNotNull("Cannot find CWWKG0016I from messages.log", getServer().waitForStringInLogUsingMark("CWWKG0016I", 10000));
        String line = getServer().waitForStringInLogUsingMark("CWWKG0017I|CWWKG0018I", 10000);
        assertNotNull("Cannot find CWWKG0017I or CWWKG0018I from messages.log", line);
        waitForStringInContainerOutput("CWWKG0017I|CWWKG0018I"); // waits for server configuration to finish updating (CWWKG0017I)
        waitForStringInContainerOutput("CWWKZ0003I"); // waits for application to finish updating (CWWKZ0003I)
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

    protected void createMessageEventWithException(String id) {
        String url = getAppUrl() + "/ExceptionURL";
        if (id != null) {
            try {
                url = url + "?id=" + URLEncoder.encode(id, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.error(c, "createMessageEventWithException", e);
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

    protected void createGCEvent() {
        String url = getAppUrl() + "?gc=true";
        runApp(url);
    }

    private static void runApp(String url) {
        String method = "runApp";
        Log.info(c, method, "---> Running the application with url : " + url);
        try {
            ValidateHelper.runGetMethod(url);
        } catch (Exception e) {
            Log.info(c, method, " ---> Exception : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getAppUrl() {
        if (APP_URL == null) {
            APP_URL = "http://" + getServer().getHostname() + ":" + getServer().getHttpDefaultPort() + "/LogstashApp";
        }
        return APP_URL;
    }

    //TODO switch to use public.ecr.aws/elastic/logstash:7.16.3
    //TODO remove withDockerfileFromBuilder and instead create a dockerfile
    private static final String IMAGE_NAME = ImageNameSubstitutor.instance() //
                    .apply(DockerImageName.parse("elastic/logstash:7.16.3")).asCanonicalNameString();

    // Can be added to the FATSuite to make the resource lifecycle bound to the entire
    // FAT bucket. Or, you can add this to any JUnit test class and the container will
    // be started just before the @BeforeClass and stopped after the @AfterClass
    @ClassRule
    public static GenericContainer<?> logstashContainer = new GenericContainer<>(new ImageFromDockerfile() //
                    .withDockerfileFromBuilder(builder -> builder.from(IMAGE_NAME) //
                                    .copy("/usr/share/logstash/pipeline/logstash.conf", "/usr/share/logstash/pipeline/logstash.conf") //
                                    .copy("/usr/share/logstash/config/logstash.yml", "/usr/share/logstash/config/logstash.yml") //
                                    .copy("/usr/share/logstash/config/logstash.key", "/usr/share/logstash/config/logstash.key") //
                                    .copy("/usr/share/logstash/config/logstash.crt", "/usr/share/logstash/config/logstash.crt") //
                                    .build()) //
                    .withFileFromFile("/usr/share/logstash/pipeline/logstash.conf", new File(PATH_TO_AUTOFVT_TESTFILES + "logstash.conf"), 644) //
                    .withFileFromFile("/usr/share/logstash/config/logstash.yml", new File(PATH_TO_AUTOFVT_TESTFILES + "logstash.yml"), 644) //
                    .withFileFromFile("/usr/share/logstash/config/logstash.key", new File(PATH_TO_AUTOFVT_TESTFILES + "logstash.key"), 644) //
                    .withFileFromFile("/usr/share/logstash/config/logstash.crt", new File(PATH_TO_AUTOFVT_TESTFILES + "logstash.crt"), 644)) //
                    .withExposedPorts(5043) //
                    .withStartupTimeout(Duration.ofSeconds(240)) //
                    .withLogConsumer(LogstashCollectorTest::log); //

    // This helper method is passed into `withLogConsumer()` of the container
    // It will consume all of the logs (System.out) of the container, which we will
    // use to pipe container output to our standard FAT output logs (output.txt)
    private static void log(OutputFrame frame) {
        String msg = frame.getUtf8String();
        if (msg.endsWith("\n")) {
            msg = msg.substring(0, msg.length() - 1);
        }

        boolean isComplete = false;
        if (msg.startsWith("{") && msg.endsWith("}")) {
            buffer = msg;
            isComplete = true;
        } else if (msg.startsWith("{")) {
            buffer = msg;
        } else if (msg.endsWith("}")) {
            buffer = buffer + msg;
            isComplete = true;
        } else {
            buffer = buffer + msg;
        }
        if (isComplete) {
            logstashOutput.add(buffer);
            Log.info(c, "logstashContainer", buffer);
            buffer = null;
        }
    }

    protected static void clearContainerOutput() {
        logstashOutput.clear();
        Log.info(c, "clearContainerOutput", "cleared logstashOutput");
    }

    protected static int getContainerOutputSize() {
        return logstashOutput.size();
    }

    protected static int waitForContainerOutputSize(int size) {
        int timeout = DEFAULT_TIMEOUT;
        while (timeout > 0) {
            if (getContainerOutputSize() >= size) {
                return size;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            timeout -= 1000;
        }
        return getContainerOutputSize();
    }

    protected static String waitForStringInContainerOutput(String regex) {
        Log.info(c, "waitForStringInOutput", "looking for " + regex);
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher;
        int timeout = DEFAULT_TIMEOUT;

        while (timeout > 0) {
            Iterator<String> it = logstashOutput.iterator();
            while (it.hasNext()) {
                String line = it.next();
                matcher = pattern.matcher(line);
                if (matcher.find()) {
                    return line;
                }
            }
            timeout -= 1000;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        Log.info(c, "waitForStringInOutput", "Timed out and could not find any lines containing : " + regex);
        return null; // timed out and not found
    }

    protected static String findStringInContainerOutput(String str) {
        Iterator<String> it = logstashOutput.iterator();
        while (it.hasNext()) {
            String line = it.next();
            if (line.contains(str)) {
                return line;
            }
        }
        return null; // not found
    }

    protected static List<JSONObject> parseJsonInContainerOutput() throws JSONException {
        ArrayList<JSONObject> list = new ArrayList<JSONObject>();
        Iterator<String> it = logstashOutput.iterator();

        String partialLine = "";
        while (it.hasNext()) {
            String line = it.next();
            if (!line.endsWith("}")) {
                // Handle a split output frame
                partialLine += line;
                continue;
            }
            if (!partialLine.isEmpty()) {
                line = partialLine + line;
                partialLine = "";
            }
            try {
                JSONObject json = new JSONObject(line);
                list.add(json);
            } catch (Exception e) {
                Log.error(c, "parseJsonInContainerOutput", e, "Unable to parse JSON: " + line);
                throw e;
            }
        }
        return list;
    }
}
