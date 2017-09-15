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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.cache.CacheEntry;
import com.ibm.websphere.cache.InvalidationEvent;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.intf.DCache;
import com.ibm.ws.cache.intf.ExternalCacheServices;
import com.ibm.ws.cache.intf.ExternalInvalidation;
import com.ibm.ws.cache.stat.CachePerf;

/**
 * This class accepts cache events (invalidation and new entry)
 * and wakes periodically to process them.
 * The interval between these batch operations is configurable
 * via the dynacache.xml configuration file.
 */
public class BatchUpdateDaemon extends RealTimeDaemon {
    private static TraceComponent tc = Tr.register(BatchUpdateDaemon.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    private ExternalCacheServices externalCacheServices = null;
    private InvalidationAuditDaemon invalidationAuditDaemon = null;

    private static final ArrayList EMPTY_ARRAYLIST = new ArrayList();

    private final HashMap updates = new HashMap();
    private final HashMap drsBuffer = new HashMap();
    private final int drsCongestionThreshold = 5;

    //private long lastTimeForTrace = 0;  //398807
    //public static final int TRACE_WINDOW = 10000;  // window 10 sec  //398807

    /**
     * Constructor with parameter.
     * 
     * @param timeBetweenBatches The time in milliseconds between batch executions.
     */
    public BatchUpdateDaemon(int timeBetweenBatches) {
        super(timeBetweenBatches);
    }

    /**
     * This sets the externalCacheServices variable.
     */
    public void setExternalCacheServices(ExternalCacheServices externalCacheServices) {
        this.externalCacheServices = externalCacheServices;
    }

    /**
     * This sets the invalidationAuditDaemon variable.
     */
    public void setInvalidationAuditDaemon(InvalidationAuditDaemon invalidationAuditDaemon) {
        this.invalidationAuditDaemon = invalidationAuditDaemon;
    }

    /**
     * This implements the method in the Thread class.
     */

    @Override
    public void start() {
        if (invalidationAuditDaemon == null) {
            throw new IllegalStateException("invalidationAuditDaemon must be set before start()");
        }
        super.start();
    }

    /**
     * This invalidates all cache entries in all caches whose template
     * is specified.
     * 
     * @param template The Template that is used to to invalidate fragments.
     * @param waitOnInvalidation True indicates that this method should
     *            not return until all invalidations have taken effect.
     *            False indicates that the invalidations will take effect the next
     *            time the BatchUpdateDaemon wakes.
     */
    public void invalidateByTemplate(String template, boolean waitOnInvalidation, DCache cache) {
        synchronized (this) {
            BatchUpdateList bul = getUpdateList(cache);
            bul.invalidateByTemplateEvents.put(template, new InvalidateByTemplateEvent(template, CachePerf.LOCAL));
        }
        if (waitOnInvalidation) {
            wakeUp(0, 0);
        }
    }

    /**
     * This will send a "CLEAR" command to all caches.
     */
    public void cacheCommand_Clear(boolean waitOnInvalidation, DCache cache) {
        String template = cache.getCacheName();
        synchronized (this) {
            BatchUpdateList bul = getUpdateList(cache);
            bul.invalidateByIdEvents.clear();
            bul.invalidateByTemplateEvents.clear();
            bul.pushCacheEntryEvents.clear();
            bul.pushECFEvents.clear();
            InvalidateByTemplateEvent invalidateByTemplateEvent = new InvalidateByTemplateEvent(template, CachePerf.LOCAL);
            invalidateByTemplateEvent.setCacheCommand_Clear();
            bul.invalidateByTemplateEvents.put(template, invalidateByTemplateEvent);
        }
        if (waitOnInvalidation) {
            wakeUp(0, 0);
        }
    }

    /**
     * This invalidates a cache entry in all
     * caches (with the same cache name)
     * whose cache id or data id is specified.
     * 
     * @param id The id (cache id or data id) that is used to to
     *            invalidate fragments.
     * @param waitOnInvalidation True indicates that this method should
     *            not return until all invalidations have taken effect.
     *            False indicates that the invalidations will take effect the next
     *            time the BatchUpdateDaemon wakes.
     * @param causeOfInvalidation The cause of this invalidation.
     * @param checkPreInvalidationListener true indicates that we will verify with the preInvalidationListener
     *            prior to invalidating. False means we will bypass this check.
     */
    public void invalidateById(Object id, int causeOfInvalidation, boolean waitOnInvalidation, DCache cache, boolean checkPreInvalidationListener) {
        invalidateById(id, causeOfInvalidation, CachePerf.LOCAL, waitOnInvalidation, InvalidateByIdEvent.INVOKE_INTERNAL_INVALIDATE_BY_ID, InvalidateByIdEvent.INVOKE_DRS_RENOUNCE,
                       cache, checkPreInvalidationListener);
    }

    public void invalidateById(Object id, int causeOfInvalidation, int sourceOfInvalidation, boolean waitOnInvalidation, boolean invokeInternalInvalidateById,
                               boolean invokeDRSRenounce, DCache cache, boolean checkPreInvalidationListener) { // LI4337-17
        if (checkPreInvalidationListener && cache.isEnableListener() && cache.getEventSource().getPreInvalidationListenerCount() > 0 &&
            causeOfInvalidation != InvalidationEvent.LRU && causeOfInvalidation != InvalidationEvent.DISK_OVERFLOW) {
            if (cache.getEventSource().shouldInvalidate(id, sourceOfInvalidation, causeOfInvalidation) == false) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "invalidateById() cacheName=" + cache.getCacheName() + " skip invalidation of id=" + id
                                 + " because PreInvalidationListener.shouldInvalidate() returns false.");
                }
                return;
            }
        }
        synchronized (this) {
            BatchUpdateList bul = getUpdateList(cache);
            InvalidateByIdEvent invalEvent = new InvalidateByIdEvent(id, causeOfInvalidation, sourceOfInvalidation, invokeInternalInvalidateById, invokeDRSRenounce, cache.getCacheName()); // LI4337-17
            invalEvent.setClassLoaderType(cache.getCacheConfig().isUseServerClassLoader());
            bul.invalidateByIdEvents.put(id, invalEvent);
        }
        if (waitOnInvalidation) {
            wakeUp(0, 0);
        }
    }

    /**
     * This invalidates all cache entries in all caches whose cache id
     * or data id is specified. Assumes this is the result of a direct
     * invalidation.
     * 
     * @param id The id (cache id or data id) that is used to to
     *            invalidate fragments.
     * @param waitOnInvalidation True indicates that this method should
     *            not return until all invalidations have taken effect.
     *            False indicates that the invalidations will take effect the next
     *            time the BatchUpdateDaemon wakes.
     * @param causeOfInvalidation The cause of this invalidation.
     */
    public void invalidateById(Object id, boolean waitOnInvalidation, DCache cache) {
        invalidateById(id, CachePerf.DIRECT, waitOnInvalidation, cache, Cache.CHECK_PREINVALIDATION_LISTENER);
    }

    /**
     * This allows a cache entry to be added to the BatchUpdateDaemon.
     * The cache entry will be added to all caches.
     * 
     * @param cacheEntry The cache entry to be added.
     */
    public synchronized void pushCacheEntry(CacheEntry cacheEntry, DCache cache) {
        BatchUpdateList bul = getUpdateList(cache);
        bul.pushCacheEntryEvents.add(cacheEntry);
    }

    /**
     * This allows an external cache fragment to be added to the
     * BatchUpdateDaemon.
     * 
     * @param cacheEntry The external cache fragment to be added.
     */
    public synchronized void pushExternalCacheFragment(ExternalInvalidation externalCacheFragment, DCache cache) {
        BatchUpdateList bul = getUpdateList(cache);
        bul.pushECFEvents.add(externalCacheFragment);
    }

    /**
     * This allows a cache entry to be added to the BatchUpdateDaemon.
     * The cache entry will be added to all caches.
     * 
     * @param cacheEntry The cache entry to be added.
     */
    public synchronized void pushAliasEntry(AliasEntry aliasEntry, DCache cache) {
        BatchUpdateList bul = getUpdateList(cache);
        bul.aliasEntryEvents.add(aliasEntry);
    }

    /**
     * This implements the method in RealTimeDaemon. It copies the eventVector,
     * processes it to remove multiple sets on the same entry and sets followed
     * by invalidations on the same entry, and applies the filtered vector to
     * all caches (first in remote CacheUnits, then in the local internal cache
     * and finally in external caches).
     */
    @Override
    protected void wakeUp(long startDaemonTime, long startWakeUpTime) {
        try {
            BatchUpdateList currentUpdates[];
            synchronized (this) {
                int sz = updates.size();
                if (sz > 0) {
                    currentUpdates = new BatchUpdateList[sz];
                    updates.values().toArray(currentUpdates);
                    updates.clear();
                } else {
                    // no events to process, out of here.
                    return;
                }
            }
            ArrayList pushCacheEntryList = new ArrayList();
            for (int i = 0; i < currentUpdates.length; i++) {
                BatchUpdateList bul = null;
                try {
                    bul = currentUpdates[i];
                    // need to save all pushed CEs in an unfiltered
                    // list so we can dec the refCount after they
                    // have been pushed.
                    pushCacheEntryList.addAll(bul.pushCacheEntryEvents);
                    cleanUpEventLists(bul.cache, bul.invalidateByIdEvents,
                                      bul.invalidateByTemplateEvents,
                                      bul.pushCacheEntryEvents, bul.pushECFEvents,
                                      bul.aliasEntryEvents); // 245015

                    if (bul.invalidateByIdEvents.size() > 0) {
                        invalidationAuditDaemon.registerInvalidations(bul.cache.getCacheName(), bul.invalidateByIdEvents.values().iterator());
                    }

                    if (bul.invalidateByTemplateEvents.size() > 0)
                        invalidationAuditDaemon.registerInvalidations(bul.cache.getCacheName(), bul.invalidateByTemplateEvents.values().iterator());

                    if (bul.pushCacheEntryEvents.size() > 0)
                        bul.pushCacheEntryEvents = invalidationAuditDaemon.filterEntryList(bul.cache.getCacheName(), bul.pushCacheEntryEvents);

                    if (bul.pushECFEvents.size() > 0)
                        bul.pushECFEvents = invalidationAuditDaemon.filterExternalCacheFragmentList(bul.cache.getCacheName(), bul.pushECFEvents);

                    if ((bul.invalidateByIdEvents.size() > 0)
                        || (bul.invalidateByTemplateEvents.size() > 0)
                        || (bul.pushCacheEntryEvents.size() > 0)) {

                        RemoteServices remoteServices = bul.cache
                                        .getRemoteServices();

                        // if DRS is ready but congested add the content to a
                        // buffer which will be pushed in the next cycle
                        if (remoteServices.isDRSReady()
                            && remoteServices.isDRSCongested()) {

                            synchronized (drsBuffer) {

                                addToDRSBuffer(bul); // also removes from the drsbuffer

                                BatchUpdateList bufferList = (BatchUpdateList) drsBuffer
                                                .get(bul.cache);

                                if (null != bufferList) {

                                    // remove push entries added to buffer from
                                    // the pushCacheEntryEvents list since we do
                                    // not want finish()to be called on those
                                    // entries
                                    Iterator it = bufferList.pushCacheEntryEvents
                                                    .iterator();

                                    while (it.hasNext()) {
                                        CacheEntry ce = (CacheEntry) it.next();
                                        int index = pushCacheEntryList
                                                        .indexOf(ce);
                                        if (index >= 0)
                                            pushCacheEntryList.remove(index);
                                    }
                                }
                            }
                        } else if (remoteServices.isDRSReady()
                                   && !remoteServices.isDRSCongested()) {

                            BatchUpdateList drsBufferList = (BatchUpdateList) drsBuffer
                                            .get(bul.cache);
                            // if DRS is not congested, then try to push the
                            // content in the drsBuffer along with
                            // the current content.
                            BatchUpdateList mergedList = null;
                            if (drsBufferList != null) {
                                try {

                                    synchronized (drsBuffer) {

                                        mergedList = mergeLists(drsBufferList,
                                                                bul);
                                        drsBuffer.remove(bul.cache);
                                    }
                                    cleanUpEventLists(
                                                      mergedList.cache,
                                                      mergedList.invalidateByIdEvents,
                                                      mergedList.invalidateByTemplateEvents,
                                                      mergedList.pushCacheEntryEvents,
                                                      mergedList.pushECFEvents,
                                                      mergedList.aliasEntryEvents);
                                    remoteServices
                                                    .batchUpdate(
                                                                 mergedList.invalidateByIdEvents,
                                                                 mergedList.invalidateByTemplateEvents,
                                                                 mergedList.pushCacheEntryEvents,
                                                                 mergedList.aliasEntryEvents);
                                } finally {
                                    if (mergedList != null) {
                                        Iterator it = mergedList.pushCacheEntryEvents
                                                        .iterator();
                                        while (it.hasNext()) {
                                            ((CacheEntry) it.next()).finish();
                                        }
                                    }// if

                                }// finally
                            } else
                                remoteServices.batchUpdate(
                                                           bul.invalidateByIdEvents,
                                                           bul.invalidateByTemplateEvents,
                                                           bul.pushCacheEntryEvents,
                                                           bul.aliasEntryEvents);
                        }
                    }

                    // cache already has the fragments in its cache
                    // it just needs to be invalidated
                    // the cache will update the InvTemplateEvents with the
                    // cache ids each event removed
                    if ((bul.invalidateByIdEvents.size() > 0)
                        || (bul.invalidateByTemplateEvents.size() > 0)) {
                        bul.cache.batchUpdate(bul.invalidateByIdEvents,
                                              bul.invalidateByTemplateEvents,
                                              EMPTY_ARRAYLIST);
                    }

                    if ((bul.invalidateByIdEvents.size() > 0)
                        || (bul.invalidateByTemplateEvents.size() > 0)
                        || (bul.pushECFEvents.size() > 0)) {

                        if (externalCacheServices != null) {
                            if (bul.cache.getCacheConfig().isEnableServletSupport()
                                || bul.cache.getCacheConfig().isEnableInterCellInvalidation()) {
                                externalCacheServices.batchUpdate(
                                                                  bul.invalidateByIdEvents,
                                                                  bul.invalidateByTemplateEvents,
                                                                  bul.pushECFEvents);
                            }
                        }
                    }
                } finally {
                    // ---------------------------------
                    // Dec the ref count
                    // ---------------------------------
                    Iterator pushCacheEntryIterator = pushCacheEntryList
                                    .iterator();
                    while (pushCacheEntryIterator.hasNext()) {
                        ((CacheEntry) pushCacheEntryIterator.next()).finish();
                    }
                    pushCacheEntryList.clear();
                    // ---------------------------------
                } // try
            } // for
        } finally {
            // ---------------------------------
            // Refresh Cache Perf
            // Trim Cache
            // ---------------------------------
            Map caches = ServerCache.getCacheInstances();
            Iterator i = caches.values().iterator();
            while (i.hasNext()) {
                DCache cache = (DCache) i.next();
                cache.refreshCachePerf();
                /*
                 * 398807 - Remove debug statement because these messages are
                 * printed out too often and fill up the trace log. You can get
                 * PushPullTable size using MBean Statistics if
                 * (tc.isDebugEnabled() && (System.currentTimeMillis() -
                 * this.lastTimeForTrace) > TRACE_WINDOW) { Tr.debug(tc,
                 * "cacheName=" + cache.cacheName + " CE Pool count=" +
                 * cache.cePoolCount + " PushPullTableSize=" +
                 * cache.getPushPullTableSize()); }
                 */
            }
            // ---------------------------------
        }
    }

    /**
     * Because it's too complicated to keep all the state necessary for sorting
     * these events as they happen, we collect all events in a single vector and
     * then sort them upon batch. Such complications would include temporary
     * Hashtables for invalidating fragments.
     */
    private void cleanUpEventLists(DCache cache, HashMap invalidateIdEvents, HashMap invalidateTemplateEvents, ArrayList pushEntryEvents, ArrayList pushECFEvents,
                                   ArrayList aliasEntryEvents) { //245015

        // Remove invalidateByIdEvent if the CacheEntry is newer than the invalidate request.
        Iterator it = invalidateIdEvents.values().iterator();
        while (it.hasNext()) {
            InvalidateByIdEvent invalidateByIdEvent = (InvalidateByIdEvent) it.next();
            Object id = invalidateByIdEvent.getId();
            CacheEntry cacheEntry = cache.getEntryFromMemory(id); //245015
            // In PushPull mode, dmap.put() --> cache.invalidateAndSet() --> (1) get old value; (2) invalidate but not performing renounce id
            // in DRS's BatchUpdate. The not-InvalidateByIdEvent.INVOKE_DRS_RENOUNCE ('False') will be passed when creating InvalidateByEvent;
            // (3)put entry in memory and performing announce id in DRS's BatchUpdate. For this case, we need to keep both invalidate event
            // in the InvalidateIdEvents and id in the PushEntryEvents. 
            if (cacheEntry != null &&
                invalidateByIdEvent.getTimeStamp() < cacheEntry.getTimeStamp() &&
                invalidateByIdEvent.isInvokeDRSRenounce() == true) { //495487
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "cleanUpEventLists(): Filtered out InvalidateByIdEvent when cache entry is newer in memory cache. cacheName=" + cache.getCacheName() + " id=" + id);

                }
                it.remove();
            }
        }

        // check pushEntryEvents to see if id, data ids, or template are in invalidate lists
        it = pushEntryEvents.iterator();
        while (it.hasNext()) {
            boolean remove = false;
            CacheEntry cacheEntry = (CacheEntry) it.next();
            if (invalidateIdEvents.containsKey(cacheEntry.getIdObject())) {
                InvalidateByIdEvent invalidateByIdEvent = (InvalidateByIdEvent) invalidateIdEvents.get(cacheEntry.getIdObject());
                // In PushPull mode, dmap.put() --> cache.invalidateAndSet() --> (1) get old value; (2) invalidate but not performing renounce id
                // in DRS's BatchUpdate. The not-InvalidateByIdEvent.INVOKE_DRS_RENOUNCE ('False') will be passed when creating InvalidateByEvent;
                // (3)put entry in memory and performing announce id in DRS's BatchUpdate. For this case, we need to keep both invalidate event
                // in the InvalidateIdEvents and id in the PushEntryEvents. 
                if (invalidateByIdEvent.isInvokeDRSRenounce() == true) { //495487
                    remove = true;
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc,
                                 "cleanUpEventLists(): Filtered out pushEntryEvents when id is n invalidation list. cacheName=" + cache.getCacheName() + " id="
                                                 + cacheEntry.getIdObject());
                    }
                }
            }
            if (!remove) {
                Enumeration e = cacheEntry.getDataIds();
                while (e.hasMoreElements()) {
                    Object did = e.nextElement();
                    if (invalidateIdEvents.containsKey(did)) {
                        remove = true;
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "cleanUpEventLists(): Filtered out pushEntryEvents when dependency id is in invalidation list. cacheName=" + cache.getCacheName() + " id="
                                         + cacheEntry.getIdObject() + " depid=" + did);
                        }
                        break;
                    }
                }
            }
            if (!remove) {
                Enumeration e = cacheEntry.getTemplates();
                while (e.hasMoreElements()) {
                    Object template = e.nextElement();
                    if (invalidateIdEvents.containsKey(template)) {
                        remove = true;
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "cleanUpEventLists(): Filtered out pushEntryEvents when template is in invalidation list. cacheName=" + cache.getCacheName() + " id="
                                         + cacheEntry.getIdObject() + " template=" + template);
                        }
                        break;
                    }
                }
            }

            // Remove PUSH event if the ce is not serializable
            if (!remove && !cacheEntry.prepareForSerialization()) {
                // a msg was logged during the above call
                remove = true;
            }

            if (remove) {
                it.remove();
            }
        }

        // check aliasEntryEvents to see if id is in invalidate lists or 
        // the CacheEntry is newer than the add/remove alias request.
        it = aliasEntryEvents.iterator();
        while (it.hasNext()) {
            boolean remove = false;
            AliasEntry aliasEntry = (AliasEntry) it.next();
            if (invalidateIdEvents.containsKey(aliasEntry.id)) {
                remove = true;
            } else {
                Iterator it_ce = pushEntryEvents.iterator();
                while (it_ce.hasNext()) {
                    CacheEntry cacheEntry = (CacheEntry) it_ce.next();
                    if (cacheEntry.getIdObject().equals(aliasEntry.id)) {
                        if (cacheEntry.getTimeStamp() > aliasEntry.getTimeStamp()) {
                            remove = true;
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, "cleanUpEventLists(): Filtered out aliasEntryEvents when cache entry is newer than alias request. cacheName=" + cache.getCacheName()
                                             + " id=" + cacheEntry.getIdObject());
                            }
                            break;
                        }
                    }
                }
            }
            if (remove) {
                it.remove();
            }
        }

        //check pushECFEvents to make sure they should not be invalidated
        it = pushECFEvents.iterator();
        while (it.hasNext()) {
            boolean remove = false;
            ExternalInvalidation externalCacheFragment = (ExternalInvalidation) it.next();

            Enumeration enumeration = externalCacheFragment.getTemplates();
            while (!remove && enumeration.hasMoreElements()) {
                String template = (String) enumeration.nextElement();
                if (invalidateTemplateEvents.containsKey(template)) {
                    remove = true;
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "cleanUpEventLists(): Filtered out pushECFEvents when template is in invalidation list. cacheName=" + cache.getCacheName() + " template="
                                     + template);
                    }
                }
            }

            enumeration = externalCacheFragment.getInvalidationIds();
            while (!remove && enumeration.hasMoreElements()) {
                String id = (String) enumeration.nextElement();
                if (invalidateIdEvents.containsKey(id)) {
                    remove = true;
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "cleanUpEventLists(): Filtered out pushECFEvents when invalidation id is in invalidation list. cacheName=" + cache.getCacheName() + " ide="
                                     + id);
                    }
                }
            }

            if (remove)
                it.remove();
        }

    }

    private final BatchUpdateList getUpdateList(DCache cache) {
        BatchUpdateList bul = (BatchUpdateList) updates.get(cache);
        if (bul == null) {
            bul = new BatchUpdateList();
            bul.cache = cache;
            if (cache.getCacheConfig().isDefaultCacheProvider()) {
                updates.put(cache, bul);
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "WARNING getUpdateList called for " + cache.getCacheName());
                }
            }
        }
        return bul;
    }

    private void addToDRSBuffer(BatchUpdateList newList) {

        if (newList != null) {

            BatchUpdateList oldList = (BatchUpdateList) drsBuffer
                            .get(newList.cache);
            // if there is already a buffer for this cache then merge the
            // existing list with the new list
            if (oldList != null) {

                DCache cache = newList.cache;

                BatchUpdateList list = mergeLists(oldList, newList);

                // around 10% of cache
                int thresholdSize = cache.getCacheConfig().getCacheSize() / 20; // PK57841 fixed deadlock problem
                int size = list.aliasEntryEvents.size()
                           + list.invalidateByIdEvents.size()
                           + list.invalidateByTemplateEvents.size()
                           + list.pushCacheEntryEvents.size();

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "list.congestionCount: " + list.congestionCount);
                    Tr.debug(tc, "drsCongestionThreshold: " + drsCongestionThreshold);
                    Tr.debug(tc, "size: " + size);
                    Tr.debug(tc, "thresholdSize: " + thresholdSize);
                }

                // remove from the buffer and stop replicating if threshold
                // limit has reached.
                if (list.congestionCount++ > drsCongestionThreshold
                    && size > thresholdSize) {

                    Tr.event(tc,
                             "Disabling replication due to DRS congestion for cache instance: "
                                             + list.cache.getCacheName()
                                             + ", invalidatedByIdEvents:"
                                             + list.invalidateByIdEvents.size()
                                             + " invalidateByTemplateEvents:"
                                             + list.invalidateByTemplateEvents.size()
                                             + " pushCacheEntryEvents:"
                                             + list.pushCacheEntryEvents.size()
                                             + " aliasEntryEvents:"
                                             + list.aliasEntryEvents.size());

                    list.cache.getCacheConfig().setDrsDisabled(true);
                    drsBuffer.remove(list.cache);

                } else {
                    drsBuffer.put(list.cache, list);
                }
            } else {
                drsBuffer.put(newList.cache, newList);
            }
        }
    }

    private BatchUpdateList mergeLists(BatchUpdateList bufferedList, BatchUpdateList currentList) {

        BatchUpdateList mergedList = new BatchUpdateList();
        mergedList.cache = bufferedList.cache;
        mergedList.congestionCount = bufferedList.congestionCount;

        //adding invalidationIdEvents to the list
        if (bufferedList.invalidateByIdEvents.size() > 0)
            mergedList.invalidateByIdEvents.putAll(bufferedList.invalidateByIdEvents);
        if (currentList.invalidateByIdEvents.size() > 0)
            mergedList.invalidateByIdEvents.putAll(currentList.invalidateByIdEvents);
        bufferedList.invalidateByIdEvents.clear();

        //adding invalidationTemplateEvents to the list
        if (bufferedList.invalidateByTemplateEvents.size() > 0)
            mergedList.invalidateByTemplateEvents.putAll(bufferedList.invalidateByTemplateEvents);
        if (currentList.invalidateByTemplateEvents.size() > 0)
            mergedList.invalidateByTemplateEvents.putAll(currentList.invalidateByTemplateEvents);
        bufferedList.invalidateByTemplateEvents.clear();

        //remove entry from the drsBuffer if it exists in the current list

        List<CacheEntry> toBeRemoved = new ArrayList<CacheEntry>(); // PK57841 fixed ConcurrentModificationException
        Iterator it = currentList.pushCacheEntryEvents.iterator();
        while (it.hasNext()) {
            CacheEntry ce = (CacheEntry) it.next();
            Iterator it_ce = bufferedList.pushCacheEntryEvents.iterator();
            while (it_ce.hasNext()) {
                CacheEntry drsEntry = (CacheEntry) it_ce.next();
                if (drsEntry != null && drsEntry.getIdObject().equals(ce.getIdObject())) {
                    toBeRemoved.add(drsEntry); // PK57841
                }
            }
        }
        bufferedList.pushCacheEntryEvents.removeAll(toBeRemoved); // PK57841
        toBeRemoved.clear();

        mergedList.pushCacheEntryEvents.addAll(bufferedList.pushCacheEntryEvents);
        mergedList.pushCacheEntryEvents.addAll(currentList.pushCacheEntryEvents);
        bufferedList.pushCacheEntryEvents.clear();

        //remove entry from the drsBuffer if it exists in the current list

        it = currentList.aliasEntryEvents.iterator();
        while (it.hasNext()) {
            AliasEntry ae = (AliasEntry) it.next();
            Iterator it_ae = bufferedList.aliasEntryEvents.iterator();
            while (it_ae.hasNext()) {
                AliasEntry drsEntry = (AliasEntry) it_ae.next();
                if (drsEntry != null && drsEntry.id.equals(ae.id)) {
                    toBeRemoved.add(drsEntry); // PK57841
                }
            }
        }
        bufferedList.aliasEntryEvents.removeAll(toBeRemoved); // PK57841
        toBeRemoved.clear();

        mergedList.aliasEntryEvents.addAll(bufferedList.aliasEntryEvents);
        mergedList.aliasEntryEvents.addAll(currentList.aliasEntryEvents);
        bufferedList.aliasEntryEvents.clear();

        return mergedList;

    }

    static class BatchUpdateList {
        /**
         * This is the sequence of Cache update events.
         * These events are collected here so they can be batched to the
         * caches every batchUpdateDaemonInterval.
         * Elements can include a CacheEntry, an ExternalCacheFragment,
         * an InvalidateByTemplateEvent and an InvalidateByIdEvent.
         */
        public DCache cache;
        public int congestionCount = 0;
        public HashMap invalidateByIdEvents = new HashMap();
        public HashMap invalidateByTemplateEvents = new HashMap();
        public ArrayList pushCacheEntryEvents = new ArrayList();
        public ArrayList pushECFEvents = new ArrayList();
        public ArrayList aliasEntryEvents = new ArrayList();
    }
}
