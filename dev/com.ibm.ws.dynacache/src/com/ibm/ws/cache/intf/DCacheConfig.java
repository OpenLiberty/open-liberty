/*******************************************************************************
 * Copyright (c) 1997, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.intf;

/**
 * This is the interface to get the cache configuration parameters.
 * It is used by internal code (servlet, DRS, etc). The DCacheConfig
 * is accessed by DCache.getCacheConfig().
 */
public interface DCacheConfig extends com.ibm.wsspi.cache.CacheConfig {

    public int getBatchUpdateInterval();

    @Override
    public String getCacheName();

    public int getCachePriority();

    public int getCacheSize();

    public int getCongestionSleepTimeMilliseconds();

    public int getCleanupFrequency();

    public int getDefaultShareType();

    public int getDelayOffloadDepIdBuckets();

    public int getDelayOffloadEntriesLimit();

    public int getDelayOffloadTemplateBuckets();

    public int getDiskCacheEntrySizeInMB();

    public int getDiskCacheEvictionPolicy();

    public int getDiskCacheHighThreshold();

    public int getDiskCacheLowThreshold();

    public int getDiskCachePerformanceLevel();

    public int getDiskCacheSize();

    public int getDiskCacheSizeInGB();

    public int getEntryWindow();

    public int getInvalidateEntryWindow();

    public int getInvalidatePercentageWindow();

    public int getLruToDiskTriggerPercent();

    public int getLruToDiskTriggerTime();

    public int getPercentageWindow();

    public String getServerNodeName();

    public String getServerServerName();

    public int getReplicationPayloadSizeInMB();

    public String getCacheProviderName();

    public boolean isCacheInstanceStoreCookies();

    public boolean isCascadeCachespecProperties();

    public boolean isDelayOffload();

    public boolean isDisableDependencyId();

    public boolean isDrsBootstrapEnabled();

    public boolean isDrsDisabled();

    public boolean isEnableCacheReplication();

    public boolean isEnableDiskOffload();

    public boolean isEnableNioSupport();

    public boolean isEnableServletSupport();

    public boolean isFilterLRUInvalidation();

    public boolean isFilterTimeOutInvalidation();

    public boolean isFilterInactivityInvalidation();

    public boolean isFlushToDiskOnStop();

    public boolean isUseServerClassLoader();

    public void setCacheInstanceStoreCookies(boolean cacheInstanceStoreCookies);

    public void setCachePriority(int cachePriority);

    public void setDrsBootstrapEnabled(boolean drsBootstrapEnabled);

    public void setDrsDisabled(boolean drsDisabled);

    public boolean isAutoFlushIncludes();

    public boolean alwaysSetSurrogateControlHdr();

    public boolean isDefaultCacheProvider();

    public boolean isUse602RequiredAttrCompatibility();

    public boolean alwaysTriggerCommandInvalidations();

    public boolean isEnableInterCellInvalidation();

    public boolean alwaysSynchronizeOnGets();

    public void setAlwaysSynchronizeOnGets(boolean alwaysSynchornizeOnGets);

    public int[] getFilteredStatusCodes();

    public boolean isIgnoreCacheableCommandDeserializationException();

    public boolean isWebservicesSetRequiredTrue();
}
