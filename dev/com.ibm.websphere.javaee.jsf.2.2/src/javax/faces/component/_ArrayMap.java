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
package javax.faces.component;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A Map implementation that stores its contents in a single
 * array.  This approach is significantly faster for small sets of
 * data than the use of a HashMap or Hashtable, though potentially
 * much slower for very large sets.
 * <p>
 * ArrayMap is optimized for many-reads-few-write.  In particular,
 * it reallocates its array on any insertion or deletion.
 * <p>
 * ArrayMap also includes a series of static methods for managing the
 * Object array. These may be used in place of instantiating an
 * ArrayMap for clients that don't need a Map implementation.
 * Clients using these methods must be careful to store the returned
 * Object array on any mutator method.  They also must provide their
 * own synchronization, if needed.  When using these static methods,
 * clients can opt to search for objects by identity (via
 * <code>getByIdentity()</code>) instead of equality, while the static
 * <code>get()</code> method will try identity before equality.  This
 * latter approach is extremely fast but still safe for retrieval of
 * Strings that have all been interned, especially if misses are
 * infrequent (since misses do require a scan using Object.equals()).
 * It's worth remembering that String constants are always interned,
 * as required by the language specification.
 * <p>
 * @version $Name:  $ ($Revision: adfrt/faces/adf-faces-api/src/main/java/oracle/adf/view/faces/util/ArrayMap.java#0 $) $Date: 10-nov-2005.19:08:36 $
 */
// -= Simon Lessard =-
//        Using a single array for both the key and the value leads to many 
//        problems, especially with type safety. Using parallel arrays or 
//        a single array containing nodes would be a much cleaner/safer idea.
// =-= AdamWiner =-=
//        True, but the whole point of this class is maximal efficiency for
//        small, transient arrays.  The type safety problems are entirely internal,
//        not exposed to clients.  Parallel arrays or arrays containing nodes
//        would be less efficient - if you're willing to allocate bonus objects,
//        and don't care about efficiency, just use HashMap.
class _ArrayMap<K, V> extends AbstractMap<K, V> implements Cloneable
{
    /**
     * Creates an empty ArrayMap, preallocating nothing.
     */
    public _ArrayMap()
    {
        this(0, 1);
    }

    /**
     * Creates an ArrayMap, preallocating for a certain size.
     * @param size the number of elements to pre-allocate for
     */
    public _ArrayMap(int size)
    {
        this(size, 1);
    }

    /**
     * Creates an ArrayMap, preallocating for a certain size.
     * @param size the number of elements to pre-allocate for
     * @param increment the number of additional elements to
     *     allocate for when overruning
     */
    public _ArrayMap(int size, int increment)
    {
        if ((increment < 1) || (size < 0))
        {
            throw new IllegalArgumentException();
        }

        if (size > 0)
        {
            _array = new Object[2 * size];
        }

        _increment = increment;
    }

    /**
     * Returns the key at a specific index in the map.
     */
    @SuppressWarnings("unchecked")
    public K getKey(int index)
    {
        if ((index < 0) || (index >= size()))
        {
            throw new IndexOutOfBoundsException();
        }
        return (K) _array[index * 2];
    }

    /**
     * Returns the value at a specific index in the map.
     */
    @SuppressWarnings("unchecked")
    public V getValue(int index)
    {
        if ((index < 0) || (index >= size()))
        {
            throw new IndexOutOfBoundsException();
        }
        return (V) _array[index * 2 + 1];
    }

    /**
     * Gets the object stored with the given key.  Scans first
     * by object identity, then by object equality.
     */
    static public Object get(Object[] array, Object key)
    {
        Object o = getByIdentity(array, key);
        if (o != null)
        {
            return o;
        }

        return getByEquality(array, key);
    }

    /**
     * Gets the object stored with the given key, using
     * only object identity.
     */
    static public Object getByIdentity(Object[] array, Object key)
    {
        if (array != null)
        {
            int length = array.length;
            for (int i = 0; i < length; i += 2)
            {
                if (array[i] == key)
                {
                    return array[i + 1];
                }
            }
        }

        return null;
    }

    /**
     * Gets the object stored with the given key, using
     * only object equality.
     */
    static public Object getByEquality(Object[] array, Object key)
    {
        if (array != null)
        {
            int length = array.length;

            for (int i = 0; i < length; i += 2)
            {
                Object targetKey = array[i];
                if (targetKey == null)
                {
                    return null;
                }
                else if (targetKey.equals(key))
                {
                    return array[i + 1];
                }
            }
        }

        return null;
    }

    /**
     * Adds the key/value pair to the array, returning a
     * new array if necessary.
     */
    static public Object[] put(Object[] array, Object key, Object value)
    {
        if (array != null)
        {
            int length = array.length;

            for (int i = 0; i < length; i += 2)
            {
                Object curKey = array[i];

                if (((curKey != null) && (curKey.equals(key)))
                        || (curKey == key))
                {
                    array[i + 1] = value;
                    return array;
                }
            }
        }

        return _addToArray(array, key, value, 1);
    }

    /**
     * Removes the value for the key from the array, returning a
     * new array if necessary.
     */
    static public Object[] remove(Object[] array, Object key)
    {
        return remove(array, key, true);
    }

    /**
     * Removes the value for the key from the array, returning a
     * new array if necessary.
     */
    static public Object[] remove(Object[] array, Object key, boolean reallocate)
    {
        if (array != null)
        {
            int length = array.length;

            for (int i = 0; i < length; i += 2)
            {
                Object curKey = array[i];

                if (((curKey != null) && curKey.equals(key)) || (curKey == key))
                {
                    Object[] newArray = array;

                    if (reallocate)
                    {
                        newArray = new Object[length - 2];
                        System.arraycopy(array, 0, newArray, 0, i);
                    }

                    System.arraycopy(array, i + 2, newArray, i, length - i - 2);

                    if (!reallocate)
                    {
                        array[length - 1] = null;
                        array[length - 2] = null;
                    }

                    return newArray;
                }
            }
        }

        return array;
    }

    //
    // GENERIC MAP API
    //
    @Override
    public int size()
    {
        return _size;
    }

    @Override
    public boolean containsValue(Object value)
    {
        int entryCount = size() * 2;
        for (int i = 0; i < entryCount; i += 2)
        {
            if (_equals(value, _array[i + 1]))
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean containsKey(Object key)
    {
        int entryCount = size() * 2;
        for (int i = 0; i < entryCount; i += 2)
        {
            if (_equals(key, _array[i]))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns an enumeration of the keys in this map.
     * the Iterator methods on the returned object to fetch the elements
     * sequentially.
     */
    @SuppressWarnings("unchecked")
    public Iterator<K> keys()
    {
        int size = _size;

        if (size == 0)
        {
            return null;
        }
        ArrayList<K> keyList = new ArrayList<K>();
        int i = (size - 1) * 2;
        while (i >= 0)
        {
            keyList.add((K) _array[i]);
            i = i - 2;
        }
        return keyList.iterator();
    }

    /**
     * Returns an Iterator of keys in the array.
     */
    public static Iterator<Object> getKeys(Object[] array)
    {
        if (array == null)
        {
            return null;
        }
        ArrayList<Object> keyList = new ArrayList<Object>();
        int i = array.length - 2;
        while (i >= 0)
        {
            keyList.add(array[i]);
            i = i - 2;
        }
        return keyList.iterator();
    }

    /**
     * Returns an Iterator of values in the array.
     */
    public static Iterator<Object> getValues(Object[] array)
    {
        if (array == null)
        {
            return null;
        }
        ArrayList<Object> valueList = new ArrayList<Object>();
        int i = array.length - 1;
        while (i >= 0)
        {
            valueList.add(array[i]);
            i = i - 2;
        }
        return valueList.iterator();
    }

    /**
     * Clones the map.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object clone()
    {
        try
        {
            _ArrayMap<K, V> am = (_ArrayMap<K, V>) super.clone();

            am._array = _array.clone();
            am._size = _size;
            am._increment = _increment;
            return am;
        }
        catch (CloneNotSupportedException cnse)
        {
            // Should never reach here
            return null;
        }
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet()
    {
        if (_entrySet == null)
        {
            _entrySet = new AbstractSet<Map.Entry<K, V>>()
            {
                @Override
                public int size()
                {
                    return _ArrayMap.this.size();
                }

                @Override
                public Iterator<Map.Entry<K, V>> iterator()
                {
                    return new Iterator<Map.Entry<K, V>>()
                    {
                        public boolean hasNext()
                        {
                            return (_index < _ArrayMap.this.size());
                        }

                        public void remove()
                        {
                            // remove() removes the last entry returned by next(),
                            // not the one about to be seen;  so that's actually
                            // the entry at (_index - 1).
                            if ((_index == 0) || _removed)
                            {
                                throw new IllegalStateException();
                            }

                            _removed = true;
                            // Shrink the array by one
                            int size = _ArrayMap.this.size();
                            Object[] array = _ArrayMap.this._array;
                            if (size > _index)
                            {
                                System.arraycopy(array, _index * 2, array,
                                        (_index - 1) * 2, (size - _index) * 2);
                            }

                            // Null out the last elements (for GC)
                            array[size * 2 - 2] = null;
                            array[size * 2 - 1] = null;

                            _ArrayMap.this._size = size - 1;

                            // And push the index back one
                            _index = _index - 1;
                        }

                        public Map.Entry<K, V> next()
                        {
                            if (!hasNext())
                            {
                                throw new NoSuchElementException();
                            }

                            final int index = _index;
                            _removed = false;
                            _index = index + 1;

                            return new Map.Entry<K, V>()
                            {
                                public K getKey()
                                {
                                    return _ArrayMap.this.getKey(index);
                                }

                                public V getValue()
                                {
                                    return _ArrayMap.this.getValue(index);
                                }

                                public V setValue(V value)
                                {
                                    V oldValue = getValue();
                                    _ArrayMap.this._array[index * 2 + 1] = value;
                                    return oldValue;
                                }

                                @SuppressWarnings("unchecked")
                                @Override
                                public boolean equals(Object o)
                                {
                                    if (!(o instanceof Map.Entry))
                                    {
                                        return false;
                                    }
                                    Map.Entry<K, V> e = (Map.Entry<K, V>) o;
                                    return _equals(getKey(), e.getKey())
                                            && _equals(getValue(), e.getValue());
                                }

                                @Override
                                public int hashCode()
                                {
                                    Object key = getKey();
                                    Object value = getValue();
                                    return ((key == null) ? 0 : key.hashCode())
                                            ^ ((value == null) ? 0 : value
                                                    .hashCode());
                                }
                            };
                        }

                        private int _index;
                        private boolean _removed;
                    };
                }
            };
        }

        return _entrySet;
    }

    @SuppressWarnings("unchecked")
    @Override
    public V get(Object key)
    {
        return (V) getByEquality(_array, key);
        //return getByIdentity(_array, key);
    }

    @SuppressWarnings("unchecked")
    public V getByIdentity(Object key)
    {
        return (V) getByIdentity(_array, key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public V put(K key, V value)
    {
        if (value == null)
        {
            return remove(key);
        }

        Object[] array = _array;
        // Use getByEquality().  In the vast majority
        // of cases, the object isn't there.  So getByIdentity()
        // will fail, and we'll call getByEquality() anyway.
        Object o = getByEquality(array, key);

        if (o == null)
        {
            int size = _size * 2;
            if ((array != null) && (size < array.length))
            {
                array[size] = key;
                array[size + 1] = value;
            }
            else
            {
                _array = _addToArray(array, key, value, _increment);
            }

            _size = _size + 1;
        }
        else
        {
            // (Actually, I know in this case that the returned array
            // isn't going to change, but that'd be a good way to introduce
            // a bug if we ever to change the implementation)
            _array = put(array, key, value);
        }

        return (V) o;
    }

    @SuppressWarnings("unchecked")
    @Override
    public V remove(Object key)
    {
        Object[] array = _array;
        Object o = get(array, key);
        if (o != null)
        {
            remove(array, key, false);
            _size = _size - 1;
        }

        return (V) o;
    }

    /**
     * Removes all elements from the ArrayMap.
     */
    @Override
    public void clear()
    {
        int size = _size;
        if (size > 0)
        {
            size = size * 2;
            for (int i = 0; i < size; i++)
            {
                _array[i] = null;
            }

            _size = 0;
        }
    }

    /**
     * Adds the key/value pair to the array, returning a
     * new array if necessary.
     */
    private static Object[] _addToArray(Object[] array, Object key,
            Object value, int increment)
    {
        Object[] newArray = null;
        if (array != null)
        {
            int length = array.length;
            newArray = new Object[length + (2 * increment)];
            System.arraycopy(array, 0, newArray, 2, length);
        }
        else
        {
            newArray = new Object[2 * increment];
        }

        newArray[0] = key;
        newArray[1] = value;
        return newArray;
    }

    private static boolean _equals(Object a, Object b)
    {
        if (a == null)
        {
            return b == null;
        }
        return a.equals(b);
    }

    private Object[] _array;
    private int _size;
    private int _increment;
    private Set<Map.Entry<K, V>> _entrySet;
}
