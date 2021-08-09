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

import java.util.Collection;
import java.util.Set;

import com.ibm.websphere.cache.ChangeListener;
import com.ibm.websphere.cache.DistributedObjectCache;
import com.ibm.websphere.cache.DistributedMap;
import com.ibm.websphere.cache.InvalidationListener;
import com.ibm.ws.cache.intf.DCache;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class DistributedMapImpl extends DistributedObjectCacheAdapter implements DistributedMap {

    private static TraceComponent   tc                      = Tr.register(DistributedMapImpl.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    protected void createMapSpecificObjects(){
        super.entryInfoPool  = EntryInfo.createEntryInfoPool(super.cache.getCacheName(),50);
    }
    protected void destroyMapSpecificObjects(){
        // what to do???
    }

    public DistributedMapImpl(DCache cache) {
        super(cache, DistributedObjectCache.TYPE_DISTRIBUTED_MAP );

        if (tc.isDebugEnabled())
            Tr.debug(tc, "DistributedMapImpl() CTOR "+this );
    }

    //------------------------------------------
    // Use adapter code for the following:
    //------------------------------------------
    // public int getMapType()
    // public void setSharingPolicy(int sharingPolicy)
    // public int getSharingPolicy()
    // public int size()
    // public boolean isEmpty()
    // public int hashCode()
    // public boolean equals(Object o)
    // public void clear()
    // public void putAll(Map t)
    // public boolean containsKey(Object key)
    // public boolean containsValue(Object value)
    //------------------------------------------

    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
    public Object get(Object key) {
        return super.common_get(key);
    }

    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
    public Object put(Object key, Object value) {   
        return super.common_put(key, value);
    }

    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
    public Object put(Object key, Object value, int priority, int timeToLive, int sharingPolicy, Object dependencyIds[]) {
        return super.common_put(key, value, priority, timeToLive, sharingPolicy, dependencyIds);
    }

    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
    public Object put(Object key, Object value, int priority, int timeToLive, int inactivityTime, int sharingPolicy, Object dependencyIds[]) {
        return super.common_put(key, value, priority, timeToLive, inactivityTime, sharingPolicy, dependencyIds);
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
    public void invalidate(Object key, boolean wait) {
        super.common_invalidate(key, wait);
    }

    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
    public Object remove(Object key) {
        return super.common_remove(key);
    }

    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
    public Set keySet() {
        return super.common_keySet();
    }

    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
    public Set keySet(boolean includeDiskCache)  {
        return super.common_keySet(includeDiskCache);
    }
    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
    public Collection values() {
        return super.common_values();
    }

    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
    public Set entrySet() {
        return super.common_entrySet();
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
    public boolean addChangeListener(ChangeListener listener) {
        return super.common_addChangeListener(listener);
    }

    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
    public boolean removeChangeListener(ChangeListener listener) {
        return super.common_removeChangeListener(listener);
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

    // todo V6.1
    //-----------------------------------------------------
    // Overrides super class method to activate function.
    // JDoc is in the super class.
    //-----------------------------------------------------
    /*
    public boolean destroy(){
        boolean success = false;

        return success;
    }
    */

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
}
