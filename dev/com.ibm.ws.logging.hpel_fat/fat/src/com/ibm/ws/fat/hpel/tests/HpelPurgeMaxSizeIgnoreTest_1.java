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
 * Test case for defect 97932 : HPEL ignores purgeMaxSize
 * Test scenario
 * 1. Bootstrap = 91 , server.xml= 201 for Trace Trace = 201, Log = 50
 * 2. No Config specified Trace = 50, Log = 50
 * 3. Bootstrap = 91 for Trace Trace = 91 and Log = 50
 * 4. Server.xml= 55 for Log Trace = 50 and Log = 55
 * 5. Server.xml= 1024 for Trace ( Used 200 for trace instead of 1024 ) Trace = 1024 and Log = 50
 * 6. Server start without any settings -> during run time, add purgeMaxSize for log to 100 Trace = 50 and Log = 100
 * 7. Server start with log settings as 100 -> during run time, remove the attribute Trace = 50 and Log = 50
 * 8. Server start with log settings as 100 -> during run time, remove entire element (binaryLog) Trace = 50 and Log = 50
 * 9. Server start with log settings as 100 -> during run time, remove entire element (logging) Trace = 50 and Log = 50
 * 
 */

public class HpelPurgeMaxSizeIgnoreTest_1 extends VerboseTestCase {

    private final static String loggerName = HpelLogDirectoryChangeTest.class.getName();
    private final static Logger logger = Logger.getLogger(loggerName);
    private final static int MAX_DEFAULT_PURGE_SIZE = 50;

    private ApplicationServer appServ = null;

    RemoteFile backup = null;

    public HpelPurgeMaxSizeIgnoreTest_1(String name) {
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
//        backup = new RemoteFile(appServ.getBackend().getMachine(), new File(appServ.getBackend().getServerRoot(), "server-backup.xml").getPath());
//        if (!backup.exists()) {
//            backup.copyFromSource(appServ.getBackend().getServerConfigurationFile());
//        }
        appServ.getBackend().updateServerConfiguration(new File(appServ.getBackend().pathToAutoFVTTestFiles, "server-HpelLogDirectoryChange_1.xml"));
        if (!appServ.getBackend().isStarted()) {
            appServ.start();
        }

        this.logStepCompleted();

    }

    /**
     * Test No config specified. Run quick log and check the size of repository. Both Tracedata and Logdata should not exceed 50 MB.
     **/

    public void testPurgeMaxSize_1() throws Exception {
        RemoteFile binaryLogDir = null;
        RemoteFile binaryTraceDir = null;
        NumberFormat nf = NumberFormat.getInstance();

        this.logStep("Setting server configuration to default..");
        appServ.getBackend().updateServerConfiguration(new File(appServ.getBackend().pathToAutoFVTTestFiles, "server-HpelLogDirectoryChange_1.xml"));
        this.logStepCompleted();

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
                   binaryLogSize < (50 * 1024 * 1024));
        logger.info("The current size of BinaryTrace files in " + binaryTraceDir.getAbsolutePath() + " is " + nf.format(binaryTraceSize));
        assertTrue("Binarytrace Repository size should be less than 50 MB ",
                   binaryTraceSize < (50 * 1024 * 1024));

