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
 * 98078        8.5.5     13/06/2013    sumam     Test case for defect 98078.
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
 * Test case for defect 98078 : HPEL purgeMinTime default setting causes all backup log/trace files to be deleted
 * Test scenario
 * 1. Start the server with purgeMaxSize as 0 for Trace and Log, run quick log Log directory size should be unlimited
 * 2. Change the size of purgeMaxSize to 10 for Log and Trace Log directory should be purged to 10MB
 * 3. Run quick log again Log directory should remain within 10 MB
 */
@RunWith(FATRunner.class)
public class HpelPurgeMaxSizeBackupFileTest {

    private final static String loggerName = HpelLogDirectoryChangeTest.class.getName();
    private final static Logger logger = Logger.getLogger(loggerName);
    private final static int MAX_DEFAULT_PURGE_SIZE = 50;
    @Server("HpelServer")
    public static LibertyServer server;
    static RemoteFile backup = null;
    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public void setUp() throws Exception {
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
        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HPELPurgeMinTimeTest_1.xml"));
        if (!server.isStarted()) {
            server.startServer();
        }

    }

    /**
     * Test that HPEL's TextLog size based retention policy works. Both within a single server instance and across
     * server restartServers.
     **/
    @Test
    public void testPurgeMinTime() throws Exception {
        RemoteFile binaryLogDir = null;
        RemoteFile binaryTraceDir = null;
        NumberFormat nf = NumberFormat.getInstance();

        // write enough records to log repository updated.
        CommonTasks.writeLogMsg(Level.INFO, "Writing log records to fill binary log repository.");
        long loopsPerFullRepository = (MAX_DEFAULT_PURGE_SIZE * 1024 * 1024) / 200;
        logger.info("writing " + nf.format(loopsPerFullRepository) + " log loops to produce " + MAX_DEFAULT_PURGE_SIZE
                    + " MB of data.");
        logger.info("Writing INFO Level Log entries: ");
        CommonTasks.createLogEntries(server, loggerName, "Sample log record for the test case " + name.getMethodName() + ".", Level.INFO,
                                     (int) loopsPerFullRepository, CommonTasks.LOGS, 0);
        logger.info("Writing FINE Level Trace entries: ");
        CommonTasks.createLogEntries(server, loggerName, "Sample log record for the test case " + name.getMethodName() + ".", Level.FINE,
                                     (int) loopsPerFullRepository, CommonTasks.TRACE, 0);

        CommonTasks.writeLogMsg(Level.INFO, "Verifying the repository size.");
        binaryLogDir = server.getFileFromLibertyServerRoot("logs/logdata");
        long binaryLogSize = getSizeOfBinaryLogs(binaryLogDir);
        binaryTraceDir = server.getFileFromLibertyServerRoot("logs/tracedata");
        long binaryTraceSize = getSizeOfBinaryLogs(binaryTraceDir);

        logger.info("The current size of BinaryLog files in " + binaryLogDir.getAbsolutePath() + " is " + nf.format(binaryLogSize));
        assertTrue("BinaryLog Repository size should have logs created",
                   binaryLogSize > ((MAX_DEFAULT_PURGE_SIZE - 2) * 1024 * 1024));
        logger.info("The current size of BinaryTrace files in " + binaryTraceDir.getAbsolutePath() + " is " + nf.format(binaryTraceSize));
        assertTrue("BinaryTrace Repository size should have logs created",
                   binaryTraceSize > ((MAX_DEFAULT_PURGE_SIZE - 2) * 1024 * 1024));

        CommonTasks.writeLogMsg(Level.INFO, "Configuring server for test case.");
        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HPELPurgeMinTimeTest_2.xml"));

        CommonTasks.writeLogMsg(Level.INFO, "Verifying the repository used for log is new location.");
        binaryLogSize = getSizeOfBinaryLogs(binaryLogDir);
        binaryTraceSize = getSizeOfBinaryLogs(binaryTraceDir);

        logger.info("The current size of BinaryLog files in " + binaryLogDir.getAbsolutePath() + " is " + nf.format(binaryLogSize));
        assertTrue("BinaryLog Repository size should have logs created",
                   binaryLogSize > (5 * 1024 * 1024) && binaryLogSize < (10 * 1024 * 1024));
        logger.info("The current size of BinaryTrace files in " + binaryTraceDir.getAbsolutePath() + " is " + nf.format(binaryTraceSize));
        assertTrue("BinaryTrace Repository size should have logs created",
                   binaryTraceSize > (5 * 1024 * 1024) && binaryTraceSize < (10 * 1024 * 1024));

        CommonTasks.writeLogMsg(Level.INFO, "Writing log records to fill binary log repository.");
        loopsPerFullRepository = (MAX_DEFAULT_PURGE_SIZE * 1024 * 1024) / 600;
        logger.info("writting " + nf.format(loopsPerFullRepository) + " log loops to produce " + MAX_DEFAULT_PURGE_SIZE
                    + " MB of data.");
        logger.info("Writing INFO Level Log entries: ");
        CommonTasks.createLogEntries(server, loggerName, "Sample log record for the test case " + name.getMethodName() + ".", Level.INFO,
                                     (int) loopsPerFullRepository, CommonTasks.LOGS, 0);
        logger.info("Writing FINE Level Trace entries: ");
        CommonTasks.createLogEntries(server, loggerName, "Sample log record for the test case " + name.getMethodName() + ".", Level.FINE,
                                     (int) loopsPerFullRepository, CommonTasks.TRACE, 0);

        CommonTasks.writeLogMsg(Level.INFO, "Verifying the repository size for new logs generated");
        binaryLogSize = getSizeOfBinaryLogs(binaryLogDir);
        binaryTraceSize = getSizeOfBinaryLogs(binaryTraceDir);

        logger.info("The current size of BinaryLog files in " + binaryLogDir.getAbsolutePath() + " is " + nf.format(binaryLogSize));
        assertTrue("BinaryLog Repository size should have logs created",
                   binaryLogSize > (5 * 1024 * 1024) && binaryLogSize < (10 * 1024 * 1024));
        logger.info("The current size of BinaryTrace files in " + binaryTraceDir.getAbsolutePath() + " is " + nf.format(binaryTraceSize));
        assertTrue("BinaryTrace Repository size should have logs created",
                   binaryTraceSize > (5 * 1024 * 1024) && binaryTraceSize < (10 * 1024 * 1024));

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