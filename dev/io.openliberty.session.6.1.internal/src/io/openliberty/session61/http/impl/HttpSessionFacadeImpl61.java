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

import java.util.logging.Level;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.session.AbstractHttpSessionFacade;
import com.ibm.ws.session.AbstractSessionData;
import com.ibm.ws.session.utils.LoggingUtil;

import jakarta.servlet.http.HttpSession;

public class HttpSessionFacadeImpl61 extends AbstractHttpSessionFacade {
    private static final String CLASS_NAME = HttpSessionFacadeImpl61.class.getName();
    protected transient AbstractSessionData _session = null;

    private static final long serialVersionUID = 3108339284895967670L;

    public HttpSessionFacadeImpl61(HttpSessionImpl61 data) {
        super(data);
        _session = data;

        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINER, CLASS_NAME + " Constructor ; sessionData [" + _session + "]");
        }
    }

    @Override
    public HttpSession.Accessor getAccessor() {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINER, CLASS_NAME + " getAccessor");
        }

        return _session.getAccessor();
    }
}
