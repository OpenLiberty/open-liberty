/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.session.store.cache;

import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.ServletContext;

import com.ibm.ws.session.MemoryStoreHelper;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.store.common.BackedHashMap;
import com.ibm.ws.session.store.common.BackedSession;
import com.ibm.ws.session.store.common.BackedStore;

/**
 * HTTP session store via JCache.
 */
public class CacheStore extends BackedStore {

    public CacheStore(SessionManagerConfig smc, String storeId, ServletContext sc, MemoryStoreHelper storeHelper, boolean isApplicationSessionStore, CacheStoreService cacheStoreService) {
        super(smc, storeId, sc, storeHelper, cacheStoreService);

        _sessions = new CacheHashMap(this, smc, cacheStoreService);

        _isApplicationSessionStore = isApplicationSessionStore;
        ((BackedHashMap) _sessions).setIsApplicationSessionHashMap(isApplicationSessionStore);
    }

    /**
     * @see com.ibm.ws.session.store.common.BackedStore#createSessionObject(java.lang.String)
     */
    @Override
    public BackedSession createSessionObject(String sessionId) {
        return new CacheSession((CacheHashMap) _sessions, sessionId, _storeCallback);
    }

    // Copied from DatabaseStore.
    // Set max inactive time to 0 in database so invalidator will will do inval
    // Then remove the session from cache
    @Override
    public void remoteInvalidate(String sessionId, boolean backendUpdate) {
        super.remoteInvalidate(sessionId, backendUpdate);

        if (backendUpdate) {
            ((CacheHashMap) _sessions).setMaxInactToZero(sessionId, getId());
        }

        // now clean this session out of cache -- we do this even if not doing db inval
        Enumeration<String> e = Collections.enumeration(Collections.singleton(sessionId));
        ((BackedHashMap) _sessions).handleDiscardedCacheItems(e);
    }
}
