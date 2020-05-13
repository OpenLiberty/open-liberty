/*******************************************************************************
 * Copyright (c) 2010, 2020 IBM Corporation and others.
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
 * F000896.23216    8.0      06/10/2010  shighbar     Update to work on z/OS requires support for reading servants. Refactored initialize method into setup to reduce restartServers which was impacting test on z/OS.
 */
package com.ibm.ws.logging.hpel.fat;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Date;
import java.util.logging.Level;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.logging.hpel.reader.RepositoryLogRecord;
import com.ibm.websphere.logging.hpel.reader.RepositoryReaderImpl;
import com.ibm.websphere.logging.hpel.reader.ServerInstanceLogRecordList;
import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.fat.util.Props;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * This test is about verifying that HPEL is accurately reporting the number of messages sent to it.
 *
 * The test outline is as follows: 1: Create an HTTP client to the LogCreator JSP that logs messages (use the
 * CommonTasks object) 2: Enable tracing so a tracedata directory and repository is created 3: Call the HTTP client with
 * a known number of logging entries 4: Get the log records from the repository and count them 5: Do they match? Yes,
 * pass, No, fail
 *
 */
@RunWith(FATRunner.class)
public class VerifyRepositoryAccuracy {
    String localLogsRepositoryPath = null;
    String localTraceRepositoryPath = null;
    String uniqueLoggerID_Tag;
    String loggerName = "VRAccuracyLogger";
    String logMessage = "VRAccuracy Message for FAT testing";
    final static String TRACE_SPECIFICATION = "VRAccuracyLogger*=all"; // trace spec needed for this test case.
    int logCount = 49;
    boolean loggerSetup = false;

    @Server("HpelServer")
    public static LibertyServer server;

