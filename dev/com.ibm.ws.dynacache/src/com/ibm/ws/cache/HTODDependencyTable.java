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
package com.ibm.ws.cache;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.stat.CachePerf;

/**
 * This is a Hashtable whose keys are dependencies and
 * whose elements are ValueSets that hold entries that depend on the
 * dependency.
 */
public class HTODDependencyTable {

    public static final int DEP_ID_TABLE = 1;
    public static final int TEMPLATE_TABLE = 2;

    private static TraceComponent tc = Tr.register(DependencyTable.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

	private Map<Object, Set<Object>> dependencyToEntryTable = null;
    private Map<Object, Set<Object>> dependencyNotUpdatedTable = null;

    private int type;
    private int maxSize;
    private int entryRemove;
    public  int delayOffloadEntriesLimit;
    private HTODDynacache htod;

    /**
     * Constructor with parameter.
     *
     * @param type The type to specify DEP_ID_TABLE or TEMPLATE_TABLE.
     * @param initialSize The initial size of the dependency table.
     * @param maxSize The maximum size of the dependency table.
     * @param htod The HTODDynacache object.
     */
    public HTODDependencyTable(int type, int initialSize, int maxSize, int entryRemove, int delayOffloadEntriesLimit, boolean dependencyCacheIndexEnabled, HTODDynacache htod) {
        this.type = type;
		this.dependencyToEntryTable = new HTODDependencyMap(10 * initialSize, 0.75f, 1, dependencyCacheIndexEnabled); 
        this.dependencyNotUpdatedTable = new ConcurrentHashMap(initialSize/2, 0.75f, 1);
        this.maxSize = maxSize;
        this.entryRemove = entryRemove;
        this.delayOffloadEntriesLimit = delayOffloadEntriesLimit;
        this.htod = htod;
    }

    /**
     * This adds a entry to the ValueSet for the specified dependency. The dependency is found 
     * in the dependencyToEntryTable.
     *
     * @param dependency The dependency.
     * @param valueSet containing all entries for this dependency.
     * @param entry The new entry to add.
     */
    public int add(Object dependency, ValueSet valueSet, Object entry) {
        int returnCode = HTODDynacache.NO_EXCEPTION;
        dependencyNotUpdatedTable.remove(dependency);
        valueSet.add(entry);
        if (valueSet.size() > this.delayOffloadEntriesLimit) {
            if (this.type == DEP_ID_TABLE) {
                returnCode = this.htod.writeValueSet(HTODDynacache.DEP_ID_DATA, dependency, valueSet, HTODDynacache.ALL);  // valueSet may be empty after writeValueSet
                this.htod.cache.getCacheStatisticsListener().depIdsOffloadedToDisk(dependency);
                if (tc.isDebugEnabled())
                	Tr.debug(tc, "***** add dependency id=" + dependency + " size=" + valueSet.size());
            } else {
                returnCode = this.htod.writeValueSet(HTODDynacache.TEMPLATE_ID_DATA, dependency, valueSet, HTODDynacache.ALL);  // valueSet may be empty after writeValueSet
                this.htod.cache.getCacheStatisticsListener().templatesOffloadedToDisk(dependency);
                if (tc.isDebugEnabled())
                	Tr.debug(tc, "***** add dependency id=" + dependency + " size=" + valueSet.size());
            }
            dependencyToEntryTable.remove(dependency);
            if (returnCode == HTODDynacache.DISK_SIZE_OVER_LIMIT_EXCEPTION && valueSet.size() > 0) {
                this.htod.delCacheEntry(valueSet, CachePerf.DISK_OVERFLOW, CachePerf.LOCAL, 
                                        !Cache.FROM_DEPID_TEMPLATE_INVALIDATION, 
                                        HTODInvalidationBuffer.FIRE_EVENT);
                returnCode = HTODDynacache.NO_EXCEPTION;
            }
        }
        return returnCode;
    }

    /**
     * This adds a new dependency with its valueSet to the DependencyToEntryTable. 
     *
     * @param dependency The dependency.
     * @param valueSet containing all entries for this dependency.
     */
    public int add(Object dependency, ValueSet valueSet) {
        int returnCode = HTODDynacache.NO_EXCEPTION;
        if (dependencyToEntryTable.size() >= this.maxSize) {
            returnCode = reduceTableSize();
        }
        dependencyNotUpdatedTable.put(dependency, valueSet);
        dependencyToEntryTable.put(dependency, valueSet);
        return returnCode;
    }

    public int add(Object dependency, Object entry) {
        int returnCode = HTODDynacache.NO_EXCEPTION;
        if (dependencyToEntryTable.size() >= this.maxSize) {
            returnCode = reduceTableSize();
        }
        ValueSet valueSet = new ValueSet(4);
        valueSet.add(entry);
        dependencyNotUpdatedTable.put(dependency, valueSet);
        dependencyToEntryTable.put(dependency, valueSet);

        return returnCode;
    }

    /**
     * This replaces the existing dependency with new valueSet in DependencyToEntryTable. 
     *
     * @param dependency The dependency.
     * @param valueSet containing new entries for this dependency.
     */
    public int replace(Object dependency, ValueSet valueSet) {
        int returnCode = HTODDynacache.NO_EXCEPTION;
        dependencyNotUpdatedTable.remove(dependency);
        if (valueSet != null && valueSet.size() > this.delayOffloadEntriesLimit) {
            dependencyToEntryTable.remove(dependency);
            if (this.type == DEP_ID_TABLE) {
                returnCode = this.htod.writeValueSet(HTODDynacache.DEP_ID_DATA, dependency, valueSet, HTODDynacache.ALL);  // valueSet may be empty after writeValueSet
                this.htod.cache.getCacheStatisticsListener().depIdsOffloadedToDisk(dependency);
                //System.out.println("***** replace dependency id=" + dependency + " size=" + valueSet.size());
            } else {
                returnCode = this.htod.writeValueSet(HTODDynacache.TEMPLATE_ID_DATA, dependency, valueSet, HTODDynacache.ALL);  // valueSet may be empty after writeValueSet
                this.htod.cache.getCacheStatisticsListener().templatesOffloadedToDisk(dependency);
                //System.out.println("***** replace template id=" + dependency + " size=" + valueSet.size());
            }
        } else {
            if (valueSet.size() > 0) {
                dependencyToEntryTable.put(dependency, valueSet);
            } else {
                dependencyToEntryTable.remove(dependency);
            }
        }
        if (returnCode == HTODDynacache.DISK_SIZE_OVER_LIMIT_EXCEPTION && valueSet.size() > 0) {
            this.htod.delCacheEntry(valueSet, CachePerf.DISK_OVERFLOW, CachePerf.LOCAL, 
                                    !Cache.FROM_DEPID_TEMPLATE_INVALIDATION, 
                                    HTODInvalidationBuffer.FIRE_EVENT);
            returnCode = HTODDynacache.NO_EXCEPTION;
        }
        return returnCode;
    }

    /**
     * This removes the dependency and its ValueSet (with all of its elements).
     *
     * @param dependency The dependency to remove
     * @return The ValueSet containing all entrys for this dependency.
     */
    public void removeDependency(Object dependency) {
        dependencyNotUpdatedTable.remove(dependency);
        dependencyToEntryTable.remove(dependency);
    }

    /**
     * This removes the specified entry from the specified dependency.
     *
     * @param dependency The dependency.
     * @param result
     */
    public Result removeEntry(Object dependency, Object entry) {
        Result result = this.htod.getFromResultPool();
        ValueSet valueSet = (ValueSet) dependencyToEntryTable.get(dependency);

        if (valueSet == null) {
            return result;
        }
        result.bExist = HTODDynacache.EXIST;
        valueSet.remove(entry);
        dependencyNotUpdatedTable.remove(dependency);

        if (valueSet.isEmpty()) {
            dependencyToEntryTable.remove(dependency);
            if (this.type == DEP_ID_TABLE) {
                result.returnCode = this.htod.delValueSet(HTODDynacache.DEP_ID_DATA, dependency);
            } else {
                result.returnCode = this.htod.delValueSet(HTODDynacache.TEMPLATE_ID_DATA, dependency);
            }
        }
        return result;
    }

    /**
     * This removes all dependencies.
     */
    public void clear() {
        dependencyToEntryTable.clear();
        dependencyNotUpdatedTable.clear();
    }

    /**
     * This returns an Enumeration of all the dependencies from the DependencyToEntryTable.
     *
     * @return An Enumeration of the dependencies.
     */
    public Enumeration getKeys() {
        return new IteratorEnumerator(dependencyToEntryTable.keySet().iterator());
    }

    /**
     * This returns an Enumeration of all the dependencies from the DependencyNotUpdatedTable.
     *
     * @return An Enumeration of the dependencies.
     */
    public Enumeration getNotUpdateKeys() {
    	return new IteratorEnumerator(dependencyNotUpdatedTable.keySet().iterator());
    }

    /**
     * This returns the ValueSet for the specified dependency from the DependencyToEntryTable.
     *
     * @param dependency The dependency to get the entries for.
     * @return The ValueSet containing all entries for this dependency.
     */
    public ValueSet getEntries(Object dependency) {
        ValueSet valueSet = (ValueSet) dependencyToEntryTable.get(dependency);

        return valueSet;
    }

    /**
     * This returns the boolean to indicate whether the DependencyToEntryTable is empty or not.
     * 
     * @return The boolean to indicate the DependencyToEntryTable is empty or not.
     */
    public boolean isEmpty() {
        return dependencyToEntryTable.isEmpty();
    }

    /**
     * This returns the boolean to indicate whether the dependency has been updated or not.
     *
     * @param dependency The dependency to get the entries for.
     * @return The boolean to indicate the specified dependency has been updated with different entries.
     */
    public boolean isUpdated(Object id) {
        return !dependencyNotUpdatedTable.containsKey(id);
    }

	public boolean containsCacheId(Object id) {		
		return dependencyToEntryTable.containsValue(id);
	}

    /**
     * This reduces the DependencyToEntryTable size by offloading some dependencies to the disk.
     */
    private int reduceTableSize() {
        int returnCode = HTODDynacache.NO_EXCEPTION;
        int count = this.entryRemove;

        if (count > 0) {
            int removeSize = 5;
            while (count > 0) {
            	int minSize = Integer.MAX_VALUE;
            	Iterator<Map.Entry<Object,Set<Object>>> e =  dependencyToEntryTable.entrySet().iterator();
            	while (e.hasNext()) {
            		Map.Entry entry = (Map.Entry) e.next();
            		Object id = entry.getKey();
            		ValueSet vs = (ValueSet) entry.getValue();
            		int vsSize = vs.size();
            		if (vsSize < removeSize) {

            			if (this.type == DEP_ID_TABLE) {
            				returnCode = this.htod.writeValueSet(HTODDynacache.DEP_ID_DATA, id, vs, HTODDynacache.ALL);  // valueSet may be empty after writeValueSet
            				this.htod.cache.getCacheStatisticsListener().depIdsOffloadedToDisk(id);
            				Tr.debug(tc, " reduceTableSize dependency id=" + id + " vs=" + vs.size() + " returnCode="+returnCode);
            			} else {
            				returnCode = this.htod.writeValueSet(HTODDynacache.TEMPLATE_ID_DATA, id, vs, HTODDynacache.ALL);  // valueSet may be empty after writeValueSet
            				this.htod.cache.getCacheStatisticsListener().templatesOffloadedToDisk(id);
            				Tr.debug(tc,"reduceTableSize template id=" + id + " vs=" + vs.size() + " returnCode="+returnCode);
            			}
            			dependencyToEntryTable.remove(id);
            			dependencyNotUpdatedTable.remove(id);

            			count--;
            			if (returnCode == HTODDynacache.DISK_EXCEPTION) {
            				return returnCode;
            			} else if (returnCode == HTODDynacache.DISK_SIZE_OVER_LIMIT_EXCEPTION) {
            				this.htod.delCacheEntry(vs, CachePerf.DISK_OVERFLOW, CachePerf.LOCAL, 
            						!Cache.FROM_DEPID_TEMPLATE_INVALIDATION, 
            						HTODInvalidationBuffer.FIRE_EVENT);
            				returnCode = HTODDynacache.NO_EXCEPTION;
            				return returnCode;
            			}
            		} else {
            			minSize = vsSize < minSize ? vsSize : minSize;
            		}

            		if (count == 0) {
            			break;
            		}
            	}
            	removeSize = minSize;
            	removeSize += 3;
            }
        }
        return returnCode;
    }

    /**
     * This gets size of dependency table. 
     */
    public int size() {
        return dependencyToEntryTable.size();
    }
    
    class IteratorEnumerator implements Enumeration 
    {
       private Iterator _iterator = null;
       
       /**
        * Constructor for IteratorEnumerator.
        * @param iter
        */
       public IteratorEnumerator(Iterator iter) 
       {
    		this._iterator = iter;    
       }
       
       /**
        * @return boolean
        * @see java.util.Enumeration#hasMoreElements()
        */
       public boolean hasMoreElements() 
       {
    		return this._iterator.hasNext();    
       }
       
       /**
        * @return Object
        * @see java.util.Enumeration#nextElement()
        */
       public Object nextElement() 
       {
    		return this._iterator.next();    
       }
    }  
}
