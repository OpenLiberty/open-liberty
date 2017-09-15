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
package com.ibm.wsspi.cache;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;


/**
 * This is the mechanism to provide cache
 * statistics to the CacheMonitor.
 * @ibm-spi
 */
public class CacheStatisticsListener {

	private static final long serialVersionUID = 8311300007167367823L;

	private static TraceComponent tc = Tr.register(CacheStatisticsListener.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

	private com.ibm.ws.cache.intf.CacheStatisticsListener cacheStatisticsListener = null;

	public CacheStatisticsListener(com.ibm.ws.cache.intf.CacheStatisticsListener csli){
		cacheStatisticsListener = csli;
	}

	/**
	 * This method resets all the statistics for memory cache.
	 */
	public final void resetMemory() {
		cacheStatisticsListener.resetMemory();
	}

	/**
	 * This method resets all the statistics for disk cache.
	 */
	public final void resetDisk() {
		cacheStatisticsListener.resetDisk();
	}

	/**
	 * This method returns the total number of cache hits.
	 * 
	 * @return The total number of cache hits.
	 */
	public long getNumGetValueHits() {
		return cacheStatisticsListener.getCacheHitsCount();
	}

	/**
	 * This method returns the total number of cache misses.
	 * 
	 * @return The total number of cache misses.
	 */
	public long getNumGetValueMisses() {
		return cacheStatisticsListener.getCacheMissesCount();
	}

	/**
	 * This method returns the total number of cache removes a Least Recently Used (LRU) algorithm.
	 * 
	 * @return The total number of cache removes by LRU algorithm.
	 */
	public long getNumLruRemoves() {
		return cacheStatisticsListener.getCacheLruRemovesCount();
	}

	/**
	 * This method returns the total number of cache removes.
	 * 
	 * @return The total number of cache removes.
	 */
	public long getNumRemoves() {
		return cacheStatisticsListener.getCacheRemovesCount();
	}

	/**
	 * This method returns the total number of garbage collector invalidations resulting 
	 * in the removal of entries from disk cache due to high threshold has been reached.
	 * 
	 * @return The total number of garbage collector invalidations.
	 */
	public long getNumGarbageCollectorInvalidationsFromDisk(){
		return cacheStatisticsListener.getGarbageCollectorInvalidationsFromDiskCount();
	}

	/**
	 * This method returns the total number of explicit invalidations resulting in the removal of entries from disk.
	 * 
	 * @return The total number of explicit invalidations.
	 */
	public long getNumExplicitInvalidationsFromDisk(){
		return cacheStatisticsListener.getExplicitInvalidationsFromDiskCount();
	}

	/**
	 * This method returns the total number of disk entries timeouts.
	 * 
	 * @return The total number of disk entries timeouts.
	 */
	public long getNumTimeoutInvalidationsFromDisk(){
		return cacheStatisticsListener.getTimeoutInvalidationsFromDiskCount();
	}

	/**
	 * This method returns the total number of invalidations resulting in the removal of 
	 * entries from disk due to exceeding the disk cache size or disk cache size in GB limit.
	 * 
	 * @return The total number of invalidations caused by disk overflow.
	 */
	public long getNumOverflowInvalidationsFromDisk(){
		return cacheStatisticsListener.getOverflowInvalidationsFromDiskCount();
	}

	public void setNumGetValueHits(long numGetValueHits) {
		if (tc.isDebugEnabled()) {
			Tr.debug(tc, "Cannot perform the operation setNumGetValueHits()");
		}
	}

	public void setNumGetValueMisses(long numGetValueMisses) {
		if (tc.isDebugEnabled()) {
			Tr.debug(tc, "Cannot perform the operation setNumGetValueMisses()");
		}
	}

	public void setNumLruRemoves(long numLruRemoves) {
		if (tc.isDebugEnabled()) {
			Tr.debug(tc, "Cannot perform the operation setNumLruRemoves()");
		}
	}

	public void setNumRemoves(long numRemoves) {
		if (tc.isDebugEnabled()) {
			Tr.debug(tc, "Cannot perform the operation setNumRemoves()");
		}
	}


}
