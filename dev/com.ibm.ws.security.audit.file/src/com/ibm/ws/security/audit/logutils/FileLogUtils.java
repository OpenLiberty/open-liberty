/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.audit.logutils;

import java.io.Closeable;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *
 */
public class FileLogUtils {

    private static final TraceComponent tc = Tr.register(FileLogUtils.class);

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
                    return directory.list(new FileLogUtils.RegexFilenameFilter(p));
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

        File newDirectory = null;

        try {
            newDirectory = AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<File>() {
                @Override
                public File run() throws Exception {
                    boolean ok = true;

                    if (!directory.exists())
                        ok = directory.mkdirs() || directory.exists(); //2nd exists check is necessary to close timing window

                    if (!ok) {
                        Tr.error(tc, "UNABLE_TO_DELETE_RESOURCE_NOEX", new Object[] { directory.getAbsolutePath() });
                        return null;
                    }
                    return directory;
                }
            });
        } catch (PrivilegedActionException e) {
            Tr.error(tc, "UNABLE_TO_DELETE_RESOURCE", new Object[] { directory.getAbsolutePath(), e });
        }

        return newDirectory;
    }

    /**
     * This method will create a new file with the specified name and extension in the specified directory. If a unique file is required then it will add a timestamp to the file
     * and if necessary a unqiue identifier to the file name.
     *
     * @return The file or <code>null</code> if an error occurs
     * @see #getUniqueFile(File, String, String)
     */
    static File createNewFile(final FileLogSet fileLogSet) {
        final File directory = fileLogSet.getDirectory();
        final String fileName = fileLogSet.getFileName();
        final String fileExtension = fileLogSet.getFileExtension();
        File f = null;

        try {
            f = AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<File>() {
                @Override
                public File run() throws Exception {
                    return fileLogSet.createNewFile();
                }
            });
        } catch (PrivilegedActionException e) {
            File exf = new File(directory, fileName + fileExtension);
            Tr.error(tc, "UNABLE_TO_DELETE_RESOURCE", new Object[] { exf.getAbsolutePath(), e });
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
        try {
            return AccessController.doPrivileged(new java.security.PrivilegedAction<Boolean>() {
                @Override
                public Boolean run() {
                    try {
                        Files.delete(f.toPath());
                        return true;
                    } catch (NoSuchFileException nsfe) {
                        Tr.debug(tc, "no such file");
                        Tr.error(tc, "UNABLE_TO_DELETE_RESOURCE", new Object[] { f, nsfe });
                        return false;

                    } catch (DirectoryNotEmptyException dnee) {
                        Tr.debug(tc, "dir not empty");
                        Tr.error(tc, "UNABLE_TO_DELETE_RESOURCE", new Object[] { f, dnee });
                        return false;
                    } catch (IOException ioe) {
                        Tr.debug(tc, "dir not empty");
                        Tr.error(tc, "UNABLE_TO_DELETE_RESOURCE", new Object[] { f, ioe });
                        return false;
                    }
                }
            });
        } finally {

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
