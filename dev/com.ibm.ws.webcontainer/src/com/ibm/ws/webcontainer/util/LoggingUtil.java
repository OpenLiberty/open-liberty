/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.util;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LoggingUtil {
    
    /**
     * A method that allows us to log a message with parameters as well as an exception
     * @param logger The java.util.logging.Logger that will log the message
     * @param level One of the message level identifiers, e.g. SEVERE
     * @param sourceClass name of class that issued the logging request
     * @param sourceMethod name of method that issued the logging request
     * @param msg The string message (or a key in the message catalog)
     * @param params Array of parameters to the message
     * @param thrown Throwable associated with log message.
     */
    public static void logParamsAndException(Logger l, Level lev, String methodClassName, String methodName, String message, Object[] p, Throwable t) {
        LogRecord logRecord = new LogRecord(lev, message);
        logRecord.setLoggerName(l.getName());
        logRecord.setResourceBundle(l.getResourceBundle());
        logRecord.setResourceBundleName(l.getResourceBundleName());
        logRecord.setSourceClassName(methodClassName);
        logRecord.setSourceMethodName(methodName);
        logRecord.setParameters(p);
        logRecord.setThrown(t);
        l.log(logRecord);
    }
}
