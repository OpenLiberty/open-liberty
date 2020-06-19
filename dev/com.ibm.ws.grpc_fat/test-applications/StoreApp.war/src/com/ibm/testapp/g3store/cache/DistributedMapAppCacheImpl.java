/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.testapp.g3store.cache;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.test.g3store.grpc.RetailApp;
import com.ibm.websphere.cache.DistributedMap;
import com.ibm.websphere.cache.EntryInfo;

/**
 * @author anupag
 *
 */
public class DistributedMapAppCacheImpl implements AppCache {

    private static Logger log = Logger.getLogger(DistributedMapAppCacheImpl.class.getName());

    private DistributedMap distMap;

    DistributedMapAppCacheImpl() {
        if (distMap == null) {
            // only lookup if it is not there
            distMap = initDistributedMap();
            if (log.isLoggable(Level.FINEST)) {
                log.finest("DistributedMapAppCacheImpl: map object , this is only once::: " + distMap);
            }
        }
    }

    private final DistributedMap initDistributedMap() {
        try {
            InitialContext ic = new InitialContext();
            distMap = (DistributedMap) ic.lookup("gRPCDistMap");
            if (log.isLoggable(Level.FINEST)) {
                log.finest("initDistributedMap = " + distMap);
            }
            this.setDSMap(distMap);

        } catch (NamingException e) {
            log.log(Level.SEVERE, "DISTMAP_INIT_FAILURE", e);
        }
        return distMap;
    }

    private DistributedMap getCacheMap() {
        return distMap;
    }

    private void setDSMap(DistributedMap distSmsSessionMap) {
        this.distMap = distSmsSessionMap;
    }

    @Override
    public RetailApp getEntryValue(String key) {
        return (RetailApp) getCacheMap().get(key);
    }

    @Override
    public boolean setEntryValue(String key, RetailApp value, int expiry) {

        boolean isSetValueSuccess = false;

        if (distMap != null) {
            distMap.put(key, value, 1, expiry, EntryInfo.NOT_SHARED, new String[] { "dependency id" });
            isSetValueSuccess = true;
        }
        return isSetValueSuccess;
    }

    @Override
    public Object removeEntryValue(String key) {
        Object removedObjectValue = getCacheMap().remove(key);
        return removedObjectValue;
    }

    @Override
    public Set<String> getAllKeys() {
        if (distMap != null) {
            return distMap.keySet();
        }
        return null;
    }

}
