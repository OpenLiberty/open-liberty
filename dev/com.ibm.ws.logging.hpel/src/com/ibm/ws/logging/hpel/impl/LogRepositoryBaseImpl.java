/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.hpel.impl;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.ibm.ejs.ras.hpel.HpelHelper;
import com.ibm.websphere.logging.hpel.writer.LogEventNotifier;
import com.ibm.ws.logging.hpel.LogRecordSerializer;
import com.ibm.ws.logging.hpel.LogRepositoryBase;

/**
 * Base class providing methods to work with files in the log repository.
 * It maintains naming convention among files in the repository and
 * provides method to retrieve timestamp and pid from them.
 */
public abstract class LogRepositoryBaseImpl implements LogRepositoryBase {
    /** Formatters used for logs in the repository. First in the list is the latest */
    public final static LogRecordSerializer[] KNOWN_FORMATTERS = new LogRecordSerializer[] {
                                                                                            new BinaryLogRecordSerializerVersion2Impl(),
                                                                                            new BinaryLogRecordSerializerImpl()
    };
    /** Default repository location when its 'type' is not specified */
    public final static String DEFAULT_LOCATION = "logdata";
    /** Trace repository location */
    public final static String TRACE_LOCATION = "tracedata";

    /** Location of the repository this instance is configured with */
    protected final File repositoryLocation;

    /** Type being managed (log, trace, or textlog) */
    protected String managedType;
    /** Listener being used by the process to notify any registered of agents when log events such as roll and delete occur */
    protected LogEventNotifier logEventNotifier;
    /** Lock name extension to use for parent directories */
    private final static String LOCK_EXT = ".lock";
    /** File name extension to use for files in the repository */
    public final static String EXTENSION = ".wbl";
    /** types that are managed (F017049-22453) */
    public final static String TRACETYPE = "trace";
    public final static String LOGTYPE = "log";
    public final static String TEXTLOGTYPE = "textlog";
    /** File filter to list files belonging to the repository */
    protected final FileFilter filter = new LogRepositoryFilesFilter();
    private static String thisClass = LogRepositoryBaseImpl.class.getName();
    // Unique logger as it does usepParentHandlers=false to avoid getting back into logging (and thus
    // risking recursion and stack overflow). No RBundle as only used for trace and write to sep file
    // To get this trace output, must include com.ibm.ws.logging.hpel.impl.*=fine in traceSpec
    private static final int ONE_MEG = 1024 * 1024;
    private static Logger debugLogger = Logger.getLogger("com.ibm.hpel.debug"); // 682032 for debugAllowed
    private static final boolean debugAllowed = "true".equalsIgnoreCase(getSystemProperty("com.ibm.ws.logging.hpel.debug"));
    private static final int debugReposSz = Integer.getInteger("com.ibm.ws.logging.hpel.internaltracesize", 20) * ONE_MEG;
    private static final String debugLocation = getSystemProperty("com.ibm.ws.logging.hpel.internaltracelocation");
    private static Boolean debugEnabled = null;

    protected final static FileFilter LOCKFILE_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return AccessHelper.isFile(pathname) && pathname.getName().endsWith(LOCK_EXT) && parseTimeStamp(pathname.getName()) >= 0;
        }
    };

    protected final FileFilter instanceFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return isDirectory(pathname) && parseTimeStamp(pathname.getName()) > 0;
        }
    };

    protected final FileFilter subprocFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return isDirectory(pathname) && parseTimeStamp(pathname.getName()) < 0;
        }
    };

    protected final FileFilter dirFilter = new LogRepositoryDirFilter();
