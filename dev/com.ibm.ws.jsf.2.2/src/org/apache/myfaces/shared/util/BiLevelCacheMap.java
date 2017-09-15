/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.shared.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A bi-level cache based on HashMap for caching objects with minimal sychronization
 * overhead. The limitation is that <code>remove()</code> is very expensive.
 * <p>
 * Access to L1 map is not sychronized, to L2 map is synchronized. New values
 * are first stored in L2. Once there have been more that a specified mumber of
 * misses on L1, L1 and L2 maps are merged and the new map assigned to L1
 * and L2 cleared.
 * </p>
 * <p>
 * IMPORTANT:entrySet(), keySet(), and values() return unmodifiable snapshot collections.
 * </p>
 */
public abstract class BiLevelCacheMap implements Map
{
    //~ Instance fields ----------------------------------------------------------------------------

    private static final int INITIAL_SIZE_L1 = 32;

    /** To preinitialize <code>_cacheL1</code> with default values use an initialization block */
    protected Map       _cacheL1;

    /** Must be final because it is used for synchronization */
    private final Map   _cacheL2;
    private final int   _mergeThreshold;
    private int         _missCount;

    //~ Constructors -------------------------------------------------------------------------------

    public BiLevelCacheMap(int mergeThreshold)
    {
        _cacheL1            = new HashMap(INITIAL_SIZE_L1);
        _cacheL2            = new HashMap(HashMapUtils.calcCapacity(mergeThreshold));
        _mergeThreshold     = mergeThreshold;
    }

    //~ Methods ------------------------------------------------------------------------------------

    public boolean isEmpty()
    {
        synchronized (_cacheL2)
        {
            return _cacheL1.isEmpty() && _cacheL2.isEmpty();
        }
    }

    public void clear()
    {
        synchronized (_cacheL2)
        {
            _cacheL1 = new HashMap(); // dafault size
            _cacheL2.clear();
        }
    }

    public boolean containsKey(Object key)
    {
        synchronized (_cacheL2)
        {
            return _cacheL1.containsKey(key) || _cacheL2.containsKey(key);
        }
    }

    public boolean containsValue(Object value)
    {
        synchronized (_cacheL2)
        {
            return _cacheL1.containsValue(value) || _cacheL2.containsValue(value);
        }
    }

    public Set entrySet()
    {
        synchronized (_cacheL2)
        {
            mergeIfL2NotEmpty();
            return Collections.unmodifiableSet(_cacheL1.entrySet());
        }
    }

    public Object get(Object key)
    {
        Map    cacheL1 = _cacheL1;
        Object retval = cacheL1.get(key);
        if (retval != null)
        {
            return retval;
        }

        synchronized (_cacheL2)
        {
            // Has another thread merged caches while we were waiting on the mutex? Then check L1 again
            if (cacheL1 != _cacheL1)
            {
                retval = _cacheL1.get(key);
                if (retval != null)
                {
                    // do not update miss count (it is not a miss anymore)
                    return retval;
                }
            }

            retval = _cacheL2.get(key);
            if (retval == null)
            {
                retval = newInstance(key);
                if (retval != null)
                {
                    put(key, retval);
                    mergeIfNeeded();
                }
            }
            else
            {
                mergeIfNeeded();
            }
        }

        return retval;
    }

    public Set keySet()
    {
        synchronized (_cacheL2)
        {
            mergeIfL2NotEmpty();
            return Collections.unmodifiableSet(_cacheL1.keySet());
        }
    }

    /**
     * If key is already in cacheL1, the new value will show with a delay,
     * since merge L2->L1 may not happen immediately. To force the merge sooner,
     * call <code>size()<code>.
     */
    public Object put(Object key, Object value)
    {
        synchronized (_cacheL2)
        {
            _cacheL2.put(key, value);

            // not really a miss, but merge to avoid big increase in L2 size
            // (it cannot be reallocated, it is final)
            mergeIfNeeded();
        }

        return value;
    }

    public void putAll(Map map)
    {
        synchronized (_cacheL2)
        {
            mergeIfL2NotEmpty();

            // sepatare merge to avoid increasing L2 size too much
            // (it cannot be reallocated, it is final)
            merge(map);
        }
    }

    /** This operation is very expensive. A full copy of the Map is created */
    public Object remove(Object key)
    {
        synchronized (_cacheL2)
        {
            if (!_cacheL1.containsKey(key) && !_cacheL2.containsKey(key))
            {
                // nothing to remove
                return null;
            }

            Object retval;
            Map newMap;
            synchronized (_cacheL1)
            {
                // "dummy" synchronization to guarantee _cacheL1 will be assigned after fully initialized
                // at least until JVM 1.5 where this should be guaranteed by the volatile keyword
                newMap = HashMapUtils.merge(_cacheL1, _cacheL2);
                retval = newMap.remove(key);
            }

            _cacheL1 = newMap;
            _cacheL2.clear();
            _missCount = 0;
            return retval;
        }
    }

    public int size()
    {
        // Note: cannot simply return L1.size + L2.size
        //       because there might be overlaping of keys
        synchronized (_cacheL2)
        {
            mergeIfL2NotEmpty();
            return _cacheL1.size();
        }
    }

    public Collection values()
    {
        synchronized (_cacheL2)
        {
            mergeIfL2NotEmpty();
            return Collections.unmodifiableCollection(_cacheL1.values());
        }
    }

    private void mergeIfL2NotEmpty()
    {
        if (!_cacheL2.isEmpty())
        {
            merge(_cacheL2);
        }
    }

    private void mergeIfNeeded()
    {
        if (++_missCount >= _mergeThreshold)
        {
            merge(_cacheL2);
        }
    }

    private void merge(Map map)
    {
        Map newMap;
        synchronized (_cacheL1)
        {
            // "dummy" synchronization to guarantee _cacheL1 will be assigned after fully initialized
            // at least until JVM 1.5 where this should be guaranteed by the volatile keyword
            // But is this enough (in our particular case) to resolve the issues with DCL?
            newMap = HashMapUtils.merge(_cacheL1, map);
        }
        _cacheL1 = newMap;
        _cacheL2.clear();
        _missCount = 0;
    }

    /**
     * Subclasses must implement to have automatic creation of new instances
     * or alternatively can use <code>put<code> to add new items to the cache.<br>
     *
     * Implementing this method is prefered to guarantee that there will be only
     * one instance per key ever created. Calling put() to add items in a multi-
     * threaded situation will require external synchronization to prevent two
     * instances for the same key, which defeats the purpose of this cache
     * (put() is useful when initialization is done during startup and items
     * are not added during execution or when (temporarily) having possibly two
     * or more instances of the same key is not of concern).<br>
     *
     * @param key lookup key
     * @return new instace for the requested key
     */
    protected abstract Object newInstance(Object key);
}
