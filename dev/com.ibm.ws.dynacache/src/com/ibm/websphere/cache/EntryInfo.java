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
 * EntryInfo and FragmentInfo objects contain metadata for caching and are attached to each cache entry.
 * IdGenerators and MetaDataGenerators use these interfaces to define the
 * caching metadata for an entry.
 * <P>
 * Typically a Id/MetaDataGenerator will get an entry's FragmentInfo object
 * from the ServletCacheRequest, and use the object's set methods to configure
 * that entry.
 * <p>
 * The following is a summary  of the caching metadata for a CacheEntry:
 * <ul>
 *     <li>The template. (set internally by WebSphere)
 *     <li>The ID. (set internally by WebSphere with the output of the IdGenerator.getId() method)
 *     <li>The priority.
 *     <li>The timeLimit and expirationTime options.
 *     <li>The dataIds option.
 *     <li>The external cache group to which this entry will be pushed (FragmentInfo)
 * </ul> </p>
 * @ibm-api 
 */
public interface EntryInfo
{
    /**
     * The entry is kept local to the JVM that executed the entry's JSP
     * or command instead of shared across all JVMs.
     * This option is useful when there is affinity between a client
     * and web application server and the data is only used by that client
     * (e.g., a shopping cart).
     */
    public static final int NOT_SHARED = 1;

    /**
     * The entry is shared across multiple JVMs; the entry  
     * is pushed to all JVMs after its JSP or command is
     * executed instead of waiting for the JVMs to pull it.
     * Pushing these entries is delayed for a short time
     * (which is configurable) to exploit the efficiency of batching
     * several entries in one message.
     * This option is useful when the entry is very heavily used by
     * all clients (e.g., a popular product display).
     */
    public static final int SHARED_PUSH = 2;

    /**
     * The entry is shared across multiple JVMs;
     * other JVMs get it by pulling it when needed and then storing
     * it in its local cache for further requests.
     * This option is useful when the entry is shared by all clients
     * but is not accessed often enough between invalidations to
     * warrant pushing it to other (e.g., a not-so-popular product display).
     * 
     * @deprecated Share type PULL should not be used in new code development.
     *             Use share type PUSH_PULL instead.  Share type PULL, if
     *             used in existing code, will function like share type
     *             PUSH_PULL.
     */
    public static final int SHARED_PULL = 3;

    /**
     * The entry is shared across multiple JVMs; the ID of the entry
     * is pushed on initial creation (execution of JSP/Servlet or command)
     * and stored in the other JVMs.
     * If the actual entry is requested, other JVMs first look to see if the
     * ID has been broadcasted previously before making a remote request for it.
     */
    public static final int SHARED_PUSH_PULL = 4;


    /**
     * Returns the string representation of cache ID.
     *
     * @return The string representation of cache ID..
     */
    public String getId();

    /**
     * Returns the object representation of cache ID.
     *
     * @return The object representation of cache ID.
     */
    public Object getIdObject();

    /**
     * Sets the cache ID.
     *
     * @param id The cache ID.
     */
    public void setId(String id);

    /**
     * Determines whether updates (when sharing is PUSH) are sent
     * immediately or in an asynchronous batch fashion
     * 
     * @deprecated The updates for Push or Push-Pull sharing policies are 
     *             always done in an asynchronous batch mode. It always
     *             returns true. 
     * 
     * @return True if updates are done in a batch
     */
    public boolean isBatchEnabled();

    /**
     * Sets whether updates (when sharing is PUSH) are sent
     * immediately or in an asynchronous batch fashion
     * 
     * @deprecated The updates for Push or Push-Pull sharing policies are 
     *             always done in an asynchronous batch mode. Calling 
     *             setBatchEnabled(false) has no effect on cache replication. 
     *
     * @param flag true to enable batch updates, false otherwise.
    */
    public void setBatchEnabled(boolean flag);

    /**
     * Returns the sharing policy in the sharingPolicy variable.
     *
     * @return The sharing policy.
     * @see EntryInfo
     */
    public int getSharingPolicy();

    /**
     * Determine whether persist-to-disk is true. 
     *
     * @return True if this entry persists to disk.
     */
    public boolean getPersistToDisk();

