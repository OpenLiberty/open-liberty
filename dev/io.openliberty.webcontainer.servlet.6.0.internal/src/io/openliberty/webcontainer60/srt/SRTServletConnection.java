/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.webcontainer60.srt;

import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.ServletConnection;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

/*
 * since: servlet 6.0
 * https://jakarta.ee/specifications/servlet/6.0/apidocs/jakarta.servlet/jakarta/servlet/servletconnection
 */

public class SRTServletConnection implements ServletConnection {

    protected static final Logger logger = LoggerFactory.getInstance().getLogger("io.openliberty.webcontainer60.srt");
    private static final String CLASS_NAME = SRTServletConnection.class.getName();
    private String connectionID = null;

    public SRTServletConnection() {
        super();

        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "constructor", "this [" + this + "]");
        }
    }

    protected void setConnectionID(String id) {
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "setConnectionID", "this [" + this + "] , connection id [" + id + "]");
        }

        connectionID = id;
    }

    /*
     * https://jakarta.ee/specifications/servlet/6.0/apidocs/jakarta.servlet/jakarta/servlet/servletconnection#getConnectionId()
     */
    @Override
    public String getConnectionId() {
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "getConnectionId", "this [" + this + "]");
        }

        return connectionID;
    }

    /*
     * https://jakarta.ee/specifications/servlet/6.0/apidocs/jakarta.servlet/jakarta/servlet/servletconnection#getProtocol()
     */
    @Override
    public String getProtocol() {
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "getProtocol", "this [" + this + "]");
        }

        //to be implemented
        return null;
    }

    /*
     * https://jakarta.ee/specifications/servlet/6.0/apidocs/jakarta.servlet/jakarta/servlet/servletconnection#getProtocolConnectionId()
     */
    @Override
    public String getProtocolConnectionId() {
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "getProtocolConnectionId", "this [" + this + "]");
        }

        //to be implemented
        return null;
    }

    /*
     * https://jakarta.ee/specifications/servlet/6.0/apidocs/jakarta.servlet/jakarta/servlet/servletconnection#isSecure()
     */
    @Override
    public boolean isSecure() {
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "isSecure", "this [" + this + "]");
        }

        //to be implemented
        return false;
    }
}
