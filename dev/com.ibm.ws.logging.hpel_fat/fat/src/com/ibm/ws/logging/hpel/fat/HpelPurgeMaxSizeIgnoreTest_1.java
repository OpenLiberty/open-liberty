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
 * 1. Bootstrap = 91 , server.xml= 201 for Trace Trace = 201, Log = 50
 * 2. No Config specified Trace = 50, Log = 50
 * 3. Bootstrap = 91 for Trace Trace = 91 and Log = 50
 * 4. Server.xml= 55 for Log Trace = 50 and Log = 55
 * 5. Server.xml= 1024 for Trace ( Used 200 for trace instead of 1024 ) Trace = 1024 and Log = 50
 * 6. Server startServer without any settings -> during run time, add purgeMaxSize for log to 100 Trace = 50 and Log = 100
 * 7. Server startServer with log settings as 100 -> during run time, remove the attribute Trace = 50 and Log = 50
 * 8. Server startServer with log settings as 100 -> during run time, remove entire element (binaryLog) Trace = 50 and Log = 50
 * 9. Server startServer with log settings as 100 -> during run time, remove entire element (logging) Trace = 50 and Log = 50
 *
 */

@RunWith(FATRunner.class)
public class HpelPurgeMaxSizeIgnoreTest_1 {

    private final static String loggerName = HpelLogDirectoryChangeTest.class.getName();
    private final static Logger logger = Logger.getLogger(loggerName);
    private final static int MAX_DEFAULT_PURGE_SIZE = 50;
    private final static int MAX_WAIT_TIME_FOR_CONFIG_UPDATE = 2500;

    @Server("HpelServer")
    public static LibertyServer server;

    RemoteFile backup = null;
    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        // Confirm HPEL is enabled
        ShrinkHelper.defaultDropinApp(server, "LogFat", "com.ibm.ws.logging.hpel");
        ShrinkHelper.defaultDropinApp(server, "HpelFat", "com.ibm.ws.logging.hpel.servlet");

        // Setting the bootstrap with trace specification to get the trace logs.
        CommonTasks.addBootstrapProperty(server, "com.ibm.ws.logging.trace.specification", "*=fine=enabled");
        server.stopServer();

