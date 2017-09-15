/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.logging.hpel.reader;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import com.ibm.websphere.logging.hpel.reader.filters.LevelFilter;
import com.ibm.websphere.logging.hpel.reader.filters.MultipleCriteriaFilter;
import com.ibm.ws.logging.hpel.LogRepositoryBrowser;
import com.ibm.ws.logging.hpel.MainLogRepositoryBrowser;
import com.ibm.ws.logging.hpel.impl.LogRecordBrowser;
import com.ibm.ws.logging.hpel.impl.LogRecordBrowser.OnePidRecordListImpl;
import com.ibm.ws.logging.hpel.impl.LogRepositoryBrowserImpl;
import com.ibm.ws.logging.hpel.impl.MainLogRepositoryBrowserImpl;
import com.ibm.ws.logging.hpel.impl.OneFileBrowserImpl;
import com.ibm.ws.logging.hpel.impl.OneInstanceBrowserImpl;
import com.ibm.ws.logging.hpel.impl.ServerInstanceLogRecordListImpl;
import com.ibm.ws.logging.hpel.impl.ZipGenericFile;
import com.ibm.ws.logging.object.hpel.RepositoryPointerImpl;

/**
 * Implementation of the {@link RepositoryReader} providing access to a local HPEL repository.
 * 
 * @ibm-api
 */
public class RepositoryReaderImpl implements RepositoryReader {
    /*
     * private final static String BUNDLE_NAME = "com.ibm.ws.logging.hpel.resources.HpelMessages";
     * private final static String className = RepositoryReaderImpl.class.getName();
     * private final static Logger logger = Logger.getLogger(className, BUNDLE_NAME);
     */

    /** Constructor must make sure that logBrowser is never <code>null</code> */
    private final MainLogRepositoryBrowser logInstanceBrowser;
    private final MainLogRepositoryBrowser traceInstanceBrowser;

    /**
     * creates RepositoryReader instance working on the binary log set.
     * 
     * @param directory or zip file containing log and/or trace files.
     */
    public RepositoryReaderImpl(String directory) {
        this(directory, directory);
    }

    /**
     * Creates RepositoryReader instance working on the binary log set and trace log set. This allows for a merging of repositories
     * in directories which may not be adjacent.
     * 
     * @param logDirectory containing log files. If this is null, then use trace only
     * @param traceDirectory containing log files. If this is null, then use log only
     * @throws IllegalArgumentException if both <code>logDirectory</code> and <code>traceDirectory</code> are null.
     */
    public RepositoryReaderImpl(String logDirectory, String traceDirectory) {
        this(logDirectory == null ? null : new File(logDirectory),
             traceDirectory == null ? null : new File(traceDirectory));
    }

    /**
     * creates RepositoryReader instance working on the binary log set.
     * 
     * @param directory or zip file containing log and/or trace files.
     */
    public RepositoryReaderImpl(File directory) {
        this(directory, directory);
    }

    /**
     * Creates RepositoryReader instance working on the binary log set and trace log set. This allows for a merging of repositories
     * in directories which may not be adjacent.
     * 
     * @param logLocation containing log files. If this is null, then use trace only
     * @param traceLocation containing log files. If this is null, then use log only
     * @throws IllegalArgumentException if both <code>logLocation</code> and <code>traceLocation</code> are null.
     */
    public RepositoryReaderImpl(File logLocation, File traceLocation) {
        if (logLocation == null && traceLocation == null) {
            throw new IllegalArgumentException("At least one \"logLocation\" or \"traceLocation\" should be specified.");
        }

        if (traceLocation != null) {
            traceLocation = checkKnownType(traceLocation);
            traceLocation = verifyLocation(traceLocation, LogRepositoryBrowserImpl.TRACE_LOCATION);
        }

        if (logLocation == null) {
            logLocation = traceLocation;
        } else {
            logLocation = checkKnownType(logLocation);
            logLocation = verifyLocation(logLocation, LogRepositoryBrowserImpl.DEFAULT_LOCATION);
        }

        // Avoid accidently duplicating records.
        if (logLocation.equals(traceLocation)) {
            traceLocation = null;
        }

        logInstanceBrowser = createBrowser(logLocation);
        if (traceLocation == null) {
            traceInstanceBrowser = null; //disable merging
        } else {
            traceInstanceBrowser = createBrowser(traceLocation);
        }
    }

    private static MainLogRepositoryBrowser createBrowser(File location) {
        if (isFile(location)) {
            // If it's a file return browser over one file.
            return new OneInstanceBrowserImpl(new OneFileBrowserImpl(location));
        }

        if (location.isDirectory()) {
            LogRepositoryBrowser browser = new LogRepositoryBrowserImpl(location, new String[] { location.getName() });
            if (browser.findNext((File) null, -1) != null || !browser.getSubProcesses().isEmpty()) {
                // If it contains log files or subprocesses, browse over one instance.
                return new OneInstanceBrowserImpl(browser);
            }
        }

        return new MainLogRepositoryBrowserImpl(location);
    }

