/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.stat.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.ServerCache;
import com.ibm.ws.cache.stat.CachePerf;
import com.ibm.ws.cache.util.ExceptionUtility;
import com.ibm.wsspi.pmi.factory.StatisticActions;
import com.ibm.wsspi.pmi.factory.StatsFactory;
import com.ibm.wsspi.pmi.factory.StatsFactoryException;
import com.ibm.wsspi.pmi.factory.StatsGroup;
import com.ibm.wsspi.pmi.factory.StatsInstance;
import com.ibm.wsspi.pmi.stat.SPICountStatistic;
import com.ibm.wsspi.pmi.stat.SPIStatistic;

/**
 * This is a Stats/PMI module for Dynacache component.
 * 
 * @since v6.0
 */
public class CacheStatsModule extends StatisticActions implements CachePerf {
    private static TraceComponent tc = Tr.register(CacheStatsModule.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    // --------------------------------------------------------------------
    // Stats template files
    // --------------------------------------------------------------------
    private static final String templateCount2 = "/com/ibm/websphere/pmi/xml/cacheModule_root.xml";
    private static final String templateCount15 = "/com/ibm/websphere/pmi/xml/cacheModule_template.xml";
    private static final String templateCount13 = "/com/ibm/websphere/pmi/xml/cacheModule_disk.xml";
    // --------------------------------------------------------------------

    // --------------------------------------------------------------------
    // Cache Instance Types
    // --------------------------------------------------------------------
    public static final int TYPE_UNDETERMINED = 0;
    public static final int TYPE_SERVLET = 1;
    public static final int TYPE_OBJECT = 2;
    // --------------------------------------------------------------------

    // --------------------------------------------------------------------
    // Stats Groups <static = one for all class instances>
    // --------------------------------------------------------------------
    private static StatsGroup _cacheRootStatsGroup; // Shared
    private StatsGroup _cacheTemplateStatsGroup; // Non-Shared
    private StatsGroup _cacheDiskStatsGroup; // Non-Shared
    private StatsGroup _cacheObjectStatsGroup; // Non-Shared
    // --------------------------------------------------------------------

    // --------------------------------------------------------------------
    // stats instance vars
    // --------------------------------------------------------------------
    private StatsInstance _statsInstance = null;
    private CacheStatsModule _csmDisk = null;
    private CacheStatsModule _csmObject = null;
    private String _sCacheName;
    private boolean _diskOffloadEnabled;
    private int _iCacheType = TYPE_UNDETERMINED;
    protected boolean _enable = true;

    // --------------------------------------------------------------------

    // --------------------------------------------------------------------
    // The Counters - reference to the individual statistics
    // --------------------------------------------------------------------
    private SPICountStatistic _maxInMemoryCacheEntryCount = null;
    private SPICountStatistic _inMemoryCacheEntryCount = null;
    private SPICountStatistic _objectsOnDisk = null;
    private SPICountStatistic _hitsOnDisk = null;
    private SPICountStatistic _explicitInvalidationsFromDisk = null;
    private SPICountStatistic _timeoutInvalidationsFromDisk = null;
    private SPICountStatistic _pendingRemovalFromDisk = null;
    private SPICountStatistic _dependencyIdsOnDisk = null;
    private SPICountStatistic _dependencyIdsBufferedForDisk = null;
    private SPICountStatistic _dependencyIdsOffloadedToDisk = null;
    private SPICountStatistic _dependencyIdBasedInvalidationsFromDisk = null;
    private SPICountStatistic _templatesOnDisk = null;
    private SPICountStatistic _templatesBufferedForDisk = null;
    private SPICountStatistic _templatesOffloadedToDisk = null;
    private SPICountStatistic _templateBasedInvalidationsFromDisk = null;
    private SPICountStatistic _garbageCollectorInvalidationsFromDisk = null;
    private SPICountStatistic _overflowInvalidationsFromDisk = null;
    private SPICountStatistic _hitsInMemoryCount = null;
    private SPICountStatistic _hitsOnDiskCount = null;
    private SPICountStatistic _explicitInvalidationCount = null;
    private SPICountStatistic _lruInvalidationCount = null;
    private SPICountStatistic _timeoutInvalidationCount = null;
    private SPICountStatistic _inMemoryAndDiskCacheEntryCount = null;
    private SPICountStatistic _remoteHitCount = null;
    private SPICountStatistic _missCount = null;
    private SPICountStatistic _clientRequestCount = null;
    private SPICountStatistic _distributedRequestCount = null;
    private SPICountStatistic _explicitMemoryInvalidationCount = null;
    private SPICountStatistic _explicitDiskInvalidationCount = null;
    private SPICountStatistic _localExplicitInvalidationCount = null;
    private SPICountStatistic _remoteExplicitInvalidationCount = null;
    private SPICountStatistic _remoteCreationCount = null;
    // --------------------------------------------------------------------

    // --------------------------------------------------------------------
    HashMap<String, CacheStatsModule> templatesPassed = new HashMap<String, CacheStatsModule>();
    ArrayList<String> templatesFailed = new ArrayList<String>();

    // --------------------------------------------------------------------

    // --------------------------------------------------------------------
    // CTOR 1 of 2 -
    // Create Root Group + Servlet Group
    // or Create Root Group + Object Group
    // --------------------------------------------------------------------
    /**
     * Constructor using cache type and cache name.
     */
    public CacheStatsModule(String cacheName, boolean swapToDisk) throws StatsFactoryException {
        final String methodName = "CacheStatsModule() CTOR #1";

        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName + " cacheName=" + cacheName + " offload " + swapToDisk);
        }

