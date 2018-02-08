/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.fat;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.common.internal.encoder.Base64Coder;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;

/**
 *
 */
public class HandlerTestHelper {

    private static final Class<?> c = HandlerTestHelper.class;

    private static final CountDownLatch lock = new CountDownLatch(1);

    static final int WAIT_SECS = 30;

    public static class LibertyLogsFound {
        boolean sysoutFound = false, auditFound = false, debugFound = false, dumpFound = false,
                        entryFound = false, errorFound = false, eventFound = false, exitFound = false,
                        fatalFound = false, infoFound = false, warningFound = false;
    }

    /**
     * Find all logs from collector.manager_fat.RESTHandlerTraceLogger.
     *
     * @param messageKey
     * @param server
     * @param logFilePath
     * @return
     * @throws Exception
     */
    public static LibertyLogsFound findAllLogsFromRESTHandlerTraceLogger(String messageKey, LibertyServer server, String logFilePath) throws Exception {
        LibertyLogsFound logsFound = new LibertyLogsFound();

        Log.info(c, "findAllLogsFromRESTHandlerTraceLogger", "------> start waiting");
        server.waitForStringInLog("[10]Received", 5000, new RemoteFile(Machine.getLocalMachine(), server.getLogsRoot() + "tracehandlerimpl.log"));
        Log.info(c, "findAllLogsFromRESTHandlerTraceLogger", "------> wait completed");

        List<String> lines = null;
        // try thrice, if not successful
        for (int i = 0; i < 3; i++) {
            // find log with messageKey in tracehandlerimpl.log, after event was pushed from source
            lines = server.findStringsInFileInLibertyServerRoot(messageKey, logFilePath);
            if (!lines.isEmpty()) {
                break;
            }
            Log.info(c, "findAllLogsFromRESTHandlerTraceLogger", "Wait for " + WAIT_SECS + " secs, before trying to find string again");
            lock.await(WAIT_SECS, TimeUnit.SECONDS);
        }

        assertTrue("No Liberty logs found in " + logFilePath, !lines.isEmpty());

        // check for each log level
        for (String string : lines) {
            Log.info(c, "findAllLogsFromRESTHandlerTraceLogger", "--------------> " + string);
            if (string.contains("sysout_log_")) {
                logsFound.sysoutFound = true;
            } else if (string.contains("audit_log_")) {
                logsFound.auditFound = true;
            } else if (string.contains("debug_log_")) {
                logsFound.debugFound = true;
            } else if (string.contains("dump_log_")) {
                logsFound.dumpFound = true;
            } else if (string.contains("entry_log_")) {
                logsFound.entryFound = true;
            } else if (string.contains("error_log_")) {
                logsFound.errorFound = true;
            } else if (string.contains("event_log_")) {
                logsFound.eventFound = true;
            } else if (string.contains("exit_log_")) {
                logsFound.exitFound = true;
            } else if (string.contains("fatal_log_")) {
                logsFound.fatalFound = true;
            } else if (string.contains("info_log_")) {
                logsFound.infoFound = true;
            } else if (string.contains("warning_log_")) {
                logsFound.warningFound = true;
            }
        }

        return logsFound;
    }

    /**
     * Find all logs from collector.manager_fat.RESTHandlerTraceLogger.
     *
     * @param messageKey
     * @param server
     * @param logFilePath
     * @return
     * @throws Exception
     */
    public static LibertyLogsFound verifyLoglevelRawInTraceLogger(String messageKey, LibertyServer server, String logFilePath) throws Exception {
        LibertyLogsFound logsFound = new LibertyLogsFound();

        Log.info(c, "findAllLogsFromRESTHandlerTraceLogger", "------> start waiting");
        server.waitForStringInLog("[10]Received", 5000, new RemoteFile(Machine.getLocalMachine(), server.getLogsRoot() + "tracehandlerimpl.log"));
        Log.info(c, "findAllLogsFromRESTHandlerTraceLogger", "------> wait completed");

        List<String> lines = null;
        // try thrice, if not successful
        for (int i = 0; i < 3; i++) {
            // find log with messageKey in tracehandlerimpl.log, after event was pushed from source
            lines = server.findStringsInFileInLibertyServerRoot(messageKey, logFilePath);
            if (!lines.isEmpty()) {
                break;
            }
            Log.info(c, "findAllLogsFromRESTHandlerTraceLogger", "Wait for " + WAIT_SECS + " secs, before trying to find string again");
            lock.await(WAIT_SECS, TimeUnit.SECONDS);
        }

        assertTrue("No Liberty logs found in " + logFilePath, !lines.isEmpty());
        List<String> rawLevels = new ArrayList<String>();
        rawLevels.add("logLevelRaw=CONFIG");
        rawLevels.add("logLevelRaw=FINE");
        rawLevels.add("logLevelRaw=FINER");
        rawLevels.add("logLevelRaw=FINEST");
        rawLevels.add("logLevelRaw=ENTRY");
        rawLevels.add("logLevelRaw=EXIT");

        // check for each log level
        for (String string : lines) {
            Log.info(c, "findAllLogsFromRESTHandlerTraceLogger", "--------------> 1:" + string);
            if (string.contains("logLevelRaw=")) {
                Iterator<String> lvls = rawLevels.iterator();
                while (lvls.hasNext()) {
                    String level = lvls.next();
                    Log.info(c, "findAllLogsFromRESTHandlerTraceLogger", "----> checking whether raw logger level found or not for the rec(" + string + ")?: " + level + "::"
                                                                         + string.contains(level));
                    if (string.contains(level)) {
                        Log.info(c, "findAllLogsFromRESTHandlerTraceLogger", "----> Found raw loggerlevel : " + level);
                        lvls.remove();
                    }
                }
            }
        }
        assertTrue("whether all types of loggerName(rawLoggerName) values have been found ?", rawLevels.isEmpty());
        /*
         * Iterator<String> lvls = rawLevels.iterator();
         * while (lvls.hasNext())
         * {
         * String level = lvls.next();
         * Log.info(c, "findAllLogsFromRESTHandlerTraceLogger", "----> Remaining key" + level);
         * }
         */
        return logsFound;
    }

