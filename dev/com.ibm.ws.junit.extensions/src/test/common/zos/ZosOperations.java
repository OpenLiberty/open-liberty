/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.common.zos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ZosOperations provides a set of services on z/OS for ztest tests.
 */
public class ZosOperations {

    /**
     * The angel process name.
     */
    protected static final String ANGEL_PROCESS_NAME = "BBGZANGL";

    /**
     * Message indicating that the angel process has fully initialized.
     */
    private static final String ANGEL_STOP_MSG = "CWWKB0057I";

    /**
     * Maximum amount of time to wait for Angel stopped message (ANGEL_STOP_MSG) in seconds.
     */
    private static final int ANGEL_STOP_MAXWAIT_TIME = 5;

    /**
     * Logs informational data.
     * 
     * @param method method name
     * @param informationl data
     */
    public void logInfo(String method, String data) {
        Utils.println(method + ". " + data);
    }

    /**
     * Issues a MVS console operator command to start the angel.
     * 
     * @throws Exception
     */
    public void startAngel() throws Exception {
        startWASProcess(ANGEL_PROCESS_NAME);
    }

    /**
     * Issue MVS console operator command to cancel the angel.
     * 
     * @throws Exception
     */
    public void cancelAngel() throws Exception {
        stopAngel(true);
    }

    /**
     * Issue MVS console operator commands to stop and start the angel.
     * 
     * @throws Exception
     */
    public void restartAngel() throws Exception {
        stopAngel(true);
        startAngel();
    }

    /**
     * Issues a MVS console operator command to stop the angel.
     * 
     * @throws Exception
     */
    public void stopAngel() throws Exception {
        stopAngel(false);
    }

    /**
     * Issues a MVS console operator command to stop the default angel.
     * It checks to ensure the angel is stopped. If it detects that
     * the angel did not stop because of active servers, it will drive a
     * cancel request on each server, then re-attempt the angel stop.
     * 
     * If the stop command did not cause the angel to terminate then a
     * cancel command will be driven on the angel.
     * 
     * @param ensureStopped will cancel active servers if true and proceed to
     *            ensure the angel terminates. First with a stop and then, if needed, a
     *            cancel command against the angel. If ensureStopped is false a stop is
     *            issued against the angel and control is returned.
     * @throws Exception
     */
    public void stopAngel(boolean ensureStopped) throws Exception {
        stopAngel(ANGEL_PROCESS_NAME, ensureStopped);
    }