    /**
     * Configures instance resources to initialize this TestCase.
     *
     * @throws Exception
     *                       if a problem happens while configuring the test fixture.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        // Call super.SetUp() cause we still want it's setup as well

        // Confirm HPEL is enabled
        ShrinkHelper.defaultDropinApp(server, "LogFat", "com.ibm.ws.logging.hpel");
        ShrinkHelper.defaultDropinApp(server, "HpelFat", "com.ibm.ws.logging.hpel.servlet");
        /*
         * Since we have multiple test methods in this test case, we will only restartServer if the spec is not already set to
         * what we need. This will avoid extra server restartServers which adds to the bucket execution time.
         */
        CommonTasks.writeLogMsg(Level.INFO, "Checking the HPEL trace spec for VerifyRepositoryAccuracy");
        if (!CommonTasks.getHpelTraceSpec(server).equals(TRACE_SPECIFICATION)) {
            // The current spec is not what we need for this test - update spec and bounce server to take effect
            CommonTasks.writeLogMsg(Level.INFO, "Updating the hpel trace spec to " + TRACE_SPECIFICATION + " as part of setup for VerifyRepositoryAccuracy");
            CommonTasks.setHpelTraceSpec(server, TRACE_SPECIFICATION);

            // need to restartServer the application server now
            // stopServer Server first.
            CommonTasks.writeLogMsg(Level.INFO, "Bouncing server for new spec to take effect. stopServerping application server");
            server.stopServer();

            Thread.sleep(10000); // stopServer operation blocks, but want short pause before restartServering.

            // startServer Server
            CommonTasks.writeLogMsg(Level.INFO, "RestartServering the application server");
            server.startServer();

            CommonTasks.writeLogMsg(Level.INFO, "Checking the trace spec post app server restartServer: "
                                                + CommonTasks.getHpelTraceSpec(server));
            assertTrue("Failed assertion that HPEL trace specification is set to " + TRACE_SPECIFICATION,
                       TRACE_SPECIFICATION.equals(CommonTasks.getHpelTraceSpec(server)));

        }
    }

    private void setupLogger() {
        if (!loggerSetup) {
            // setup a new unique identifier for the logger name
            Date d = new Date();
            loggerName = loggerName + d.getTime();
            CommonTasks.writeLogMsg(Level.INFO, "Creating a new logger named: " + loggerName);

            loggerSetup = true;
        }
    }

    /*
     * Tests that HPEL accurately reports the number of logs records entered. Assumes that HPEL is enabled on the active
     * server
     */
    @Test
    public void testLogRecordCountAccuracyCheck() throws Exception {
        // Create the following log entries in the repository
        // Levels: All (pass null)
        // LoggerName: com.ibm.ws.fat.hpel.tests.VRAccuracy + uniqueLoggerID_Tag
        // Log Message: VRAccuracy Message for FAT testing
        // Iterations: 49 (just picked a random number)

        setupLogger();

        CommonTasks.writeLogMsg(Level.INFO, "Creating the log entries");
        CommonTasks.createLogEntries(server, loggerName, logMessage, null, logCount, CommonTasks.LOGS, -1);

        Thread.sleep(10 * 1000); // sleep for 10 seconds

        // Get the logs from the server to check what is in them
        CommonTasks.writeLogMsg(Level.INFO, "Getting the message logs from the server");
        getLogsFromServer(CommonTasks.LOGS);

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
        //CommonTasks.writeLogMsg(Level.INFO,"Since this is z/OS - using a MergedRepository representing all the servant and adjunct sub-processes");
        //recordList = new MergedRepository(CurrentProcessLogList.getChildren().values());
        //}

        CommonTasks.writeLogMsg(Level.INFO, "Counting log records by level");
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

        // Verify the results
        CommonTasks.writeLogMsg(Level.INFO, "Verifying the repository log (Level.SEVERE) count against the expected count.");
        assertTrue("The repository does not contain the number of records it had wrote for Level.SEVERE {counted:wrote} {" + severe
                   + ":" + logCount + "}", severe == logCount);

        CommonTasks.writeLogMsg(Level.INFO, "Verifying the repository log (Level.WARNING) count against the expected count.");
        assertTrue("The repository does not contain the number of records it had wrote for Level.WARNING {counted:wrote} {" + warning
                   + ":" + logCount + "}", warning == logCount);

        CommonTasks.writeLogMsg(Level.INFO, "Verifying the repository log (Level.INFO) count against the expected count.");
        assertTrue("The repository does not contain the number of records it had wrote for Level.INFO {counted:wrote} {" + info + ":"
                   + logCount + "}", info == logCount);

    }

    /*
     * Tests that HPEL accurately reports the number of logs records entered. Assumes that HPEL is enabled on the active
     * server
     */
    @Test
    public void testTraceRecordCountAccuracyCheck() throws Exception {
        // Create the following log entries in the repository
        // Levels: All (pass null)
        // LoggerName: com.ibm.ws.fat.hpel.tests.VRAccuracy + uniqueLoggerID_Tag
        // Log Message: VRAccuracy Message for FAT testing
        // Iterations: 476 (just picked a random number)

        setupLogger();

        CommonTasks.writeLogMsg(Level.INFO, "Creating the trace entries");
        CommonTasks.createLogEntries(server, loggerName, logMessage, null, logCount, CommonTasks.TRACE, -1);

        Thread.sleep(10 * 1000); // sleep for 10 seconds

        // Get the logs from the server to check what is in them
        CommonTasks.writeLogMsg(Level.INFO, "Getting the trace logs from the server");
        getLogsFromServer(CommonTasks.TRACE);

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
        //CommonTasks.writeLogMsg(Level.INFO,"Since this is z/OS - using a MergedRepository representing all the servant and adjunct sub-processes");
        //recordList = new MergedRepository(CurrentProcessLogList.getChildren().values());
        //}

        CommonTasks.writeLogMsg(Level.INFO, "Counting log records by level");
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

        // Verify the results
        CommonTasks.writeLogMsg(Level.INFO, "Verifying the repository trace count (Level.FINE) against the expected count.");
        assertTrue("The repository does not contain the number of records it had wrote for Level.FINE {counted:wrote} {" + fine + ":"
                   + logCount + "}", fine == logCount);

        CommonTasks.writeLogMsg(Level.INFO, "Verifying the repository trace count (Level.FINER) against the expected count.");
        assertTrue("The repository does not contain the number of records it had wrote for Level.FINER {counted:wrote} {" + finer
                   + ":" + logCount + "}", finer == logCount);

        CommonTasks.writeLogMsg(Level.INFO, "Verifying the repository trace count (Level.FINEST) against the expected count.");
        assertTrue("The repository does not contain the number of records it had wrote for Level.FINEST {counted:wrote} {" + finest
                   + ":" + logCount + "}", finest == logCount);

    }

    /*
     * This is a utility method to get all of the repository logs as well as the trace logs.
     *
     * @ param filesToGet This is a switch for which files to get. Options are CommonTasks.LOGS or CommonTasks.TRACE,
     */
    private void getLogsFromServer(String filesToGet) throws Exception {
        Props props = Props.getInstance();
        if (filesToGet.equals(CommonTasks.LOGS)) {
            // Set up the Simplicity objects to the remote files
            RemoteFile remoteLogsDir = new RemoteFile(server.getMachine(), CommonTasks.getBinaryLogDir(server), "logdata");

            // Set up the Simplicity object to the local results directory
            RemoteFile localLogsResultsDir = new RemoteFile(Machine.getLocalMachine(), props.getFileProperty(Props.DIR_LOG).getCanonicalPath()
                                                                                       + File.separator + VerifyRepositoryAccuracy.class.getSimpleName() + File.separator + "logs");

            // Save off the path of the repository log files directory
            localLogsRepositoryPath = localLogsResultsDir.getAbsolutePath();
            localLogsResultsDir.copyFromSource(remoteLogsDir, true, true);
        } else if (filesToGet.equals(CommonTasks.TRACE)) {
            RemoteFile remoteTraceDir = new RemoteFile(server.getMachine(), CommonTasks.getBinaryTraceDir(server), "tracedata");
            RemoteFile localTraceResultsDir = new RemoteFile(Machine.getLocalMachine(), props.getFileProperty(Props.DIR_LOG).getCanonicalPath()
                                                                                        + File.separator + VerifyRepositoryAccuracy.class.getSimpleName() + File.separator
                                                                                        + "trace");

            // Save off the path of the repository trace files directory
            localTraceRepositoryPath = localTraceResultsDir.getAbsolutePath();
            localTraceResultsDir.copyFromSource(remoteTraceDir, true, true);
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}
