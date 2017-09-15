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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.simplicity.ApplicationServer;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.ws.fat.VerboseTestCase;
import com.ibm.ws.fat.hpel.setup.HpelSetup;
import com.ibm.ws.fat.ras.util.CommonTasks;

/**
 * Test case for defect 97932 : HPEL ignores purgeMaxSize
 * Test scenario
 * 10. Server start without any settings -> during run time, add purgeMaxSize for log to 100, stop server, remove purgeMaxSize attribute, start server --> expect output: trace=50,
 * log=50
 * 
 */

public class HpelPurgeMaxSizeIgnoreTest_2 extends VerboseTestCase {

    private final static String loggerName = HpelLogDirectoryChangeTest.class.getName();
    private final static Logger logger = Logger.getLogger(loggerName);
    private final static int MAX_DEFAULT_PURGE_SIZE = 50;

    private ApplicationServer appServ = null;

    RemoteFile backup = null;

    public HpelPurgeMaxSizeIgnoreTest_2(String name) {
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

    }

    /**
     * Test Server start without any settings -> during run time, add purgeMaxSize for log to 100, stop server, remove purgeMaxSize attribute, start server --> expect output:
     * trace=50,
     * log=50
     **/
    public void testPurgeMaxSize_10() throws Exception {
        RemoteFile binaryLogDir = null;
        RemoteFile binaryTraceDir = null;
        NumberFormat nf = NumberFormat.getInstance();

        appServ.getBackend().updateServerConfiguration(new File(appServ.getBackend().pathToAutoFVTTestFiles, "server-HPELPurgeMaxSizeTest_4.xml"));
        appServ.stop();

        appServ.getBackend().updateServerConfiguration(new File(appServ.getBackend().pathToAutoFVTTestFiles, "server-HPELPurgeMaxSizeTest_5.xml"));
        appServ.start();

        // write enough records to new log repository updated.
        this.logStep("Writting log records to fill binary log repository.");
        long loopsPerFullRepository = (MAX_DEFAULT_PURGE_SIZE * 1024 * 1024) / 200;
        logger.info("writting " + nf.format(loopsPerFullRepository) + " log loops to produce " + MAX_DEFAULT_PURGE_SIZE
                    + " MB of data.");
        CommonTasks.createLogEntries(appServ, loggerName, "Sample log record for the test case " + this.getName() + ".", null,
                                     (int) loopsPerFullRepository, CommonTasks.LOGS_TRACE, 0);

        this.logStepCompleted();

        this.logVerificationPoint("Verifying the repository size ");
        binaryLogDir = appServ.getBackend().getFileFromLibertyServerRoot("logs/logdata");
        binaryTraceDir = appServ.getBackend().getFileFromLibertyServerRoot("logs/tracedata");

        long binaryLogSize = getSizeOfBinaryLogs(binaryLogDir);
        long binaryTraceSize = getSizeOfBinaryLogs(binaryTraceDir);

        logger.info("The current size of BinaryLog files in " + binaryLogDir.getAbsolutePath() + " is " + nf.format(binaryLogSize));
        assertTrue("BinaryLog Repository size should be less than 50 MB ",
                   binaryLogSize < (MAX_DEFAULT_PURGE_SIZE * 1024 * 1024));
        logger.info("The current size of BinaryTrace files in " + binaryTraceDir.getAbsolutePath() + " is " + nf.format(binaryTraceSize));
        assertTrue("Binarytrace Repository size should be less than 50 MB ",
                   binaryTraceSize < (MAX_DEFAULT_PURGE_SIZE * 1024 * 1024));

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

    /**
     * Determine if we should or should not execute this test. Returns true if the test should NOT be ran.
     * **/
    @Override
    public boolean skipTest() {
        // Test does not do any good on z/OS since TextLog is for Controller only - so we can't generate logs to fill up
        // TextLog repository. This may need to be revisited if we implement TextLog for servant.
        try {
            return HpelSetup.getNodeUnderTest().getMachine().getOperatingSystem().equals(OperatingSystem.ZOS);
        } catch (Exception e) {
            CommonTasks.writeLogMsg(Level.SEVERE, "Unable to determine if we are on z/OS or not. Not skipping test");
            e.printStackTrace(System.err);
        }
        return false;
    }

}