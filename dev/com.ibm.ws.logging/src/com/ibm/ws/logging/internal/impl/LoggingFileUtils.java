/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

import java.io.Closeable;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.Tr;
import com.ibm.ws.logging.internal.TraceSpecification;

/**
 *
 */
public class LoggingFileUtils {
    /** Simple date format for file names: use only while synchronized */
    public final static SimpleDateFormat FILE_DATE = new SimpleDateFormat("_yy.MM.dd_HH.mm.ss");

    /**
     * Filter used to match filenames for pruning when file rolling is enabled.
     */
    static class RegexFilenameFilter implements FilenameFilter {
        final Pattern p;

        RegexFilenameFilter(Pattern p) {
            this.p = p;
        }

        @Override
        public boolean accept(File dir, String name) {
            Matcher m = p.matcher(name);
            return m.matches();
        }
    }

    /**
     * Safely retrieve a list of files from the given directory:
     * filter the files using the specified regular expression
     *
     * @param dirName Directory to list
     * @param p Regex pattern that should be used to filter listed files
     * @return String array containing files matching the pattern, or null
     */
    static String[] safelyFindFiles(final File directory, final Pattern p) {
        String[] rc = null;

        try {
            rc = AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<String[]>() {
                @Override
                public String[] run() throws Exception {
                    return directory.list(new LoggingFileUtils.RegexFilenameFilter(p));
                }
            });
        } catch (Exception ex) {
            // No FFDC code needed
        }

        return rc;
    }

    /**
     * This method will create the directory if it does not exist,
     * ensuring the specified location is writable.
     *
     * @param The new directory location
     * @return A valid/accessible/created directory or null
     */
    static File validateDirectory(final File directory) {
        return validateDirectory(directory, true);
    }

    /**
     * This method will create the directory if it does not exist,
     * ensuring the specified location is writable.
     *
     * @param The new directory location
     * @param show error message if showError is true
     * @return A valid/accessible/created directory or null
     */
    static File validateDirectory(final File directory, final boolean showError) {

        File newDirectory = null;

        try {
            newDirectory = AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<File>() {
                @Override
                public File run() throws Exception {
                    boolean ok = true;

                    if (!directory.exists())
                        ok = directory.mkdirs() || directory.exists(); //2nd exists check is necessary to close timing window

                    if (!ok) {
                        // Unsafe to use FFDC or ras: log to raw stderr
                        showErrorMsg(showError, "UNABLE_TO_CREATE_RESOURCE_NOEX", new Object[] { directory.getAbsolutePath() });
                        return null;
                    }
                    return directory;
                }
            });
        } catch (PrivilegedActionException e) {
            // Unsafe to use FFDC or ras: log to raw stderr
            showErrorMsg(showError, "UNABLE_TO_CREATE_RESOURCE", new Object[] { directory.getAbsolutePath(), e });
        }

        return newDirectory;
    }

    /**
     * Show the error message if showError is true
     *
     * @param showError
     * @param msgKey
     * @param objs
     */
    private static void showErrorMsg(boolean showError, String msgKey, Object[] objs) {
        if (showError) {
            String msg = Tr.formatMessage(TraceSpecification.getTc(), msgKey, objs);
            BaseTraceService.rawSystemErr.println(msg);
        }
    }

    /**
     * This method will create a new file with the specified name and extension in the specified directory. If a unique file is required then it will add a timestamp to the file
     * and if necessary a unqiue identifier to the file name.
     *
     * @return The file or <code>null</code> if an error occurs
     * @see #getUniqueFile(File, String, String)
     */
    public static File createNewFile(final FileLogSet fileLogSet) {
        return createNewFile(fileLogSet, true);
    }

    /**
     * This method will create a new file with the specified name and extension in the specified directory. If a unique file is required then it will add a timestamp to the file
     * and if necessary a unqiue identifier to the file name.
     *
     * @return The file or <code>null</code> if an error occurs
     * @see #getUniqueFile(File, String, String)
     */
    public static File createNewFile(final FileLogSet fileLogSet, final boolean showError) {
        final File directory = fileLogSet.getDirectory();
        final String fileName = fileLogSet.getFileName();
        final String fileExtension = fileLogSet.getFileExtension();
        File f = null;

        try {
            f = AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<File>() {
                @Override
                public File run() throws Exception {
                    return fileLogSet.createNewFile(showError);
                }
            });
        } catch (PrivilegedActionException e) {
            File exf = new File(directory, fileName + fileExtension);

            // Unsafe to use FFDC or ras: log to raw stderr
            showErrorMsg(showError, "UNABLE_TO_CREATE_RESOURCE", new Object[] { exf.getAbsolutePath(), e });
        }

        return f;
    }

    /**
     * Compiles a pattern that can be used to match rolled over log file names.
     * The first capturing group is the date/time string, and the second
     * capturing group is the ID.
     *
     * @param baseName the log file basename (e.g., "messages")
     * @param extension the log file extension (e.g., ".log")
     * @param idOptional true if the rollover ID is optional
     * @return
     */
    static Pattern compileLogFileRegex(String baseName, String extension) {
        StringBuilder builder = new StringBuilder();

        // filename (dots escaped)
        builder.append(Pattern.quote(baseName));

        // _yy.MM.dd_HH.mm.ss
        builder.append("(_\\d\\d\\.\\d\\d\\.\\d\\d_\\d\\d\\.\\d\\d\\.\\d\\d)");

        // numeric identifier
        builder.append("(?:\\.(\\d+))");

        // extension (dots escaped)
        builder.append(Pattern.quote(extension));

        return Pattern.compile(builder.toString());
    }

    /**
     * Safely delete the specified file.
     *
     * @param dirName Directory containing the file
     * @param name Target file name
     */
    static final void deleteFile(final File directory, final String name) {
        deleteFile(new File(directory, name));
    }

    /**
     * @param file
     */
    public static boolean deleteFile(final File f) {
        return deleteFile(f, true);
    }

    /**
     * @param file
     * @param show error message if showError is true
     */
    public static boolean deleteFile(final File f, final boolean showError) {
        try {
            return AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<Boolean>() {
                @Override
                public Boolean run() {
                    if (!f.delete()) {
                        // Unsafe to use FFDC or ras: log to raw stderr
                        showErrorMsg(showError, "UNABLE_TO_DELETE_RESOURCE_NOEX", new Object[] { f });
                        return false;
                    }

                    return true;
                }
            });
        } catch (PrivilegedActionException e) {
            // Unsafe to use FFDC or ras: log to raw stderr
            showErrorMsg(showError, "UNABLE_TO_DELETE_RESOURCE", new Object[] { f, e });
            return false;
        }
    }

    /**
     * Try to close the closable (output or input stream) passed as a parameter:
     * handles null, and exceptions on close operation itself.
     *
     * @param c
     */
    public static boolean tryToClose(Closeable c) {
        if (c != null) {
            try {
                c.close();
                return true;
            } catch (IOException e) {
            }
        }
        return false;
    }
}
