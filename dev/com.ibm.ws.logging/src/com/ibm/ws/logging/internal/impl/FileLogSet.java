/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.ws.logging.internal.TraceSpecification;
import com.ibm.wsspi.logging.TextFileOutputStreamFactory;

/**
 * Represents a set of log files with the same prefix and extension that are
 * uniquely identified by a date and id.
 * <p>
 * This object is not thread-safe.
 */
public class FileLogSet {
    /**
     * True if this represents a rolling log file. A rolling log file starts
     * with a base name (e.g., "messages.log"), and the file is renamed once it
     * exceeds a maximum size (e.g., "messages_yy.MM.dd_HH.mm.ss.id.log").
     */
    private final boolean rolling;

    /**
     * The directory containing the logs.
     */
    private File directory;

    /**
     * The log file base name (e.g., "messages").
     */
    private String fileName;

    /**
     * The log file extension (e.g., ".log").
     */
    private String fileExtension;

    /**
     * The maximum number of logs, or less than or equal to zero for unlimited.
     * For a rolling log file, this includes the base log file.
     */
    private int maxFiles;

    /**
     * A pattern that matches a log with date and id.
     *
     * @see LoggingFileUtils#compileLogFileRegex
     */
    private Pattern filePattern;

    /**
     * Null if maxFiles is not positive.
     */
    private ArrayList<String> files;

    /**
     * The date string ("_yy.MM.dd_HH.mm.ss") of the last created file.
     */
    private String lastDateString;

    /**
     * The counter for the last created file.
     */
    private int lastCounter;

    public FileLogSet(boolean rolling) {
        this.rolling = rolling;
    }

