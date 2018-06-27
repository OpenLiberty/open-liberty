/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.hpel.viewer;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.logging.Level;

import com.ibm.ejs.ras.hpel.Messages;
import com.ibm.websphere.logging.hpel.reader.HpelFormatter;
import com.ibm.websphere.logging.hpel.reader.RepositoryLogRecord;
import com.ibm.websphere.logging.hpel.reader.RepositoryPointer;
import com.ibm.websphere.logging.hpel.reader.RepositoryReaderImpl;
import com.ibm.websphere.logging.hpel.reader.ServerInstanceLogRecordList;
import com.ibm.websphere.logging.hpel.reader.filters.LogViewerFilter;
import com.ibm.websphere.logging.hpel.writer.HPELRepositoryExporter;
import com.ibm.ws.logging.object.hpel.RepositoryLogRecordImpl;

/**
 * The LogViewer class is used to output log data from the common binary log file to a file or systemOut/console. The
 * resulting information is in text and can be formatted as needed. *
 */
public class LogViewer {
    static final DateFormat dateFormat = new SimpleDateFormat(getLocalizedString("CWTRA0003I"));
    static final DateFormat instanceDF = new SimpleDateFormat(getLocalizedString("CWTRA0073I"));

    static final int MAX_ZCOLUMN_SIZE = 72;
    static final int MAX_DCOLUMN_SIZE = 30;
    static final String zOSHeader = getLocalizedString("CWTRA0071I");
    static final String zOSSpaces = "                                    ";
    static final String distHeader = getLocalizedString("CWTRA0072I");
    static final String distSpaces = "                ";
    private String spaces;

    private static final String BUNDLE_NAME = "com.ibm.ws.logging.hpel.resources.LogViewerMessages";
    private static final String OPTION_DELIMITER = "-";

    private static final boolean useHeaderTimeZone = true;

    private String binaryRepositoryDir = null;
    private String outputLogFilename = null;
    private Date startDate = null;
    private Date stopDate = null;

    //for subprocess viewing
    private boolean listInstances = false;
    private Date mainInstanceId = null;
    private String subInstanceId = null;

    // List of level names acceptable by the LogViewer.
    private String levelString = null;

    //indicates that only the most recent server instance is retrieved
    private boolean latestInstance = false;

    private Level minLevel = null;
    private Level maxLevel = null;
    private String includeLoggers = null;
    private String excludeLoggers = null;
    private String hexThreadID = null;
    private HpelFormatter theFormatter = HpelFormatter.getFormatter(HpelFormatter.FORMAT_BASIC);
    private boolean hasFooter = false; //initial formatter is BASIC, so no footer
    private static final int DEFAULT_TAIL_INTERVAL = 5; // default tail interval
    private int tailInterval = -1; // by default don't tail
    private Locale locale = null;
    private HPELRepositoryExporter outputRepository = null; // the file path to where to write new repository if requested.
    private String message;
    private String excludeMessages = null;
    private final ArrayList<LogViewerFilter.Extension> extensions = new ArrayList<LogViewerFilter.Extension>();
    private String encoding = null;
    private boolean isSystemOut = false;

    /**
     * The main method.
     * <p>
     *
     * @param args
     *            - command line arguments to LogViewer
     */
    public static void main(String[] args) {
        LogViewer logViewer = new LogViewer();
        int code = logViewer.execute(args);
        if (code > 0) {
            System.err.println(getLocalizedString("CWTRA0029I"));
        }
        System.exit(code);
    }

    // Comparator to sort list of levels according to their integer values.
    private final static Comparator<Level> LEVEL_COMPARATOR = new Comparator<Level>() {
        @Override
        public int compare(Level object1, Level object2) {
            int level1 = object1.intValue();
            int level2 = object2.intValue();
            if (level1 < level2) {
                return -1;
            } else if (level1 > level2) {
                return 1;
            } else {
                return 0;
            }
        }
    };

    // Default list of levels used by Level.parse();
    private final static Level[] DEFAULT_LEVEL_LIST = {
                                                        Level.FINEST,
                                                        Level.FINER,
                                                        Level.FINE,
                                                        Level.CONFIG,
                                                        Level.INFO,
                                                        Level.WARNING,
                                                        Level.SEVERE
    };

    /**
     * Utility class containing attributes of a custom level.
     */
    public static class LevelDetails {
        final int intValue;
        final String id;
        final String resourceBundleName;

        /**
         * Creates instance of a custom level attributes.
         *
         * @param intValue an integer value indicating the level
         * @param id a short id string for this level. Usually one letter.
         * @param resourceBundleName the name of the resource bundle to use
         */
        public LevelDetails(int intValue, String id, String resourceBundleName) {
            this.intValue = intValue;
            this.id = id;
            this.resourceBundleName = resourceBundleName;
        }
    }

    Map<String, LevelDetails> readLevels(String customLevelsPropertyFile) {
        if (customLevelsPropertyFile != null) {
            Map<String, LevelDetails> result = new HashMap<String, LevelDetails>();
            try {
                Properties levelProps = new Properties();
                levelProps.load(new FileInputStream(new File(customLevelsPropertyFile)));
                for (String level : levelProps.stringPropertyNames()) {
                    try {
                        int intValue = Integer.parseInt(level);
                        String valueString = levelProps.getProperty(level);
                        String[] values = valueString.trim().split("\\s+");
                        if (values.length == 0) {
                            System.err.println(getLocalizedParmString("NoLevelNameInCustomLevelsFile", new Object[] { customLevelsPropertyFile, level }));
                            break;
                        }
                        if (values.length > 3) {
                            System.err.println(getLocalizedParmString("TooManyValuesInCustomLevelsFile", new Object[] { customLevelsPropertyFile, level, valueString, 3 }));
                            break;
                        }
                        String name = values[0];
                        String id = values.length > 1 ? values[1] : null;
                        String resourceBundleName = values.length > 2 ? values[2] : null;
                        if (id != null && id.length() > 1) {
                            if (resourceBundleName == null || resourceBundleName.length() == 1) {
                                String tmp = resourceBundleName;
                                resourceBundleName = id;
                                id = tmp;
                            } else {
                                System.err.println(getLocalizedParmString("LevelIdTooBigInCustomLevelsFile",
                                                                          new Object[] { customLevelsPropertyFile, level, id, resourceBundleName }));
                                break;
                            }
                        }
                        result.put(name, new LevelDetails(intValue, id, resourceBundleName));
                    } catch (NumberFormatException ex) {
                        System.err.println(getLocalizedParmString("NotIntegerKeyInCustomLevelsFile", new Object[] { customLevelsPropertyFile, level }));
                        break;
                    }
                }
                return result;
            } catch (FileNotFoundException e) {
                System.err.println(getLocalizedParmString("SpecifiedCustomLevelsFileNotFound", new Object[] { customLevelsPropertyFile }));
            } catch (IOException e) {
                System.err.println(getLocalizedParmString("ErrorReadingCustomLevelsFile", new Object[] { customLevelsPropertyFile, e.getMessage() }));
            }
        }

        return null;
    }

    String getLevelsString(Map<String, LevelDetails> levels) {
        java.util.SortedSet<Level> list = new TreeSet<Level>(LEVEL_COMPARATOR);
        if (levels != null) {
            for (Map.Entry<String, LevelDetails> entry : levels.entrySet()) {
                list.add(HpelFormatter.addCustomLevel(entry.getKey(), entry.getValue().intValue, entry.getValue().id, entry.getValue().resourceBundleName));
            }
        }
        for (Level level : DEFAULT_LEVEL_LIST) {
            list.add(level);
        }
        StringBuilder result = null;
        for (Level level : list) {
            if (result == null) {
                result = new StringBuilder(level.getName());
            } else {
                result.append(" | ").append(level.getName());
            }
        }
        return result.toString();
    }

    String[] readHeader(String headerFileName) {
        if (headerFileName != null) {
            try {
                BufferedReader input = new BufferedReader(new FileReader(headerFileName));
                String line;
                ArrayList<String> header = new ArrayList<String>();
                while ((line = input.readLine()) != null) {
                    header.add(line);
                }
                return header.toArray(new String[header.size()]);
            } catch (FileNotFoundException e) {
                System.err.println(getLocalizedParmString("SpecifiedCustomHeaderFileNotFound", new Object[] { headerFileName }));
            } catch (IOException e) {
                System.err.println(getLocalizedParmString("ErrorReadingCustomHeaderFile", new Object[] { headerFileName, e.getMessage() }));
            }
        }
        return null;
    }

