//%Z% %I% %W% %G% %U% [%H% %T%]
/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************
 *
 * DESCRIPTION:
 *
 * Change History:
 *
 * Reason       Version     Date        User id     Description
 * ----------------------------------------------------------------------------
 * 97932        8.5.5     13/06/2013    sumam     Test case for defect 97932.
 */
package com.ibm.ws.logging.hpel.fat;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Test case for defect 97932 : HPEL ignores purgeMaxSize
 * Test scenario
 * 10. Server startServer without any settings -> during run time, add purgeMaxSize for log to 100, stopServer server, remove purgeMaxSize attribute, startServer server --> expect
 * output: trace=50,
 * log=50
 *
 */
@RunWith(FATRunner.class)
public class HpelPurgeMaxSizeIgnoreTest_2 {

    private final static String loggerName = HpelLogDirectoryChangeTest.class.getName();
    private final static Logger logger = Logger.getLogger(loggerName);
    private final static int MAX_DEFAULT_PURGE_SIZE = 50;
    private final static int MAX_WAIT_TIME_FOR_CONFIG_UPDATE = 2500;

    @Server("HpelServer")
    public static LibertyServer server;

    static RemoteFile backup = null;
    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        // Call super.SetUp() cause we still want it's setup as well
        // Confirm HPEL is enabled
        assumeTrue(!skipTest());
        ShrinkHelper.defaultDropinApp(server, "LogFat", "com.ibm.ws.logging.hpel");
        ShrinkHelper.defaultDropinApp(server, "HpelFat", "com.ibm.ws.logging.hpel.servlet");

