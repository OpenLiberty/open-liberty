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
package com.ibm.websphere.cache;

import java.util.*;

/**
 * A CacheEntry is a struct object that holds the
 * cache ID and value, as well as metadata for caching.
 * The information in these variables is obtained from the
 * EntryInfo object used when the entry was cached.
 * @ibm-spi
 * @ibm-api 
 */
public interface CacheEntry {

	/**
	 * Default type of a cache entry. All cache provider entries are of this type.
	 */
	public  static final int      CACHE_TYPE_DEFAULT  = 0; 
	
	/**
	 * This type of cache entry is is used internally by Dynamic Cache for 
	 * when caching webservices. Not to be used by cache providers.
	 */
	public  static final int      CACHE_TYPE_JAXRPC   = 1;
	
	/**
	 * Returns all templates that this entry depends on for invalidation.
	 * For a JSP/servlet, a template is the URI for this entry.
	 * <ul>
	 *     <li>For a top-level entry (eg, a JSP/Servlet that is
	 *         externally requested), this is obtained from the HTTP
	 *         request object's URL.  It can be set either by
	 *         the server or by the top-level JSP/Servlet.
	 *     <li>For a contained entry, this is the JSP/Servlet
	 *         file name URL (the parameter that would be used
	 *         in the callPage method).  It can be set either by
	 *         the JSP/Servlet it names or its containing JSP/Servlet,
	 *         plus other information from the request object.
	 * </ul>
	 *
	 * @return An Enumeration of the templates.
     * @ibm-api 
	 */
	public Enumeration getTemplates();

	/**
	 * Returns all IDs (cache IDs and data IDs) that this entry
	 * depends on for invalidation. Its elements are Strings.
	 * They are the identifiers used in the invalidateById methods
	 * to invalidate all cache entries having a dependency on these IDs.
	 * Data IDs must be unique within the same scope as cache IDs.
	 *
	 * @return An Enumeration of the IDs.
	 */
	public Enumeration getDataIds();

	/**
	 * Returns the creation time of this entry.
	 *
	 * @return The creation timestamp.
	 */
	public long getTimeStamp();

	/**
	 * @Override the default equals method.
	 * It compares cache IDs.
	 *
	 * @return True if they are equal.
	 */
	public boolean equals(Object object);

	/**
	 * @Override the default hashcode method of the cache entry.
	 *
	 * @return The hashCode of the cache ID.
	 */
	public int hashCode();

	/**
	 * Returns the maximum interval of time in seconds
	 * that the entry is allowed to stay in the cache.
	 * The entry may be discarded via LRU replacement prior to this time.
	 * A negative value indicates no time limit.
	 *
	 * @return The time limit.
	 */
	public int getTimeLimit();

	/**
	 * Returns the absolute time when the entry should expire.
	 * The entry may be discarded via LRU replacement prior to this time.
	 * A negative value indicates no expiration time.
	 *
	 * @return The expiration time.
	 */
	public long getExpirationTime();
	/**
	 * Returns the priority of this cache entry, which 
	 * determines how long the entry will stay in cache 
	 * when it is not being used.
	 * A larger priority gives an entry a longer time in the
	 * cache. 
	 * The value of priority should be based on the ratio of
	 * the cost of computing the entry to the cost of
	 * the memory in the cache (the size of the entry).
	 * The default is 1.
	 *
	 * @return This entry's priority.
	 */
	public int getPriority();
	/**
	 * Returns the sharing policy of this entry. In a 
	 * multi-JVM environment, this indicates whether the cache entry
	 * should be EntryInfo.NOT_SHARED, EntryInfo.SHARED_PUSH_PULL or
	 * EntryInfo.SHARED_PUSH.  The default is NOT_SHARED.
	 *
	 * @return The sharing policy.
	 */
	public int getSharingPolicy();

    /**
     * In a multi-JVM environment, this indicates whether updates to the
     * cache entry (when using EntryInfo.SHARED_PUSH) will be written out
     * immediately or in a batched, asynchronous fashion.
     * 
     * @deprecated The updates for Push or Push-Pull sharing policies are 
     *             always done in an asynchronous batch mode. It always
     *             returns true. 
     * 
     * @return True if batch is enabled, false otherwise.
     */
	public boolean isBatchEnabled();
	
	/**
	 * Returns the unique identifier of this cached entry.
	 * It must be unique within the scope of the group of Cache instances.
	 * Having this in the CacheEntry allows an entry obtained
	 * via the LRU array to know how to find it in the entryHashtable.
	 *
	 * @return The String ID of this CacheEntry.
	 */
	public String getId();

    /**
     * Returns the entry's value.
     * 
     * @return The enrty's value.
     */
	public Object getValue();

    /**
     * Returns the entry's value in a displayable format.
     * 
     * @return The entry's value.
     */
	public byte[] getDisplayValue();

	/**
	 * This mimics a cache Hit, refreshing an entries spot in the replacement algorithm.
	 */
	public void refreshEntry();

	/**
	 * Returns the alias list.
	 *
	 * @return The alias list.
	 */
	public Enumeration getAliasList();
	
	/**
	 * Returns the user metadata.
	 * 
	 * @return The user metadata.
	 */
	public Object getUserMetaData();

	/**
	 * Call the finish() method when you no longer need this
     * CacheEntry. This will allow any NIO buffers to be released
     * when the CacheEntry is removed from cache.
	 */
    public void finish();
    
    /**
     * Returns the best-effort size of the cache entry's value.
     * 
     * @return The best-effort determination of the size of the cache entry's value.  If the size
     * cannot be determined, the return value is -1;
     */

    public long getCacheValueSize();
    
    /**
     * Is this entry an invalid entry? 
     * 
     * An invalid entry is like a zombie i.e. the entry's age is beyond its validator expiration time 
     * and before its real expiration time.
     * Returns false if the validator expiration time was not set on the CacheEntry. 
     * Returns true if the validator expiration time < current time < real expiration time.   
     * 
     * @return boolean to indicate the entry invalid or not.
     */
    public boolean isInvalid();

    /**
     * Returns the time of the entry was first created.
     * 
     * @Return the time of the entry was first created. 
     */
    public long getCreationTime();
    
    /**
     * This method gets the unique identifier of this cached entry.
     * It must be unique within the scope of a Cache Instance
     *
     * @return The Object representation of the ID of this Cache Entry.
     */
    public Object getIdObject();
    
    /**
     * Returns the type of CacheEntry (CACHE_TYPE_DEFAULT or CACHE_TYPE_JAXRPC)
     * ALL implementors of this method other than Dynacache should return CACHE_TYPE_DEFAULT
     * for this method.
     *
     * @return cache type
     */
    public int getCacheType();
	
    /**
     * Returns the validator expiration time of the entry in the cache
     * The validator expiration time along with the expiration time
     * control the state of the entry in the cache.
     * 
     * @return long the current validator expiration time in milliseconds
     */
    public long getValidatorExpirationTime();
    
    /**
     * This method will be called before serialization of the CacheEntry. 
     * The implementor of this method returns a boolean if this CacheEntry
     * for some reason cannot be serialized
     * 
     * @return  true - The value was serialized
     *         false - The value can not be serialized.
     */
     public boolean prepareForSerialization();

    /**
     * @Override default implementation of CacheEntry
     * 
     * @return The string representation of the cache entry.
     */
    public String toString();
    
    /**
     * Returns the {@link ExternalCacheGroup} name this the cache entry belongs too.
     * 
     * @return null of this cache entry is not cached externally
     */
    public String getExternalCacheGroupId();
       
    
}