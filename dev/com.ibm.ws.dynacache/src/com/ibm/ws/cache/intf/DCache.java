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
package com.ibm.ws.cache.intf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.cache.ChangeListener;
import com.ibm.websphere.cache.InvalidationListener;
import com.ibm.websphere.cache.PreInvalidationListener;
import com.ibm.websphere.cache.exception.DynamicCacheException;
import com.ibm.ws.cache.BatchUpdateDaemon;
import com.ibm.ws.cache.EntryInfo;
import com.ibm.ws.cache.CacheEntry;
import com.ibm.ws.cache.FreeLruEntryResult;
import com.ibm.ws.cache.InvalidationAuditDaemon;
import com.ibm.ws.cache.RemoteServices;
import com.ibm.ws.cache.TimeLimitDaemon;
import com.ibm.ws.cache.stat.CachePerf;
import com.ibm.ws.cache.stat.CachePerfFactory;
import com.ibm.wsspi.cache.CacheStatistics;
import com.ibm.wsspi.cache.EventSource;

/**
 * This is the MOTHER interface for all caches. Each cache instance be it a third party cache provider like OG or the
 * default Dynamic Cache Provider implements this interface.
 * 
 * All the CacheEntries returned using the methods of this interface belong to the com.ibm.websphere.cache.CacheEntry
 * class.
 * 
 * ALL Dynacache internal code uses DCache as an abstraction to the cache. This abstraction shields the impl from the
 * different type of cache provider implementations.
 * 
 * All the testcases in dynacache.fvt should ideally use the DCache interface rather than the com.ibm.ws.Cache
 * interface. dynacache.fvt should use com.ibm.websphere.CacheEntry rather than com.ibm.ws.CacheEntry.
 * 
 */
public interface DCache extends com.ibm.websphere.cache.Cache, com.ibm.websphere.cache.CacheLocal {

    public String getCacheName();

    public DCacheConfig getCacheConfig();

    public CacheStatistics getCacheStatistics();

    public CacheStatisticsListener getCacheStatisticsListener();

    public void updateStatisticsForVBC(com.ibm.websphere.cache.CacheEntry cacheEntry, boolean directive);

    public void setBatchUpdateDaemon(BatchUpdateDaemon batchUpdateDaemon);

    public void setTimeLimitDaemon(TimeLimitDaemon timeLimitDaemon);

    public void setInvalidationAuditDaemon(InvalidationAuditDaemon iad);

    public void setRemoteServices(RemoteServices remoteServices);

    public RemoteServices getRemoteServices();

    public CachePerf getCachePerf();

    public int getDefaultPriority();

    public boolean containsCacheId(Object cacheId);

    public Object getValue(String id, boolean askPermission);

    public Object getValue(Object id, boolean askPermission);

    public Object getValue(com.ibm.websphere.cache.EntryInfo ei, boolean askPermission);

    public Object getValue(Object id, String template, boolean askPermission, boolean ignoreCounting);

    public com.ibm.websphere.cache.CacheEntry getEntry(String id);

    public com.ibm.websphere.cache.CacheEntry getEntry(Object id);

    public com.ibm.websphere.cache.CacheEntry getEntryFromMemory(Object id);

    public com.ibm.websphere.cache.CacheEntry getEntry(com.ibm.websphere.cache.EntryInfo ei);

    public com.ibm.websphere.cache.CacheEntry getEntry(com.ibm.websphere.cache.EntryInfo ei, boolean checkAskPermission);

    public com.ibm.websphere.cache.CacheEntry getEntry(com.ibm.websphere.cache.EntryInfo ei, boolean checkAskPermission, boolean ignoreCounting);

    public com.ibm.websphere.cache.CacheEntry getEntry(Object id, int source, boolean ignoreCounting, boolean incrementRefCount);

    public void setValue(EntryInfo entryInfo, Object value);

    public void setValue(EntryInfo entryInfo, Object value, boolean directive);

    public void setValue(EntryInfo entryInfo, Object value, boolean coordinate, boolean directive);

    public com.ibm.websphere.cache.CacheEntry setEntry(CacheEntry cacheEntry);

    public com.ibm.websphere.cache.CacheEntry setEntry(CacheEntry cacheEntry, int source);

    public com.ibm.websphere.cache.CacheEntry setEntry(CacheEntry cacheEntry, int source, boolean ignoreCounting, boolean coordinate,
            boolean incRefcount);

    public Object invalidateAndSet(EntryInfo ei, Object value, boolean coordinate);

    public void refreshEntry(com.ibm.websphere.cache.CacheEntry cacheEntry);

    public boolean isValid(String id);

    public void invalidateById(String id, boolean waitOnInvalidation);

