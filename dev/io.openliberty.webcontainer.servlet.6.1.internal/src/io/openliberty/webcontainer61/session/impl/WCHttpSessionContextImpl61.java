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
import com.ibm.ws.session.SessionApplicationParameters;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.SessionStoreService;
import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.wsspi.session.ISession;

import io.openliberty.webcontainer60.session.impl.HttpSessionContextImpl60;
import jakarta.servlet.ServletContext;

/*
 * Rename to WCHttpSessionContextImpl61 to distinguish classes b/w WC HTTP session and Session HTTP session
 */

public class WCHttpSessionContextImpl61 extends HttpSessionContextImpl60 {
    private static final String CLASS_NAME = WCHttpSessionContextImpl61.class.getName();

    public WCHttpSessionContextImpl61(SessionManagerConfig smc, SessionApplicationParameters sap, SessionStoreService sessionStoreService) {
        super(smc, sap, sessionStoreService);

        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINER, CLASS_NAME + " Constructor , this -> " + this);

            //Delegate SessionCookieConfig (SCC) setup to parent HttpSessionContextImpl60...log the SCC here
            LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINER, CLASS_NAME + " SessionCookieConfig [" + smc.getSessionCookieConfig().toString() + "] , this -> " + this);
        }
    }

    /**
     * create chain WC.HttpSesionImpl61 > SES.HttpSessionImpl61
     */
    @Override
    public Object createSessionObject(ISession isess, ServletContext servCtx) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINER, CLASS_NAME + " createSessionObject " + this);
        }

        return new WCHttpSessionImpl61(isess, this, servCtx);
    }
}
