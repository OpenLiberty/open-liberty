/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.session61.http.impl;

import java.util.function.Consumer;
import java.util.logging.Level;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.session.AbstractSessionData;
import com.ibm.ws.session.SessionContext;
import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.wsspi.session.ISession;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;

/**
 * Create instance and pass the ISession, along with others contexts, into the parent's hierarchy
 */
public class HttpSessionImpl61 extends AbstractSessionData {
    private static final String CLASS_NAME = HttpSessionImpl61.class.getName();
    private static TraceNLS nls = TraceNLS.getTraceNLS(HttpSessionImpl61.class, "io.openliberty.session.resources.SessionMessages");

    protected HttpSessionImpl61(ISession session, SessionContext sessCtx, ServletContext servCtx) {
        super(session, sessCtx, servCtx);

        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINER, CLASS_NAME + " Constructor ISession [" + session + "] ; SessionContext [" + sessCtx + "] , servletContext [" + servCtx
                                                             + "] this [" + this + "]");
        }
    }

    private static class SessionDataAccessor implements HttpSession.Accessor {
        private final String INNER_CLASS_NAME = "SessionDataAccessor";
        private final ISession iSession;
        String savedSessionId = null;

        public SessionDataAccessor(ISession session, String id) {
            iSession = session;
            savedSessionId = id;

            if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
                LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINER,
                                                    INNER_CLASS_NAME + " Constructor , session id [" + savedSessionId + "] , this [" + this + "] . ISession [" + iSession + "]");
            }
        }

        /**
         * Wrap and create a HttpSession reference which application can access via the passed in {@code sessionConsumer}
         * Update the last accessed time.
         */
        @Override
        public void access(Consumer<HttpSession> sessionConsumer) {
            if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
                LoggingUtil.SESSION_LOGGER_CORE.entering(INNER_CLASS_NAME, " access");
            }

            //Check again since app can delay b/w the getAccessor() and call into access()
            if (!iSession.isValid()) {
                if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
                    LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINER, INNER_CLASS_NAME + " access , session is invalidated, throw ISE , this [" + this + "]");
                }
                throw new IllegalStateException(nls.getString("session.is.invalid"));
            }

            // ISE if Session Id has changed
            String accessSessionId = iSession.getId();
            if (accessSessionId == null || !accessSessionId.equals(savedSessionId)) {
                if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
                    LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINER, INNER_CLASS_NAME + " access , session id [" + (accessSessionId == null ? null : accessSessionId)
                                                                     + "] has changed from previous session id [" + savedSessionId + "] , throw ISE , this [" + this + "]");
                }
                throw new IllegalStateException(nls.getString("session.id.is.invalid"));
            }

            //update last accessed time.
            iSession.updateLastAccessTime(System.currentTimeMillis());

            //adapt into HttpSession and give it back to the app.
            sessionConsumer.accept((HttpSession) iSession.getAdaptation());

            if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
                LoggingUtil.SESSION_LOGGER_CORE.exiting(INNER_CLASS_NAME,
                                                        " access , updated last accessed time , session id[" + accessSessionId + "] , this [" + this + "]");
            }
        }
    }

    @Override
    public HttpSession.Accessor getAccessor() {
        ISession iSession = super.getISession();
        if (iSession == null || !iSession.isValid()) {
            if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
                LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINER, CLASS_NAME + " getAccessor , session is invalidated, throw ISE");
            }
            throw new IllegalStateException(nls.getString("session.is.invalid"));
        }

        return new SessionDataAccessor(iSession, super.getId());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("# HttpSessionImpl61 # \n { ").append("\n _iSession=").append(getISession()).append("\n } \n");
        return sb.toString();
    }
}
