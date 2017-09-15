/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
//%Z% %I% %W% %G% %U% [%H% %T%]
/*
 * Change History:
 *
 * Reason    	 	Version  Date        User id     Description
 * ----------------------------------------------------------------------------
 * F017049-18796.1  8.0      1/08/2010   spaungam     Update test to support subdirectories
 * F000896.23216    8.0      06/10/2010  shighbar     Update to work on z/OS requires support for reading servants. Refactored initialize method into setup to reduce restarts which was impacting test on z/OS. 
 */
package com.ibm.ws.fat.hpel.tests;

import java.io.File;
import java.util.Date;

import com.ibm.websphere.logging.hpel.reader.RepositoryLogRecord;
import com.ibm.websphere.logging.hpel.reader.RepositoryReaderImpl;
import com.ibm.websphere.logging.hpel.reader.ServerInstanceLogRecordList;
import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.ws.fat.Props;
import com.ibm.ws.fat.VerboseTestCase;
import com.ibm.ws.fat.hpel.setup.HpelSetup;
import com.ibm.ws.fat.ras.util.CommonTasks;

/**
 * This test is about verifying that HPEL is accurately reporting the number of messages sent to it.
 * 
 * The test outline is as follows: 1: Create an HTTP client to the LogCreator JSP that logs messages (use the
 * CommonTasks object) 2: Enable tracing so a tracedata directory and repository is created 3: Call the HTTP client with
 * a known number of logging entries 4: Get the log records from the repository and count them 5: Do they match? Yes,
 * pass, No, fail
 * 
 */
public class VerifyRepositoryAccuracy extends VerboseTestCase {
    String localLogsRepositoryPath = null;
    String localTraceRepositoryPath = null;
    String uniqueLoggerID_Tag;
    String loggerName = "VRAccuracyLogger";
    String logMessage = "VRAccuracy Message for FAT testing";
    final String TRACE_SPECIFICATION = "VRAccuracyLogger*=all"; // trace spec needed for this test case.
    int logCount = 49;
    boolean loggerSetup = false;

    public VerifyRepositoryAccuracy(String name) {
        super(name);
    }

    /**
     * Configures instance resources to initialize this TestCase.
     * 
     * @throws Exception
     *             if a problem happens while configuring the test fixture.
     */
    @Override
    public void setUp() throws Exception {
        // Call super.SetUp() cause we still want it's setup as well
        super.setUp();

        // Confirm HPEL is enabled
        if (!CommonTasks.isHpelEnabled(HpelSetup.getServerUnderTest())) {
            // HPEL is not enabled.
            this.logStep("HPEL is not enabled on " + HpelSetup.getServerUnderTest().getName() + ", attempting to enable.");
            CommonTasks.setHpelEnabled(HpelSetup.getServerUnderTest(), true);
            // if HPEL was not enabled, make sure trace spec is not valid to ensure restart below.
            CommonTasks.setHpelTraceSpec(HpelSetup.getServerUnderTest(), null);
            this.logStepCompleted();
        }

        /*
         * Since we have multiple test methods in this test case, we will only restart if the spec is not already set to
         * what we need. This will avoid extra server restarts which adds to the bucket execution time.
         */
        this.logStep("Checking the HPEL trace spec for " + this.getName());
        if (!CommonTasks.getHpelTraceSpec(HpelSetup.getServerUnderTest()).equals(TRACE_SPECIFICATION)) {
            // The current spec is not what we need for this test - update spec and bounce server to take effect
            this.logStep("Updating the hpel trace spec to " + TRACE_SPECIFICATION + " as part of setup for " + this.getName());
            CommonTasks.setHpelTraceSpec(HpelSetup.getServerUnderTest(), TRACE_SPECIFICATION);

            // need to restart the application server now
            // Stop Server first.
            this.logStep("Bouncing server for new spec to take effect. Stopping application server");
            HpelSetup.getServerUnderTest().stop();
            this.logStepCompleted();

            Thread.sleep(10000); // stop operation blocks, but want short pause before restarting.

            // Start Server
            this.logStep("Restarting the application server");
            HpelSetup.getServerUnderTest().start();

            this.logStep("Checking the trace spec post app server restart: "
                         + CommonTasks.getHpelTraceSpec(HpelSetup.getServerUnderTest()));
            assertTrue("Failed assertion that HPEL trace specification is set to " + TRACE_SPECIFICATION, TRACE_SPECIFICATION
                            .equals(CommonTasks.getHpelTraceSpec(HpelSetup.getServerUnderTest())));
            this.logStepCompleted();

        }
    }

