/*******************************************************************************
 * Copyright (c) 2003,2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.parser.util;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.properties.StackProperties;
import com.ibm.ws.sip.stack.transaction.util.ApplicationProperties;

/**
 * @author Amir Perlman, Feb 17, 2005
 * 
 * Fixed size Least Recently Used Cache for Strings (need to change the compare
 * operation to make it more general purpose hash). Strings are matched
 * accordint to a hash value calculated from a given char array. In case a match
 * is not available in cache a new instance is created and added to the cache.
 */
public class LRUStringCache {

    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(LRUStringCache.class);
    
    /**
     * Items stored in cache.
     */
    private final HashBucket[] _values;

    /**
     * Firt object in LRU list - most accessed.
     */
    private LRUBucket _LRURoot;

    /**
     * Last object in LRU list - least accessed.
     */
    private LRUBucket _LRUTail;

    /**
     * Current number of items in cache.
     */
    private int _size;

    /**
     * Max number of items in cache.
     */
    private int _maxSize;

    /**
     * Number of successful hits in cache.
     */
    private int _cacheHit;

    /**
     * Number of misses to locate the requeted object in cache.
     */
    private int _cacheMiss;

    /**
     * Number of items removed from cache due to limit of max size exceeded
     */
    private int _cacheOverflow;
    
    /**
     * Singleton instance of this cache. User can choose the shared instane
     * or create their own local cache. 
     */
    private static volatile LRUStringCache c_singletonCache;
    
    /**
     * Free LRU Bucket that can be reused. 
     */
    private LRUBucket _freeLRUBucket; 
    
    /**
     * Locks the cache to prevent two updates happening concurrently from
     * different threads
     */
    private final Lock m_lock;

    /**
     * Controls whether the strings returned by the pool are "intern" strings,
     * that is, instances of strings that are maintained in an intern JVM table.
     * Enabling this buys faster string comparisons, but causes contention when
     * instantiating the string.
     */
    private static final boolean INTERN = true;

    /**
     * thread-local copy of a character array used when caching a byte array
     * as a string. the byte array is first converted to a char array,
     * and then the string is cached from the char array.
     */
    private static final ThreadLocal<char[]> s_threadLocalCharArray =
    	new ThreadLocal<char[]>() {
    		protected char[] initialValue() {
    			return new char[64];
    		}
    	};

