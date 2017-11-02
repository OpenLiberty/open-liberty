/*******************************************************************************
 * Copyright (c) 2013, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.embeddable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.kernel.boot.ReturnCode;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.wsspi.kernel.embeddable.Server.Result;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

public class EmbeddedServerDriver implements ServerEventListener {

    private final Class<?> c = EmbeddedServerDriver.class;
    private String CURRENT_METHOD_NAME = null;

    private CountDownLatch startingEventOccurred;
    private CountDownLatch startedEventOccurred;
    private CountDownLatch stoppedEventOccurred;
    private CountDownLatch failedEventOccurred;

    private String serverName = null;
    private String userDir = null;
    private String outputDir = null;

    private ServerBuilder sb = null;
    private Server server = null;
    private Result result = null;
    private Properties props = null;
    private Properties props2 = null;
    private Properties props3 = null;

    private List<AssertionFailedError> failures = null;

    public EmbeddedServerDriver(String serverName, String userDir, String outputDir) throws UnsupportedEncodingException {
        this.serverName = serverName;
        this.userDir = userDir;
        this.outputDir = outputDir;
        this.props = new Properties();
        this.props.setProperty("com.ibm.websphere.productId", "com.ibm.cicsts");
        this.props.setProperty("com.ibm.websphere.productInstall", "wlp/usr/servers/com.ibm.wsspi.kernel.embeddable.add.product.extension.fat/producttest");
        this.props2 = new Properties();
        this.props2.setProperty("com.ibm.websphere.productId", "com.ibm.cicstsb");
        this.props2.setProperty("com.ibm.websphere.productInstall", "wlp/usr/servers/com.ibm.wsspi.kernel.embeddable.add.product.extension.multiple.fat/producttestb");
        this.props3 = new Properties();
        this.props3.setProperty("com.ibm.websphere.productId", "com.ibm.cicsts");
        this.props3.setProperty("com.ibm.websphere.productInstall", "wlp/usr/servers/com.ibm.wsspi.kernel.embeddable.add.product.extension.multiple.fat/producttest");
        init("EmbeddedServerDriver");
    }

    public void init(String CURRENT_METHOD_NAME) throws UnsupportedEncodingException {
        this.CURRENT_METHOD_NAME = CURRENT_METHOD_NAME;

        Log.info(c, "init", "Setting up for " + this.CURRENT_METHOD_NAME);

        startingEventOccurred = new CountDownLatch(1);
        startedEventOccurred = new CountDownLatch(1);
        stoppedEventOccurred = new CountDownLatch(1);
        failedEventOccurred = new CountDownLatch(1);

        failures = new ArrayList<AssertionFailedError>();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos, true, "UTF-8"));

        sb = new ServerBuilder();
        if (CURRENT_METHOD_NAME.equals("testAddProductExtension")) {
            Log.info(c, "init", "current method name is testAddProductExtension ");
            server = sb.setName(serverName).setOutputDir(new File(outputDir)).setUserDir(new File(userDir)).addProductExtension("productA",
                                                                                                                                props).setServerEventListener(this).build();
        } else if (CURRENT_METHOD_NAME.equals("testAddProductExtensionMultiple")) {
            Log.info(c, "init", "current method name is testAddProductExtensionMultiple ");
            server = sb.setName(serverName).setOutputDir(new File(outputDir)).setUserDir(new File(userDir)).addProductExtension("productA",
                                                                                                                                props3).addProductExtension("productB",
                                                                                                                                                            props2).setServerEventListener(this).build();

        } else {
            server = sb.setName(serverName).setOutputDir(new File(outputDir)).setUserDir(new File(userDir)).setServerEventListener(this).build();
        }

        String serverConsoleOutput = new String(baos.toByteArray(), "UTF-8");
        Log.info(c, "init", "consoleOutput = " + serverConsoleOutput);

        result = null;
        checkServerRunning(false); // server should be stopped
    }

    public void tearDown() {
        Log.info(c, "init", "Cleaning up after " + this.CURRENT_METHOD_NAME);

        if (server != null) {
            Future<Result> stopFuture = server.stop();
            stopFuture.cancel(true);
        }

        startingEventOccurred = null;
        startedEventOccurred = null;
        stoppedEventOccurred = null;
        failedEventOccurred = null;
    }

    public void testStoppingAStoppedServer() {

        // Stop a stopped server
        Future<Result> stopFuture = server.stop();
        try {
            result = stopFuture.get();
            dumpResult("Stopping a stopped server", result);
            Assert.assertFalse("Stopping a stopped server should not be successful", result.successful());
            Assert.assertEquals("Should have a redundant operation returned", ReturnCode.REDUNDANT_ACTION_STATUS.getValue(), result.getReturnCode());
        } catch (AssertionFailedError e) {
            failures.add(e);
            Log.error(c, CURRENT_METHOD_NAME, e);
        } catch (InterruptedException e) {
            failures.add(new AssertionFailedError("Stop operation did not complete normally: " + e));
            Log.error(c, CURRENT_METHOD_NAME, e);
        } catch (ExecutionException e) {
            failures.add(new AssertionFailedError("Stop operation could not be queued: " + e));
            Log.error(c, CURRENT_METHOD_NAME, e);
        }

        checkServerRunning(false); // server should be stopped
    }

    public void testStartingAStoppedServer() {
        PrintStream originalSysOut = System.out;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setOut(new PrintStream(baos, true, "UTF-8"));
            coldStartServer();
            verifyServerEvent("\"STARTING\" ServerEvent should have fired", startingEventOccurred);
            verifyServerEvent("\"STARTED\" ServerEvent should have fired", startedEventOccurred);

            String serverConsoleOutput = new String(baos.toByteArray(), "UTF-8");
            Log.info(c, "testStartingAStoppedServer", "consoleOutput = " + serverConsoleOutput);
            try {
                Assert.assertTrue("No indication that application started", serverConsoleOutput.contains("CWWKZ0001I: Application simpleApp started"));
            } catch (Throwable t) {
                failures.add(new AssertionFailedError("Exception occurred while searching for app started message in logs - " + t));
                Log.error(c, CURRENT_METHOD_NAME, t);
            }

            // PI20344: Verify that Utils.java is using the correct user dir
            String utilsUserDirAbsolutePath = Utils.getUserDir().getAbsolutePath().replace('\\', '/');
            String userDirAbsolutePath = userDir.replace('\\', '/');
            Log.info(c, "testStartingAStoppedServer", "UserDirAbsolutePath=[" + userDirAbsolutePath + "] utilsUserDirAbsolutePath=[" + utilsUserDirAbsolutePath + "]");
            Assert.assertTrue("Utils.userDir() should be using the java property with any luck at all.", userDirAbsolutePath.equals(utilsUserDirAbsolutePath));

            stopRunningServer();
        } catch (UnsupportedEncodingException ex) {
        } finally {
            System.setOut(originalSysOut);
        }
    }

    public void testAddProductExtension() {
        PrintStream originalSysOut = System.out;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setOut(new PrintStream(baos, true, "UTF-8"));
            coldStartServer();
            verifyServerEvent("\"STARTING\" ServerEvent should have fired", startingEventOccurred);
            verifyServerEvent("\"STARTED\" ServerEvent should have fired", startedEventOccurred);

            String serverConsoleOutput = new String(baos.toByteArray(), "UTF-8");
            Log.info(c, "testAddProductExtension", "consoleOutput = " + serverConsoleOutput);
            try {
                Assert.assertTrue("No indication that application started", serverConsoleOutput.contains("CWWKZ0001I: Application simpleApp started"));
            } catch (Throwable t) {
                failures.add(new AssertionFailedError("Exception occurred while searching for app started message in logs - " + t));
                Log.error(c, CURRENT_METHOD_NAME, t);
            }
            try {
                //[AUDIT   ] CWWKF0012I: The server installed the following features: [jsp-2.2, productA:prodtest-1.0, timedexit-1.0, servlet-3.1].
                Assert.assertTrue("No indication that the product extension feature productA:prodtest-1.0 was installed",
                                  isProductExtensionInstalled(serverConsoleOutput, "productA:prodtest-1.0"));
            } catch (Throwable t) {
                failures.add(new AssertionFailedError("Exception occurred while searching for the product extension feature was installed message in logs - " + t));
                Log.error(c, CURRENT_METHOD_NAME, t);
            }

            try {
                // Check that the server logs contain the CWWKE0108I message
                String messageToFind = "CWWKE0108I";
                boolean messageFound = false;
                Log.info(c, CURRENT_METHOD_NAME, "SCANNING: " + outputDir + "/" + serverName + "/logs/message.log");
                File logFile = new File(outputDir + "/" + serverName + "/logs/messages.log");
                Scanner logScanner = new Scanner(logFile);
                while (logScanner.hasNextLine() && messageFound == false) {
                    String line = logScanner.nextLine();
                    Log.info(c, CURRENT_METHOD_NAME, line);
                    if (line.contains(messageToFind)) {
                        messageFound = true;
                    }
                }
                logScanner.close();
                Assert.assertTrue(messageFound);
            } catch (Throwable t) {
                failures.add(new AssertionFailedError("Exception occurred while searching for product extension was added message in logs - " + t));
                Log.error(c, CURRENT_METHOD_NAME, t);
            }

            stopRunningServer();
        } catch (UnsupportedEncodingException ex) {
        } finally {
            System.setOut(originalSysOut);
        }
    }

    /**
     * Determine if the input product extension exists in the input string.
     *
     * @param inputString string to search.
     * @param productExtension product extension to search for.
     * @return true if input product extension is found in the input string.
     */
    private boolean isProductExtensionInstalled(String inputString, String productExtension) {
        if ((productExtension == null) || (inputString == null)) {
            return false;
        }
        int msgIndex = inputString.indexOf("CWWKF0012I: The server installed the following features:");
        if (msgIndex == -1) {
            return false;
        }

        String msgString = inputString.substring(msgIndex);
        int leftBracketIndex = msgString.indexOf("[");
        int rightBracketIndex = msgString.indexOf("]");
        if ((leftBracketIndex == -1) ||
            (rightBracketIndex == -1) ||
            (rightBracketIndex < leftBracketIndex)) {
            return false;
        }

        String features = msgString.substring(leftBracketIndex, rightBracketIndex);
        Log.info(c, "isProductExtensionInstalled", features);
        if (features.indexOf(productExtension) == -1) {
            return false;
        }
        return true;
    }

    public void testAddProductExtensionMultiple() {
        PrintStream originalSysOut = System.out;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setOut(new PrintStream(baos, true, "UTF-8"));
            coldStartServer();
            verifyServerEvent("\"STARTING\" ServerEvent should have fired", startingEventOccurred);
            verifyServerEvent("\"STARTED\" ServerEvent should have fired", startedEventOccurred);

            String serverConsoleOutput = new String(baos.toByteArray(), "UTF-8");
            Log.info(c, "testAddProductExtensionMultiple", "consoleOutput = " + serverConsoleOutput);
            try {
                Assert.assertTrue("No indication that application started", serverConsoleOutput.contains("CWWKZ0001I: Application simpleApp started"));
            } catch (Throwable t) {
                failures.add(new AssertionFailedError("Exception occurred while searching for app started message in logs - " + t));
                Log.error(c, CURRENT_METHOD_NAME, t);
            }
            // [AUDIT   ] CWWKF0012I: The server installed the following features: [jsp-2.2, productA:prodtest-1.0, productB:prodtestb-1.0, timedexit-1.0, servlet-3.1].
            try {
                Assert.assertTrue("No indication that the product extension feature productA:prodtest-1.0 was installed",
                                  isProductExtensionInstalled(serverConsoleOutput, "productA:prodtest-1.0"));
            } catch (Throwable t) {
                failures.add(new AssertionFailedError("Exception occurred while searching for the product extension feature was installed message in logs - " + t));
                Log.error(c, CURRENT_METHOD_NAME, t);
            }

            try {
                Assert.assertTrue("No indication that the product extension feature productB:prodtestb-1.0 was installed",
                                  isProductExtensionInstalled(serverConsoleOutput, "productB:prodtestb-1.0"));
            } catch (Throwable t) {
                failures.add(new AssertionFailedError("Exception occurred while searching for the product extension feature was installed message in logs - " + t));
                Log.error(c, CURRENT_METHOD_NAME, t);
            }

            try {
                // Check that the server logs contain the CWWKE0108I message
                String messageToFind = "CWWKE0108I";
                boolean messageFound = false;
                Log.info(c, CURRENT_METHOD_NAME, "SCANNING: " + outputDir + "/" + serverName + "/logs/message.log");
                File logFile = new File(outputDir + "/" + serverName + "/logs/messages.log");
                Scanner logScanner = new Scanner(logFile);
                while (logScanner.hasNextLine() && messageFound == false) {
                    String line = logScanner.nextLine();
                    Log.info(c, CURRENT_METHOD_NAME, line);
                    if (line.contains(messageToFind)) {
                        messageFound = true;
                    }
                }
                logScanner.close();
                Assert.assertTrue(messageFound);
            } catch (Throwable t) {
                failures.add(new AssertionFailedError("Exception occurred while searching for product extension was added message in logs - " + t));
                Log.error(c, CURRENT_METHOD_NAME, t);
            }

            stopRunningServer();
        } catch (UnsupportedEncodingException ex) {
        } finally {
            System.setOut(originalSysOut);
        }
    }

    public void testStartingAStartedServer() {
        warmStartServer();

        verifyServerEvent("\"STARTING\" ServerEvent should have fired", startingEventOccurred);
        verifyServerEvent("\"STARTED\" ServerEvent should have fired", startedEventOccurred);

        // Start the started server
        Future<Result> startFuture2 = server.start();
        try {
            result = startFuture2.get();
            dumpResult("Starting a started server", result);
            Assert.assertFalse("Starting a started server should not be successful", result.successful());
            Assert.assertEquals("Should have a redundant operation returned", ReturnCode.REDUNDANT_ACTION_STATUS.getValue(), result.getReturnCode());
        } catch (AssertionFailedError e) {
            failures.add(e);
            Log.error(c, CURRENT_METHOD_NAME, e);
        } catch (InterruptedException e) {
            failures.add(new AssertionFailedError("Start operation did not complete normally: " + e));
            Log.error(c, CURRENT_METHOD_NAME, e);
        } catch (ExecutionException e) {
            failures.add(new AssertionFailedError("Start operation could not be queued: " + e));
            Log.error(c, CURRENT_METHOD_NAME, e);
        }

        checkServerRunning(true); // server should be started
        stopRunningServer();
    }

    public void testStoppingAStartedServer() {
        warmStartServer();
        verifyServerEvent("\"STARTING\" ServerEvent should have fired", startingEventOccurred);
        verifyServerEvent("\"STARTED\" ServerEvent should have fired", startedEventOccurred);
        stopRunningServer();
    }

    public void testBadArgument() {
        Future<Result> startFuture = server.start(new String[] { "--nOnSeNsE" });

        try {
            // check for failed event: should fire w/o requiring interaction w/ future.get
            verifyServerEvent("\"FAILED\" ServerEvent should have fired", failedEventOccurred);

            result = startFuture.get();
            dumpResult("Starting a server", result);
            Assert.assertFalse("Result of start attempt with bad args should fail", result.successful());
            Assert.assertEquals("Should have an BAD_ARGUMENT return code", ReturnCode.BAD_ARGUMENT.getValue(), result.getReturnCode());
        } catch (AssertionFailedError e) {
            failures.add(e);
            Log.error(c, CURRENT_METHOD_NAME, e);
        } catch (InterruptedException e) {
            failures.add(new AssertionFailedError("Start operation did not complete normally: " + e));
            Log.error(c, CURRENT_METHOD_NAME, e);
            startFuture.cancel(true);
        } catch (ExecutionException e) {
            failures.add(new AssertionFailedError("Start operation could not be queued: " + e));
            Log.error(c, CURRENT_METHOD_NAME, e);
        }

        checkServerRunning(false); // server should not be started
    }

    public void testLaunchException() {
        Future<Result> startFuture = server.start("--create"); // server already created in init(), double create should fail

        try {
            result = startFuture.get();
            dumpResult("Starting an embedded server with \"--create\", which is a bad thing to do", result);
            Assert.assertFalse("Result of LaunchException should be failure", result.successful());
            Assert.assertEquals("Should have a BAD_ARGUMENT return code", ReturnCode.BAD_ARGUMENT.getValue(), result.getReturnCode());
        } catch (AssertionFailedError e) {
            failures.add(e);
            Log.error(c, CURRENT_METHOD_NAME, e);
        } catch (InterruptedException e) {
            failures.add(new AssertionFailedError("Start operation did not complete normally: " + e));
            Log.error(c, CURRENT_METHOD_NAME, e);
            startFuture.cancel(true);
        } catch (ExecutionException e) {
            failures.add(new AssertionFailedError("Start operation could not be queued: " + e));
            Log.error(c, CURRENT_METHOD_NAME, e);
        }

        checkServerRunning(false); // server should not be started
    }

    public void testLocationException() {

        String bogusName = "bOgUsSeRvErNaMe";
        server = sb.setName(bogusName).setOutputDir(new File(outputDir)).setUserDir(new File(userDir)).setServerEventListener(this).build();

        (new File(outputDir + "/" + bogusName, "server.xml")).delete(); // delete the server from the hfs, so start() throws a LocationException

        Future<Result> startFuture = server.start();

        try {
            // check for failed event: should fire w/o requiring interaction w/ future.get
            verifyServerEvent("\"FAILED\" ServerEvent should have fired", failedEventOccurred);

            result = startFuture.get();
            dumpResult("Starting a server with a bogus name", result);
            Assert.assertFalse("Result of LocationException should be failure", result.successful());
            Assert.assertEquals("Should have a LOCATION_EXCEPTION return code", ReturnCode.LOCATION_EXCEPTION.getValue(), result.getReturnCode());
        } catch (AssertionFailedError e) {
            failures.add(e);
            Log.error(c, CURRENT_METHOD_NAME, e);
        } catch (InterruptedException e) {
            failures.add(new AssertionFailedError("Start operation did not complete normally: " + e));
            Log.error(c, CURRENT_METHOD_NAME, e);
            startFuture.cancel(true);
        } catch (ExecutionException e) {
            failures.add(new AssertionFailedError("Start operation could not be queued: " + e));
            Log.error(c, CURRENT_METHOD_NAME, e);
        }

        checkServerRunning(false); // server should not be started
    }

    public void testServerUnknownStatus() {
        //TODO: This is not called by the test framework
        //      because I don't know how to guarantee an InterruptedException
        //      without jMock, which seems pointless in a FAT scenario like this

        Future<Result> startFuture = server.start(new String[] { "--clean" });

        try {
            result = startFuture.get();
            dumpResult("Force an InterruptException while starting a server", result);
            Assert.assertFalse("Result of InterruptedException should be failure", result.successful());
            Assert.assertEquals("Should have a SERVER_UNKNOWN_STATUS return code", ReturnCode.SERVER_UNKNOWN_STATUS.getValue(), result.getReturnCode());
        } catch (AssertionFailedError e) {
            failures.add(e);
            Log.error(c, CURRENT_METHOD_NAME, e);
        } catch (InterruptedException e) {
            failures.add(new AssertionFailedError("Start operation did not complete normally: " + e));
            Log.error(c, CURRENT_METHOD_NAME, e);
            startFuture.cancel(true);
        } catch (ExecutionException e) {
            failures.add(new AssertionFailedError("Start operation could not be queued: " + e));
            Log.error(c, CURRENT_METHOD_NAME, e);
        }

        checkServerRunning(true); // server should be started

    }

    public void testErrorServerStart() {
        //TODO: This is not called by the test framework
        //      because I don't know how to force waitForStarted() to return false
        //      without jMock, which seems pointless in a FAT scenario like this

    }

    public List<AssertionFailedError> getFailures() {
        return failures;
    }

    private void verifyServerEvent(String msg, CountDownLatch event) {
        boolean pass = false;

        try {
            event.await(5, TimeUnit.SECONDS);
            pass = true;
        } catch (InterruptedException e) {
            pass = false;
        }

        try {
            Assert.assertTrue(msg, pass);
        } catch (AssertionFailedError e) {
            failures.add(new AssertionFailedError(e.toString()));
            Log.error(c, CURRENT_METHOD_NAME, e);
        }

    }

    private void warmStartServer() {
        Future<Result> startFuture = server.start();

        try {
            result = startFuture.get();
            dumpResult("Starting a server", result);
            Assert.assertTrue("Result of start attempt should be successful", result.successful());
            Assert.assertEquals("Should have an OK return code", ReturnCode.OK.getValue(), result.getReturnCode());
        } catch (AssertionFailedError e) {
            failures.add(e);
            Log.error(c, CURRENT_METHOD_NAME, e);
        } catch (InterruptedException e) {
            failures.add(new AssertionFailedError("Start operation did not complete normally: " + e));
            Log.error(c, CURRENT_METHOD_NAME, e);
            startFuture.cancel(true);
        } catch (ExecutionException e) {
            failures.add(new AssertionFailedError("Start operation could not be queued: " + e));
            Log.error(c, CURRENT_METHOD_NAME, e);
        }

        checkServerRunning(true); // server should be started
    }

    private void coldStartServer() {
        Future<Result> startFuture = server.start(new String[] { "--clean" });

        try {
            result = startFuture.get();
            dumpResult("Starting a server", result);
            Assert.assertTrue("Result of first start attempt should be successful", result.successful());
            Assert.assertEquals("Should have an OK return code", ReturnCode.OK.getValue(), result.getReturnCode());
        } catch (AssertionFailedError e) {
            failures.add(e);
            Log.error(c, CURRENT_METHOD_NAME, e);
        } catch (InterruptedException e) {
            failures.add(new AssertionFailedError("Start operation did not complete normally: " + e));
            Log.error(c, CURRENT_METHOD_NAME, e);
            startFuture.cancel(true);
        } catch (ExecutionException e) {
            failures.add(new AssertionFailedError("Start operation could not be queued: " + e));
            Log.error(c, CURRENT_METHOD_NAME, e);
        }

        checkServerRunning(true); // server should be started
    }

    private void stopRunningServer() {
        Future<Result> stopFuture = server.stop();
        try {
            result = stopFuture.get();
            dumpResult("Stopping a started server", result);
            Assert.assertTrue("Stopping a running server should be successful", result.successful());
            Assert.assertEquals("Should have an OK return code", ReturnCode.OK.getValue(), result.getReturnCode());
        } catch (AssertionFailedError e) {
            failures.add(e);
            Log.error(c, CURRENT_METHOD_NAME, e);
        } catch (InterruptedException e) {
            failures.add(new AssertionFailedError("Stop operation did not complete normally: " + e));
            Log.error(c, CURRENT_METHOD_NAME, e);
        } catch (ExecutionException e) {
            failures.add(new AssertionFailedError("Stop operation could not be queued: " + e));
            Log.error(c, CURRENT_METHOD_NAME, e);
        }

        checkServerRunning(false); // server should be stopped

        verifyServerEvent("\"STOPPED\" ServerEvent should have fired", stoppedEventOccurred);
    }

    private void checkServerRunning(boolean expectRunning) {
        try {
            if (expectRunning) {
                Assert.assertTrue("Server should be running", server.isRunning());
            } else {
                Assert.assertFalse("Server should not be running", server.isRunning());
            }
        } catch (AssertionFailedError e) {
            failures.add(e);
        }
    }

    private void dumpResult(String msg, Result result) {
        Log.info(c, "dumpResult", msg + " -- Result: success=" + result.successful() + ", rc=" + result.getReturnCode() + ", ex=" + result.getException());
    }

    @Override
    public void serverEvent(ServerEvent serverEvent) {
        String event = serverEvent.toString();
        Log.info(c, "serverEvent", "NEW SERVER EVENT FIRED: " + event);

        switch (serverEvent.getType()) {
            case STARTING:
                startingEventOccurred.countDown();
                break;
            case STARTED:
                startedEventOccurred.countDown();
                break;
            case STOPPED:
                stoppedEventOccurred.countDown();
                break;
            case FAILED:
                failedEventOccurred.countDown();
                break;
            default:
                break;
        }
    }
}
