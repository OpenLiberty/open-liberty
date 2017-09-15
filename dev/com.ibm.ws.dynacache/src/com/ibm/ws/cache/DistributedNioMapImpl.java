/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.cache.ChangeListener;
import com.ibm.websphere.cache.DistributedNioMap;
import com.ibm.websphere.cache.InvalidationListener;
import com.ibm.websphere.cache.PreInvalidationListener;
import com.ibm.websphere.cache.exception.DynamicCacheException;
import com.ibm.ws.cache.intf.DCache;

public class DistributedNioMapImpl extends DistributedObjectCacheAdapter implements DistributedNioMap {

    private   static TraceComponent     tc                  = Tr.register(DistributedNioMapImpl.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
	

    protected void createMapSpecificObjects(){
        super.entryInfoPool  = EntryInfo.createEntryInfoPool(super.cache.getCacheName(),50);
        super.cacheEntryPool = CacheEntry.createCacheEntryPool(super.cache, 50);
    }
    protected void destroyMapSpecificObjects(){
        // what to do???
    }
	
    public DistributedNioMapImpl(DCache cache) {
        super(cache, TYPE_DISTRIBUTED_NIO_MAP);
        
        if (tc.isDebugEnabled())
            Tr.debug(tc, "DistributedNioMapImpl() CTOR "+this );
	}

    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
	public com.ibm.websphere.cache.CacheEntry getCacheEntry(Object key) {
        return super.common_getCacheEntry(key);
	}

    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
	public void put(Object key, Object value, Object userMetaData, int priority, int timeToLive, int sharingPolicy, Object dependencyIds[], Object alias[]) {
        super.common_put(key, value, userMetaData, priority, timeToLive, sharingPolicy, dependencyIds, alias );
	}

    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
	public void put(Object key, Object value, Object userMetaData, int priority, int timeToLive, int inactivityTime, int sharingPolicy, Object dependencyIds[], Object alias[]) {
        super.common_put(key, value, userMetaData, priority, timeToLive, inactivityTime, sharingPolicy, dependencyIds, alias );
	}

    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
    public void put(Object key, Object value, Object userMetaData, int priority, int timeToLive, int inactivityTime, int sharingPolicy, Object dependencyIds[], Object alias[], boolean skipMemoryAndWriteToDisk) throws DynamicCacheException {
        super.common_put(key, value, userMetaData, priority, timeToLive, inactivityTime, sharingPolicy, dependencyIds, alias, skipMemoryAndWriteToDisk );
    }

    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
	public com.ibm.websphere.cache.CacheEntry putAndGet(Object key, Object value, Object userMetaData, int priority, int timeToLive, int sharingPolicy, Object dependencyIds[], Object alias[]) {
        return super.common_putAndGet(key, value, userMetaData, priority, timeToLive, sharingPolicy, dependencyIds, alias );
	}

     // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
	public com.ibm.websphere.cache.CacheEntry putAndGet(Object key, Object value, Object userMetaData, int priority, int timeToLive, int inactivityTime, int sharingPolicy, Object dependencyIds[], Object alias[]) {
        return super.common_putAndGet(key, value, userMetaData, priority, timeToLive, inactivityTime, sharingPolicy, dependencyIds, alias );
	}
	
    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
    public void invalidate(Object key, boolean wait, boolean checkPreInvalidationListener) {
        super.common_invalidate(key, wait, checkPreInvalidationListener);
    }

    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
	public void invalidate(Object key, boolean wait) {
		super.common_invalidate(key, wait);
	}

    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
	public void invalidate(Object key) {
		super.common_invalidate(key);
	}

    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
	public void addAlias(Object key, Object[] aliasArray) {
        super.common_addAlias(key, aliasArray );
	}

    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
	public void removeAlias(Object alias) {
		super.common_removeAlias(alias);
	}

	/**
	 * For now, uses this to release LRU cache entry (regular objects or ByteByffers/MetaData)
	 * @param numOfEntries the number of cache entries to be released
	 */
	public void releaseLruEntries(int numOfEntries) {
        super.common_releaseLruEntries(numOfEntries);
	}

    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
    public int size(boolean includeDiskCache) {
        return super.size(includeDiskCache);
    }

    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
    public boolean isEmpty(boolean includeDiskCache) {
        return super.isEmpty(includeDiskCache);
    }

    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
    public boolean containsKey(Object key, boolean includeDiskCache) {
        return super.containsKey(key, includeDiskCache);
    }

    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
    public boolean enableListener(boolean enable) {
        return super.common_enableListener(enable);
    }

    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
    public boolean addInvalidationListener(InvalidationListener listener) {
        return super.common_addInvalidationListener(listener);
    }

    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
    public boolean removeInvalidationListener(InvalidationListener listener) {
        return super.common_removeInvalidationListener(listener);
    }
    
    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
    @Override
    public boolean addPreInvalidationListener(PreInvalidationListener listener) {
        return super.common_addPreInvalidationListener(listener);
    }

    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
    public boolean removePreInvalidationListener(PreInvalidationListener listener) {
        return super.common_removePreInvalidationListener(listener);
    }    

    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
    public boolean addChangeListener(ChangeListener listener) {
        return super.common_addChangeListener(listener);
    }    
    
//  -----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
    public boolean removeChangeListener(ChangeListener listener) {
        return super.common_removeChangeListener(listener);
    }    
}
