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
package com.ibm.ws.session;

import java.util.logging.Level;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.wsspi.session.IProtocolAdapter;
import com.ibm.wsspi.session.ISession;

public class WasHttpSessionAdapter implements IProtocolAdapter {
    /*
     * For logging the CMVC file version once.
     */
    private static boolean _loggedVersion = false;

    private ServletContext _servletCtx = null;
    private SessionContext _sessionCtx = null;

    private static final String methodClassName = "WasHttpSessionAdapter";

    /*
     * Default constructor.
     */
    public WasHttpSessionAdapter(SessionContext sessionCtx, ServletContext servletCtx) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            if (!_loggedVersion) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "", "CMVC Version 1.5 5/30/08 16:35:27");
                _loggedVersion = true;
            }
        }
        _servletCtx = servletCtx;
        _sessionCtx = sessionCtx;
    }

    /*
     * Method adapt
     * Creates a WAS http session wrapper, SessionData, around the ISession
     */
    public Object adapt(ISession isess) {

        Object adaptation = isess.getAdaptation();
        if (null == adaptation) {
            adaptation = _sessionCtx.createSessionObject(isess, _servletCtx);
            isess.setAdaptation(adaptation);
        }
        return adaptation;
    }

    public Object adaptToAppSession(ISession isess) {
        Object adaptation = isess.getAdaptation(ISession.ADAPT_TO_APPLICATION_SESSION);
        if (null == adaptation) {
            // adaptation = _sessionCtx.createAppSessionObject(isess, _servletCtx);
            adaptation = new IBMApplicationSessionImpl(isess);
            isess.setAdaptation(adaptation, ISession.ADAPT_TO_APPLICATION_SESSION);
        }
        return adaptation;

    }

    // XD methods
    // not used in ND
    public Object adapt(ISession session, Integer protocol) {
        return null;
    }

    public Object adapt(ISession session, Integer protocol, ServletRequest request, ServletContext context) {
        return null;
    }

    public Object getCorrelator(ServletRequest request, Object session) {
        return null;
    }

    public Integer getProtocol(Object session) {
        return null;
    }
    // end of XD methods
}