    /**
     * Sets the sharing policy.
     *
     * @param policy The sharing policy.
     * @see EntryInfo
     */
    public void setSharingPolicy(int policy);

    /**
     * Sets the persist-to-disk. If disk cache offload is enabled and persist-to-disk is true,
     * the entry will be offloaded to the disk. 
     *
     * @param persistToDisk The persist-to-disk.
     */
    public void setPersistToDisk(boolean persistToDisk);

    /**
     * Determines whether the sharingPolicy is EntryInfo.NOT_SHARED.
     *
     * @return True indicates that the sharingPolicy is EntryInfo.NOT_SHARED.
     */
    public boolean isNotShared();
    
    /**
     * Determines whether the sharingPolicy is EntryInfo.SHARED_PUSH.
     *
     * @return True indicates that the sharingPolicy is EntryInfo.SHARED_PUSH or EntryInfo.SHARED_PUSH_PULL.
     */
    public boolean isSharedPush();
    
    /**
     * Determines whether the sharingPolicy is EntryInfo.SHARED_PULL.
     *
     * @return True indicates that the sharingPolicy is EntryInfo.SHARED_PULL or EntryInfo.SHARED_PUSH_PULL.
     */
    public boolean isSharedPull() ;
    
    /**
     * Returns the time limit.
     *
     * @return The time limit.
     */
    public int getTimeLimit();
    
    /**
     * Assigns the time limit. Once an entry is cached,
     * it will remain in the cache for this many seconds
     *
     * @param timeLimit The time limit.
     */
    public void setTimeLimit(int timeLimit);
    
    /**
     * Returns the inactiviy timer.
     *
     * @return The inactivity timer.
     */
    public int getInactivity(); 
    
    /**
     * Assigns the inactivity timer. Once an entry is cached,
     * it will remain in the cache for this many seconds if not accessed.
     *
     * @param inactivity This inactivity timer.
     */
    public void setInactivity(int inactivity);
    
    /**
     * Returns the expiration time.
     *
     * @return The expiration time.
     */
    public long getExpirationTime();

    /**
     * Assigns new expiration time.
     *
     * @param expirationTime The new expiration time.
     */
    public void setExpirationTime(long expirationTime);

    /**
     * Returns the priority.
     *
     * @return The priority.
     */
    public int getPriority();
    /**
     * Assigns the new priority.
     *
     * @param priority The new priority.
     */
    public void setPriority(int priority);

    /**
     * Returns the templates set on this entry info.
     *
     * @return An Enumeration of the template names.
     */
    public Enumeration getTemplates();

    /**
     * Returns one of the templates set on this entry info.
     *
     * @return A template name.
     */
    public String getTemplate();

    /**
     * Adds a template.
     *
     * @param template The new Template name.
     */
    public void addTemplate(String template);

    /**
     * Returns the data IDs set on this entry info.
     *
     * @return The Enumeration of data IDs.
     */
    public Enumeration getDataIds();
    /**
     * Adds a new data ID.
     *
     * @param dataId The new data ID.
     */
    public void addDataId(String dataId);

	/**
	 * Returns the alias IDs set on this entry info.
	 *
	 * @return The Enumeration of alias IDs.
	 */
	public Enumeration getAliasList();

	/**
	 * Adds a new alias ID.
	 *
	 * @param alias The new alias ID.
	 */
	public void addAlias(Object alias);

	/**
	 * Returns the userMetaData.
	 *
	 * @return The userMetaData.
	 */
	public Object getUserMetaData();
	
	/**
	 * Assigns the new userMetaData.
	 *
	 * @param userMetaData The new userMetaData.
	 */
	public void setUserMetaData(Object userMetaData);
		
    /**
     * Returns the validator expiration time of the entry in the cache
     * The validator expiration time along with the expiration time
     * control the state of the entry in the cache.
     * 
     * @return long the current validator expiration time in milliseconds
     * @ibm-api 
     */
    public long getValidatorExpirationTime();
    
	/**
	 * Returns cache type (CACHE_TYPE_DEFAULT or CACHE_TYPE_JAXRPC)
	 *
	 * @return cache type
     * @ibm-api 
	 */
	public int getCacheType();
	
}
