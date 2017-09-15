/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class HTODDependencyMap extends ConcurrentHashMap {
	
	private static final long serialVersionUID = -867177773157684697L;
    private static TraceComponent tc = Tr.register(DependencyTable.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
	
	Map< Object, Integer> cacheIdTable = null;
	boolean dependencyCacheIndexEnabled = false;

	public HTODDependencyMap(int initialCapacity, float loadFactor,
			int concurrencyLevel, boolean dependencyCacheIndexEnabled) {
		super(initialCapacity, loadFactor, concurrencyLevel);
		this.dependencyCacheIndexEnabled = dependencyCacheIndexEnabled;
        if (tc.isDebugEnabled())
        	Tr.debug(tc, "dependencyCacheIndexEnabled="+ dependencyCacheIndexEnabled );
		if (dependencyCacheIndexEnabled)
		    cacheIdTable = new HashMap<Object, Integer>(10 * initialCapacity);
	}
	
	public Object put(Object key, Object value) {
		if (dependencyCacheIndexEnabled)		
		    addToCacheIdTable((ValueSet)value);
		return super.put(key, value);
	}
	
	public Object remove(Object key) {
		ValueSet valueSet = (ValueSet) super.remove(key);
		if (dependencyCacheIndexEnabled)
		    removeFromCacheIdTable(valueSet);
		return valueSet;
	}
	
	public boolean containsValue(Object id) {
		if (dependencyCacheIndexEnabled)		
		    return cacheIdTable.containsKey(id);
		
		boolean found = false;
		Iterator<Map.Entry<Object, Set<Object>>> edependency = 
				this.entrySet().iterator();
		while (edependency.hasNext()) {			
			Map.Entry<Object, Set<Object>> dependencyToCacheIDs = edependency.next();
			if (dependencyToCacheIDs.getValue().contains(id)) {
				found = true;
			}
		}
		
		return found;
	}

	public void clear() {
		if (cacheIdTable != null)
		    cacheIdTable.clear();
		super.clear();
	}
	
    private void addToCacheIdTable (ValueSet valueSet) {
    	Iterator values = valueSet.iterator();
    	while (values.hasNext()) {
    		Object cacheId = values.next();
        	Integer count = (Integer) cacheIdTable.get(cacheId);
        	if (count != null ) {
        		cacheIdTable.put(cacheId, Integer.valueOf(count.intValue()+1));
        	} else {
        		cacheIdTable.put(cacheId, Integer.valueOf(1));
        	}
    	}    		   	
    }
    
    private void removeFromCacheIdTable (ValueSet valueSet) {
    	Iterator values = valueSet.iterator();
    	while (values.hasNext()) {
    		Object cacheId = values.next();
        	Integer count = (Integer) cacheIdTable.get(cacheId);
        	if (count != null ) {
        		int counter = count.intValue();
        		if (counter == 1)
        			cacheIdTable.remove(cacheId);
        		else
        		    cacheIdTable.put(cacheId, Integer.valueOf(counter-1));
        	} 
    	}    		   	
    }
}
