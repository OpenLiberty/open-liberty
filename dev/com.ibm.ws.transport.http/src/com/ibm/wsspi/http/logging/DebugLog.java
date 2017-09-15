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

import com.ibm.wsspi.http.channel.HttpServiceContext;

/**
 * Log used to record debug or informational messages outside of the regular
 * logging utilities.
 */
public interface DebugLog extends LogFile {

    /** List of logging levels in increasing order of verbosity */
    enum Level {
        /** Debug level of none */
        NONE,
        /** Debug level of critical messages */
        CRIT,
        /** Debug level of error, critical messages */
        ERROR,
        /** Debug level of warning, error, critical messages */
        WARN,
        /** Debug level of informational, warning, error, critical messages */
        INFO,
        /** Debug level of debug, informational, warning, error, critical messages */
        DEBUG
    }

    /**
     * Request the following message be written to the output file. This is
     * based on the input requested log level versus the currently enabled value
     * on the log.
     * 
     * @param logLevel
     * @param message
     * @param hsc
     */
    void log(Level logLevel, byte[] message, HttpServiceContext hsc);

    /**
     * Request the following message be written to the output file. This is
     * based on the input requested log level versus the currently enabled value
     * on the log.
     * 
     * @param logLevel
     * @param message
     * @param hsc
     */
    void log(Level logLevel, String message, HttpServiceContext hsc);

    /**
     * Request the following message be written to the output file. This is
     * based on the input requested log level versus the currently enabled value
     * on the log.
     * 
     * @param logLevel
     * @param message
     * @param remoteIP
     * @param remotePort
     * @param localIP
     * @param localPort
     */
    void log(Level logLevel, byte[] message, String remoteIP, String remotePort, String localIP, String localPort);

    /**
     * Request the following message be written to the output file. This is
     * based on the input requested log level versus the currently enabled value
     * on the log.
     * 
     * @param logLevel
     * @param message
     * @param remoteIP
     * @param remotePort
     * @param localIP
     * @param localPort
     */
    void log(Level logLevel, String message, String remoteIP, String remotePort, String localIP, String localPort);

    /**
     * Set the current logging level for this file to the input value. Each
     * request to log request is checked against this to determine whether it
     * will be written to the file or ignored.
     * 
     * @param logLevel
     */
    void setCurrentLevel(Level logLevel);

    /**
     * Query what logging level this file is currently configured at.
     * 
     * @return Level
     */
    Level getCurrentLevel();

    /**
     * Query whether the debug log has the input level currently enabled for
     * writing to the file.
     * 
     * @param logLevel
     * @return boolean
     */
    boolean isEnabled(Level logLevel);

}
