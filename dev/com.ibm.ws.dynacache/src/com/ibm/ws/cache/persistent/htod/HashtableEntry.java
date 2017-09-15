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
package com.ibm.ws.cache.persistent.htod;

/**
 * Collision list entry.
 */
public class HashtableEntry {

	long location = 0;									// Location of structure on disk
	long first_created = 0;
	long expiration = -1;                               // RET
	long validatorExpiration = -1;                      // VET < t < RET  ==> invalid; t < VET ==> valid

	Object key = null;									// The key, duh.
	Object value = null;								// Ptr to memory object
	int    valuelen = -1;  							    // Optional length if value is byte[]

	long next = 0;										// disk address of next entry

	long previous = 0;									// address of previous

	int hash = 0;										// Hash value for this entry
	int index = 0;
	int tableid = 0;

    byte[] serializedKey        = null;                 // serialized key - used when writeEntry()
    byte[] serializedCacheValue = null;                 // serialized value in CE
    int cacheValueSize = 0;
    int cacheValueHashcode = 0;                         // hashcode for cache value   // LI4337-17
    int size = -1;                                      // size (key size + CE size + cacheValue size if any) 
    boolean bAliasId = false;                           // is alias Id
    boolean bValidHashcode = true;                      // is valid hashcode for value  // LI4337-17

	public HashtableEntry() {
	}

	public Object getKey() { 
		return key;
	}

	public Object getValue() {
		return value;
	}

	public long expirationTime() {
		return this.expiration;
	}

	public long validatorExpirationTime() {
		return this.validatorExpiration;
	}

	public long firstCreated() {
		return first_created;
	}

	public int valueLength() {
		return valuelen;
	}

	public boolean isExpired() {
		if (this.expiration <= 0) {
			return false;
		}
        return( (System.currentTimeMillis() -  this.expiration) >= 0);
	}

    public boolean isAliasId() {
        return this.bAliasId;
    }

    public boolean isValidHashcodeForValue() {  // LI4337-17
        return this.bValidHashcode;
    }

	public byte[] getSerializedCacheValue() {
		return this.serializedCacheValue;
	}

	public int getCacheValueSize() {
		return this.cacheValueSize;
	}

	public int getCacheValueHashcode() {  // LI4337-17
		return this.cacheValueHashcode;
	}

	public int size() {
		return this.size;
	}

	/**
	 * resets this HashtableEntry for reuse
	 */
	public void reset() {
		this.location = 0;
		this.first_created = 0;
		this.expiration = -1;
		this.validatorExpiration = -1;  // LI4537-24
		this.tableid = 0;
		this.key = null;
		this.value = null;
		this.next = 0;
		this.previous = 0;
		this.valuelen = -1;
		this.hash = 0;
		this.index = 0;
        this.size = -1;
        this.serializedKey        = null;
        this.serializedCacheValue = null;
        this.cacheValueSize = 0;
        this.cacheValueHashcode = 0;  // LI4337-17
        this.bAliasId = false;
        this.bValidHashcode = true;  // LI4337-17
	}

	/**************************************************************************
	 * After reading entry header and data from disk, this method will used to create the HashTableEntry.
	 *  
	 * @param location              disk location of the object (seek value)
	 * @param first_created         time object was first inserted into the hashtable.
	 * @param key                   The object's key
	 * @param value                 The object's value
	 * @param next                  disk location of next object in hash chain
	 * @param previous              disk location of previous object in hash chain
	 * @param tablesize             number of objects in the table.  
	 * @param tableid               instance id of the hashtable. Used to support multiple
	 *                              htods in the same physical file.
	 * @param length                the length of the serialized value
	 * @param expirationTime        when object becomes stale (if set when writing object,
	 *                              unused otherwise)
	 * @param validatorExpirationTime  when object becomes invalid (if set when writing object,
	 *                              unused otherwise)
     * @param size                  size of the object (key size + data size + attachement size)
     * @param serializedKey         the object's serialized key (skip serialization of the key)
     * @param serializedCacheValue  the object's serialized attachment
     * @param cacheValueSize        the size of cache value
     * @param cacheValueHashcode    the hashcode for cache value
     * @param isAliasId             indicate whether it is alias id
     * @param isValidHashcode       indicate whether the hashcode for cache value is valid or not
	 *************************************************************************/
	public void copy(long location, long first_created,
					 Object key, Object value,
					 long next, long previous, int tablesize, int tableid, int valuelen,
					 long expirationTime, long validatorExpirationTime, 
					 int size, byte[] serializedCacheValue, 
					 int cacheValueSize, int cacheValueHashcode,
					 boolean isAliasId, boolean isValidHashcode) {  // LI4337-17
		this.location = location;
		this.first_created = first_created;
		this.next = next;
		this.previous = previous;
		this.key = key;
		this.value = value;
		this.valuelen = valuelen;
		this.expiration = expirationTime;
		this.validatorExpiration = validatorExpirationTime;  // LI4537-24
		this.tableid = tableid;
        this.hash = key.hashCode();
        this.index = (hash & 0x7FFFFFFF ) % tablesize;
        this.size = size;
        this.serializedCacheValue = serializedCacheValue;
        this.cacheValueSize = cacheValueSize;
        this.cacheValueHashcode = cacheValueHashcode;  // LI4337-17
        this.bAliasId = isAliasId;
        this.bValidHashcode = isValidHashcode;  // LI4337-17
	}

	/**************************************************************************
	 * Before writing data to the disk, this method is used to create a HashtableEntry.
	 * 
	 * @param key                   The object's key
	 * @param value                 The object
	 * @param tablesize             number of objects in the table.  
	 * @param tableid               instance id of the hashtable. Used to support multiple
	 *                              htods in the same physical file.
	 * @param valuelen              the length of the value
	 * @param prevous               disk location of previous object in hash chain
	 * @param expirationTime        when object becomes stale (if set when writing object,
	 *                              unused otherwise)
	 * @param validatorExpirationTime  when object becomes invalid (if set when writing object,
	 *                              unused otherwise)
     * @param serializedKey         the object's serialized key (skip serialization of the key)
     * @param serializedCacheValue  the object's serialized attachment
     * @param cacheValueHashcode    the hashcode for cache value
     * @param isAliasId             indicate whether it is alias id
	 *************************************************************************/
	public void copy(Object key, Object value, int tablesize, int tableid, int valuelen, 
					 long previous, long expirationTime, long validatorExpirationTime,
					 byte[] serializedKey, byte[] serializedCacheValue,
					 int cacheValueHashcode, boolean isAliasId)  // LI4337-17
	{
		this.key = key;
		this.value = value;
		this.valuelen = valuelen;
		this.previous = previous;
		this.expiration = expirationTime;
		this.validatorExpiration = validatorExpirationTime;  // LI4537-24
		this.tableid = tableid;
		this.first_created = System.currentTimeMillis();
		this.hash = key.hashCode();
		this.index = (hash & 0x7FFFFFFF ) % tablesize;
        this.serializedKey = serializedKey;
        this.serializedCacheValue = serializedCacheValue;
        this.cacheValueHashcode = cacheValueHashcode;  // LI4337-17
        this.bAliasId = isAliasId;
	}
}



