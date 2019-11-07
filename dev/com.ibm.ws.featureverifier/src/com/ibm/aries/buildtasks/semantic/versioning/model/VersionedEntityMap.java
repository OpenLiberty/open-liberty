/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.aries.buildtasks.semantic.versioning.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/*
 * a special map, that allows addition of versioned and unversioned entities as keys.
 * 
 * sets of objects can be associated to versioned entities, and to unversioned. 
 * 
 * querying the map with a versioned entity will return unversioned responses, and versioned matches.
 *  this is the primary use case, to allow looking up featureinfos for a versioned package.
 * 
 * worthy of note is that the collection obtain via get may not match that placed via put.
 * 
 * Eg   map.put(mykey,myset);  map.get(mykey) may not return myset, as it may match additional entities.
 *  
 * This means logic like
 *   map.get(mykey).add(myvalue); 
 * will NOT work as expected.
 * 
 * Instead, use the 'merge' method provided.
 *   map.merge(mykey,myvalue);
 *  
 * the entry set of the map is every versioned/unversioned entity key->value set.
 */
public class VersionedEntityMap<K extends VersionedEntity, T> implements Map<K, Set<T>> {
    @SuppressWarnings("unused")
    private static final long serialVersionUID = -592304052431729385L;
    private final Map<K, Set<T>> keyedByNullVersionedEntities = new HashMap<K, Set<T>>();
    private final Map<K, Set<T>> keyedByVersionedEntities = new HashMap<K, Set<T>>();

    @Override
    public void clear() {
        keyedByNullVersionedEntities.clear();
        keyedByVersionedEntities.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key).size() > 0;
    }

    @Override
    public boolean containsValue(Object value) {
        //no need to go looking for a given set of values.. makes no sense!
        throw new UnsupportedOperationException("Map does not support lookup by value");
    }

    /**
     * Will return & iterate null-versioned and versioned package->feature mappings.
     */
    @Override
    public Set<java.util.Map.Entry<K, Set<T>>> entrySet() {
        //we want to iterate both sets combined and in order..
        Set<K> notVersioned = keyedByNullVersionedEntities.keySet();
        Set<K> versioned = keyedByVersionedEntities.keySet();
        TreeSet<K> results = new TreeSet<K>();
        results.addAll(notVersioned);
        results.addAll(versioned);
        Set<java.util.Map.Entry<K, Set<T>>> resultSet = new LinkedHashSet<Map.Entry<K, Set<T>>>();
        for (K p : results) {
            final K pkg = p;
            final Set<T> fis = get(pkg);
            if (!fis.isEmpty()) {
                resultSet.add(new Map.Entry<K, Set<T>>() {
                    @Override
                    public K getKey() {
                        return pkg;
                    }

                    @Override
                    public Set<T> getValue() {
                        return fis;
                    }

                    @Override
                    public Set<T> setValue(Set<T> object) {
                        throw new UnsupportedOperationException("Set not supported on map.entry");
                    }
                });
            }
        }
        return resultSet;
    }

    /**
     * Returns the set of features that match the versioned key
     */
    @Override
    public Set<T> get(Object key) {
        Set<T> results = new HashSet<T>();
        if (key instanceof VersionedEntity) {
            VersionedEntity pkey = (VersionedEntity) key;
            if (pkey.getVersion() != null) {
                //request was made with a versioned key.. match nulls, and version matches.				
                VersionedEntity nullEquiv = new VersionedEntity(pkey.getName(), null);
                boolean nullMatch = keyedByNullVersionedEntities.containsKey(nullEquiv);
                boolean versMatch = keyedByVersionedEntities.containsKey(pkey);
                if (nullMatch) {
                    results.addAll(keyedByNullVersionedEntities.get(nullEquiv));
                }
                if (versMatch) {
                    results.addAll(keyedByVersionedEntities.get(pkey));
                }
            } else {
                //request was made with a null key.. we need to match all packages, regardless of version
                Set<T> nulls = keyedByNullVersionedEntities.get(pkey);
                if (nulls != null)
                    results.addAll(nulls);
                for (Map.Entry<K, Set<T>> entry : keyedByVersionedEntities.entrySet()) {
                    if (entry.getKey().getName().equals(pkey.getName())) {
                        results.addAll(entry.getValue());
                    }
                }
            }
        }
        return results;
    }

    @Override
    public boolean isEmpty() {
        return keyedByNullVersionedEntities.isEmpty() && keyedByVersionedEntities.isEmpty();
    }

    @Override
    public Set<K> keySet() {
        Set<K> results = new HashSet<K>();
        results.addAll(keyedByNullVersionedEntities.keySet());
        results.addAll(keyedByVersionedEntities.keySet());
        return results;
    }

    @Override
    public Set<T> put(K key, Set<T> value) {
        if (key.getVersion() == null) {
            return keyedByNullVersionedEntities.put(key, value);
        } else {
            return keyedByVersionedEntities.put(key, value);
        }
    }

    public void merge(K key, T value) {
        if (key.getVersion() == null) {
            if (!keyedByNullVersionedEntities.containsKey(key)) {
                keyedByNullVersionedEntities.put(key, new HashSet<T>());
            }
            keyedByNullVersionedEntities.get(key).add(value);
        } else {
            if (!keyedByVersionedEntities.containsKey(key)) {
                keyedByVersionedEntities.put(key, new HashSet<T>());
            }
            keyedByVersionedEntities.get(key).add(value);
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends Set<T>> map) {
        for (Map.Entry<? extends K, ? extends Set<T>> entry : map.entrySet()) {
            this.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Set<T> remove(Object key) {
        //remove is kinda tricky, as our keys are not 1:1 to values... 
        //we could allow remove to remove all matches from getEntry, but that's 
        //probably not what we need.. 
        throw new UnsupportedOperationException("Remove not supported");
    }

    @Override
    public int size() {
        return keyedByNullVersionedEntities.size() + keyedByVersionedEntities.size();
    }

    @Override
    public Collection<Set<T>> values() {
        throw new UnsupportedOperationException("Not implemented");
    }

}
