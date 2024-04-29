/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer61.session.impl;

import java.util.logging.Level;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.session.SessionContext;
import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.ws.webcontainer.facade.IFacade;
import com.ibm.wsspi.session.ISession;

import io.openliberty.session61.http.impl.HttpSessionFacadeImpl61;
import io.openliberty.session61.http.impl.HttpSessionImpl61;
import jakarta.servlet.ServletContext;

public class WCHttpSessionImpl61 extends HttpSessionImpl61 implements IFacade {
    private static final String CLASS_NAME = WCHttpSessionImpl61.class.getName();
    private final HttpSessionFacadeImpl61 _httpSessionFacade;

    public WCHttpSessionImpl61(ISession session, SessionContext sessCtx, ServletContext servCtx) {
        super(session, sessCtx, servCtx);
        _httpSessionFacade = returnFacade();

        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINER, CLASS_NAME + " constructor , _httpSessionFacade [" + _httpSessionFacade + "]  this -> " + this);
        }
    }

    protected HttpSessionFacadeImpl61 returnFacade() {
        return new HttpSessionFacadeImpl61(this);
    }

    /*
     * To get the facade given out to the application
     *
     * @see com.ibm.ws.webcontainer.facade.IFacade#getFacade()
     */
    @Override
    public Object getFacade() {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINER, CLASS_NAME + " getFacade , returns [" + _httpSessionFacade + "]");
        }
        return _httpSessionFacade;
    }
}