    /**
     * Issues a MVS console operator command to stop an angel.
     * It checks to ensure the angel is stopped. If it detects that
     * the angel did not stop because of active servers, it will drive a
     * cancel request on each server, then re-attempt the angel stop.
     * 
     * If the stop command did not cause the angel to terminate then a
     * cancel command will be driven on the angel.
     * 
     * @param jobName The job name given to the angel.
     * @param ensureStopped will cancel active servers if true and proceed to
     *            ensure the angel terminates. First with a stop and then, if needed, a
     *            cancel command against the angel. If ensureStopped is false a stop is
     *            issued against the angel and control is returned.
     * @throws Exception
     */
    public void stopAngel(String jobName, boolean ensureStopped) throws Exception {
        String method = "stopAngel";
        logInfo(method, "ensureStopped: " + Boolean.toString(ensureStopped));

        if (false == ensureStopped) {
            stopWASProcess(jobName);
        } else {
            // Messages to search for in angel's msg log.
            String[] serverActiveMsg = { "CWWKB0052I ACTIVE SERVER ASID" };

            // To verify that the active server is not us, the current address space, 
            // we need to compare the ASID from the CWWKB0052I message against ours.
            //
            // In the ztest environment, the server is the ztest process.  
            String currentASID = getCurrentASID(getPid());

            boolean done = false;
            int attempts = 0;

            while (!done && attempts < 10) {
                attempts++;

                // Issue stop...wait a little and see if the Angel is still around.
                Date stopStart = new Date();
                Thread.sleep(1000); // need to delay a second so the jesmsglg searches can see a difference

                if (stopServerAndWaitForMessage(jobName, ANGEL_STOP_MSG, ANGEL_STOP_MAXWAIT_TIME) == 1) {
                    logInfo(method, "stopServerAndWaitForMessage verified Angel is down at last stop issued after: "
                                    + Utils.tsFormatZone.format(stopStart));

                    done = true;
                    break;
                }

                logInfo(method, "Angel not stopped ... checking for active servers, attempt:" + attempts);

                // Stop request did not bring Angel down.
                ZWASJoblogReader logReader = getJoblog(jobName);
                if (logReader != null) {
                    // Angel still around.  Check Angel's JESMSGLG for CWWKB0052I msgs.  They indicate that active servers
                    // exist and the Angel won't honor the stop command.
                    logInfo(method, "Fast-forward jesmsglg to: " + Utils.tsFormatZone.format(stopStart));

                    boolean weAreRegisteredWithAngel = false;

                    for (;;) {
                        String msgEntry = null;
                        msgEntry = logReader.findJesmsglgRecord(stopStart, null, serverActiveMsg);

                        if (msgEntry != null) {
                            logInfo(method, "Active server msg found: " + msgEntry);

                            // Build a cancel command for the "current" active server preventing the Angel from Stopping.
                            //
                            // Need to parse the msg and build/issue the cancel "c jobname,a=asid"
                            //   14.27.39 STC00096  CWWKB0052I ACTIVE SERVER ASID 45 JOBNAME MSTONE17  
                            //   Should build cmd "c MSTONE17,a=45" for the above.
                            String asid = parseMSG_CWWKB0052I(msgEntry, MSG52_ASID_HEX);
                            if (!!!currentASID.equals(asid)) {
                                String jobname = parseMSG_CWWKB0052I(msgEntry, MSG52_JOBNAME);

                                cancelWASProcess(jobname + ",a=" + asid);
                                Thread.sleep(3000);
                                break;
                            } else {
                                weAreRegisteredWithAngel = true;

                                logInfo(method, "Active server was us, skipping a cancel of ourself.  My ASID: " + currentASID);
                                continue;
                            }
                        } else {
                            logInfo(method, "No active servers (other than us) found.  Issue a cancel of the angel");

                            cancelWASProcess(jobName);
                            Thread.sleep(3000);

                            // If we were the reason that Angel couldn't stop then skip the whining
                            if (weAreRegisteredWithAngel) {
                                done = true;
                                break;
                            } else {
                                // Don't know why Angel didn't stop.
                                throw new Exception("ZosOperationsFat.stopAngel. Angel still up after stop command. No active servers found");
                            }
                        }
                    }
                } else {
                    // Angel not around....good.
                    done = true;
                }
            }

            // Cancel angel if its still around
            ZWASJoblogReader logReader = getJoblog(jobName);
            if (logReader != null) {
                cancelWASProcess(jobName);
            }
        }
    }

    /**
     * CWWKB0052I messages parsing.
     */
    static final int MSG52_MSG_TIME = 1;
    static final String rx_MSG52_TIME = "(\\d{2}\\.\\d{2}\\.\\d{2})";

    static final int MSG52_JOBID = 2;
    static final String rx_MSG52_JOBID = "([\\w]*)";

    static final int MSG52_ASID_HEX = 3;
    static final String rx_MSG52_ASID_HEX = "([A-Fa-f0-9]*)";

    static final int MSG52_JOBNAME = 4;
    static final String rx_MSG52_JOBNAME = "([\\w]*)";

    // "  14.27.39 STC00096  CWWKB0052I ACTIVE SERVER ASID 45 JOBNAME MSTONE17  "
    // "[ ](\\d{2}\\.\\d{2}\\.\\d{2})[ ]([\\w]*)[\\s]*CWWKB0052I ACTIVE SERVER ASID[ ]([A-Fa-f0-9]*)[ ]JOBNAME[ ]([\\w]*)[\\w\\W]*"
    private static final Pattern _jesmsglog_MSG52_Pattern =
                    Pattern.compile("[ ]" + rx_MSG52_TIME + "[ ]" + rx_MSG52_JOBID + "[\\s]*CWWKB0052I ACTIVE SERVER ASID[ ]" +
                                    rx_MSG52_ASID_HEX + "[ ]JOBNAME[ ]" + rx_MSG52_JOBNAME + "[\\w\\W]*");

