/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.fat;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Logging;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
@RunWith(FATRunner.class)
public class TimeBasedLogRolloverTest {
    private static Class<?> c = TimeBasedLogRolloverTest.class;

    private static final String MESSAGES_LOG_PREFIX = "messages";
    private static final String TRACE_LOG_PREFIX = "trace";
    private static final String TRACE_LOG_NAME = "/trace.log";
    private static final String LOG_EXT = ".0.log";
    private static final String FILE_SEPARATOR = "/";
    private static final SimpleDateFormat FILE_DATE = new SimpleDateFormat("_yy.MM.dd_HH.mm.ss");
    private static final long FILE_WAIT_SECONDS_PADDING = 1000;
    private static final String CLASS_NAME = TimeBasedLogRolloverTest.class.getName();
    private static final String TEST_SEPARATOR = "*******************";

    private static final String SERVER_NAME_XML = "com.ibm.ws.logging.timedrolloverxml";
    private static final String SERVER_NAME_TIME_ROLLOVER_DISABLED = "com.ibm.ws.logging.timedrolloverdisabled";
    private static final String SERVER_NAME_NO_TRACE = "com.ibm.ws.logging.timedrollovernotrace";
    //env and bootstrap server initial config
    private static final String SERVER_NAME_ENV = "com.ibm.ws.logging.timedrolloverenv";
    private static final String SERVER_NAME_BOOTSTRAP = "com.ibm.ws.logging.timedrolloverbootstrap";
    private static final String SERVER_NAME_JSON = "com.ibm.ws.logging.timedrolloverjson";

    private static LibertyServer server_xml;
    private static LibertyServer server_env;
    private static LibertyServer server_bootstrap;
    private static LibertyServer server_time_rollover_disabled;
    private static LibertyServer server_no_trace;
    private static LibertyServer server_json_logs;

    private static LibertyServer serverInUse; // hold on to the server currently used so cleanUp knows which server to stop
    private static final Logger LOG = Logger.getLogger(TimeBasedLogRolloverTest.class.getName());

    @BeforeClass
    public static void initialSetup() throws Exception {
        server_xml = LibertyServerFactory.getLibertyServer(SERVER_NAME_XML);
        server_env = LibertyServerFactory.getLibertyServer(SERVER_NAME_ENV);
        server_bootstrap = LibertyServerFactory.getLibertyServer(SERVER_NAME_BOOTSTRAP);
        server_time_rollover_disabled = LibertyServerFactory.getLibertyServer(SERVER_NAME_TIME_ROLLOVER_DISABLED);
        server_no_trace = LibertyServerFactory.getLibertyServer(SERVER_NAME_NO_TRACE);
        server_json_logs = LibertyServerFactory.getLibertyServer(SERVER_NAME_JSON);

        // Preserve the original server configuration
        server_xml.saveServerConfiguration();
        server_env.saveServerConfiguration();
        server_bootstrap.saveServerConfiguration();
        server_time_rollover_disabled.saveServerConfiguration();
        server_no_trace.saveServerConfiguration();
        server_json_logs.saveServerConfiguration();

        ShrinkHelper.defaultDropinApp(server_xml, "quicklogtest", "com.ibm.ws.logging.fat.quick.log.test");
    }

    public void setUp(LibertyServer server, String method) throws Exception {
        LOG.logp(Level.INFO, CLASS_NAME, method, TEST_SEPARATOR + " TEST: " + method + " " + TEST_SEPARATOR);
        serverInUse = server;
        waitForBeginningOfMinute();
        if (server != null && !server.isStarted()) {
            // Restore the original server configuration, before starting the server for each test case.
            server.restoreServerConfiguration();
            server.startServer();
        }
    }

    private static void waitForBeginningOfMinute() {
        //wait for the beginning of the minute
        if (Calendar.getInstance().get(Calendar.SECOND) != 0) {
            try {
                Thread.sleep((60000 - Calendar.getInstance().get(Calendar.SECOND) * 1000));
                Thread.sleep(2000); //padding
            } catch (InterruptedException e) {
            }
        }
    }