    /**
     * Construct a new cache with the specified size.
     * 
     * @param size
     */
    public LRUStringCache(int maxSize) {
        if (maxSize < 2) {
            maxSize = 2;
        }

        _maxSize = maxSize;

        int arraySize = 1;
        //Find a power of 2 >= maximum size
        while (arraySize < maxSize) {
            arraySize <<= 1;
        }

        //Double the size to get performance improvment at the cost of memory.
        arraySize <<= 1;
        _values = new HashBucket[arraySize];
        
        m_lock = new ReentrantLock();

        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(this, "LRUStringCache", "Size: " + _size);
        }
    }

    /**
     * Get a String object from cache that matches the specified char array. If
     * such object does not exist a new String will be created and added to
     * the cache.
     * 
     * @param value
     * @param offset
     * @param count
     * @return A string containing the specified char array. All String are 
     * created using the String.intern() method which allows comparing values
     * by pointers. 
     */
    public final String get(char[] value, int offset, int count) {
    	if (m_lock.tryLock()) {
    		// exclusive ownership of the cache
    		try {
    			return unsafeGet(value, offset, count);
    		}
    		finally {
    			m_lock.unlock();
    		}
    	}
    	else {
    		// some other thread is holding the cache. allocate from the heap.
       		return instantiate(value, offset, count);
    	}
    }

    /**
     * same as get() but may never get called from two threads concurrently
     */
    private final String unsafeGet(char[] value, int offset, int count) {
        if(value == null || count <= 0 || offset < 0 )
        {
        	return "";
        }
        
    	String rValue = null;
        int hash = calcHash(value, offset, count);
        int index = calcIndex(hash);

        if (_values[index] != null) {
            rValue = searchValueInHashChain(index, value, offset, count);
        }

        if (rValue == null) {
            _cacheMiss++;
            rValue = create(hash, index, value, offset, count);
        }
        
        if (c_logger.isTraceDebugEnabled()) {
            if((_cacheHit + _cacheMiss) % _maxSize == 0)
            {
                c_logger.traceDebug(this, "get", this.toString());
            }
        }
        return rValue;
    }

    /**
     * same as {@link #get(char[], int, int)} but accepts a byte array.
     * @param byteArray source byte array
     * @param offset offset into the byte array
     * @param count string size
     * @return the cached string
     */
    public final String get(byte[] byteArray, int offset, int count) {
    	char[] charArray = s_threadLocalCharArray.get();
    	if (charArray.length < count) {
    		// grow the thread-local copy
    		charArray = new char[count];
    		s_threadLocalCharArray.set(charArray);
    	}
    	// copy from byte array to char array
    	for (int i = 0; i < count; i++) {
    		byte b = byteArray[offset+i];
    		charArray[i] = (char)b;
    	}
    	// cache the char array
    	return get(charArray, 0, count);
    }

    /**
     * Search for the specified value at a specific index in the hash table.
     * 
     * @param index
     * @param value
     * @param offset
     * @param count
     * @return
     */
    private final String searchValueInHashChain(int index, char[] value,
                                                int offset, int count) {
        String rValue = null;
        HashBucket hBucket = _values[index];
        String cachedVal;
        boolean match;

        //Go through all buckets in the chain of this index
        while (hBucket != null && rValue == null) {
            cachedVal = hBucket._value;
            match = true;

            //Compare the length of string to avoid unnecessary compares
            if (cachedVal.length() == count) {

                int off = offset;
                //Both String equal in length compare content
                for (int i = 0; i < count; i++) {
                    if (cachedVal.charAt(i) != value[off++]) {
                        match = false;
                        break;
                    }
                }
            }
            else {
                match = false;
            }

            if (match) {
                rValue = cachedVal;

                //Move the last accesed value to be the first item in the lru list
                moveToTopOfLRUList(hBucket._LRUBucket);
                _cacheHit++;
            }
            else {

                //Still no match - move pointer to the next bucket in chain.
                hBucket = hBucket._next;
            }
        }

        return rValue;
    }

    /**
     * Move the specified LRU bucket to the top of the list.
     * 
     * @param current
     *            bucket to move in LRU
     */
    private final void moveToTopOfLRUList(LRUBucket current) {

        if (current == _LRURoot) {
            //Already first - nothing to do
            return;
        }

        LRUBucket previous = current._prev;
        LRUBucket next = current._next;

        previous._next = next;
        if (next != null) {
            next._prev = previous;
        }

        //if the 'current' bucket was the tail - the tail now is the 'previous'
        if (current == _LRUTail) {
            _LRUTail = previous;
            _LRUTail._next = null;
        }

        //The 'current' bucket is not out of the list. Put it back at the
        // begining of the list.
        current._next = _LRURoot;
        _LRURoot._prev = current;

        _LRURoot = current;
        current._prev = null;

        //dumpLRUList();
    }

    /**
     * Create a String object from the specified char array and add it to cache.
     */
    private final String create(int hash, int index, char[] value, int offset,
                                int count) {
        String rValue = instantiate(value, offset, count);

        HashBucket hBucket;
        LRUBucket lruBucket;
        if(_freeLRUBucket != null)
        {
            hBucket = _freeLRUBucket._hashBucket;
            hBucket._hash = hash;
            hBucket._value = rValue;
            hBucket._next = null;
            
            lruBucket = _freeLRUBucket;
            lruBucket._next = null;
            lruBucket._prev = null;
            _freeLRUBucket = null;
        }
        else
        {
            hBucket = new HashBucket(hash, rValue);
            lruBucket =  new LRUBucket(hBucket);
            hBucket._LRUBucket = lruBucket;
        }
        
        if (_values[index] != null) {
            //Push the firt bucket in chain one step forward.
            hBucket._next = _values[index];
        }

        //Add to hashtable
        _values[index] = hBucket;
        _size++;

        //Position the LRU bucket in the LRU list
        if (null == _LRURoot) {
            _LRURoot = lruBucket;
            _LRUTail = lruBucket;
        }
        else {
            //New bucket is first in the LRU list - push previous bucket one
            // step forward.
            lruBucket._next = _LRURoot;
            _LRURoot._prev = lruBucket;
            _LRURoot = lruBucket;

            //dumpLRUList();
        }

        if (_size > _maxSize) {
            removeLeastUsedValue();

        }

        return rValue;
    }

    /**
     * instantiates a new String from the heap
     * @param value string contents
     * @param offset offset into the array
     * @param count string length
     * @return the new string
     */
    private final String instantiate(char[] value, int offset,int count) {
        String string = new String(value, offset, count);
        if (INTERN) {
        	string = string.intern();
        }
        return string;
    }

    /**
     * Remove the least used value from cache.
     */
    private final void removeLeastUsedValue() {
        LRUBucket leastUsedB = _LRUTail;

        //Clear the pointers to the least used bucket in the LRU list
        leastUsedB._prev._next = null;
        _LRUTail = leastUsedB._prev;
        leastUsedB._prev = null;

        //Remove the bucket from the table of cached values
        int leastUsedIndex = calcIndex(leastUsedB._hashBucket._hash);
        HashBucket currentHBucket = _values[leastUsedIndex];

        if (currentHBucket == leastUsedB._hashBucket) {
            _values[leastUsedIndex] = null;
        }
        else {
            HashBucket prevBucket;
            while (currentHBucket != null) {
                prevBucket = currentHBucket;
                currentHBucket = currentHBucket._next;
                if (currentHBucket == leastUsedB._hashBucket) {
                    prevBucket._next = currentHBucket._next;
                    _cacheOverflow++;

                    break;
                }
            }
        }

        _size--;
        _freeLRUBucket = leastUsedB;
    }

    /**
     * Helper function for dumpting the LRU list to System.Out
     */
    public void dumpLRUList() {
        LRUBucket temp = _LRURoot;
        System.out.print("\nRoot ");
        while (temp != null) {
            System.out.print(" -> " + temp._hashBucket._value);
            temp = temp._next;
        }

        System.out.print("\nTail: ");
        temp = _LRUTail;
        while (temp != null) {
            System.out.print(" -> " + temp._hashBucket._value);
            temp = temp._prev;
        }

        System.out.println("");
    }

    /**
     * Helper function for dumping the content of the cache to System.out
     */
    public void dumpHash() {
        System.out.println("Cache contents: ");
        HashBucket hBucket;
        for (int i = 0; i < _values.length; i++) {

            if (_values[i] != null) {
                System.out.print(i + " : ");
                hBucket = _values[i];
                while (hBucket != null) {
                    System.out.print(" -> " + hBucket._value);
                    hBucket = hBucket._next;
                }
                System.out.println("");
            }
        }
    }

    /**
     * Calculate the index in the array of cached values.
     * 
     * @param hash
     * @return
     */
    private final int calcIndex(int hash) {
        return hash & (_values.length - 1);
    }

    /**
     * Calc the hash value for the specified char array.
     * 
     * @param value
     * @param offset
     * @param count
     * @return
     */
    private final int calcHash(char[] value, int offset, int count) {
        int h = 0;
        int off = offset;
        char val[] = value;
        int len = count;

        for (int i = 0; i < len; i++) {
            h = 31 * h + val[off++];
        }

        return h;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        double hitPercent = (int) (1000.0 * _cacheHit / (_cacheHit + _cacheMiss)) / 10.0;
        StringBuffer buffer = new StringBuffer();
        buffer.append("Cache size: ");
        buffer.append(_size);
        buffer.append('/');
        buffer.append(_maxSize);
        buffer.append(" ,");
        buffer.append("Cache Hit: ");
        buffer.append(_cacheHit);
        buffer.append(" ");
        buffer.append(hitPercent);
        buffer.append("%");
        buffer.append(" ,");
        buffer.append("Cache Overflow: ");
        buffer.append(_cacheOverflow);

        return buffer.toString();
    }

    /**
     * Gets the number of successful hits in cache.
     * 
     * @return
     */
    public int getHits() {
        return _cacheHit;
    }

    /**
     * Gets the number of get requests that were not located in cache and
     * resulted in a creation of a new entry.
     * 
     * @return
     */
    public int getMisses() {
        return _cacheMiss;
    }

    /**
     * Gets the cache's size - number of elements in the cache.
     * 
     * @return
     */
    public int getSize() {
        return _size;
    }
    
    /**
     * Gets the globally shared cache object. Applications can choose to create
     * their own local cache by accessing the public constructor. The shared
     * instance gets its size from configuration, if a config is not available
     * the default size of 1000 is used. 
     * @return
     */
    public static final LRUStringCache getInstance()
    {
        if(null == c_singletonCache) {
            int size = ApplicationProperties.getProperties().getInt(
            		StackProperties.LRU_STRING_CACHE);
            c_singletonCache = new LRUStringCache(size);
        }
        
        return c_singletonCache;
    }
}

/**
 * A bucket containing a single entry in the hashed set.
 *  
 */

final class HashBucket {
    /**
     * Construct a new hush bucket
     * 
     * @param hash
     * @param value
     */
    HashBucket(int hash, String value) {
        _hash = hash;
        _value = value;
    }

    /**
     * Hash value of object in bucket.
     */
    int _hash;

    /**
     * Object contained in bucket.
     */
    String _value;

    /**
     * Pointer to the next bucket in chain.
     */
    HashBucket _next;

    /**
     * Pointer to LRU bucket associated with this Hash bucket.
     */
    LRUBucket _LRUBucket;
}

/**
 * A bucket containing a single entry in the LRU list.
 *  
 */

final class LRUBucket {

    /**
     * Construct a new LRU bucket for the specified hash bucket
     * 
     * @param hBucket
     */
    LRUBucket(HashBucket hBucket) {
        _hashBucket = hBucket;
    }

    /**
     * Next bucket in LRU list.
     */
    LRUBucket _next;

    /**
     * Previous bucket in LRU list.
     */
    LRUBucket _prev;

    /**
     * Pointer to the bucket containing the actual value in the hashed set.
     */
    HashBucket _hashBucket;
}