    /**
     * Parse the CWWKB0052I message and return the specified data grouping.
     * 
     * 14.27.39 STC00096 CWWKB0052I ACTIVE SERVER ASID 45 JOBNAME MSTONE17
     * 
     * @param CWWKB0052I message
     * @param retGroup the data group index (e.g. MSG52_ASID_HEX, MSG52_JOBNAME, etc).
     * @return The requested data group, or null if no match.
     */
    public String parseMSG_CWWKB0052I(String traceRecord, int retGroup)
    {
        String method = "parseMSG_CWWKB0052I";

        if (traceRecord != null)
        {
            String retString = null;

            logInfo(method, "Pattern:\n" + _jesmsglog_MSG52_Pattern);

            logInfo(method, "traceRecord:\n" + traceRecord);

            logInfo(method, "retGroup:\n" + (new Integer(retGroup)).toString());

            Matcher m = _jesmsglog_MSG52_Pattern.matcher(traceRecord);

            if (m.matches())
            {
                retString = m.group(retGroup);

                logInfo(method, "retString for group:" + retGroup + ", is:" + retString);
                return retString;
            }
        }
        return null;
    }

    /**
     * Issues a MVS operator (console) command.
     * 
     * @param cmd e.g: "s bbgzsrv"
     * @return The process.
     * @throws Exception
     */
    public Process executeMVSConsoleCommand(String cmd) throws Exception {
        return executeMVSConsoleCommandCommon("\"" + cmd + "\"");
    }

    /**
     * Issues a MVS operator (console) command.
     * 
     * @param cmd e.g: "s bbgzsrv"
     * @return The process.
     * @throws Exception
     */
    public Process executeMVSConsoleCommandWithoutQuotes(String cmd) throws Exception {
        return executeMVSConsoleCommandCommon(cmd);
    }

    private Process executeMVSConsoleCommandCommon(String cmd) throws Exception {
        String method = "executeMVSConsoleCommandCommon";

        String newCmd = cmd.replaceAll("'", "''");
        logInfo(method, "Entry. Command to issue: " + newCmd);

        String[] cmdArr = new String[] { "/usr/local/bin/execsh", "exconcmd", newCmd };

        Process process = Runtime.getRuntime().exec(cmdArr);

        process.waitFor();
        logInfo(method, "Input stream: " + getStreamData(process.getInputStream()));
        logInfo(method, "Error stream: " + getStreamData(process.getErrorStream()));;

        logInfo(method, "Exit.");
        return process;
    }

    /**
     * Execute a tsocmd - tsocmd "<cmd>"
     * 
     * @return the cmd Process
     */
    public Process executeTsocmd(String cmd) throws IOException, InterruptedException {
        String method = "executeTsocmd";

        logInfo(method, "Entry. tsocmd to issue: " + cmd);

        Process process = Runtime.getRuntime().exec(new String[] { "tsocmd", cmd });
        process.waitFor();
        logInfo(method, "Input stream: " + getStreamData(process.getInputStream()));
        logInfo(method, "Error stream: " + getStreamData(process.getErrorStream()));;

        logInfo(method, "Exit.");

        return process;
    }

    /**
     * Purge all closed sysout segments.
     * 
     * @return the tsocmd process
     */
    public Process purgeSyslog() throws IOException, InterruptedException {
        return executeTsocmd("exec 'zwasteam.exec(psyslog)'");
    }

    /**
     * Issues a MVS console operator command to start the specified WAS process.
     * 
     * @param processName The WAS process name to start.
     * 
     * @throws Exception
     */
    public void startWASProcess(String processName) throws Exception {
        String method = "startWASProcess";

        logInfo(method, "Entry. ProcessName: " + processName);
        String command = "s " + processName;
        Process process = executeMVSConsoleCommand(command);
        int rc = process.exitValue();
        if (rc != 0) {
            String extMsg = ".\nErrorStream:\n" + process.getErrorStream() + ".\nInputStream:\n" + process.getOutputStream();
            throw new Exception("The attempt to start " + processName + " failed with return code: " + rc + extMsg);
        }

        logInfo(method, "Exit.");
    }

