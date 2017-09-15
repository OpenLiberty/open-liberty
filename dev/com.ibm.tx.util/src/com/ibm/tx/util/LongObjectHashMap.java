/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.tx.util;

import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;


/** 
 * An implementation of a hash map that stores <code>Objects</code>
 * keyed by <code>longs</code>. Any long is a valid key, null is not
 * permitted as a value.
 * <p>
 * To enable the map to function correctly its size must be
 * a power of two. If an initial size is specified when the map is
 * constructed and it is not a power of two an
 * <code>IllegalArgumentException</code> is thrown.
 * </p>
 * <p>
 * This implementation is not synchronized and therefore external
 * synchronization must be used when the map is being accessed
 * concurrently by multiple threads and one or more of the threads
 * is modifying the map's contents.
 * </p>
 */
public class LongObjectHashMap
{
	private static TraceComponent tc = Tr.register(LongObjectHashMap.class, null, null);
		
	// A special entry in the map to indicate that
	// a bucket previously held a value but that value
	// has now been deleted.
	private static final Object DELETED = new Object();
	
	// The values currently in the map
	private Object[] _values;
	
	// The keys corresponding to the values in the map
	// The key of _values[n] is _keys[n];
	private long[] _keys;
	
	// The load factor for the map. Once this load factor
	// has been exceeded the map will be grown and its
	// entire contents rehashed.
	private int _loadFactor;
	
	// The size of the map.
	private int _mapSize;
	
	// The number of items currently held in the map.
	private int _currentLoad;

    private static final int DEFAULT_INITIAL_MAP_SIZE = 256;
    private static final int DEFAULT_LOAD_FACTOR = 60;

    private int _resizeThreshold;
	
	/** 
	 * Construct a new, empty hash map with default
	 * intial size and load factor
	 */
	public LongObjectHashMap()
	{
		this(DEFAULT_INITIAL_MAP_SIZE, DEFAULT_LOAD_FACTOR);
	}
	
	/** 
	 * Construct a new, empty hash map using the default load factor
     * and with the given initial size.
     * 
	 * @param initialSize the initial size of the hash map
     * @throws IllegalArgumentException Thrown if <code>initialSize</code> is not a power of 2
	 */
	public LongObjectHashMap(int initialSize) throws IllegalArgumentException
	{
		this(initialSize, DEFAULT_LOAD_FACTOR);
	}
	
	/**
     * Construct a new empty map with the given initial size and load factor.
     * <p>
     * The map's size must be a power of two; an initial size that is not
     * a power of two will result in an <code>IllegalArgumentException</code> being
     * thrown.
     * </p>
	 * <p>
     * The load factor is expressed as a percentage of the map's size. Once the
     * percentage of in-use buckets exceeds the load factor the map is resized.
     * The valid range for the load factor is 0 < loadFactor <= 100.
     * </p>
	 * @param initialSize the initial size of the hash map
	 * @param loadFactor the load factor of the hash map
     * @throws IllegalArgumentException Thrown if <code>initialSize</code> is not a power of 2
	 */
	public LongObjectHashMap(int initialSize, int loadFactor) throws IllegalArgumentException
	{
		if (tc.isEntryEnabled()) Tr.entry(tc, "LongObjectHashMap", new Object[]{new Integer(initialSize), new Integer(loadFactor)});
		
        
        if (loadFactor <= 0 || loadFactor > 100)
		{
			if (tc.isEntryEnabled()) Tr.exit(tc, "LongObjectHashMap", "IllegalArgumentException");
			throw new IllegalArgumentException("loadFactor must lie in the range 0 < loadFactor <= 100");
		}

        if ((initialSize < 0) || ((initialSize & (initialSize - 1)) != 0))
        {
        	if (tc.isEntryEnabled()) Tr.exit(tc, "LongObjectHashMap", "IllegalArgumentException");
			throw new IllegalArgumentException("initialSize must be a power of two and greater than zero");
		}
		
		_loadFactor = loadFactor;
		_mapSize = initialSize;
		
		_values = new Object[_mapSize];		
		_keys = new long[_mapSize];
		
		_currentLoad = 0;

        _resizeThreshold = (int)(((float)_mapSize) * ((float)_loadFactor) / 100f);

        if (tc.isDebugEnabled()) Tr.debug(tc, "resizeThreshold = " + _resizeThreshold);
        
		if (tc.isEntryEnabled()) Tr.exit(tc, "LongObjectHashMap", this);		
	}
	
