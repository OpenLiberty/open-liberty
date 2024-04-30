/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.apache.cxf.common.logging;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.logging.internal.WsLogger;

/**
 *  CXFLogger is inherited from the WsLogger class to preserve same behavior
 *  except the one that we aimed to change. Which is to add class name to log records
 */
public class CXFLogger extends WsLogger {

    private String className="";
    
    /**
     * @param name
     * @param c
     * @param resourceBundleName
     */
    public CXFLogger(String name, String resourceBundleName, Class<?> c) {
        super(name, c, resourceBundleName);
        setClassName(c.getCanonicalName());
    }
    
    /**
     * This method is not aimed to use since due to trace injection 
     * it creates significant amount of excessive logs
     * this.className will be used instead
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

    @Trivial
    @Override
    public void log(Level level, String msg) {
        logp(level,this.className,"",msg);
    }

    @Trivial
    @Override
    public void severe(String msg) {
        logp(Level.SEVERE,this.className,"",msg);
    }

    @Trivial
    @Override
    public void warning(String msg) {
        logp(Level.WARNING,this.className,"",msg);
    }

    @Trivial
    @Override
    public void info(String msg) {
        logp(Level.INFO,this.className,"",msg);
    }

    @Trivial
    @Override
    public void fine(String msg) {
        logp(Level.FINE,this.className,"",msg);
    }

    @Trivial
    @Override
    public void finer(String msg) {
        logp(Level.FINER,this.className,"",msg);
    }

    @Trivial
    @Override
    public void finest(String msg) {
        logp(Level.FINEST,this.className,"",msg);
    }
    /**
     * @param logger
     * @param cls
     * @return
     */
    public static CXFLogger getLogger(String loggerName, String bundleName, Class<?> cls) {
        CXFLogger logger = (CXFLogger) CXFLogger.getLogger(loggerName, bundleName);
        logger.setClassName(cls.getCanonicalName());
        return logger;
    }
    /**
     * @param loggerName
     * @param cls
     * @return
     */
    public static CXFLogger getLogger(String loggerName, Class<?> cls) {
        CXFLogger logger = (CXFLogger) getLogger(loggerName);
        logger.setClassName(cls.getCanonicalName());
        return logger;
    }
}
