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
package com.ibm.ws.security.fat.common;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.ibm.websphere.simplicity.log.Log;

public class CommonTest {

    private final static Class<?> thisClass = CommonTest.class;
    public static String _testName = "";
    protected static int timeoutCounter = 0;
    protected static int allowableTimeoutCount = 0;
    public static CommonMessageTools msgUtils = new CommonMessageTools();

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
                    if (theLine.contains("TestClass END")) {
                        Log.info(thisClass, method, "Found an end of test class marker in log");
                        timeoutFound = false;
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
}