    private void setupLogger() {
        if (!loggerSetup) {
            // setup a new unique identifier for the logger name
            Date d = new Date();
            loggerName = loggerName + d.getTime();
            this.logStep("Creating a new logger named: " + loggerName);
            this.logStepCompleted();
            loggerSetup = true;
        }
    }

    /*
     * Tests that HPEL accurately reports the number of logs records entered. Assumes that HPEL is enabled on the active
     * server
     */
    public void testLogRecordCountAccuracyCheck() throws Exception {
        // Create the following log entries in the repository
        // Levels: All (pass null)
        // LoggerName: com.ibm.ws.fat.hpel.tests.VRAccuracy + uniqueLoggerID_Tag
        // Log Message: VRAccuracy Message for FAT testing
        // Iterations: 49 (just picked a random number)

        setupLogger();

        this.logStep("Creating the log entries");
        CommonTasks.createLogEntries(HpelSetup.getServerUnderTest(), loggerName, logMessage, null, logCount, CommonTasks.LOGS, -1);
        this.logStepCompleted();

        Thread.sleep(10 * 1000); // sleep for 10 seconds

        // Get the logs from the server to check what is in them
        this.logStep("Getting the message logs from the server");
        getLogsFromServer(CommonTasks.LOGS);
        this.logStepCompleted();

        // Go read the repository and see if it has what we are looking for
        RepositoryReaderImpl logRepository = new RepositoryReaderImpl(localLogsRepositoryPath);
        ServerInstanceLogRecordList CurrentProcessLogList = logRepository.getLogListForCurrentServerInstance();
        // record list is the list of records we will compare against. Assume it's the currentServerInstance list by
        // default.
        Iterable<RepositoryLogRecord> recordList = CurrentProcessLogList;

        /*
         * Note, if we are on z/OS the CurrentProcessLogList represents only the controller's logs. Our test log entries
         * will be written by servant though. Will setup a Merged repository to include all the logs from sub-process
         * instances.
         */
        //The problem is with how test counts records on zOS. HPEL allows server to have a parent process (controller) with a set of child processes (servants). 
        //For such servers result for a server instance has none empty result from getChildren() call. The test problem is that it assumes that logs recorded by an application on zOS can be found _only_ in children processes. 
        //On Liberty that is not the case since even on zOS server consists of a single process processing all the requests.

        //The problem can be solved in two ways. 1. Remove 'if zOS' condition since the test is maintained separately on Liberty. 
        //2. Adjust test to do merge between parent and children logs for all OSs. Following the first solution here.

        //if (HpelSetup.getNodeUnderTest().getMachine().getOperatingSystem().equals(OperatingSystem.ZOS)) {
        //this.logStep("Since this is z/OS - using a MergedRepository representing all the servant and adjunct sub-processes");
        //recordList = new MergedRepository(CurrentProcessLogList.getChildren().values());
        //}

        this.logStep("Counting log records by level");
        int severe = 0;
        int warning = 0;
        int info = 0;
        for (RepositoryLogRecord record : recordList) {
            // process a record
            if (record.getLoggerName().equals(loggerName)) {
                switch (record.getLevel().intValue()) {
                    case 1000: // SEVERE == 1000
                        severe++;
                        break;
                    case 900: // WARNING == 900
                        warning++;
                        break;
                    case 800: // INFO == 800
                        info++;
                        break;
                }
            }
        }
        this.logStepCompleted();

        // Verify the results
        this.logVerificationPoint("Verifying the repository log (Level.SEVERE) count against the expected count.");
        assertTrue("The repository does not contain the number of records it had wrote for Level.SEVERE {counted:wrote} {" + severe
                   + ":" + logCount + "}", severe == logCount);
        this.logVerificationPassed();

        this.logVerificationPoint("Verifying the repository log (Level.WARNING) count against the expected count.");
        assertTrue("The repository does not contain the number of records it had wrote for Level.WARNING {counted:wrote} {" + warning
                   + ":" + logCount + "}", warning == logCount);
        this.logVerificationPassed();

        this.logVerificationPoint("Verifying the repository log (Level.INFO) count against the expected count.");
        assertTrue("The repository does not contain the number of records it had wrote for Level.INFO {counted:wrote} {" + info + ":"
                   + logCount + "}", info == logCount);
        this.logVerificationPassed();
    }

