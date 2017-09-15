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
 * Test case for defect 91946 : HPEL configuration value doesn't revert to default when cleared from server.xml
 * While deleting the <logging> element purgeMaxSize property is not set to default value.
 * Test scenario
 * 1. Bootstrap = 91 , server.xml= 201 for Trace remove the entire <logging> element Trace = 91 and Log = 50
 * 2. server.xml= 201 for Trace remove the entire <logging> element Trace = 50 and Log = 50
 */

public class HpelLoggingElementDeleteTest extends VerboseTestCase {

    private final static String loggerName = HpelLoggingElementDeleteTest.class.getName();
    private final static Logger logger = Logger.getLogger(loggerName);
    private final static int MAX_DEFAULT_PURGE_SIZE = 100;

    private ApplicationServer appServ = null;

    RemoteFile backup = null;

    public HpelLoggingElementDeleteTest(String name) {
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
        appServ.getBackend().updateServerConfiguration(new File(appServ.getBackend().pathToAutoFVTTestFiles, "server-HpelLogElementDelete_1.xml"));
        if (!appServ.getBackend().isStarted()) {
            appServ.start();
        }

        this.logStepCompleted();

    }

    /**
     * Test 1. Set server.xml Trace = 201, during runtime remove the entire <logging> element,
     * run quick log and check for the repository size, both logdata and tracedata should not exceed more than 50 MB.
     **/

    public void testLoggingElementDelete_1() throws Exception {
        RemoteFile binaryLogDir = null;
        RemoteFile binaryTraceDir = null;

        NumberFormat nf = NumberFormat.getInstance();

        this.logStep("Configuring server for test case.");
        appServ.getBackend().updateServerConfiguration(new File(appServ.getBackend().pathToAutoFVTTestFiles, "server-HpelLogElementDelete_2.xml"));
        this.logStepCompleted();

        // write enough records to new log repository updated.
        this.logStep("Writting log records to fill binary log repository.");
        long loopsPerFullRepository = (MAX_DEFAULT_PURGE_SIZE * 1024 * 1024) / 200;
        logger.info("writting " + nf.format(loopsPerFullRepository) + " log loops to produce " + MAX_DEFAULT_PURGE_SIZE
                    + " MB of data.");
        CommonTasks.createLogEntries(appServ, loggerName, "Sample log record for the test case " + this.getName() + ".", null,
                                     (int) loopsPerFullRepository, CommonTasks.LOGS_TRACE, 0);

        this.logStepCompleted();

        this.logVerificationPoint("Verifying the repository size after deleting the logging element.");
        binaryLogDir = appServ.getBackend().getFileFromLibertyServerRoot("logs/logdata");
        long binaryLogSize = getSizeOfBinaryLogs(binaryLogDir);
        binaryTraceDir = appServ.getBackend().getFileFromLibertyServerRoot("logs/tracedata");
        long binaryTraceSize = getSizeOfBinaryLogs(binaryTraceDir);

        logger.info("The current size of BinaryLog files in " + binaryLogDir.getAbsolutePath() + " is " + nf.format(binaryLogSize));
        assertTrue("BinaryLog Repository size should be less than 50 MB ",
                   binaryLogSize > (45 * 1024 * 1024) && binaryLogSize < (50 * 1024 * 1024));
        logger.info("The current size of BinaryTrace files in " + binaryTraceDir.getAbsolutePath() + " is " + nf.format(binaryTraceSize));
        assertTrue("BinaryTrace Repository size should be less than 50 MB ",
                   binaryTraceSize > (45 * 1024 * 1024) && binaryTraceSize < (50 * 1024 * 1024));

        this.logVerificationPassed();

    }

    /**
     * Test 1. Bootstrap Trace = 91 ,in server.xml Trace = 201, during runtime remove the entire <logging> element
     * and run quick log, check the size of the repository it should not exceed more than 91 MB for trace data and 50 MB for log data.
     **/
    public void testLoggingElementDelete_2() throws Exception {
        RemoteFile binaryLogDir = null;
        RemoteFile binaryTraceDir = null;
        NumberFormat nf = NumberFormat.getInstance();

        CommonTasks.addBootstrapProperty(appServ, "com.ibm.hpel.trace.purgeMaxSize", "90");
        appServ.stop();
        appServ.start();

        this.logStep("Configuring server for test case.");
        appServ.getBackend().updateServerConfiguration(new File(appServ.getBackend().pathToAutoFVTTestFiles, "server-HpelLogElementDelete_1.xml"));
        this.logStepCompleted();

        appServ.stop();
        appServ.start();

        this.logStep("Configuring server for test case.");
        appServ.getBackend().updateServerConfiguration(new File(appServ.getBackend().pathToAutoFVTTestFiles, "server-HpelLogElementDelete_2.xml"));
        this.logStepCompleted();

        // write enough records to new log repository updated.
        this.logStep("Writting log records to fill binary log repository.");
        long loopsPerFullRepository = (MAX_DEFAULT_PURGE_SIZE * 1024 * 1024) / 200;
        logger.info("writting " + nf.format(loopsPerFullRepository) + " log loops to produce " + MAX_DEFAULT_PURGE_SIZE
                    + " MB of data.");
        CommonTasks.createLogEntries(appServ, loggerName, "Sample log record for the test case " + this.getName() + ".", null,
                                     (int) loopsPerFullRepository, CommonTasks.LOGS_TRACE, 0);

        this.logStepCompleted();

        this.logVerificationPoint("Verifying the repository size after deleting the logging element .");
        binaryLogDir = appServ.getBackend().getFileFromLibertyServerRoot("logs/logdata");
        long binaryLogSize = getSizeOfBinaryLogs(binaryLogDir);
        binaryTraceDir = appServ.getBackend().getFileFromLibertyServerRoot("logs/tracedata");
        long binaryTraceSize = getSizeOfBinaryLogs(binaryTraceDir);

        logger.info("The current size of BinaryLog files in " + binaryLogDir.getAbsolutePath() + " is " + nf.format(binaryLogSize));
        assertTrue("BinaryLog Repository size should be less than 50 MB ",
                   binaryLogSize > (45 * 1024 * 1024) && binaryLogSize < (50 * 1024 * 1024));
        logger.info("The current size of BinaryTrace files in " + binaryTraceDir.getAbsolutePath() + " is " + nf.format(binaryTraceSize));
        assertTrue("BinaryTrace Repository size should be less than 91 MB ",
                   binaryTraceSize > (85 * 1024 * 1024) && binaryTraceSize < (91 * 1024 * 1024));

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