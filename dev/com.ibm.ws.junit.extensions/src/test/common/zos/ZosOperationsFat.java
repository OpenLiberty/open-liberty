/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.common.zos;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

/**
 * ZosOperationsFat provides a set of services on z/OS for zfat FAT tests.
 * FATs can use simplicity stuff but ztest can not.
 */
public class ZosOperationsFat extends ZosOperations {

    private final int JOB_SEARCH_TIMEOUT; // seconds

    public ZosOperationsFat() {
        this(5);
    }

    public ZosOperationsFat(int jobSearchTimeoutSeconds) {
        JOB_SEARCH_TIMEOUT = jobSearchTimeoutSeconds;
    }

    /**
     * This class' reference used for logging.
     */
    private static final Class<?> c = ZosOperationsFat.class;

    /**
     * Logs informational data.
     *
     * @param method method name
     * @param informationl data
     */
    @Override
    public void logInfo(String method, String data) {
        Log.info(c, method, data);
    }

    /**
     * Retrieves the active syslog.
     *
     * @return A custom reader wrapped around the SYSLOG.
     * @throws Exception
     */
    public ZWASJoblogReader getSyslog() throws Exception {
        String method = "getSyslog";

        logInfo(method, "Entry");

        ZWASJoblogReader joblog = null;

        Machine machine = Machine.getLocalMachine();
        ProgramOutput jobIds = machine.execute(
                                               "/usr/local/bin/sysout -r SYSLOG");
        logInfo(method, "jobIds stdout: " + jobIds.getStdout());
        logInfo(method, "jobIds stderr: " + jobIds.getStderr());

        String jobId = jobIds.getStdout();
        if (jobId != null && !jobId.equals("")) {
            ProgramOutput jobout = machine.execute("/usr/local/bin/sysout -o " + jobId);

            joblog = new ZWASJoblogReader(jobout.getStdout());
        }

        logInfo(method, "Exit, JobLog: " + joblog);
        return joblog;
    }

    /**
     * Executes a command line command.
     *
     * @param cmd The command to execute.
     * @param params The parameters for the command to execute.
     *            This input can be null.
     * @param workingDir The location where the command is to be executed.
     *            This input can be null.
     * @return the command execution output.
     * @throws Exception.
     */
    public String executeCommandLineCmd(String cmd, String[] params,
                                        String workingDir) throws Exception {
        String method = "executeCommandLineCmd";

        logInfo(method, "Entry. Command: " + cmd + ", WorkingDir: " + workingDir + ", params: ");

        if (params != null) {
            StringBuffer parmsString = new StringBuffer();
            for (int i = 0; i < params.length; i++)
                parmsString.append(params[i] + ". ");

            logInfo(method, parmsString.toString());
        }

        if (cmd == null || cmd.equals("")) {
            String s = "executeCommandLineCmd. Invalid command parameter: " + cmd;
            logInfo(method, s);
            throw new Exception(s);
        }

        Machine machine = Machine.getLocalMachine();
        ProgramOutput output = null;

        if (params == null)
            output = (workingDir != null) ? machine.execute(cmd, workingDir) : machine.execute(cmd);
        else if (workingDir == null)
            output = machine.execute(cmd, params);
        else
            output = machine.execute(cmd, params, workingDir);

        String stdout = output.getStdout();
        String stderr = output.getStderr();

        // Check Results.
        if (output.getReturnCode() == 0) {
            if (stderr.matches("[a-zA-Z]+")) {
                logInfo(method, "executeCommandLineCmd. Return Code = 0. However, " +
                                "there maybe an error recorded. Processing will " +
                                "continue.\nInformation:\nStdErrTrace: " + stderr +
                                "\nStdOutTrace: " + stdout);
            }
        } else {
            String s = "executeCommandLineCmd. Failed command line execution. " +
                       "InputData: cmd: " + cmd + ", params: " + params + ", workingDir: " +
                       workingDir + ".\nStdErrTrace: " + stderr +
                       ".\nStdOutTrace: " + stdout;
            logInfo(method, s);

            throw new Exception(s);
        }

        // Try to be reasonable with the output here.  Don't print more than about
        // 200 lines.
        String truncationString = "";
        String traceStdout = stdout;
        if ((stdout != null) && (stdout.length() > (80 * 200))) {
            truncationString = " (" + stdout.length() + " chars, truncated)";
            traceStdout = stdout.substring(0, 80 * 200);
        }
        logInfo(method, "Exit. Cmd: " + cmd + ". Output" + truncationString + ": " + traceStdout);
        return stdout;
    }

