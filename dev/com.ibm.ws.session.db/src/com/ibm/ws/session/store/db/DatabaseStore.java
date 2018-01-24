/*******************************************************************************
 * Copyright (c) 1997, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.session.store.db;

import java.util.Vector;
import java.util.logging.Level;

import javax.servlet.ServletContext;

import com.ibm.ws.session.MemoryStoreHelper;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.store.common.BackedHashMap;
import com.ibm.ws.session.store.common.BackedSession;
import com.ibm.ws.session.store.common.BackedStore;

public class DatabaseStore extends BackedStore {

    //----------------------------------------
    // Private members.
    //----------------------------------------

    /*
     * For logging the CMVC file version once.
     */
    private static boolean _loggedVersion = false;
    private static final String methodClassName = "DatabaseStore";

    private static final int REMOTE_INVALIDATE = 0;
    private static final String methodNames[] = { "remoteInvalidate" };

    //----------------------------------------
    // Public constructor
    //----------------------------------------

    public DatabaseStore(SessionManagerConfig smc, String storeId, ServletContext sc, MemoryStoreHelper storeHelper, DatabaseStoreService databaseStoreService) {
        super(smc, storeId, sc, storeHelper, databaseStoreService);
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            if (!_loggedVersion) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, "", "CMVC Version 1.5 3/12/08 09:22:18");
                _loggedVersion = true;
            }
        }
        if (_smc.isUsingMultirow()) {
            _sessions = new DatabaseHashMapMR(this, smc, databaseStoreService);
        } else {
            _sessions = new DatabaseHashMap(this, smc, databaseStoreService);
        }
    }

    public DatabaseStore(SessionManagerConfig smc, String storeId, ServletContext sc, MemoryStoreHelper storeHelper, boolean isApplicationSessionStore, DatabaseStoreService databaseStoreService) {
        this(smc, storeId, sc, storeHelper, databaseStoreService);
        _isApplicationSessionStore = isApplicationSessionStore;
        ((BackedHashMap) _sessions).setIsApplicationSessionHashMap(isApplicationSessionStore);
    }

    //----------------------------------------
    // Public Methods
    //----------------------------------------

    // Set max inactive time to 0 in database so invalidator will will do inval
    // Then remove the session from cache
    public void remoteInvalidate(String sessionId, boolean backendUpdate) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[REMOTE_INVALIDATE], "for app " + getId() + " id " + sessionId + " backendUpdate " + backendUpdate);
        }

        super.remoteInvalidate(sessionId, backendUpdate);

        if (backendUpdate) {
            ((DatabaseHashMap) _sessions).setMaxInactToZero(sessionId, getId());
        }

        // now clean this session out of cache -- we do this even if not doing db inval
        Vector v = new Vector(1);
        v.add(sessionId);
        ((BackedHashMap) _sessions).handleDiscardedCacheItems(v.elements());

        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[REMOTE_INVALIDATE], "for app " + getId() + " id " + sessionId);
        }
    }

    /*
     * @see com.ibm.ws.session.store.common.BackedStore#createSessionObject(java.lang.String)
     */
    public BackedSession createSessionObject(String sessionId) {
        return new DatabaseSession((DatabaseHashMap) _sessions, sessionId, _storeCallback);
    }

}
