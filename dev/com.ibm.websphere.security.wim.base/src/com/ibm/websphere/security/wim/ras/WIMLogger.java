/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.wim.ras;

import java.util.logging.Logger;

/**
 * This class provides static methods to retrieve message and trace logger.
 * For convenience, this class sets the default resource bundle for message loggers.
 * This class should be used on vmm server side. This class can also be
 * used on the vmm client side, but if the client is running in non-WAS
 * environment then the log properties file must be provided for the
 * messaging and tracing to work.<p>
 * This class also defines static signature strings that can be used during
 * method entry and exit trace calls
 *
 **/
public class WIMLogger {
    /**
     * Default message resource bundle for vmm
     **/
    public final static String MESSAGE_RB = "com.ibm.websphere.wim.ras.properties.CWWIMMessages";

    /**
     * Signature string for method Entry
     **/
    public final static String ENTRY = ">>ENTRY";

    /**
     * Signature string for method Exit
     **/
    public final static String EXIT = "<<EXIT";

    /**
     * Signature string for vmm API
     **/
    public final static String API_PREFIX = "WIM_API ";

    /**
     * Signature string for vmm SPI
     **/
    public final static String SPI_PREFIX = "WIM_SPI ";

    /**
     * Return the trace logger for the specified package.
     * 
     * @param name A name for the trace logger. This should be a dot-separated
     *            name and should normally be based on the package names,
     *            such as com.ibm.webshere.wim, com.ibm.ws.wim.adapter, com.ibm.wsspi.wim
     *
     * @return a trace logger
     * @see java.util.logging.Logger
     **/
    public static Logger getTraceLogger(String name) {
        return Logger.getLogger(name);
    }

    /**
     * Return the message logger for the vmm. The vmm message
     * resource bundle is set for this logger to use.
     * 
     * @param name A name for the message logger. This should be a dot-separated
     *            name and should normally be based on the package names,
     *            such as com.ibm.webshere.wim, com.ibm.ws.wim.adapter, com.ibm.wsspi.wim
     *
     * @return a message logger
     * @see java.util.logging.Logger
     **/
    public static Logger getMessageLogger(String name) {
        return Logger.getLogger(name, MESSAGE_RB);
    }
}