    @After
    public void cleanUp() throws Exception {
        if (serverInUse != null && serverInUse.isStarted()) {
            serverInUse.stopServer("com.ibm.ws.logging.fat.ffdc.servlet.FFDCServlet.doGet", "ArithmeticException",
                                   "CWWKG0081E", "CWWKG0083W", "TRAS3015W", "TRAS3013W");
        }
    }

    /*
     * Tests setting WLP_LOGGING_ROLLOVER_START_TIME=00:00
     * and WLP_LOGGING_ROLLOVER_INTERVAL=1m in server.env.
     */
    @Test
    public void testTimedRolloverEnv() throws Exception {
        setUp(server_env, "testTimedRolloverEnv");
        checkForRolledLogsAtTime(getNextRolloverTime(0, 1));
    }

    /*
     * Tests setting com.ibm.ws.logging.rollover.start.time
     * and com.ibm.ws.logging.rollover.interval in bootstrap.properties.
     */
    @Test
    public void testTimedRolloverBootstrap() throws Exception {
        setUp(server_bootstrap, "testTimedRolloverBootstrap");
        checkForRolledLogsAtTime(getNextRolloverTime(0, 1));
    }

    /*
     * Tests setting
     * <logging rolloverStartTime="00:00" rolloverInterval="1m"/>
     * in server.xml.
     */
    @Test
    public void testTimedRolloverXML() throws Exception {
        setUp(server_xml, "testTimedRolloverXML");
        checkForRolledLogsAtTime(getNextRolloverTime(0, 1));
    }

