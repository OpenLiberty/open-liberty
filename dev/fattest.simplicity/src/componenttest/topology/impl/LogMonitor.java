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
package componenttest.topology.impl;

import java.io.BufferedInputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.websphere.soe_reporting.SOEHttpPostUtil;

import componenttest.exception.NoStringFoundInLogException;
import componenttest.topology.impl.LibertyFileManager.LogSearchResult;

/**
 * This class was initial created as an extraction of methods and members from the LibertyServer class.
 * The intent here is to provide some basic log wait/search capability to server types beyond LibertyServer.
 * Users of this class will need to implement the LogMonitorClient interface. Over time, it is hoped, perhaps,
 * more of the log wait/search logic can be migrated to this class.
 */
public class LogMonitor {
    private static final Class<?> c = LogMonitor.class;

    /** How frequently we poll the logs when waiting for something to happen */
    protected static final int WAIT_INCREMENT = 300; //milliseconds

    protected static final boolean FAT_TEST_LOCALRUN = Boolean.getBoolean("fat.test.localrun");

    /** Default wait period for log search requests **/
    protected static final long LOG_SEARCH_TIMEOUT = FAT_TEST_LOCALRUN ? 12 * 1000 : 120 * 1000;

    //Used for keeping track of mark positions of log files
    protected HashMap<String, Long> logMarks = new HashMap<String, Long>();

    protected HashMap<String, Long> originMarks = new HashMap<String, Long>();

    //The sole client for this LogMonitor instance.  The LogMonitorClient provides a means of hiding
    //the underlying server class for which this class providing log monitoring services.
    private final LogMonitorClient client;

    public LogMonitor(LogMonitorClient client) {
        this.client = client;
    }

    /**
     * Reset the mark and offset values for logs back to the start of the JVM's run.
     */
    public void resetLogMarks() {
        client.lmcResetLogOffsets();
        logMarks = new HashMap<String, Long>(originMarks);
        Log.info(c, "resetLogMarks", "Reset log offsets " + logMarks);
    }

    /**
     * Note: This method doesn't set the offset values to the beginning of the file per se,
     * rather this method sets the list of logs and their offset values to null. When one
     * of the findStringsInLogsAndTrace...(...) methods are called, it will recreate the
     * list of logs and set each offset value to 0L - the start of the file.
     */
    public void clearLogMarks() {
        client.lmcClearLogOffsets();
        logMarks.clear();
        originMarks.clear();

        Log.info(c, "clearLogMarks", "Cleared log marks");
    }

    /**
     * Set the marks that we'll go back to when {@link #resetLogMarks()} is called.
     */
    public void setOriginLogMarks() {

        client.lmcSetOriginLogOffsets();
        originMarks = new HashMap<String, Long>(logMarks);

        Log.info(c, "setOriginLogMarks", "Set origin log marks " + originMarks);
    }

    /**
     * Set the mark offset to the end of the log file.
     *
     * @param log files to mark. If none are specified, the default log file is marked.
     */
    public void setMarkToEndOfLog(RemoteFile... logFiles) throws Exception {
        if (logFiles == null || logFiles.length == 0)
            logFiles = new RemoteFile[] { client.lmcGetDefaultLogFile() };

        for (RemoteFile logFile : logFiles) {
            String path = logFile.getAbsolutePath();

            long offset = 0;
            BufferedInputStream input = new BufferedInputStream(logFile.openForReading());
            try {
                int available = input.available();
                offset = input.skip(available);
                while (input.read() != -1) {
                    offset++;
                }
            } finally {
                input.close();
            }

            Long oldMarkOffset = logMarks.put(path, offset);
            Log.info(c, "setMarkToEndOfLog", path + ", old mark offset=" + oldMarkOffset + ", new mark offset=" + offset + " for " + logFile);
        }
    }

    /**
     * Get the mark offset for the specified log file.
     */
    protected Long getMarkOffset(String logFile) {

        String method = "getMarkOffset";
        Log.finer(c, method, logFile);

        if (!logMarks.containsKey(logFile)) {
            Log.finer(c, method, "file does not exist in logMarks, set initial offset");
            logMarks.put(logFile, 0L);
        }

        Log.info(c, "getMarkOffset", "Mark offset for " + logFile + "=" + logMarks.get(logFile) + " for " + logFile);
        return logMarks.get(logFile);
    }

