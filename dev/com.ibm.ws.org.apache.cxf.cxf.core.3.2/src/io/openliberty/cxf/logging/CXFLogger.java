/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.cxf.logging;

import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.ibm.ws.logging.internal.WsLogger;

/**
 * CXFLogger is inherited from the WsLogger class to preserve same behavior
 * except the one that we aimed to change. Which is to add class name to log records.
 * The whole design is aimed to support the addition of class name to the logRecord 
 * in public void log(LogRecord logRecord) method in line logRecord.setSourceClassName(getClassName());
 */
public class CXFLogger extends WsLogger {

    private String className = "";

    /**
     * @param name
     * @param resourceBundleName
     * @param c
     */
    public CXFLogger(String name, String resourceBundleName, Class<?> c) {
        super(name, c, resourceBundleName);
        setClassName(c.getName());
    }

    /**
     * @return the className
     */
    public String getClassName() {
        return this.className;
    }

    /**
     * @param className the className to set
     */
    public void setClassName(String clsName) {
        this.className = clsName;
    }

    /*
     * @see java.util.logging.Logger#log(java.util.logging.LogRecord)
     */
    @Override
    public void log(LogRecord logRecord) {
        if (logRecord == null) {
            return;
        }
        // The whole point this task is to 
        // add the class name to the logRecord
        logRecord.setSourceClassName(getClassName());
        super.log(logRecord);
    }

    /**
     * @param loggerName
     * @param bundleName
     * @param cls
     * @return
     */
    public static CXFLogger getLogger(String loggerName, String bundleName, Class<?> cls) {
        return new CXFLogger(loggerName, bundleName, cls);
    }

    /**
     * @param loggerName
     * @param cls
     * @return
     */
    public static CXFLogger getLogger(String loggerName, Class<?> cls) {
        return getLogger(loggerName, null, cls);
    }

    /**
     * @param logger
     * @param cls
     * @return
     */
    public static Logger getLogger(Logger logger, Class<?> cls) {
        return getLogger(logger.getName(), logger.getResourceBundleName(), cls);
    }
    
    
}
