/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.fat.hpel.tests;

import java.io.File;
import java.text.NumberFormat;
import java.util.logging.Logger;

import com.ibm.websphere.simplicity.ApplicationServer;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.ws.fat.VerboseTestCase;
import com.ibm.ws.fat.hpel.setup.HpelSetup;
import com.ibm.ws.fat.ras.util.CommonTasks;

/**
 * Test case for defect 98078 : HPEL purgeMinTime default setting causes all backup log/trace files to be deleted
 * Test scenario
 * 1. Start the server with purgeMaxSize as 0 for Trace and Log, run quick log Log directory size should be unlimited
 * 2. Change the size of purgeMaxSize to 10 for Log and Trace Log directory should be purged to 10MB
 * 3. Run quick log again Log directory should remain within 10 MB
 */

public class HpelPurgeMaxSizeBackupFileTest extends VerboseTestCase {

    private final static String loggerName = HpelLogDirectoryChangeTest.class.getName();
    private final static Logger logger = Logger.getLogger(loggerName);
    private final static int MAX_DEFAULT_PURGE_SIZE = 50;

    private ApplicationServer appServ = null;

    RemoteFile backup = null;

    public HpelPurgeMaxSizeBackupFileTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        // Call super.SetUp() cause we still want it's setup as well
        super.setUp();
        appServ = HpelSetup.getServerUnderTest();
        // Confirm HPEL is enabled
        if (!CommonTasks.isHpelEnabled(appServ)) {
            // HPEL is not enabled.
            this.logStep("HPEL is not enabled on " + appServ.getName() + ", attempting to enable.");
            CommonTasks.setHpelEnabled(appServ, true);
            // Restart now to complete switching to HPEL
            appServ.stop();
            appServ.start();
            this.logStepCompleted();
        }

        // Setting the bootstrap with trace specification to get the trace logs.
        CommonTasks.addBootstrapProperty(appServ, "com.ibm.ws.logging.trace.specification", "*=fine=enabled");
        appServ.stop();
        appServ.start();

        this.logStep("Configuring server for test case.");
        backup = new RemoteFile(appServ.getBackend().getMachine(), new File(appServ.getBackend().getServerRoot(), "server-backup.xml").getPath());
        if (!backup.exists()) {
            backup.copyFromSource(appServ.getBackend().getServerConfigurationFile());
        }
        appServ.getBackend().updateServerConfiguration(new File(appServ.getBackend().pathToAutoFVTTestFiles, "server-HPELPurgeMinTimeTest_1.xml"));
        if (!appServ.getBackend().isStarted()) {
            appServ.start();
        }