    /*
     * Tests setting
     * <logging rolloverStartTime="00:00" rolloverInterval="1m"/>
     * in server.xml, with json logging.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTimedRolloverJsonLogs() throws Exception {
        setUp(server_json_logs, "testTimedRolloverXML");
        checkForRolledLogsAtTime(getNextRolloverTime(0, 1));
    }

    /*
     * Tests overriding bootstrap rollover properties with server.xml.
     * <logging rolloverStartTime="00:00" rolloverInterval="2m"/>
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTimedRolloverXMLOverrides() throws Exception {
        setUp(server_bootstrap, "testTimedRolloverXMLOverrides");
        /**
         * In the case where the current minutes value is an odd number,
         * an updated rollover interval of 2m in reference to 00:00 will still
         * rollover at the next minute, which doesn't properly check the update.
         * In this case, sleep for 1m to get an even minute that should rollover
         * 2 minutes (or at least > 1 minute) from the current time.
         */
        if (Calendar.getInstance().get(Calendar.MINUTE) % 2 != 0) {
            Thread.sleep(60000);
        }
        setServerConfiguration(true, true, false, "00:00", "2m", 0);
        checkForRolledLogsAtTime(getNextRolloverTime(0, 2));
    }

    /*
     * Tests that files are rolled periodically at the specified interval.
     * <logging rolloverStartTime="00:00" rolloverInterval="1m"/>
     */
    @Test
    @Mode(TestMode.FULL)
    public void testMultipleRollovers() throws Exception {
        setUp(server_xml, "testMultipleRollovers");
        Calendar cal = getNextRolloverTime(0, 1);
        checkForRolledLogsAtTime(cal);
        cal.add(Calendar.MINUTE, 1);
        checkForRolledLogsAtTime(cal);
    }

    /*
     * Tests updating the server.xml to update the next scheduled time
     * <logging rolloverStartTime="00:00" rolloverInterval="2m"/>
     * in server.xml.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTimedRolloverXMLUpdates() throws Exception {
        setUp(server_xml, "testTimedRolloverXMLUpdates");
        checkForRolledLogsAtTime(getNextRolloverTime(0, 1));
        /**
         * In the case where the current minutes value is an odd number,
         * an updated rollover interval of 2m in reference to 00:00 will still
         * rollover at the next minute, which doesn't properly check the update.
         * In this case, sleep for 1m to get an even minute that should rollover
         * 2 minutes (or at least > 1 minute) from the current time.
         */
        if (Calendar.getInstance().get(Calendar.MINUTE) % 2 != 0) {
            Thread.sleep(60000);
        }
        setServerConfiguration(true, true, false, "00:00", "2m", 0);
        checkForRolledLogsAtTime(getNextRolloverTime(0, 2));
    }

    /*
     * Tests that no trace logs exist with rollover set and
     * no trace specification.
     * <logging rolloverStartTime="00:00" rolloverInterval="1m"/>
     */
    @Test
    @Mode(TestMode.FULL)
    public void testRolloverNoTraceSpecification() throws Exception {
        setUp(server_no_trace, "testRolloverNoTraceSpecification");
        checkForRolledLogsAtTime(getNextRolloverTime(0, 1), false);

        //checks that no trace* prefixed logs exist
        File logsDir = new File(getLogsDirPath());
        String[] logsDirFiles = logsDir.list();
        List<String> traceLogs = new ArrayList<String>();

        for (int i = 0; i < logsDirFiles.length; i++) {
            if (logsDirFiles[i].startsWith(TRACE_LOG_PREFIX))
                traceLogs.add(logsDirFiles[i]);
        }

        assertTrue("There should be no trace prefixed logs. Instead, these are the trace "
                   + "prefixed logs: " + traceLogs.toString(), traceLogs.size() == 0);
    }

    /*
     * Tests missing rolloverStartTime and specified rolloverInterval.
     * Should set rolloverStartTime to default 00:00.
     * <logging rolloverInterval="3m"/>
     * Hard to calculate/find root rolloverStartTime from behaviour, so will
     * search in trace to find the message where rolloverStartTime is set.
     */
    @Test
    public void testMissingRolloverStartTime() throws Exception {
        setUp(server_time_rollover_disabled, "testMissingRolloverStartTime");
        setServerConfiguration(false, true, false, "", "3m", 0);

        RemoteFile traceLog = new RemoteFile(serverInUse.getMachine(), getLogsDirPath() + TRACE_LOG_NAME);
        List<String> lines = serverInUse.findStringsInLogs("rolloverStartTime=00:00", traceLog);
        assertTrue("The rolloverStartTime was not set to a default of 00:00.", lines.size() > 0);
    }

    /*
     * Tests missing rolloverInterval and specified rolloverStartTime.
     * Should set rolloverInterval to default 1d.
     * <logging rolloverStartTime="00:00"/>
     * Cannot wait for 1 day for the logs to rollover, so will search
     * in trace to find the info message where rolloverInterval it is set.
     */
    @Test
    public void testMissingRolloverInterval() throws Exception {
        setUp(server_time_rollover_disabled, "testMissingRolloverInterval");
        setServerConfiguration(true, false, false, "00:00", "", 0);

        RemoteFile traceLog = new RemoteFile(serverInUse.getMachine(), getLogsDirPath() + TRACE_LOG_NAME);
        List<String> lines = serverInUse.findStringsInLogs("rolloverInterval=1440", traceLog);
        assertTrue("The rolloverInterval was not set to a default of 1440 minutes.", lines.size() > 0);
    }

    /*
     * Tests enabling and disabling time based log rollover.
     * <logging rolloverStartTime="00:00" rolloverInterval="1m"/>
     */
    @Test
    @Mode(TestMode.FULL)
    public void testEnableDisableTimedRollover() throws Exception {
        setUp(server_time_rollover_disabled, "testEnableDisableTimedRollover");
        setServerConfiguration(true, true, false, "00:00", "1m", 0); //enable
        Calendar cal = getNextRolloverTime(0, 1);
        checkForRolledLogsAtTime(cal);
        cal.add(Calendar.MINUTE, 1);

        serverInUse.restoreServerConfiguration(); //restore disabled server config
        serverInUse.waitForStringInLogUsingMark("CWWKG0017I"); //wait for server config update

        long nextRollover = cal.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
        if (nextRollover > 0)
            Thread.sleep(nextRollover + FILE_WAIT_SECONDS_PADDING); //sleep for another length of the old specified log rolloverInterval

        //check that only 2 messages*/trace* prefixed logs exist
        //aka the messages/trace logs were not rolled over again
        File logsDir = new File(getLogsDirPath());
        String[] logsDirFiles = logsDir.list();
        List<String> messagesLogs = new ArrayList<String>();
        List<String> traceLogs = new ArrayList<String>();

        for (int i = 0; i < logsDirFiles.length; i++) {
            if (logsDirFiles[i].startsWith(MESSAGES_LOG_PREFIX))
                messagesLogs.add(logsDirFiles[i]);
            if (logsDirFiles[i].startsWith(TRACE_LOG_PREFIX))
                traceLogs.add(logsDirFiles[i]);
        }

        assertTrue("The number of messages prefixed log files should be equal to 2. Instead, " +
                   "these are the messages prefixed logs: " + messagesLogs.toString(), messagesLogs.size() == 2);
        assertTrue("The number of trace prefixed log files should be equal to 2. Instead, " +
                   "these are the trace prefixed logs: " + traceLogs.toString(), traceLogs.size() == 2);
    }

    /*
     * Tests invalid rolloverStartTime. Should output warning and set rolloverStartTime to default 00:00.
     * <logging rolloverStartTime="24:00">
     */
    @Test
    public void testInvalidRolloverStartTime() throws Exception {
        setUp(server_xml, "testInvalidRolloverStartTime");

        waitForBeginningOfMinute();
        setServerConfiguration(true, false, false, "24:00", "", 0);
        List<String> lines = serverInUse.findStringsInLogs("TRAS3015W");
        LOG.logp(Level.INFO, CLASS_NAME, "testInvalidRolloverStartTime", "Found warning: " + lines.toString());
        assertTrue("No TRAS3015W warning was found indicating that 24:00 is an invalid rolloverStartTime", lines.size() > 0);
    }

    /*
     * Tests rolloverInterval < 1m. Should output warning and set rolloverInterval to default 1d.
     * <logging rolloverStartTime="00:00"/>
     */
    @Test
    public void testInvalidRolloverInterval() throws Exception {
        setUp(server_xml, "testInvalidRolloverInterval");

        waitForBeginningOfMinute();
        setServerConfiguration(false, true, false, "", "30s", 0);
        List<String> lines = serverInUse.findStringsInLogs("TRAS3013W");
        LOG.logp(Level.INFO, CLASS_NAME, "testInvalidRolloverInterval", "Found warning: " + lines.toString());
        assertTrue("No TRAS3013W warning was found indicating that 24:00 is an invalid rolloverStartTime", lines.size() > 0);
    }

    /*
     * Tests that maxFileSize takes priority over timed log rollover.
     *
     */
    @Test
    @Mode(TestMode.FULL)
    public void testMaxFileSizePriorityRolling() throws Exception {
        setUp(server_xml, "testMaxFileSizeRolling");

        waitForBeginningOfMinute();
        setServerConfiguration(false, false, true, "", "", 1);

        //check for rolled log first, to ensure we start writing messages at the next minute
        checkForRolledLogsAtTime(getNextRolloverTime(0, 1));

        getHttpServlet("/quicklogtest/QuickLogTest?threads=1&duration=10&messageSize=0&delay=0&action=log&stepThreads=false", server_xml);

        File logsDir = new File(getLogsDirPath());
        String[] logsDirFiles = logsDir.list();
        List<String> messagesLogs = new ArrayList<String>();

        for (int i = 0; i < logsDirFiles.length; i++) {
            if (logsDirFiles[i].startsWith(MESSAGES_LOG_PREFIX))
                messagesLogs.add(logsDirFiles[i]);
        }

        //at least three messages_*.log files: the first rolled over by time,
        //the second rolloed over by size, and then the newest file
        assertTrue("There should be at least 3 messages prefixed log files. Instead, " +
                   "these are the messages prefixed logs: " + messagesLogs.toString(), messagesLogs.size() >= 3);

        //check Log is rolled over at next rollover time
        checkForRolledLogsAtTime(getNextRolloverTime(0, 1));
    }

    private static String getLogsDirPath() throws Exception {
        return serverInUse.getDefaultLogFile().getParentFile().getAbsolutePath();
    }

    private static void checkForRolledLogsAtTime(Calendar cal) throws Exception {
        checkForRolledLogsAtTime(cal, true);
    }

    private static void checkForRolledLogsAtTime(Calendar cal, boolean trace) throws Exception {
        LOG.logp(Level.INFO, CLASS_NAME, "checkForRolledLogsAtTime", "The next log rollover is scheduled to be at: " + cal.getTime());

        String date = FILE_DATE.format(cal.getTime());
        String messagesLogName = FILE_SEPARATOR + MESSAGES_LOG_PREFIX + date + LOG_EXT;
        String traceLogName = FILE_SEPARATOR + TRACE_LOG_PREFIX + date + LOG_EXT;

        //get rolled messages and trace logs
        File messagesLog = new File(getLogsDirPath(), messagesLogName);
        File traceLog = new File(getLogsDirPath(), traceLogName);

        long timeToFirstRollover = cal.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
        if (timeToFirstRollover > 0)
            Thread.sleep(timeToFirstRollover + FILE_WAIT_SECONDS_PADDING); //sleep until next time the log is set to rollover

        assertTrue("The rolled messages.log file " + messagesLog + " doesn't exist.", messagesLog.exists());

        if (trace)
            assertTrue("The rolled trace.log file " + traceLog + " doesn't exist.", traceLog.exists());
    }

    private static Calendar getNextRolloverTime(int rolloverStartHour, int rolloverInterval) {
        //set calendar start time
        Calendar sched = Calendar.getInstance();
        sched.set(Calendar.HOUR_OF_DAY, rolloverStartHour);
        sched.set(Calendar.MINUTE, 0);
        sched.set(Calendar.SECOND, 0);
        sched.set(Calendar.MILLISECOND, 0);
        Calendar currCal = Calendar.getInstance();

        if (currCal.before(sched)) { //if currTime before startTime, firstRollover = startTime - n(interval)
            while (currCal.before(sched)) {
                sched.add(Calendar.MINUTE, rolloverInterval * (-1));
            }
            sched.add(Calendar.MINUTE, rolloverInterval); //add back interval due to time overlap
        } else if (currCal.after(sched)) { //if currTime after startTime, firstRollover = startTime + n(interval)
            while (currCal.after(sched)) {
                sched.add(Calendar.MINUTE, rolloverInterval);
            }
        } else if (currCal.equals(sched)) { //if currTime == startTime, set first rollover to next rolloverInterval
            sched.add(Calendar.MINUTE, rolloverInterval);
        }
        return sched;
    }

    private static void setServerConfiguration(boolean isRolloverStartTime, boolean isRolloverInterval, boolean isMaxFileSize, String newRolloverStartTime,
                                               String newRolloverInterval, int maxFileSize) throws Exception {
        Logging loggingObj;
        ServerConfiguration serverConfig = serverInUse.getServerConfiguration();
        loggingObj = serverConfig.getLogging();
        if (isRolloverStartTime) {
            loggingObj.setRolloverStartTime(newRolloverStartTime);
        }
        if (isRolloverInterval) {
            loggingObj.setRolloverInterval(newRolloverInterval);
        }
        if (isMaxFileSize) {
            loggingObj.setMaxFileSize(maxFileSize);
        }
        serverInUse.setMarkToEndOfLog();
        serverInUse.updateServerConfiguration(serverConfig);
        serverInUse.waitForConfigUpdateInLogUsingMark(null);
    }

    //setup connection for extension fields
    private String getHttpServlet(String servletPath, LibertyServer server) throws Exception {
        HttpURLConnection con = null;
        try {
            String sURL = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + servletPath;
            URL checkerServletURL = new URL(sURL);
            con = (HttpURLConnection) checkerServletURL.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            String sep = System.getProperty("line.separator");
            String line = null;
            StringBuilder lines = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

            while ((line = br.readLine()) != null && line.length() > 0) {
                lines.append(line).append(sep);
            }
            Log.info(c, "getHttpServlet", sURL);
            return lines.toString();
        } finally {
            if (con != null)
                con.disconnect();
        }
    }
}