    /**
     * Wait for the specified regex in the default logs from the last mark.
     * <p>
     * This method will time out after a sensible period of
     * time has elapsed.
     * <p>The best practice for this method is as follows:
     * <tt><p>
     * // Set the mark to the current end of log<br/>
     * server.setMarkToEndOfLog();<br/>
     * // Do something, e.g. config change<br/>
     * server.setServerConfigurationFile("newServer.xml");<br/>
     * // Wait for message that was a result of the config change<br/>
     * server.waitForStringInLogUsingMark("CWWKZ0009I");<br/>
     * </p></tt></p>
     *
     * @param regexp a regular expression to search for
     * @return the matching line in the log, or null if no matches
     *         appear before the timeout expires
     */
    public String waitForStringInLogUsingMark(String regexp) {
        return waitForStringInLogUsingMark(regexp, LOG_SEARCH_TIMEOUT);
    }

    /**
     * Wait for the specified regex in the default logs from the last mark.
     * <p>
     * Unless there's a strong functional requirement that
     * your string appear super-quickly, or you know your string
     * might take a ridiculously long time (like five minutes),
     * consider using the method which takes a default timeout, {@link }
     *
     * @param regexp
     * @param timeout a timeout, in milliseconds
     * @return
     */
    public String waitForStringInLogUsingMark(String regexp, long timeout) {
        try {
            return waitForStringInLogUsingMark(regexp, timeout, timeout * 2, client.lmcGetDefaultLogFile());
        } catch (Exception e) {
            Log.warning(c, "Could not find string in trace log file due to exception " + e);
            return null;
        }
    }