        this.logStepCompleted();

    }

    /**
     * Test that HPEL's TextLog size based retention policy works. Both within a single server instance and across
     * server restarts.
     **/
    public void testPurgeMinTime() throws Exception {
        RemoteFile binaryLogDir = null;
        RemoteFile binaryTraceDir = null;
        NumberFormat nf = NumberFormat.getInstance();

        // write enough records to log repository updated.
        this.logStep("Writting log records to fill binary log repository.");
        long loopsPerFullRepository = (MAX_DEFAULT_PURGE_SIZE * 1024 * 1024) / 200;
        logger.info("writting " + nf.format(loopsPerFullRepository) + " log loops to produce " + MAX_DEFAULT_PURGE_SIZE
                    + " MB of data.");
        CommonTasks.createLogEntries(appServ, loggerName, "Sample log record for the test case " + this.getName() + ".", null,
                                     (int) loopsPerFullRepository, CommonTasks.LOGS_TRACE, 0);

        this.logStepCompleted();

        this.logVerificationPoint("Verifying the repository size.");
        binaryLogDir = appServ.getBackend().getFileFromLibertyServerRoot("logs/logdata");
        long binaryLogSize = getSizeOfBinaryLogs(binaryLogDir);
        binaryTraceDir = appServ.getBackend().getFileFromLibertyServerRoot("logs/tracedata");
        long binaryTraceSize = getSizeOfBinaryLogs(binaryTraceDir);

        logger.info("The current size of BinaryLog files in " + binaryLogDir.getAbsolutePath() + " is " + nf.format(binaryLogSize));
        assertTrue("BinaryLog Repository size should have logs created",
                   binaryLogSize > ((MAX_DEFAULT_PURGE_SIZE - 2) * 1024 * 1024));
        logger.info("The current size of BinaryTrace files in " + binaryTraceDir.getAbsolutePath() + " is " + nf.format(binaryTraceSize));
        assertTrue("BinaryTrace Repository size should have logs created",
                   binaryTraceSize > ((MAX_DEFAULT_PURGE_SIZE - 2) * 1024 * 1024));

        this.logVerificationPassed();

        this.logStep("Configuring server for test case.");
        appServ.getBackend().updateServerConfiguration(new File(appServ.getBackend().pathToAutoFVTTestFiles, "server-HPELPurgeMinTimeTest_2.xml"));
        this.logStepCompleted();

        this.logVerificationPoint("Verifying the repository used for log is new location.");
        binaryLogSize = getSizeOfBinaryLogs(binaryLogDir);
        binaryTraceSize = getSizeOfBinaryLogs(binaryTraceDir);

        logger.info("The current size of BinaryLog files in " + binaryLogDir.getAbsolutePath() + " is " + nf.format(binaryLogSize));
        assertTrue("BinaryLog Repository size should have logs created",
                   binaryLogSize > (5 * 1024 * 1024) && binaryLogSize < (10 * 1024 * 1024));
        logger.info("The current size of BinaryTrace files in " + binaryTraceDir.getAbsolutePath() + " is " + nf.format(binaryTraceSize));
        assertTrue("BinaryTrace Repository size should have logs created",
                   binaryTraceSize > (5 * 1024 * 1024) && binaryTraceSize < (10 * 1024 * 1024));

        this.logVerificationPassed();

        this.logStep("Writting log records to fill binary log repository.");
        loopsPerFullRepository = (MAX_DEFAULT_PURGE_SIZE * 1024 * 1024) / 600;
        logger.info("writting " + nf.format(loopsPerFullRepository) + " log loops to produce " + MAX_DEFAULT_PURGE_SIZE
                    + " MB of data.");
        CommonTasks.createLogEntries(appServ, loggerName, "Sample log record for the test case " + this.getName() + ".", null,
                                     (int) loopsPerFullRepository, CommonTasks.LOGS_TRACE, 0);

        this.logStepCompleted();

        this.logVerificationPoint("Verifying the repository size for new logs generated");
        binaryLogSize = getSizeOfBinaryLogs(binaryLogDir);
        binaryTraceSize = getSizeOfBinaryLogs(binaryTraceDir);

        logger.info("The current size of BinaryLog files in " + binaryLogDir.getAbsolutePath() + " is " + nf.format(binaryLogSize));
        assertTrue("BinaryLog Repository size should have logs created",
                   binaryLogSize > (5 * 1024 * 1024) && binaryLogSize < (10 * 1024 * 1024));
        logger.info("The current size of BinaryTrace files in " + binaryTraceDir.getAbsolutePath() + " is " + nf.format(binaryTraceSize));
        assertTrue("BinaryTrace Repository size should have logs created",
                   binaryTraceSize > (5 * 1024 * 1024) && binaryTraceSize < (10 * 1024 * 1024));

        this.logVerificationPassed();

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

    @Override
    public void tearDown() throws Exception {
        // Restore values we saw before changing them in setUp()
        this.logStep("Resetting configuration to pre test values.");
        if (backup != null && backup.exists()) {
            appServ.getBackend().getServerConfigurationFile().copyFromSource(backup);
        }
        this.logStepCompleted();

        // call the super
        super.tearDown();
    }

}