        CommonTasks.writeLogMsg(Level.INFO, "Configuring server for test case.");

        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HpelLogDirectoryChange_1.xml"));
        if (!server.isStarted()) {
            server.startServer();
        }

    }

    /**
     * Test No config specified. Run quick log and check the size of repository. Both Tracedata and Logdata should not exceed 50 MB.
     **/

    @Mode(TestMode.FULL)
    @Test
    public void testPurgeMaxSize_1() throws Exception {
        RemoteFile binaryLogDir = null;
        RemoteFile binaryTraceDir = null;
        NumberFormat nf = NumberFormat.getInstance();

        CommonTasks.writeLogMsg(Level.INFO, "Setting server configuration to default..");
        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HpelLogDirectoryChange_1.xml"));

        // write enough records to new log repository updated.
        CommonTasks.writeLogMsg(Level.INFO, "Writing log records to fill binary log repository.");
        long loopsPerFullRepository = (MAX_DEFAULT_PURGE_SIZE * 1024 * 1024) / 200;
        logger.info("Writing " + nf.format(loopsPerFullRepository) + " log loops to produce " + MAX_DEFAULT_PURGE_SIZE
                    + " MB of data.");
        logger.info("Writing INFO Level Log entries: ");
        CommonTasks.createLogEntries(server, loggerName, "Sample log record for the test case " + name.getMethodName() + ".", Level.INFO,
                                     (int) loopsPerFullRepository, CommonTasks.LOGS, 0);
        logger.info("Writing FINE Level Trace entries: ");
        CommonTasks.createLogEntries(server, loggerName, "Sample log record for the test case " + name.getMethodName() + ".", Level.FINE,
                                     (int) loopsPerFullRepository, CommonTasks.TRACE, 0);

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
     * Test In Server.xml set log = 55. Run quick log and check the size of repository.
     * Trace data should not exceed more than 50 MB and Log data should not exceed more than 55 MB.
     **/

    @Mode(TestMode.FULL)
    @Test
    public void testPurgeMaxSize_2() throws Exception {
        RemoteFile binaryLogDir = null;
        RemoteFile binaryTraceDir = null;
        NumberFormat nf = NumberFormat.getInstance();

        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HPELPurgeMaxSizeTest_1.xml"));
        // Short pause to ensure that the configuration is updated.
        Thread.sleep(MAX_WAIT_TIME_FOR_CONFIG_UPDATE);
        //server.stopServer();
        //server.startServer();

        // write enough records to new log repository updated.
        CommonTasks.writeLogMsg(Level.INFO, "Writing log records to fill binary log repository.");
        long loopsPerFullRepository = (MAX_DEFAULT_PURGE_SIZE * 1024 * 1024) / 200;
        logger.info("writing " + nf.format(loopsPerFullRepository) + " log loops to produce " + MAX_DEFAULT_PURGE_SIZE
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
        assertTrue("BinaryLog Repository size should be less than 55 MB ",
                   binaryLogSize < (55 * 1024 * 1024));
        logger.info("The current size of BinaryTrace files in " + binaryTraceDir.getAbsolutePath() + " is " + nf.format(binaryTraceSize));
        assertTrue("BinaryTrace Repository size should be less than 50 MB ",
                   binaryTraceSize < (MAX_DEFAULT_PURGE_SIZE * 1024 * 1024));

        ;

    }

    /**
     * Test In server.xml set Trace = 200MB. startServer the server and run quick log and check the repository size.
     * Trace data size should not exceed more than 200 MB and log data size should not exceed more than 50 MB.
     **/
    @Mode(TestMode.FULL)
    @Test
    public void testPurgeMaxSize_3() throws Exception {
        RemoteFile binaryLogDir = null;
        RemoteFile binaryTraceDir = null;
        NumberFormat nf = NumberFormat.getInstance();

        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HPELPurgeMaxSizeTest_2.xml"));
        // Short pause to ensure that the configuration is updated.
        Thread.sleep(MAX_WAIT_TIME_FOR_CONFIG_UPDATE);

        // write enough records to new log repository updated.
        CommonTasks.writeLogMsg(Level.INFO, "Writing log records to fill binary log repository.");
        long loopsPerFullRepository = (200 * 1024 * 1024) / 200;
        logger.info("writing " + nf.format(loopsPerFullRepository) + " log loops to produce " + 200
                    + " MB of data.");

        logger.info("Writing FINE Level Trace entries: ");
        CommonTasks.createLogEntries(server, loggerName, "Sample log record for the test case " + name.getMethodName() + ".", Level.FINE,
                                     (int) loopsPerFullRepository, CommonTasks.TRACE, 0);

        CommonTasks.writeLogMsg(Level.INFO, "Verifying the repository size ");
        binaryLogDir = server.getFileFromLibertyServerRoot("logs/logdata");
        binaryTraceDir = server.getFileFromLibertyServerRoot("logs/tracedata");

        long binaryLogSize = getSizeOfBinaryLogs(binaryLogDir);
        long binaryTraceSize = getSizeOfBinaryLogs(binaryTraceDir);

        logger.info("The current size of BinaryLog files in " + binaryLogDir.getAbsolutePath() + " is " + nf.format(binaryLogSize));
        assertTrue("BinaryLog Repository size should be less than 50 MB ",
                   binaryLogSize < (MAX_DEFAULT_PURGE_SIZE * 1024 * 1024));
        logger.info("The current size of BinaryTrace files in " + binaryTraceDir.getAbsolutePath() + " is " + nf.format(binaryTraceSize));
        assertTrue("Binarytrace Repository size should be less than 200 MB ",
                   binaryTraceSize < (200 * 1024 * 1024));

        ;

    }

    /**
     * Server startServer without any setting -> during runtime add purgeMaxSize for log to 100 and run the quick log.
     * Check the size of repository, it should not exceed 100 MB for log and 50 MB for trace.
     */

    @Mode(TestMode.FULL)
    @Test
    public void testPurgeMaxSize_4() throws Exception {
        RemoteFile binaryLogDir = null;
        RemoteFile binaryTraceDir = null;
        NumberFormat nf = NumberFormat.getInstance();

        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HPELPurgeMaxSizeTest_3.xml"));
        server.restartServer();

        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HPELPurgeMaxSizeTest_4.xml"));

        // write enough records to new log repository updated.
        CommonTasks.writeLogMsg(Level.INFO, "Writing log records to fill binary log repository.");
        long loopsPerFullRepository = (100 * 1024 * 1024) / 200;
        logger.info("writing " + nf.format(loopsPerFullRepository) + " log loops to produce " + 100
                    + " MB of data.");

        logger.info("Writing INFO Level Log entries: ");
        CommonTasks.createLogEntries(server, loggerName, "Sample log record for the test case " + server.getServerName() + ".", Level.INFO,
                                     (int) loopsPerFullRepository, CommonTasks.LOGS, 0);

        CommonTasks.writeLogMsg(Level.INFO, "Verifying the repository size ");
        binaryLogDir = server.getFileFromLibertyServerRoot("logs/logdata");
        binaryTraceDir = server.getFileFromLibertyServerRoot("logs/tracedata");

        long binaryLogSize = getSizeOfBinaryLogs(binaryLogDir);
        long binaryTraceSize = getSizeOfBinaryLogs(binaryTraceDir);

        logger.info("The current size of BinaryLog files in " + binaryLogDir.getAbsolutePath() + " is " + nf.format(binaryLogSize));
        assertTrue("BinaryLog Repository size should be less than 100 MB ",
                   binaryLogSize < (100 * 1024 * 1024));
        logger.info("The current size of BinaryTrace files in " + binaryTraceDir.getAbsolutePath() + " is " + nf.format(binaryTraceSize));
        assertTrue("Binarytrace Repository size should be less than 50 MB ",
                   binaryTraceSize < (MAX_DEFAULT_PURGE_SIZE * 1024 * 1024));

        ;

    }

    /**
     * Server startServer with log settings as 100 -> during run time, remove the attribute "purgeMaxSize" from <binaryLog> element.
     * Run the quick log and check the size of the repository, it should not exceed 50 MB for tracedata and logdata.
     **/
    @Mode(TestMode.FULL)
    @Test
    public void testPurgeMaxSize_5() throws Exception {
        RemoteFile binaryLogDir = null;
        RemoteFile binaryTraceDir = null;
        NumberFormat nf = NumberFormat.getInstance();

        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HPELPurgeMaxSizeTest_4.xml"));
        server.restartServer();

        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HPELPurgeMaxSizeTest_5.xml"));

        // write enough records to new log repository updated.
        CommonTasks.writeLogMsg(Level.INFO, "Writing log records to fill binary log repository.");
        long loopsPerFullRepository = (MAX_DEFAULT_PURGE_SIZE * 1024 * 1024) / 200;
        logger.info("writting " + nf.format(loopsPerFullRepository) + " log loops to produce " + MAX_DEFAULT_PURGE_SIZE
                    + " MB of data.");

        logger.info("Writing INFO Level Log entries: ");
        CommonTasks.createLogEntries(server, loggerName, "Sample log record for the test case " + server.getServerName() + ".", Level.INFO,
                                     (int) loopsPerFullRepository, CommonTasks.LOGS, 0);
        logger.info("Writing FINE Level Trace entries: ");
        CommonTasks.createLogEntries(server, loggerName, "Sample log record for the test case " + server.getServerName() + ".", Level.FINE,
                                     (int) loopsPerFullRepository, CommonTasks.TRACE, 0);

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

        ;
    }

    /**
     * Server startServer with log settings as 100 -> during run time, remove entire element (binaryLog) from server.xml
     * run the quick log and check the size of the repository it should not exceed 50 MB for both logdata and tracedata.
     **/
    @Mode(TestMode.FULL)
    @Test
    public void testPurgeMaxSize_6() throws Exception {
        RemoteFile binaryLogDir = null;
        RemoteFile binaryTraceDir = null;
        NumberFormat nf = NumberFormat.getInstance();

        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HPELPurgeMaxSizeTest_4.xml"));
        server.restartServer();

        CommonTasks.writeLogMsg(Level.INFO, "Configuring server for test case.");
        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HPELPurgeMaxSizeTest_6.xml"));
        // Short pause to ensure that the configuration is updated.
        Thread.sleep(MAX_WAIT_TIME_FOR_CONFIG_UPDATE);

        // write enough records to new log repository updated.
        CommonTasks.writeLogMsg(Level.INFO, "Writing log records to fill binary log repository.");
        long loopsPerFullRepository = (50 * 1024 * 1024) / 50;
        logger.info("writting " + nf.format(loopsPerFullRepository) + " log loops to produce " + MAX_DEFAULT_PURGE_SIZE
                    + " MB of data.");

        logger.info("Writing INFO Level Log entries: ");
        CommonTasks.createLogEntries(server, loggerName, "Sample log record for the test case " + name.getMethodName() + ".", Level.INFO,
                                     (int) loopsPerFullRepository, CommonTasks.LOGS, 0);
        logger.info("Writing FINE Level Trace entries: ");
        CommonTasks.createLogEntries(server, loggerName, "Sample log record for the test case " + name.getMethodName() + ".", Level.FINE,
                                     (int) loopsPerFullRepository, CommonTasks.TRACE, 0);

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

        ;
    }

    /**
     * Server startServer with log settings as 100 -> during run time, remove entire element (logging)
     * Run the quick log and check the size of the repository, it should not exceed 50 MB for both logdata and tracedata.
     **/
    @Mode(TestMode.FULL)
    @Test
    public void testPurgeMaxSize_7() throws Exception {
        RemoteFile binaryLogDir = null;
        RemoteFile binaryTraceDir = null;
        NumberFormat nf = NumberFormat.getInstance();

        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HPELPurgeMaxSizeTest_4.xml"));
        server.restartServer();

        CommonTasks.writeLogMsg(Level.INFO, "Configuring server for test case.");
        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HPELPurgeMaxSizeTest_7.xml"));
        // Short pause to ensure that the configuration is updated.
        Thread.sleep(MAX_WAIT_TIME_FOR_CONFIG_UPDATE);

        // write enough records to new log repository updated.
        CommonTasks.writeLogMsg(Level.INFO, "Writing log records to fill binary log repository.");
        long loopsPerFullRepository = (50 * 1024 * 1024) / 200;
        logger.info("writing " + nf.format(loopsPerFullRepository) + " log loops to produce " + 50
                    + " MB of data.");

        logger.info("Writing INFO Level Log entries: ");
        CommonTasks.createLogEntries(server, loggerName, "Sample log record for the test case " + name.getMethodName() + ".", Level.INFO,
                                     (int) loopsPerFullRepository, CommonTasks.LOGS, 0);
        logger.info("Writing FINE Level Trace entries: ");
        CommonTasks.createLogEntries(server, loggerName, "Sample log record for the test case " + name.getMethodName() + ".", Level.FINE,
                                     (int) loopsPerFullRepository, CommonTasks.TRACE, 0);

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

        ;
        server.stopServer();
    }

    /**
     * Returns the total size of Log files in the given directory
     *
     * @throws Exception
     **/
    private long getSizeOfBinaryLogs(RemoteFile dirToCheck) throws Exception {

        long totalBinaryLogRepositorySize = 0;
        RemoteFile[] allBinaryLogFiles = dirToCheck.list(true);
        for (RemoteFile i : allBinaryLogFiles) {
            totalBinaryLogRepositorySize += i.length();
        }
        return totalBinaryLogRepositorySize;
    }

    @AfterClass
    public static void tearDown() throws Exception {

        // call the super
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

}