//	private final FileFilter allFilter = new AcceptAllFilesFilter() ;
    /** File comparator to sort files in ascending order (oldest to youngest) based on their timestamps */
    protected final Comparator<File> fileComparator = new LogRepositoryFilesComparator();

    /**
     * creates LogRepositoryBase instance. This one is generally for reading, as writing will also pass in process-specific info
     * 
     * @param repositoryLocation the location of repository log files.
     */
    protected LogRepositoryBaseImpl(File repositoryLocation) {
        if (repositoryLocation == null) {
            throw new IllegalArgumentException("Specified repository location can't be null.");
        }
        this.repositoryLocation = repositoryLocation;
    }

    /**
     * @param propertyName
     * @return
     */
    private static String getSystemProperty(final String propertyName) {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(propertyName);
            }
        });
    }

    public File getLocation() {
        return repositoryLocation;
    }

    // Separator to use between timestamp and PID in repository directory names.
    public final static char TIMESEPARATOR = '_';
    // Separator to use between PID and a label in repository directory names
    public final static char LABELSEPARATOR = '-';

    /**
     * Creates lock file to be used as a pattern for instance repository.
     * Should be called only by parent's manager on start up.
     * 
     * @param pid process ID of the parent.
     * @param label label of the parent.
     * @throws IOException if there's a problem to create lock file.
     */
    protected void createLockFile(String pid, String label) throws IOException {
        if (!AccessHelper.isDirectory(repositoryLocation)) {
            AccessHelper.makeDirectories(repositoryLocation);
        }

        // Remove stale lock files if any.
        for (File lock : listFiles(LOCKFILE_FILTER)) {
            AccessHelper.deleteFile(lock);
        }

        // Create a new lock file.
        StringBuilder sb = new StringBuilder();
        sb.append(getLogDirectoryName(System.currentTimeMillis(), pid, label)).append(LOCK_EXT);
        AccessHelper.createFileOutputStream(new File(repositoryLocation, sb.toString()), false).close();
    }

    /**
     * @param lockfileFilter
     * @return
     */
    private File[] listFiles(final FileFilter lockfileFilter) {
        return AccessController.doPrivileged(new PrivilegedAction<File[]>() {
            @Override
            public File[] run() {
                return repositoryLocation.listFiles(lockfileFilter);
            }
        });
    }

    /**
     * creates instance directory to use for log files. Both parent and children
     * need to use it when figuring out the log repository location.
     * 
     * @param timestamp time on the first record to be stored.
     * @param pid process ID of the parent
     * @return parent's directory (kids need to create directories under it) or
     *         <code>null</code> if directory can't be created or found due to race
     *         condition. In the later case user need to wait and try again.
     */
    protected File makeLogDirectory(long timestamp, final String pid, boolean flag) {
        // Find the most recent instance directory.
        File instanceDir = null;
        long maxTimestamp = -1;
        for (File dir : AccessHelper.listFiles(repositoryLocation, new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return AccessHelper.isDirectory(pathname);
            }
        })) {
            long dirTimestamp = parseTimeStamp(dir.getName());
            if (maxTimestamp < dirTimestamp) {
                instanceDir = dir;
                maxTimestamp = dirTimestamp;
            }
        }

        // If the most recent instance directory was generated by the required pid, use it.
        if (maxTimestamp >= 0 && pid.equals(parseProcessID(instanceDir.getName()))) {
            return instanceDir;
        }
        // PM99024 , Code fix when .lock file removed
        if (flag == true)
        {
            if (debugLogger.isLoggable(Level.FINE) && isDebugEnabled())
            {
                debugLogger.logp(Level.FINE, thisClass, "makingLogDirectory", "no lock files found , creating folder with XXXXXX label");
            }
            instanceDir = new File(repositoryLocation, getLogDirectoryName(timestamp, pid, "XXXXXXX"));
            AccessHelper.makeDirectories(instanceDir);

            return instanceDir;
        }

        // Find lock files
        File[] lockFiles = AccessHelper.listFiles(repositoryLocation, LOCKFILE_FILTER);

        // If there's more than one lock file or a file generated by the wrong process then parent manager didn't
        //    clean up old locks yet, need to wait.
        // If there's no lock files then either parent manager didn't create one yet or somebody already
        //    deleted it to create instance directory, need to wait and try again.
        if (lockFiles.length != 1 || !pid.equals(parseProcessID(lockFiles[0].getName()))) {
            // Use System.err to report that condition since logger is not ready yet.
            if (lockFiles.length < 1) {
                if (debugLogger.isLoggable(Level.FINE) && isDebugEnabled())
                    debugLogger.logp(Level.FINE, thisClass, "makingLogDirectory", "no lock files found.");
            } else if (lockFiles.length > 1) {
                StringBuilder sb = new StringBuilder();
                for (File lock : lockFiles) {
                    sb.append(lock.getName()).append(" ");
                }
                if (debugLogger.isLoggable(Level.FINE) && isDebugEnabled())
                    debugLogger.logp(Level.FINE, thisClass, "makeLogDirectory", "too many lock files found: " + sb.toString());
            } else {
                if (debugLogger.isLoggable(Level.FINE) && isDebugEnabled())
                    debugLogger.logp(Level.FINE, thisClass, "makeLogDirectory", "found stale lock file " + lockFiles[0].getName() + " but was expecting one generated by process "
                                                                                + pid);
            }
            return null;
        }

        String lockname = lockFiles[0].getName();
        // If somebody else deleted lock file right under our nose, need to wait until that somebody
        //    creates the right directory.
        if (!AccessHelper.deleteFile(lockFiles[0])) {
            if (debugLogger.isLoggable(Level.FINE) && isDebugEnabled())
                debugLogger.logp(Level.FINE, thisClass, "makeLogDirectory", "failed to delete found lock file " + lockFiles[0].getName() + ". Assume it was deleted already");
            return null;
        }

        // The lock file was deleted - call cannot return 'null' anymore without causing hang - need to create
        //     and return the new instance directory.
        String label = parseLabel(lockname.substring(0, lockname.length() - LOCK_EXT.length()));
        instanceDir = new File(repositoryLocation, getLogDirectoryName(timestamp, pid, label));
        AccessHelper.makeDirectories(instanceDir);

        return instanceDir;
    }

    //Method for forcefully creating Instance Directory , pass flag as true
    protected File makeLogDirectory(long timestamp, final String pid)
    {

        return makeLogDirectory(timestamp, pid, false);
    }

    /**
     * calculates repository file name.
     * 
     * @param timestamp the time in 'millis' of the first record in the file.
     * @return the file according to repository pattern
     */
    protected File getLogFile(File parentLocation, long timestamp) {
        if (timestamp < 0) {
            throw new IllegalArgumentException("timestamp cannot be negative");
        }
        StringBuilder sb = new StringBuilder();
        sb.append(timestamp).append(EXTENSION);
        return new File(parentLocation, sb.toString());
    }

    /**
     * Retrieves the timestamp from the name of the file.
     * 
     * @param file to retrieve timestamp from.
     * @return timestamp in millis or -1 if name's pattern does not correspond
     *         to the one used for files in the repository.
     */
    public long getLogFileTimestamp(File file) {
        if (file == null) {
            return -1L;
        }

        String name = file.getName();

        // Check name for extension
        if (name == null || name.length() == 0 || !name.endsWith(EXTENSION)) {
            return -1L;
        }

        try {
            return Long.parseLong(name.substring(0, name.indexOf(EXTENSION)));
        } catch (NumberFormatException ex) {
            return -1L;
        }
    }

    /**
     * returns sub-directory name to be used for instances and sub-processes
     * 
     * @param timestamp time of the instance start. It should be negative for sub-processes
     * @param pid process Id of the instance or sub-process. It cannot be null or empty.
     * @param label additional identification to use on the sub-directory.
     * @return string representing requested sub-directory.
     */
    public String getLogDirectoryName(long timestamp, String pid, String label) {
        if (pid == null || pid.isEmpty()) {
            throw new IllegalArgumentException("pid cannot be empty");
        }

        StringBuilder sb = new StringBuilder();
        if (timestamp > 0) {
            sb.append(timestamp).append(TIMESEPARATOR);
        }

        sb.append(pid);

        if (label != null && !label.trim().isEmpty()) {
            sb.append(LABELSEPARATOR).append(label);
        }

        return sb.toString();
    }

    /**
     * Returns the list of files in the repository.
     * 
     * @return array of File instances representing files in the repository.
     */
    protected File[] listRepositoryFiles() {
        //return AccessHelper.listFiles(repositoryLocation, filter);
        File[] directories = listRepositoryDirs();
        ArrayList<File> allFilesArray = new ArrayList<File>();
        File[] allFiles = new File[allFilesArray.size()];

        //get all the files in each server instance directory
        for (int i = 0; i < directories.length; i++) {

            //for each server instance directory, list the files, filtering out subdirectories
            File[] files = AccessHelper.listFiles(directories[i], filter);
            if (files != null && files.length > 0) {
                allFilesArray.addAll(Arrays.asList(files));
            }
            // F017049-22453 ... now look for files in child processes
            files = AccessHelper.listFiles(directories[i], subprocFilter);
            for (File curFile : files) {
                File[] subFiles = AccessHelper.listFiles(curFile, filter);
                if (subFiles != null && subFiles.length > 0) {
                    allFilesArray.addAll(Arrays.asList(subFiles));
                }
            }
        }

        //if there are files to sort, then sort
        if (!allFilesArray.isEmpty()) {
            allFiles = allFilesArray.toArray(allFiles);
        }

        return allFiles;
    }

    /**
     * Implementation of the FileFilter used to return only the files
     * belonging to the current instance of the repository. It simply
     * checks that file's name can be used to retrieve a timestamp.
     */
    private class LogRepositoryFilesFilter implements FileFilter {
        @Override
        public boolean accept(File fileObject) {
            // if the file exists and it is a file
            if (fileExists(fileObject) && isFile(fileObject)) {
                return getLogFileTimestamp(fileObject) >= 0;
            }
            return false;
        }

    }

    /**
     * This enables using same listFiles functions but accepting all
     */