    /**
     * Issues a MVS console operator command to stop the specified WAS process.
     * 
     * @param processName The WAS process name to stop.
     * 
     * @throws Exception
     */
    public void stopWASProcess(String processName) throws Exception {
        String method = "stopWASProcess";

        logInfo(method, "Entry. ProcessName: " + processName);

        String command = "p " + processName;
        Process process = executeMVSConsoleCommand(command);
        int rc = process.exitValue();
        if (rc != 0) {
            String extMsg = ".\nErrorStream:\n" + process.getErrorStream() + ".\nInputStream:\n" + process.getOutputStream();
            throw new Exception("The attempt to stop " + processName + "failed with return code: " + rc + extMsg);
        }

        logInfo(method, "Exit");
    }

    /**
     * Start a z/OS process and waits for the input message (ex. "CWWKB0056I" for angel) to appear in the process' jes message log.
     * 
     * @throws Exception If the started message could not be found.
     */
    public void startServerAndWaitForMessage(String serverName, String validationString, int waitTime) throws Exception {
        startWASProcess(serverName);

        String msgEntry = waitForStringInJobLog(serverName, waitTime, validationString);
        if (msgEntry == null) {
            throw new Exception("The " + serverName + " process could not be verified to be active after waiting for " +
                                waitTime + " seconds for message \"" + validationString + "\".");

        }
    }

    /**
     * Stop a z/OS process and waits for the message (ex. "CWWKE0036I") to appear in the process' jes message log.
     * 
     * @param serverName target process jobname to stop.
     * @param validationString the strings to search for in the JESMSGLG to verify stop.
     * @param waitTime maximum time in seconds to allow for stop.
     * @return 0 if server process stop could not be verified, 1 if the server stop was verified with either the input
     *         validationString or detecting the process as inactive.
     */
    public int stopServerAndWaitForMessage(String serverName, String validationString, int waitTime) throws Exception {
        int localRC = 1;
        stopWASProcess(serverName);

        String msgEntry = waitForStringInJobLog(serverName, null, true, waitTime, validationString);

        // If we didn't find the expected messages
        if (msgEntry == null) {
            String jobId = getActiveJobId(serverName);
            // If the target process is still active then we must have timed out.
            if ((jobId != null) && !jobId.equals("")) {
                localRC = 0;
            }
        }

        return localRC;
    }

    /**
     * Issues a MVS console operator command to cancel the specified WAS process.
     * 
     * @param processName The WAS process to cancel.
     * 
     * @throws Exception
     */
    public void cancelWASProcess(String processName) throws Exception {
        String method = "cancelWASProcess";
        logInfo(method, "Entry. ProcessName: " + processName);

        Process process = executeMVSConsoleCommand("c " + processName);
        int rc = process.exitValue();
        if (rc != 0) {
            String extMsg = ".\nErrorStream:\n" + process.getErrorStream() + ".\nInputStream:\n" + process.getOutputStream();
            throw new Exception("The attempt to cancel " + processName + " failed with return code: " + rc + extMsg);
        }

        logInfo(method, "Exit");
    }

    public static String getStreamData(InputStream ins) throws IOException {
        boolean markSupported = ins.markSupported();
        if (markSupported) {
            ins.mark(ins.available());
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(ins));

        String output = "";
        String line = reader.readLine();
        while (line != null) {
            output += line + "\n";
            line = reader.readLine();
        }

        if (markSupported) {
            ins.reset();
        }

        return output;
    }

    /**
     * 
     * @return The SYSLOG jobId, or null if it could not be obtained.
     * 
     * @throws InterruptedException
     */
    public static String getSyslogJobId() throws InterruptedException, IOException {

        Process process = Runtime.getRuntime().exec(new String[] { "/usr/local/bin/sysout", "-r", "SYSLOG" });
        List<String> out = new WaitForProcess().waitFor(process).getStdout();
        return (out.size() > 0) ? out.get(0).trim() : null;
    }