    public File getDirectory() {
        return directory;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public Pattern getFilePattern() {
        return filePattern;
    }

    public int getMaxFiles() {
        return maxFiles;
    }

    /**
     * The maximum number of files that can have date strings.
     */
    private int getMaxDateFiles() {
        return rolling ? maxFiles - 1 : maxFiles;
    }

    /**
     * Updates the configuration for this set of logs.
     *
     * @param directory the log directory
     * @param fileName the file base name (e.g., "messages")
     * @param fileExtension the file extension (e.g., ".log")
     * @param maxFiles the maximum number of files, or 0 for unlimited
     * @return true if the directory, name, or extension changed
     */
    public boolean update(File directory, String fileName, String fileExtension, int maxFiles) {
        this.maxFiles = maxFiles;

        boolean updateLocation = !directory.equals(this.directory) || !fileName.equals(this.fileName) || !fileExtension.equals(this.fileExtension);
        if (updateLocation) {
            this.directory = directory;
            this.fileName = fileName;
            this.fileExtension = fileExtension;
            filePattern = LoggingFileUtils.compileLogFileRegex(fileName, fileExtension);

            // The location was updated, so remove our list of cached files.
            files = null;
        }

        if (maxFiles <= 0) {
            files = null;
        } else {
            if (files == null) {
                files = new ArrayList<String>();

                // Sort and add all existing files.
                String[] existing = LoggingFileUtils.safelyFindFiles(directory, filePattern);
                if (existing != null) {
                    Arrays.sort(existing, NaturalComparator.instance);
                    files.addAll(Arrays.asList(existing));
                }
            }

            // Delete excess old files if necessary.
            int maxTrackedFiles = getMaxDateFiles();
            while (files.size() > maxTrackedFiles) {
                removeFile(0);
            }

            // If the location was updated, initialize the counter from the
            // remaining files (if any).
            if (updateLocation) {
                if (files.isEmpty()) {
                    lastDateString = null;
                } else {
                    Matcher matcher = filePattern.matcher(files.get(files.size() - 1));
                    if (!matcher.matches())
                        throw new IllegalStateException();

                    lastDateString = matcher.group(1);
                    lastCounter = Integer.parseInt(matcher.group(2));
                }
            }
        }

        return updateLocation;
    }

    /**
     * Creates a new file for this log set. If this is a rolling log, this will
     * rename the existing file and then return it on success. Otherwise, this
     * will return a new unique file.
     *
     * @return the file on success, or null on failure
     * @throws IOException
     */
    public File createNewFile() throws IOException {
        return createNewFile(true);
    }

    /**
     * Creates a new file for this log set. If this is a rolling log, this will
     * rename the existing file and then return it on success. Otherwise, this
     * will return a new unique file.
     *
     * @param show error message if showError is true
     * @return the file on success, or null on failure
     * @throws IOException
     */
    public File createNewFile(boolean showError) throws IOException {
        if (LoggingFileUtils.validateDirectory(directory, showError) == null) {
            return null;
        }

        return rolling ? rollFile(showError) : createNewUniqueFile(null, showError);
    }

    /**
     * Attempt to rename the base log file to a new log file, and then recreate
     * the base log file.
     *
     * @param show error message if showError is true
     * @return the base log file
     */
    private File rollFile(boolean showError) throws IOException {
        // If the base file exists, rename it and recreate it.
        File file = new File(directory, fileName + fileExtension);
        if (file.isFile()) {
            // Reuse the file if it's already empty.
            if (file.length() == 0) {
                return file;
            }

            if (maxFiles == 1) {
                if (!LoggingFileUtils.deleteFile(file, showError)) {
                    // We failed to delete the file and issued a message.
                    return file;
                }
            } else if (createNewUniqueFile(file, showError) == null) {
                // We failed to rename (or copy + delete) the base file to a new
                // file, and we already issued a message.
                return file;
            }
        }

        if (!file.createNewFile() && showError) {
            // Unsafe to use FFDC or ras: log to raw stderr
            String msg = Tr.formatMessage(TraceSpecification.getTc(), "UNABLE_TO_CREATE_RESOURCE_NOEX", file);
            BaseTraceService.rawSystemErr.println(msg);
        }

        return file;
    }

    /**
     * Create a new unique file name. If the source file is specified, the new
     * file should be created by renaming the source file as the unique file.
     *
     * @param srcFile the file to rename, or null to create a new file
     * @param show error message if showError is true
     * @return the newly created file, or null if the could not be created
     * @throws IOException if an unexpected I/O error occurs
     */
    private File createNewUniqueFile(File srcFile, boolean showError) throws IOException {
        String dateString = getDateString();
        int index = findFileIndexAndUpdateCounter(dateString);

        String destFileName;
        File destFile;
        do {
            int counter = lastCounter++;
            destFileName = fileName + dateString + '.' + counter + fileExtension;
            destFile = new File(directory, destFileName);

            boolean success;
            if (srcFile == null) {
                success = destFile.createNewFile();
            } else {
                // We don't want to rename over an existing file, so try to
                // avoid doing so with a racy exists() check.
                if (!destFile.exists()) {
                    success = srcFile.renameTo(destFile);
                } else {
                    success = false;
                }
            }

            if (success) {
                // Add the file to our list, which will cause old files to be
                // deleted if we've reached the max.
                addFile(index, destFileName);
                return destFile;
            }
        } while (destFile.isFile());

        if (srcFile != null && copyFileTo(srcFile, destFile)) {
            addFile(index, destFileName);
            return LoggingFileUtils.deleteFile(srcFile, showError) ? destFile : null;
        }

        // Unsafe to use FFDC or ras: log to raw stderr
        if (showError) {
            String msg = Tr.formatMessage(TraceSpecification.getTc(), "UNABLE_TO_CREATE_RESOURCE_NOEX", destFile);
            BaseTraceService.rawSystemErr.println(msg);
        }
        return null;
    }

    /**
     * Returns the current date as a string.
     * <p>
     * This method is intended to be overridden by unit tests.
     */
    protected String getDateString() {
        synchronized (LoggingFileUtils.FILE_DATE) {
            return LoggingFileUtils.FILE_DATE.format(new Date());
        }
    }

    /**
     * Find the index in the files list where a new file should be inserted,
     * and ensure {@link #lastDataString} and {@link #lastCounter} are accurate.
     *
     * @param dateString the date string for the new file
     * @return the position in the files list where a new file should be inserted
     */
    private int findFileIndexAndUpdateCounter(String dateString) {
        if (dateString.equals(lastDateString)) {
            if (maxFiles <= 0) {
                // addFile is a no-op, so it doesn't matter what we return.
                return -1;
            }

            // Current date is the same, so the file goes at the end.
            return files.size();
        }

        // Use the new date string.
        lastDateString = dateString;
        lastCounter = 0;

        if (maxFiles <= 0) {
            // addFile is a no-op, so it doesn't matter what we return.
            return -1;
        }

        if (files.isEmpty() || dateString.compareTo(lastDateString) > 0) {
            // Current date is newer, so the file goes at the end.
            return files.size();
        }

        // Current date is older, so determine where it goes.
        String partialFileName = fileName + dateString + '.';
        return -Collections.binarySearch(files, partialFileName, NaturalComparator.instance) - 1;
    }

    private boolean copyFileTo(File srcFile, File destFile) throws IOException {
        // bummer. We have to copy it.
        if (!destFile.createNewFile()) {
            return false;
        }

        // Use file channels, as that defers to OS level mechanisms to copy the file
        FileChannel target = null;
        FileChannel source = null;
        boolean success = false;
        try {
            TextFileOutputStreamFactory fileStreamFactory = TrConfigurator.getFileOutputStreamFactory();
            target = fileStreamFactory.createOutputStream(destFile).getChannel();

            source = new FileInputStream(srcFile).getChannel();
            source.transferTo(0, source.size(), target);
            success = true;
        } catch (IOException ioe) {
        } finally {
            LoggingFileUtils.tryToClose(target);
            LoggingFileUtils.tryToClose(source);

            if (!success) {
                LoggingFileUtils.deleteFile(destFile);
                return false;
            }
        }

        return true;
    }

    /**
     * Adds a file name to the files list at the specified index. If adding
     * this file would cause the number of files to exceed the maximum, remove
     * all files after the specified index, and then remove the oldest files
     * until the number is reduced to the maximum.
     *
     * @param index the index in the files list to insert the file
     * @param file the file name
     */
    private void addFile(int index, String file) {
        if (maxFiles > 0) {
            int numFiles = files.size();
            int maxDateFiles = getMaxDateFiles();

            // If there is no max or we have fewer than max, then we're done.
            if (maxDateFiles <= 0 || numFiles < maxDateFiles) {
                files.add(index, file);
            } else {
                // The file names we deal with (messages_xx.xx.xx_xx.xx.xx.log)
                // have dates, and we want to always be using the "most recent",
                // so delete everything "newer" (index and after), which might be
                // present if the system clock goes backwards, and then delete
                // the oldest files until only maxDateFiles-1 remain.
                while (files.size() > index) {
                    removeFile(files.size() - 1);
                }
                while (files.size() >= maxDateFiles) {
                    removeFile(0);
                }

                files.add(file);
            }
        }
    }

    /**
     * Remove and delete a file from the file list.
     *
     * @param index the file index
     */
    private void removeFile(int index) {
        String file = files.remove(index);
        LoggingFileUtils.deleteFile(directory, file);
    }
}
