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

package com.ibm.ws.session.http;

import java.util.logging.Level;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.wsspi.session.IProtocolAdapter;
import com.ibm.wsspi.session.ISession;

public class HttpSessionAdapter implements IProtocolAdapter {

    // ----------------------------------------
    // Public methods
    // ----------------------------------------
    /*
     * For logging.
     */
    private static final String methodClassName = "HttpSessionAdapter";

    /*
     * For logging the CMVC file version once.
     */
    private static boolean _loggedVersion = false;

    /**
     * Default constructor.
     */
    public HttpSessionAdapter() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            if (!_loggedVersion) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "", "CMVC Version 1.5 5/30/08 16:34:51");
                _loggedVersion = true;
            }
        }
    }

    /**
     * Method adapt
     * <p>
     * This method adapts a protocol agnostic session into one that conforms to a protocol such as HTTP.
     * 
     * @see com.ibm.wsspi.session.IProtocolAdapter#adapt(com.ibm.wsspi.session.ISession, Integer)
     */
    public Object adapt(ISession session) {

        Object adaptation = session.getAdaptation();
        if (null == adaptation) {
            adaptation = new HttpSessionImpl(session);
            session.setAdaptation(adaptation);
        }
        return adaptation;
    }

    // not supported outside of WAS ... return the same as adapt
    public Object adaptToAppSession(ISession session) {
        return adapt(session);
    }

    // XD methods
    public Object adapt(ISession session, Integer protocol) {
        Object adaptation = session.getAdaptation(protocol);
        if (null == adaptation) {
            adaptation = new HttpSessionImpl(session);
            session.setAdaptation(adaptation, protocol);
        }
        return adaptation;
    }

    public Object adapt(ISession session, Integer protocol, ServletRequest request, ServletContext context) {
        return adapt(session, protocol);
    }

    public Object getCorrelator(ServletRequest request, Object session) {
        return null;
    }

    public Integer getProtocol(Object session) {
        return IProtocolAdapter.HTTP;
    }
    // end of XD methods

}