    private static File verifyLocation(File location, String suffix) {
        // If location is a file use it as a repository.
        if (isFile(location)) {
            return location;
        }

        // If location already contains the expected name it is a repository.
        if (suffix.equals(location.getName())) {
            return location;
        }

        // If appending expected name can be a directory return it as a repository.
        File newLocation = getChild(location, suffix);
        if (newLocation.isDirectory()) {
            return newLocation;
        }

        if (location.isDirectory()) {
            // If it contains log files it is a repository
            LogRepositoryBrowserImpl browser = new LogRepositoryBrowserImpl(location, new String[] { location.getName() });
            if (browser.findNext((File) null, -1) != null) {
                return location;
            }

            // If its directories contain WBL files it is a repository
            File[] wblDirs = location.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isDirectory() && new LogRepositoryBrowserImpl(pathname, new String[] { pathname.getName() }).findNext((File) null, -1) != null;
                }
            });
            if (wblDirs.length > 0) {
                return location;
            }

            // If location contains instance directories it is a repository.		
            MainLogRepositoryBrowserImpl fileBrowser = new MainLogRepositoryBrowserImpl(location);
            // If it contains instance directories it's a repository.
            if (fileBrowser.findByMillis(-1) != null) {
                return location;
            }
        }

        if (!fileExists(newLocation)) {
            return newLocation;
        }

        return location;
    }

    /**
     * @param newLocation
     * @return
     */
    private static boolean fileExists(final File newLocation) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return newLocation.exists();
            }
        });
    }

    /**
     * Returns log directory used by this reader.
     * 
     * @return absolute path to log directory.
     */
    public String getLogLocation() {
        return ((MainLogRepositoryBrowserImpl) logInstanceBrowser).getLocation().getAbsolutePath();
    }

    /**
     * Returns trace directory used by this reader.
     * 
     * @return absolute path to trace directory.
     */
    public String getTraceLocation() {
        if (traceInstanceBrowser != null) {
            return ((MainLogRepositoryBrowserImpl) traceInstanceBrowser).getLocation().getAbsolutePath();
        } else {
            return null;
        }
    }

    private static File getChild(File pathname, String name) {
        if (pathname instanceof GenericFile) {
            return ((GenericFile) pathname).getChild(name);
        } else {
            return new File(pathname, name);
        }
    }

    private static File checkKnownType(File location) {
        if (location instanceof GenericFile || !isFile(location)) {
            return location;
        }

        try {
            return new ZipGenericFile(location);
        } catch (IOException e) {
            // Not a zip file, fall through
        }

        // Unknown type keep it as it is.
        return location;

    }

    /**
     * checks if specified location contains log files.
     * 
     * @param location to check.
     * @return <code>true</code> if the directory itself or known subdirectories contain log files.
     */
    public static boolean containsLogFiles(File location) {
        if (location == null) {
            return false;
        }

        location = checkKnownType(location);

        if (isFile(location)) {
            try {
                // Is a WBL file.
                if (new OneFileBrowserImpl(location).getProcessId() != null) {
                    return true;
                }
            } catch (IllegalArgumentException ex) {
                // Fall through, can't use as a WBL file.
            }
            return false;
        }

        if (location.isDirectory()) {
            // Contains WBL files.
            LogRepositoryBrowserImpl browser = new LogRepositoryBrowserImpl(location, new String[] { location.getName() });
            if (browser.findNext((File) null, -1) != null) {
                return true;
            }

            // Its directories contain WBL files
            File[] wblDirs = location.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isDirectory() && new LogRepositoryBrowserImpl(pathname, new String[] { pathname.getName() }).findNext((File) null, -1) != null;
                }
            });
            if (wblDirs.length > 0) {
                return true;
            }
        }

        for (File file : new File[] {
                                     location,
                                     getChild(location, LogRepositoryBrowserImpl.DEFAULT_LOCATION),
                                     getChild(location, LogRepositoryBrowserImpl.TRACE_LOCATION) }) {
            // Contains Instance directories
            if (file.isDirectory()) {
                MainLogRepositoryBrowserImpl fileBrowser = new MainLogRepositoryBrowserImpl(file);
                if (fileBrowser.findByMillis(-1) != null) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @param location
     * @return
     */
    private static boolean isFile(final File location) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return location.isFile();
            }
        });
    }

    /**
     * list subdirectories containing repository files.
     * 
     * @param parent the directory to check subdirectories in.
     * @return list of subdirectory names containing repository files.
     */
    public static File[] listRepositories(File parent) {
        if (parent == null) {
            throw new IllegalArgumentException("Input parameter can't be null.");
        }
        parent = checkKnownType(parent);
        if (!parent.isDirectory()) {
            throw new IllegalArgumentException("Input parameter should be an existing directory: " + parent);
        }
        File[] result = parent.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory() &&
                       getChild(pathname, LogRepositoryBrowserImpl.DEFAULT_LOCATION).isDirectory() ||
                       getChild(pathname, LogRepositoryBrowserImpl.TRACE_LOCATION).isDirectory();
            }
        });
        return result == null ? new File[] {} : result;
    }

    /**
     * returns log records from the binary repository
     * 
     * @return the iterable instance of a list of log records
     *         If no records are available, the iterable has no entries
     */
    @Override
    public Iterable<ServerInstanceLogRecordList> getLogLists() {
        return getLogLists((Date) null, (Date) null, (LogRecordHeaderFilter) null);
    }

    /**
     * returns log records from the binary repository that are within the date range and which satisfy condition of the filter as specified by the parameters.
     * 
     * @param after pointer to the a repository location where the query should start. Only includes records after this point
     * @return the iterable instance of a list of log records within a process that are within the parameter range and satisfy the condition.
     *         If no records exist after the location, an Iterable is returned with no entries
     */
    @Override
    public Iterable<ServerInstanceLogRecordList> getLogLists(RepositoryPointer after) {
        return getLogLists(after, (Date) null, (LogRecordHeaderFilter) null);
    }

    /**
     * returns log records from the binary repository that are within the date range and which satisfy condition of the filter as specified by the parameters.
     * 
     * @param beginTime the minimum {@link Date} value that the returned records can have
     * @param endTime the maximum {@link Date} value that the returned records can have
     * @param filter an instance implementing {@link LogRecordHeaderFilter} interface to verify one record at a time.
     * @return the iterable instance of a list of log records within a process that are within the parameter range and satisfy the condition.
     *         If no records meet the criteria, an Iterable is returned with no entries
     */
    public Iterable<ServerInstanceLogRecordList> getLogLists(Date beginTime, Date endTime, final LogRecordHeaderFilter filter) {
        final long min = beginTime == null ? -1 : beginTime.getTime();
        final long max = endTime == null ? -1 : endTime.getTime();

        LogRepositoryBrowser logs;
        if (beginTime == null) {
            logs = logInstanceBrowser.findNext((LogRepositoryBrowser) null, max);
        } else {
            logs = logInstanceBrowser.findByMillis(min);
            // Get first instance if min is earlier than the first record
            if (logs == null) {
                logs = logInstanceBrowser.findNext((LogRepositoryBrowser) null, max);
            }

        }
        LogRepositoryBrowser traces;
        if (traceInstanceBrowser != null) {
            if (beginTime == null) {
                traces = traceInstanceBrowser.findNext((LogRepositoryBrowser) null, max);
            } else {
                traces = traceInstanceBrowser.findByMillis(min);
                // Get first instance if min is earlier than the first record
                if (traces == null) {
                    traces = traceInstanceBrowser.findNext((LogRepositoryBrowser) null, max);
                }
            }
        } else {
            traces = null;
        }
        final LogRepositoryBrowser finalLogs = logs;
        final LogRepositoryBrowser finalTraces = traces;

        return new Iterable<ServerInstanceLogRecordList>() {
            @Override
            public Iterator<ServerInstanceLogRecordList> iterator() {
                return new ServerInstanceListsIterator(max, finalLogs, finalTraces) {
                    @Override
                    protected OnePidRecordListImpl queryResult(
                                                               LogRepositoryBrowser browser) {
                        return new LogRecordBrowser(browser).recordsInProcess(min, max, filter);
                    }
                };
            }
        };
    }

    /**
     * returns log records from the binary repository that are beyond a given repository location, occured before a given time, and meet a filter condition
     * as specified by the parameters. Callers would have to invoke {@link RepositoryLogRecord#getRepositoryPointer()} to obtain the RepositoryPointer
     * of the last record read.
     * 
     * @param after RepositoryPointer of the last read log record.
     * @param endTime the maximum {@link Date} value that the returned records can have
     * @param filter an instance implementing {@link LogRecordHeaderFilter} interface to verify one record at a time.
     * @return the iterable instance of a list of log records within a process that are within the parameter range and satisfy the condition.
     *         If no records meet the criteria, an Iterable is returned with no entries
     */
    public Iterable<ServerInstanceLogRecordList> getLogLists(RepositoryPointer after, Date endTime, final LogRecordHeaderFilter filter) {
        if (after instanceof RepositoryPointerImpl) {
            final RepositoryPointerImpl location = (RepositoryPointerImpl) after;
            final long max = endTime == null ? -1 : endTime.getTime();

            LogRepositoryBrowser logs = logInstanceBrowser.find(location, false);
            LogRepositoryBrowser traces = null;
            if (traceInstanceBrowser != null) {
                traces = traceInstanceBrowser.find(location, false);
            }

            if (logs == null && traces == null) {
                return EMPTY_ITERABLE;
            }

            // Check that location come from the log.
            if (logs != null) {
                final RepositoryLogRecord current = new LogRecordBrowser(logs).getRecord(location);
                if (current != null) {
                    final LogRepositoryBrowser finalLogs = logs;
                    final LogRepositoryBrowser finalTraces = traceInstanceBrowser == null ? null : traceInstanceBrowser.find(location, true);
                    return new Iterable<ServerInstanceLogRecordList>() {
                        @Override
                        public Iterator<ServerInstanceLogRecordList> iterator() {
                            return new ServerInstanceListsIterator(max, finalLogs, finalTraces) {
                                @Override
                                protected OnePidRecordListImpl queryResult(
                                                                           LogRepositoryBrowser browser) {
                                    return new LogRecordBrowser(browser).recordsInProcess(-1, max, filter);
                                }

                                @Override
                                protected ServerInstanceLogRecordList queryFirstInstance(
                                                                                         LogRepositoryBrowser firstLog,
                                                                                         LogRepositoryBrowser firstTrace) {
                                    return new ServerInstanceLogRecordListHeaderPointerImpl(firstLog, firstTrace, false, location, current, filter, max);
                                }
                            };
                        }
                    };
                }
            }

            // Check that location come from the trace.
            if (traces != null) {
                final RepositoryLogRecord current = new LogRecordBrowser(traces).getRecord(location);
                if (current != null) {
                    final LogRepositoryBrowser finalLogs = logInstanceBrowser.find(location, true);
                    final LogRepositoryBrowser finalTraces = traces;
                    return new Iterable<ServerInstanceLogRecordList>() {
                        @Override
                        public Iterator<ServerInstanceLogRecordList> iterator() {
                            return new ServerInstanceListsIterator(max, finalLogs, finalTraces) {
                                @Override
                                protected OnePidRecordListImpl queryResult(
                                                                           LogRepositoryBrowser browser) {
                                    return new LogRecordBrowser(browser).recordsInProcess(-1, max, filter);
                                }

                                @Override
                                protected ServerInstanceLogRecordList queryFirstInstance(
                                                                                         LogRepositoryBrowser firstLog,
                                                                                         LogRepositoryBrowser firstTrace) {
                                    return new ServerInstanceLogRecordListHeaderPointerImpl(firstTrace, firstLog, true, location, current, filter, max);
                                }
                            };
                        }
                    };
                }
            }

            // Neither trace nor log contain the location which means that file containing
            // it was purged already and that we can just return all the records we can find.
            final LogRepositoryBrowser finalLogs = logInstanceBrowser.find(location, true);
            final LogRepositoryBrowser finalTraces = traceInstanceBrowser == null ? null : traceInstanceBrowser.find(location, true);
            return new Iterable<ServerInstanceLogRecordList>() {
                @Override
                public Iterator<ServerInstanceLogRecordList> iterator() {
                    return new ServerInstanceListsIterator(max, finalLogs, finalTraces) {
                        @Override
                        protected OnePidRecordListImpl queryResult(
                                                                   LogRepositoryBrowser browser) {
                            return new LogRecordBrowser(browser).recordsInProcess(-1, max, filter);
                        }
                    };
                }
            };

        } else if (after != null) {
            throw new IllegalArgumentException("This method accept only RepositoryPointer instances retrieved from previously read records");
        }

        return getLogLists((Date) null, endTime, filter);
    }

    /**
     * returns log records from the binary repository that are within the date range and which satisfy condition of the filter as specified by the parameters.
     * 
     * @param beginTime the minimum {@link Date} value that the returned records can have
     * @param endTime the maximum {@link Date} value that the returned records can have
     * @param filter an instance implementing {@link LogRecordFilter} interface to verify one record at a time.
     * @return the iterable instance of a list of log records within a process that are within the parameter range and satisfy the condition.
     *         If no records meet the criteria, an Iterable is returned with no entries
     */
    public Iterable<ServerInstanceLogRecordList> getLogLists(Date beginTime, Date endTime, final LogRecordFilter filter) {
        final long min = beginTime == null ? -1 : beginTime.getTime();
        final long max = endTime == null ? -1 : endTime.getTime();

        LogRepositoryBrowser logs;
        if (beginTime == null) {
            logs = logInstanceBrowser.findNext((LogRepositoryBrowser) null, max);
        } else {
            logs = logInstanceBrowser.findByMillis(min);
            // Get first instance if min is earlier than the first record
            if (logs == null) {
                logs = logInstanceBrowser.findNext((LogRepositoryBrowser) null, max);
            }

        }
        LogRepositoryBrowser traces;
        if (traceInstanceBrowser != null) {
            if (beginTime == null) {
                traces = traceInstanceBrowser.findNext((LogRepositoryBrowser) null, max);
            } else {
                traces = traceInstanceBrowser.findByMillis(min);
                // Get first instance if min is earlier than the first record
                if (traces == null) {
                    traces = traceInstanceBrowser.findNext((LogRepositoryBrowser) null, max);
                }
            }
        } else {
            traces = null;
        }
        final LogRepositoryBrowser finalLogs = logs;
        final LogRepositoryBrowser finalTraces = traces;

        return new Iterable<ServerInstanceLogRecordList>() {
            @Override
            public Iterator<ServerInstanceLogRecordList> iterator() {
                return new ServerInstanceListsIterator(max, finalLogs, finalTraces) {
                    @Override
                    protected OnePidRecordListImpl queryResult(
                                                               LogRepositoryBrowser browser) {
                        return new LogRecordBrowser(browser).recordsInProcess(min, max, filter);
                    }
                };
            }
        };
    }

    /**
     * returns log records from the binary repository that are beyond a given repository location, occured before a given time, and meet a filter condition
     * as specified by the parameters. Callers would have to invoke {@link RepositoryLogRecord#getRepositoryPointer()} to obtain the RepositoryPointer
     * of the last record read.
     * 
     * @param after RepositoryPointer of the last read log record.
     * @param endTime the maximum {@link Date} value that the returned records can have
     * @param filter an instance implementing {@link LogRecordFilter} interface to verify one record at a time.
     * @return the iterable instance of a list of log records within a process that are within the parameter range and satisfy the condition.
     *         If no records meet the criteria, an Iterable is returned with no entries
     */
    public Iterable<ServerInstanceLogRecordList> getLogLists(RepositoryPointer after, Date endTime, final LogRecordFilter filter) {
        if (after instanceof RepositoryPointerImpl) {
            final RepositoryPointerImpl location = (RepositoryPointerImpl) after;
            final long max = endTime == null ? -1 : endTime.getTime();

            LogRepositoryBrowser logs = logInstanceBrowser.find(location, false);
            LogRepositoryBrowser traces = null;
            if (traceInstanceBrowser != null) {
                traces = traceInstanceBrowser.find(location, false);
            }

            if (logs == null && traces == null) {
                return EMPTY_ITERABLE;
            }

            // Check that location come from the log.
            if (logs != null) {
                final RepositoryLogRecord current = new LogRecordBrowser(logs).getRecord(location);
                if (current != null) {
                    final LogRepositoryBrowser finalLogs = logs;
                    final LogRepositoryBrowser finalTraces = traceInstanceBrowser == null ? null : traceInstanceBrowser.find(location, true);
                    return new Iterable<ServerInstanceLogRecordList>() {
                        @Override
                        public Iterator<ServerInstanceLogRecordList> iterator() {
                            return new ServerInstanceListsIterator(max, finalLogs, finalTraces) {
                                @Override
                                protected OnePidRecordListImpl queryResult(
                                                                           LogRepositoryBrowser browser) {
                                    return new LogRecordBrowser(browser).recordsInProcess(-1, max, filter);
                                }

                                @Override
                                protected ServerInstanceLogRecordList queryFirstInstance(
                                                                                         LogRepositoryBrowser firstLog,
                                                                                         LogRepositoryBrowser firstTrace) {
                                    return new ServerInstanceLogRecordListPointerImpl(firstLog, firstTrace, false, location, current, filter, max);
                                }
                            };
                        }
                    };
                }
            }

            // Check that location come from the trace.
            if (traces != null) {
                final RepositoryLogRecord current = new LogRecordBrowser(traces).getRecord(location);
                if (current != null) {
                    final LogRepositoryBrowser finalLogs = logInstanceBrowser.find(location, true);
                    final LogRepositoryBrowser finalTraces = traces;
                    return new Iterable<ServerInstanceLogRecordList>() {
                        @Override
                        public Iterator<ServerInstanceLogRecordList> iterator() {
                            return new ServerInstanceListsIterator(max, finalLogs, finalTraces) {
                                @Override
                                protected OnePidRecordListImpl queryResult(
                                                                           LogRepositoryBrowser browser) {
                                    return new LogRecordBrowser(browser).recordsInProcess(-1, max, filter);
                                }

                                @Override
                                protected ServerInstanceLogRecordList queryFirstInstance(
                                                                                         LogRepositoryBrowser firstLog,
                                                                                         LogRepositoryBrowser firstTrace) {
                                    return new ServerInstanceLogRecordListPointerImpl(firstTrace, firstLog, true, location, current, filter, max);
                                }
                            };
                        }
                    };
                }
            }

            // Neither trace nor log contain the location which means that file containing
            // it was purged already and that we can just return all the records we can find.
            final LogRepositoryBrowser finalLogs = logInstanceBrowser.find(location, true);
            final LogRepositoryBrowser finalTraces = traceInstanceBrowser == null ? null : traceInstanceBrowser.find(location, true);
            return new Iterable<ServerInstanceLogRecordList>() {
                @Override
                public Iterator<ServerInstanceLogRecordList> iterator() {
                    return new ServerInstanceListsIterator(max, finalLogs, finalTraces) {
                        @Override
                        protected OnePidRecordListImpl queryResult(
                                                                   LogRepositoryBrowser browser) {
                            return new LogRecordBrowser(browser).recordsInProcess(-1, max, filter);
                        }
                    };
                }
            };

        } else if (after != null) {
            throw new IllegalArgumentException("This method accept only RepositoryPointer instances retrieved from previously read records");
        }

        return getLogLists((Date) null, endTime, filter);
    }

    /**
     * returns log records from the binary repository that are within the level range as specified by the parameters.
     * 
     * @param minLevel integer value of the minimum {@link Level} that the returned records need to match
     * @param maxLevel integer value of the maximum {@link Level} that the returned records need to match
     * @return the iterable instance of a list of log records within a process that are within the parameter range
     *         If no records meet the criteria, an Iterable is returned with no entries
     */
    public Iterable<ServerInstanceLogRecordList> getLogLists(int minLevel, int maxLevel) {
        return getLogLists((Date) null, (Date) null, new LevelFilter(minLevel, maxLevel));
    }

    /**
     * returns log records from the binary repository beyond the given repository pointer and within the level range as specified by the parameters.
     * Callers would have to invoke {@link RepositoryLogRecord#getRepositoryPointer()} to obtain the RepositoryPointer of the last record read.
     * 
     * @param after RepositoryPointer of the last read log record.
     * @param minLevel integer value of the minimum {@link Level} that the returned records need to match
     * @param maxLevel integer value of the maximum {@link Level} that the returned records need to match
     * @return the iterable instance of a list of log records within a process that are within the parameter range
     *         If no records meet the criteria, an Iterable is returned with no entries
     */
    public Iterable<ServerInstanceLogRecordList> getLogLists(RepositoryPointer after, int minLevel, int maxLevel) {
        return getLogLists(after, (Date) null, new LevelFilter(minLevel, maxLevel));
    }

    /**
     * returns log records from the binary repository beyond the given repository pointer and within the level range as specified by the parameters.
     * Callers would have to invoke {@link RepositoryLogRecord#getRepositoryPointer()} to obtain the RepositoryPointer of the last record read.
     * 
     * @param minLevel integer value of the minimum {@link Level} that the returned records need to match
     * @param maxLevel integer value of the maximum {@link Level} that the returned records need to match
     * @return the iterable instance of a list of log records within a process that are within the parameter range
     *         If no records meet the criteria, an Iterable is returned with no entries
     */
    @Override
    public Iterable<ServerInstanceLogRecordList> getLogLists(Level minLevel, Level maxLevel) {
        return getLogLists((Date) null, (Date) null, new LevelFilter(minLevel, maxLevel));
    }

    /**
     * returns log records from the binary repository beyond the given repository pointer and within the level range as specified by the parameters.
     * Callers would have to invoke {@link RepositoryLogRecord#getRepositoryPointer()} to obtain the RepositoryPointer of the last record read.
     * 
     * @param after RepositoryPointer of the last read log record.
     * @param minLevel Level value of the minimum {@link Level} that the returned records need to match
     * @param maxLevel Level value of the maximum {@link Level} that the returned records need to match
     * @return the iterable instance of a list of log records within a process that are within the parameter range
     *         If no records meet the criteria, an Iterable is returned with no entries
     */
    @Override
    public Iterable<ServerInstanceLogRecordList> getLogLists(RepositoryPointer after, Level minLevel, Level maxLevel) {
        return getLogLists(after, (Date) null, new LevelFilter(minLevel, maxLevel));
    }

    /**
     * returns log records from the binary repository that are between 2 dates (inclusive)
     * 
     * @param minTime the minimum {@link Date} value that the returned records can have
     * @param maxTime the maximum {@link Date} value that the returned records can have
     * @return the iterable instance of a list of log records within a process that are within the parameter range and satisfy the condition.
     *         If no records meet the criteria, an Iterable is returned with no entries
     */
    @Override
    public Iterable<ServerInstanceLogRecordList> getLogLists(Date minTime, Date maxTime) {
        return getLogLists(minTime, maxTime, (LogRecordFilter) null);
    }

    /**
     * returns log records from the binary repository that are after a given location and less than or equal to a given date
     * Callers would have to invoke {@link RepositoryLogRecord#getRepositoryPointer()} to obtain the RepositoryPointer of the last record read.
     * 
     * @param after RepositoryPointer of the last read log record.
     * @param maxTime the maximum {@link Date} value that the returned records can have
     * @return the iterable instance of a list of log records within a process that are within the parameter range and satisfy the condition.
     *         If no records meet the criteria, an Iterable is returned with no entries
     */
    @Override
    public Iterable<ServerInstanceLogRecordList> getLogLists(RepositoryPointer after, Date maxTime) {
        return getLogLists(after, maxTime, (LogRecordFilter) null);
    }

    @Override
    public Iterable<ServerInstanceLogRecordList> getLogLists(LogQueryBean query) {
        if (query == null) {
            query = new LogQueryBean();
        }
        MultipleCriteriaFilter multipleCriteriaFilter = new MultipleCriteriaFilter(query);
        return getLogLists(query.getMinTime(), query.getMaxTime(), multipleCriteriaFilter);
    }

    @Override
    public Iterable<ServerInstanceLogRecordList> getLogLists(
                                                             RepositoryPointer after, LogQueryBean query) {
        if (query == null) {
            query = new LogQueryBean();
        }
        MultipleCriteriaFilter multipleCriteriaFilter = new MultipleCriteriaFilter(query);
        return getLogLists(after, query.getMaxTime(), multipleCriteriaFilter);
    }

    @Override
    public ServerInstanceLogRecordList getLogListForCurrentServerInstance() {
        return getLogListForServerInstance((Date) null, (LogRecordFilter) null);
    }

    @Override
    public ServerInstanceLogRecordList getLogListForServerInstance(Date time) {
        return getLogListForServerInstance(time, (LogRecordFilter) null);
    }

    @Override
    public ServerInstanceLogRecordList getLogListForServerInstance(RepositoryPointer after) {
        return getLogListForServerInstance(after, (LogRecordFilter) null);
    }

    /**
     * returns log records from the binary repository which satisfy condition of the filter as specified by the parameter. The returned logs
     * will be from the same server instance. The server instance will be determined by the time as specified by the parameter.
     * 
     * @param time the {@link Date} time value used to determine the server instance where the server start time occurs before this value
     *            and the server stop time occurs after this value
     * @param filter an instance implementing {@link LogRecordHeaderFilter} interface to verify one record at a time.
     * @return the iterable list of log records
     */
    public ServerInstanceLogRecordList getLogListForServerInstance(Date time, final LogRecordHeaderFilter filter) {
        ServerInstanceByTime instance = new ServerInstanceByTime(time == null ? -1 : time.getTime());

        if (instance.logs == null && instance.traces == null) {
            return EMPTY_LIST;
        } else {
            return new ServerInstanceLogRecordListImpl(instance.logs, instance.traces, false) {
                @Override
                public OnePidRecordListImpl queryResult(LogRepositoryBrowser browser) {
                    return new LogRecordBrowser(browser).recordsInProcess(-1, -1, filter);
                }
            };
        }
    }

    /**
     * returns log records from the binary repository that are beyond a given repository location and satisfies the filter criteria as specified
     * by the parameters. Callers would have to invoke {@link RepositoryLogRecord#getRepositoryPointer()} to obtain the RepositoryPointer
     * of the last record read. The returned logs will be from the same server instance.
     * 
     * @param after RepositoryPointer of the last read log record.
     * @param filter an instance implementing {@link LogRecordHeaderFilter} interface to verify one record at a time.
     * @return the iterable list of log records
     */
    public ServerInstanceLogRecordList getLogListForServerInstance(RepositoryPointer after, final LogRecordHeaderFilter filter) {
        if (after instanceof RepositoryPointerImpl) {
            ServerInstanceByPointer instance = new ServerInstanceByPointer((RepositoryPointerImpl) after);

            if (instance.logs == null && instance.traces == null) {
                return EMPTY_LIST;
            }

            if (instance.record != null) {
                return new ServerInstanceLogRecordListHeaderPointerImpl(instance.logs, instance.traces, instance.switched, (RepositoryPointerImpl) after, instance.record, filter, -1);
            }

            // Neither trace nor log contain the location which means that file containing
            // it was purged already and that we can just return all the records we can find.
            return new ServerInstanceLogRecordListImpl(instance.logs, instance.traces, instance.switched) {
                @Override
                public OnePidRecordListImpl queryResult(
                                                        LogRepositoryBrowser browser) {
                    return new LogRecordBrowser(browser).recordsInProcess(-1, -1, filter);
                }
            };
        } else if (after != null) {
            throw new IllegalArgumentException("This method accept only RepositoryPointer instances retrieved from previously read records");
        }

        return getLogListForServerInstance((Date) null, filter);
    }

    /**
     * returns log records from the binary repository which satisfy condition of the filter as specified by the parameter. The returned logs
     * will be from the same server instance. The server instance will be determined by the time as specified by the parameter.
     * 
     * @param time the {@link Date} time value used to determine the server instance where the server start time occurs before this value
     *            and the server stop time occurs after this value
     * @param filter an instance implementing {@link LogRecordFilter} interface to verify one record at a time.
     * @return the iterable list of log records
     */
    public ServerInstanceLogRecordList getLogListForServerInstance(Date time, final LogRecordFilter filter) {
        ServerInstanceByTime instance = new ServerInstanceByTime(time == null ? -1 : time.getTime());

        if (instance.logs == null && instance.traces == null) {
            return EMPTY_LIST;
        } else {
            return new ServerInstanceLogRecordListImpl(instance.logs, instance.traces, false) {
                @Override
                public OnePidRecordListImpl queryResult(LogRepositoryBrowser browser) {
                    return new LogRecordBrowser(browser).recordsInProcess(-1, -1, filter);
                }
            };
        }
    }

    /**
     * returns log records from the binary repository that are beyond a given repository location and satisfies the filter criteria as specified
     * by the parameters. Callers would have to invoke {@link RepositoryLogRecord#getRepositoryPointer()} to obtain the RepositoryPointer
     * of the last record read. The returned logs will be from the same server instance.
     * 
     * @param after RepositoryPointer of the last read log record.
     * @param filter an instance implementing {@link LogRecordFilter} interface to verify one record at a time.
     * @return the iterable list of log records
     */
    public ServerInstanceLogRecordList getLogListForServerInstance(RepositoryPointer after, final LogRecordFilter filter) {
        if (after instanceof RepositoryPointerImpl) {
            ServerInstanceByPointer instance = new ServerInstanceByPointer((RepositoryPointerImpl) after);

            if (instance.logs == null && instance.traces == null) {
                return EMPTY_LIST;
            }

            if (instance.record != null) {
                return new ServerInstanceLogRecordListPointerImpl(instance.logs, instance.traces, instance.switched, (RepositoryPointerImpl) after, instance.record, filter, -1);
            }

            // Neither trace nor log contain the location which means that file containing
            // it was purged already and that we can just return all the records we can find.
            return new ServerInstanceLogRecordListImpl(instance.logs, instance.traces, instance.switched) {
                @Override
                public OnePidRecordListImpl queryResult(
                                                        LogRepositoryBrowser browser) {
                    return new LogRecordBrowser(browser).recordsInProcess(-1, -1, filter);
                }
            };
        } else if (after != null) {
            throw new IllegalArgumentException("This method accept only RepositoryPointer instances retrieved from previously read records");
        }

        return getLogListForServerInstance((Date) null, filter);
    }

    @Override
    public ServerInstanceLogRecordList getLogListForServerInstance(Date time, Level minLevel, Level maxLevel) {
        return getLogListForServerInstance(time, new LevelFilter(minLevel, maxLevel));
    }

    @Override
    public ServerInstanceLogRecordList getLogListForServerInstance(RepositoryPointer after, Level minLevel, Level maxLevel) {
        return getLogListForServerInstance(after, new LevelFilter(minLevel, maxLevel));
    }

    @Override
    public ServerInstanceLogRecordList getLogListForServerInstance(Date time, final int threadID) {
        return getLogListForServerInstance(time, threadID < 0 ? (LogRecordHeaderFilter) null : new LogRecordHeaderFilter() {
            @Override
            public boolean accept(RepositoryLogRecordHeader record) {
                return record.getThreadID() == threadID;
            }
        });
    }

    @Override
    public ServerInstanceLogRecordList getLogListForServerInstance(RepositoryPointer after, final int threadID) {
        return getLogListForServerInstance(after, threadID < 0 ? (LogRecordHeaderFilter) null : new LogRecordHeaderFilter() {
            @Override
            public boolean accept(RepositoryLogRecordHeader record) {
                return record.getThreadID() == threadID;
            }
        });
    }

    @Override
    public ServerInstanceLogRecordList getLogListForServerInstance(Date time,
                                                                   LogQueryBean query) {
        ServerInstanceByTime instance = new ServerInstanceByTime(time == null ? -1 : time.getTime());

        if (instance.logs == null && instance.traces == null) {
            return EMPTY_LIST;
        } else {
            if (query == null) {
                query = new LogQueryBean();
            }
            final MultipleCriteriaFilter filter = new MultipleCriteriaFilter(query);
            final long min = query.getMinTime() == null ? -1 : query.getMinTime().getTime();
            final long max = query.getMaxTime() == null ? -1 : query.getMaxTime().getTime();
            return new ServerInstanceLogRecordListImpl(instance.logs, instance.traces, false) {
                @Override
                public OnePidRecordListImpl queryResult(LogRepositoryBrowser browser) {
                    return new LogRecordBrowser(browser).recordsInProcess(min, max, filter);
                }
            };
        }
    }

    @Override
    public ServerInstanceLogRecordList getLogListForServerInstance(
                                                                   RepositoryPointer after, LogQueryBean query) {
        if (after instanceof RepositoryPointerImpl) {
            ServerInstanceByPointer instance = new ServerInstanceByPointer((RepositoryPointerImpl) after);

            if (instance.logs == null && instance.traces == null) {
                return EMPTY_LIST;
            }

            if (query == null) {
                query = new LogQueryBean();
            }
            final MultipleCriteriaFilter filter = new MultipleCriteriaFilter(query);

            if (instance.record != null) {
                return new ServerInstanceLogRecordListPointerImpl(instance.logs, instance.traces, instance.switched, (RepositoryPointerImpl) after, instance.record, filter, -1);
            }

            // Neither trace nor log contain the location which means that file containing
            // it was purged already and that we can just return all the records we can find.
            final long min = query.getMinTime() == null ? -1 : query.getMinTime().getTime();
            final long max = query.getMaxTime() == null ? -1 : query.getMaxTime().getTime();
            return new ServerInstanceLogRecordListImpl(instance.logs, instance.traces, instance.switched) {
                @Override
                public OnePidRecordListImpl queryResult(
                                                        LogRepositoryBrowser browser) {
                    return new LogRecordBrowser(browser).recordsInProcess(min, max, filter);
                }
            };
        } else if (after != null) {
            throw new IllegalArgumentException("This method accept only RepositoryPointer instances retrieved from previously read records");
        }

        return getLogListForServerInstance((Date) null, query);
    }

    /*
     * Bellow are the private helper methods and classes used by the public methods above to merge log and trace repositories into one log record stream.
     */
    private final static Properties EMPTY_HEADER = new Properties();

    private final static ServerInstanceLogRecordList EMPTY_LIST = new ServerInstanceLogRecordList() {
        @Override
        public Iterator<RepositoryLogRecord> iterator() {
            return ServerInstanceLogRecordListImpl.EMPTY_ITERATOR;
        }

        @Override
        public Iterable<RepositoryLogRecord> range(int offset, int length) {
            return new Iterable<RepositoryLogRecord>() {
                @Override
                public Iterator<RepositoryLogRecord> iterator() {
                    return ServerInstanceLogRecordListImpl.EMPTY_ITERATOR;
                }
            };
        }

        @Override
        public Properties getHeader() {
            return EMPTY_HEADER;
        }

        @Override
        public Map<String, ServerInstanceLogRecordList> getChildren() {
            return new HashMap<String, ServerInstanceLogRecordList>();
        }

        @Override
        public Date getStartTime() {
            return null;
        }
    };

    private final static Iterable<ServerInstanceLogRecordList> EMPTY_ITERABLE = new Iterable<ServerInstanceLogRecordList>() {
        @Override
        public Iterator<ServerInstanceLogRecordList> iterator() {
            return new Iterator<ServerInstanceLogRecordList>() {
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public ServerInstanceLogRecordList next() {
                    return null;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("Method is not applicable to this class");
                }
            };
        }
    };

    /**
     * Helper class to find instance log and trace directories based on the time it was active.
     */
    private class ServerInstanceByTime {
        final LogRepositoryBrowser logs;
        final LogRepositoryBrowser traces;

        ServerInstanceByTime(long timestamp) {
            LogRepositoryBrowser iLogs = logInstanceBrowser.findByMillis(timestamp);
            LogRepositoryBrowser iTraces = null;
            if (traceInstanceBrowser != null) {
                iTraces = traceInstanceBrowser.findByMillis(timestamp);
                if (iLogs == null && iTraces != null) {
                    iLogs = logInstanceBrowser.findNext(iLogs, -1);
                } else if (iTraces == null && iLogs != null) {
                    iTraces = traceInstanceBrowser.findNext(iLogs, -1);
                }
            }
            if (iLogs != null && iTraces != null) {
                String logPid = iLogs.getProcessId();
                String tracePid = iTraces.getProcessId();
                if ((logPid == null && tracePid != null) ||
                    (logPid != null && !logPid.equals(tracePid))) {
                    // PID of the result from log and trace are different. The searched one is the youngest (closest to the specified time)
                    long logTimestamp = iLogs.getTimestamp();
                    long traceTimestamp = iTraces.getTimestamp();
                    if (logTimestamp < traceTimestamp) {
                        if (timestamp < 0) {
                            // Request came for the latest records, ignore logs
                            iLogs = null;
                        } else if (timestamp < traceTimestamp) {
                            // Trace records were generated after the referenced timestamp, ignore them.
                            iTraces = null;
                        } else {
                            // Trace result is for the right process, see if log was generated by this process later than specified time.
                            iLogs = logInstanceBrowser.findNext(iLogs, -1);
                            if (iLogs != null) {
                                logPid = iLogs.getProcessId();
                                if ((logPid == null && tracePid != null) ||
                                    (logPid != null && !logPid.equals(tracePid))) {
                                    // next result for the log is still from different process which means the required process didn't have log result at all.
                                    iLogs = null;
                                }
                            }
                        }
                    } else {
                        if (timestamp < 0) {
                            // Request came for the latest records, ignore traces
                            iTraces = null;
                        } else if (timestamp < logTimestamp) {
                            // Log records were generated after the referenced timestamp, ignore them.
                            iLogs = null;
                        } else {
                            // Log result is for the right process, see if trace was generated by this process later than specified time.
                            iTraces = traceInstanceBrowser.findNext(iLogs, -1);
                            if (iTraces != null) {
                                tracePid = iTraces.getProcessId();
                                if ((logPid == null && tracePid != null) ||
                                    (logPid != null && !logPid.equals(tracePid))) {
                                    // next result for the trace is still from different process which means the required process didn't have trace result at all.
                                    iTraces = null;
                                }
                            }
                        }
                    }
                }
            }

            logs = iLogs;
            traces = iTraces;
        }
    }

    /**
     * Helper class to find instance based on the RepositoryPointer of a previously read record
     */
    private class ServerInstanceByPointer {
        final LogRepositoryBrowser logs;
        final LogRepositoryBrowser traces;
        final RepositoryLogRecord record;
        boolean switched = false;

        ServerInstanceByPointer(RepositoryPointerImpl location) {
            LogRepositoryBrowser iLogs = logInstanceBrowser.find(location, false);
            LogRepositoryBrowser iTraces = null;
            RepositoryLogRecord iRecord = null;
            if (traceInstanceBrowser != null) {
                iTraces = traceInstanceBrowser.find(location, false);
            }
            if (iLogs != null || iTraces != null) {

                // Check if location came from the log
                if (iLogs != null) {
                    iRecord = new LogRecordBrowser(iLogs).getRecord(location);
                    if (iRecord != null) {
                        iTraces = traceInstanceBrowser == null ? null : traceInstanceBrowser.find(location, true);
                    }
                }

                // If not from the log check that location came from the trace.
                if (iRecord == null && iTraces != null) {
                    iRecord = new LogRecordBrowser(iTraces).getRecord(location);
                    if (iRecord != null) {
                        // Switching log and trace as if record still came from the log
                        iLogs = iTraces;
                        iTraces = logInstanceBrowser.find(location, true);
                        switched = true;
                    }
                }

            }

            logs = iLogs;
            traces = iTraces;
            record = iRecord;
        }
    }

    /**
     * ServerInstanceLogRecordList implementation to be used for pointer related queries with {@link LogRecordHeaderFilter} parameters
     */
    private static class ServerInstanceLogRecordListHeaderPointerImpl extends ServerInstanceLogRecordListImpl {
        private final LogRecordHeaderFilter filter;
        private final long max;
        private final RepositoryPointerImpl location;
        private final RepositoryLogRecord current;

        /**
         * Construct entry using correct relative information for browsers.
         * 
         * @param pointerBrowser file browser the pointer came from.
         * @param recordBrowser file browser which need to use RepositoryLogRecord as a relative point.
         * @param location the pointer used in the query.
         * @param current the record pointer points to.
         * @param filter filter used in the query.
         * @param max top time limit used in the query.
         */
        public ServerInstanceLogRecordListHeaderPointerImpl(LogRepositoryBrowser pointerBrowser, LogRepositoryBrowser recordBrowser, boolean switched,
                                                            RepositoryPointerImpl location, RepositoryLogRecord current, LogRecordHeaderFilter filter, long max) {
            super(pointerBrowser, recordBrowser, switched);
            this.location = location;
            this.current = current;
            this.filter = filter;
            this.max = max;
        }

        @Override
        public OnePidRecordListImpl queryResult(LogRepositoryBrowser browser) {
            return new LogRecordBrowser(browser).recordsInProcess(-1, max, filter);
        }

        @Override
        protected OnePidRecordListImpl getLogResult() {
            if (logResult == null && logBrowser != null) {
                logResult = new LogRecordBrowser(logBrowser).recordsInProcess(location, max, filter);
            }
            return logResult;
        }

        @Override
        protected OnePidRecordListImpl getTraceResult() {
            if (traceResult == null && traceBrowser != null) {
                traceResult = new LogRecordBrowser(traceBrowser).recordsInProcess(current, max, filter);
            }
            return traceResult;
        }

    }

    /**
     * ServerInstanceLogRecordList implementation to be used for pointer related queries with {@link LogRecordFilter} parameters
     */
    private static class ServerInstanceLogRecordListPointerImpl extends ServerInstanceLogRecordListImpl {
        private final LogRecordFilter filter;
        private final long max;
        private final RepositoryPointerImpl location;
        private final RepositoryLogRecord current;

        /**
         * Construct entry using correct relative information for browsers.
         * 
         * @param pointerBrowser file browser the pointer came from.
         * @param recordBrowser file browser which need to use RepositoryLogRecord as a relative point.
         * @param location the pointer used in the query.
         * @param current the record pointer points to.
         * @param filter filter used in the query.
         * @param max top time limit used in the query.
         */
        public ServerInstanceLogRecordListPointerImpl(LogRepositoryBrowser pointerBrowser, LogRepositoryBrowser recordBrowser, boolean switched, RepositoryPointerImpl location,
                                                      RepositoryLogRecord current, LogRecordFilter filter, long max) {
            super(pointerBrowser, recordBrowser, switched);
            this.location = location;
            this.current = current;
            this.filter = filter;
            this.max = max;
        }

        @Override
        public OnePidRecordListImpl queryResult(LogRepositoryBrowser browser) {
            return new LogRecordBrowser(browser).recordsInProcess(-1, max, filter);
        }

        @Override
        protected OnePidRecordListImpl getLogResult() {
            if (logResult == null && logBrowser != null) {
                logResult = new LogRecordBrowser(logBrowser).recordsInProcess(location, max, filter);
            }
            return logResult;
        }

        @Override
        protected OnePidRecordListImpl getTraceResult() {
            if (traceResult == null && traceBrowser != null) {
                traceResult = new LogRecordBrowser(traceBrowser).recordsInProcess(current, max, filter);
            }
            return traceResult;
        }

    }

    /**
     * Implementation of a {@link ServerInstanceLogRecordList} iterator combining log and trace
     * instances. It assumes that instances are sequential (all records of one instance have timestamps
     * either earlier or later than all instances of another). It is used only for the root/parent instances.
     */
    private abstract class ServerInstanceListsIterator implements Iterator<ServerInstanceLogRecordList> {
        boolean readFirstInstance = false;
        LogRepositoryBrowser nextLog;
        LogRepositoryBrowser nextTrace;
        LogRepositoryBrowser nextToNextLog;
        private final long max;

        ServerInstanceListsIterator(long max, LogRepositoryBrowser firstLog, LogRepositoryBrowser firstTrace) {
            this.max = max;
            this.nextLog = firstLog;
            this.nextTrace = firstTrace;
            nextToNextLog = logInstanceBrowser.findNext(nextLog, max);
        }

        /**
         * retrieves result from the repository using user specified query. It is used for all
         * processes in the list.
         * 
         * @param browser retriever of the files in the repository.
         * @return instance containing result of the user query. It is used to construct ServerInstanceLogRecordList
         *         instances return by the iterator.
         */
        protected abstract OnePidRecordListImpl queryResult(LogRepositoryBrowser browser);

        /**
         * retrieves result from the first repository in the list using user specified query. By
         * default it uses {@link #queryResult(LogRepositoryBrowser)}.
         * 
         * @param firstLog log part of the first repository in the list.
         * @param firstTrace trace part of the first repository in the list.
         * @return instance which is return as the first entry by the iterator.
         */
        protected ServerInstanceLogRecordList queryFirstInstance(LogRepositoryBrowser firstLog, LogRepositoryBrowser firstTrace) {
            return new ServerInstanceLogRecordListImpl(firstLog, firstTrace, false) {
                @Override
                public OnePidRecordListImpl queryResult(
                                                        LogRepositoryBrowser browser) {
                    return ServerInstanceListsIterator.this.queryResult(browser);
                }
            };
        }

        @Override
        public boolean hasNext() {
            return nextLog != null || nextTrace != null;
        }

        @Override
        public ServerInstanceLogRecordList next() {
            if (nextLog == null && nextTrace == null) {
                return null;
            }
            LogRepositoryBrowser useLog = nextLog;
            LogRepositoryBrowser useTrace = nextTrace;
            LogRepositoryBrowser tmpNextToNextLog = nextToNextLog;
            if (nextLog != null && nextTrace != null) {
                if ((nextLog.getProcessId() == null && nextTrace.getProcessId() != null) ||
                    (nextLog.getProcessId() != null)) {
                    if (nextLog.getTimestamp() < nextTrace.getTimestamp()) {
                        if (tmpNextToNextLog != null) {
                            if (nextTrace.getTimestamp() > tmpNextToNextLog.getTimestamp()) {
                                useTrace = null;
                            }
                        }
                    } else {
                        useLog = null;
                    }
                }
            }
            if (useLog != null) {
                nextLog = logInstanceBrowser.findNext(nextLog, max);
                nextToNextLog = logInstanceBrowser.findNext(nextLog, max);
            }
            if (useTrace != null) {
                nextTrace = traceInstanceBrowser.findNext(nextTrace, max);
            }
            if (readFirstInstance) {
                return new ServerInstanceLogRecordListImpl(useLog, useTrace, false) {
                    @Override
                    public OnePidRecordListImpl queryResult(
                                                            LogRepositoryBrowser browser) {
                        return ServerInstanceListsIterator.this.queryResult(browser);
                    }
                };
            } else {
                readFirstInstance = true;
                return queryFirstInstance(useLog, useTrace);
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Method is not applicable to this class");
        }

    }

}
