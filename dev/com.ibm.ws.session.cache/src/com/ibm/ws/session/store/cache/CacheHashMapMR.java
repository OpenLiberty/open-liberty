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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Iterator;

import javax.cache.Cache;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.SessionStatistics;
import com.ibm.ws.session.store.cache.serializable.SessionPropertyKey;
import com.ibm.ws.session.store.common.BackedSession;
import com.ibm.wsspi.session.IStore;

/**
 * Hash map backed by JCache, with cache entry per session property.
 * TODO most methods are not implemented
 */
public class CacheHashMapMR extends CacheHashMap {
    private static final long serialVersionUID = 1L; // not serializable, rejects writeObject

    private static final TraceComponent tc = Tr.register(CacheHashMapMR.class);

    CacheStoreService cacheStoreService;
    IStore _iStore;
    SessionManagerConfig _smc;

    public CacheHashMapMR(IStore store, SessionManagerConfig smc, CacheStoreService cacheStoreService) {
        super(store, smc, cacheStoreService);
        // We know we're running multi-row..if not writeAllProperties and not time-based writes,
        // we must keep the app data tables per thread (rather than per session)
        appDataTablesPerThread = (!_smc.writeAllProperties() && !_smc.getEnableTimeBasedWrite());
    }

    /**
     * Loads all the properties.
     * TODO: copy from DatabaseHashMapMR
     */
    protected Object getAllValues(BackedSession sess) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object getValue(String id, BackedSession s) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        String sessId = s.getId();
        Object tmp = null;

        SessionPropertyKey key = new SessionPropertyKey(sessId, id);
        byte[] sessionPropBytes = cacheStoreService.getCache(getIStore().getId()).get(key);

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, key.toString(), sessionPropBytes == null ? null : sessionPropBytes.length);

        if (sessionPropBytes == null)
            return null;

        long startTime = System.currentTimeMillis();

        if (sessionPropBytes != null && sessionPropBytes.length > 0) {
            BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(sessionPropBytes));
            try {
                try {
                    tmp = ((CacheStore) getIStore()).getLoader().loadObject(in);
                } finally {
                    in.close();
                }
            } catch (ClassNotFoundException | IOException x) {
                FFDCFilter.processException(x, getClass().getName(), "96", s);
                throw new RuntimeException(x);
            }
        }

        SessionStatistics pmiStats = getIStore().getSessionStatistics();
        if (pmiStats != null) {
            pmiStats.readTimes(sessionPropBytes == null ? 0 : sessionPropBytes.length, System.currentTimeMillis() - startTime);
        }

        return tmp;
    }

    /**
     * TODO: copy from DatabaseHashMapMR
     */
    @Override
    boolean handlePropertyHits(BackedSession d2) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see com.ibm.ws.session.store.common.BackedHashMap#removePersistedSession(java.lang.String)
     */
    @Override
    protected void removePersistedSession(String id) {
        super.removePersistedSession(id);

        Cache<SessionPropertyKey, byte[]> cache = cacheStoreService.getCache(_iStore.getId());
        for (Iterator<Cache.Entry<SessionPropertyKey, byte[]>> it = cache.iterator(); it.hasNext(); ) {
            Cache.Entry<SessionPropertyKey, byte[]> entry = it.next();
            if (entry != null && id.equals(entry.getKey().sessionId))
                it.remove();
        }
    }

    /**
     * This type of hash map is not serializable.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        throw new UnsupportedOperationException();
    }
}
