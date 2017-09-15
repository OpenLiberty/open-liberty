/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
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
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.http.logging.LogFile;

/**
 * This utility class handles writing data to a log file. The disk IO is
 * performed on the calling thread.
 * 
 * This logger can be stopped and started repeatedly; however, it cannot be
 * restarted once the destroy method has been used.
 */
public class LoggerOnThread implements LogFile {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(LoggerOnThread.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** List of possible logger states */
    private enum State {
        /** Idle stopped state, can be started from here */
        IDLE,
        /** Currently running state */
        RUNNING,
        /** Disabled state, cannot be restarted */
        DISABLED
    }

    /** Log file associated with this particular thread */
    private File myFile = null;
    /** Simple name of the file */
    private String myName = null;
    /** Full directory+name of the file */
    private String myFullName = null;
    /** File channel for this logging thread */
    private FileChannel myChannel = null;
    /** Current state of the logger */
    private State state = State.IDLE;
    /** Maximum size for the log to grow to */
    private long maxFileSize = LogFile.UNLIMITED;
    /** Maximum number of backup files to keep around */
    private int maxBackupFiles = 1;
    /** List of backup files */
    private LinkedList<File> backups = null;
    /** Path+filename information for backups */
    private String fileinfo = null;
    /** File extension information for backups */
    private String extensioninfo = null;
    /** Date formatter for backup file names */
    private SimpleDateFormat myFormat = null;

    /**
     * Constructor that opens a reference to the input file name. Note that
     * the start() method must be used before logging will actually begin.
     * 
     * @param name
     * @throws FileNotFoundException
     */
    public LoggerOnThread(String name) throws FileNotFoundException {
        this.myFullName = name;
        this.myFile = new File(name);
        this.myName = this.myFile.getName();
        this.myChannel = new FileOutputStream(this.myFile, true).getChannel();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, getFileName() + ": New logger-on-thread created");
        }
    }

    /**
     * @see LogFile#getFileName()
     */
    @Override
    public String getFileName() {
        return this.myName;
    }

    /**
     * @see LogFile#isStarted()
     */
    @Override
    public boolean isStarted() {
        return State.RUNNING == this.state;
    }

    /**
     * Log the given data to the log file. If this returns false, then the input
     * buffer has also been released, the same as if it was successfully written
     * to the logfile.
     * 
     * @param data
     * @return boolean (true means it succeeded)
     */
    public synchronized boolean log(WsByteBuffer data) {
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
        int length = data.remaining();
        if (isOverFileLimit(length)) {
            rotate();
        }
        boolean rc = true;
        int bytesWritten = 0;
        try {
            ByteBuffer buffer = data.getWrappedByteBuffer();
            while (bytesWritten < length) {
                bytesWritten += this.myChannel.write(buffer);
            }
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName() + ".log", "166", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, getFileName() + ": error writing to log; " + e);
            }
            rc = false;
        } finally {
            data.release();
        }
        return rc;
    }

    /**
     * @see LogFile#start()
     */
    @Override
    public synchronized boolean start() {
        if (State.IDLE != this.state) {
            return false;
        }
        if (0 < getMaximumBackupFiles()) {
            this.myFormat = new SimpleDateFormat("_yy.MM.dd_HH.mm.ss", Locale.US);
            this.backups = new LinkedList<File>();
            // backups would convert access.log to access_date.log, but would
            // convert http_access to just http_access_date
            int index = getFileName().lastIndexOf('.');
            if (-1 != index) {
                index += (this.myFullName.length() - this.myName.length());
                this.fileinfo = this.myFullName.substring(0, index);
                this.extensioninfo = this.myFullName.substring(index);
            } else {
                this.fileinfo = this.myFullName;
                this.extensioninfo = "";
            }
        }
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
    public synchronized boolean stop() {
        // check to see if we've already stopped
        if (State.RUNNING != this.state) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, getFileName() + ": Logger already stopped");
            }
            return true;
        }
        // cleanup any backup related items
        this.myFormat = null;
        this.backups = null;

        // set the flag that we're no longer accepting new data
        this.state = State.IDLE;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, getFileName() + ": stopped");
        }
        return true;
    }

    /**
     * @see LogFile#disable()
     */
    @Override
    public synchronized boolean disable() {
        if (State.RUNNING == this.state) {
            stop();
        } else if (State.DISABLED == this.state) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, getFileName() + ": attempt to double destroy");
            }
            return false;
        }
        // safe to close the file now
        try {
            this.myChannel.close();
        } catch (IOException ioe) {
            FFDCFilter.processException(ioe, getClass().getName() + ".disable", "124", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, getFileName() + ": Failed to close the output file; " + ioe);
            }
        }
        this.state = State.DISABLED;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, getFileName() + ": disabled");
        }
        return true;
    }

    /**
     * @see LogFile#setMaximumSize(long)
     */
    @Override
    public boolean setMaximumSize(long size) {
        if (LogFile.UNLIMITED > size) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, getFileName() + ": Invalid file size-> " + size);
            }
            return false;
        }
        if (0L == size) {
            // can't have a 0 size file as it would always be "full"
            this.maxFileSize = LogFile.UNLIMITED;
        } else {
            this.maxFileSize = size;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, getFileName() + ": Set maximum size to " + this.maxFileSize);
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
                Tr.debug(tc, getFileName() + ": Invalid negative number of backup files-> " + number);
            }
            return false;
        }
        this.maxBackupFiles = number;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, getFileName() + ": Set maximum files to " + this.maxBackupFiles);
        }
        return true;
    }

    /**
     * Query whether the input amount will push the log file over the maximum
     * allowed size.
     * 
     * @param addition
     * @return boolean
     */
    private boolean isOverFileLimit(int addition) {
        if (LogFile.UNLIMITED == getMaximumSize()) {
            return false;
        }
        // Note: to do this properly, we need to know if the admin updates the
        // file externally, i.e. 'cat /dev/null > file' to clear it out. Windows
        // JDK filechannel.size() was a buffered value (AIX was not), but the
        // file.length() call on each checked the real value each time. This is
        // a slight performance hit but it's the only 100% guarentee of being
        // accurate.
        long newlen = this.myFile.length() + addition;
        return (newlen > getMaximumSize() || 0 > newlen);
    }

    /**
     * Print this object as a string.
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(256);
        sb.append(super.toString());
        sb.append("\n  FilePathName: ");
        sb.append(this.myFullName);
        sb.append("\n  FileName: ");
        sb.append(this.myName);
        sb.append("\n  MaxFileSize: ");
        sb.append(this.maxFileSize);
        sb.append("\n  CurrentFileSize: ");
        sb.append(this.myFile.length());
        sb.append("\n  MaxBackupFiles: ");
        sb.append(this.maxBackupFiles);
        sb.append("\n  Number of backups: ");
        sb.append(((null != this.backups) ? this.backups.size() : 0));
        sb.append("\n  State: ");
        sb.append(this.state);
        return sb.toString();
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
        renameFile(this.myFile, newFile);
        // now see if we need to delete an existing backup to make room
        if (this.backups.size() == getMaximumBackupFiles()) {
            File oldest = this.backups.removeLast();
            if (null != oldest && oldest.exists()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, getFileName() + ": Purging oldest backup-> " + oldest.getName());
                }
                oldest.delete();
            }
        }
        this.backups.addFirst(newFile);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, getFileName() + ": number of backup files-> " + this.backups.size());
        }
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
        try {
            this.myChannel.close();
        } catch (IOException ioe) {
            FFDCFilter.processException(ioe, getClass().getName() + ".rotate", "424", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, getFileName() + ": Failed to close the output file; " + ioe);
            }
        }
        try {
            if (0 < getMaximumBackupFiles()) {
                // add the new backup file to the stored list
                addBackup();
            }
            this.myChannel = new FileOutputStream(this.myFile, true).getChannel();
        } catch (SecurityException se) {
            FFDCFilter.processException(se, getClass().getName() + ".rotate", "436", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, getFileName() + ": security error in rotate; " + se);
            }
        } catch (Throwable t) {
            FFDCFilter.processException(t, getClass().getName() + ".rotate", "441", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, getFileName() + ": error in rotate; " + t);
            }
        }
    }
}
