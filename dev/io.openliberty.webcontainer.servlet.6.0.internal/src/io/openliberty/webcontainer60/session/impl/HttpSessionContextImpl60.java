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
package io.openliberty.webcontainer60.session.impl;

import java.util.logging.Level;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.session.SessionApplicationParameters;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.SessionStoreService;
import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.ws.webcontainer31.session.impl.HttpSessionContext31Impl;
import com.ibm.wsspi.session.ISession;
import com.ibm.wsspi.session.SessionAffinityContext;

import io.openliberty.session.impl.SessionCookieConfigImpl60;
import io.openliberty.session.impl.http.HttpSessionImpl60;
import jakarta.servlet.ServletContext;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Since Servlet 6.0
 */

public class HttpSessionContextImpl60 extends HttpSessionContext31Impl {
    private static final String methodClassName = "HttpSessionContextImpl60";

    /**
     * @param smc
     * @param sap
     * @param sessionStoreService
     */
    public HttpSessionContextImpl60(SessionManagerConfig smc, SessionApplicationParameters sap, SessionStoreService sessionStoreService) {
        super(smc, sap, sessionStoreService);

        /*
         * Upon super() returns, SessionContext has configured all the necessary for this application.
         * If the SessionCookieConfig (SCC) has configured (i.e cookie-config presents)
         * the webapp's SCC 6.0 should have copied/replaced the SMC's SCC (i.e SMC's SCC is now updated to the SessionCookieConfigImpl60)
         */
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINE, methodClassName + " Constructor");
        }

        /*
         * If the WebAppConfiguration still does not have the SCC at this point, create SCC 6.0, copy over the SMC's SCC info then replace the SMC's SCC.
         * This only happens if there is no cookie-config in web.xml. We need SCC 6.0 instance by now in case the SCI will
         * programmatically set it during application startup
         */
        if (sap.getSessionCookieConfig() == null) {
            if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINE, methodClassName + " Constructor , WebAppConfiguration does not have SCC; create SCC 60 version and set to SMC");
            }
            SessionCookieConfig scc = smc.getSessionCookieConfig();
            smc.setClonedCookieConfig(new SessionCookieConfigImpl60(scc.getName(), scc.getDomain(), scc.getPath(), scc.getComment(), scc.getMaxAge(), scc.isHttpOnly(), scc.isSecure()));
        }
    }

    /**
     * create chain wc.HttpSesionImpl60 > SessionData60 > ses.HttpSessionImpl60
     */
    @Override
    public Object createSessionObject(ISession isess, ServletContext servCtx) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINE, methodClassName + "createSessionObject");
        }

        return new WCHttpSessionImpl60(isess, this, servCtx);
    }

    @Override
    public HttpSession generateNewId(HttpServletRequest request, HttpServletResponse response, HttpSession existingSession) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINE, methodClassName + "generateNewId");
        }

        SessionAffinityContext sac = getSessionAffinityContext(request);
        HttpSession session = (HttpSession) _coreHttpSessionManager.generateNewId(request, response, sac, ((HttpSessionImpl60) existingSession).getISession());
        return session;
    }
}