    /**
     * Wait for the specified regexp in the default logs from the last mark.
     * The offset is also incremented every time this method is called.
     *
     * @param regexp a regular expression to search for
     * @param intendedTimeout a timeout, in milliseconds, within which the wait should complete. Exceeding this is a soft fail.
     * @param extendedTimeout a timeout, in milliseconds, within which the wait must complete. Exceeding this is a hard fail.
     * @param outputFile file to check
     * @return line that matched the regexp
     */
    protected String waitForStringInLogUsingMark(String regexp, long intendedTimeout, long extendedTimeout, RemoteFile outputFile) {
        final String METHOD_NAME = "waitForStringInLogUsingMark";
        long startTime = System.currentTimeMillis();
        int waited = 0;
        String firstLine = null;
        String lastLine = null;
        boolean hitEof = false;
        Long offset = getMarkOffset(outputFile.getAbsolutePath());

        try {
            LogSearchResult newOffsetAndMatches;
            while (waited <= extendedTimeout) {
                if (waited > intendedTimeout) { // first time only
                    SOEHttpPostUtil.reportSoftLogTimeoutToSOE(c.getName(), METHOD_NAME, 3977, intendedTimeout, regexp);
                    intendedTimeout = extendedTimeout + WAIT_INCREMENT; // don't report again
                }
                newOffsetAndMatches = LibertyFileManager.findStringInFile(regexp, outputFile, offset);
                if (firstLine == null)
                    firstLine = newOffsetAndMatches.getFirstLine();
                if (newOffsetAndMatches.getLastLine() == null)
                    hitEof = true;
                else
                    lastLine = newOffsetAndMatches.getLastLine();
                offset = newOffsetAndMatches.getOffset();
                List<String> matches = newOffsetAndMatches.getMatches();
                if (matches.isEmpty()) {
                    try {
                        Thread.sleep(WAIT_INCREMENT);
                    } catch (InterruptedException e) {
                        // Ignore and carry on
                    }
                    waited += WAIT_INCREMENT;
                } else {
                    client.lmcUpdateLogOffset(outputFile.getAbsolutePath(), offset);
                    return matches.get(0);
                }
            }
            Log.warning(c, "Timed out searching for " + regexp + " in log file: " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            // I think we can assume if we can't read the file it doesn't contain our string
            Log.warning(c, "Could not read log file: " + outputFile + " due do exception " + e.toString());
            e.printStackTrace();
        } finally {
            long endTime = System.currentTimeMillis();
            DateFormat formatter = DateFormat.getTimeInstance(DateFormat.LONG);
            Log.finer(c, "waitForStringInLogUsingMark",
                      "Started waiting for message matching regexp [ " + regexp + "] at " + formatter.format(new Date(startTime))
                                                        + " and finished at " + formatter.format(new Date(endTime)));
            Log.finer(c, "waitForStringInLogUsingMark", "First line searched: [ " + firstLine + " ]");
            Log.finer(c, "waitForStringInLogUsingMark", "Last line searched:  [ " + lastLine + " ]");
            if (hitEof)
                Log.finer(c, "waitForStringInLogUsingMark", "Last line searching reached end of file, preceding last line was the last line of text seen.");
        }
        return null;
    }

    /**
     * Wait for the specified regexp in the default logs from the last mark
     * and verify that the regex does not show up in the logs during the
     * specfied duration.
     *
     * @param regexp a regular expression to search for
     * @param intendedTimeout a timeout, in milliseconds, within which the wait should complete. Exceeding this is a soft fail.
     * @param extendedTimeout a timeout, in milliseconds, within which the wait must complete. Exceeding this is a hard fail.
     * @param outputFile file to check
     * @return line that matched the regexp
     */
    public String verifyStringNotInLogUsingMark(String regexp, long timeout) {
        try {
            return waitForStringInLogUsingMarkWithException(regexp, timeout, timeout * 2, client.lmcGetDefaultLogFile());
        } catch (Exception ex) {
            if (ex instanceof NoStringFoundInLogException) {
                return null;
            } else {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Wait for the specified regexp in the default logs from the last mark
     * and verify that the regex does not show up in the logs during the
     * specfied duration.
     *
     * @param timeout Timeout (in milliseconds)
     * @return line that matched the regexp
     */
    public String verifyStringNotInLogUsingMark(String regexToSearchFor, long timeout, RemoteFile logFileToSearch) {
        try {
            return waitForStringInLogUsingMarkWithException(regexToSearchFor, timeout, timeout * 2, logFileToSearch);
        } catch (Exception ex) {
            if (ex instanceof NoStringFoundInLogException) {
                return null;
            } else {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Wait for the specified regexp in the default logs from the last mark.
     * The offset is also incremented every time this method is called.
     *
     * TODO: This is a temporary version of this method that will be used for negative
     * checks. Remove this method and update the verifyStringNotInLogUsingMark method to use
     * the waitForStringInLogUsingMark method eventually.
     *
     * @param regexp a regular expression to search for
     * @param intendedTimeout a timeout, in milliseconds, within which the wait should complete. Exceeding this is a soft fail.
     * @param extendedTimeout a timeout, in milliseconds, within which the wait must complete. Exceeding this is a hard fail.
     * @param outputFile file to check
     * @return line that matched the regexp
     */
    protected String waitForStringInLogUsingMarkWithException(String regexp, long intendedTimeout, long extendedTimeout, RemoteFile outputFile) {
        final String METHOD_NAME = "waitForStringInLogUsingMarkWithException";
        long startTime = System.currentTimeMillis();
        int waited = 0;
        String firstLine = null;
        String lastLine = null;
        boolean hitEof = false;
        Long offset = getMarkOffset(outputFile.getAbsolutePath());
        try {
            LogSearchResult newOffsetAndMatches;
            while (waited <= extendedTimeout) {
                if (waited > intendedTimeout) { // first time only
                    SOEHttpPostUtil.reportSoftLogTimeoutToSOE(c.getName(), METHOD_NAME, 3977, intendedTimeout, regexp);
                    intendedTimeout = extendedTimeout + WAIT_INCREMENT; // don't report again
                }
                newOffsetAndMatches = LibertyFileManager.findStringInFile(regexp, outputFile, offset);
                if (firstLine == null)
                    firstLine = newOffsetAndMatches.getFirstLine();
                if (newOffsetAndMatches.getLastLine() == null)
                    hitEof = true;
                else
                    lastLine = newOffsetAndMatches.getLastLine();
                offset = newOffsetAndMatches.getOffset();
                List<String> matches = newOffsetAndMatches.getMatches();
                if (matches.isEmpty()) {
                    try {
                        Thread.sleep(WAIT_INCREMENT);
                    } catch (InterruptedException e) {
                        // Ignore and carry on
                    }
                    waited += WAIT_INCREMENT;
                } else {
                    client.lmcUpdateLogOffset(outputFile.getAbsolutePath(), offset);
                    return matches.get(0);
                }
            }
            //Log.warning(c, "Timed out searching for " + regexp + " in log file: " + outputFile.getAbsolutePath());
            NoStringFoundInLogException ex = new NoStringFoundInLogException("Timed out searching for " + regexp + " in log file: " + outputFile.getAbsolutePath());
            throw ex;
        } catch (Exception e) {
            // I think we can assume if we can't read the file it doesn't contain our string
            if (e instanceof NoStringFoundInLogException) {
                throw (NoStringFoundInLogException) e;
            }
            Log.warning(c, "Could not read log file: " + outputFile + " due do exception " + e.toString());
            e.printStackTrace();
        } finally {
            long endTime = System.currentTimeMillis();
            DateFormat formatter = DateFormat.getTimeInstance(DateFormat.LONG);
            Log.finer(LibertyServer.class, "waitForStringInLogUsingMark",
                      "Started waiting for message matching regexp [ " + regexp + "] at " + formatter.format(new Date(startTime))
                                                                          + " and finished at " + formatter.format(new Date(endTime)));
            Log.finer(LibertyServer.class, "waitForStringInLogUsingMark", "First line searched: [ " + firstLine + " ]");
            Log.finer(LibertyServer.class, "waitForStringInLogUsingMark", "Last line searched:  [ " + lastLine + " ]");
            if (hitEof)
                Log.finer(LibertyServer.class, "waitForStringInLogUsingMark", "Last line searching reached end of file, preceding last line was the last line of text seen.");
        }
        return null;
    }

    /**
     * Check for multiple instances of the regex in log using mark
     *
     * @param numberOfMatches number of matches required
     * @param regexp a regular expression to search for
     * @param timeout a timeout, in milliseconds
     * @param outputFile file to check
     * @return number of matches found
     */
    public int waitForMultipleStringsInLogUsingMark(int numberOfMatches, String regexp, long timeout, RemoteFile outputFile) {
        long startTime = System.currentTimeMillis();
        int waited = 0;
        int count = 0;

        long extendedTimeout = 2 * timeout;
        Long offset = getMarkOffset(outputFile.getAbsolutePath());

        //Ensure we always search for at least 1 occurrence
        if (numberOfMatches <= 0) {
            numberOfMatches = 1;
        }

        try {
            LogSearchResult newOffsetAndMatches;
            while (count < numberOfMatches && waited <= extendedTimeout) {
                if (waited > timeout) { // first time only
                    SOEHttpPostUtil.reportSoftLogTimeoutToSOE(c.getName(), "waitForMultipleStringsInLogUsingMark", 4319, timeout, regexp);
                    timeout = extendedTimeout + WAIT_INCREMENT; // don't report again
                }
                newOffsetAndMatches = LibertyFileManager.findStringInFile(regexp, outputFile, offset);
                offset = newOffsetAndMatches.getOffset();
                try {
                    Thread.sleep(WAIT_INCREMENT);
                } catch (InterruptedException e) {
                    // Ignore and carry on
                }
                waited += WAIT_INCREMENT;
                client.lmcUpdateLogOffset(outputFile.getAbsolutePath(), offset);
                count += newOffsetAndMatches.getMatches().size();
            }
        } catch (Exception e) {
            // I think we can assume if we can't read the file it doesn't contain our string
            Log.warning(c, "Could not read log file: " + outputFile + " due to exception " + e.toString());
            e.printStackTrace();
            return 0;
        } finally {
            long endTime = System.currentTimeMillis();
            DateFormat formatter = DateFormat.getTimeInstance(DateFormat.LONG);
            Log.finer(LibertyServer.class, "waitForMultipleStringsInLog",
                      "Started waiting for " + numberOfMatches + " messages matching regexp [ " + regexp + "] at " + formatter.format(new Date(startTime))
                                                                          + " and finished at " + formatter.format(new Date(endTime)) + " finding " + count + " matches.");
        }

        return count;
    }

}
