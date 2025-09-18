/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/

package io.openliberty.transport.http_fat;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static componenttest.custom.junit.runner.Mode.TestMode.FULL;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class AccessLogRolloverTest {

    private static final String ACCESS_LOG_PREFIX = "http_access";
    private static final SimpleDateFormat FILE_DATE_NO_SECONDS = new SimpleDateFormat("_yy.MM.dd_HH.mm");
    private static final String CLASS_NAME = AccessLogRolloverTest.class.getName();
    private static final String TEST_SEPARATOR = "*******************";
    private static final String FILE_SEPARATOR = "/";
    private static final String LOG_EXT = ".log";

    private static final long FILE_WAIT_SECONDS_PADDING = 1000;
    private static final long POLLING_TIMEOUT = 60000;
    private static final long POllING_INTERVAL = 1000; 

    private static final Logger LOG = Logger.getLogger(AccessLogRolloverTest.class.getName());

    private static final String SERVER_NAME_XML = "Accesslogging";

    @Server(SERVER_NAME_XML)

    public static LibertyServer serverXml;
    public static LibertyServer serverInUse;

    @BeforeClass
    public static void initialSetup() throws Exception {
        LOG.info("Saving original server configuration before modifications.");
        serverXml.saveServerConfiguration();
    }

    @Before
    public void setUp() throws Exception {
        String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
        LOG.logp(Level.INFO, CLASS_NAME, methodName, TEST_SEPARATOR + " TEST: " + methodName + " " + TEST_SEPARATOR);

        serverInUse = serverXml;
        if (!serverInUse.isStarted()) {
            serverInUse.restoreServerConfiguration();
            serverInUse.startServer();
        }
    }

    @After
    public void cleanUp() throws Exception {
        if (serverInUse != null && serverInUse.isStarted()) {
            serverInUse.stopServer();
        }
    }

    /**
     * Tests the default value of the maxFiles property, ensuring that the property is set to 2 even when it is not explicitly defined.
     * The maxFiles value is not set in the XML.
     */
    @Test
    @Mode(FULL)
    public void testmaxFilesDefaultValue() throws Exception {
        LOG.info("Applying test-specific server configuration for maxFilesDefaultValue.");
        serverXml.setServerConfigurationFile("accessLogging/server-rollover-default-maxFiles.xml");
        serverXml.waitForStringInLogUsingMark("CWWKG0017I", 5000);
    
        // Ensure test does not start near the top of a minute to avoid timing issues
        avoidTopOfMinute();
    
        // Wait for 3 rollovers to occur 
        LOG.info("Waiting for 3 log rollovers to complete...");
        Calendar cal = getNextRolloverTime(0, 1);
        for (int i = 0; i < 3; i++) {
            checkForRolledLogsAtTime(cal);
            cal.add(Calendar.MINUTE, 1);
        }

        // Validate that only 2 log files exist default value of maxfiles is 2
        validateMaxBackupFiles(2);
    }

    /**
     * Tests the maxBackupFiles property, ensuring the correct number of log files are retained.
     * Also verifies behavior after a server restart.
     * The maxFiles value is set to 3 in the XML.
     */
    @Test
    @Mode(FULL)
    public void testmaxFilesAfterRestart() throws Exception {
        LOG.info("Applying test-specific server configuration for maxFilesAfterRestart.");
        serverXml.setServerConfigurationFile("accessLogging/server-rollover-maxFiles.xml");
        serverXml.waitForStringInLogUsingMark("CWWKG0017I", 5000);
    
        // Ensure test does not start near the top of a minute to avoid timing issues
        avoidTopOfMinute();
    
        // Wait for 4 rollovers to occur (maxFiles = 3, interval = 1 min)
        LOG.info("Waiting for 4 log rollovers to complete...");
        Calendar cal = getNextRolloverTime(0, 1);
        
        for (int i = 0; i < 4; i++) {
            checkForRolledLogsAtTime(cal);
            cal.add(Calendar.MINUTE, 1);
        }

        // Validate that only 3 log files exist after restart
        validateMaxBackupFiles(3);

        // Restart server and verify again
        LOG.info("Restarting server to verify persistence of maxFiles setting.");
        serverXml.stopServer();
        serverXml.startServer();
        serverXml.waitForStringInLogUsingMark("CWWKG0017I", 5000);
        avoidTopOfMinute();

        // Wait for another 4 rollovers after restart
        cal = getNextRolloverTime(0, 1);

        for (int i = 0; i < 4; i++) {
            checkForRolledLogsAtTime(cal);
            cal.add(Calendar.MINUTE, 1);
        }
        // Validate that only 3 log files exist after restart
        validateMaxBackupFiles(3);
    }

    /*
     * Tests maxFiles = "0", a value of 0 means no limit, just checks if the server started properly
     *  maxFiles="0"
     */
   @Test
   public void testZeroMaxFilesValue() throws Exception {
        LOG.info("Applying test-specific server configuration for ZeroMaxFilesValue.");
        serverXml.setServerConfigurationFile("accessLogging/server-rollover-zero-maxFiles.xml");
        serverXml.waitForStringInLogUsingMark("CWWKG0017I", 5000);

        List<String> lines = serverInUse.findStringsInLogs("CWWKG0017I");
        assertTrue("Maxfiles is not zero", lines.size() > 0);
    }

    /*
     * Tests maxFiles = "i".Should output warning and set maxFiles to default 2
     *  maxFiles="i"
     */
   @Test
   public void testInvalidMaxFilesValue() throws Exception {
        LOG.info("Applying test-specific server configuration for InvalidMaxFilesValue");
        serverXml.setServerConfigurationFile("accessLogging/server-rollover-invalid-maxFiles.xml");
        serverXml.waitForStringInLogUsingMark("CWWKG0017I", 5000);

        serverXml.waitForStringInTraceUsingMark("CWWKG0083W", 10000);
        List<String> lines = serverInUse.findStringsInLogs("CWWKG0083W");
        LOG.logp(Level.INFO, CLASS_NAME, "testInvalidMaxFilesValue", "Found warning: " + lines.toString());
        assertTrue("No CWWKG0083W warning was found indicating that i is an invalid value", lines.size() > 0);

        // Stop the server while ignoring the expected warning "CWWKG0083W". 
        // This warning is expected due to an invalid maxFiles value in the test configuration.
        // Without this, the test would fail during server shutdown because the framework 
        // treats all warnings/errors as failures by default.
        serverInUse.stopServer("CWWKG0083W");   
    }

    private void validateMaxBackupFiles(int maxFiles) throws Exception {
    File logsDir = new File(getLogsDirPath());
    assertTrue("Log directory does not exist: " + logsDir.getAbsolutePath(), logsDir.exists());
    String[] logs = logsDir.list((dir, name) -> name.startsWith(ACCESS_LOG_PREFIX));
    // null check
    int logCount = (logs != null) ? logs.length : 0;
    assertTrue("Expected at most " + maxFiles + " log files, but found " + logCount + ": " + 
               (logs != null ? Arrays.toString(logs) : "No logs found"),
               logCount == maxFiles);
    }

    private static String getLogsDirPath() throws Exception {
        return serverInUse.getDefaultLogFile().getParentFile().getAbsolutePath();
    }
    
    private static void checkForRolledLogsAtTime(Calendar cal) throws Exception {
        LOG.logp(Level.INFO, CLASS_NAME, "checkForRolledLogsAtTime", "The next log rollover is scheduled to be at: " + cal.getTime());

        // Format date prefix without seconds for regex matching
        String datePrefix = FILE_DATE_NO_SECONDS.format(cal.getTime());

        // Regex pattern to match any seconds (two digits) in the log filename
        String regexPattern = ACCESS_LOG_PREFIX + datePrefix + "\\.\\d{2}" + LOG_EXT;

        long timeToFirstRollover = cal.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
        if (timeToFirstRollover > 0)
            Thread.sleep(timeToFirstRollover + FILE_WAIT_SECONDS_PADDING); //sleep until next time the log is set to rollover

        File logsDir = new File(getLogsDirPath());
        
        boolean fileFound = false;
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < POLLING_TIMEOUT) {
            File[] matchingLogs = logsDir.listFiles((dir, name) -> {
                boolean matches = name.matches(regexPattern);
                LOG.info("Checking file: " + name + " matches: " + matches);
                return matches;
            });

            if (matchingLogs != null && matchingLogs.length == 1) {
                fileFound = true;
                break;
            }
            Thread.sleep(POllING_INTERVAL);
        }

        assertTrue("No rolled access log file matching pattern: " + regexPattern + " was found within timeout", fileFound);
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

    /**
     * This is a common 'avoid minute rollover timing issue' method so that all cases benefit from
     * common tuning. By default we avoid the 10 seconds prior to the minute rolling over and the
     * first second of the minute.
     *
     * @throws InterruptedException
     */
    public static void avoidTopOfMinute() throws InterruptedException {
        avoidTopOfMinute(10 * 1000, 1 * 1000);
    }

    /**
     * @param tolerateIfEnoughBeforeMs if > 0 then we only wait if there is less than this ms left in the minute
     * @param avoidAfterMs             if > 0 we will wait this long after the start of the next minute
     * @throws InterruptedException
     */
    public static void avoidTopOfMinute(int tolerateIfEnoughBeforeMs, int avoidAfterMs) throws InterruptedException {

        // How long till the next minute starts?
        int nextMinStartsInMs = 60000 - Calendar.getInstance().get(Calendar.MILLISECOND);

        // Sometimes we only wait if we are 'near' the top to avoid a timing window
        if (tolerateIfEnoughBeforeMs > 0 && nextMinStartsInMs > tolerateIfEnoughBeforeMs) {
            return;
        } else if (Calendar.getInstance().get(Calendar.SECOND) != 0) {
            //wait to do a server config update at the start of the minute
            Thread.sleep(nextMinStartsInMs);
            Thread.sleep(avoidAfterMs); //padding
        }}
    
}

