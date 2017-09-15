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
 * F017049-18796.1  8.0     01/08/2010   spaungam     Update test to support subdirectories
 * F000896.23216	1.6		06/14/2010	 shighbar	  Update test case for z/OS support.
 */
package com.ibm.ws.fat.hpel.tests;

import java.io.File;
import java.util.Date;
import java.util.logging.Level;

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
 * Tests that more than one thread can correctly write to a single repository at the same time.
 * 
 */
public class ConcurrentLoggingAccuracyTest extends VerboseTestCase {
    int loggerCount = 40;
    int iterations = 40;
    int secondsToSleep = 169; // milliseconds
    int totalCount = loggerCount * iterations;
    private final String[] loggers = new String[loggerCount];
    String localLogsRepositoryPath = null;
    String localTraceRepositoryPath = null;
    String uniqueLoggerID_Tag;
    String loggerName = "ConAccLogger";
    final String TRACE_SPECIFICATION = "ConAccLogger*=all"; // trace spec needed for this test case.

    public ConcurrentLoggingAccuracyTest(String name) {
        super(name);
    }

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

    private void setupLoggers() {
        // setup a new unique identifier for the logger name
        Date d = new Date();
        for (int i = 0; i < loggers.length; i++) {
            loggers[i] = loggerName + d + "_" + i;
        }
    }

    /**
     * A utility method to get all of the repository logs as well as the trace logs.
     */
    private void getLogsFromServer() throws Exception {
        RemoteFile remoteLogsDir = CommonTasks.getBinaryLogDir(HpelSetup.getServerUnderTest());
        RemoteFile localLogsResultsDir = new RemoteFile(Machine.getLocalMachine(), Props.getFileProperty(Props.DIR_LOG)
                        .getCanonicalPath()
                                                                                   + File.separator + ConcurrentLoggingAccuracyTest.class.getSimpleName());

        // Save off the path of the repository log files directory
        localLogsRepositoryPath = localLogsResultsDir.getAbsolutePath();

        localLogsResultsDir.copyFromSource(remoteLogsDir, true, true);
    }

    /**
     * Tests that HPEL accurately reports the number of logs records entered. Assumes that HPEL is enabled on the active
     * server
     */
    public void testConcurrentLoggingAccuracyCheck() throws Exception {
        this.logStep("Creating logger name");
        setupLoggers();
        this.logStepCompleted();

        this.logStep("Creating the log entries");
        createLogEntries();
        this.logStepCompleted();

        Thread.sleep(10000); // wait to ensure logs are written out. Default flash is 10 secs.

        // Get the logs from the server to check what is in them
        this.logStep("Getting the message logs from the server");
        getLogsFromServer();
        this.logStepCompleted();

        // Go read the repository and see if it has what we are looking for
        this.logStep("Use API to read HPEL repository");

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
        int fine = 0;
        int finer = 0;
        int finest = 0;
        for (RepositoryLogRecord record : recordList) {
            // process a record
            if (record.getLoggerName().startsWith((loggerName))) {
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
                    case 500: // FINE == 500
                        fine++;
                        break;
                    case 400: // FINER == 400
                        finer++;
                        break;
                    case 300: // FINEST == 300
                        finest++;
                        break;
                }
            }
        }
        this.logStepCompleted();

        this.logVerificationPoint("Verifying the repository log (Level.SEVERE) count against the expected count.");
        assertTrue("The repository does not contain the number of records it had wrote for Level.SEVERE {counted:wrote} {" + severe
                   + ":" + totalCount + "}", severe == totalCount);
        this.logVerificationPassed();

        this.logVerificationPoint("Verifying the repository log (Level.WARNING) count against the expected count.");
        assertTrue("The repository does not contain the number of records it had wrote for Level.WARNING {counted:wrote} {" + warning
                   + ":" + totalCount + "}", warning == totalCount);
        this.logVerificationPassed();

        this.logVerificationPoint("Verifying the repository log (Level.INFO) count against the expected count.");
        assertTrue("The repository does not contain the number of records it had wrote for Level.INFO {counted:wrote} {" + info + ":"
                   + totalCount + "}", info == totalCount);
        this.logVerificationPassed();

        this.logVerificationPoint("Verifying the repository trace count (Level.FINE) against the expected count.");
        assertTrue("The repository does not contain the number of records it had wrote for Level.FINE {counted:wrote} {" + fine + ":"
                   + totalCount + "}", fine == totalCount);
        this.logVerificationPassed();

        this.logVerificationPoint("Verifying the repository trace count (Level.FINER) against the expected count.");
        assertTrue("The repository does not contain the number of records it had wrote for Level.FINER {counted:wrote} {" + finer
                   + ":" + totalCount + "}", finer == totalCount);
        this.logVerificationPassed();

        this.logVerificationPoint("Verifying the repository trace count (Level.FINEST) against the expected count.");
        assertTrue("The repository does not contain the number of records it had wrote for Level.FINEST {counted:wrote} {" + finest
                   + ":" + totalCount + "}", finest == totalCount);
        this.logVerificationPassed();
    }

    /**
     * Driver method for executing all the LoggerThread instances. This method will not return until all the Loggers
     * have completed writing their records.
     * 
     */
    public void createLogEntries() throws Exception {
        LoggerThread[] lt = new LoggerThread[loggerCount];

        for (int i = 0; i < loggers.length; i++) {
            lt[i] = new LoggerThread(loggers[i]);
        }

        for (int i = 0; i < lt.length; i++) {
            lt[i].start();
        }

        // cycle thru all the threads until each one has finished
        for (int i = 0; i < lt.length; i++) {
            while (lt[i].isAlive()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    // do nothing in this case.
                }
            }
        }
    }

    /**
     * Thread definition that will be writing to the repository.
     * 
     * @author schleus
     * 
     */
    class LoggerThread extends Thread {
        private String loggerName = null;

        public LoggerThread(String name) {
            this.loggerName = name;
        }

        @Override
        public void run() {
            try {
                CommonTasks.createLogEntries(HpelSetup.getServerUnderTest(), loggerName, "Hi from logger " + loggerName + " in this thread", Level.ALL, iterations,
                                             CommonTasks.LOGS_TRACE, secondsToSleep);
            } catch (Exception e) {
                // nothing for now
            }
        }
    }
}
