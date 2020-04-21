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
 * F017049-18796.1  8.0     01/08/2010   spaungam     Update test to support subdirectories
 * F000896.23216	1.6		06/14/2010	 shighbar	  Update test case for z/OS support.
 */
package com.ibm.ws.logging.hpel.fat;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Date;
import java.util.logging.Level;

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
 * Tests that more than one thread can correctly write to a single repository at the same time.
 *
 */
@RunWith(FATRunner.class)
public class ConcurrentLoggingAccuracyTest {
    int loggerCount = 40;
    int iterations = 40;
    int secondsToSleep = 169; // milliseconds
    int totalCount = loggerCount * iterations;
    private final String[] loggers = new String[loggerCount];
    String localLogsRepositoryPath = null;
    String localTraceRepositoryPath = null;
    String uniqueLoggerID_Tag;
    String loggerName = "ConAccLogger";
    private static final String TRACE_SPECIFICATION = "ConAccLogger*=all"; // trace spec needed for this test case.
    @Server("HpelServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "LogFat", "com.ibm.ws.logging.hpel");
        ShrinkHelper.defaultDropinApp(server, "HpelFat", "com.ibm.ws.logging.hpel.servlet");

        // Confirm HPEL is enabled
        if (!CommonTasks.isHpelEnabled(server)) {
            // HPEL is not enabled.
            CommonTasks.writeLogMsg(Level.INFO, "HPEL is not enabled on " + server.getServerName() + ", attempting to enable.");
            CommonTasks.setHpelEnabled(server, true);
            // if HPEL was not enabled, make sure trace spec is not valid to ensure restart below.
            CommonTasks.setHpelTraceSpec(server, null);

        }

        /*
         * Since we have multiple test methods in this test case, we will only restart if the spec is not already set to
         * what we need. This will avoid extra server restarts which adds to the bucket execution time.
         */
        CommonTasks.writeLogMsg(Level.INFO, "Checking the HPEL trace spec for ConcurrentLoggingAccuracyTest");
        if (!CommonTasks.getHpelTraceSpec(server).equals(TRACE_SPECIFICATION)) {
            // The current spec is not what we need for this test - update spec and bounce server to take effect
            CommonTasks.writeLogMsg(Level.INFO, "Updating the hpel trace spec to " + TRACE_SPECIFICATION + " as part of setup for ConcurrentLoggingAccuracyTest");
            CommonTasks.setHpelTraceSpec(server, TRACE_SPECIFICATION);

            // need to restart the application server now
            // stopServer Server first.
            CommonTasks.writeLogMsg(Level.INFO, "Bouncing server for new spec to take effect. stopServerping application server");
            server.stopServer();

            Thread.sleep(10000); // stopServer operation blocks, but want short pause before restarting.

            // Start Server
            CommonTasks.writeLogMsg(Level.INFO, "Restarting the application server");
            server.startServer();

            CommonTasks.writeLogMsg(Level.INFO, "Checking the trace spec post app server restart: "
                                                + CommonTasks.getHpelTraceSpec(server));
            assertTrue("Failed assertion that HPEL trace specification is set to " + TRACE_SPECIFICATION,
                       TRACE_SPECIFICATION.equals(CommonTasks.getHpelTraceSpec(server)));

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
        RemoteFile remoteLogsDir = CommonTasks.getBinaryLogDir(server);
        Props props = Props.getInstance();
        RemoteFile localLogsResultsDir = new RemoteFile(Machine.getLocalMachine(), props.getFileProperty(props.DIR_LOG).getCanonicalPath()
                                                                                   + File.separator + ConcurrentLoggingAccuracyTest.class.getSimpleName());

        // Save off the path of the repository log files directory
        localLogsRepositoryPath = localLogsResultsDir.getAbsolutePath();

        localLogsResultsDir.copyFromSource(remoteLogsDir, true, true);
    }

    /**
     * Tests that HPEL accurately reports the number of logs records entered. Assumes that HPEL is enabled on the active
     * server
     */
    @Test
    public void testConcurrentLoggingAccuracyCheck() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "Creating logger name");
        setupLoggers();

        CommonTasks.writeLogMsg(Level.INFO, "Creating the log entries");
        createLogEntries();

        Thread.sleep(10000); // wait to ensure logs are written out. Default flash is 10 secs.

        // Get the logs from the server to check what is in them
        CommonTasks.writeLogMsg(Level.INFO, "Getting the message logs from the server");
        getLogsFromServer();

        // Go read the repository and see if it has what we are looking for
        CommonTasks.writeLogMsg(Level.INFO, "Use API to read HPEL repository");

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
        //CommonTasks.writeLogMsg(Level.INFO, "Since this is z/OS - using a MergedRepository representing all the servant and adjunct sub-processes");
        //recordList = new MergedRepository(CurrentProcessLogList.getChildren().values());
        //}

        CommonTasks.writeLogMsg(Level.INFO, "Counting log records by level");
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

        CommonTasks.writeLogMsg(Level.INFO, "Verifying the repository log (Level.SEVERE) count against the expected count.");
        assertTrue("The repository does not contain the number of records it had wrote for Level.SEVERE {counted:wrote} {" + severe
                   + ":" + totalCount + "}", severe == totalCount);

        CommonTasks.writeLogMsg(Level.INFO, "Verifying the repository log (Level.WARNING) count against the expected count.");
        assertTrue("The repository does not contain the number of records it had wrote for Level.WARNING {counted:wrote} {" + warning
                   + ":" + totalCount + "}", warning == totalCount);

        CommonTasks.writeLogMsg(Level.INFO, "Verifying the repository log (Level.INFO) count against the expected count.");
        assertTrue("The repository does not contain the number of records it had wrote for Level.INFO {counted:wrote} {" + info + ":"
                   + totalCount + "}", info == totalCount);

        CommonTasks.writeLogMsg(Level.INFO, "Verifying the repository trace count (Level.FINE) against the expected count.");
        assertTrue("The repository does not contain the number of records it had wrote for Level.FINE {counted:wrote} {" + fine + ":"
                   + totalCount + "}", fine == totalCount);

        CommonTasks.writeLogMsg(Level.INFO, "Verifying the repository trace count (Level.FINER) against the expected count.");
        assertTrue("The repository does not contain the number of records it had wrote for Level.FINER {counted:wrote} {" + finer
                   + ":" + totalCount + "}", finer == totalCount);

        CommonTasks.writeLogMsg(Level.INFO, "Verifying the repository trace count (Level.FINEST) against the expected count.");
        assertTrue("The repository does not contain the number of records it had wrote for Level.FINEST {counted:wrote} {" + finest
                   + ":" + totalCount + "}", finest == totalCount);

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
                CommonTasks.createLogEntries(server, loggerName, "Hi from logger " + loggerName + " in this thread", Level.ALL, iterations,
                                             CommonTasks.LOGS_TRACE, secondsToSleep);
            } catch (Exception e) {
                // nothing for now
            }
        }
    }
}