    /*
     * Tests that HPEL accurately reports the number of logs records entered. Assumes that HPEL is enabled on the active
     * server
     */
    public void testTraceRecordCountAccuracyCheck() throws Exception {
        // Create the following log entries in the repository
        // Levels: All (pass null)
        // LoggerName: com.ibm.ws.fat.hpel.tests.VRAccuracy + uniqueLoggerID_Tag
        // Log Message: VRAccuracy Message for FAT testing
        // Iterations: 476 (just picked a random number)

        setupLogger();

        this.logStep("Creating the trace entries");
        CommonTasks.createLogEntries(HpelSetup.getServerUnderTest(), loggerName, logMessage, null, logCount, CommonTasks.TRACE, -1);
        this.logStepCompleted();

        Thread.sleep(10 * 1000); // sleep for 10 seconds

        // Get the logs from the server to check what is in them
        this.logStep("Getting the trace logs from the server");
        getLogsFromServer(CommonTasks.TRACE);
        this.logStepCompleted();

        // ===================================

        // Go read the repository and see if it has what we are looking for
        RepositoryReaderImpl logRepository = new RepositoryReaderImpl(localTraceRepositoryPath);
        ServerInstanceLogRecordList CurrentProcessLogList = logRepository.getLogListForCurrentServerInstance();
        // record list is the list of records we will compare against. Assume it's the currentServerInstance list by
        // default.
        Iterable<RepositoryLogRecord> recordList = CurrentProcessLogList;

        /*
         * Note, if we are on z/OS the CurrentProcessLogList represents only the controller's logs. Our test log entries
         * will be written by servant though. Will setup a Merged repository to include all the logs from sub-process
         * instances.
         */
        //The problem is with how test counts records on zOS. HPEL allows server to have a parent process (controller) with a set of child processes (servants). 
        //For such servers result for a server instance has none empty result from getChildren() call. The test problem is that it assumes that logs recorded by an application on zOS can be found _only_ in children processes. 
        //On Liberty that is not the case since even on zOS server consists of a single process processing all the requests.

        //The problem can be solved in two ways. 1. Remove 'if zOS' condition since the test is maintained separately on Liberty. 
        //2. Adjust test to do merge between parent and children logs for all OSs. Following the first solution here.

        //if (HpelSetup.getNodeUnderTest().getMachine().getOperatingSystem().equals(OperatingSystem.ZOS)) {
        //this.logStep("Since this is z/OS - using a MergedRepository representing all the servant and adjunct sub-processes");
        //recordList = new MergedRepository(CurrentProcessLogList.getChildren().values());
        //}

        this.logStep("Counting log records by level");
        int fine = 0;
        int finer = 0;
        int finest = 0;
        for (RepositoryLogRecord record : recordList) {
            // process a record
            if (record.getLoggerName().equals(loggerName)) {
                switch (record.getLevel().intValue()) {
                    case 500: // FINE == 500
                        fine++;
                        break;
                    case 400: // FINER == 400
                        finer++;
                        break;
                    case 300: // FINEST == 300
                        finest++;
                        break;
                    default:
                        System.out.println("loggerName expected: " + loggerName);
                        System.out.println("loggerName found: " + record.getLoggerName());
                        System.out.println("level: " + record.getLevel());
                        System.out.println("level in value: " + record.getLevel().intValue());
                }
            }
        }
        this.logStepCompleted();

        // Verify the results
        this.logVerificationPoint("Verifying the repository trace count (Level.FINE) against the expected count.");
        assertTrue("The repository does not contain the number of records it had wrote for Level.FINE {counted:wrote} {" + fine + ":"
                   + logCount + "}", fine == logCount);
        this.logVerificationPassed();

        this.logVerificationPoint("Verifying the repository trace count (Level.FINER) against the expected count.");
        assertTrue("The repository does not contain the number of records it had wrote for Level.FINER {counted:wrote} {" + finer
                   + ":" + logCount + "}", finer == logCount);
        this.logVerificationPassed();

        this.logVerificationPoint("Verifying the repository trace count (Level.FINEST) against the expected count.");
        assertTrue("The repository does not contain the number of records it had wrote for Level.FINEST {counted:wrote} {" + finest
                   + ":" + logCount + "}", finest == logCount);
        this.logVerificationPassed();

    }