    /**
     * Assert all logs are found.
     *
     * @param message
     * @param logsFound
     */
    public static void assertFoundAllLogs(String message, LibertyLogsFound logsFound) {
        if (message == null) {
            message = "%s log for Liberty logging not found";
        }
        // finer
        assertTrue(String.format(message, "debug"), logsFound.debugFound);
        assertTrue(String.format(message, "dump"), logsFound.dumpFound);
        assertTrue(String.format(message, "entry"), logsFound.entryFound);
        assertTrue(String.format(message, "event"), logsFound.eventFound);
        assertTrue(String.format(message, "exit"), logsFound.exitFound);

        // info
        assertTrue(String.format(message, "info"), logsFound.infoFound);
        assertTrue(String.format(message, "audit"), logsFound.auditFound);
        assertTrue(String.format(message, "error"), logsFound.errorFound);
        assertTrue(String.format(message, "fatal"), logsFound.fatalFound);
        assertTrue(String.format(message, "warning"), logsFound.warningFound);
        assertTrue(String.format(message, "sysout"), logsFound.sysoutFound);
    }

    /**
     * Assert fine/trace logs are found, assert info/message logs are NOT found, for liberty logging.
     *
     * @param message
     * @param logsFound
     */
    public static final void assertFoundTraceLogsOnly(String notFoundMessage, String foundMessage, LibertyLogsFound logsFound) {
        if (notFoundMessage == null) {
            notFoundMessage = "%s log for Liberty logging not found";
        }
        if (foundMessage == null) {
            foundMessage = "%s log for Liberty logging found";
        }
        // finer
        assertTrue(String.format(notFoundMessage, "debug"), logsFound.debugFound);
        assertTrue(String.format(notFoundMessage, "dump"), logsFound.dumpFound);
        assertTrue(String.format(notFoundMessage, "entry"), logsFound.entryFound);
        assertTrue(String.format(notFoundMessage, "event"), logsFound.eventFound);
        assertTrue(String.format(notFoundMessage, "exit"), logsFound.exitFound);

        // info
        assertTrue(String.format(foundMessage, "info"), !logsFound.infoFound);
        assertTrue(String.format(foundMessage, "audit"), !logsFound.auditFound);
        assertTrue(String.format(foundMessage, "error"), !logsFound.errorFound);
        assertTrue(String.format(foundMessage, "fatal"), !logsFound.fatalFound);
        assertTrue(String.format(foundMessage, "warning"), !logsFound.warningFound);
        assertTrue(String.format(foundMessage, "sysout"), !logsFound.sysoutFound);
    }

    public static class JULLogsFound {
        boolean sysoutFound = false, configFound = false, enteringFound = false, exitingFound = false,
                        fineFound = false, finerFound = false, finestFound = false, infoFound = false,
                        severeFound = false, warningFound = false, throwingFound = false;
    }

