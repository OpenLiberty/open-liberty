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
import com.ibm.ws.session.SessionContext;
import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.ws.webcontainer.facade.IFacade;
import com.ibm.wsspi.session.ISession;

import io.openliberty.session.impl.HttpSessionFacade60;
import io.openliberty.session.impl.http.HttpSessionImpl60;
import jakarta.servlet.ServletContext;

/*
 * Since Servlet 6.0
 */

public class WCHttpSessionImpl60 extends HttpSessionImpl60 implements IFacade {

    private final HttpSessionFacade60 _httpSessionFacade;

    public WCHttpSessionImpl60(ISession session, SessionContext sessCtx, ServletContext servCtx) {
        super(session, sessCtx, servCtx);
        _httpSessionFacade = returnFacade();

        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINE, "WCHttpSessionImpl60 Constructor");
        }
    }

    protected HttpSessionFacade60 returnFacade() {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINE, "WCHttpSessionImpl60 returnFacade HttpSessionFacade60");
        }

        return new HttpSessionFacade60(this);
    }

    /*
     * To get the facade given out to the application
     *
     * @see com.ibm.ws.webcontainer.facade.IFacade#getFacade()
     */
    @Override
    public Object getFacade() {
        return _httpSessionFacade;
    }

}
