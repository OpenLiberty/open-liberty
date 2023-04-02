/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.webcontainer60.srt;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

import jakarta.servlet.ServletConnection;

/*
 * since: servlet 6.0
 * https://jakarta.ee/specifications/servlet/6.0/apidocs/jakarta.servlet/jakarta/servlet/servletconnection
 */

public class SRTServletConnection implements ServletConnection {

    protected static final Logger logger = LoggerFactory.getInstance().getLogger("io.openliberty.webcontainer60.srt");
    private static final String CLASS_NAME = SRTServletConnection.class.getName();
    private String connectionID = null;
    private String protocol = null;
    private boolean isSSL = false;

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

    protected void setConnectionSecure(boolean secure) {
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "setConnectionSecure", "this [" + this + "] , secure [" + secure + "]");
        }

        isSSL = secure;
    }

    protected void setProtocol(String prot) {
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "setProtocol", "this [" + this + "] , protocol [" + prot + "]");
        }

        protocol = prot;
    }

    /*
     * https://jakarta.ee/specifications/servlet/6.0/apidocs/jakarta.servlet/jakarta/servlet/servletconnection#getConnectionId()
     */
    @Override
    public String getConnectionId() {
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "getConnectionId", "this [" + this + "] , connection id [" + connectionID + "]");
        }

        return connectionID;
    }

    /*
     * https://jakarta.ee/specifications/servlet/6.0/apidocs/jakarta.servlet/jakarta/servlet/servletconnection#getProtocol()
     */
    @Override
    public String getProtocol() {
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "getProtocol", "this [" + this + "] , protocol [" + protocol + "]");
        }

        if (protocol == null || protocol.isBlank())
            return "unknown";

        return protocol;
    }

    /*
     * https://jakarta.ee/specifications/servlet/6.0/apidocs/jakarta.servlet/jakarta/servlet/servletconnection#getProtocolConnectionId()
     */
    @Override
    public String getProtocolConnectionId() {
        //Return empty string for most protocol connectionID. HTTP 3 is the only one should have an ID; however it is not supported yet
        //Currently, this method always return an empty string
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "getProtocolConnectionId", "this [" + this + "] , return empty string");
        }

        return "";
    }

    /*
     * https://jakarta.ee/specifications/servlet/6.0/apidocs/jakarta.servlet/jakarta/servlet/servletconnection#isSecure()
     */
    @Override
    public boolean isSecure() {
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "isSecure", "this [" + this + "] , isSecure [" + isSSL + "]");
        }

        return isSSL;
    }
}