    /**
     * Assert all logs found.
     *
     * @param message
     * @param logsFound
     */
    public static final void assertFoundAllLogs(String message, JULLogsFound logsFound) {
        if (message == null) {
            message = "%s log for JUL not found";
        }

        // finer
        assertTrue(String.format(message, "entering"), logsFound.enteringFound);
        assertTrue(String.format(message, "exiting"), logsFound.exitingFound);
        assertTrue(String.format(message, "fine"), logsFound.fineFound);
        assertTrue(String.format(message, "finer"), logsFound.finerFound);
        assertTrue(String.format(message, "finest"), logsFound.finestFound);
        assertTrue(String.format(message, "throwing"), logsFound.throwingFound);
        // info
        assertTrue(String.format(message, "info"), logsFound.infoFound);
        assertTrue(String.format(message, "config"), logsFound.configFound);
        assertTrue(String.format(message, "severe"), logsFound.severeFound);
        assertTrue(String.format(message, "warning"), logsFound.warningFound);
        assertTrue(String.format(message, "sysout"), logsFound.sysoutFound);
    }

    /**
     * Assert fine/trace logs are found, assert info/message logs are NOT found, for JUL logging.
     *
     * @param message
     * @param logsFound
     */
    public static void assertFoundTraceLogsOnly(String notFoundMessage, String foundMessage, JULLogsFound logsFound) {
        if (notFoundMessage == null) {
            notFoundMessage = "%s log for JUL not found";
        }
        if (foundMessage == null) {
            foundMessage = "%s log for JUL found";
        }

        // Fine/Trace
        assertTrue(String.format(notFoundMessage, "entering"), logsFound.enteringFound);
        assertTrue(String.format(notFoundMessage, "exiting"), logsFound.exitingFound);
        assertTrue(String.format(notFoundMessage, "fine"), logsFound.fineFound);
        assertTrue(String.format(notFoundMessage, "finer"), logsFound.finerFound);
        assertTrue(String.format(notFoundMessage, "finest"), logsFound.finestFound);
        assertTrue(String.format(notFoundMessage, "throwing"), logsFound.throwingFound);
        assertTrue(String.format(notFoundMessage, "config"), logsFound.configFound);

        // Info/Message
        assertTrue(String.format(foundMessage, "info"), !logsFound.infoFound);
        assertTrue(String.format(foundMessage, "severe"), !logsFound.severeFound);
        assertTrue(String.format(foundMessage, "warning"), !logsFound.warningFound);
        assertTrue(String.format(foundMessage, "sysout"), !logsFound.sysoutFound);
    }

    /**
     * Find all logs from TestApp/jul.jsp.
     *
     * @param messageKey
     * @param server
     * @param logFilePath
     * @return
     * @throws Exception
     */
    public static final JULLogsFound findAllLogsForTestAppJUL_jsp(LibertyServer server, String messageKey, String logFilePath) throws Exception {
        JULLogsFound logsFound = new JULLogsFound();

        Log.info(c, "findAllLogsForTestAppJUL_jsp", "------> start waiting");
        server.waitForStringInLog("[11]Received", 5000, new RemoteFile(Machine.getLocalMachine(), server.getLogsRoot() + "tracehandlerimpl.log"));
        Log.info(c, "findAllLogsForTestAppJUL_jsp", "------> wait completed");

        List<String> lines = null;

        // try thrice, if not successful
        for (int i = 0; i < 3; i++) {
            // find log with messageKey in tracehandlerimpl.log, after event pushed from source.
            lines = server.findStringsInFileInLibertyServerRoot(messageKey, logFilePath);
            if (!lines.isEmpty()) {
                break;
            }
            Log.info(c, "findAllLogsForTestAppJUL_jsp", "Wait for " + WAIT_SECS + " secs, before trying to find string again");
            lock.await(WAIT_SECS, TimeUnit.SECONDS);
        }

        assertTrue("No JUL logs found in " + logFilePath, !lines.isEmpty());

        // check for each log level
        for (String string : lines) {
            if (string.contains("sysout for")) {
                logsFound.sysoutFound = true;
            } else if (string.contains("config log for")) {
                logsFound.configFound = true;
            } else if (string.contains("entering_method_for")) {
                logsFound.enteringFound = true;
            } else if (string.contains("exiting_method_for")) {
                logsFound.exitingFound = true;
            } else if (string.contains("fine log for")) {
                logsFound.fineFound = true;
            } else if (string.contains("finer log for")) {
                logsFound.finerFound = true;
            } else if (string.contains("finest log for")) {
                logsFound.finestFound = true;
            } else if (string.contains("info log for")) {
                logsFound.infoFound = true;
            } else if (string.contains("severe log for")) {
                logsFound.severeFound = true;
            } else if (string.contains("warning log for")) {
                logsFound.warningFound = true;
            } else if (string.contains("throwing_method_for")) {
                logsFound.throwingFound = true;
            }
        }

        return logsFound;
    }

    public static int[] allowed_retcode = { 200, 201, 400 };