	/**
	 * Store the given value in the map, associating it with the given key. If the
	 * map already contains an entry associated with the given key that entry will
	 * be replaced.
	 * @param key The key to associate with the entry
	 * @param value The entry to be associated with the key
	 * @return The entry replaced by this put operation, or null if no entry was replaced.
	 * Null can also be returned if the given key was previously associated with a null value.
	 */
	public Object put(long key, Object value)
	{
		if (tc.isEntryEnabled()) Tr.entry(tc, "put", new Object[]{new Long(key), value, this});
		
		if (value == null)
		{
			if (tc.isEntryEnabled()) Tr.exit(tc, "put", "IllegalArgumentException");
			throw new IllegalArgumentException("Null is not a permitted value.");
		}
		
		// Find an empty bucket for the new value or
		// a bucket the contains an entry with the
		// same key.
		int hash = getHashForNewEntry(key);
		
		// Store the value previously held in the bucket
		// mapping DELETED to null so that it
		// can be returned to the caller.
		Object previous = _values[hash] == DELETED ? null : _values[hash];
		
		// Add the new value and key to the map.
		_values[hash] = value;
		_keys[hash] = key;        
		
		if (previous == null)
		{
			// This put resulted in a new entry being added to
			// the map rather than an existing entry being
			// updated. Increment the count of objects in the
			// map and rehash if necessary.
			_currentLoad++;
			
			if (_currentLoad == _resizeThreshold)
			{
                if (tc.isEntryEnabled()) Tr.entry(tc, "put", new Object[]{new Long(key), value, this});

				// The current load of the map means that it
				// has reached the desired loadFactor. Increase
				// the size of the map and rehash all the entries.
				
                // Take a copy of the map's contents so that we
                // can add them back in once it's been resized.
                long[] oldKeys = new long[_mapSize];
				Object[] oldValues = new Object[_mapSize];
                System.arraycopy(_keys, 0, oldKeys, 0, oldKeys.length);
                System.arraycopy(_values, 0, oldValues, 0, oldValues.length);
				
				// We know that the inital size of the map
                // is a power of 2 so, by doubling it, we
                // can increase the map's capacity and 
                // still be sure that it's size is a power
                // of two.
				_mapSize = (_mapSize * 2);

                if (tc.isDebugEnabled()) Tr.debug(tc, "mapSize = " + _mapSize);

                _resizeThreshold = (int)(((float)_mapSize) * ((float)_loadFactor) / 100f);

                if (tc.isDebugEnabled()) Tr.debug(tc, "resizeThreshold = " + _resizeThreshold);

				_values = new Object[_mapSize];
				_keys = new long[_mapSize];
				
				// Zero the count of objects in the map. This will
				// be incremented to the correct value as the
				// entries are added back into the resized map
				// below.
				_currentLoad = 0;
				
				// Loop through the old values adding live
				// entries back into the map.
				for (int i = 0; i < oldValues.length; i++)
				{
					Object oldValue = oldValues[i];
					
					if (oldValue != null && oldValue != DELETED)
					{
						put(oldKeys[i], oldValue);
					}
				}			
			}
		}
		
		if (tc.isEntryEnabled()) Tr.exit(tc, "put", previous);
		return previous;				
	}
	