    /*
     * This is a utility method to get all of the repository logs as well as the trace logs.
     * 
     * @ param filesToGet This is a switch for which files to get. Options are CommonTasks.LOGS or CommonTasks.TRACE,
     */
    private void getLogsFromServer(String filesToGet) throws Exception {
        if (filesToGet.equals(CommonTasks.LOGS)) {
            // Set up the Simplicity objects to the remote files
            RemoteFile remoteLogsDir = new RemoteFile(HpelSetup.getNodeUnderTest().getMachine(), CommonTasks.getBinaryLogDir(HpelSetup
                            .getServerUnderTest()), "logdata");

            // Set up the Simplicity object to the local results directory
            RemoteFile localLogsResultsDir = new RemoteFile(Machine.getLocalMachine(), Props.getFileProperty(Props.DIR_LOG)
                            .getCanonicalPath()
                                                                                       + File.separator + VerifyRepositoryAccuracy.class.getSimpleName() + File.separator + "logs");

            // Save off the path of the repository log files directory
            localLogsRepositoryPath = localLogsResultsDir.getAbsolutePath();
            localLogsResultsDir.copyFromSource(remoteLogsDir, true, true);
        } else if (filesToGet.equals(CommonTasks.TRACE)) {
            RemoteFile remoteTraceDir = new RemoteFile(HpelSetup.getNodeUnderTest().getMachine(), CommonTasks
                            .getBinaryTraceDir(HpelSetup.getServerUnderTest()), "tracedata");
            RemoteFile localTraceResultsDir = new RemoteFile(Machine.getLocalMachine(), Props.getFileProperty(Props.DIR_LOG)
                            .getCanonicalPath()
                                                                                        + File.separator + VerifyRepositoryAccuracy.class.getSimpleName() + File.separator
                                                                                        + "trace");

            // Save off the path of the repository trace files directory
            localTraceRepositoryPath = localTraceResultsDir.getAbsolutePath();
            localTraceResultsDir.copyFromSource(remoteTraceDir, true, true);
        }
    }

    @Override
    public void tearDown() throws Exception {
        /*
         * Not disabling the spec in tearDown because we would only re-enable for the second test Method. By not
         * disabling it we avoid the extra bounce. We assume that any future test cases will update the spec if
         * required.
         */

        // call the super
        super.tearDown();
    }
}
