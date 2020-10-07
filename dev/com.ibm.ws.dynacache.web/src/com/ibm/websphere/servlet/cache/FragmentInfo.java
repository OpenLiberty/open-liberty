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
package com.ibm.websphere.servlet.cache;

import com.ibm.websphere.cache.EntryInfo;


/**
 * FragmentInfo extends the EntryInfo interface to add variables unique to externally cacheable pages.
 * <p>
 * EntryInfo and FragmentInfo Objects are attached to each cache entry.
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
 *     <li>The id. (set internally by WebSphere with the output of the IdGenerator.getId() method)
 *     <li>The priority.
 *     <li>The timeLimit and expirationTime options.
 *     <li>The dataIds option.
 *     <li>The external cache group to which this entry will be pushed (FragmentInfo)
 * </ul> </p>
 * @ibm-api 
 */
public interface FragmentInfo extends EntryInfo
{
    /**
     * This gets the externalCacheGroupId variable.
     *
     * @return The externalCacheGroupId.
     * @ibm-api 
     */
    public String getExternalCacheGroupId();
    /**
     * This sets the externalCacheGroupId variable.
     *
     * @param externalCacheGroupId The externalCacheGroupId.
     * @ibm-api 
     */
    public void setExternalCacheGroupId(String externalCacheGroupId);
    /**
     * This indicates whether the client set the external cache group id
     * in this FragmentInfo.
     *
     * @return True implies it was set.
     * @ibm-api 
     */
    public boolean wasExternalCacheGroupIdSet();
    
    /**
	 * This sets the store-attributes variable.  If storeAttributes is set
	 * to false then the request attributes will not be saved with the
	 * servlet response.  The default value is true.
	 * 
	 * @param b a boolean that indicates whether or not attributes should be
	 * saved.
     * @ibm-api 
     */
	public	void setStoreAttributes(boolean b);
	
	/**
	 * This indicates whether or not request attributes are being saved with the
	 * servlet response.
	 * 
	 * @return boolean true means that attributes will be saved
     * @ibm-api 
	 */
	public boolean getStoreAttributes();

    /**
     * This sets the store-cookies variable.  If storeCookies is set
     * to false then the cookies will not be saved with the
     * servlet response.  The default value is true.
     * 
     * @param b      a boolean that indicates whether or not cookies should be
     *               saved.
     * @ibm-api 
     */
	public	void setStoreCookies(boolean b);
	
	/**
	 * This indicates whether or not cookies are being saved with the
	 * servlet response.
	 * 
	 * @return boolean true means that cookies will be saved
     * @ibm-api 
	 */
	public boolean getStoreCookies();

    /**
     * This sets the consume-subfragments variable.  If consumeSubfragments is set
     * to true then the parent will consume child fragments in its cache entry.
     * The default value is false.
     * 
     * @param b      a boolean that indicates whether or not the parent will
     *               consume-subfragments
     * @ibm-api 
     */
	public void setConsumeSubfragments(boolean b);

	/**
	 * This indicates whether or not the parent is consuming child fragments in its cache entry.
	 * 
	 * @return boolean true means that the parent will consume child fragments
     * @ibm-api 
	 */
	public boolean getConsumeSubfragments();

	/**
	 * This sets the ignore-get-post variable.  If ignore-get-post is set
	 * to true then the requestType will automatically be appended to the cache-id
	 * 
	 * @param b a boolean that indicates whether or not to append the requestType to the cache-id
	 * saved.
	 * @ibm-api 
     	 */
	public void setIgnoreGetPost(boolean b);
	
	/**
	 * This indicates whether or not the requestType will automatically be appended to the cache-id
	 * 
	 * @return boolean true means that the requestType will not be appended to the cache-id
	 * @ibm-api 
	 */
	public boolean isIgnoreGetPost();
		
	/**
	 * This method returns the name of the cache-instance that will be used to store this fragment.
	 * 
	 * @return a String specifying the cache-instance name
	 * @ibm-api 
	 */
	 public String getInstanceName();
	 
}
