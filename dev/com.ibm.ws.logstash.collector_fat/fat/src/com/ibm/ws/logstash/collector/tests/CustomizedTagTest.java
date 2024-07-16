/*******************************************************************************
 * Copyright (c) 2011, 2024 IBM Corporation and others.
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class CustomizedTagTest extends LogstashCollectorTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("TagTestServer");

    static boolean msgType = false;
    static boolean ffdcType = false;
    static boolean gcType = false;
    static boolean traceType = false;
    static boolean accessType = false;
    static int mark = 0;
    static boolean newMsgsFound = false;

    static String record = "";

    private final String testName = "";
    private static Class<?> c = CustomizedTagTest.class;
    private static String os = "";

    protected static boolean runTest = true;

    @BeforeClass
    public static void setUp() throws Exception {

        server.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        String os = System.getProperty("os.name").toLowerCase();
        Log.info(c, "setUp", "os.name = " + os);
        Log.info(c, "setUp", "runTest = " + runTest);

        if (!runTest) {
            return;
        }

        clearContainerOutput();
        String host = logstashContainer.getHost();
        String port = String.valueOf(logstashContainer.getMappedPort(5043));
        Log.info(c, "setUp", "Logstash container: host=" + host + "  port=" + port);
        server.addEnvVar("LOGSTASH_HOST", host);
        server.addEnvVar("LOGSTASH_PORT", port);

        ShrinkHelper.defaultDropinApp(server, "LogstashApp", "com.ibm.logs");
        serverStart();
    }

    @Before
    public void setUpTest() throws Exception {
        Assume.assumeTrue(runTest); // runTest must be true to run test

        if (!server.isStarted()) {
            serverStart();
        }
        server.addInstalledAppForValidation("LogstashApp");
    }

    @Test
    public void testMessages() {
        assertNotNull("Cannot find TRAS4301W from messages.log", server.waitForStringInLog("TRAS4301W", 10000));
    }

    @Test
    @AllowedFFDC({ "java.lang.NullPointerException" })
    public void testTags() throws Exception {
        String testName = "testTags";

        clearContainerOutput();
        String[] tagList = new String[] { "externalTag1", "externalTag2", "externalTag3", "singleTag", "correctTag", "_", ";", "1234?:" };

        // Create message and access log event
        createMessageEvent(testName);

        // Create trace and access log event
        createTraceEvent();

        // Create FFDC and access log event
        createFFDCEvent(1);
        assertNotNull(waitForStringInContainerOutput(LIBERTY_MESSAGE));
        assertNotNull(waitForStringInContainerOutput(LIBERTY_TRACE));
        assertNotNull(waitForStringInContainerOutput(LIBERTY_FFDC));
        assertNotNull(waitForStringInContainerOutput(LIBERTY_ACCESSLOG));

        // Check results
        List<JSONObject> jObjList = parseJsonInContainerOutput();
        boolean foundMessage = false;
        boolean foundGC = false;
        boolean foundFFDC = false;
        boolean foundTrace = false;
        boolean foundAccessLog = false;

        String type;
        String tags;
        String msg;
        for (JSONObject jObj : jObjList) {
            type = jObj.getString("type");
            if (type.equals(LIBERTY_MESSAGE)) {
                if (!foundMessage) {
                    msg = jObj.getString(KEY_MESSAGE);
                    if (msg.contains(testName)) {
                        Log.info(c, testName, "found " + LIBERTY_MESSAGE + " message=" + msg);
                        tags = jObj.getString(KEY_TAGS);
                        assertTrue("Cannot find all tags from " + LIBERTY_MESSAGE + " json=" + jObj.toString(), checkIfTagPresent(tags, tagList));
                        foundMessage = true;
                    }
                }
            } else if (type.equals(LIBERTY_TRACE)) {
                if (!foundTrace) {
                    msg = jObj.getString(KEY_MESSAGE);
                    Log.info(c, testName, "found " + LIBERTY_TRACE + " message=" + msg);
                    tags = jObj.getString(KEY_TAGS);
                    assertTrue("Cannot find all tags from " + LIBERTY_TRACE + " json=" + jObj.toString(), checkIfTagPresent(tags, tagList));
                    foundTrace = true;
                }
            } else if (type.equals(LIBERTY_FFDC)) {
                if (!foundFFDC) {
                    msg = jObj.getString(KEY_OBJECTDETAILS);
                    Log.info(c, testName, "found " + LIBERTY_FFDC + " " + KEY_OBJECTDETAILS + "=" + msg);
                    tags = jObj.getString(KEY_TAGS);
                    assertTrue("Cannot find all tags from " + LIBERTY_FFDC + " json=" + jObj.toString(), checkIfTagPresent(tags, tagList));
                    foundFFDC = true;
                }
            } else if (type.equals(LIBERTY_ACCESSLOG)) {
                if (!foundAccessLog) {
                    msg = jObj.getString(KEY_URLPATH);
                    Log.info(c, testName, "found " + LIBERTY_ACCESSLOG + " " + KEY_URLPATH + "=" + msg);
                    tags = jObj.getString(KEY_TAGS);
                    assertTrue("Cannot find all tags from " + LIBERTY_ACCESSLOG + " json=" + jObj.toString(), checkIfTagPresent(tags, tagList));
                    foundAccessLog = true;
                }
            } else if (type.equals(LIBERTY_GC)) {
                if (!foundGC) {
                    Log.info(c, testName, "found " + LIBERTY_GC + " reason=" + jObj.getString(KEY_REASON));
                    tags = jObj.getString(KEY_TAGS);
                    assertTrue("Cannot find all tags from " + LIBERTY_GC + " json=" + jObj.toString(), checkIfTagPresent(tags, tagList));
                    foundGC = true;
                }
            } else {
                fail("Invalid event type found: " + type);
            }
            if (foundMessage && foundTrace && foundFFDC && foundAccessLog && foundGC) {
                Log.info(c, testName, "All 5 event types found");
                return;
            }
        }
        assertTrue(LIBERTY_MESSAGE + " not found", foundMessage);
        assertTrue(LIBERTY_TRACE + " not found", foundTrace);
        assertTrue(LIBERTY_FFDC + " not found", foundFFDC);
        assertTrue(LIBERTY_ACCESSLOG + " not found", foundAccessLog);
        // Not all JVM produce GC
        if (isGCSupported()) {
            assertTrue(LIBERTY_GC + " not found", foundGC);
        } else {
            Log.info(c, testName, "GC not supported " + LIBERTY_GC + foundGC);
        }
    }

    @After
    public void tearDown() {
    }

    @AfterClass
    public static void completeTest() throws Exception {
        if (!runTest) {
            return;
        }

        try {
            if (server.isStarted()) {
                Log.info(c, "competeTest", "---> Stopping server..");
                server.stopServer("TRAS4301W");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void serverStart() throws Exception {
        Log.info(c, "serverStart", "--->  Starting Server.. ");
        server.startServer();
    }

    private boolean isGCSupported() {
        Log.info(c, testName, "os_name: " + os.toLowerCase() + "\t java_jdk: " + System.getProperty("java.vendor"));
        String JAVA_HOME = System.getenv("JAVA_HOME");
        Log.info(c, testName, "JAVA_HOME: " + JAVA_HOME);
        boolean healthCenterInstalled = false;
        if (os.toLowerCase().contains("mac") || !System.getProperty("java.vendor").toLowerCase().contains("ibm")
            || System.getProperty("java.vendor.url").toLowerCase().contains("sun") || !healthCenterInstalled) {
            return false;
        }
        if (JAVA_HOME.endsWith("jre")) {
            if (new File(JAVA_HOME + "/lib/ext/healthcenter.jar").exists()) {
                healthCenterInstalled = true;
                Log.info(c, testName, " jar file for health center under path " + JAVA_HOME + "/lib/ext/healthcenter.jar");
            }
            Log.info(c, testName, " jar file for health center under path " + JAVA_HOME + "/lib/ext/healthcenter.jar exist:"
                                  + new File(JAVA_HOME + "/lib/ext/healthcenter.jar").exists());
            return true;
        } else if (JAVA_HOME.endsWith("bin")) {
            healthCenterInstalled = findHealthCenterDirecotry(JAVA_HOME.substring(0, JAVA_HOME.indexOf("bin") + 1));
            if (!healthCenterInstalled) {
                Log.info(c, testName, " unable to find heathcenter.jar, thus unable to produce gc events. Thus, this check will be by-passed");
            }
            return false;
        } else {
            healthCenterInstalled = findHealthCenterDirecotry(JAVA_HOME);
            if (!healthCenterInstalled) {
                Log.info(c, testName, " unable to find heathcenter.jar, thus unable to produce gc events. Thus, this check will be by-passed");
            }
            return false;
        }
    }

    private boolean findHealthCenterDirecotry(String directoryPath) {
        boolean jarFileExist = false;
        File[] files = new File(directoryPath).listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                jarFileExist = findHealthCenterDirecotry(file.getAbsolutePath());
                if (jarFileExist == true) {
                    return true;
                }
            } else {
                if (file.getAbsolutePath().contains("healthcenter.jar")) {
                    Log.info(c, testName, " healthcetner.jar is found under path " + file.getAbsolutePath());
                    return true;
                }
            }
        }
        return jarFileExist;
    }

    private boolean checkIfTagPresent(String line, String[] tags) {
        for (String tag : tags) {
            if (!line.contains(tag)) {
                return false;
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    protected LibertyServer getServer() {
        return server;
    }

}