    public void invalidateById(Object id, boolean waitOnInvalidation);

    public void invalidateById(Object id, boolean waitOnInvalidation, boolean checkPreInvalidationListener);

    public void invalidateById(Object id, int causeOfInvalidation, boolean waitOnInvalidation);

    public void invalidateById(Object id, int causeOfInvalidation, boolean waitOnInvalidation, boolean checkPreInvalidationListener);

    public void invalidateById(Object id, int causeOfInvalidation, int sourceOfInvalidation, boolean waitOnInvalidation,
            boolean invokeInternalInvalidateById);

    public void invalidateById(Object id, int causeOfInvalidation, int sourceOfInvalidation, boolean waitOnInvalidation,
            boolean invokeInternalInvalidateById, boolean invokeDRSRenounce);

    public void internalInvalidateByDepId(Object id, int causeOfInvalidation, int source, boolean bFireIL);

    public void invalidateByTemplate(String template, boolean waitOnInvalidation);

    public void clear(boolean waitOnInvalidation);

    public void clear();

    public int getMaxNumberCacheEntries();

    public int getNumberCacheEntries();

    public int getNumberCacheEntriesUnsynchronized();

    public Enumeration getAllIds();

    public Set getCacheIds();

    public Set getCacheIdsByDependency(String dependency);

    public Set getCacheIdsByDependency(Object dependency);

    public Set getCacheIdsByTemplate(String template);

    public Collection getAllDependencyIds();

    public void start();

    public void stop();

    public void trimCache();

    public FreeLruEntryResult freeLruEntry();

    public void addAlias(Object key, Object[] aliasArray, boolean askPermission, boolean coordinate);

    public void removeAlias(Object alias, boolean askPermission, boolean coordinate);

    public boolean getSwapToDisk();

    public void setSwapToDisk(boolean enable);

    public int getDiskCacheHashcode(boolean debug, boolean includeValue) throws DynamicCacheException;

    public Set getIdsByRangeDisk(int index, int length);

    public Set getDepIdsByRangeDisk(int index, int length);

    public Set getTemplatesByRangeDisk(int index, int length);

    public com.ibm.websphere.cache.CacheEntry getEntryDisk(Object cacheId);

    public Set getCacheIdsByDependencyDisk(Object depId);

    public Set getCacheIdsByTemplateDisk(String template);

    public int getIdsSizeDisk();

    public int getActualIdsSizeDisk();

    public long getCacheSizeInBytesDisk();

    public int getDepIdsSizeDisk();

    public int getTemplatesSizeDisk();

    public int getPendingRemovalSizeDisk();

    public int getDepIdsBufferedSizeDisk();

    public int getTemplatesBufferedSizeDisk();

    public void clearDisk();

    public void invokeDiskCleanup(boolean scan);

    public boolean isDiskCleanupRunning();

    public boolean isDiskInvalidationBufferFull();

    public float getDiskCacheSizeInMBs();

    public void releaseDiskCacheUnusedPools();

    public boolean containsKeyDisk(Object key);

    public Exception getDiskCacheException();

    public void addToTimeLimitDaemon(Object id, long expirationTime, int inactivity);

    public void setEnableDiskCacheSizeInBytesChecking(boolean enableDiskCacheSizeInBytesChecking);

    public EventSource getEventSource();

    public boolean enableListener(boolean enable);

    public boolean isEnableListener();

    public boolean addInvalidationListener(InvalidationListener listener);

    public boolean removeInvalidationListener(InvalidationListener listener);

    public boolean addPreInvalidationListener(PreInvalidationListener listener);

    public boolean removePreInvalidationListener(PreInvalidationListener listener);

    public boolean addChangeListener(ChangeListener listener);

    public boolean removeChangeListener(ChangeListener listener);

    public void refreshCachePerf();

    public void resetPMICounters();

    public int getMemoryCacheHashcode(boolean debug, boolean includeValue);

    public int getCacheIdsHashcodeInPushPullTable(boolean debug);

    public boolean shouldPull(int share, Object id);

    public int getPushPullTableSize();

    public List getCacheIdsInPushPullTable();

    public boolean isCacheSizeInMBEnabled();

    public void disableCacheSizeInMB();

    public void increaseCacheSizeInBytes(long size, String msg);

    public void decreaseCacheSizeInBytes(long size, String msg);

    public float getCurrentMemoryCacheSizeInMB();

    // called by DRSMessageListener -> CacheUnitImpl and BatchUpdateDaemon
    public void batchUpdate(HashMap invalidateIdEvents, HashMap invalidateTemplateEvents, ArrayList pushEntryEvents);

    public boolean hasPushPullEntries();

    public void setCachePerf(CachePerfFactory factory);
}