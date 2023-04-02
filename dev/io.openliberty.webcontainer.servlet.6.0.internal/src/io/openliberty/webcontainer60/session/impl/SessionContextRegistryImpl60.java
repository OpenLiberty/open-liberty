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
import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.ws.webcontainer.httpsession.SessionManager;
import com.ibm.ws.webcontainer.session.IHttpSessionContext;
import com.ibm.ws.webcontainer31.session.impl.SessionContextRegistry31Impl;

/**
 *
 */
public class SessionContextRegistryImpl60 extends SessionContextRegistry31Impl {
    private static final String methodClassName = "SessionContextRegistryImpl60";

    /**
     * @param smgr
     */
    public SessionContextRegistryImpl60(SessionManager smgr) {
        super(smgr);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.webcontainer.session.impl.SessionContextRegistryImpl#createSessionContextObject(com.ibm.ws.session.SessionManagerConfig,
     * com.ibm.ws.session.SessionApplicationParameters)
     */
    @Override
    protected IHttpSessionContext createSessionContextObject(SessionManagerConfig smc, SessionApplicationParameters sap) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINE, methodClassName + " createSessionContextObject to create HttpSessionContextImpl60 with smc [" + smc + "]");
        }
        return new HttpSessionContextImpl60(smc, sap, this.smgr.getSessionStoreService());
    }

}
