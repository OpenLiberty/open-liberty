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
package com.ibm.ws.cache;

import java.util.ArrayList;
import java.util.Iterator;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.util.AssertUtility;

/**
 * This class holds information about Dynamic Cache
 * cache instances.
 * The loading of this class is triggered by use of the cache.<br>
 */
public class CacheInstanceInfo {

	private static TraceComponent tc = Tr.register(CacheInstanceInfo.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
	private static boolean assertRanOnce= false;

	private static CacheServiceImpl     cacheService                = null;
	private static long                 lastPopulate                = 0;
	private static long                 populateInterval            = 3000;
	private static CacheInstanceInfo    cacheInstanceInfo           = null;
	private static CacheInstanceInfo[]  currentCacheInstanceInfoOut = null;

	//-----------------------------------------------------------------
	// Internal use - String names
	//-----------------------------------------------------------------
	static   ArrayList <String>  allFactory          = new ArrayList <String>();
	static   ArrayList <String>  allFile             = new ArrayList <String>();
	static   ArrayList <String>  allConfigured       = new ArrayList <String>();
	static   ArrayList <String>  allActive           = new ArrayList <String>();
	static   ArrayList <String>  objectConfigured    = new ArrayList <String>();
	static   ArrayList <String>  servletConfigured   = new ArrayList <String>();
	//-----------------------------------------------------------------

	//-----------------------------------------------------------------
	// For Performance Advisor use
	//-----------------------------------------------------------------

	/**
	 * The name is the cache instance.
	 */
	private String   cacheName       = null;
	/**
	 * True if the instance was created using a factory.
	 */
	private boolean  isFactory       = false;
	/**
	 * True if the instance was created using a properties file.
	 */
	private boolean  isFile          = false;
	/**
	 * True if the instance has been instantiated.
	 */
	private boolean  isActive        = false;
	/**
	 * True if the instance is an object cache instance.
	 */
	private boolean  isObjectCache   = false;
	/**
	 * True if the instance is a servlet cache instance.
	 */
	private boolean  isServletCache  = false;
	//-----------------------------------------------------------------

	//-----------------------------------------------------------------
	// For Performance Advisor use
	//-----------------------------------------------------------------
	/**
	 * Returns cache name.
	 */
	public String getCacheName() {
		return this.cacheName;
	}
	//-----------------------------------------------------------------

	//-----------------------------------------------------------------
	// For Performance Advisor use
	//-----------------------------------------------------------------
	/**
	 * Returns true if the instance was created using a factory.
	 */
	public boolean isFactory() {
		return this.isFactory;
	}
	//-----------------------------------------------------------------

	//-----------------------------------------------------------------
	// For Performance Advisor use
	//-----------------------------------------------------------------
	/**
	 * Returns true if the instance was created using a properties file.
	 */
	public boolean isFile() {
		return this.isFile;
	}
	//-----------------------------------------------------------------

	//-----------------------------------------------------------------
	// For Performance Advisor use
	//-----------------------------------------------------------------
	/**
	 * Returns true if the instance has been instantiated.
	 */
	public boolean isActive() {
		return this.isActive;
	}
	//-----------------------------------------------------------------

	//-----------------------------------------------------------------
	// For Performance Advisor use
	//-----------------------------------------------------------------
	/**
	 * Return true if the instance is an object cache instance.
	 */
	public boolean isObjectCache() {
		return this.isObjectCache;
	}
	//-----------------------------------------------------------------

	//-----------------------------------------------------------------
	// For Performance Advisor use
	//-----------------------------------------------------------------
	/**
	 * Return true if the instance is a servlet cache instance.
	 */
	public boolean isServletCache() {
		return this.isServletCache;
	}
	//-----------------------------------------------------------------

	//-----------------------------------------------------------------
	//
	//-----------------------------------------------------------------
	private CacheInstanceInfo() {

		final String methodName = "<init>";
		assert assertRanOnce=AssertUtility.assertCheck(assertRanOnce, this);
		if (tc.isEntryEnabled())
			Tr.entry(tc, methodName );

		if (tc.isEntryEnabled())
			Tr.exit(tc, methodName+ " cacheService=" + cacheService );
	}
	//-----------------------------------------------------------------

	static synchronized CacheInstanceInfo [] getCacheInstanceInfo( CacheServiceImpl cacheService ) {
		CacheInstanceInfo infoOut [] = currentCacheInstanceInfoOut;
		if ( System.currentTimeMillis() - lastPopulate > populateInterval ) {
			if (cacheService!=null) {
				if ( cacheInstanceInfo == null ) {
					cacheInstanceInfo = new CacheInstanceInfo();
				}
				cacheService.populateCacheInstanceInfo( cacheInstanceInfo );
				CacheInstanceInfo.cacheService = cacheService;
				infoOut = new CacheInstanceInfo[allConfigured.size()];
				Iterator i = allConfigured.iterator();
				int x=0;
				while ( i.hasNext() ) {
					String cacheName =  (String)i.next();
					CacheInstanceInfo info = new CacheInstanceInfo();

					info.cacheName      = cacheName;
					info.isActive       = CacheInstanceInfo.allActive.contains(cacheName);
					info.isFactory      = CacheInstanceInfo.allFactory.contains(cacheName);
					info.isFile         = CacheInstanceInfo.allFile.contains(cacheName);
					info.isObjectCache  = CacheInstanceInfo.objectConfigured.contains(cacheName);
					info.isServletCache = CacheInstanceInfo.servletConfigured.contains(cacheName);

					infoOut[x++] = info;
				}
				currentCacheInstanceInfoOut = infoOut;
				CacheInstanceInfo.allConfigured.clear();
				CacheInstanceInfo.allActive.clear();
				CacheInstanceInfo.allFactory.clear();
				CacheInstanceInfo.allFile.clear();
				CacheInstanceInfo.objectConfigured.clear();
				CacheInstanceInfo.servletConfigured.clear();

			}
			lastPopulate = System.currentTimeMillis();
		}
		return infoOut;
	}
	//-----------------------------------------------------------------

}