        // Setting the bootstrap with trace specification to get the trace logs.
        CommonTasks.addBootstrapProperty(server, "com.ibm.ws.logging.trace.specification", "*=fine=enabled");
        server.stopServer();
        server.startServer();

    }

    /**
     * Set Bootstrap Trace = 91 , server.xml Trace = 201, run the quick log and check the size of the repository.
     * It should not exceed 201 MB for tracedata and 50 MB for logdata.
     **/
    @Mode(TestMode.FULL)
    @Test
    public void testPurgeMaxSize_8() throws Exception {
        RemoteFile binaryLogDir = null;
        RemoteFile binaryTraceDir = null;
        NumberFormat nf = NumberFormat.getInstance();

        CommonTasks.addBootstrapProperty(server, "com.ibm.hpel.trace.purgeMaxSize", "91");
        //restart?
//        server.restartServer();
        if (!server.isStarted()) {
            server.startServer();
        }
        server.waitForStringInLog("CWWKF0011I", 30000);

        CommonTasks.writeLogMsg(Level.INFO, "Configuring server for test case.");
        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HpelLogElementDelete_1.xml"));
        // Short pause to ensure that the configuration is updated.
        Thread.sleep(MAX_WAIT_TIME_FOR_CONFIG_UPDATE);

        // write enough records to new log repository updated.
        CommonTasks.writeLogMsg(Level.INFO, "Writing log records to fill binary log repository.");
        long loopsPerFullRepository = (201 * 1024 * 1024) / 200;
        logger.info("writing " + nf.format(loopsPerFullRepository) + " log loops to produce " + 201
                    + " MB of data.");

        logger.info("Writing FINE Level Trace entries: ");
        CommonTasks.createLogEntries(server, loggerName, "Sample log record for the test case " + name.getMethodName() + ".", Level.FINE,
                                     (int) loopsPerFullRepository, CommonTasks.TRACE, 0);

        CommonTasks.writeLogMsg(Level.INFO, "Verifying the repository size .");
        binaryLogDir = server.getFileFromLibertyServerRoot("logs/logdata");
        binaryTraceDir = server.getFileFromLibertyServerRoot("logs/tracedata");
        long binaryLogSize = getSizeOfBinaryLogs(binaryLogDir);
        long binaryTraceSize = getSizeOfBinaryLogs(binaryTraceDir);

        logger.info("The current size of BinaryLog files in " + binaryLogDir.getAbsolutePath() + " is " + nf.format(binaryLogSize));
        assertTrue("BinaryLog Repository size should be less than 50 MB",
                   binaryLogSize < (MAX_DEFAULT_PURGE_SIZE * 1024 * 1024));
        logger.info("The current size of BinaryTrace files in " + binaryTraceDir.getAbsolutePath() + " is " + nf.format(binaryTraceSize));
        assertTrue("BinaryTrace Repository size should be less than 201 MB",
                   binaryTraceSize < (201 * 1024 * 1024));

        ;

        // Re-enable console.log
        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HpelLogDirectoryChange_1.xml"));

        //remove bootstrap
        CommonTasks.removeBootstrapProperty(server, "com.ibm.hpel.trace.purgeMaxSize");
        server.restartServer();
    }

    /**
     * Set Bootstrap Trace = 91 and run the quick log and check the size of the repository.
     * It should not exceed 91 MB for tracedata and 50 MB for logdata
     **/
    @Mode(TestMode.FULL)
    @Test
    public void testPurgeMaxSize_9() throws Exception {
        RemoteFile binaryLogDir = null;
        RemoteFile binaryTraceDir = null;
        NumberFormat nf = NumberFormat.getInstance();

        CommonTasks.addBootstrapProperty(server, "com.ibm.hpel.trace.purgeMaxSize", "91");//
        server.restartServer();

        // write enough records to new log repository updated.
        CommonTasks.writeLogMsg(Level.INFO, "Writing log records to fill binary log repository.");
        long loopsPerFullRepository = (91 * 1024 * 1024) / 200;
        logger.info("writting " + nf.format(loopsPerFullRepository) + " log loops to produce " + 91
                    + " MB of data.");

        logger.info("Writing FINE Level Trace entries: ");
        CommonTasks.createLogEntries(server, loggerName, "Sample log record for the test case " + name.getMethodName() + ".", Level.FINE,
                                     (int) loopsPerFullRepository, CommonTasks.TRACE, 0);

        CommonTasks.writeLogMsg(Level.INFO, "Verifying the repository size .");
        binaryLogDir = server.getFileFromLibertyServerRoot("logs/logdata");
        binaryTraceDir = server.getFileFromLibertyServerRoot("logs/tracedata");
        long binaryLogSize = getSizeOfBinaryLogs(binaryLogDir);
        long binaryTraceSize = getSizeOfBinaryLogs(binaryTraceDir);

        logger.info("The current size of BinaryLog files in " + binaryLogDir.getAbsolutePath() + " is " + nf.format(binaryLogSize));
        assertTrue("BinaryLog Repository size should be less than 50 MB",
                   binaryLogSize < (MAX_DEFAULT_PURGE_SIZE * 1024 * 1024));
        logger.info("The current size of BinaryTrace files in " + binaryTraceDir.getAbsolutePath() + " is " + nf.format(binaryTraceSize));
        assertTrue("BinaryTrace Repository size should be less than 91 MB",
                   binaryTraceSize < (91 * 1024 * 1024));

        ;

        // Re-enable console.log
        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HpelLogDirectoryChange_1.xml"));

        //remove bootstrap
        CommonTasks.removeBootstrapProperty(server, "com.ibm.hpel.trace.purgeMaxSize");
        server.restartServer();
    }

    /**
     * Test Server startServer without any settings -> during run time, add purgeMaxSize for log to 100, stopServer server, remove purgeMaxSize attribute, startServer server -->
     * expect output:
     * trace=50,
     * log=50
     **/

    @Mode(TestMode.FULL)
    @Test
    public void testPurgeMaxSize_10() throws Exception {
        RemoteFile binaryLogDir = null;
        RemoteFile binaryTraceDir = null;
        NumberFormat nf = NumberFormat.getInstance();

        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HPELPurgeMaxSizeTest_4.xml"));
        server.stopServer();

        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HPELPurgeMaxSizeTest_5.xml"));
        server.startServer();

        // write enough records to new log repository updated.
        CommonTasks.writeLogMsg(Level.INFO, "Writing log records to fill binary log repository.");
        long loopsPerFullRepository = (MAX_DEFAULT_PURGE_SIZE * 1024 * 1024) / 200;
        logger.info("writting " + nf.format(loopsPerFullRepository) + " log loops to produce " + MAX_DEFAULT_PURGE_SIZE
                    + " MB of data.");

        logger.info("Writing INFO Level Log entries: ");
        CommonTasks.createLogEntries(server, loggerName, "Sample log record for the test case " + name.getMethodName() + ".", Level.INFO,
                                     (int) loopsPerFullRepository, CommonTasks.LOGS, 0);

        CommonTasks.writeLogMsg(Level.INFO, "Verifying the repository size ");
        binaryLogDir = server.getFileFromLibertyServerRoot("logs/logdata");
        binaryTraceDir = server.getFileFromLibertyServerRoot("logs/tracedata");

        long binaryLogSize = getSizeOfBinaryLogs(binaryLogDir);
        long binaryTraceSize = getSizeOfBinaryLogs(binaryTraceDir);

        logger.info("The current size of BinaryLog files in " + binaryLogDir.getAbsolutePath() + " is " + nf.format(binaryLogSize));
        assertTrue("BinaryLog Repository size should be less than 50 MB ",
                   binaryLogSize < (MAX_DEFAULT_PURGE_SIZE * 1024 * 1024));
        logger.info("The current size of BinaryTrace files in " + binaryTraceDir.getAbsolutePath() + " is " + nf.format(binaryTraceSize));
        assertTrue("Binarytrace Repository size should be less than 50 MB ",
                   binaryTraceSize < (MAX_DEFAULT_PURGE_SIZE * 1024 * 1024));

    }

    /**
     * Returns the total size of log files in the given directory
     *
     * @throws Exception
     **/
    private long getSizeOfBinaryLogs(RemoteFile dirToCheck) throws Exception {

        long totalBinaryLogRepositorySize = 0;
        RemoteFile[] allBinaryLogFiles = dirToCheck.list(true);
        for (RemoteFile i : allBinaryLogFiles) {
            totalBinaryLogRepositorySize += i.length();
//            }
        }
        return totalBinaryLogRepositorySize;
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Restore values we saw before changing them in setUp()
        CommonTasks.writeLogMsg(Level.INFO, "Resetting configuration to pre test values.");
        if (backup != null && backup.exists()) {
            server.getServerConfigurationFile().copyFromSource(backup);
        }
        // call the super
    }

    /**
     * Determine if we should or should not execute this test. Returns true if the test should NOT be ran.
     **/
    public static boolean skipTest() {
        // Test does not do any good on z/OS since TextLog is for Controller only - so we can't generate logs to fill up
        // TextLog repository. This may need to be revisited if we implement TextLog for servant.
        try {
            return server.getMachine().getOperatingSystem().equals(OperatingSystem.ZOS);
        } catch (Exception e) {
            CommonTasks.writeLogMsg(Level.SEVERE, "Unable to determine if we are on z/OS or not. Not skipping test");
            e.printStackTrace(System.err);
        }
        return false;
    }

}