    /**
     * Runs LogViewer using values in System Properties to find custom levels and header.
     *
     * @param args
     *            - command line arguments to LogViewer
     * @return code indicating status of the execution: 0 on success, 1 otherwise.
     */
    public int execute(String[] args) {
        Map<String, LevelDetails> levels = readLevels(System.getProperty("logviewer.custom.levels"));
        String[] header = readHeader(System.getProperty("logviewer.custom.header"));

        return execute(args, levels, header);
    }

    /**
     * Runs LogViewer.
     *
     * @param args command line arguments to LogViewer
     * @param levels map of custom level names to their attributes
     * @param header custom header template as an array of strings
     * @return code indicating status of the execution: 0 on success, 1 otherwise.
     */
    public int execute(String[] args, Map<String, LevelDetails> levels, String[] header) {

        levelString = getLevelsString(levels);

        RepositoryReaderImpl logRepository;
        try {
            // Parse the command line arguments and validate arguments
            if (parseCmdLineArgs(args) || validateSettings()) {
                return 0;
            }

            // Setup custom header here since parseCmdLineArgs may alter the formatter.
            if (header != null) {
                theFormatter.setCustomHeader(header);
            }

            // Call HPEL repository API to get log entries
            logRepository = new RepositoryReaderImpl(binaryRepositoryDir);

            if (mainInstanceId != null) {
                // Verify requested instance ID.
                ServerInstanceLogRecordList silrl = logRepository.getLogListForServerInstance(mainInstanceId);
                if (silrl == null || silrl.getStartTime() == null || !silrl.getStartTime().equals(mainInstanceId) ||
                    (subInstanceId != null && !subInstanceId.isEmpty() && !silrl.getChildren().containsKey(subInstanceId))) {
                    throw new IllegalArgumentException(getLocalizedString("LVM_ERROR_INSTANCEID"));
                }
            }

            // Create the output stream (either an output file or Console)
            PrintStream outps = createOutputStream();

            /*
             * Create a filter object with our search criteria, passing null for startDate and stopDate as we will
             * be using the API to search by date for efficiency.
             */
            LogViewerFilter searchCriteria = new LogViewerFilter(startDate, stopDate, minLevel, maxLevel, includeLoggers, excludeLoggers, hexThreadID, message, excludeMessages, extensions);

            //Determine if we just display instances or start displaying records based on the -listInstances option
            if (listInstances) {
                Iterable<ServerInstanceLogRecordList> results = logRepository.getLogLists();
                displayInstances(outps, results);
            } else {
                Properties initialProps = logRepository.getLogListForCurrentServerInstance().getHeader();
                if (initialProps != null) {

                    boolean isZOS = "Y".equalsIgnoreCase(initialProps.getProperty(ServerInstanceLogRecordList.HEADER_ISZOS));

                    //instanceId is required for z/OS.  If it was not provided, then list the possible instances
                    if (isZOS && !latestInstance && mainInstanceId == null) {
                        Iterable<ServerInstanceLogRecordList> results = logRepository.getLogLists();
                        displayInstances(outps, results);
                    } else {
                        displayRecords(outps, searchCriteria, logRepository, mainInstanceId, subInstanceId);
                    }
                }
            }

        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 1;
        }

        return 0;
    }

    private static class LocalMergedRepository implements Iterable<RepositoryLogRecord> {
        final Iterator<RepositoryLogRecord> logIt;
        final Iterator<RepositoryLogRecord> traceIt;
        RepositoryLogRecordImpl nextLog;
        RepositoryLogRecordImpl nextTrace;
        boolean returnLog = false;
        Iterator<RepositoryLogRecord> it = null;

        LocalMergedRepository(ServerInstanceLogRecordList logList, ServerInstanceLogRecordList traceList) {
            logIt = logList.iterator();
            traceIt = traceList.iterator();
            nextLog = (RepositoryLogRecordImpl) logIt.next();
            nextTrace = (RepositoryLogRecordImpl) traceIt.next();
        }

        boolean isLastLog() {
            return returnLog;
        }

