/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This is a Hashtable whose keys are dependencies and 
 * whose elements are ValueSets that hold entries that depend on the
 * dependency. 
 * It is used by the Cache to hold the mapping from ids 
 * (cache ids or data ids) to the set of CacheEntrys that depend on them,
 * and to hold the mapping from template names to the 
 * set of CacheEntrys that depend on them. 
 */
public class DependencyTable implements Serializable {
	
    private static final long serialVersionUID = 1342185474L;
   
	private static TraceComponent tc = Tr.register(DependencyTable.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
	private static final int DEFAULT_SIZE = DCacheBase.DEFAULT_CACHE_SIZE;

	static final int CONCURRENT_HASHMAP = 0;     
    static public final int HASHTABLE = 1;         

	private int tableType = CONCURRENT_HASHMAP;  

	
	private Map<Object, ValueSet> dependencyToEntryTable = null;   

	public DependencyTable() {
		this(DEFAULT_SIZE);
	}

	/**
	 * Constructor with parameter.
	 *
	 * @param initialSize The initial size of the dependency table.
	 */
	public DependencyTable(int initialSize) {
		dependencyToEntryTable = new ConcurrentHashMap<Object, ValueSet>(initialSize, 0.75f, 1);
	}

	/**
	 * Constructor with parameter.
	 *
         * @param tableType   Underlying table type - Hashtable or FastHashtable.
	 * @param initialSize The initial size of the dependency table.
	 */
	public DependencyTable(int tableType, int initialSize) {

		if (tableType == HASHTABLE) {
			dependencyToEntryTable = new Hashtable(initialSize);
			this.tableType = tableType;
		} else {
			dependencyToEntryTable = new ConcurrentHashMap<Object, ValueSet>(initialSize, 0.75f, 1);
		}
	}                                                                           

	/**
	 * This adds a entry to the ValueSet for the specified dependency.
	 * 
	 * @param dependency
	 *            The dependency.
	 * @param entry
	 *            The new entry to add.
	 */
	//public void add(String dependency, Object entry) {  //SKS-O
        public void add(Object dependency, Object entry) {  //SKS-O
		if (tc.isDebugEnabled())
			Tr.debug(tc, "IMPORTANT: adding dependency " + dependency + " --> " + entry);
		if (dependency == null) {
			throw new IllegalArgumentException("dependency cannot be null");
		}
		ValueSet valueSet = (ValueSet) dependencyToEntryTable.get(dependency);

		if (valueSet == null) {
			valueSet = new ValueSet(4);
			dependencyToEntryTable.put(dependency, valueSet);
		}
		valueSet.add(entry);
	}

	/**
	 * This adds a valueSet for the specified dependency.
	 * 
	 * @param dependency The dependency.
	 * @param valueSet The valueSet to add. 
	 */
    public void add(Object dependency, ValueSet valueSet) {
		if (dependency == null) {
			throw new IllegalArgumentException("dependency cannot be null");
		}
		if (valueSet != null) {
			dependencyToEntryTable.put(dependency, valueSet);
		}
	}

	/**
	 * This removes the dependency and its ValueSet 
	 * (with all of its elements). 
	 * 
	 * @param dependency The dependency to remove
	 * @return The ValueSet containing all entrys for this dependency.
	 */
	//public ValueSet removeDependency(String dependency) {   //SKS-O
        public ValueSet removeDependency(Object dependency) {   //SKS-O
		return (ValueSet) dependencyToEntryTable.remove(dependency);
	}

	/**
	 * This removes the specified entry from the specified dependency.
	 * 
	 * @param dependency The dependency.
	 * @param entry The entry to remove. 
	 */
	public boolean removeEntry(String dependency, Object entry) {  //SKS-O
            return removeEntry((Object)dependency, entry);
        }                                                           //SKS-O

    public boolean removeEntry(Object dependency, Object entry) {  //SKS-O
        boolean found = false;
		ValueSet valueSet = (ValueSet) dependencyToEntryTable.get(dependency);

		if (valueSet == null) {
			return found;
		}
		found = valueSet.remove(entry);
                if (valueSet.size()==0) 
                    removeDependency(dependency);
        return found;
	}

               
	/**
	 * This removes all dependencies. 
	 */
	public void clear() {
		if (tableType == HASHTABLE) {
			((Hashtable) dependencyToEntryTable).clear();
		} else {
			((FastHashtable) dependencyToEntryTable).clear();
		}
	}

        /** 
         * This returns an Enumeration of all the dependencies currently in the cache.
	 * 
	 * @return An Iterator of the dependencies.
	 */
	public Iterator<Object> getKeys() {
		return dependencyToEntryTable.keySet().iterator();
	}

        /** 
         * This returns the ValueSet for the specified dependency.
	 * 
	 * @param dependency The dependency to get the entries for.
	 * @return The ValueSet containing all entries for this dependency.
	 */
        //public ValueSet getEntries(String dependency) { //SKS-O
        public ValueSet getEntries(Object dependency) { //SKS-O
            ValueSet valueSet = (ValueSet) dependencyToEntryTable.get(dependency);

            return valueSet;
        }
}