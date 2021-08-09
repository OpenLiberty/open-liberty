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
 * 91930        8.5.5     13/06/2013    sumam    Test case for defect 91930.
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
 * Test case for defect 91930: HPEL log ignores purgeMaxSize after dataDirectory is changed.
 * While changing the path of logDirectory to new location it should use the should follow the same purgeMaxSize property.
 * Testing scenario: Start the server with logDirectory as logs and purgeMaxSize = 29 → during runtime change the logDirectory as logX and check for the purgeMaxSize property for
 * the new repository, it should not exceed 29 MB.
 *
 */
@RunWith(FATRunner.class)
public class HPELLogDirectoryPurgeMaxSizeTest {
    @Server("HpelServer")
    public static LibertyServer server;
    private final static String loggerName = HpelLogDirectoryChangeTest.class.getName();
    private final static Logger logger = Logger.getLogger(loggerName);
    private final static int MAX_DEFAULT_PURGE_SIZE = 30;

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

        CommonTasks.writeLogMsg(Level.INFO, "Configuring server for test case.");
        backup = new RemoteFile(server.getMachine(), new File(server.getServerRoot(), "server-backup.xml").getPath());
        if (!backup.exists()) {
            backup.copyFromSource(server.getServerConfigurationFile());
        }
        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HpelDirChangePurgeMaxTest_1.xml"));
        if (!server.isStarted()) {
            server.startServer();
        }

    }

    /**
     * Test that logs generated are retaining the purgeMaxSize value for new location.
     * Start the server with logDirectory as logs and purgeMaxSize as 29 MB, during runtime change the logDirectory to logx and run quick log
     * the new repository should not exceed size more than 29 MB.
     **/
    @Test
    public void testLogDirectoryChange() throws Exception {
        RemoteFile binaryLogDir = null;
        NumberFormat nf = NumberFormat.getInstance();

        CommonTasks.writeLogMsg(Level.INFO, "Configuring server for test case.");
        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HpelDirChangePurgeMaxTest_2.xml"));

        // write enough records to new log repository updated.
        CommonTasks.writeLogMsg(Level.INFO, "Writting log records to fill binary log repository.");
        long loopsPerFullRepository = (MAX_DEFAULT_PURGE_SIZE * 1024 * 1024) / 200;
        logger.info("writting " + nf.format(loopsPerFullRepository) + " log loops to produce " + MAX_DEFAULT_PURGE_SIZE
                    + " MB of data.");
        CommonTasks.createLogEntries(server, loggerName, "Sample log record for the test case " + name.getMethodName() + ".", null,
                                     (int) loopsPerFullRepository, CommonTasks.LOGS, 0);

        CommonTasks.writeLogMsg(Level.INFO, "Verifying the repository size for new location is same as old");
        binaryLogDir = server.getFileFromLibertyServerRoot("logx/logdata");
        long binaryLogSize = getSizeOfBinaryLogs(binaryLogDir);
        logger.info("The current size of BinaryLog files in " + binaryLogDir.getAbsolutePath() + " is " + nf.format(binaryLogSize));
        assertTrue("BinaryLog Repository size should be less than 29 MB ",
                   binaryLogSize < (29 * 1024 * 1024) && binaryLogSize > ((MAX_DEFAULT_PURGE_SIZE - 5) * 1024 * 1024));

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
    }

}