        @Override
        public Iterator<RepositoryLogRecord> iterator() {
            if (it != null) {
                throw new UnsupportedOperationException("Creating second iterator on LocalMergeRepository is not supported.");
            }
            it = new Iterator<RepositoryLogRecord>() {
                @Override
                public boolean hasNext() {
                    return nextLog != null || nextTrace != null;
                }

                @Override
                public RepositoryLogRecord next() {
                    if (nextLog == null && nextTrace == null) {
                        return null;
                    }
                    if (nextLog == null) {
                        returnLog = false;
                    } else if (nextTrace == null) {
                        returnLog = true;
                    } else {
                        returnLog = nextLog.getInternalSeqNumber() < nextTrace.getInternalSeqNumber();
                    }
                    RepositoryLogRecord result = null;
                    if (returnLog) {
                        result = nextLog;
                        nextLog = (RepositoryLogRecordImpl) logIt.next();
                    } else {
                        result = nextTrace;
                        nextTrace = (RepositoryLogRecordImpl) traceIt.next();
                    }
                    return result;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
            return it;
        }
    }

    /*
     * Attempts to display log and trace records from server instances that match the search criteria and instanceId.
     * Search criteria determines the type of records returned while the instanceId identifies which ServerInstanceLogRecordList
     * the records need to be displayed from. In an environment where subprocesses exist such as z/OS, an instanceID is
     * required. If an instanceID was not provided while the logViewer was invoked in a z/OS environment, this method
     * will then display the list of instances.
     */
    private void displayRecords(PrintStream outps, LogViewerFilter searchCriteria, RepositoryReaderImpl logRepository, Date requestedProcess, String requestedSubProcess) {

        // count records
        int recordcount = 0;
        long opStart = System.currentTimeMillis();

        //This flag determines whether a footer should be printed or not.  The footer should
        //only be printed when the header has been printed.
        boolean headerPrinted = false;

        Iterable<ServerInstanceLogRecordList> repResults = null;
        boolean needHeader = false;

        // If we are executing -tail command
        if (tailInterval > 0) {

            boolean firstIteration = true;
            String tailPid = null;
            RepositoryPointer logRepositoryPointer = null;
            RepositoryPointer traceRepositoryPointer = null;

            if (latestInstance) {
                ServerInstanceLogRecordList silrl = logRepository.getLogListForCurrentServerInstance();
                requestedSubProcess = getLatestKid(silrl);
                requestedProcess = silrl.getStartTime();
            }

            do {
                // For tail command need to read repository on each iteration in case log or trace were created later.
                logRepository = new RepositoryReaderImpl(binaryRepositoryDir);
                RepositoryReaderImpl logReader = null;
                if (logRepository.getLogLocation() != null) {
                    logReader = new RepositoryReaderImpl(logRepository.getLogLocation());
                }
                RepositoryReaderImpl traceReader = null;
                if (logRepository.getTraceLocation() != null) {
                    traceReader = new RepositoryReaderImpl(logRepository.getTraceLocation());
                }
                if (logReader == null && traceReader == null) {
                    return;
                }

                // Adjust requested process only if we didn't see either log or trace records yet.
                if (requestedProcess != null && (logRepositoryPointer == null || traceRepositoryPointer == null)) {
                    // Adjust requestedProcess to use in both log and trace repositories
                    if (logReader != null && traceReader != null) {
                        ServerInstanceLogRecordList logSilrl = logReader.getLogListForServerInstance(requestedProcess);
                        ServerInstanceLogRecordList traceSilrl = traceReader.getLogListForServerInstance(requestedProcess);
                        if (logRepositoryPointer == null && (logSilrl.getStartTime() == null || !logSilrl.getStartTime().equals(requestedProcess))) {
                            // requestedProcess correspond to trace start time, see if there are logs for the same process.
                            if (logSilrl.getStartTime() == null) {
                                // First log was generated after requested date. Check the first log
                                logSilrl = logReader.getLogLists(requestedProcess, null).iterator().next();
                            } else if (logSilrl.getStartTime().before(requestedProcess)) {
                                // Log record was generated before requested date. Check the next log after 'requested date.
                                Iterator<ServerInstanceLogRecordList> it = logReader.getLogLists(requestedProcess, null).iterator();
                                it.next();
                                logSilrl = it.next();
                            } else {
                                // requestedDate should correspond to the first record among logs and traces
                                throw new RuntimeException("Internal Error! Log start time is after requested date");
                            }
                            if (logSilrl != null) {
                                String logPid = logSilrl.getHeader().getProperty(ServerInstanceLogRecordList.HEADER_PROCESSID);

                                String tracePid = traceSilrl.getHeader().getProperty(ServerInstanceLogRecordList.HEADER_PROCESSID);
                                if (tracePid.equals(logPid)) {
                                    requestedProcess = logSilrl.getStartTime();
                                    //no need for null check on requestedProcess here as long as both logReader and traceReader have
                                    //been checked for non-null values.
                                } else {
                                    logReader = null;
                                }
                            } // No adjustment is necessary if logSilrl is 'null'. It can mean the requestedProcess was already adjusted for trace.
                        } else if (traceRepositoryPointer == null && (traceSilrl.getStartTime() == null || !traceSilrl.getStartTime().equals(requestedProcess))) {
                            // requestedProcess correspond to log start time, see if there are traces for the same process.
                            if (traceSilrl.getStartTime() == null) {
                                // First trace was generated after requested date. Check the first trace
                                traceSilrl = traceReader.getLogLists(requestedProcess, null).iterator().next();
                            } else if (traceSilrl.getStartTime().before(requestedProcess)) {
                                // Trace record was generated before requested date. Check the next trace after 'requested date.
                                Iterator<ServerInstanceLogRecordList> it = traceReader.getLogLists(requestedProcess, null).iterator();
                                it.next();
                                traceSilrl = it.next();
                            } else {
                                // requestedDate should correspond to the first record among logs and traces
                                throw new RuntimeException("Internal Error! Trace start time is after requested date");
                            }
                            if (traceSilrl != null) {
                                String tracePid = traceSilrl.getHeader().getProperty(ServerInstanceLogRecordList.HEADER_PROCESSID);

                                String logPid = logSilrl.getHeader().getProperty(ServerInstanceLogRecordList.HEADER_PROCESSID);
                                if (logPid.equals(tracePid)) {
                                    requestedProcess = traceSilrl.getStartTime();
                                    //no need for null check on requestedProcess here as long as both logReader and traceReader have
                                    //been checked for non-null values.
                                } else {
                                    traceReader = null;
                                }
                            } // No adjustment is necessary if traceSilrl is 'null'. It can mean the requestedProcess was already adjusted for trace.
                        }
                    }
                }

                // set startDate or initialize first repository pointers only on the first iteration.
                if (startDate == null && firstIteration) {
                    // we are tailing the log, don't want old messages.
                    if (requestedProcess != null) {
                        ServerInstanceLogRecordList silrl;
                        RepositoryLogRecord lastRecord;

                        if (logReader != null) {
                            // Initialize logRepositoryPointer with the last log record
                            silrl = logReader.getLogListForServerInstance(requestedProcess, searchCriteria);
                            if (requestedSubProcess != null && !requestedSubProcess.isEmpty()) {
                                silrl = silrl.getChildren().get(requestedSubProcess);
                            }
                            // silrl can be 'null' if subprocess don't have log records.
                            if (silrl != null) {
                                lastRecord = silrl.range(-1, 1).iterator().next();
                                if (lastRecord != null) {
                                    logRepositoryPointer = lastRecord.getRepositoryPointer();
                                }
                            }
                        }

                        if (traceReader != null) {
                            // Initialize traceRepositoryPointer with the last trace record
                            silrl = traceReader.getLogListForServerInstance(requestedProcess, searchCriteria);
                            if (requestedSubProcess != null && !requestedSubProcess.isEmpty()) {
                                silrl = silrl.getChildren().get(requestedSubProcess);
                            }
                            // silrl can be 'null' if subprocess don't have trace records.
                            if (silrl != null) {
                                lastRecord = silrl.range(-1, 1).iterator().next();
                                if (lastRecord != null) {
                                    traceRepositoryPointer = lastRecord.getRepositoryPointer();
                                }
                            }
                        }
                    } else {
                        startDate = new Date();
                    }
                }

                firstIteration = false;

                //get all server instances
                final Iterator<ServerInstanceLogRecordList> logResults;
                ServerInstanceLogRecordList nextLogList = null;
                final Iterator<ServerInstanceLogRecordList> traceResults;
                ServerInstanceLogRecordList nextTraceList = null;

                if (logReader != null) {
                    if (requestedProcess != null) {
                        if (logRepositoryPointer == null) {
                            nextLogList = logReader.getLogListForServerInstance(requestedProcess, searchCriteria);
                            if (requestedSubProcess != null && !requestedSubProcess.isEmpty()) {
                                nextLogList = nextLogList.getChildren().get(requestedSubProcess);
                            }
                        } else {
                            nextLogList = logReader.getLogListForServerInstance(logRepositoryPointer, searchCriteria);
                            // Note that using pointer returns list for the same process/subprocess the pointer's record
                            // came from so we don't need to retrieve children here.
                        }
                        logResults = null;
                    } else {
                        if (logRepositoryPointer == null) {
                            logResults = logReader.getLogLists(startDate, stopDate, searchCriteria).iterator();
                        } else {
                            logResults = logReader.getLogLists(logRepositoryPointer, stopDate, searchCriteria).iterator();
                        }
                        nextLogList = logResults.next();
                    }
                } else {
                    logResults = null;
                }
                if (traceReader != null) {
                    if (requestedProcess != null) {
                        if (traceRepositoryPointer == null) {
                            nextTraceList = traceReader.getLogListForServerInstance(requestedProcess, searchCriteria);
                            if (requestedSubProcess != null && !requestedSubProcess.isEmpty()) {
                                nextTraceList = nextTraceList.getChildren().get(requestedSubProcess);
                            }
                        } else {
                            nextTraceList = traceReader.getLogListForServerInstance(traceRepositoryPointer, searchCriteria);
                            // Note that using pointer returns list for the same process/subprocess the pointer's record
                            // came from so we don't need to retrieve children here.
                        }
                        traceResults = null;
                    } else {
                        if (traceRepositoryPointer == null) {
                            traceResults = traceReader.getLogLists(startDate, stopDate, searchCriteria).iterator();
                        } else {
                            traceResults = traceReader.getLogLists(traceRepositoryPointer, stopDate, searchCriteria).iterator();
                        }
                        nextTraceList = traceResults.next();
                    }
                } else {
                    traceResults = null;
                }

                while (nextLogList != null || nextTraceList != null) {
                    // Skip log instances with no records
                    RepositoryLogRecord log = null;
                    if (nextLogList != null) {
                        while ((log = nextLogList.iterator().next()) == null) {
                            nextLogList = logResults == null ? null : logResults.next();
                            if (nextLogList == null) {
                                break;
                            }
                        }
                    }
                    // Skip trace instances with no records
                    RepositoryLogRecord trace = null;
                    if (nextTraceList != null) {
                        while ((trace = nextTraceList.iterator().next()) == null) {
                            nextTraceList = traceResults == null ? null : traceResults.next();
                            if (nextTraceList == null) {
                                break;
                            }
                        }
                    }
                    // If no records in either log or trace we are done.
                    if (log == null && trace == null) {
                        break;
                    }

                    boolean useLog = false;
                    boolean useTrace = false;
                    // If there's no more trace instances use log
                    if (nextTraceList == null) {
                        useLog = true;
                        // If there's no more log instances use trace
                    } else if (nextLogList == null) {
                        useTrace = true;
                    } else {
                        String logPid = nextLogList.getHeader().getProperty(ServerInstanceLogRecordList.HEADER_PROCESSID);
                        String tracePid = nextTraceList.getHeader().getProperty(ServerInstanceLogRecordList.HEADER_PROCESSID);
                        // If both log and trace came from the same process use both
                        if ((logPid == null && tracePid == null) || (logPid != null && logPid.equals(tracePid))) {
                            useLog = true;
                            useTrace = true;
                            // If first log record written earlier than first trace record written by a different process use log
                        } else if (log.getMillis() < trace.getMillis()) {
                            useLog = true;
                        } else {
                            useTrace = true;
                        }
                    }

                    Iterable<RepositoryLogRecord> resultsList;
                    Properties pidHeaderProps;
                    if (!useTrace) {
                        pidHeaderProps = nextLogList.getHeader();
                        resultsList = nextLogList;
                        nextLogList = logResults == null ? null : logResults.next();
                    } else if (!useLog) {
                        pidHeaderProps = nextTraceList.getHeader();
                        resultsList = nextTraceList;
                        nextTraceList = traceResults == null ? null : traceResults.next();
                    } else {
                        pidHeaderProps = nextLogList.getHeader();
                        resultsList = new LocalMergedRepository(nextLogList, nextTraceList);
                        nextLogList = logResults == null ? null : logResults.next();
                        nextTraceList = traceResults == null ? null : traceResults.next();
                    }

                    String pid = pidHeaderProps.getProperty(ServerInstanceLogRecordList.HEADER_PROCESSID);
                    if (pid != null && !pid.equals(tailPid)) { // this is a new server instance.
                        needHeader = true;
                    }
                    tailPid = pid;

                    // For each record in this Pid's logs - write the record - to either text file or binary repository
                    for (RepositoryLogRecord record : resultsList) {
                        // we have a non-zero resultList set, so print header.

                        if (needHeader) {

                            theFormatter.setStartDatetime(record.getMillis());
                            printHeader(pidHeaderProps, outps, headerPrinted);
                            headerPrinted = true;
                            needHeader = false;
                        }
                        if (!useTrace) {
                            logRepositoryPointer = record.getRepositoryPointer();
                        } else if (!useLog) {
                            traceRepositoryPointer = record.getRepositoryPointer();
                        } else {
                            if (((LocalMergedRepository) resultsList).isLastLog()) {
                                logRepositoryPointer = record.getRepositoryPointer();
                            } else {
                                traceRepositoryPointer = record.getRepositoryPointer();
                            }
                        }
                        if (outputRepository != null) {
                            outputRepository.storeRecord(record);
                        } else {
                            outps.println(theFormatter.formatRecord(record));
                        }
                        recordcount++;
                    }

                }
                // If there's a new instance after the one we are monitoring we don't need to wait anymore.
                if (requestedProcess != null && requestedProcess.before(logRepository.getLogListForCurrentServerInstance().getStartTime())) {
                    break;
                }
                // We are tailing the log, wait interval and pickup where we left off.
                try {
                    Thread.sleep(tailInterval * 1000);
                } catch (InterruptedException e) {
                    // User wants to exit.
                    break;
                }
            } while (true);

            // If specific instance was selected
        } else if (latestInstance || requestedProcess != null) {

            ServerInstanceLogRecordList silrl = null;
            if (latestInstance) {
                silrl = logRepository.getLogListForCurrentServerInstance();
                requestedSubProcess = getLatestKid(silrl);

                silrl = logRepository.getLogListForServerInstance((Date) null, searchCriteria);
            }
            //else instanceId was specified so get a specific silrl
            else {
                //get specific ServerInstnace
                silrl = logRepository.getLogListForServerInstance(requestedProcess, searchCriteria);
            }
            if (requestedSubProcess != null && !requestedSubProcess.isEmpty()) {
                silrl = silrl.getChildren().get(requestedSubProcess);
            }

            //process all records in the requested process or the subprocess
            if (silrl != null) {
                Properties pidHeaderProps = silrl.getHeader();

                // For each record in this Pid's logs - write the record - to either text file or binary repository
                for (RepositoryLogRecord record : silrl) {
                    // we have a non-zero resultList set, so print header.
                    if (!headerPrinted) {
                        //pidHeaderProps.put("headerStartDatetime", record.getMillis());
                        theFormatter.setStartDatetime(record.getMillis());
                        printHeader(pidHeaderProps, outps, false);
                        headerPrinted = true;
                    }
                    if (outputRepository != null) {
                        outputRepository.storeRecord(record);
                    } else {
                        outps.println(theFormatter.formatRecord(record));
                    }
                    recordcount++;
                }

            }

            //if no instanceid was provided
        } else {

            repResults = logRepository.getLogLists(startDate, stopDate, searchCriteria);

            for (ServerInstanceLogRecordList resultsList : repResults) {
                Properties pidHeaderProps = resultsList.getHeader();
                needHeader = true;

                // For each record in this Pid's logs - write the record - to either text file or binary repository

                for (RepositoryLogRecord record : resultsList) {
                    // we have a non-zero resultList set, so print header.
                    if (needHeader) {
                        //pidHeaderProps.put("headerStartDatetime", record.getMillis());
                        theFormatter.setStartDatetime(record.getMillis());
                        printHeader(pidHeaderProps, outps, headerPrinted);
                        headerPrinted = true;
                        needHeader = false;
                    }
                    if (outputRepository != null) {
                        outputRepository.storeRecord(record);
                    } else {
                        outps.println(theFormatter.formatRecord(record));
                    }
                    recordcount++;
                }
            }
        }

        if (outputRepository != null) {
            outputRepository.close();
        } else if (hasFooter && headerPrinted) {
            // need to print a CBE footer
            outps.println(theFormatter.getFooter());
        }

        if (!isSystemOut) {
            outps.flush();
            // close the file
            outps.close();
        }

        System.out.println(getLocalizedString("CWTRA0010I"));
        long opEnd = System.currentTimeMillis();
        double opSec = (opEnd - opStart) / 1000.0;
        double opAvg = recordcount / opSec;

        NumberFormat numFormat = null;
        if (locale == null) {
            numFormat = NumberFormat.getInstance(Locale.getDefault());
        } else {
            numFormat = NumberFormat.getInstance(locale);
        }

        System.out.println(getLocalizedParmString("CWTRA0018I",
                                                  new Object[] { numFormat.format(recordcount), numFormat.format(opSec), numFormat.format(opAvg) }));

    }

    /*
     * Parses the main process ID out of a user's instanceId. The main process must be a long.
     */
    private long getProcessInstanceId(String instanceId) throws NumberFormatException {
        int endIndx = instanceId.indexOf("/");

        if (endIndx > -1) {
            return Long.parseLong(instanceId.substring(0, endIndx));
        }

        return Long.parseLong(instanceId);

    }

    /*
     * Parses the subprocess ID out of a user's instance id.
     */
    private String getSubProcessInstanceId(String instanceId) {
        int beg = instanceId.indexOf("/");

        if (beg > -1 && beg + 1 < instanceId.length()) {
            return instanceId.substring(beg + 1);
        }

        return "";
    }

    /*
     * Displays the main and subprocesses instance IDs from all the server instances
     */
    private void displayInstances(PrintStream outps, Iterable<ServerInstanceLogRecordList> repResults) {
        boolean isZOS = false;
        ArrayList<DisplayInstance> unsortedInstances = new ArrayList<DisplayInstance>();

        Comparator<DisplayInstance> comparator = new Comparator<DisplayInstance>() {
            @Override
            public int compare(DisplayInstance serverList1, DisplayInstance serverList2) {
                long time1 = serverList1.getValue();
                long time2 = serverList2.getValue();

                if (time1 < time2) {
                    return -1;
                } else if (time1 > time2) {
                    return 1;
                }

                return 0;
            }
        };

        //setup spaces
        char[] spacesAr = new char[MAX_ZCOLUMN_SIZE]; //choose the larger of the two
        for (int i = 0; i < spacesAr.length; i++) {
            spacesAr[i] = ' ';
        }
        spaces = new String(spacesAr);

        //gather all the instances and convert to a display object
        for (ServerInstanceLogRecordList list : repResults) {
            //determine platform
            isZOS = "Y".equalsIgnoreCase(list.getHeader().getProperty(ServerInstanceLogRecordList.HEADER_ISZOS));

            ArrayList<DisplayInstance> kidResult = new ArrayList<DisplayInstance>();

            long kidTimeStamp = collectAllKids(kidResult, list);

            //if kids exist or current instance has records
            if (kidTimeStamp > 0) {
                if (list.iterator().next() == null) {
                    //if the current instance doesn't have any records, it won't be displayed. A timestamp
                    //still needs to be provided so that the instance can be sorted accordingly if it has kids,
                    //so provide the kid's timestamp
                    unsortedInstances.add(new DisplayInstance("", "", kidTimeStamp, kidResult));
                } else {
                    long timestamp = list.getStartTime().getTime();
                    unsortedInstances.add(new DisplayInstance(Long.toString(timestamp), "", timestamp, kidResult));
                }
            }
        }

        printInstances(unsortedInstances, comparator, false, isZOS);
    }

    private void printInstances(ArrayList<DisplayInstance> unsorted, Comparator<DisplayInstance> comparator, boolean headerPrinted, boolean isZOS) {

        DisplayInstance sortedAr[] = unsorted.toArray(new DisplayInstance[unsorted.size()]);

        //sort the array
        Arrays.sort(sortedAr, comparator);

        // process each Pid/server restart in the sorted array
        for (DisplayInstance displayInstance : sortedAr) {

            if (!headerPrinted) {
                System.out.println();
                if (isZOS) {
                    System.out.println(zOSHeader);
                } else {
                    System.out.println(distHeader);
                }
                headerPrinted = true;

            }

            String parentID = displayInstance.getParent();
            long timeStamp = displayInstance.getValue();

            String formattedTime = getFormattedDateTime(new Date(timeStamp));

            //Print Current Instance if current instance has a display name (indication that instance has records)
            if (!"".equals(displayInstance.name)) {
                //if instance has a parent, print the parent as part of the instance ID
                if (!"".equals(parentID)) {
                    printInstanceLine(parentID + "/" + displayInstance.name, formattedTime, isZOS);

                } else {//no parent, so don't print parent{
                    printInstanceLine(displayInstance.name, formattedTime, isZOS);
                }

            }

            //print the children of the instance recursively
            printInstances(displayInstance.getKids(), comparator, headerPrinted, isZOS);

        }
    }

    private void printInstanceLine(String instanceId, String formattedTime, boolean isZOS) {
        int instanceSize = instanceId.length();

        int spacesNum;

        if (isZOS) {
            spacesNum = MAX_ZCOLUMN_SIZE - instanceSize;

        } else {
            spacesNum = MAX_DCOLUMN_SIZE - instanceSize;
        }

        System.out.println(instanceId.toUpperCase() + spaces.substring(0, spacesNum - 1) + formattedTime);
    }

    private static final class DisplayInstance {
        final String parent;
        final String name;
        final long value;

        final ArrayList<DisplayInstance> kids;

        DisplayInstance(String name, String parent, long value, ArrayList<DisplayInstance> kids) {
            this.name = name;
            this.parent = parent;
            this.value = value;
            this.kids = kids;
        }

        public long getValue() {
            return value;
        }

        public String getParent() {
            return parent;
        }

        public ArrayList<DisplayInstance> getKids() {
            return kids;
        }
    }

    /**
     * collects all descendant instances into an array and calculates largest timestamp
     * of their first records.
     *
     * @param result array to add all descendant instances to.
     * @param list the root instance result.
     * @return largest timestamp among first record timestamps.
     */
    private long collectAllKids(ArrayList<DisplayInstance> result, ServerInstanceLogRecordList list) {
        long timestamp = -1;

        RepositoryLogRecord first = list.iterator().next();
        if (first != null) {
            timestamp = first.getMillis();
        }

        for (Entry<String, ServerInstanceLogRecordList> kid : list.getChildren().entrySet()) {
            ArrayList<DisplayInstance> kidResult = new ArrayList<DisplayInstance>();
            long curTimestamp = collectAllKids(kidResult, kid.getValue());
            // Add this kid only if there's a record among its descendants
            if (curTimestamp > 0) {
                result.add(new DisplayInstance(kid.getKey(), Long.toString(list.getStartTime().getTime()), curTimestamp, kidResult));
                if (timestamp < curTimestamp) {
                    timestamp = curTimestamp;
                }
            }
        }

        return timestamp;
    }

    /**
     * Determines the latest child of a given ServerInstanceLogRecordList and returns the latest child's key.
     *
     * @param list ServerInstanceLogRecordList to get the latest child from
     *
     * @return key of the latest child or null if the list does not have any children
     */
    private String getLatestKid(ServerInstanceLogRecordList list) {
        long timestamp = -1;
        String lastKid = null;

        RepositoryLogRecord first = list.iterator().next();
        if (first != null) {
            timestamp = first.getMillis();
        }

        for (Entry<String, ServerInstanceLogRecordList> kid : list.getChildren().entrySet()) {
            ArrayList<DisplayInstance> kidResult = new ArrayList<DisplayInstance>();
            long curTimestamp = collectAllKids(kidResult, kid.getValue());
            // Add this kid only if there's a record among its descendants
            if (curTimestamp > 0) {

                if (timestamp < curTimestamp) {
                    timestamp = curTimestamp;
                    lastKid = kid.getKey();
                }
            }
        }

        return lastKid;
    }

    private void printHeader(Properties pidHeaderProps, PrintStream outps, boolean headerPrinted) {

        applyHeaderPropsToFormatter(theFormatter, pidHeaderProps);

        if (outputRepository == null) {
            if (hasFooter && headerPrinted) {
                // need to print a CBE footer before printing new header.
                outps.println(theFormatter.getFooter());
            }
            // need to print a header
            for (String headerLine : theFormatter.getHeader()) {
                outps.println(headerLine);
            }
        } else {
            outputRepository.storeHeader(pidHeaderProps);
        }

    }

    String getFormattedDateTime(Date dateTime) {
        return instanceDF.format(dateTime);
    }

    String getLevelString() {
        return levelString;
    }

    String getValidatedBinaryRepositoryDir() {
        return binaryRepositoryDir;
    }

    void setValidatedBinaryRepositoryDir(String validatedBinaryRepositoryDir) {
        binaryRepositoryDir = validatedBinaryRepositoryDir;
    }

    /**
     * @param binaryDir
     *            the binaryRepositoryDir to set
     */
    void setBinaryRepositoryDir(String binaryDir) throws IllegalArgumentException {
        File rd = new File(binaryDir);
        /*
         * if (!rd.isDirectory()) {
         * // The binaryRepositoryDir does not exist or is not a directory.
         * throw new IllegalArgumentException(getLocalizedString("CWTRA0000E"));
         * }
         */

        if (!RepositoryReaderImpl.containsLogFiles(rd)) { // subDirectories exist, but no data yet
            if (tailInterval > 0) { // If we are tailing this is OK w/msg
                System.out.println(getLocalizedString_NO_FILES_FOUND_MONITOR());
            } else { // No files and not tailing, we're outta here
                throw new IllegalArgumentException(getLocalizedString_NO_FILES_FOUND());
            }
        }
        setValidatedBinaryRepositoryDir(rd.getAbsolutePath());

    }

    String getLocalizedString_NO_FILES_FOUND_MONITOR() {
        return getLocalizedString("CWTRA0020I");
    }

    String getLocalizedString_NO_FILES_FOUND() {
        return getLocalizedString("CWTRA0021E");
    }

    /**
     * @param localeString
     *            the string representation of the locale to set
     */
    void setLocale(String localeString) {
        this.locale = new Locale(localeString);
    }

    void setDateFormat() {
        theFormatter.setDateFormat(this.locale, true);
    }

    /**
     * @param outputFilename
     *            the outputLogFilename to set
     */
    private void setOutputLogFilename(String outputFilename) throws IllegalArgumentException {
        File theFile = new File(outputFilename).getAbsoluteFile();
        File theDir = theFile.getParentFile();
        if ((theDir != null) && (theDir.isDirectory()) && (theDir.canWrite())) {
            outputLogFilename = theFile.getAbsolutePath();
        } else {
            throw new IllegalArgumentException(getLocalizedString("CWTRA0005E"), null);
        }

    }

    Date getStartDate() {
        return startDate;
    }

    /**
     * @param newStartDate
     *            the startDate to set
     */
    void setStartDate(String newStartDate) throws IllegalArgumentException {
        String sdfPattern = getLocalizedString("CWTRA0001I");
        if (newStartDate.contains(":")) {
            // User passed in a time as well as date
            sdfPattern = getLocalizedString("CWTRA0003I");
        }
        DateFormat df = new SimpleDateFormat(sdfPattern);
        df.setLenient(false);
        try {
            startDate = df.parse(newStartDate);
        } catch (ParseException e) {
            throw new IllegalArgumentException(getLocalizedString("CWTRA0004E"), e);
        }
        //No need to check format of date , as setLenient() is already doing
        //if(!df.format(startDate).equals(newStartDate))
        //	throw new IllegalArgumentException(getLocalizedString("CWTRA0004E"));

    }

    void setStartDate(Date date) {
        startDate = date;
    }

    Date getStopDate() {
        return stopDate;
    }

    /**
     * @param newStopDate
     *            - the stopDate to set
     *
     */
    void setStopDate(String newStopDate) throws IllegalArgumentException {
        String sdfPattern = getLocalizedString("CWTRA0001I");
        if (newStopDate.contains(":")) {
            // User passed in a time as well as date
            sdfPattern = getLocalizedString("CWTRA0003I");
        }
        DateFormat df = new SimpleDateFormat(sdfPattern);
        df.setLenient(false);
        try {
            stopDate = df.parse(newStopDate);
        } catch (ParseException e) {
            throw new IllegalArgumentException(getLocalizedString("CWTRA0006E"), e);
        }
        //No need to check format of date , as setLenient() is already doing
        //if(!df.format(stopDate).equals(newStopDate))
        //	throw new IllegalArgumentException(getLocalizedString("CWTRA0006E"));

    }

    void setStopDate(Date date) {
        stopDate = date;
    }

    /**
     * @param outFormat
     *            - the outFormat to set
     */
    void setOutFormat(String outFormat) throws IllegalArgumentException {
        // Create the formatter
        try {
            theFormatter = HpelFormatter.getFormatter(outFormat);
            hasFooter = "" == theFormatter.getFooter() ? false : true;
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException(getLocalizedParmString_BAD_FORMAT(new Object[] { outFormat }), null);
        }
    }

    String getLocalizedParmString_BAD_FORMAT(Object[] parms) {
        return getLocalizedParmString("CWTRA0028E", parms);
    }

    void setEncoding(String encoding) throws IllegalArgumentException {
        if (!Charset.isSupported(encoding)) {
            throw new IllegalArgumentException(getLocalizedParmString("UnsupportedEncodingError", new Object[] { encoding }));
        }
        this.encoding = encoding;
    }

    void setLatestInstance(boolean latestInstance) {
        this.latestInstance = latestInstance;
    }

    Level getMinLevel() {
        return minLevel;
    }

    /**
     * @param minLevel
     *            the minLevel to set
     */
    void setMinLevel(String minLevel) throws IllegalArgumentException {
        this.minLevel = createLevelByString(minLevel);
    }

    Level getMaxLevel() {
        return maxLevel;
    }

    /**
     * @param maxLevel
     *            - the maxLevel to set
     */
    void setMaxLevel(String maxLevel) throws IllegalArgumentException {
        this.maxLevel = createLevelByString(maxLevel);
        if (this.maxLevel == Level.ALL) {
            // Max level of All implies all records, but All is the lowest level. So just don't limit max level if set
            // to all.
            this.maxLevel = null;
        }
    }

    void setIncludeLoggers(String loggers) {
        includeLoggers = loggers;
    }

    void setExcludeLoggers(String loggers) {
        excludeLoggers = loggers;
    }

    void setHexThreadID(String hexString) throws IllegalArgumentException {
        try {
            Integer.parseInt(hexString, 16); // convert from hex
            hexThreadID = hexString; // parse worked, so hex string is ok.
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(getLocalizedString("CWTRA0008E"), e);
        }
    }

    void setMessage(String message) throws IllegalArgumentException {
        this.message = message;
    }

    void setExcludeMessages(String messages) {
        excludeMessages = messages;
    }

    void setExtensions(String extensions) {

        this.extensions.clear();
        StringBuilder key = new StringBuilder();
        StringBuilder value = new StringBuilder();

        try {

            for (int i = 0; i < extensions.length(); i++) {
                if (extensions.charAt(i) == ',') { // If extension contains ',' search count of bs (/) preceding (,)
                    int index = i - 1;
                    int count = 0;
                    while (true) {
                        if (index > -1 && extensions.charAt(index) == '\\') {
                            count++;
                            index--;
                        } else {
                            break;
                        }

                    }
                    if (count % 2 != 0) { // If no of '/' is odd we need to treat the comma as Literal
                        extensions = extensions.substring(0, i - 1) + "[comma]" + extensions.substring(i + 1);
                        i = i + 5; // Move 5 steps ahead due to addition of [comma] instead of '\,'
                    }

                }
            }
            String[] extension = extensions.split(",");
            for (int i = 0; i < extension.length; i++) {
                key.append(extension[i].substring(0, extension[i].indexOf('=')));
                value.append(extension[i].substring(extension[i].indexOf("=") + 1).replace("[comma]", "\\,"));
                this.extensions.add(new LogViewerFilter.Extension(key, value));

                key.setLength(0);
                value.setLength(0);
            }

        } catch (Exception e) {
            throw new IllegalArgumentException(" Invalid Extensions ", e);

        }

    }

    void setOutputRepository(HPELRepositoryExporter hpelRepositoryExporter) {
        outputRepository = hpelRepositoryExporter;
    }

    void setOutputRepositoryDir(String outputRepositoryDir) throws IllegalArgumentException {
        File theDir = new File(outputRepositoryDir);

        try {
            //if the requested directory to extract to does not exist, try to create it for the user
            if (!theDir.exists()) {
                theDir.mkdirs();
            }

            if (theDir.isDirectory() && theDir.canWrite() && theDir.listFiles().length == 0) {
                setOutputRepository(new HPELRepositoryExporter(theDir));
            } else {
                throw new IllegalArgumentException(getLocalizedString_UNABLE_TO_COPY(), null);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(getLocalizedString_UNABLE_TO_COPY(), null);
        }

    }

    String getLocalizedString_UNABLE_TO_COPY() {
        return getLocalizedString("CWTRA0009E");
    }

    void setListInstances(boolean listInstances) {
        this.listInstances = listInstances;
    }

    int getTailInterval() {
        return tailInterval;
    }

    void setTailInterval(int tailInterval) {
        this.tailInterval = tailInterval;
    }

    /**
     * This method creates a java.util.Level object from a string with the level name.
     *
     * @param levelString
     *            - a String representing the name of the Level.
     * @return a Level object
     */
    private Level createLevelByString(String levelString) throws IllegalArgumentException {
        try {
            return Level.parse(levelString.toUpperCase());
            //return WsLevel.parse(levelString.toUpperCase());
        } catch (Exception npe) {
            throw new IllegalArgumentException(getLocalizedParmString("CWTRA0013E", new Object[] { levelString }));
        }

    }

    /**
     * Parses the instanceId into the requested main process instanceId and the subprocess instanceid. The main
     * process instanceId must be a long value as the main instance Id is a timestamp.
     *
     * @param instanceId - the instanceId requested by the user
     */
    void setInstanceId(String instanceId) throws IllegalArgumentException {
        if (instanceId != null && !"".equals(instanceId)) {

            subInstanceId = getSubProcessInstanceId(instanceId);

            try {
                long id = getProcessInstanceId(instanceId);
                mainInstanceId = id < 0 ? null : new Date(id);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException(getLocalizedString("LVM_ERROR_INSTANCEID"), nfe);
            }
        }
    }

    private void applyHeaderPropsToFormatter(HpelFormatter formatter, Properties props) {
        formatter.setHeaderProps(props);
        if (props == null) {
            throw new IllegalArgumentException("Argument 'sysProps' cannot be null.");
        }

        if (useHeaderTimeZone) {
            String timeZoneID = props.getProperty(ServerInstanceLogRecordList.HEADER_SERVER_TIMEZONE);
            try {
                formatter.setTimeZoneID(timeZoneID);
            } catch (IllegalArgumentException ex) {
                if (timeZoneID != null) {
                    System.err.println(getLocalizedParmString("ErrorUsingHeaderTimeZone", new Object[] { timeZoneID }));
                }
                formatter.setTimeZoneID(TimeZone.getDefault().getID());
            }
        }
    }

    private static abstract class Option {
        // Display name of this option.
        final String name;
        // Number of extra arguments required by this option.
        final int nargs;

        Option(String name, int nargs) {
            this.name = name;
            this.nargs = nargs;
        }

        /**
         * Assign value indicated by this option.
         *
         * @param args
         *            all arguments to the program.
         * @param i
         *            index of the argument after this option
         * @return number of extra arguments consumed including <code>nargs</code>.
         */
        abstract int accept(String args[], int i) throws IllegalArgumentException;
    }

    private static abstract class OneArgOption extends Option {
        OneArgOption(String name) {
            super(name, 1);
        }

        @Override
        int accept(String args[], int i) throws IllegalArgumentException {
            // Default implementation is for options with one required argument.
            setValue(args[i]);
            return 1;
        }

        /**
         * Set value for this option.
         *
         * @param arg
         */
        abstract void setValue(String arg) throws IllegalArgumentException;
    }

    private abstract static class NoArgOption extends Option {
        NoArgOption(String name) {
            super(name, 0);
        }

        @Override
        int accept(String args[], int i) throws IllegalArgumentException {
            enableOption();
            return 0;
        }

        abstract void enableOption();
    }

    private class TailOption extends Option {
        TailOption(String name) {
            super(name, 0);
        }

        @Override
        int accept(String args[], int i) throws IllegalArgumentException {
            tailInterval = DEFAULT_TAIL_INTERVAL;
            if (i < args.length && !args[i].startsWith(OPTION_DELIMITER)) {
                try {
                    tailInterval = Integer.parseInt(args[i]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(getLocalizedParmString("CWTRA0019E", new Object[] { name.substring(1), args[i] }));
                }
                return 1;
            }
            return 0;
        }
    }

    private final Option[] options = new Option[] { new OneArgOption("-repositoryDir") {
        @Override
        void setValue(String arg) {
            setBinaryRepositoryDir(arg);
        }
    }, new NoArgOption("-listInstances") {
        @Override
        void enableOption() {
            listInstances = true;
        }
    }, new NoArgOption("-latestInstance") {
        @Override
        void enableOption() {
            latestInstance = true;
        }
    }, new OneArgOption("-instance") {
        @Override
        void setValue(String arg) throws IllegalArgumentException {
            setInstanceId(arg);
        }
    }, new OneArgOption("-startDate") {
        @Override
        void setValue(String arg) throws IllegalArgumentException {
            setStartDate(arg);
        }
    }, new OneArgOption("-stopDate") {
        @Override
        void setValue(String arg) throws IllegalArgumentException {
            setStopDate(arg);
        }
    }, new OneArgOption("-outLog") {
        @Override
        void setValue(String arg) throws IllegalArgumentException {
            setOutputLogFilename(arg);
        }
    }, new OneArgOption("-format") {
        @Override
        void setValue(String arg) throws IllegalArgumentException {
            setOutFormat(arg);
        }
    }, new OneArgOption("-level") {
        @Override
        void setValue(String arg) throws IllegalArgumentException {
            setMinLevel(arg);
            setMaxLevel(arg);
        }
    }, new OneArgOption("-minLevel") {
        @Override
        void setValue(String arg) throws IllegalArgumentException {
            setMinLevel(arg);
        }
    }, new OneArgOption("-maxLevel") {
        @Override
        void setValue(String arg) throws IllegalArgumentException {
            setMaxLevel(arg);
        }
    }, new OneArgOption("-locale") {
        @Override
        void setValue(String arg) {
            setLocale(arg);
        }
    }, new OneArgOption("-includeLoggers") {
        @Override
        void setValue(String arg) {
            includeLoggers = arg;
        }
    }, new OneArgOption("-excludeLoggers") {
        @Override
        void setValue(String arg) {
            excludeLoggers = arg;
        }
    }, new OneArgOption("-thread") {
        @Override
        void setValue(String arg) throws IllegalArgumentException {
            setHexThreadID(arg);
        }
    }, new OneArgOption("-extractToNewRepository") {
        @Override
        void setValue(String arg) throws IllegalArgumentException {
            setOutputRepositoryDir(arg);
        }
    }, new OneArgOption("-message") {
        @Override
        void setValue(String arg) throws IllegalArgumentException {
            setMessage(arg);
        }
    }, new OneArgOption("-excludeMessages") {
        @Override
        void setValue(String arg) {
            excludeMessages = arg;
        }
    }, new OneArgOption("-includeExtensions") {
        @Override
        public void setValue(String arg) throws IllegalArgumentException {
            setExtensions(arg);
        }
    }, new OneArgOption("-encoding") {
        @Override
        public void setValue(String arg) throws IllegalArgumentException {
            if (!Charset.isSupported(arg)) {
                throw new IllegalArgumentException(getLocalizedParmString("UnsupportedEncodingError", new Object[] { arg }));
            }
            encoding = arg;
        }
    }, new TailOption("-monitor") };

    /**
     * The parseCmdLineArgs method.
     * <p>
     * This method parses the cmd line args and stores them into the local variables.
     *
     * @param args
     *            - command line arguments to LogViewer
     */
    boolean parseCmdLineArgs(String[] args) throws IllegalArgumentException {
        for (int i = 0; i < args.length; i++) {
            if (args[i].toLowerCase().contains("help") || args[i].toLowerCase().contains("usage")) {
                // print the usage information and exit.
                printUsage();
                return true;
            }
            boolean accepted = false;
            for (Option option : options) {
                if (args[i].equalsIgnoreCase(option.name)) {
                    if (i + option.nargs < args.length) {
                        i += option.accept(args, i + 1);
                    } else {
                        throw new IllegalArgumentException(getLocalizedParmString("CWTRA0022E",
                                                                                  new Object[] { option.name, option.nargs }));
                    }
                    accepted = true;
                    break;
                }
            }
            if (!accepted) {
                throw new IllegalArgumentException(getLocalizedParmString("CWTRA0023E", new Object[] { args[i] }));
            }
        }
        return false;
    }

    /**
     * The validateSettings method.
     * <p>
     * This method validates the settings which are configured via cmd line arguments. Invalid settings will either be
     * set to defaults or the program must exit.
     *
     */
    boolean validateSettings() throws IllegalArgumentException {
        // Validate values which do not require user input first to avoid unnecesary
        // work on the user part.

        // dates
        if (startDate != null && stopDate != null) {
            if (startDate.after(stopDate)) {
                // stop date is before the start date.
                throw new IllegalArgumentException(getLocalizedString("CWTRA0026E"));
            }
        }

        // Validate levels if specified.
        if (maxLevel != null && minLevel != null) {
            // We have both a max and min level. Check that the min level is
            // equal to or less than the max level
            if (minLevel.intValue() > maxLevel.intValue()) {
                // Min is greater than Max level. Report error
                throw new IllegalArgumentException(getLocalizedString("CWTRA0027E"));
            }
        }

        // Required options
        if (binaryRepositoryDir == null) {
            File[] servers = listRepositoryChoices();
            File repositoryDirFile = null;
            if (servers.length > 1) {
                System.out.println(getLocalizedString("LVM_SelectServerPrompt"));
                int i = 0;
                for (File s : servers) {
                    i++;
                    System.out.println(i + ") " + getLogDirName(s));
                }
                String serverNum = null;
                int serverNumInt = 0;
                BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
                while (repositoryDirFile == null) {
                    System.out.print("[1-" + i + "]: ");
                    try {
                        serverNum = input.readLine();
                        serverNumInt = Integer.parseInt(serverNum);
                        if ((serverNumInt >= 1) && (serverNumInt <= i)) {
                            repositoryDirFile = servers[serverNumInt - 1];
                        }
                    } catch (NumberFormatException e) {
                        // Line does not contain a number.
                    } catch (IOException e) {
                        // Assume user just want to exit.
                        return true;
                    }
                } // end while loop
            } else if (servers.length == 1) {
                repositoryDirFile = servers[0];
            }

            if (repositoryDirFile != null) {
                // appears we have a repositoryDir
                try {
                    setBinaryRepositoryDir(repositoryDirFile.getCanonicalPath());
                    System.out.println(getLocalizedParmString("CWTRA0030I", new Object[] { binaryRepositoryDir }));
                } catch (IOException e) {
                    // TODO We don't want to print stack traces so what should we be doing here?
                    //e.printStackTrace();
                }
            }

            if (binaryRepositoryDir == null) {
                throw new IllegalArgumentException(getLocalizedString("CWTRA0024E"));
            }
        }

        return false;

    }

    /**
     * Lists directories containing HPEL repositories. It is called
     * when repository is not specified explicitly in the arguments
     *
     * @return List of files which can be used as repositories
     */
    protected File[] listRepositoryChoices() {
        // check current location
        String currentDir = System.getProperty("log.repository.root");
        if (currentDir == null)
            currentDir = System.getProperty("user.dir");
        File logDir = new File(currentDir);
        if (logDir.isDirectory()) {
            File[] result = RepositoryReaderImpl.listRepositories(logDir);
            if (result.length == 0 && (RepositoryReaderImpl.containsLogFiles(logDir) || tailInterval > 0)) {
                return new File[] { logDir };
            } else {
                return result;
            }
        } else {
            return new File[] {};
        }
    }

    /**
     * Returns identifier to be used for the repository. It is called with
     * instances returned in {@link #listRepositoryChoices()} call as input.
     * The result is presented to the user to make a choice among the
     * available repositories.
     *
     * @param repository directory
     * @return String identifier to make a choice.
     */
    protected String getLogDirName(File repository) {
        return repository.getName();
    }

    /**
     * The printUsage method.
     * <p>
     * Prints the usage/help to the System.out stream.
     *
     */
    private void printUsage() {

        String WINDOWS_SAMPLE_PATH = "C:\\temp\\newRepository";
        String UNIX_SAMPLE_PATH = "/tmp/newRepository";

        Calendar calendar = Calendar.getInstance();
        DateFormat df1 = new SimpleDateFormat(getLocalizedString("CWTRA0001I"));
        DateFormat df2 = new SimpleDateFormat(getLocalizedString("CWTRA0003I"));
        DateFormat df3 = new SimpleDateFormat(getLocalizedString("CWTRA0002I"));

        //sample to display   -startDate 1/30/09
        calendar.set(2009, 0, 30);
        Date date = calendar.getTime();
        String fStartDate1 = df1.format(date);

        //sample to display -startDate "1/30/09 04:00:00:100 CST"
        calendar.set(2009, 0, 30, 4, 0, 0);
        calendar.set(Calendar.MILLISECOND, 100);
        date = calendar.getTime();
        String fStartDate2 = df2.format(date);

        //sample to display  -stopDate 5/28/09
        calendar.set(2009, 4, 28);
        date = calendar.getTime();
        String fStopDate1 = df1.format(date);

        //sample to display -stopDate "6/28/09 14:32:9:763 CDT"
        calendar.set(2009, 4, 28, 14, 32, 9);
        calendar.set(Calendar.MILLISECOND, 100);
        date = calendar.getTime();
        String fStopDate2 = df2.format(date);

        //sample to display begin range "April 27,2010 1:30 A.M. CDT"
        calendar.set(2010, 3, 27, 1, 30, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        date = calendar.getTime();
        String fBeginDate3 = df3.format(date);
        //display -startDate "04/27/10 01:30:00:000 CDT"
        String fStartDate3 = df2.format(date);

        //sample to display begin range" April 27,2010 1:35 A.M. CDT"
        calendar.set(2010, 3, 27, 1, 35, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        date = calendar.getTime();
        String fEndDate3 = df3.format(date);
        //display -stopDate "04/27/10 01:35:00:000 CDT"
        String fStopDate3 = df2.format(date);

        String uDesc = getLocalizedString("CWTRA0031I");
        String uUsage = getLocalizedString("CWTRA0033I");
        StringBuilder uOptions = new StringBuilder(300);
        uOptions.append(getLocalizedString("CWTRA0034I"));
        uOptions.append("\n\n");
        uOptions.append(getLocalizedString("CWTRA0035I") + "\n\n" + getLocalizedString("CWTRA0036I"));
        uOptions.append("\n\n");
        uOptions.append(getLocalizedString("CWTRA0037I") + "\n\n"
                        + getLocalizedParmString("CWTRA0038I", new Object[] { getLocalizedString("CWTRA0074I"), getLocalizedString("CWTRA0075I") }));
        uOptions.append("\n\n");
        uOptions.append(getLocalizedParmString("CWTRA0039I", new Object[] { fStartDate1, fStartDate2 }));
        uOptions.append("\n\n");
        uOptions.append(getLocalizedString("CWTRA0040I") + "\n\n"
                        + getLocalizedParmString("CWTRA0041I", new Object[] { getLocalizedString("CWTRA0074I"), getLocalizedString("CWTRA0075I") }));
        uOptions.append("\n\n");
        uOptions.append(getLocalizedParmString("CWTRA0042I", new Object[] { fStopDate1, fStopDate2 }));
        uOptions.append("\n\n");
        uOptions.append("-level ").append(levelString).append("\n\n").append(getLocalizedString("CWTRA0044I"));
        uOptions.append("\n\n");
        uOptions.append("-minLevel ").append(levelString).append("\n\n").append(getLocalizedString("CWTRA0046I"));
        uOptions.append("\n\n");
        uOptions.append("-maxLevel ").append(levelString).append("\n\n").append(getLocalizedString("CWTRA0048I"));
        uOptions.append("\n\n");
        uOptions.append(getLocalizedString("CWTRA0049I") + "\n\n" + getLocalizedString("CWTRA0050I"));
        uOptions.append("\n\n");
        uOptions.append(getLocalizedString("CWTRA0053I") + "\n\n" + getLocalizedString("CWTRA0052I"));
        uOptions.append("\n\n");
        uOptions.append(getLocalizedString("CWTRA0055I") + "\n\n" + getLocalizedString("CWTRA0056I"));
        uOptions.append("\n\n");
        uOptions.append(getLocalizedString("CWTRA0057I"));
        uOptions.append("\n\n");
        uOptions.append(getLocalizedString("CWTRA0058I") + "\n\n" + getLocalizedString("CWTRA0059I"));
        uOptions.append("\n\n");
        uOptions.append(getLocalizedString("CWTRA0060I"));
        uOptions.append("\n\n");
        uOptions.append(getLocalizedString("CWTRA0061I") + "\n\n" + getLocalizedString("CWTRA0062I"));
        uOptions.append("\n\n");
        uOptions.append(getLocalizedString("CWTRA0065I") + "\n\n" + getLocalizedString("CWTRA0066I"));
        uOptions.append("\n\n");
        uOptions.append(getLocalizedString("CWTRA0067I") + "\n\n" + getLocalizedString("CWTRA0068I"));
        uOptions.append("\n\n");
        uOptions.append(getLocalizedString("CWTRA0069I") + "\n\n" + getLocalizedString("CWTRA0070I"));
        uOptions.append("\n\n");
        uOptions.append(getLocalizedString("LVM_HELP_LATESTINSTANCE") + "\n\n" + getLocalizedString("LVM_HELP_LATESTINSTANCE_DESCR"));
        uOptions.append("\n\n");
        uOptions.append(getLocalizedString("LVM_HELP_MESSAGE") + "\n\n" + getLocalizedString("LVM_HELP_MESSAGE_DESCR"));
        uOptions.append("\n\n");
        uOptions.append(getLocalizedString("LVM_HELP_EXTENSIONS") + "\n\n" + getLocalizedString("LVM_HELP_EXTENSIONS_DESCR"));
        uOptions.append("\n\n");
        uOptions.append(getLocalizedString("LVM_HELP_ENCODING") + "\n\n" + getLocalizedString("LVM_HELP_ENCODING_DESCR"));

        System.out.println(uDesc + "\n\n");
        System.out.println(uUsage + "\n\n");
        System.out.println(uOptions + "\n\n");
        System.out.println(getLocalizedString("LVM_HELP_SAMPLES_INTRO") + "\n\n");
        System.out.println(getLocalizedString("LVM_HELP_SAMPLE1") + "\n\n");
        System.out.println(getLocalizedString("LVM_HELP_SAMPLE4") + "\n\n");
        System.out.println(getLocalizedParmString("LVM_HELP_SAMPLE2", new Object[] { fBeginDate3, fEndDate3, fStartDate3, fStopDate3 }) + "\n\n");

        if (File.separator.equals("/")) {
            System.out.println(getLocalizedParmString("LVM_HELP_SAMPLE3", new Object[] { fBeginDate3, UNIX_SAMPLE_PATH, fStartDate3 }));
        } else {
            System.out.println(getLocalizedParmString("LVM_HELP_SAMPLE3", new Object[] { fBeginDate3, WINDOWS_SAMPLE_PATH, fStartDate3 }));
        }

    }

    /**
     * The createOutputSteam method.
     * <p>
     * Creates or sets up (in the case of the console) the output steam that LogViewer will use to write to.
     *
     * @return PrintStream representing the output file or the System.out or console.
     *
     */
    private PrintStream createOutputStream() {
        if (outputLogFilename != null) {
            try {
                FileOutputStream fout = new FileOutputStream(outputLogFilename, false);
                BufferedOutputStream bos = new BufferedOutputStream(fout, 4096);
                // We are using a PrintStream for output to match System.out
                if (encoding != null) {
                    return new PrintStream(bos, false, encoding);
                } else {
                    return new PrintStream(bos, false);
                }
            } catch (IOException e) {
                throw new IllegalArgumentException(getLocalizedString("CWTRA0005E"));
            }
        }

        // No output filename specified.
        isSystemOut = true;
        if (encoding != null) {
            try {
                // Encode output before directing it into System.out
                return new PrintStream(System.out, false, encoding);
            } catch (UnsupportedEncodingException e) {
                // We already checked that encoding is supported
            }
        }
        return System.out;
    }

    private static String getLocalizedString(String key) {
        return getLocalizedString(BUNDLE_NAME, key);
    }

    static String getLocalizedString(String bundleName, String key) {
        try {
            //rather than return an empty string, need to return a default value when the resource bundle can not be resolved
            return Messages.getStringFromBundle(bundleName, key, null, key);
            //return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    private static String getLocalizedParmString(String key, Object[] parms) {
        return getLocalizedParmString(BUNDLE_NAME, key, parms);
    }

    static String getLocalizedParmString(String bundleName, String key, Object[] parms) {
        return java.text.MessageFormat.format(getLocalizedString(bundleName, key), parms);
    }

    // used by our unit tests
    @Override
    public String toString() {
        return "latestInstance=" + Boolean.toString(latestInstance) +
               ", minLevel=" + ((minLevel != null) ? minLevel.toString() : "null") +
               ", maxLevel=" + ((maxLevel != null) ? maxLevel.toString() : "null") +
               ", startDate=" + ((startDate != null) ? dateFormat.format(startDate) : "null") +
               ", stopDate=" + ((stopDate != null) ? dateFormat.format(stopDate) : "null") +
               ", includeLoggers=" + includeLoggers +
               ", excludeLoggers=" + excludeLoggers +
               ", hexThreadID=" + hexThreadID +
               ", tailInterval=" + Integer.toString(tailInterval) +
               ", locale=" + ((locale != null) ? locale.toString() : "null") +
               ", message=" + message +
               ", excludeMessages=" + excludeMessages +
               ", extensions=" + ((extensions != null) ? extensions.toString() : "null") +
               ", encoding=" + encoding;

        //TODO: would be nice to also validate...
        //outputRepository

    }

}
