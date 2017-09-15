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
package com.ibm.wsspi.http.logging;

/**
 * Generic log file representation.
 * 
 */
public interface LogFile {

    /** Unlimited file size setting */
    int UNLIMITED = -1;
    /** CRLF representation */
    byte[] CRLF = new byte[] { '\r', '\n' };

    /**
     * Query the configured file name.
     * 
     * @return String
     */
    String getFileName();

    /**
     * Start the log file. The log may be started and stopped repeatedly, but
     * cannot be restarted once disabled.
     * 
     * @return boolean, returns false if fails to start, such as if not configured
     */
    boolean start();

    /**
     * Stop the log file. The log may be started and stopped repeatedly.
     * 
     * @return boolean, returns false if failed to stop, ie not started
     */
    boolean stop();

    /**
     * Stop and disable the log from restarting.
     * 
     * @return boolean, returns false if failed to disable (ie not configured)
     */
    boolean disable();

    /**
     * Query whether this log file is started yet.
     * 
     * @return boolean
     */
    boolean isStarted();

    /**
     * Set the maximum allowed size this log can grow to before it wraps.
     * 
     * @param size
     * @return boolean, returns false if the setting fails (ie negative)
     */
    boolean setMaximumSize(long size);

    /**
     * Query the current maximum file size allowed for this log.
     * 
     * @return long
     */
    long getMaximumSize();

    /**
     * Set the maximum number of backup files to keep when wrapping. This does
     * not take effect if the maximum size is unlimited.
     * 
     * @param number
     * @return boolean, returns false if setting fails (ie negative)
     */
    boolean setMaximumBackupFiles(int number);

    /**
     * Query the current setting for the backup files to keep when wrapping.
     * 
     * @return int
     */
    int getMaximumBackupFiles();

}