        _sCacheName = cacheName;
        _diskOffloadEnabled = swapToDisk;

        // -------------------------------------------------------
        // Create the static root group
        // -------------------------------------------------------
        if (_cacheRootStatsGroup == null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName + " Creating cacheModule group" + " for cacheName=" + _sCacheName);
            _cacheRootStatsGroup = StatsFactory.createStatsGroup(WSDynamicCacheStats.NAME, templateCount2, null, this);
        }
        // -------------------------------------------------------

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, methodName + " _cacheRootStatsGroup=" + _cacheRootStatsGroup + " for cacheName=" + _sCacheName);
        }
    }

    // -------------------------------------------------------
    // Create the static Servlet Cache group & cache stats instance
    // -------------------------------------------------------
    public void enableServletCacheStats() {
        final String methodName = "enableServletCacheStats";
        try {
            if (null == _statsInstance) {

                _iCacheType = TYPE_SERVLET;

                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName + " Creating statistic for " + _sCacheName + " instance (servlet cache)");
                _statsInstance = StatsFactory.createStatsInstance(WSDynamicCacheStats.SERVLET_CACHE_TYPE_PREFIX + _sCacheName, _cacheRootStatsGroup,
                        null, this);
                if (_diskOffloadEnabled) {
                    if (_cacheDiskStatsGroup == null) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, methodName + " Creating disk group" + " for cacheName=" + _sCacheName);
                        _cacheDiskStatsGroup = StatsFactory.createStatsGroup(WSDynamicCacheStats.DISK_GROUP, templateCount13, _statsInstance, null,
                                this);
                    }
                    _csmDisk = new CacheStatsModule(_sCacheName, WSDynamicCacheStats.DISK_OFFLOAD_ENABLED, _cacheDiskStatsGroup, this);
                }
            }
        } catch (StatsFactoryException e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.stat.CacheStatsModule", "198", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName + " Exception while enabling servlet cache stats template - cacheName=" + _sCacheName + ": "
                        + ExceptionUtility.getStackTrace(e));
        }
    }

    // -------------------------------------------------------
    // Create the static Object Cache group & cache stats instance
    // -------------------------------------------------------
    public void enableObjectCacheStats() {
        final String methodName = "enableObjectCacheStats";
        try {
            if (null == _statsInstance) {
                _iCacheType = TYPE_OBJECT;

                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName + " Creating statistic for " + _sCacheName + " instance (object cache)");
                _statsInstance = StatsFactory.createStatsInstance(WSDynamicCacheStats.OBJECT_CACHE_TYPE_PREFIX + _sCacheName, _cacheRootStatsGroup,
                        null, this);

                if (null == _cacheObjectStatsGroup) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, methodName + " Creating object cache group" + " for cacheName=" + _sCacheName);
                    _cacheObjectStatsGroup = StatsFactory.createStatsGroup(WSDynamicCacheStats.OBJECT_GROUP, templateCount15, _statsInstance, null,
                            this);
                    _csmObject = new CacheStatsModule(_sCacheName, WSDynamicCacheStats.OBJECT_COUNTERS, _cacheObjectStatsGroup, this);
                }

                if (_diskOffloadEnabled) {
                    if (_cacheDiskStatsGroup == null) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, methodName + " Creating disk group" + " for cacheName=" + _sCacheName);
                        _cacheDiskStatsGroup = StatsFactory.createStatsGroup(WSDynamicCacheStats.DISK_GROUP, templateCount13, _statsInstance, null,
                                this);
                    }
                    _csmDisk = new CacheStatsModule(_sCacheName, WSDynamicCacheStats.DISK_OFFLOAD_ENABLED, _cacheDiskStatsGroup, this);
                }
            }
        } catch (StatsFactoryException e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.stat.CacheStatsModule", "176", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc,
                        methodName + " Exception while enabling object cache stats for" + _sCacheName + " : " + ExceptionUtility.getStackTrace(e));
        }

    }

    // --------------------------------------------------------------------

    // --------------------------------------------------------------------
    // CTOR 2 of 2 -
    // Create Template Group
    // --------------------------------------------------------------------
    /**
     * Constructor using template name and template group
     */
    public CacheStatsModule(String cacheName, String template, StatsGroup templateGroup, CacheStatsModule parent) throws StatsFactoryException {
        final String methodName = "CacheStatsModule() CTOR #2";

        if (tc.isEntryEnabled())
            Tr.entry(tc, methodName + " cacheName=" + cacheName, " name/template=" + template);

        _sCacheName = cacheName;
        _iCacheType = parent._iCacheType;
        _diskOffloadEnabled = parent._diskOffloadEnabled;

        if (tc.isDebugEnabled())
            Tr.debug(tc, methodName + " Creating statistic for name/template=" + template + " instance for cacheName=" + cacheName);

        _statsInstance = StatsFactory.createStatsInstance(template, templateGroup, null, this);

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, methodName + " _statsInstance=" + _statsInstance + " for cacheName=" + cacheName);
        }
    }

    private synchronized CacheStatsModule createTemplateModule(String template) {
        final String methodName = "createTemplateModule()";
        CacheStatsModule csm = (CacheStatsModule) templatesPassed.get(template);
        if (csm == null) {
            if (!templatesFailed.contains(template)) {
                try {
                    if (_cacheTemplateStatsGroup == null) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, methodName + " Creating template group" + " for cacheName=" + _sCacheName);

                        _cacheTemplateStatsGroup = StatsFactory.createStatsGroup(WSDynamicCacheStats.TEMPLATE_GROUP, templateCount15, _statsInstance,
                                null, this);
                    }
                    csm = new CacheStatsModule(_sCacheName, template, _cacheTemplateStatsGroup, this);
                    templatesPassed.put(template, csm);
                } catch (Throwable t) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.cache.stat.CacheStatsModule", "572", this);
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, methodName + " Exception while creating template - cacheName=" + _sCacheName + " template=" + template + ": "
                                + ExceptionUtility.getStackTrace(t));
                    templatesFailed.add(template);
                    csm = null;
                }
            }
        }
        return csm;
    }

    private CacheStatsModule getCSM(String template) {

        CacheStatsModule csm = null;

        try {
            // This should happen only once per cache instance
            if (_iCacheType == TYPE_UNDETERMINED) {
                if (template == null || template.equals("")) {
                    enableObjectCacheStats();
                } else {
                    enableServletCacheStats();
                }
            }

            if (_iCacheType == TYPE_SERVLET) {
                csm = (CacheStatsModule) templatesPassed.get(template);
                if (csm == null) {
                    if (null != template) {
                        csm = createTemplateModule(template);
                    }
                }
            } else if (_iCacheType == TYPE_OBJECT) {
                csm = _csmObject;
            }
        }

        finally {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "getCSM " + template + " > " + csm);
            }
        }

        return csm;
    }

    // --------------------------------------------------------------------

    /**
     * Implementing StatisticActionListener interface
     * 
     * Grab a reference to the Statistic
     */
    public void statisticCreated(SPIStatistic s) {
        final String methodName = "statisticCreated()";
        // if (tc.isEntryEnabled()) {
        // Tr.entry(tc, methodName + " cacheName=" + _sCacheName + " statistic=" + s, this);
        // }
        switch (s.getId()) {
        case WSDynamicCacheStats.MaxInMemoryCacheEntryCount:
            _maxInMemoryCacheEntryCount = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.InMemoryCacheEntryCount:
            _inMemoryCacheEntryCount = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.HitsInMemoryCount:
            _hitsInMemoryCount = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.HitsOnDiskCount:
            _hitsOnDiskCount = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.ExplicitInvalidationCount:
            _explicitInvalidationCount = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.LruInvalidationCount:
            _lruInvalidationCount = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.TimeoutInvalidationCount:
            _timeoutInvalidationCount = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.InMemoryAndDiskCacheEntryCount:
            _inMemoryAndDiskCacheEntryCount = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.RemoteHitCount:
            _remoteHitCount = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.MissCount:
            _missCount = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.ClientRequestCount:
            _clientRequestCount = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.DistributedRequestCount:
            _distributedRequestCount = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.ExplicitMemoryInvalidationCount:
            _explicitMemoryInvalidationCount = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.ExplicitDiskInvalidationCount:
            _explicitDiskInvalidationCount = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.LocalExplicitInvalidationCount:
            _localExplicitInvalidationCount = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.RemoteExplicitInvalidationCount:
            _remoteExplicitInvalidationCount = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.RemoteCreationCount:
            _remoteCreationCount = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.ObjectsOnDisk:
            _objectsOnDisk = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.HitsOnDisk:
            _hitsOnDisk = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.ExplicitInvalidationsFromDisk:
            _explicitInvalidationsFromDisk = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.TimeoutInvalidationsFromDisk:
            _timeoutInvalidationsFromDisk = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.PendingRemovalFromDisk:
            _pendingRemovalFromDisk = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.DependencyIdsOnDisk:
            _dependencyIdsOnDisk = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.DependencyIdsBufferedForDisk:
            _dependencyIdsBufferedForDisk = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.DependencyIdsOffloadedToDisk:
            _dependencyIdsOffloadedToDisk = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.DependencyIdBasedInvalidationsFromDisk:
            _dependencyIdBasedInvalidationsFromDisk = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.TemplatesOnDisk:
            _templatesOnDisk = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.TemplatesBufferedForDisk:
            _templatesBufferedForDisk = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.TemplatesOffloadedToDisk:
            _templatesOffloadedToDisk = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.TemplateBasedInvalidationsFromDisk:
            _templateBasedInvalidationsFromDisk = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.GarbageCollectorInvalidationsFromDisk:
            _garbageCollectorInvalidationsFromDisk = (SPICountStatistic) s;
            break;
        case WSDynamicCacheStats.OverflowInvalidationsFromDisk:
            _overflowInvalidationsFromDisk = (SPICountStatistic) s;
            break;
        default:
            Tr.debug(tc, methodName,
                    "Error - Unknown stats Id for " + WSDynamicCacheStats.NAME + " cacheName=" + _sCacheName + " dataID=" + s.getId());

            assert false;
            break;
        }
    }

    public void enableStatusChanged(int[] enabled, int[] disabled) {
        final String methodName = "enableStatusChanged()";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, _sCacheName, this, enabled, disabled);
        }

        ServerCache.getCache(_sCacheName).refreshCachePerf();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, methodName);
        }
    }

    /**
     * Not used by dynacache this method will be called by the PMI service ONLY when a client is requesting for this
     * statistic. note that other statistics are updated based on some event in the component. this is useful if you
     * have a statistic that is expensive to compute
     */
    public void updateStatisticOnRequest(int dataId) {
    }

    /**
     * Reports whether PMI reporting is set to a sufficiently high value for the cache to report info.
     * 
     * @return whether the cache should report info to PMI
     */
    public boolean isPMIEnabled() {
        _enable = StatsFactory.isPMIEnabled();
        return _enable;
    }

    /*
     * resets the PMI counters to zero
     */
    public void resetPMICounters() {
        final String methodName = "resetPMICounters()";
        if (tc.isDebugEnabled())
            Tr.debug(tc, methodName + " cacheName=" + _sCacheName);

        if (_enable && this._maxInMemoryCacheEntryCount != null) {
            this._maxInMemoryCacheEntryCount.setCount(0);
        }
        if (_enable && this._inMemoryCacheEntryCount != null) {
            this._inMemoryCacheEntryCount.setCount(0);
        }
        if (_csmDisk != null && _csmDisk._enable) {
            if (_csmDisk._objectsOnDisk != null) {
                _csmDisk._objectsOnDisk.setCount(0);
            }
            if (_csmDisk._hitsOnDisk != null) {
                _csmDisk._hitsOnDisk.setCount(0);
            }
            if (_csmDisk._explicitInvalidationsFromDisk != null) {
                _csmDisk._explicitInvalidationsFromDisk.setCount(0);
            }
            if (_csmDisk._timeoutInvalidationsFromDisk != null) {
                _csmDisk._timeoutInvalidationsFromDisk.setCount(0);
            }
            if (_csmDisk._pendingRemovalFromDisk != null) {
                _csmDisk._pendingRemovalFromDisk.setCount(0);
            }
            if (_csmDisk._dependencyIdsOnDisk != null) {
                _csmDisk._dependencyIdsOnDisk.setCount(0);
            }
            if (_csmDisk._dependencyIdsBufferedForDisk != null) {
                _csmDisk._dependencyIdsBufferedForDisk.setCount(0);
            }
            if (_csmDisk._dependencyIdsOffloadedToDisk != null) {
                _csmDisk._dependencyIdsOffloadedToDisk.setCount(0);
            }
            if (_csmDisk._dependencyIdBasedInvalidationsFromDisk != null) {
                _csmDisk._dependencyIdBasedInvalidationsFromDisk.setCount(0);
            }
            if (_csmDisk._templatesOnDisk != null) {
                _csmDisk._templatesOnDisk.setCount(0);
            }
            if (_csmDisk._templatesBufferedForDisk != null) {
                _csmDisk._templatesBufferedForDisk.setCount(0);
            }
            if (_csmDisk._templatesOffloadedToDisk != null) {
                _csmDisk._templatesOffloadedToDisk.setCount(0);
            }
            if (_csmDisk._templateBasedInvalidationsFromDisk != null) {
                _csmDisk._templateBasedInvalidationsFromDisk.setCount(0);
            }
            if (_csmDisk._garbageCollectorInvalidationsFromDisk != null) {
                _csmDisk._garbageCollectorInvalidationsFromDisk.setCount(0);
            }
            if (_csmDisk._overflowInvalidationsFromDisk != null) {
                _csmDisk._overflowInvalidationsFromDisk.setCount(0);
            }
        }
        if (this._iCacheType == TYPE_SERVLET) {
            for (CacheStatsModule csm : templatesPassed.values()) {
                if (csm != null && csm._enable) {
                    if (csm._hitsInMemoryCount != null) {
                        csm._hitsInMemoryCount.setCount(0);
                    }
                    if (csm._hitsOnDiskCount != null) {
                        csm._hitsOnDiskCount.setCount(0);
                    }
                    if (csm._explicitInvalidationCount != null) {
                        csm._explicitInvalidationCount.setCount(0);
                    }
                    if (csm._lruInvalidationCount != null) {
                        csm._lruInvalidationCount.setCount(0);
                    }
                    if (csm._timeoutInvalidationCount != null) {
                        csm._timeoutInvalidationCount.setCount(0);
                    }
                    if (csm._inMemoryAndDiskCacheEntryCount != null) {
                        csm._inMemoryAndDiskCacheEntryCount.setCount(0);
                    }
                    if (csm._remoteHitCount != null) {
                        csm._remoteHitCount.setCount(0);
                    }
                    if (csm._missCount != null) {
                        csm._missCount.setCount(0);
                    }
                    if (csm._clientRequestCount != null) {
                        csm._clientRequestCount.setCount(0);
                    }
                    if (csm._distributedRequestCount != null) {
                        csm._distributedRequestCount.setCount(0);
                    }
                    if (csm._explicitMemoryInvalidationCount != null) {
                        csm._explicitMemoryInvalidationCount.setCount(0);
                    }
                    if (csm._explicitDiskInvalidationCount != null) {
                        csm._explicitDiskInvalidationCount.setCount(0);
                    }
                    if (csm._localExplicitInvalidationCount != null) {
                        csm._localExplicitInvalidationCount.setCount(0);
                    }
                    if (csm._remoteExplicitInvalidationCount != null) {
                        csm._remoteExplicitInvalidationCount.setCount(0);
                    }
                    if (csm._remoteCreationCount != null) {
                        csm._remoteCreationCount.setCount(0);
                    }
                }
            }
        } else if (_csmObject != null && _csmObject._enable) {
            if (_csmObject._hitsInMemoryCount != null) {
                _csmObject._hitsInMemoryCount.setCount(0);
            }
            if (_csmObject._hitsOnDiskCount != null) {
                _csmObject._hitsOnDiskCount.setCount(0);
            }
            if (_csmObject._explicitInvalidationCount != null) {
                _csmObject._explicitInvalidationCount.setCount(0);
            }
            if (_csmObject._lruInvalidationCount != null) {
                _csmObject._lruInvalidationCount.setCount(0);
            }
            if (_csmObject._timeoutInvalidationCount != null) {
                _csmObject._timeoutInvalidationCount.setCount(0);
            }
            if (_csmObject._inMemoryAndDiskCacheEntryCount != null) {
                _csmObject._inMemoryAndDiskCacheEntryCount.setCount(0);
            }
            if (_csmObject._remoteHitCount != null) {
                _csmObject._remoteHitCount.setCount(0);
            }
            if (_csmObject._missCount != null) {
                _csmObject._missCount.setCount(0);
            }
            if (_csmObject._clientRequestCount != null) {
                _csmObject._clientRequestCount.setCount(0);
            }
            if (_csmObject._distributedRequestCount != null) {
                _csmObject._distributedRequestCount.setCount(0);
            }
            if (_csmObject._explicitMemoryInvalidationCount != null) {
                _csmObject._explicitMemoryInvalidationCount.setCount(0);
            }
            if (_csmObject._explicitDiskInvalidationCount != null) {
                _csmObject._explicitDiskInvalidationCount.setCount(0);
            }
            if (_csmObject._localExplicitInvalidationCount != null) {
                _csmObject._localExplicitInvalidationCount.setCount(0);
            }
            if (_csmObject._remoteExplicitInvalidationCount != null) {
                _csmObject._remoteExplicitInvalidationCount.setCount(0);
            }
            if (_csmObject._remoteCreationCount != null) {
                _csmObject._remoteCreationCount.setCount(0);
            }
        }
    }

    /**
     * Updates statistics using two supplied arguments - maxInMemoryCacheSize and currentInMemoryCacheSize.
     * 
     * @param max
     *            Maximum # of entries that can be stored in memory
     * @param current
     *            Current # of in memory cache entries
     */
    public void updateCacheSizes(long max, long current) {

        final String methodName = "updateCacheSizes()";

        if (tc.isDebugEnabled() && null != _maxInMemoryCacheEntryCount && null != _inMemoryCacheEntryCount) {
            if (max != _maxInMemoryCacheEntryCount.getCount() && _inMemoryCacheEntryCount.getCount() != current)
                Tr.debug(tc, methodName + " cacheName=" + _sCacheName + " max=" + max + " current=" + current + " enable=" + this._enable, this);
        }

        if (_enable) {
            if (_maxInMemoryCacheEntryCount != null)
                _maxInMemoryCacheEntryCount.setCount(max);

            if (_inMemoryCacheEntryCount != null)
                _inMemoryCacheEntryCount.setCount(current);
        }
    }

    /*
     * updates the global statistics for disk information
     * 
     * @param objectsOnDisk number of cache entries that are currently on disk
     * 
     * @param pendingRemoval number of objects that have been invalidated by are yet to be removed from disk
     * 
     * @param depidsOnDisk number of dependency ids that are currently on disk
     * 
     * @param depidsBuffered number of dependency ids that have been currently bufferred in memory for the disk
     * 
     * @param depidsOffloaded number of dependency ids offloaded to disk
     * 
     * @param depidBasedInvalidations number of dependency id based invalidations
     * 
     * @param templatesOnDisk number of templates that are currently on disk
     * 
     * @param templatesBuffered number of templates that have been currently bufferred in memory for the disk
     * 
     * @param templatesOffloaded number of templates offloaded to disk
     * 
     * @param templateBasedInvalidations number of template based invalidations
     */
    public void updateDiskCacheStatistics(long objectsOnDisk, long pendingRemoval, long depidsOnDisk, long depidsBuffered, long depidsOffloaded,
            long depidBasedInvalidations, long templatesOnDisk, long templatesBuffered, long templatesOffloaded, long templateBasedInvalidations) {

        final String methodName = "updateDiskCacheStatistics()";
        boolean anyStatChangedValue = false;

        if (_csmDisk != null && _csmDisk._enable) {

            if (_csmDisk._objectsOnDisk != null) {
                if (!anyStatChangedValue)
                    anyStatChangedValue = anyValueChanged(_csmDisk._objectsOnDisk, objectsOnDisk);
                _csmDisk._objectsOnDisk.setCount(objectsOnDisk);
            }
            if (_csmDisk._pendingRemovalFromDisk != null) {
                if (!anyStatChangedValue)
                    anyStatChangedValue = anyValueChanged(_csmDisk._pendingRemovalFromDisk, pendingRemoval);
                _csmDisk._pendingRemovalFromDisk.setCount(pendingRemoval);
            }
            if (_csmDisk._dependencyIdsOnDisk != null) {
                if (!anyStatChangedValue)
                    anyStatChangedValue = anyValueChanged(_csmDisk._dependencyIdsOnDisk, depidsOnDisk);
                _csmDisk._dependencyIdsOnDisk.setCount(depidsOnDisk);
            }
            if (_csmDisk._dependencyIdsBufferedForDisk != null) {
                if (!anyStatChangedValue)
                    anyStatChangedValue = anyValueChanged(_csmDisk._dependencyIdsBufferedForDisk, depidsBuffered);
                _csmDisk._dependencyIdsBufferedForDisk.setCount(depidsBuffered);
            }
            if (_csmDisk._dependencyIdsOffloadedToDisk != null) {
                if (!anyStatChangedValue)
                    anyStatChangedValue = anyValueChanged(_csmDisk._dependencyIdsOffloadedToDisk, depidsOffloaded);
                _csmDisk._dependencyIdsOffloadedToDisk.setCount(depidsOffloaded);
            }
            if (_csmDisk._dependencyIdBasedInvalidationsFromDisk != null) {
                if (!anyStatChangedValue)
                    anyStatChangedValue = anyValueChanged(_csmDisk._dependencyIdBasedInvalidationsFromDisk, depidBasedInvalidations);
                _csmDisk._dependencyIdBasedInvalidationsFromDisk.setCount(depidBasedInvalidations);
            }
            if (_csmDisk._templatesOnDisk != null) {
                if (!anyStatChangedValue)
                    anyStatChangedValue = anyValueChanged(_csmDisk._templatesOnDisk, templatesOnDisk);
                _csmDisk._templatesOnDisk.setCount(templatesOnDisk);
            }
            if (_csmDisk._templatesBufferedForDisk != null) {
                if (!anyStatChangedValue)
                    anyStatChangedValue = anyValueChanged(_csmDisk._templatesBufferedForDisk, templatesBuffered);
                _csmDisk._templatesBufferedForDisk.setCount(templatesBuffered);
            }
            if (_csmDisk._templatesOffloadedToDisk != null) {
                if (!anyStatChangedValue)
                    anyStatChangedValue = anyValueChanged(_csmDisk._templatesOffloadedToDisk, templatesOffloaded);
                _csmDisk._templatesOffloadedToDisk.setCount(templatesOffloaded);
            }
            if (_csmDisk._templateBasedInvalidationsFromDisk != null) {
                if (!anyStatChangedValue)
                    anyStatChangedValue = anyValueChanged(_csmDisk._templateBasedInvalidationsFromDisk, templateBasedInvalidations);
                _csmDisk._templateBasedInvalidationsFromDisk.setCount(templateBasedInvalidations);
            }

            if (tc.isDebugEnabled() && anyStatChangedValue)
                Tr.debug(tc, methodName + " cacheName=" + _sCacheName + " updateCacheStatistics, objectsOnDisk=" + objectsOnDisk
                        + " pendingRemovalFromDisk=" + pendingRemoval + " dependencyIdsOnDisk=" + depidsOnDisk + " dependencyIdsBufferedForDisk="
                        + depidsBuffered + " dependencyIdsOffloadedToDisk=" + depidsOffloaded + " dependencyIdBasedInvalidations="
                        + depidBasedInvalidations + " templatesOnDisk=" + templatesOnDisk + " templatesBufferedForDisk=" + templatesBuffered
                        + " templatesOffloadedToDisk=" + templatesOffloaded + " templateBasedInvalidations=" + templateBasedInvalidations
                        + " enable=" + _csmDisk._enable + " " + this);

        }
    }

    private boolean anyValueChanged(SPICountStatistic s, long passedIn) {
        return (s.getCount() != passedIn);
    }

    /**
     * Updates statistics for the cache hit case.
     * 
     * @param template
     *            the template of the cache entry. The template cannot be null for Servlet cache.
     * @param locality
     *            Whether the miss was local or remote
     */
    public void onCacheHit(String template, int locality) {
        final String methodName = "onCacheHit()";

        CacheStatsModule csm = null;
        if ((csm = getCSM(template)) == null) {
            return;
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, methodName + " cacheName=" + _sCacheName + " template=" + template + " locality=" + locality + " enable=" + csm._enable
                    + " parentEnable=" + _enable + " " + this);

        switch (locality) {
        case REMOTE:
            if (csm._enable) {
                if (csm._remoteHitCount != null) {
                    csm._remoteHitCount.increment();
                }
                if (csm._inMemoryAndDiskCacheEntryCount != null) {
                    csm._inMemoryAndDiskCacheEntryCount.increment();
                }
                if (csm._remoteCreationCount != null) {
                    csm._remoteCreationCount.increment();
                }
            }
            break;
        case MEMORY:
            if (csm._enable && csm._hitsInMemoryCount != null)
                csm._hitsInMemoryCount.increment();
            break;
        case DISK:
            if (_csmDisk != null && _csmDisk._enable && _csmDisk._hitsOnDisk != null) {
                _csmDisk._hitsOnDisk.increment();
            }
            if (csm._enable && csm._hitsOnDiskCount != null)
                csm._hitsOnDiskCount.increment();
            break;
        default:
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName + " Error - Unrecognized locality " + locality + " cacheName=" + _sCacheName);
            break;
        }
        return;
    }

    /**
     * Updates statistics for the cache miss case.
     * 
     * @param template
     *            the template of the cache entry. The template cannot be null for Servlet cache.
     * @param locality
     *            Whether the miss was local or remote
     */
    public void onCacheMiss(String template, int locality) {
        final String methodName = "onCacheMiss()";

        CacheStatsModule csm = null;
        if ((csm = getCSM(template)) == null) {
            return;
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, methodName + " cacheName=" + _sCacheName + " template=" + template + " locality=" + locality + " enable=" + csm._enable
                    + " " + this);
        if (csm._enable && csm._missCount != null)
            csm._missCount.increment();
        return;
    }

    /**
     * Updates statistics for the cache entry creation case.
     * 
     * @param template
     *            the template of the cache entry. The template cannot be null for Servlet cache.
     * @param source
     *            whether the invalidation was generated internally or remotely
     */
    public void onEntryCreation(String template, int source) {
        final String methodName = "onEntryCreation()";

        CacheStatsModule csm = null;
        if ((csm = getCSM(template)) == null) {
            return;
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, methodName + " cacheName=" + _sCacheName + " template=" + template + " source=" + source + " enable=" + csm._enable + " "
                    + this);
        if (csm._enable) {
            if (source == REMOTE) {
                if (csm._remoteCreationCount != null)
                    csm._remoteCreationCount.increment();
            }
            if (csm._inMemoryAndDiskCacheEntryCount != null)
                csm._inMemoryAndDiskCacheEntryCount.increment();
        }
        return;
    }

    /*
     * registers that a request came into the cache for a cacheable object
     * 
     * @param template the template of the cache entry. The template cannot be null for Servlet cache.
     * 
     * @param source whether the invalidation was generated internally or remotely
     */
    public void onRequest(String template, int source) {
        final String methodName = "onRequest()";

        CacheStatsModule csm = null;
        if ((csm = getCSM(template)) == null) {
            return;
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, methodName + " cacheName=" + _sCacheName + " template=" + template + " source=" + source + " enable=" + csm._enable + " "
                    + this);
        if (csm._enable) {
            if (source == LOCAL) {
                if (csm._clientRequestCount != null)
                    csm._clientRequestCount.increment();
            } else if (source == REMOTE) {
                if (csm._distributedRequestCount != null)
                    csm._distributedRequestCount.increment();
            }
        }
        return;
    }

    /*
     * Updates statistics for all types of invalidations - timeout, LRU, explicit invalidations
     * 
     * @param template the template of the cache entry. The template cannot be null for Servlet cache.
     * 
     * @param cause the cause of invalidation
     * 
     * @param locality whether the invalidation occurred on disk, in memory, or neither
     * 
     * @param source whether the invalidation was generated internally or remotely
     * 
     * @param dels number of invalidations that occurred
     */
    public void batchOnInvalidate(String template, int cause, int locality, int source, int dels) {
        final String methodName = "batchOnInvalidate()";

        CacheStatsModule csm = null;
        if ((csm = getCSM(template)) == null) {
            return;
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, methodName + " cacheName=" + _sCacheName + " template=" + template + " cause=" + cause + " locality=" + locality
                    + " source=" + source + " delete=" + dels + " disk=" + _csmDisk + " parentEnable=" + _enable + " " + this);
        switch (cause) {
        case DIRECT:
            if (csm._enable && csm._explicitInvalidationCount != null)
                csm._explicitInvalidationCount.increment(dels);
            switch (locality) {
            case MEMORY:
                if (csm._enable) {
                    if (csm._explicitMemoryInvalidationCount != null)
                        csm._explicitMemoryInvalidationCount.increment(dels);
                    if (csm._inMemoryAndDiskCacheEntryCount != null)
                        csm._inMemoryAndDiskCacheEntryCount.decrement(dels);
                }
                break;
            case DISK:
                if (_csmDisk != null && _csmDisk._enable && _csmDisk._explicitInvalidationsFromDisk != null) {
                    _csmDisk._explicitInvalidationsFromDisk.increment(dels);
                }
                if (csm._enable) {
                    if (csm._explicitDiskInvalidationCount != null)
                        csm._explicitDiskInvalidationCount.increment(dels);
                    if (csm._inMemoryAndDiskCacheEntryCount != null)
                        csm._inMemoryAndDiskCacheEntryCount.decrement(dels);
                }
                break;
            default:
                break;
            }
            switch (source) {
            case LOCAL:
                if (csm._enable && csm._localExplicitInvalidationCount != null)
                    csm._localExplicitInvalidationCount.increment(dels);
                break;
            case REMOTE:
                if (csm._enable && csm._remoteExplicitInvalidationCount != null)
                    csm._remoteExplicitInvalidationCount.increment(dels);
                break;
            default:
                break;
            }
            break;
        case LRU:
            if (csm._enable && csm._lruInvalidationCount != null)
                csm._lruInvalidationCount.increment(dels);
            if (locality != DISK) {
                if (csm._enable && csm._inMemoryAndDiskCacheEntryCount != null)
                    csm._inMemoryAndDiskCacheEntryCount.decrement(dels);
            }
            break;
        case TIMEOUT:
            if (_csmDisk != null && locality == DISK) {
                if (_csmDisk._enable && _csmDisk._timeoutInvalidationsFromDisk != null) {
                    _csmDisk._timeoutInvalidationsFromDisk.increment(dels);
                }
            }
            if (csm._enable) {
                if (csm._timeoutInvalidationCount != null)
                    csm._timeoutInvalidationCount.increment(dels);
                if (csm._inMemoryAndDiskCacheEntryCount != null)
                    csm._inMemoryAndDiskCacheEntryCount.decrement(dels);
            }
            break;
        case DISK_GARBAGE_COLLECTOR:
            if (_csmDisk != null && _csmDisk._enable && _csmDisk._garbageCollectorInvalidationsFromDisk != null) {
                _csmDisk._garbageCollectorInvalidationsFromDisk.increment(dels);
            }
            if (csm._enable && csm._inMemoryAndDiskCacheEntryCount != null) {
                csm._inMemoryAndDiskCacheEntryCount.decrement(dels);
            }
            break;
        case DISK_OVERFLOW:
            if (_csmDisk != null && _csmDisk._enable && _csmDisk._overflowInvalidationsFromDisk != null) {
                _csmDisk._overflowInvalidationsFromDisk.increment(dels);
            }
            if (csm._enable && csm._inMemoryAndDiskCacheEntryCount != null) {
                csm._inMemoryAndDiskCacheEntryCount.decrement(dels);
            }
            break;
        default:
            break;
        }
        return;
    }

    /*
     * Updates statistics for all types of invalidations - timeout, LRU, explicit invalidations
     * 
     * @param template the template of the cache entry. The template cannot be null for Servlet cache.
     * 
     * @param cause the cause of invalidation
     * 
     * @param locality whether the invalidation occurred on disk, in memory, or neither
     * 
     * @param source whether the invalidation was generated internally or remotely
     * 
     * @param dels number of invalidations that occurred
     */
    public void onInvalidate(String template, int cause, int locality, int source) {
        final String methodName = "onInvalidate()";
        if (tc.isDebugEnabled())
            Tr.debug(tc, methodName + " cacheName=" + _sCacheName + " template=" + template + " cause=" + cause + " locality=" + locality
                    + " source=" + source);
        batchOnInvalidate(template, cause, locality, source, 1);
        return;
    }

    /*
     * updates statistics when clear cache is invoked.
     * 
     * @param memory boolean true to indicate memory cache is cleared
     * 
     * @param disk boolean true to indicate disk cache is cleared
     */
    public void onCacheClear(boolean memory, boolean disk) {
        final String methodName = "onCacheClear()";
        if (tc.isDebugEnabled())
            Tr.debug(tc, methodName + " cacheName=" + _sCacheName + " memory=" + memory + " disk=" + disk + " " + this);
        if (_enable && this._inMemoryCacheEntryCount != null && memory) {
            this._inMemoryCacheEntryCount.setCount(0);
        }
        if (_csmDisk != null && _csmDisk._enable && disk) {
            if (_csmDisk._objectsOnDisk != null) {
                _csmDisk._objectsOnDisk.setCount(0);
            }
            if (_csmDisk._pendingRemovalFromDisk != null) {
                _csmDisk._pendingRemovalFromDisk.setCount(0);
            }
            if (_csmDisk._dependencyIdsOnDisk != null) {
                _csmDisk._dependencyIdsOnDisk.setCount(0);
            }
            if (_csmDisk._dependencyIdsBufferedForDisk != null) {
                _csmDisk._dependencyIdsBufferedForDisk.setCount(0);
            }
            if (_csmDisk._templatesOnDisk != null) {
                _csmDisk._templatesOnDisk.setCount(0);
            }
            if (_csmDisk._templatesBufferedForDisk != null) {
                _csmDisk._templatesBufferedForDisk.setCount(0);
            }
        }
        if (this._iCacheType == TYPE_SERVLET && (memory || disk)) {
            for (Map.Entry<String, CacheStatsModule> entry : templatesPassed.entrySet()) {
                CacheStatsModule csm = entry.getValue();
                if (csm != null && csm._enable) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "***** clear _inMemoryAndDiskCacheEntryCount for template=" + entry.getValue() + " cacheName=" + _sCacheName);
                    if (csm._inMemoryAndDiskCacheEntryCount != null) {
                        csm._inMemoryAndDiskCacheEntryCount.setCount(0);
                    }
                }
            }
        } else if (_csmObject != null && _csmObject._enable && (memory || disk)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "***** clear _inMemoryAndDiskCacheEntryCount for cacheName=" + _sCacheName);
            if (_csmObject._inMemoryAndDiskCacheEntryCount != null) {
                _csmObject._inMemoryAndDiskCacheEntryCount.setCount(0);
            }
        }
    }

    @Override
    public void removePMICounters() {
        try {
            if (null != _statsInstance)
                StatsFactory.removeStatsInstance(_statsInstance);
            if (null != _cacheObjectStatsGroup)
                StatsFactory.removeStatsGroup(_cacheObjectStatsGroup);
            if (null != _cacheDiskStatsGroup)
                StatsFactory.removeStatsGroup(_cacheDiskStatsGroup);
            if (null != _cacheTemplateStatsGroup)
                StatsFactory.removeStatsGroup(_cacheTemplateStatsGroup);
            if (null != _cacheRootStatsGroup)
                StatsFactory.removeStatsGroup(_cacheRootStatsGroup);
        } catch (StatsFactoryException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Could NOT unregister PMI for cache instance {0} due to {1}", _sCacheName, e, this);
            }
        }
    }

}
