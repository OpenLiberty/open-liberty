/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer60.session.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import com.ibm.ws.session.SessionContext;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.ws.webcontainer.session.impl.SessionAffinityManagerImpl;
import com.ibm.wsspi.session.IStore;
import com.ibm.wsspi.session.SessionAffinityContext;
import com.ibm.wsspi.webcontainer.servlet.IExtendedResponse;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.http.Cookie;

public class SessionAffinityManagerImpl60 extends SessionAffinityManagerImpl {

    private static final String methodClassName = "SessionAffinityManagerImpl60";

    public SessionAffinityManagerImpl60(SessionManagerConfig smc, SessionContext sctx, IStore istore) {
        super(smc, sctx, istore);

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodClassName, "Clone ID of this server=" + _cloneID);
        }
    }

    public void setCookie(ServletRequest request, ServletResponse response, SessionAffinityContext affinityContext, Object session) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, "setCookie");
        }

        Cookie cookie = super.cookieGenerator(request, response, affinityContext, session);

        //setAttribute any remaining
        if (cookie != null) {
            SessionCookieConfig scc = _smc.getSessionCookieConfig();

            Map<String, String> cookieAttrs = scc.getAttributes();
            if (cookieAttrs != null) {
                ArrayList<String> preDefinedAttList = new ArrayList<String>(Arrays.asList("COMMENT", "DOMAIN", "HTTPONLY", "MAX-AGE", "PATH", "SECURE")); //Exclude the predefined attributes
                String key, value;

                for (Entry<String, String> entry : cookieAttrs.entrySet()) {
                    key = entry.getKey();
                    if (!preDefinedAttList.contains(key.toUpperCase(Locale.ENGLISH))) {
                        value = entry.getValue();

                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "setCookie", "setAttribute (" + key + "," + value + ")");
                        }
                        cookie.setAttribute(key, value);
                    }
                }
            }

            ((IExtendedResponse) response).addSessionCookie(cookie);
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, "setCookie [" + cookie + "]");
        }
    }
}
