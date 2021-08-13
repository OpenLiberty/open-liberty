/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;
import java.util.Set;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.utils.WebClientTracker;

import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FileUtils;
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
            Log.info(thisClass, methodName, "");
            Log.info(thisClass, methodName, "TTTTT EEEEE  SSSS TTTTT   FFFFF  AAA  IIIII L     EEEEE DDDD");
            Log.info(thisClass, methodName, "  T   E     S       T     F     A   A   I   L     E     D   D");
            Log.info(thisClass, methodName, "  T   EEE    SSS    T     FFF   AAAAA   I   L     EEE   D   D");
            Log.info(thisClass, methodName, "  T   E         S   T     F     A   A   I   L     E     D   D");
            Log.info(thisClass, methodName, "  T   EEEEE SSSS    T     F     A   A IIIII LLLLL EEEEE DDDD");
            Log.info(thisClass, methodName, "");
            super.failed(e, description);
        }

        @Override
        protected void succeeded(Description description) {

            String methodName = "succeeded";
            Log.info(thisClass, methodName, _testName + ": Test succeeded");
            Log.info(thisClass, methodName, "");
            Log.info(thisClass, methodName, "TTTTT EEEEE  SSSS TTTTT   PPPP   AAA   SSSS SSSSS EEEEE DDDD");
            Log.info(thisClass, methodName, "  T   E     S       T     P   P A   A S     S     E     D   D");
            Log.info(thisClass, methodName, "  T   EEE    SSS    T     PPPP  AAAAA  SSS   SSS  EEE   D   D");
            Log.info(thisClass, methodName, "  T   E         S   T     F     A   A     S     S E     D   D");
            Log.info(thisClass, methodName, "  T   EEEEE SSSS    T     F     A   A SSSS  SSSS  EEEEE DDDD");
            Log.info(thisClass, methodName, "");
            super.succeeded(description);
        }
    };

    protected static void testSkipped() {

        String methodName = "testSkipped";
        Log.info(thisClass, methodName, "");
        Log.info(thisClass, methodName, "TTTTT EEEEE  SSSS TTTTT   SSSS K   K IIIII PPPP  PPPP  EEEEE DDDD");
        Log.info(thisClass, methodName, "  T   E     S       T    S     K  K    I   P   P P   P E     D   D");
        Log.info(thisClass, methodName, "  T   EEE    SSS    T     SSS  KKK     I   PPPP  PPPP  EEE   D   D");
        Log.info(thisClass, methodName, "  T   E         S   T        S K  K    I   P     P     E     D   D");
        Log.info(thisClass, methodName, "  T   EEEEE SSSS    T    SSSS  K   K IIIII P     P     EEEEE DDDD");
        Log.info(thisClass, methodName, "");
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

        LibertyServer myServer = server.getServer();
        Machine machine = myServer.getMachine();

        try {
            Set<String> beforeAppList = myServer.listAllInstalledAppsForValidation();
            for (String app : beforeAppList) {
                Log.info(thisClass, "transformAppsInDefaultDirs", "Before: installed app to check: " + app);
            }
        } catch (Exception e) {
            Log.error(thisClass, "transformAppsInDefaultDirs", e, "Failed to get the list of applications to validate");
        }

        Log.info(thisClass, "transformAppsInDefaultDirs", "Processing " + appDirName + " for serverName: " + myServer.getServerName());
        RemoteFile appDir = new RemoteFile(machine, LibertyServerUtils.makeJavaCompatible(myServer.getServerRoot() + File.separatorChar + appDirName, machine));

        RemoteFile[] list = null;
        try {
            if (appDir.isDirectory()) {
                list = appDir.list(false);
            }
        } catch (Exception e) {
            Log.error(thisClass, "transformAppsInDefaultDirs", e);
        }
        try {
            if (list != null) {
                Log.info(thisClass, "transformAppsInDefaultDirs", "number of files before cleanup: " + list.length);
                for (RemoteFile app : list) {
                    Log.info(thisClass, "transformAppsInDefaultDirs", "Cleanup generic file: file name is: " + app.toString());
                    if (!app.toString().endsWith(".jakarta") && !app.toString().endsWith(".orig")) {
                        Log.info(thisClass, "transformAppsInDefaultDirs", "Delete the file " + app + " as it is not needed for this instance");
                        File fromApp = new File(app.toString());
                        FileUtils.recursiveDelete(fromApp.getAbsoluteFile());

                    }
                }
                // now that the we've removed the original file, we can copy the appropriate jakarta or orig file
                list = appDir.list(false);
                if (list != null) {
                    Log.info(thisClass, "transformAppsInDefaultDirs", "number of files after cleanup: " + list.length);
                    for (RemoteFile app : list) {
                        Log.info(thisClass, "transformAppsInDefaultDirs", "file name is: " + app.toString());
                        File newName = new File(LibertyServerUtils.makeJavaCompatible(app.toString().replace(".jakarta", "").replace(".orig", ""), machine));
                        File fromApp = new File(app.toString());
                        if ((JakartaEE9Action.isActive() && app.toString().endsWith(".jakarta")) ||
                                (!JakartaEE9Action.isActive() && app.toString().endsWith(".orig"))) {
                            Log.info(thisClass, "transformAppsInDefaultDirs", "Copy " + app.toString() + " to generic app name");
                            //                JakartaEE9Action.transformApp(Paths.get(app.getAbsolutePath()));
                            FileUtils.copyFile(fromApp.getAbsoluteFile(), newName.getAbsoluteFile());
                            FileUtils.recursiveDelete(fromApp.getAbsoluteFile());
                            // remove the .jakarta or .orig named app from the list of apps to wait for
                            myServer.removeInstalledAppForValidation(app.getName());
                            // make sure that the app we will need to start is in the list of installed apps
                            myServer.addInstalledAppForValidation(newName.getAbsoluteFile().getName().replace(".war", "").replace(".ear", ""));
                        } else {
                            Log.info(thisClass, "transformAppsInDefaultDirs", "Delete the file " + app + " as it is not needed for this instance");
                            FileUtils.recursiveDelete(fromApp.getAbsoluteFile());
                            myServer.removeInstalledAppForValidation(app.getName());
                        }
                    }
                }
            }

            RemoteFile[] afterList = appDir.list(false);
            if (afterList != null) {
                for (RemoteFile app : afterList) {
                    Log.info(thisClass, "transformAppsInDefaultDirs", "file name after update: " + app.toString());
                }
            } else {
                Log.info(thisClass, "transformAppsInDefaultDirs", "No files after update");
            }
        } catch (Exception e) {
            Log.error(thisClass, "transformAppsInDefaultDirs", e);
        }

        try {
            Set<String> afterAppList = myServer.listAllInstalledAppsForValidation();
            for (String app : afterAppList) {
                Log.info(thisClass, "transformAppsInDefaultDirs", "After: installed app to check: " + app);
            }
        } catch (Exception e) {
            Log.error(thisClass, "transformAppsInDefaultDirs", e, "Failed to get the list of applications to validate");
        }

    }

    /**
     * JakartaEE9 transform applications for a specified server.
     *
     * @param serverName
     *            The server to transform the applications on.
     */
    public static void transformApps(TestServer server) throws Exception {

        transformAppsInDefaultDirs(server, "dropins");
        transformAppsInDefaultDirs(server, "test-apps");

        // we have to handle the idp app differently - as we have to start the idp and sp servers
        // and exchange config info between them before starting the idp app
        server.getServer().removeInstalledAppForValidation("idp");

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

    @BeforeClass
    public static void xyz() {

        CommonAppTransformer.transformAppsInPublish();

    }

}
