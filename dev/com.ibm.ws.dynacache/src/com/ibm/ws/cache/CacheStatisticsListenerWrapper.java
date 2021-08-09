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

import com.ibm.ws.cache.intf.CacheStatisticsListener;
import com.ibm.wsspi.cache.CacheStatistics;

/**
 *  This is the impl class thats wraps cache statistics for each third party cache provider. 
 *  The CacheStatistics object provided by cache provider is used to retrieve the cache 
 *  statistics counter. There are some APIs that used to increment counters will not do any 
 *  operation in this wrapper. They are used by core cache of dynamic cache to increment the 
 *  statistics counters.
 *  The core cache of cache provider has its responsability to increment the statistics counters.
 */
public class CacheStatisticsListenerWrapper implements CacheStatisticsListener {

	private CacheStatistics cacheStatistics = null;
	
	public CacheStatisticsListenerWrapper(CacheStatistics cacheStatistics) {
		this.cacheStatistics = cacheStatistics;
	}
	
	public long getCacheHitsCount() {
		if (this.cacheStatistics != null) {
			return this.cacheStatistics.getCacheHitsCount();
		}
		return 0;
	}

	public long getMemoryCacheHitsCount() {
		if (this.cacheStatistics != null) {
			return this.cacheStatistics.getCacheHitsCount();
		}
		return 0;
	}

	public long getCacheLruRemovesCount() {
		if (this.cacheStatistics != null) {
			return this.cacheStatistics.getCacheLruRemovesCount();
		}
		return 0;
	}

	public long getCacheMissesCount() {
		if (this.cacheStatistics != null) {
			return this.cacheStatistics.getCacheMissesCount();
		}
		return 0;
	}

	public long getCacheRemovesCount() {
		if (this.cacheStatistics != null) {
			return this.cacheStatistics.getCacheRemovesCount();
		}
		return 0;
	}

	public long getExplicitInvalidationsFromMemoryCount() {
		if (this.cacheStatistics != null) {
			return this.cacheStatistics.getExplicitInvalidationsFromMemoryCount();
		}
		return 0;
	}

	public long getExplicitInvalidationsLocalCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getOverflowEntriesFromMemoryCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getTimeoutInvalidationsFromMemoryCount() {
		if (this.cacheStatistics != null) {
			return this.cacheStatistics.getTimeoutInvalidationsFromMemoryCount();
		}
		return 0;
	}

	public void reset() {
		if (this.cacheStatistics != null) {
			this.cacheStatistics.reset();
		}
	}

	public void resetMemory() {
		if (this.cacheStatistics != null) {
			this.cacheStatistics.reset();
		}
	}

	public void start() {
	}

	/************************************************************************
	 * The following get methods are used to support cache replication
	 ************************************************************************/
	
	public long getExplicitInvalidationsRemoteCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getRemoteInvalidationNotificationsCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getRemoteObjectFetchSizeCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getRemoteObjectHitsCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getRemoteObjectMissesCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getRemoteObjectUpdateSizeCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getRemoteObjectUpdatesCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getRemoteUpdateNotificationsCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	/************************************************************************
	 * The following get methods are used to support disk cache 
	 ************************************************************************/
	
	public long getDepIdBasedInvalidationsFromDiskCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getDepIdsOffloadedToDiskCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getDiskCacheHitsCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getExplicitInvalidationsFromDiskCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getGarbageCollectorInvalidationsFromDiskCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getObjectsAsyncLruToDiskCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getObjectsDeleteFromDisk4000KCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getObjectsDeleteFromDisk400KCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getObjectsDeleteFromDisk40KCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getObjectsDeleteFromDisk4KCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getObjectsDeleteFromDiskCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getObjectsDeleteFromDiskSizeCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getObjectsReadFromDisk4000KCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getObjectsReadFromDisk400KCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getObjectsReadFromDisk40KCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getObjectsReadFromDisk4KCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getObjectsReadFromDiskCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getObjectsReadFromDiskSizeCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getObjectsWriteToDisk4000KCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getObjectsWriteToDisk400KCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getObjectsWriteToDisk40KCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getObjectsWriteToDisk4KCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getObjectsWriteToDiskCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getObjectsWriteToDiskSizeCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getOverflowInvalidationsFromDiskCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getTemplateBasedInvalidationsFromDiskCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getTemplatesOffloadedToDiskCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public long getTimeoutInvalidationsFromDiskCount() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
		return 0;
	}

	public void resetDisk() {
		// TODO implement this counter if this counter is defined in CacheStatistics interface
	}

	/************************************************************************
	 * The following methods are used by Dynacache to increment the counters. 
	 * Do not implement these methods
	 ************************************************************************/
	public void cacheMiss(Object id) {
	}

	public void deleteEntryFromDisk(Object id, int valueSize) {
	}

	public void depIdBasedInvalidationsFromDisk(Object id) {
	}

	public void depIdsOffloadedToDisk(Object id) {
	}

	public void localCacheHit(Object id, int locality) {
	}

	public void objectsAsyncLruToDisk() {
	}

	public void overflowEntriesFromMemory() {
	}

	public void readEntryFromDisk(Object id, int valueSize) {
	}

	public void remoteInvalidationNotifications(Object id) {
	}

	public void remoteObjectHits(Object id, int valueSize) {
	}

	public void remoteObjectMisses(Object id) {
	}

	public void remoteObjectUpdates(Object id, int valueSize) {
	}

	public void remoteUpdateNotifications(int numNotifications) {
	}

	public void remoteUpdateNotifications(Object id) {
	}

	public void remove(Object id, int cause, int locality, int source) {
	}

	public void templateBasedInvalidationsFromDisk(Object id) {
	}

	public void templatesOffloadedToDisk(Object id) {
	}

	public void writeEntryToDisk(Object id, int valueSize) {
	}
}
