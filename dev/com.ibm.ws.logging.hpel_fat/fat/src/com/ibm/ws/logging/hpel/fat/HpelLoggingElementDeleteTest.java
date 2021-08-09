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
 * 91946         8.5.5     13/06/2013    sumam     Test case for defect 91946.
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
import componenttest.topology.impl.LibertyServer;

/**
 * Test case for defect 91946 : HPEL configuration value doesn't revert to default when cleared from server.xml
 * While deleting the <logging> element purgeMaxSize property is not set to default value.
 * Test scenario
 * 1. Bootstrap = 91 , server.xml= 201 for Trace remove the entire <logging> element Trace = 91 and Log = 50
 * 2. server.xml= 201 for Trace remove the entire <logging> element Trace = 50 and Log = 50
 */
@RunWith(FATRunner.class)
public class HpelLoggingElementDeleteTest {
    @Server("HpelServer")
    public static LibertyServer server;
    private final static String loggerName = HpelLoggingElementDeleteTest.class.getName();
    private final static Logger logger = Logger.getLogger(loggerName);
    private final static int MAX_DEFAULT_PURGE_SIZE = 100;

    static RemoteFile backup = null;
    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "LogFat", "com.ibm.ws.logging.hpel");
        ShrinkHelper.defaultDropinApp(server, "HpelFat", "com.ibm.ws.logging.hpel.servlet");
        // Confirm HPEL is enabled
        if (!CommonTasks.isHpelEnabled(server)) {
            // HPEL is not enabled.
            CommonTasks.writeLogMsg(Level.INFO, "HPEL is not enabled on " + server.getServerName() + ", attempting to enable.");
            CommonTasks.setHpelEnabled(server, true);
            // RestartServer now to complete switching to HPEL
            server.stopServer();
            server.startServer();

        }
        // Setting the bootstrap with trace specification to get the trace logs.

        CommonTasks.addBootstrapProperty(server, "com.ibm.ws.logging.trace.specification", "*=fine=enabled");
        server.stopServer();
        server.startServer();

        CommonTasks.writeLogMsg(Level.INFO, "Configuring server for test case.");
        backup = new RemoteFile(server.getMachine(), new File(server.getServerRoot(), "server-backup.xml").getPath());
        if (!backup.exists()) {
            backup.copyFromSource(server.getServerConfigurationFile());
        }
        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HpelLogElementDelete_1.xml"));
        if (!server.isStarted()) {
            server.startServer();
        }

    }

    /**
     * Test 1. Set server.xml Trace = 201, during runtime remove the entire <logging> element,
     * run quick log and check for the repository size, both logdata and tracedata should not exceed more than 50 MB.
     **/
    @Test
    public void testLoggingElementDelete_1() throws Exception {
        RemoteFile binaryLogDir = null;
        RemoteFile binaryTraceDir = null;

        NumberFormat nf = NumberFormat.getInstance();

        CommonTasks.writeLogMsg(Level.INFO, "Configuring server for test case.");
        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HpelLogElementDelete_2.xml"));

        // write enough records to new log repository updated.
        CommonTasks.writeLogMsg(Level.INFO, "Writting log records to fill binary log repository.");
        long loopsPerFullRepository = (MAX_DEFAULT_PURGE_SIZE * 1024 * 1024) / 200;
        logger.info("writting " + nf.format(loopsPerFullRepository) + " log loops to produce " + MAX_DEFAULT_PURGE_SIZE
                    + " MB of data.");
        logger.info("Writing INFO Level Log entries: ");
        CommonTasks.createLogEntries(server, loggerName, "Sample log record for the test case " + name.getMethodName() + ".", Level.INFO,
                                     (int) loopsPerFullRepository, CommonTasks.LOGS, 0);
        logger.info("Writing FINE Level Trace entries: ");
        CommonTasks.createLogEntries(server, loggerName, "Sample log record for the test case " + name.getMethodName() + ".", Level.FINE,
                                     (int) loopsPerFullRepository, CommonTasks.TRACE, 0);

        CommonTasks.writeLogMsg(Level.INFO, "Verifying the repository size after deleting the logging element.");
        binaryLogDir = server.getFileFromLibertyServerRoot("logs/logdata");
        long binaryLogSize = getSizeOfBinaryLogs(binaryLogDir);
        binaryTraceDir = server.getFileFromLibertyServerRoot("logs/tracedata");
        long binaryTraceSize = getSizeOfBinaryLogs(binaryTraceDir);

        logger.info("The current size of BinaryLog files in " + binaryLogDir.getAbsolutePath() + " is " + nf.format(binaryLogSize));
        assertTrue("BinaryLog Repository size should be less than 50 MB ",
                   binaryLogSize > (45 * 1024 * 1024) && binaryLogSize < (50 * 1024 * 1024));
        logger.info("The current size of BinaryTrace files in " + binaryTraceDir.getAbsolutePath() + " is " + nf.format(binaryTraceSize));
        assertTrue("BinaryTrace Repository size should be less than 50 MB ",
                   binaryTraceSize > (45 * 1024 * 1024) && binaryTraceSize < (50 * 1024 * 1024));

    }

    /**
     * Test 1. Bootstrap Trace = 91 ,in server.xml Trace = 201, during runtime remove the entire <logging> element
     * and run quick log, check the size of the repository it should not exceed more than 91 MB for trace data and 50 MB for log data.
     **/
    @Test
    public void testLoggingElementDelete_2() throws Exception {
        RemoteFile binaryLogDir = null;
        RemoteFile binaryTraceDir = null;
        NumberFormat nf = NumberFormat.getInstance();

        CommonTasks.addBootstrapProperty(server, "com.ibm.hpel.trace.purgeMaxSize", "90");
        server.stopServer();
        server.startServer();

        CommonTasks.writeLogMsg(Level.INFO, "Configuring server for test case.");
        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HpelLogElementDelete_1.xml"));

        server.stopServer();
        server.startServer();

        CommonTasks.writeLogMsg(Level.INFO, "Configuring server for test case.");
        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HpelLogElementDelete_2.xml"));

        // write enough records to new log repository updated.
        CommonTasks.writeLogMsg(Level.INFO, "Writting log records to fill binary log repository.");
        long loopsPerFullRepository = (MAX_DEFAULT_PURGE_SIZE * 1024 * 1024) / 200;
        logger.info("writting " + nf.format(loopsPerFullRepository) + " log loops to produce " + MAX_DEFAULT_PURGE_SIZE
                    + " MB of data.");
        CommonTasks.createLogEntries(server, loggerName, "Sample log record for the test case " + name.getMethodName() + ".", null,
                                     (int) loopsPerFullRepository, CommonTasks.LOGS_TRACE, 0);

        CommonTasks.writeLogMsg(Level.INFO, "Verifying the repository size after deleting the logging element .");
        binaryLogDir = server.getFileFromLibertyServerRoot("logs/logdata");
        long binaryLogSize = getSizeOfBinaryLogs(binaryLogDir);
        binaryTraceDir = server.getFileFromLibertyServerRoot("logs/tracedata");
        long binaryTraceSize = getSizeOfBinaryLogs(binaryTraceDir);

        logger.info("The current size of BinaryLog files in " + binaryLogDir.getAbsolutePath() + " is " + nf.format(binaryLogSize));
        assertTrue("BinaryLog Repository size should be less than 50 MB ",
                   binaryLogSize > (45 * 1024 * 1024) && binaryLogSize < (50 * 1024 * 1024));
        logger.info("The current size of BinaryTrace files in " + binaryTraceDir.getAbsolutePath() + " is " + nf.format(binaryTraceSize));
        assertTrue("BinaryTrace Repository size should be less than 91 MB ",
                   binaryTraceSize > (85 * 1024 * 1024) && binaryTraceSize < (91 * 1024 * 1024));

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
    }

}