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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;
import com.ibm.wsspi.logging.TextFileOutputStreamFactory;

/**
 * This class manages the writing to a log or selection of log files.
 * All files will be replaced (meaning there is no append -- if you re-use a file
 * name, you get a new file). If a max file size is configured, logs will be rolled
 * when the file reaches that size. A limit is placed on how many files are kept.
 * <p>
 * Even in the case of rolling logs: the log will be originally created with
 * a non-unique name. It will be renamed when the log is rolled.
 */
public class FileLog {
    private static final TraceComponent tc = Tr.register(FileLog.class);

    enum StreamStatus {
        INIT, ACTIVE, CLOSED
    };

    // None of these variables should be *set* directly.b See #setStreamStatus
    protected FileOutputStream currentFileStream;
    protected CountingOutputStream currentCountingStream;
    private PrintStream currentPrintStream;
    protected StreamStatus currentStatus;

    protected static final String nl = AccessController.doPrivileged(new PrivilegedAction<String>() {
        @Override
        public String run() {
            return System.getProperty("line.separator");
        }
    });

    int nlen = nl.length();

    protected static final boolean isWindows = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
        @Override
        public Boolean run() {
            return System.getProperty("os.name").toLowerCase().contains("windows");
        }
    });

    /**
     * Empty/dummy output stream used to satisfy PrintStream constructor
     * requirements
     */
    static class DummyOutputStream extends OutputStream {
        final static DummyOutputStream instance = new DummyOutputStream();
        final static PrintStream psInstance = new PrintStream(instance);

        @Override
        public void write(int b) throws IOException {}
    }

    protected final FileLogSet fileLogSet = new FileLogSet(true);

    /**
     * The maximum size of the file, in bytes. When this size is reached, the
     * log will be rolled over into a new file.
     */
    protected long maxFileSizeBytes;

    /**
     * This method will check to see if the supplied parameters match the settings on the <code>oldLog</code>,
     * if they do then the <code>oldLog</code> is returned, otherwise a new FileLogHolder will be created.
     *
     * @param oldLog The previous FileLogHolder that may or may not be replaced by a new one, may
     *            be <code>null</code> (will cause a new instance to be created)
     * @param logHeader
     *            Header to print at the top of new log files
     * @param logDirectory
     *            Directory in which to store created log files
     * @param newFileName
     *            File name for new log: this will be split into a name and extension
     * @param maxFiles
     *            New maximum number of log files. If 0, log files won't be pruned.
     * @param maxSizeBytes
     *            New maximum log file size in bytes. If 0, log files won't be rolled.
     * @return a log holder. If all values are the same, the old one is returned, otherwise a new log holder is created.
     */
    public static FileLog createFileLogHolder(FileLog oldLog,
                                              File logDirectory,
                                              String newFileName,
                                              int maxFiles,
                                              long maxSizeBytes) {

        final FileLog logHolder;

        // We're only supporting names in the log directory
        // Our configurations encourage use of forward slash on all platforms
        int lio = newFileName.lastIndexOf("/");
        if (lio > 0) {
            newFileName = newFileName.substring(lio + 1);
        }
        if (File.separatorChar != '/') {
            // Go sniffing for other separators where we should (windows)2
            lio = newFileName.lastIndexOf(File.separatorChar);
            if (lio > 0) {
                newFileName = newFileName.substring(lio + 1);
            }
        }

        final String fileName;
        final String fileExtension;

        // Find the name vs. extension: name=file extension=.log
        final int dio = newFileName.lastIndexOf(".");
        if (dio > 0) {
            fileName = newFileName.substring(0, dio);
            fileExtension = newFileName.substring(dio);
        } else {
            fileName = newFileName;
            fileExtension = "";
        }

        // IF there are changes to the rolling behavior, it will show up in a change to either
        // maxFiles or maxBytes
        if (oldLog != null) {
            logHolder = oldLog;
            logHolder.update(logDirectory, fileName, fileExtension, maxFiles, maxSizeBytes);
        } else {
            // Send to bit bucket until the file is created (true -- create/replace if needed).
            logHolder = new FileLog(logDirectory, fileName, fileExtension, maxFiles, maxSizeBytes);
        }

        return logHolder;
    }

    /**
     * Private constructor for this class as a lot of conversion needs to take place on the parameters prior to the object being constructed.
     *
     * @param logHeader The header to write at the beginning of all new log files
     * @param dirName The fully qualified name of the directory to put the logs into
     * @param fileName The name of the file without an extension to put the logs into
     * @param fileExtension The extension to put on the file that the logs are going into
     * @param maxNumFiles The maximum number of files to create if this is a rolling log (i.e. when <code>alwaysCreateNewFile</code> is <code>false</code> and
     *            <code>maxSizeBytes</code> is greater than 0)
     * @param maxFileSizeBytes The maximum file size a single file should create when this is a rolling log (i.e. when <code>alwaysCreateNewFile</code> is <code>false</code>)
     */
    private FileLog(File directory, String fileName, String fileExtension, int maxNumFiles, long maxFileSizeBytes) {

        currentPrintStream = DummyOutputStream.psInstance;
        update(directory, fileName, fileExtension, maxNumFiles, maxFileSizeBytes);
    }

    private synchronized void update(File newDirectory, String newFileName, String newFileExtension, int newMaxFiles, long newMaxSizeBytes) {
        boolean updateLocation;

        Object token = ThreadIdentityManager.runAsServer();
        try {
            updateLocation = fileLogSet.update(newDirectory, newFileName, newFileExtension, newMaxFiles);
        } finally {
            ThreadIdentityManager.reset(token);
        }

        if (updateLocation) {
            // If the file name/extension/directory has changed,
            // change status to "INIT" to force it to be replaced
            setStreamStatus(StreamStatus.INIT, currentFileStream, currentCountingStream, currentPrintStream);
        }

        maxFileSizeBytes = newMaxSizeBytes;
    }

    /**
     * Close this file/stream holder:
     * Flush the print stream, try to close the print and file streams --
     * replace the "current" stream with the bit bucket.
     */
    public synchronized void close() {
        if (currentStatus != StreamStatus.CLOSED) {
            // Only flush the print stream: don't close it.
            currentPrintStream.flush();

            if (!FileLogUtils.tryToClose(currentPrintStream)) {
                FileLogUtils.tryToClose(currentFileStream);
            }

            // Send to bit bucket again-- this holder is done (false -- do not replace)
            setStreamStatus(StreamStatus.CLOSED, null, null, DummyOutputStream.psInstance);
        }
    }

    /**
     * Write a pre-formated record
     *
     * @param record
     */
    public synchronized void writeRecord(String record) {
        long length = record.length() + nlen;
        PrintStream ps = getPrintStream(length, null);
        ps.println(record);
    }

    /**
     * Write a pre-formated record. This is used for when we have
     * signed and/or encrypted audit records.
     *
     * @param record
     */
    public synchronized void writeRecord(byte[] record, String header) {
        long length = record.length + nlen;
        PrintStream ps = getPrintStream(length, header);
        if (isWindows)
            ps.write(record, 0, (int) length - 2);
        else
            ps.write(record, 0, (int) length - 1);

        ps.println();
    }

    /**
     * Obtain the current printstream: called from synchronized methods
     *
     * @param requiredLength
     * @return
     */
    private synchronized PrintStream getPrintStream(long numNewChars, String header) {
        switch (currentStatus) {
            case INIT:
                return createStream(header);
            case ACTIVE:
                if (maxFileSizeBytes > 0) {
                    long bytesWritten = currentCountingStream.count();

                    // Replace the stream if the size is or will likely be
                    // exceeded.  We're estimating one byte per char, which is
                    // only accurate for single-byte character sets.  That's
                    // fine: if a multi-byte character set is being used and
                    // we underestimate the number of bytes that will be
                    // written, we'll roll the log next time
                    if (bytesWritten + numNewChars > maxFileSizeBytes) {
                        return createStream(header);

                    }
                }
                break;
        }

        return currentPrintStream;
    }

    /**
     * @return current file size
     */
    public synchronized long getCurrentCountStream() {

        if (currentCountingStream != null) {
            return currentCountingStream.count();
        } else {
            return 0;
        }
    }

    /**
     * @return a new print stream
     */
    private synchronized PrintStream createStream(String header) {
        FileOutputStream newFileStream = null;
        CountingOutputStream newCountingStream = null;
        PrintStream newPrintStream = null;

        File targetLogFile = null;
        long realMaxFileSizeBytes = maxFileSizeBytes;

        // Store the value, and temporarily "disable" log rolling to avoid
        // re-trying to roll the log if the ThreadIdentityManager creates
        // log or trace records. This value is reset in the finally block
        maxFileSizeBytes = 0;

        Object token = ThreadIdentityManager.runAsServer();
        try {
            // Close the existing file
            currentPrintStream.flush();
            if (currentFileStream != null) {
                FileLogUtils.tryToClose(currentFileStream);
            }

            // Get the non-unique file (e.g. trace.log)
            targetLogFile = FileLogUtils.createNewFile(fileLogSet);

            if (targetLogFile != null) {
                try {
                    // create the new file stream -- never append
                    // When asking Erin about the above comment, she could not find a reason for why we would
                    // never use the append flag.  Testing across numerous platforms has shown that using the
                    // append flag does not appear to cause any problems with log creation and log rolling - a
                    // switch to using append = true is being made to address APAR PI57488
                    TextFileOutputStreamFactory fileStreamFactory = TrConfigurator.getFileOutputStreamFactory();
                    newFileStream = fileStreamFactory.createOutputStream(targetLogFile, true);
                    newCountingStream = new CountingOutputStream(newFileStream);
                    newPrintStream = new PrintStream(newCountingStream);
                } catch (IOException e) {
                    // should not happen: we created the new file in createNewFile
                }

                // If both the file and print streams were created successfully,
                // re-assign the active/current streams to point to the new
                // artifacts
                if (newFileStream != null && newCountingStream != null && newPrintStream != null) {

                    // re-assign the current file and print streams
                    setStreamStatus(StreamStatus.ACTIVE, newFileStream, newCountingStream, newPrintStream);
                } else {
                    // something didn't go right along the way...
                    // don't keep spinning trying to create files when you can't
                    setStreamStatus(StreamStatus.CLOSED, null, null, DummyOutputStream.psInstance);
                }
            } else {
                // something didn't go right along the way...
                // don't keep spinning trying to create files when you can't
                setStreamStatus(StreamStatus.CLOSED, null, null, DummyOutputStream.psInstance);
            }
        } finally {
            ThreadIdentityManager.reset(token);
            maxFileSizeBytes = realMaxFileSizeBytes;
            if (currentStatus == StreamStatus.ACTIVE && header != null) {
                currentPrintStream.println(header);
            }
        }

        return currentPrintStream;
    }

    private synchronized void setStreamStatus(StreamStatus newStatus, FileOutputStream newFileStream, CountingOutputStream newCountingStream, PrintStream newPrintStream) {
        currentStatus = newStatus;
        currentFileStream = newFileStream;
        currentCountingStream = newCountingStream;
        currentPrintStream = newPrintStream;
    }

    /**
     * Release the file/streams for future activation
     */
    public synchronized void releaseFile() {
        if (currentStatus == StreamStatus.ACTIVE) {
            // Only flush the print stream: don't close it.
            currentPrintStream.flush();

            if (!FileLogUtils.tryToClose(currentPrintStream)) {
                FileLogUtils.tryToClose(currentFileStream);
            }

            // Send to bit bucket again-- this holder is done (false -- do not replace)
            setStreamStatus(StreamStatus.INIT, null, null, DummyOutputStream.psInstance);
        }
    }
}