/*
 * private class AcceptAllFilesFilter implements FileFilter {
 * public boolean accept(File fileObject) {
 * return true ; // Accepts all files
 * }
 * }
 */

    private class LogRepositoryFilesComparator implements Comparator<File> {
        @Override
        public int compare(File f1, File f2) {
            long t1 = getLogFileTimestamp(f1);
            long t2 = getLogFileTimestamp(f2);
            // we want the old files first
            return t1 < t2 ? -1 : (t1 > t2 ? 1 : 0);
        }
    }

    /**
     * Implementation of the FileFilter used to return only the server instances ( represented as a directory)
     * belonging to the current instance of the repository. It simply checks that file's name can
     * be used to retrieve a timestamp, as server instance directories are expected to have a timestamp in the name.
     */
    protected class LogRepositoryDirFilter implements FileFilter {
        @Override
        public boolean accept(File fileObject) {
            // if the file exists and it is a directory
            if (fileExists(fileObject) && isDirectory(fileObject)) {
                return parseTimeStamp(fileObject.getName()) >= 0;
            }
            return false;
        }

    }

    /**
     * Retrieves the timestamp out of the directory name.
     * 
     * example: directory file name for a serverinstance would be <repos_timestamp>_<pid>-<label>
     * 
     * returned would be <repos_timestamp>
     */
    public static long parseTimeStamp(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return -1L;
        }
        int pidIndex = fileName.indexOf(TIMESEPARATOR);
        int labelIndex = fileName.indexOf(LABELSEPARATOR);
        // If no time separator or it's a part of the label there's no timestamp
        if (pidIndex < 0 || (labelIndex > 0 && labelIndex < pidIndex)) {
            return -1L;
        }
        try {
            return Long.parseLong(fileName.substring(0, pidIndex));
        } catch (NumberFormatException ex) {
            return -1L;
        }
    }

    /**
     * @param fileObject
     * @return
     */
    private boolean isDirectory(final File fileObject) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return fileObject.isDirectory();
            }
        });
    }

    /**
     * @param fileObject
     * @return
     */
    private boolean isFile(final File fileObject) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return fileObject.isFile();
            }
        });
    }

    /**
     * @param fileObject
     * @return
     */
    public boolean fileExists(final File fileObject) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return fileObject.exists();
            }
        });
    }

    /**
     * Retrieves the PID out of the directory name. (Qualifier 2)
     * 
     * @param fileName
     * @return
     */
    public static String parseProcessID(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        int pidIndex = fileName.indexOf(TIMESEPARATOR);
        int labelIndex = fileName.indexOf(LABELSEPARATOR);

        // If neither time separator or label separator present PID is the whole name
        if (pidIndex < 0 && labelIndex < 0) {
            return fileName;
        }

        // If no label separator then there's a time separator
        if (labelIndex < 0) {
            return fileName.substring(pidIndex + 1);
        }

        // If no time separator or it's a part of the label use label separator
        if (pidIndex < 0 || labelIndex < pidIndex) {
            return fileName.substring(0, labelIndex);
        }

        return fileName.substring(pidIndex + 1, labelIndex);
    }

    /**
     * Retrieves the label out of the directory name (Qualifier 3)
     * 
     * @param fileName
     * @return
     */
    public static String parseLabel(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        int labelIndex = fileName.indexOf(LABELSEPARATOR);
        return labelIndex < 0 ? null : fileName.substring(labelIndex + 1);
    }

    /**
     * Retrieves the PID and label combined out of the directory name.
     * 
     * @param fileName
     * @return
     */
    public static String parsePIDandLabel(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        int pidIndex = fileName.indexOf(TIMESEPARATOR);
        int labelIndex = fileName.indexOf(LABELSEPARATOR);

        // If no time separator or it's a part of the label the full name is the result
        if (pidIndex < 0 || (labelIndex > 0 && labelIndex < pidIndex)) {
            return fileName;
        }

        return fileName.substring(pidIndex + 1);
    }

    /**
     * Returns the list of server instance directories in the repository.
     * 
     * @return array of File instances representing files in the repository.
     */
    protected File[] listRepositoryDirs() {
        return AccessHelper.listFiles(repositoryLocation, dirFilter);
    }

    public void setManagedType(String managedType) {
        this.managedType = managedType;
    }

    public String getManagedType() {
        return managedType;
    }

    public void setLogEventNotifier(LogEventNotifier logEventNotifier) {
        if (debugLogger.isLoggable(Level.FINE) && isDebugEnabled())
            debugLogger.logp(Level.FINE, thisClass, "setLogEventNotifier", "LEN: " + logEventNotifier);
        this.logEventNotifier = logEventNotifier;
    }

    public LogEventNotifier getLogEventNotifier() {
        if (debugLogger.isLoggable(Level.FINE) && isDebugEnabled())
            debugLogger.logp(Level.FINE, thisClass, "getLogEventNotifier", "getLEN: " + logEventNotifier);
        return logEventNotifier;
    }

    /**
     * determine if the logger is ready. This logger does not get set up since it has a separate
     * file handler (avoiding logging thru the normal channels as that would cause recursion). So
     * this check is done regularly as it can start dynamically
     * 
     * @return determination as to whether or not file was created and file handler is in place
     */
    public static boolean isDebugEnabled() {
        if (debugEnabled != null)
            return debugEnabled;
        debugEnabled = false;
        if (!debugAllowed)
            return false; // 682032
        int numDebugFiles; // Calculate # of files and file size based on repository size
        int debugFileSz;
        if (debugReposSz < (4 * ONE_MEG)) { // Small repository = 2 files, otherwise 10
            numDebugFiles = 2;
            debugFileSz = debugReposSz / 2;
        } else {
            numDebugFiles = 10;
            debugFileSz = debugReposSz / 10;
        }

        System.out.println("HPEL Debugging in use. Internal repository sz: " + debugReposSz + ". Num Files: " +
                           numDebugFiles + " FileSz: " + debugFileSz + " Location (null means system temp directory): " + debugLocation);
        FileHandler fHandler;
        String pid = HpelHelper.getProcessId();

        //On iSeries platforms, the pid will be a qualified combination of job number, user, and jobname with a
        //qualifier of /.  The job number is a unique number assigned by the iSeries system that is always 6 digits.
        //Since job number is unique and numeric, we will strip off the rest and treat the job number as a pid value.
        int index = pid.indexOf("/");
        if (index > -1) {
            pid = pid.substring(0, index);
        }

        if (!pid.equals(""))
            pid += "."; // If there, suffix with dot to split it from generated info
        try {
            File thisLog;
            if (debugLocation == null) {
                thisLog = File.createTempFile("hpelDebugLogger." + pid, ".log");
            } else {
                File debugLoc = new File(debugLocation);
                AccessHelper.makeDirectories(debugLoc);
                thisLog = File.createTempFile("hpelDebugLogger." + pid, ".log", debugLoc);
            }
            fHandler = new FileHandler(thisLog.getPath(), debugFileSz, numDebugFiles);
        } catch (Exception e) {
            System.out.println("Exception creating fileHandler for separate LogRepositoryManager logging. Turning off special logging: " + e);
            LogRepositoryBaseImpl.debugLogger.setUseParentHandlers(false); // Logger functionally does nothing at this point
            return false;
        }
        fHandler.setFormatter(new SimpleFormatter());
        LogRepositoryBaseImpl.debugLogger.addHandler(fHandler);
        LogRepositoryBaseImpl.debugLogger.setUseParentHandlers(false);
        debugEnabled = true;
        return true;
    }

    /**
     * for classes in logging (which cannot log into the normal space, retrieve the special logger
     * 
     * @return the specific logger that classes logging but not in normal flow will use
     */
    public static Logger getLogger() {
        return debugLogger;
    }

    /**
     * take action on notification of trace or log file deletion or rolling. This is the dummy version as nonRuntime
     * managers need not implement this method, and subManagers who are unaware of file management cannot implement it
     * 
     * @param eventType Type of event (delete or roll). It is assumed that the manager already knows what type of
     *            logging it is working with (log vs trace).
     */
    public void notifyOfFileAction(String eventType) {}

}