        this.logVerificationPassed();

    }

    /**
     * Test In Server.xml set log = 55. Run quick log and check the size of repository.
     * Trace data should not exceed more than 50 MB and Log data should not exceed more than 55 MB.
     **/

    public void testPurgeMaxSize_2() throws Exception {
        RemoteFile binaryLogDir = null;
        RemoteFile binaryTraceDir = null;
        NumberFormat nf = NumberFormat.getInstance();

        appServ.getBackend().updateServerConfiguration(new File(appServ.getBackend().pathToAutoFVTTestFiles, "server-HPELPurgeMaxSizeTest_1.xml"));
        appServ.stop();
        appServ.start();;

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
        assertTrue("BinaryLog Repository size should be less than 55 MB ",
                   binaryLogSize < (55 * 1024 * 1024));
        logger.info("The current size of BinaryTrace files in " + binaryTraceDir.getAbsolutePath() + " is " + nf.format(binaryTraceSize));
        assertTrue("BinaryTrace Repository size should be less than 50 MB ",
                   binaryTraceSize < (50 * 1024 * 1024));

        this.logVerificationPassed();

    }

    /**
     * Test In server.xml set Trace = 200MB. Start the server and run quick log and check the repository size.
     * Trace data size should not exceed more than 200 MB and log data size should not exceed more than 50 MB.
     **/
    public void testPurgeMaxSize_3() throws Exception {
        RemoteFile binaryLogDir = null;
        RemoteFile binaryTraceDir = null;
        NumberFormat nf = NumberFormat.getInstance();

        appServ.getBackend().updateServerConfiguration(new File(appServ.getBackend().pathToAutoFVTTestFiles, "server-HPELPurgeMaxSizeTest_2.xml"));
        appServ.stop();
        appServ.start();

        // write enough records to new log repository updated.
        this.logStep("Writting log records to fill binary log repository.");
        long loopsPerFullRepository = (200 * 1024 * 1024) / 200;
        logger.info("writting " + nf.format(loopsPerFullRepository) + " log loops to produce " + 200
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
                   binaryLogSize < (50 * 1024 * 1024));
        logger.info("The current size of BinaryTrace files in " + binaryTraceDir.getAbsolutePath() + " is " + nf.format(binaryTraceSize));
        assertTrue("Binarytrace Repository size should be less than 200 MB ",
                   binaryTraceSize < (200 * 1024 * 1024));

        this.logVerificationPassed();

    }

    /**
     * Server start without any setting -> during runtime add purgeMaxSize for log to 100 and run the quick log.
     * Check the size of repository, it should not exceed 100 MB for log and 50 MB for trace.
     */

    public void testPurgeMaxSize_4() throws Exception {
        RemoteFile binaryLogDir = null;
        RemoteFile binaryTraceDir = null;
        NumberFormat nf = NumberFormat.getInstance();

        appServ.getBackend().updateServerConfiguration(new File(appServ.getBackend().pathToAutoFVTTestFiles, "server-HPELPurgeMaxSizeTest_3.xml"));
        appServ.stop();
        appServ.start();

        appServ.getBackend().updateServerConfiguration(new File(appServ.getBackend().pathToAutoFVTTestFiles, "server-HPELPurgeMaxSizeTest_4.xml"));

        // write enough records to new log repository updated.
        this.logStep("Writting log records to fill binary log repository.");
        long loopsPerFullRepository = (100 * 1024 * 1024) / 200;
        logger.info("writting " + nf.format(loopsPerFullRepository) + " log loops to produce " + 100
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
        assertTrue("BinaryLog Repository size should be less than 100 MB ",
                   binaryLogSize < (100 * 1024 * 1024));
        logger.info("The current size of BinaryTrace files in " + binaryTraceDir.getAbsolutePath() + " is " + nf.format(binaryTraceSize));
        assertTrue("Binarytrace Repository size should be less than 50 MB ",
                   binaryTraceSize < (50 * 1024 * 1024));

        this.logVerificationPassed();

    }

    /**
     * Server start with log settings as 100 -> during run time, remove the attribute "purgeMaxSize" from <binaryLog> element.
     * Run the quick log and check the size of the repository, it should not exceed 50 MB for tracedata and logdata.
     **/
    public void testPurgeMaxSize_5() throws Exception {
        RemoteFile binaryLogDir = null;
        RemoteFile binaryTraceDir = null;
        NumberFormat nf = NumberFormat.getInstance();

        appServ.getBackend().updateServerConfiguration(new File(appServ.getBackend().pathToAutoFVTTestFiles, "server-HPELPurgeMaxSizeTest_4.xml"));
        appServ.stop();
        appServ.start();

        appServ.getBackend().updateServerConfiguration(new File(appServ.getBackend().pathToAutoFVTTestFiles, "server-HPELPurgeMaxSizeTest_5.xml"));

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
                   binaryLogSize < (50 * 1024 * 1024));
        logger.info("The current size of BinaryTrace files in " + binaryTraceDir.getAbsolutePath() + " is " + nf.format(binaryTraceSize));
        assertTrue("Binarytrace Repository size should be less than 50 MB ",
                   binaryTraceSize < (50 * 1024 * 1024));

        this.logVerificationPassed();
    }

    /**
     * Server start with log settings as 100 -> during run time, remove entire element (binaryLog) from server.xml
     * run the quick log and check the size of the repository it should not exceed 50 MB for both logdata and tracedata.
     **/
    public void testPurgeMaxSize_6() throws Exception {
        RemoteFile binaryLogDir = null;
        RemoteFile binaryTraceDir = null;
        NumberFormat nf = NumberFormat.getInstance();

        appServ.getBackend().updateServerConfiguration(new File(appServ.getBackend().pathToAutoFVTTestFiles, "server-HPELPurgeMaxSizeTest_4.xml"));
        appServ.stop();
        appServ.start();

        appServ.getBackend().updateServerConfiguration(new File(appServ.getBackend().pathToAutoFVTTestFiles, "server-HPELPurgeMaxSizeTest_6.xml"));

        // write enough records to new log repository updated.
        this.logStep("Writting log records to fill binary log repository.");
        long loopsPerFullRepository = (50 * 1024 * 1024) / 50;
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
                   binaryLogSize < (50 * 1024 * 1024));
        logger.info("The current size of BinaryTrace files in " + binaryTraceDir.getAbsolutePath() + " is " + nf.format(binaryTraceSize));
        assertTrue("Binarytrace Repository size should be less than 50 MB ",
                   binaryTraceSize < (50 * 1024 * 1024));

        this.logVerificationPassed();
    }

    /**
     * Server start with log settings as 100 -> during run time, remove entire element (logging)
     * Run the quick log and check the size of the repository, it should not exceed 50 MB for both logdata and tracedata.
     **/
    public void testPurgeMaxSize_7() throws Exception {
        RemoteFile binaryLogDir = null;
        RemoteFile binaryTraceDir = null;
        NumberFormat nf = NumberFormat.getInstance();

        appServ.getBackend().updateServerConfiguration(new File(appServ.getBackend().pathToAutoFVTTestFiles, "server-HPELPurgeMaxSizeTest_4.xml"));
        appServ.stop();
        appServ.start();

        appServ.getBackend().updateServerConfiguration(new File(appServ.getBackend().pathToAutoFVTTestFiles, "server-HPELPurgeMaxSizeTest_7.xml"));

        // write enough records to new log repository updated.
        this.logStep("Writting log records to fill binary log repository.");
        long loopsPerFullRepository = (50 * 1024 * 1024) / 200;
        logger.info("writting " + nf.format(loopsPerFullRepository) + " log loops to produce " + 50
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
                   binaryLogSize < (50 * 1024 * 1024));
        logger.info("The current size of BinaryTrace files in " + binaryTraceDir.getAbsolutePath() + " is " + nf.format(binaryTraceSize));
        assertTrue("Binarytrace Repository size should be less than 50 MB ",
                   binaryTraceSize < (50 * 1024 * 1024));

        this.logVerificationPassed();
    }

    /**
     * Set Bootstrap Trace = 91 , server.xml Trace = 201, run the quick log and check the size of the repository.
     * It should not exceed 201 MB for tracedata and 50 MB for logdata.
     **/
    public void testPurgeMaxSize_8() throws Exception {
        RemoteFile binaryLogDir = null;
        RemoteFile binaryTraceDir = null;
        NumberFormat nf = NumberFormat.getInstance();

        CommonTasks.addBootstrapProperty(appServ, "com.ibm.hpel.trace.purgeMaxSize", "91");
        appServ.stop();
        appServ.start();

        this.logStep("Configuring server for test case.");
        appServ.getBackend().updateServerConfiguration(new File(appServ.getBackend().pathToAutoFVTTestFiles, "server-HpelLogElementDelete_1.xml"));
        this.logStepCompleted();

        // write enough records to new log repository updated.
        this.logStep("Writting log records to fill binary log repository.");
        long loopsPerFullRepository = (201 * 1024 * 1024) / 200;
        logger.info("writting " + nf.format(loopsPerFullRepository) + " log loops to produce " + 201
                    + " MB of data.");
        CommonTasks.createLogEntries(appServ, loggerName, "Sample log record for the test case " + this.getName() + ".", null,
                                     (int) loopsPerFullRepository, CommonTasks.LOGS_TRACE, 0);

        this.logStepCompleted();

        this.logVerificationPoint("Verifying the repository size .");
        binaryLogDir = appServ.getBackend().getFileFromLibertyServerRoot("logs/logdata");
        binaryTraceDir = appServ.getBackend().getFileFromLibertyServerRoot("logs/tracedata");
        long binaryLogSize = getSizeOfBinaryLogs(binaryLogDir);
        long binaryTraceSize = getSizeOfBinaryLogs(binaryTraceDir);

        logger.info("The current size of BinaryLog files in " + binaryLogDir.getAbsolutePath() + " is " + nf.format(binaryLogSize));
        assertTrue("BinaryLog Repository size should be less than 50 MB",
                   binaryLogSize > (45 * 1024 * 1024));
        logger.info("The current size of BinaryTrace files in " + binaryTraceDir.getAbsolutePath() + " is " + nf.format(binaryTraceSize));
        assertTrue("BinaryTrace Repository size should be less than 201 MB",
                   binaryTraceSize > (190 * 1024 * 1024));

        this.logVerificationPassed();
    }

    /**
     * Set Bootstrap Trace = 91 and run the quick log and check the size of the repository.
     * It should not exceed 91 MB for tracedata and 50 MB for logdata
     **/
    public void testPurgeMaxSize_9() throws Exception {
        RemoteFile binaryLogDir = null;
        RemoteFile binaryTraceDir = null;
        NumberFormat nf = NumberFormat.getInstance();

        CommonTasks.addBootstrapProperty(appServ, "com.ibm.hpel.trace.purgeMaxSize", "91");
        appServ.stop();
        appServ.start();

        // write enough records to new log repository updated.
        this.logStep("Writting log records to fill binary log repository.");
        long loopsPerFullRepository = (91 * 1024 * 1024) / 200;
        logger.info("writting " + nf.format(loopsPerFullRepository) + " log loops to produce " + 91
                    + " MB of data.");
        CommonTasks.createLogEntries(appServ, loggerName, "Sample log record for the test case " + this.getName() + ".", null,
                                     (int) loopsPerFullRepository, CommonTasks.LOGS_TRACE, 0);

        this.logStepCompleted();

        this.logVerificationPoint("Verifying the repository size .");
        binaryLogDir = appServ.getBackend().getFileFromLibertyServerRoot("logs/logdata");
        binaryTraceDir = appServ.getBackend().getFileFromLibertyServerRoot("logs/tracedata");
        long binaryLogSize = getSizeOfBinaryLogs(binaryLogDir);
        long binaryTraceSize = getSizeOfBinaryLogs(binaryTraceDir);

        logger.info("The current size of BinaryLog files in " + binaryLogDir.getAbsolutePath() + " is " + nf.format(binaryLogSize));
        assertTrue("BinaryLog Repository size should be less than 50 MB",
                   binaryLogSize > (45 * 1024 * 1024));
        logger.info("The current size of BinaryTrace files in " + binaryTraceDir.getAbsolutePath() + " is " + nf.format(binaryTraceSize));
        assertTrue("BinaryTrace Repository size should be less than 91 MB",
                   binaryTraceSize > (85 * 1024 * 1024));

        this.logVerificationPassed();
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
//            }
        }
        return totalBinaryLogRepositorySize;
    }

    @Override
    public void tearDown() throws Exception {
        // Restore values we saw before changing them in setUp()
//        this.logStep("Resetting configuration to pre test values.");
//        if (backup != null && backup.exists()) {
//            appServ.getBackend().getServerConfigurationFile().copyFromSource(backup);
//        }
//        this.logStepCompleted();

        // call the super
        super.tearDown();
    }

}