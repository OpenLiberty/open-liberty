/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.logging.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.http.logging.LogFile;
import com.ibm.wsspi.logging.TextFileOutputStreamFactory;

/**
 * This utility class is the entry point for writing to the actual log file.
 * This takes the input data and queues it up for writing to a file on
 * another thread, thus freeing up the caller to continue on while the IO occurs
 * in the background.
 * 
 * This logger can be stopped and started repeatedly; however, it cannot be
 * restarted once the destroy method has been used.
 */
public class LoggerOffThread implements LogFile {

    /** RAS tracing variable */
    protected static final TraceComponent tc = Tr.register(LoggerOffThread.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** List of possible logger states */
    private enum State {
        /** Idle stopped state, can be started from here */
        IDLE,
        /** Currently running state */
        RUNNING,
        /** Disabled state, cannot be restarted */
        DISABLED
    }

    /** List of possible worker thread states */
    protected enum WorkerState {
        /** Worker thread is running */
        RUNNING,
        /** Worker thread is in the process of stopping */
        STOPPING,
        /** Worker thread is fully stopped */
        STOPPED
    }

    /** Timeout to use for some lock waits */
    protected static final long TIMEOUT = 10000L;

    /** Log file associated with this particular thread */
    private File myFile = null;
    /** Simple name of the file */
    private String myName = null;
    /** Full directory+name of the file */
    private String myFullName = null;
    /** File channel for this logging thread */
    private FileChannel myChannel = null;
    /** Worker thread handling the queued up action */
    private WorkerThread myWorker = null;
    /** Current state of the logger */
    private State state = State.IDLE;
    /** Maximum size for the log to grow to */
    private long maxFileSize = LogFile.UNLIMITED;
    /** Maximum number of backup files to keep around */
    private int maxBackupFiles = 1;

    /**
     * Constructor that opens a reference to the input file name. Note that
     * the start() method must be used before logging will actually begin.
     * 
     * @param name
     * @throws FileNotFoundException
     */
    public LoggerOffThread(String name) throws FileNotFoundException {
        setFilename(name);
    }

    protected LoggerOffThread() {

    }

    public void setFilename(String name) throws FileNotFoundException {
        boolean started = isStarted();
        if (started) {
            stop();
        }
        this.myFullName = name;
        this.myFile = new File(name);
        this.myName = this.myFile.getName();
        this.myChannel = createFileOutputStream().getChannel();

        if (started) {
            start();
        }
    }

    protected FileOutputStream createFileOutputStream() throws FileNotFoundException {
        FileOutputStream fileOutputStream = null;
        // allocate so it will be tagged on z/OS.
        // allows log to be readable using tail on z/OS.
        TextFileOutputStreamFactory f = TrConfigurator.getFileOutputStreamFactory();
        try {
            fileOutputStream = f.createOutputStream(this.myFile, true);
        } catch (IOException e) {
            // We'll get an FFDC here...
            fileOutputStream = new FileOutputStream(this.myFile, true);
        }
        return fileOutputStream;
    }

    /**
     * Provide access to the output channel object.
     * 
     * @return FileChannel
     */
    protected FileChannel getChannel() {
        return this.myChannel;
    }

    /**
     * Set the output channel object to the input value.
     * 
     * @param channel
     */
    protected void setChannel(FileChannel channel) {
        this.myChannel = channel;
    }

    /**
     * Get access to the File associated with this logger.
     * 
     * @return File
     */
    protected File getFile() {
        return this.myFile;
    }

    /**
     * Query what the name of the file is for this logger. This will return
     * null if no file is currently configured.
     * 
     * @return String
     */
    public String getFilePathName() {
        return this.myFullName;
    }

    /**
     * Query the name of the file for this logger. This is just the file name
     * without any path information.
     * 
     * @return String
     */
    @Override
    public String getFileName() {
        return this.myName;
    }

    /**
     * Log the given data to the log file. If this returns false, then the input
     * buffer has also been released, the same as if it was successfully written
     * to the logfile.
     * 
     * @param data
     * @return boolean (true means it succeeded)
     */
    public boolean log(WsByteBuffer data) {
        if (null == data) {
            // return failure
            return false;
        }
        // if we've stopped then there is no worker to hand this to; however, the
        // caller does not expect to have to release buffers handed to the
        // logger so do that here first
        if (State.RUNNING != this.state) {
            data.release();
            return false;
        }
        return this.myWorker.enqueue(data);
    }

    /**
     * @see LogFile#start()
     */
    @Override
    public boolean start() {
        if (State.IDLE != this.state) {
            return false;
        }
        // start the worker thread
        if (null == this.myWorker) {
            this.myWorker = new WorkerThread();
        }
        this.myWorker.start();
        this.state = State.RUNNING;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, getFileName() + ": started\n" + this);
        }
        return true;
    }

    /**
     * @see LogFile#stop()
     */
    @Override
    public boolean stop() {
        // check to see if we've already stopped
        if (State.RUNNING != this.state) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, this.myName + ": Logger already stopped");
            }
            return true;
        }
        // set the flag that we're no longer accepting new data
        this.state = State.IDLE;

        // tell the worker to stop accepting new data
        this.myWorker.triggerStop();
        this.myWorker = null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, getFileName() + ": stopped");
        }
        return true;
    }

    /**
     * @see LogFile#disable()
     */
    @Override
    public boolean disable() {
        if (State.RUNNING == this.state) {
            stop();
        }
        // safe to close the file now
        try {
            this.myChannel.close();
        } catch (IOException ioe) {
            FFDCFilter.processException(ioe, getClass().getName() + ".disable", "124", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to close the output file: " + this.myChannel);
            }
        }
        this.state = State.DISABLED;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, getFileName() + ": disabled");
        }
        return true;
    }

    /**
     * @see LogFile#isStarted()
     */
    @Override
    public boolean isStarted() {
        return State.RUNNING == this.state;
    }

    /**
     * @see LogFile#setMaximumSize(long)
     */
    @Override
    public boolean setMaximumSize(long size) {
        if (LogFile.UNLIMITED > size) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, this.myName + ": Invalid file size-> " + size);
            }
            return false;
        }
        if (0 == size) {
            // can't have a 0 size file as it would always be "full"
            this.maxFileSize = LogFile.UNLIMITED;
        } else {
            this.maxFileSize = size;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, this.myName + ": Set maximum size to " + this.maxFileSize);
        }
        return true;
    }

    /**
     * @see LogFile#getMaximumSize()
     */
    @Override
    public long getMaximumSize() {
        return this.maxFileSize;
    }

    /**
     * @see LogFile#getMaximumBackupFiles()
     */
    @Override
    public int getMaximumBackupFiles() {
        return this.maxBackupFiles;
    }

    /**
     * @see LogFile#setMaximumBackupFiles(int)
     */
    @Override
    public boolean setMaximumBackupFiles(int number) {
        if (0 > number) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, this.myName + ": Invalid negative number of backup files-> " + number);
            }
            return false;
        }
        this.maxBackupFiles = number;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, this.myName + ": Set maximum files to " + this.maxBackupFiles);
        }
        return true;
    }

    /**
     * Print this object as a string.
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(256);
        buffer.append(super.toString());
        buffer.append("\n  FilePathName: " + this.myFullName);
        buffer.append("\n  FileName: " + this.myName);
        buffer.append("\n  MaxFileSize: " + this.maxFileSize);
        buffer.append("\n  MaxBackupFiles: " + this.maxBackupFiles);
        buffer.append("\n  State: " + this.state);
        buffer.append("\n  Worker: " + this.myWorker);
        return buffer.toString();
    }

    /**
     * Worker thread class that handles pulling data off of the outgoing queue
     * and writing each buffer to the file. Each time this wakes up it will
     * purge the entire queue to the file.
     * 
     */
    private class WorkerThread extends Thread {

        /** State of worker thread */
        private WorkerState workerState = WorkerState.RUNNING;
        /** Lock object for the stop process */
        private final Object stopLock = new Object()
        {};
        /** Lock object for interacting with the work queue */
        private final Object lock = new Object()
        {};
        /** Work queue storing data */
        private LinkedList<WsByteBuffer> queue = null;
        /** List of backup files stored */
        private LinkedList<File> backups = null;
        /** path+name information of log files */
        private String fileinfo = null;
        /** Any filetype extension information of log files */
        private String extensioninfo = null;
        /** Formatter to use when saving backup files */
        private SimpleDateFormat myFormat = null;

        private long bytesWritten = 0;

        /**
         * Constructor to create this new worker thread.
         */
        protected WorkerThread() {
            super();
            this.queue = new LinkedList<WsByteBuffer>();
        }

        /**
         * Start this worker thread accepting and handling new data.
         * 
         */
        @Override
        public void start() {
            if (0 < getMaximumBackupFiles()) {
                this.myFormat = new SimpleDateFormat("_yy.MM.dd_HH.mm.ss", Locale.US);
                int index = getFileName().lastIndexOf(".");
                if (-1 != index) {
                    index += (getFilePathName().length() - getFileName().length());
                    this.fileinfo = getFilePathName().substring(0, index);
                    this.extensioninfo = getFilePathName().substring(index);
                } else {
                    this.fileinfo = getFilePathName();
                    this.extensioninfo = "";
                }
                this.backups = new LinkedList<File>();
            }

            //Set bytesWritten from the already existing file
            try {
                bytesWritten = myChannel.size();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "bytesWritten : " + bytesWritten);
                }
            } catch (IOException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unable to aquire current file size. Setting to 0");
                }

                bytesWritten = 0;
            }

            super.start();
        }

        /**
         * Add a message to the outgoing queue.
         * 
         * @param buff
         * @return boolean (true means success)
         */
        protected boolean enqueue(WsByteBuffer buff) {
            if (WorkerState.RUNNING != this.workerState) {
                buff.release();
                return false;
            }
            synchronized (this.lock) {
                this.queue.add(buff);
                this.lock.notify();
            }
            return true;
        }

        /**
         * Notify this worker thread that a shutdown is in progress and to not
         * accept new requests and to stop once the last data is flushed. This
         * method will return when the worker thread has stopped.
         * 
         */
        protected void triggerStop() {
            if (WorkerState.RUNNING != this.workerState) {
                return;
            }
            this.workerState = WorkerState.STOPPING;
            // flush whatever might be on the queue
            synchronized (this.lock) {
                this.lock.notify();
            }
            try {
                // wait until that flush stops, verify we haven't already stopped
                if (WorkerState.STOPPING == this.workerState) {
                    synchronized (this.stopLock) {
                        if (WorkerState.STOPPING == this.workerState) {
                            this.stopLock.wait(TIMEOUT);
                        }
                    }
                }
            } catch (InterruptedException ie) {
                FFDCFilter.processException(ie, getClass().getName() + ".triggerStop", "201", this);
            }
            synchronized (this.lock) {
                // free all the buffers left on the queue
                while (!this.queue.isEmpty()) {
                    this.queue.removeFirst().release();
                }
            }
            // cleanup any backup related items
            if (null != this.backups) {
                this.myFormat = null;
                this.backups = null;
            }
        }

        /**
         * Rename the source file into the target file, deleting the target if
         * necessary.
         * 
         * @param source
         * @param target
         */
        private void renameFile(File source, File target) {
            if (!source.exists()) {
                // don't do anything if the source file doesn't exist
                return;
            }
            if (target.exists()) {
                target.delete();
            }
            boolean rc = source.renameTo(target);
            if (!rc) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, getFileName() + ": Unable to rename " + source + " to " + target);
                }
            }
        }

        /**
         * Move the current logfile to a backup name, taking care of any existing
         * backup files according to the configured limit.
         */
        private void addBackup() {
            // move the current log file to the newly formatted backup name
            String newname = this.fileinfo + this.myFormat.format(new Date(HttpDispatcher.getApproxTime())) + this.extensioninfo;
            File newFile = new File(newname);
            renameFile(getFile(), newFile);
            // now see if we need to delete an existing backup to make room
            while (this.backups.size() >= getMaximumBackupFiles()) {
                File oldest = this.backups.removeLast();
                if (null != oldest && oldest.exists()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, getFileName() + ": Purging oldest backup-> " + oldest.getName());
                    }
                    oldest.delete();
                }
            }
            this.backups.addFirst(newFile);
        }

        /**
         * When the output file has reached it's maximum size, this code will
         * rotate the current log to a backup and get ready to start logging
         * with a new file.
         * 
         */
        private void rotate() {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, getFileName() + ": Rotating output log");
            }

            bytesWritten = 0;

            // safe to close the file now
            try {
                getChannel().close();
            } catch (IOException ioe) {
                FFDCFilter.processException(ioe, getClass().getName() + ".rotate", "547", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, getFileName() + ": Failed to close the output file; " + ioe);
                }
            }
            try {
                if (0 < getMaximumBackupFiles()) {
                    // add the new backup file to the stored list
                    addBackup();
                }
                setChannel(createFileOutputStream().getChannel());
            } catch (Throwable t) {
                FFDCFilter.processException(t, getClass().getName() + ".rotate", "564", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, getFileName() + ": error in rotate; " + t);
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Completed rotation : " + getFileName());
            }
        }

        /**
         * Query whether the input amount will push the log file over the maximum
         * allowed size.
         * 
         * @param addition
         * @return boolean
         */
        private boolean isOverFileLimit(int addition) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "isOverFileLimit, " + getMaximumSize());
            }
            if (LogFile.UNLIMITED == getMaximumSize()) {
                return false;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "bytesWritten : " + bytesWritten + ", addition : " + addition);
            }
            // Rather than always check the file's length, lets do a best guess
            if (bytesWritten + addition < getMaximumSize()) {
                return false;
            }

            long newlen = bytesWritten + addition;
            return (newlen > getMaximumSize() || 0 > newlen);
        }

        /**
         * Method that actually handles writing data to the file.
         * 
         * @param data
         */
        private void logData(WsByteBuffer data) {
            int length = data.remaining();
            if (isOverFileLimit(length)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "logData, rotate");
                }
                rotate();
            }

            int written = 0;
            try {

                ByteBuffer buffer = data.getWrappedByteBuffer();
                while (written < length) {
                    written += getChannel().write(buffer);
                }
            } catch (IOException ioe) {
                FFDCFilter.processException(ioe, getClass().getName() + ".logData", "235", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, getFileName() + ": error writing to log; " + ioe);
                }
            } finally {
                this.bytesWritten += written;
                data.release();
            }
        }

        /**
         * Run method that will watch for data on the outgoing queue and pull
         * that off to save to the file. This runs in the background so the
         * main logger thread does no disk IO itself, but merely queues log
         * items here and returns back as fast as possible.
         */
        @Override
        public void run() {

            LinkedList<WsByteBuffer> workList = new LinkedList<WsByteBuffer>();
            LinkedList<WsByteBuffer> tmpList;
            while (true) {

                // pull all the data off of the parent queue and on to our local
                // work list
                if (!this.queue.isEmpty()) {
                    synchronized (this.lock) {
                        // swap the last work list that is now empty
                        // and the newly filled queue list
                        tmpList = workList;
                        workList = this.queue;
                        this.queue = tmpList;
                    }
                    // now work through each outgoing item on the list
                    while (!workList.isEmpty()) {
                        try {
                            logData(workList.removeFirst());
                        } catch (Throwable t) {
                            FFDCFilter.processException(t, getClass().getName() + ".run", "588", this);
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, getFileName() + ": Unexpected exception in logData; " + t);
                            }
                        }
                    }
                }
                // if we're in the process of stopping then exit the while loop
                if (WorkerState.RUNNING != this.workerState) {
                    break;
                }
                // wait until somebody tells us we have work to do
                if (this.queue.isEmpty()) {
                    try {
                        synchronized (this.lock) {
                            // if there's already work on the queue, loop back around
                            if (!this.queue.isEmpty()) {
                                continue;
                            }
                            this.lock.wait(TIMEOUT);
                        }
                    } catch (InterruptedException ie) {
                        FFDCFilter.processException(ie, getClass().getName() + ".run", "278", this);
                    }
                }
            } // end of while

            // tell the triggerStop method to exit now
            this.workerState = WorkerState.STOPPED;
            synchronized (this.stopLock) {
                this.stopLock.notify();
            }
        }

        /**
         * @see java.lang.Thread#toString()
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(super.toString());
            sb.append("\n    Current file size: ");
            sb.append(getFile().length());
            sb.append("\n    Number of backups: ");
            sb.append(((null != this.backups) ? this.backups.size() : 0));
            sb.append("\n    State: ");
            sb.append(this.workerState);
            return sb.toString();
        }
    }
}