    /**
     * Retrieves the most recent job id.
     *
     * @param jobname The job name whose most recent id is to be retrieved.
     *
     * @return A job ID (i.e. STC00XXX).
     * @throws Exception
     *
     *             NOTE: The jobids at the beginning of the list could be older than
     *             ones later in the list. This can happen if the STC numbers wrap.
     *             Going to see if sysout -s can be changed.
     */
    public String getMostRecentJobId(String jobName) throws Exception {
        String method = "getMostRecentJobId";

        logInfo(method, "Entry. JobName: " + jobName);
        String mostRecentJobId = null;
        String result = executeCommandLineCmd("/usr/local/bin/sysout -s " + jobName, null, null);

        // Sysout -s sorts the jobids in descending order prior to display.
        String[] ids = result.split("\\n");
        if (ids.length > 0) {
            mostRecentJobId = ids[0];
        }

        logInfo(method, "Exit. JobId: " + mostRecentJobId);
        return mostRecentJobId;
    }

    /**
     * Retrieves the joblog matching the specified job id.
     *
     * @param jobId The job id (i.e: STC00XXX).
     * @return A custom reader wrapped around the joblog.
     * @throws Exception
     */
    public ZWASJoblogReader getJoblogWithID(String jobId) throws Exception {
        String method = "getJoblogWithID";

        logInfo(method, "Entry. JobId: " + jobId);

        ZWASJoblogReader logReader = null;

        if (jobId != null && !jobId.equals("")) {
            String sOutput = executeCommandLineCmd("/usr/local/bin/sysout -o " + jobId, null, null);
            logReader = new ZWASJoblogReader(sOutput);
        }

        logInfo(method, "Exit. ZWASJoblogReader: " + logReader);
        return logReader;
    }

    /**
     * Wait for a new JobID, after the given prevJobId, to show up for the given jobname.
     *
     * @return the new JobID.
     */
    public String waitForNewJobId(String jobName, String prevJobId, int timeout_s) throws Exception {

        String newJobId = getMostRecentJobId(jobName);
        logInfo("waitForNewJobId", "jobName=" + jobName + ", prevJobId=" + prevJobId + ", newJobId=" + newJobId);

        for (int i = 0; i < timeout_s && (newJobId == null || newJobId.equals(prevJobId)); ++i) {
            Thread.sleep(1 * 1000);
            newJobId = getMostRecentJobId(jobName);
            logInfo("waitForNewJobId", "jobName=" + jobName + ", prevJobId=" + prevJobId + ", newJobId=" + newJobId);
        }

        return newJobId;
    }

    /**
     * @return the jobId, if the job has yet to finish by the time this method is called;
     *         otherwise, if the job had already finished, this method returns null.
     */
    public String waitForJobToFinish(String jobName, int timeout_s) throws Exception {
        String jobId = getActiveJobId(jobName);
        logInfo("waitForJobToFinish", "jobName=" + jobName + ", jobId=" + jobId);

        for (int i = 0; i < timeout_s && jobId != null; ++i) {
            Thread.sleep(1 * 1000);
            jobId = getActiveJobId(jobName);
            logInfo("waitForJobToFinish", "jobName=" + jobName + ", jobId=" + jobId);
        }

        return jobId;
    }

    /**
     * Waits for a started task to appear and have a STC ID (would also work for jobs).
     *
     * @return The STC ID
     */
    public static String waitForSTC(ZosOperationsFat zosOp, String stcName, int timeoutSeconds) throws Exception {
        String stcId = null;
        for (int x = timeoutSeconds; ((stcId == null) && (x > 0)); x--) {
            stcId = zosOp.getActiveJobId(stcName);
            if ((stcId == null) || (stcId.trim().length() == 0)) {
                stcId = null;
                Thread.sleep(1000);
            }
        }
        return stcId;
    }

    /**
     * Wait for some text to appear in a joblog / STC log, up to a timeout.
     *
     * @return the entire joblog
     */
    public String waitForOutputInLog(String jobId, String text, int timeoutInSeconds) throws Exception {
        // This loop is somewhat silly, unfortunately.  We want to loop until we have
        // job output.  However, sometimes the job output will seem available, but is
        // not complete (sections are missing).  If the caller has provided text to
        // search for, we will wait to see if that text appears in the log, and if not,
        // we'll keep waiting, up to our timeout.  The caller still needs to check if
        // that output appears in the log.
        int i = 0;
        ZWASJoblogReader myJobLogReader = getJoblogWithID(jobId);
        String myJobLog = myJobLogReader.getRawJobLog();
        String textClue = (text != null) ? text : " ";
        while (((myJobLog == null) || (myJobLog.isEmpty()) || (myJobLog.contains(textClue) == false))
               && (i++ < JOB_SEARCH_TIMEOUT)) { // give it a few seconds even though our methods are 100% foolproof
            Thread.sleep(1000);
            myJobLogReader = getJoblogWithID(jobId);
            myJobLog = myJobLogReader.getRawJobLog();
        }
        return myJobLog;
    }

}
