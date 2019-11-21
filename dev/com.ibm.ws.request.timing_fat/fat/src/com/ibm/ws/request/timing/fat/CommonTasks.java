/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.request.timing.fat;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holding class for driving common logic across multiple test cases/drivers
 * 
 */
public class CommonTasks {

    /**
     * A simple method used to log messages to the output.txt log
     * 
     * @param logLevel
     *            The level you want the message written as. If null will default to INFO
     * 
     * @param msg
     *            The message to log.
     */
    public static void writeLogMsg(Level logLevel, String msg) {
        // default to INFO if level is null
        if (logLevel == null)
            logLevel = Level.INFO;
        // determine who called me
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        StackTraceElement element = stackTraceElements[3]; // get the latest entry after the calls to get stracktrace
        Logger bucketLogger = Logger.getLogger(element.getClassName());
        bucketLogger.logp(logLevel, element.getClassName(), element.getMethodName(), msg);
    }

    /**
     * A simple method used to log messages to the output.txt log
     * 
     * @param logLevel
     *            The level you want the message written as. If null will default to INFO
     * 
     * @param msg
     *            The message to log.
     * @param thrown
     *            Throwable to be logged
     */
    public static void writeLogMsg(Level logLevel, String msg, Throwable thrown) {
        // default to INFO if level is null
        if (logLevel == null)
            logLevel = Level.INFO;
        // determine who called me
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        StackTraceElement element = stackTraceElements[3]; // get the latest entry after the calls to get stracktrace
        Logger bucketLogger = Logger.getLogger(element.getClassName());
        bucketLogger.logp(logLevel, element.getClassName(), element.getMethodName(), msg, thrown);
    }

}