    public static String getHttpResponseAsString(String urlPath, String username, String password) throws Exception {
        return callURL(urlPath, username, password, 200, allowed_retcode, HTTPRequestMethod.GET, null, "application/json").toString();
    }

    public static StringBuilder callURL(String urlPath, String username, String password, int expected_resp, int[] allowedUnexpectedresp, HTTPRequestMethod verb,
                                        InputStream body, String conttype) throws Exception {
        String m = "callURL";

        Log.info(c, m, "callURL : " + username + "@" + urlPath);

        URL url = new URL(urlPath);
        Map<String, String> headers = new HashMap<String, String>();
        if (conttype != null)
            headers.put("Content-Type", conttype);
        else
            headers.put("Content-Type", "application/json");
        String userpass = username + ":" + password;
        String basicAuth = "Basic " + new String(Base64Coder.base64Encode(userpass.getBytes("UTF-8")), "UTF-8");
        headers.put("Authorization", basicAuth);

        HttpUtils.trustAllCertificates();
        HttpURLConnection conn = null;
        conn = HttpUtils.getHttpConnection(url, expected_resp, allowedUnexpectedresp, 1, verb, headers, body);
        BufferedReader br = HttpUtils.getConnectionStream(conn);
        String line;
        StringBuilder outputBuilder = new StringBuilder();
        while ((line = br.readLine()) != null) {
            outputBuilder.append(line).append('\n'); //.append(System.lineSeparator());
        }
        conn.disconnect();
        return outputBuilder;
    }

    public static void callAndVerifyFFDCEvent(Class c, String testName, LibertyServer server, String path, String errorMessage, String sequenceNo) throws Exception {
        // Set Log Mark
        server.setMarkToEndOfLog(server.getFileFromLibertyServerRoot(HandlerTest.TRACE_LOG));

        HttpURLConnection con;
        con = HttpUtils.getHttpConnectionWithAnyResponseCode(server, path);
        con.connect();
        Log.info(c, testName, "Response code:" + con.getResponseCode());
        con.disconnect();

        final String threadIDLabel = "threadID=";
        String findString;
        List<String> lines;
        String line;
        String threadID;

        // find string in trace.log, after event pushed from source
        findString = "message=" + errorMessage + ",";
        Log.info(c, testName, "testFFDC: waiting for the String " + findString);

        //Adding a wait for string in order to resume the search for string to avoid timeout failures
        String message = server.waitForStringInTraceUsingMark(findString);
        Log.info(c, testName, message);
        lines = server.findStringsInLogsAndTraceUsingMark(findString);
        assertTrue("FFDC event NOT pushed from source in timely fashion",
                   !lines.isEmpty());
        Log.info(c, testName, "FFDC event was successfully pushed from the source");

        // find string in trace.log, after event received in handler
        findString = "Received ffdc event.*message=" + errorMessage;

        //Adding a wait for string in order to resume the search for the string to avoid timeout failures
        message = server.waitForStringInTraceUsingMark(findString);
        Log.info(c, testName, message);
        lines = server.findStringsInLogsAndTraceUsingMark(findString);
        assertTrue("FFDC event NOT received in handler in timely fashion",
                   !lines.isEmpty());

        line = lines.get(0);
        Log.info(c, testName, "Found ffdc event line : " + line);

        // ** Get FFDC ThreadID
        int start = line.indexOf(threadIDLabel);
        int end = line.indexOf(',', start + threadIDLabel.length());
        threadID = line.substring((start + threadIDLabel.length()), end);
        Log.info(c, testName, "FFDC event threadID=" + threadID);

        // ** Verify Sequence Number ( will be next line, because stacktrace dump )
        findString = sequenceNo;
        lines = server.findStringsInLogsAndTraceUsingMark(findString);
        assertTrue("Seqence number not available/not of correct format",
                   !lines.isEmpty());
        Log.info(c, testName, "FFDC event for RuntimeException was successfully received at the handler");
        // *** Verify FFDC threadID with MessageLogData.threadID
        findString = "MessageLogData.*threadId=" + threadID + ",";
        lines = server.findStringsInLogsAndTraceUsingMark(findString);
        assertTrue("FFDC.threadID[" + threadID + "] does not match with MessageLogData threadID : " + line,
                   !lines.isEmpty());
        findString = "objectDetails\\=Object type \\= com\\.ibm\\.ws\\.webcontainer\\.osgi\\.filter\\.WebAppFilterManagerImpl";
        lines = server.findStringsInLogsAndTraceUsingMark(findString);
        assertTrue("Matching ObjectDetails not found in trace.log : " + findString,
                   !lines.isEmpty());

        server.resetLogMarks();
    }

}
