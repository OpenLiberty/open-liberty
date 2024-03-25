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
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Scanner;

import org.junit.After;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.utils.WebClientTracker;

import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.LibertyServerUtils;

public class CommonTest {

    private final static Class<?> thisClass = CommonTest.class;
    public static String _testName = "";
    protected static int allowableTimeoutCount = 0;
    public static CommonMessageTools msgUtils = new CommonMessageTools();
    protected WebClientTracker webClientTracker = new WebClientTracker();

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {

            String methodName = "failed";
            Log.info(thisClass, methodName, _testName + ": Test failed");
            super.failed(e, description);
        }

        @Override
        protected void succeeded(Description description) {

            String methodName = "succeeded";
            Log.info(thisClass, methodName, _testName + ": Test succeeded");
            super.succeeded(description);
        }
    };

    protected static void testSkipped() {

        String methodName = "testSkipped";
        Log.info(thisClass, methodName, _testName + ": Test skipped");
    }

    /**
     * Sets the number of allowable timeout messages in output.txt. This should be set if messages are expected NOT to
     * be found, resulting in an expected wait timeout.
     *
     * @param count
     */
    public void setAllowableTimeoutCount(int count) {
        Log.info(thisClass, "setAllowableTimeoutCount", "Setting number of allowed timeout messages to " + count);
        allowableTimeoutCount = count;
    }

    /**
     * Increments the number of allowable timeout messages by the amount provided.
     *
     * @param additionalCount
     */
    public static void addToAllowableTimeoutCount(int additionalCount) {
        allowableTimeoutCount += additionalCount;
        Log.info(thisClass, "addToAllowableTimeoutCount", "Added " + additionalCount + " to the number of allowable timeout messages. New total: " + allowableTimeoutCount);
    }

    /***
     * Test to make sure that no test case in the class has timed out waiting for msgs in the log
     * This is more of a test case test, than a product test
     *
     * @throws Exception
     */
    public static void timeoutChecker() throws Exception {
        String method = "timeoutChecker";

        int timeoutCounter = 0;
        boolean timeoutFound = false;
        String outputFile = "./results/output.txt";
        File f = new File(outputFile);
        if (f.exists() && !f.isDirectory()) {
            Log.info(thisClass, method, "Found the output.txt file");
            Scanner in = null;
            try {
                in = new Scanner(new FileReader(f));
                String theLine;
                // look for time out msg
                while (in.hasNextLine()) {
                    theLine = in.nextLine();
                    //if (in.nextLine().indexOf("Timed out") >= 0) {
                    if (theLine.indexOf("Timed out") >= 0) {
                        // timed out message stating that an update is NOT necessary should be ignored
                        if (theLine.indexOf("CWWKG0018I") < 0) {
                            if (theLine.indexOf("Found a line in output.txt that contains") < 0) {
                                Log.info(thisClass, method, "Found a line in output.txt that contains 'Timed out' : " + theLine);
                                timeoutCounter++;
                                timeoutFound = true;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    in.close();
                } catch (Exception e) { /* ignore */
                }
                Log.info(thisClass, method, " ** TestClass END ** ");
            }
        }

        if (timeoutFound && (timeoutCounter > allowableTimeoutCount)) {
            Log.info(thisClass, method, "Timeout messages found: " + timeoutCounter + ", number of allowed timeout messages: " + allowableTimeoutCount);
            throw new RuntimeException("Unexpected number of timed out messages found in output.txt - most likly a test case issue - search output.txt for 'Timed out'."
                    + System.getProperty("line.separator")
                    + "This exception is issued from an end of test class check and appears as an extra test case.  Fix the timed out msg issue and the test count will be correct!!!");
        }

    }

    /***
     * Test to make sure that no password has been logged in any of the server logs
     *
     * @throws Exception
     */
    public static void passwordChecker(TestServer server) throws Exception {

        msgUtils.printMethodName("passwordChecker");

        try {
            // updated searchForPassordsInLogs to return a count of the number of timeout msgs it generates
            // the password checker is run AFTER the timeout checker runs, so, in the first class of a project
            // everything works ok.  For the next classes, we need to account for the password checker timeout
            // msgs of the previous test classes.
            int count = server.searchForPasswordsInLogs(Constants.MESSAGES_LOG);
            count = count + server.searchForPasswordsInLogs(Constants.TRACE_LOG);
            allowableTimeoutCount = allowableTimeoutCount + count;
        } catch (RuntimeException e) {
            throw new RuntimeException("Password found in one of the server logs");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw new RuntimeException("Exception thrown searching for passwords in the server logs");
        }
    }

    public WebClient getAndSaveWebClient() throws Exception {

        WebClient webClient = TestHelpers.getWebClient();
        webClientTracker.addWebClient(webClient);
        return webClient;
    }

    public WebClient getAndSaveWebClient(boolean override) throws Exception {

        WebClient webClient = TestHelpers.getWebClient(override);
        webClientTracker.addWebClient(webClient);
        return webClient;
    }

    private static void transformAppsInDefaultDirs(TestServer server, String appDirName) {

        try {
            LibertyServer myServer = server.getServer();
            Machine machine = myServer.getMachine();

            Log.info(thisClass, "transformAppsInDefaultDirs", "Processing " + appDirName + " for serverName: " + myServer.getServerName());
            RemoteFile appDir = new RemoteFile(machine, LibertyServerUtils.makeJavaCompatible(myServer.getServerRoot() + File.separatorChar + appDirName, machine));

            RemoteFile[] list = null;
            if (appDir.isDirectory()) {
                list = appDir.list(false);
            }
            if (list != null) {
                for (RemoteFile app : list) {
                    if (!app.getName().contains("idp.war")) { // the idp.war should have already been transformed
                        JakartaEEAction.transformApp(Paths.get(app.getAbsolutePath()));
                    }
                }
            }
        } catch (Exception e) {
            Log.error(thisClass, "transformAppsInDefaultDirs", e);
            e.printStackTrace();
        }
    }

    /**
     * JakartaEE9 transform applications for a specified server.
     *
     * @param serverName
     *            The server to transform the applications on.
     */
    public static void transformApps(TestServer server) {
        if (JakartaEEAction.isEE9OrLaterActive()) {

            transformAppsInDefaultDirs(server, "dropins");
            transformAppsInDefaultDirs(server, "test-apps");
            //            // TODO - may break saml - may have to update saml rules
            //            transformAppsInDefaultDirs(server, "idp-apps");

        }
    }

    @After
    public void endTestCleanup() throws Exception {

        try {

            // clean up webClients
            webClientTracker.closeAllWebClients();

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }
}