    /**
     * Retrieve the active syslog, without using simplicity APIs,
     * which aren't (apparently) available to unit tests.
     * 
     * @return the syslog as a List<String>; or null if the syslog could not be obtained.
     */
    public static List<String> getSyslogRaw() throws IOException, InterruptedException {
        return getJoblogRaw(getSyslogJobId());
    }

    /**
     * Retrieves the joblog matching the specified job id.
     * 
     * @param jobId The job id (i.e: STC00XXX).
     * 
     * @return The joblog as a List<String>, one entry per line.
     * 
     * @throws IOException, InterruptedException
     */
    public static List<String> getJoblogRaw(String jobId) throws IOException, InterruptedException {

        if (jobId == null || jobId.length() == 0) {
            return null;
        }

        Process process = Runtime.getRuntime().exec(new String[] { "/usr/local/bin/sysout", "-o", jobId });
        return new WaitForProcess().waitFor(process).getStdout();
    }

    /**
     * @return true if the given String is in the given log.
     */
    public boolean isStringInLog(String findMe, List<String> inLog) {

        for (String s : ((inLog != null) ? inLog : new ArrayList<String>())) {
            if (s != null && s.contains(findMe)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return true if the string was found; false if a timeout occurred.
     */
    public boolean waitForStringInJobLog(String jobId,
                                         String findMe,
                                         int timeout_s) throws InterruptedException, IOException {
        return waitForStringInJobLog(jobId, findMe, timeout_s, null);
    }

    /**
     * @return true if the string was found; false if a timeout occurred.
     */
    public boolean waitForStringInJobLog(String jobId,
                                         String findMe,
                                         int timeout_s,
                                         List<String> initialLog) throws InterruptedException, IOException {

        List<String> joblog = (initialLog != null) ? initialLog : getJoblogRaw(jobId);

        for (int i = 0; i < timeout_s; ++i) {
            if (isStringInLog(findMe, joblog)) {
                return true;
            }
            Thread.sleep(1000 * 1);
            joblog = getJoblogRaw(jobId);
        }

        return isStringInLog(findMe, joblog);
    }

    /**
     * Waits for the joblog line containing the desired string.
     * 
     * @return String The entry in the joblog that contains the desired string.
     */

    /**
     * Returns the joblog entry that contains the desired string.
     * 
     * @param jobId The jobId of the WAS process associated with the joblog to be read.
     * @param stringToFind The string to find in the joblog.
     * @param forwardRead True if the joblog entries are searched starting with the first entry. False, if the the joblog entries are searched starting with the last entry.
     * @param timeout The timeout in seconds.
     * @return Returns the joblog entry that contains the desired string. Null if the desired string is not found.
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    public String waitForJobLogLineContainingString(String jobId,
                                                    String stringToFind,
                                                    boolean forwardRead,
                                                    int timeout) throws InterruptedException, IOException {

        List<String> joblogEntries = ZosOperationsFat.getJoblogRaw(jobId);

        // If there is nothing to read, we are done.
        if (joblogEntries == null) {
            return null;
        }

        for (int i = 0; i < timeout; ++i) {
            if (forwardRead) {
                for (String s : joblogEntries) {
                    if (s != null && s.contains(stringToFind)) {
                        return s;
                    }
                }
            } else {
                ListIterator<String> iter = joblogEntries.listIterator(joblogEntries.size());
                while (iter.hasPrevious()) {
                    String s = iter.previous();
                    if (s != null && s.contains(stringToFind)) {
                        return s;
                    }
                }
            }

            // We have not found the desired string. sleep and read the joblog again.
            Thread.sleep(1000 * 1);
            joblogEntries = ZosOperationsFat.getJoblogRaw(jobId);
        }

        return null;
    }

    /**
     * Retrieves the joblog for the active process matching the input job name.
     * 
     * @param jobname The jobname (e.g: "BBGZANGL").
     * @return A custom reader wrapped around the joblog.
     * @throws Exception
     */
    public ZWASJoblogReader getJoblog(String jobName) throws Exception {
        String method = "getJoblog";

        logInfo(method, "Entry. JobName: " + jobName);
        String joblog = null;
        ZWASJoblogReader logReader = null;

        List<String> joblogLines = getActiveJoblog(jobName);
        if (joblogLines != null) {
            for (String line : joblogLines)
                joblog = (joblog != null) ? joblog + line + "\n" : line + "\n";

            logReader = new ZWASJoblogReader(joblog);
        }

        logInfo(method, "Exit. ZWASJoblogReader: " + logReader);
        return logReader;
    }

    /**
     * Gets the job log of the active process matching the specified job name.
     * 
     * @param jobName the job name.
     * 
     * @return The joblog or null.
     * @throws Exception
     */
    public List<String> getActiveJoblog(String jobName) throws Exception {
        String method = "getActiveJoblog";
        logInfo(method, "Entry. JobName: " + jobName);
        List<String> joblog = null;
        String jobId = null;

        Process process = Runtime.getRuntime().exec(new String[] { "/usr/local/bin/sysout", "-r", jobName });
        WaitForProcess wrappedProcess = new WaitForProcess().waitFor(process);
        List<String> stdout = wrappedProcess.getStdout();
        List<String> stderr = wrappedProcess.getStderr();

        if (process.exitValue() == 0) {
            jobId = (stdout != null && stdout.size() > 0) ? stdout.get(0) : null;
            if (jobId != null && !jobId.equals("")) {
                logInfo(method, "jobId from stdout: " + jobId);

                process = Runtime.getRuntime().exec(new String[] { "/usr/local/bin/sysout", "-o", jobId });
                wrappedProcess = new WaitForProcess().waitFor(process);
                stdout = wrappedProcess.getStdout();
                stderr = wrappedProcess.getStderr();

                if (process.exitValue() == 0) {
                    joblog = stdout;
                } else {
                    logInfo(method, "get jobIds failed stdout: " + stdout);
                    logInfo(method, "get jobIds failed stderr: " + stderr);
                }
            }
        } else {
            logInfo(method, "get jobName failed stdout: " + stdout);
            logInfo(method, "get jobName failed stderr: " + stderr);
        }

        logInfo(method, "Exit. JobId: " + jobId);
        return joblog;
    }

    /**
     * Gets the job id of the active process matching the specified job name.
     * 
     * @param jobname the job name.
     * 
     * @return The job ID (i.e. STC00XXX).
     * @throws Exception
     */
    public String getActiveJobId(String jobName) throws Exception {
        String method = "getActiveJobId";
        logInfo(method, "Entry. JobName: " + jobName);

        String jobId = null;

        Process process = Runtime.getRuntime().exec(new String[] { "/usr/local/bin/sysout", "-r", jobName });
        WaitForProcess wrappedProcess = new WaitForProcess().waitFor(process);
        List<String> stdout = wrappedProcess.getStdout();

        if (process.exitValue() == 0) {
            jobId = (stdout != null && stdout.size() > 0) ? stdout.get(0) : null;
            if (jobId != null && !jobId.equals("")) {
                logInfo(method, "jobId from stdout: " + jobId);
            }
        }

        logInfo(method, "Exit. JobId: " + jobId);
        return jobId;
    }

    /**
     * Waits for the specified string(s) to appear in a jes message log entry for the active
     * process matching the input job name. The log will be checked at least once.
     * 
     * @param jobname The process job name.
     * @param secondsToWait The amount of seconds to wait.
     * @param findValues The string(s) to search for.
     * 
     * @return The jes message entry containing the matched string(s) or NULL if not found.
     * @throws Exception
     */
    public String waitForStringInJobLog(String jobname, int secondsToWait, String... findValues) throws Exception {
        return waitForStringInJobLog(jobname, null, false, secondsToWait, findValues);
    }

    /**
     * Waits for the specified string(s) to appear in a jes message log entry for the active
     * process matching the input job name. The log will be checked at least once.
     * 
     * @param jobname The process job name.
     * @param t1 starting Date within the ZWASJoblogReader to start searching
     *            for a match or null
     * @param bailOnInactive return if the target process is not active.
     * @param secondsToWait The amount of seconds to wait.
     * @param findValues The string(s) to search for.
     * 
     * @return The jes message entry containing the matched string(s) or NULL if not found or target process terminated.
     * @throws Exception
     */
    public String waitForStringInJobLog(String jobname, Date t1, boolean bailOnInactive, int secondsToWait, String... findValues) throws Exception {
        String method = "waitForStringInJobLog";

        logInfo(method, "Entry. JobName: " + jobname + ", secondsToWait: " + secondsToWait + ", starting at: " + t1);
        String msgEntry = null;
        int itererations = (secondsToWait <= 0) ? 1 : secondsToWait;
        ZWASJoblogReader logReader = null;
        for (int i = 0; i < itererations; i++) {
            logReader = getJoblog(jobname);
            if (logReader != null) {
                msgEntry = logReader.findJesmsglgRecord(t1, null, findValues);
                if (msgEntry != null) {
                    break;
                }
            } else if (true == bailOnInactive) {
                break;
            }

            Thread.sleep(1000);
        }

        // Check if the address space is up if expected to be.
        if ((logReader == null) && (false == bailOnInactive)) {
            throw new Exception("Unable to find job log for " + jobname + ". The process is not active.");
        }

        logInfo(method, "Exit. Message Entry:  " + msgEntry);
        return msgEntry;
    }

    /**
     * Get the PID for the current process. This implementation spawns a process that gets
     * its parent's PID (ie. our process's PID).
     * 
     * @return the process identifier as a string
     */
    private String getPid() throws IOException {
        List<String> args = new ArrayList<String>();
        args.add("/bin/sh");
        args.add("-c");
        args.add("/bin/echo $PPID");

        // Create the process
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        Process pidProcess = processBuilder.redirectErrorStream(true).start();

        // Wrap the input stream with a reader
        InputStream pidInputStream = pidProcess.getInputStream();
        InputStreamReader pidInputStreamReader = new InputStreamReader(pidInputStream);
        BufferedReader pidReader = new BufferedReader(pidInputStreamReader);

        // The PID should be the only line of output
        String pid = pidReader.readLine();

        // Eat all output after the first line
        while (pidReader.readLine() != null);
        pidReader.close();

        // Get the return code from the process
        int returnCode = -1;
        try {
            returnCode = pidProcess.waitFor();
        } catch (InterruptedException ie) {
            throw new IOException(ie);
        } finally {
            pidProcess.getOutputStream().close();
            pidProcess.getErrorStream().close();
        }

        if (returnCode != 0 || !pid.matches("\\d+")) {
            throw new IOException("Unable to acquire process ID");
        }

        return pid;
    }

    /**
     * Get the ASID for the current process.
     * 
     * @return the ASID in hex string
     */
    public static String getCurrentASID(String pid) {
        List<String> args = new ArrayList<String>();
        args.add("/bin/ps");
        args.add("-o"); // Format options
        args.add("xasid");
        args.add("-p"); // Process ID
        args.add(pid);

        String asid = null;
        int returnCode = -1;

        try {
            // Create the process
            ProcessBuilder processBuilder = new ProcessBuilder(args);
            Process psProcess = processBuilder.redirectErrorStream(true).start();

            // Wrap the input stream with a reader
            InputStream pidInputStream = psProcess.getInputStream();
            InputStreamReader pidInputStreamReader = new InputStreamReader(pidInputStream);
            BufferedReader psReader = new BufferedReader(pidInputStreamReader);

            // The asid should be after the line "ASID"        
            String line;
            boolean saveNext = false;
            while ((line = psReader.readLine()) != null) {
                if (saveNext) {
                    saveNext = false;
                    asid = line;
                }
                if (line.startsWith("ASID")) {
                    saveNext = true;
                }
            }
            psReader.close();

            // Get the return code from the process
            returnCode = -1;
            try {
                returnCode = psProcess.waitFor();
            } catch (InterruptedException ie) {
                throw new IOException(ie);
            } finally {
                psProcess.getOutputStream().close();
                psProcess.getErrorStream().close();
            }

        } catch (IOException ioe) {
            throw new RuntimeException("Unable to acquire process ASID", ioe);
        }

        if (returnCode != 0 || (asid == null)) {
            throw new RuntimeException("Unable to acquire process ASID");
        }

        return asid.trim();
    }

}