	/**
	 * Remove the entry from the map that is associated with
	 * the given key.
	 * @param key The key associated with the object that is to be removed
	 * @return The object that has been removed from the map, or null if no object was removed.
	 * Null can also be returned if the given key was previously associated with a null value.
	 */
	public Object remove(long key)
	{
		if (tc.isEntryEnabled()) Tr.entry(tc, "remove", new Object[]{new Long(key), this});
		
		int hash = getHashForExistingEntry(key);	
		Object result = null;
        
        if (hash >= 0)
        {
            result = _values[hash];
        }
	
		if (result != null)
		{
			if (tc.isDebugEnabled()) Tr.debug(tc, "Entry found in map - removing");
			
			_values[hash] = DELETED;
			_currentLoad--;
		}
		
		if (tc.isEntryEnabled()) Tr.exit(tc, "remove", result); 	
		return result;
	}
	
	/**
	 * Return the entry from the map associated with the given key
	 * @param key the key whose associated value is to be returned
	 * @return the entry in the map associated with the given key.
	 */
	public Object get(long key)
	{
		if (tc.isEntryEnabled()) Tr.entry(tc, "get", new Object[]{new Long(key), this});
		
		int hash = getHashForExistingEntry(key);
		Object value = null;
         
        if (hash >= 0)
        {
            value = _values[hash];
        }
		
		if (tc.isEntryEnabled()) Tr.exit(tc, "get", value);				
		return value;
	}
	
	/**
	 * Return all of the Objects currently stored in the map
	 * as an Object[] - the ordering of the Objects in the
     * array is undetermined.
     * <p>
     * Note that this method is not thread safe. Another thread
     * making changes to the map while this method is being invoked
     * will cause unpredictable results.
	 * 
	 * @return An array of the Objects currently in the map
	 */
	public Object[] values()
	{
		if (tc.isEntryEnabled()) Tr.entry(tc, "values", this);

        if (tc.isDebugEnabled()) Tr.debug(tc, "Current load = " + _currentLoad);
		
		Object[] values = new Object[_currentLoad];
		int objCount = 0;
		
		// Iterate through the map pulling out each
		// live entry into the values array.
		for (int i = 0; i < _mapSize; i++)
		{
			Object value = _values[i];
			
			if (value != null && value != DELETED)
			{
				values[objCount++] = value;		
			}
		}
		
		return values;
	}
	
	// This method will return a hash value that points
	// to a bucket containing a live entry in the map
	// or a bucket containing null if the map does not
	// contain an entry for the given key. If the
    // map is full of DELETED values -1 will be 
    // returned.
	private int getHashForExistingEntry(long key)
	{
		if (tc.isEntryEnabled()) Tr.entry(tc, "getHashForExistingEntry", new Object[]{new Long(key), this});
		
		int hash = getHashForKey(key);

        int i;

        for (i = 1; i < _mapSize && (_values[hash] == DELETED || (_values[hash] != null && _keys[hash] != key)); i++)
        {
            hash = (hash + i) % _mapSize;
        }

        if (i == _mapSize)
        {
            hash = -1;
        }
		
		if (tc.isEntryEnabled()) Tr.exit(tc, "getHashForExistingEntry", new Integer(hash));		
		return hash;
	}
	
	// This method will return a hash value for the
	// given key that points to a bucket that is empty
	// (that is it contains null or DELETED), or that
	// contains a live entry whose key is the same as
	// the given one.
	private int getHashForNewEntry(long key)
	{
		if (tc.isEntryEnabled()) Tr.entry(tc, "getHashForNewEntry", new Object[]{new Long(key), this});
		int hash = getHashForKey(key);

        // Loop while the current bucket contains a live
        // entry and that entry's key does not match
        // that of the new entry.
        for (int i = 1; _values[hash] != null && _values[hash] != DELETED && _keys[hash] != key; i++)
        {
            hash = (hash + i) % _mapSize;
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "getHashForNewEntry", new Integer(hash));
        return hash;
    }
	
	// Maps the given long key to a 
	// suitable int index into the map
	private int getHashForKey(long key)
	{
		if (tc.isEntryEnabled()) Tr.entry(tc, "getHashForKey", new Object[]{new Long(key), this});
		
		int hash = (int) (key % _mapSize);
		if (hash < 0) hash = -hash;
	
		if (tc.isEntryEnabled()) Tr.exit(tc, "getHashForKey", new Integer(hash));
		return hash;
	}
}
