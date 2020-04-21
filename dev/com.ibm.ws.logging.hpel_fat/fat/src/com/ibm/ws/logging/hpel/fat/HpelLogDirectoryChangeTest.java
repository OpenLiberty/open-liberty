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
 * 98216        8.5.5     13/06/2013    sumam    Test case for defect 98216.
 *
 */

package com.ibm.ws.logging.hpel.fat;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Test case for defect 98216 : HPEL doesn't change location of logdata/tracedata when logDirectory changes.
 * While changing the logDirectory attribute of <logging> element, the new logs were going inside the old directory.
 * Test Scenario -> startServer the server with logDirectory as logs → during runtime change the logDirectory to logX and run quick log, new logs should be generated under the logx
 * repository.
 *
 */
@RunWith(FATRunner.class)
public class HpelLogDirectoryChangeTest {

    private final static String loggerName = HpelLogDirectoryChangeTest.class.getName();
    private final static Logger logger = Logger.getLogger(loggerName);
    private final static int MAX_DEFAULT_PURGE_SIZE = 30;

    @Server("HpelServer")
    public static LibertyServer server;

    static RemoteFile backup = null;

    @BeforeClass
    public static void setUp() throws Exception {
        // Confirm HPEL is enabled
        ShrinkHelper.defaultDropinApp(server, "LogFat", "com.ibm.ws.logging.hpel");
        ShrinkHelper.defaultDropinApp(server, "HpelFat", "com.ibm.ws.logging.hpel.servlet");
        if (!CommonTasks.isHpelEnabled(server)) {
            // HPEL is not enabled.
            CommonTasks.writeLogMsg(Level.INFO, "HPEL is not enabled on " + server.getServerName() + ", attempting to enable.");
            CommonTasks.setHpelEnabled(server, true);
            // RestartServer now to complete switching to HPEL
            server.stopServer();
            server.startServer();

        }
        CommonTasks.writeLogMsg(Level.INFO, "Configuring server for test case.");
        backup = new RemoteFile(server.getMachine(), new File(server.getServerRoot(), "server-backup.xml").getPath());
        if (!backup.exists()) {
            backup.copyFromSource(server.getServerConfigurationFile());
        }
        // Setting the log directory of logs
        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HpelLogDirectoryChange_1.xml"));
        if (!server.isStarted()) {
            server.startServer();
        }

    }

    /**
     * Testing the logDirectory attribute by changing to new location and running the quick logs.
     * startServer the server with logDirectory as logs, during runtime change the value of logDirectory to logx, run quick logs
     * and check the location where logs are getting stored.
     **/
    @Test
    public void testLogDirectoryChange() throws Exception {
        RemoteFile binaryLogDir = null;
        NumberFormat nf = NumberFormat.getInstance();

        CommonTasks.writeLogMsg(Level.INFO, "Configuring server for test case by setting the logDirectory to logx");
        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HpelLogDirectoryChange_2.xml"));

        // write enough records to new log repository updated.
        CommonTasks.writeLogMsg(Level.INFO, "Writting log records to fill binary log repository.");
        long loopsPerFullRepository = (MAX_DEFAULT_PURGE_SIZE * 1024 * 1024) / 200;
        logger.info("writting " + nf.format(loopsPerFullRepository) + " log loops to produce " + MAX_DEFAULT_PURGE_SIZE
                    + " MB of data.");
        CommonTasks.createLogEntries(server, loggerName, "Sample log record for the test case " + "testLogDirectoryChange" + ".", null,
                                     (int) loopsPerFullRepository, CommonTasks.LOGS, 0);

        CommonTasks.writeLogMsg(Level.INFO, "Verifying the repository used for log is new location (logx).");
        binaryLogDir = server.getFileFromLibertyServerRoot("logx/logdata");
        long binaryLogSize = getSizeOfBinaryLogs(binaryLogDir);
        logger.info("The current size of BinaryLog files in " + binaryLogDir.getAbsolutePath() + " is " + nf.format(binaryLogSize));
        assertTrue("BinaryLog Repository should be the new location logx ",
                   binaryLogSize > ((MAX_DEFAULT_PURGE_SIZE - 5) * 1024 * 1024) && binaryLogSize < (50 * 1024 * 1024));

    }

    /**
     * Returns the total size of log files in the given directory
     *
     * @throws Exception
     **/
    private long getSizeOfBinaryLogs(RemoteFile dirToCheck) throws Exception {
        long totalgRepositorySize = 0;
        RemoteFile[] allLogFiles = dirToCheck.list(true);
        for (RemoteFile i : allLogFiles) {
            totalgRepositorySize += i.length();
        }
        return totalgRepositorySize;
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

}