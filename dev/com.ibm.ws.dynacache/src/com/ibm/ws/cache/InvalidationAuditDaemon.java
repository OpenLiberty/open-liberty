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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ibm.ws.cache.intf.ExternalInvalidation;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This class handles synchronization between invalidations and 
 * set cache entries.
 * This auditing ensures that an entry does not get set if there is 
 * an invalidation with an earlier timestamp.  
 * To do this, it saves invalidation for a configurable period of time.
 * The daemon wakes periodically to prune old invalidations to reduce 
 * the overhead of doing these audits.
 * Read/write locks are used to enable higher concurrency than exclusive locks,
 * especially since most accesses are reads.
 * This class uses the InvalidationEvent interface to deal uniformly with 
 * InvalidationByIdEvent, InvalidationByTemplateEvent,
 * CacheEntry and ExternalCacheFragment classes.
 */
public class InvalidationAuditDaemon extends RealTimeDaemon {
	TraceComponent tc = Tr.register(InvalidationAuditDaemon.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
	
	private long lastTimeCleared = 0;
	private volatile Map<String, InvalidationTableList> cacheinvalidationTables = new ConcurrentHashMap<String, InvalidationTableList>(5, 0.75f, 2);
	
	/**
	 * Constructor with parameters.
	 *
	 * @param timeHoldingInvalidations The length of time (in ms) 
	 * invalidations are remembered. 
	 */
	public InvalidationAuditDaemon(int timeHoldingInvalidations) {
		super(timeHoldingInvalidations);
		lastTimeCleared = System.currentTimeMillis();
	}

	/**
	 * This is the method in the RealTimeDaemon base class. 
	 * It is called periodically (period is timeHoldingInvalidations).
	 *
	 * @param startDaemonTime The absolute time when this daemon was first 
	 * started.
	 * @param startWakeUpTime The absolute time when this wakeUp call 
	 * was made.  
	 */
	public void wakeUp(long startDaemonTime, long startWakeUpTime) {
		lastTimeCleared = startWakeUpTime;
		
		for (Map.Entry<String, InvalidationTableList> entry : cacheinvalidationTables.entrySet()) {
			InvalidationTableList invalidationTableList = entry.getValue();
			try {
				
				invalidationTableList.readWriteLock.writeLock().lock();
				
				invalidationTableList.pastIdSet.clear();
				Map<Object, InvalidationEvent> temp = invalidationTableList.pastIdSet;
				invalidationTableList.pastIdSet = invalidationTableList.presentIdSet;
				invalidationTableList.presentIdSet = invalidationTableList.futureIdSet;
				invalidationTableList.futureIdSet = temp;

				invalidationTableList.pastTemplateSet.clear();
				temp = invalidationTableList.pastTemplateSet;
				invalidationTableList.pastTemplateSet = invalidationTableList.presentTemplateSet;
				invalidationTableList.presentTemplateSet = invalidationTableList.futureTemplateSet;
				invalidationTableList.futureTemplateSet = temp;

			} finally {
				invalidationTableList.readWriteLock.writeLock().unlock();
			}
		}
	}
	

	/**
	 * This adds id and template invalidations.
	 * 
	 * @param invalidations
	 *            The list of invalidations. This is a Vector of either
	 *            InvalidateByIdEvent or InvalidateByTemplateEvent.
	 */
	public void registerInvalidations(String cacheName, Iterator invalidations) {

		InvalidationTableList invalidationTableList = getInvalidationTableList(cacheName);

		if (invalidationTableList != null) {
			try {
				invalidationTableList.readWriteLock.writeLock().lock();
				
				while (invalidations.hasNext()) {
					try {
						Object invalidation = invalidations.next();
						if (invalidation instanceof InvalidateByIdEvent) {
							InvalidateByIdEvent idEvent = (InvalidateByIdEvent) invalidation;

							Object id = idEvent.getId();
							InvalidateByIdEvent oldIdEvent = (InvalidateByIdEvent) invalidationTableList.presentIdSet.get(id);

							long timeStamp = idEvent.getTimeStamp();
							if ((oldIdEvent != null) && (oldIdEvent.getTimeStamp() >= timeStamp)) {
								continue;
							}
							invalidationTableList.presentIdSet.put(id, idEvent);
							continue;
						}
						InvalidateByTemplateEvent templateEvent = (InvalidateByTemplateEvent) invalidation;

						String template = templateEvent.getTemplate();

						InvalidateByTemplateEvent oldTemplateEvent = (InvalidateByTemplateEvent) invalidationTableList.presentTemplateSet.get(template);

						long timeStamp = templateEvent.getTimeStamp();
						if ((oldTemplateEvent != null) && (oldTemplateEvent.getTimeStamp() >= timeStamp)) {
							continue;
						}
						invalidationTableList.presentTemplateSet.put(template, templateEvent);
					} catch (Exception ex) {
						com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.InvalidationAuditDaemon.registerInvalidations", "126", this);
					}
				}
			} finally {
				invalidationTableList.readWriteLock.writeLock().unlock();
			}
		}
	}

	/**
	 * This ensures the specified CacheEntrys have not been invalidated.
	 *
	 * @param cacheEntry The unfiltered CacheEntry.
	 * @return The filtered CacheEntry.
	 */
	public CacheEntry filterEntry(String cacheName, CacheEntry cacheEntry) {
		
		InvalidationTableList invalidationTableList = getInvalidationTableList(cacheName);
		try {
			invalidationTableList.readWriteLock.readLock().lock();
			return internalFilterEntry(cacheName, invalidationTableList, cacheEntry);
		} finally {
			invalidationTableList.readWriteLock.readLock().unlock();
		}
	}

	/**
	 * This ensures all incoming CacheEntrys have not been invalidated.
	 *
	 * @param incomingList The unfiltered list of CacheEntrys.
	 * @return The filtered list of CacheEntrys.
	 */
	public ArrayList filterEntryList(String cacheName, ArrayList incomingList) {
		InvalidationTableList invalidationTableList = getInvalidationTableList(cacheName);
		try {
			invalidationTableList.readWriteLock.readLock().lock();
			Iterator it = incomingList.iterator();
			while (it.hasNext()) {
				Object obj = it.next();
				if (obj instanceof CacheEntry) {// ignore any "push-pull" String id's in DRS case
					CacheEntry cacheEntry = (CacheEntry) obj;
					if (internalFilterEntry(cacheName, invalidationTableList, cacheEntry) == null) {
						if (tc.isDebugEnabled()) {
							Tr.debug(tc, "filterEntryList(): Filtered OUT cacheName=" + cacheName + " id=" + cacheEntry.id);
						}
						it.remove();
					}
				}
			}
			return incomingList;

		} finally {
			invalidationTableList.readWriteLock.readLock().unlock();
		}
	}
	
	/**
	 * This is a helper method that filters a single CacheEntry.
	 * It is called by the filterEntry and filterEntryList methods.
	 * 
	 * @param cacheEntry The unfiltered CacheEntry.
	 * @return The filtered CacheEntry.
	 */
	private final CacheEntry internalFilterEntry(String cacheName, InvalidationTableList invalidationTableList, CacheEntry cacheEntry) {
		InvalidateByIdEvent idEvent = null;
		if (cacheEntry == null) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "internalFilterEntry(): Filtered cacheName=" + cacheName + " CE == NULL");
			}
			return null;
		} else if (cacheEntry.id == null) {		
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "internalFilterEntry(): Filtered cacheName=" + cacheName + " id == NULL");
			}
		    return null;
		}

		long timeStamp = cacheEntry.getTimeStamp();
		idEvent = (InvalidateByIdEvent) invalidationTableList.presentIdSet.get(cacheEntry.id);
		if ((idEvent != null) && (idEvent.getTimeStamp() > timeStamp)) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "internalFilterEntry(): Filtered Found a more recent InvalidateByIdEvent for the cacheEntry in presentIdSet. cacheName=" + cacheName + " id=" + cacheEntry.id);
			}
			return null;
		}
		if (collision(invalidationTableList.presentTemplateSet, cacheEntry.getTemplates(), timeStamp) || collision(invalidationTableList.presentIdSet, cacheEntry.getDataIds(), timeStamp)) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "internalFilterEntry(): Filtered due to preexisting invalidations due to dependencies and templates in present template and IdSet. cacheName=" + cacheName + " id=" + cacheEntry.id);
			}
			return null;
		}
		if (timeStamp > lastTimeCleared) {
			return cacheEntry;
		}
		//else check past values as well
		idEvent = (InvalidateByIdEvent) invalidationTableList.pastIdSet.get(cacheEntry.id);

		if ((idEvent != null) && (idEvent.getTimeStamp() > timeStamp)) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "internalFilterEntry(): Filtered Found a more recent InvalidateByIdEvent for the cacheEntry in pastIdSet. cacheName=" + cacheName + " id=" + cacheEntry.id);
			}
			return null;
		}
		if (collision(invalidationTableList.pastTemplateSet, cacheEntry.getTemplates(), timeStamp) || collision(invalidationTableList.pastIdSet, cacheEntry.getDataIds(), timeStamp)) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "internalFilterEntry(): Filtered due to preexisting invalidations due to dependencies and templates in past template and IdSet. cacheName=" + cacheName + " id=" + cacheEntry.id);
			}
			return null;
		}
		return cacheEntry;
	}
	/**
	 * This ensures that the specified ExternalCacheFragment has not 
	 * been invalidated.
	 * 
	 * @param externalCacheFragment The unfiltered ExternalCacheFragment.
	 * @return The filtered ExternalCacheFragment.
	 */
	public ExternalInvalidation filterExternalCacheFragment(String cacheName, ExternalInvalidation externalCacheFragment) {
		InvalidationTableList invalidationTableList = getInvalidationTableList(cacheName);
		try {
			invalidationTableList.readWriteLock.readLock().lock();
			return internalFilterExternalCacheFragment(cacheName, invalidationTableList, externalCacheFragment);
		} finally {
			invalidationTableList.readWriteLock.readLock().unlock();
		}
	}

	/**
	 * This ensures all incoming ExternalCacheFragments have not been 
	 * invalidated.
	 * 
	 * @param incomingList The unfiltered list of ExternalCacheFragments.
	 * @return The filtered list of ExternalCacheFragments.
	 */
	public ArrayList filterExternalCacheFragmentList(String cacheName, ArrayList incomingList) {
		InvalidationTableList invalidationTableList = getInvalidationTableList(cacheName);
		try {
			invalidationTableList.readWriteLock.readLock().lock();
			Iterator it = incomingList.iterator();
			while (it.hasNext()) {
				ExternalInvalidation externalCacheFragment = (ExternalInvalidation) it.next();

				if (null == internalFilterExternalCacheFragment(cacheName, invalidationTableList, externalCacheFragment)) {
					if (tc.isDebugEnabled()) {
						Tr.debug(tc, "filterExternalCacheFragmentList(): Filtered OUT cacheName=" + cacheName + " uri=" + externalCacheFragment.getUri());
					}
					it.remove();
				}
			}
			return incomingList;

		} finally {
			invalidationTableList.readWriteLock.readLock().unlock();
		}
	}

	/**
	 * This is a helper method that filters a single ExternalCacheFragment.
	 * It is called by the filterExternalCacheFragment and 
	 * filterExternalCacheFragmentList methods.
	 * 
	 * @param externalCacheFragment The unfiltered ExternalCacheFragment.
	 * @return The filtered ExternalCacheFragment.
	 */
	private final ExternalInvalidation internalFilterExternalCacheFragment(String cacheName, InvalidationTableList invalidationTableList, ExternalInvalidation externalCacheFragment) {
		if (externalCacheFragment == null) {
			return null;
		}
		long timeStamp = externalCacheFragment.getTimeStamp();
		if (collision(invalidationTableList.presentTemplateSet, externalCacheFragment.getInvalidationIds(), timeStamp) || collision(invalidationTableList.presentIdSet, externalCacheFragment.getTemplates(), timeStamp)) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "internalFilterExternalCacheFragment(): Filtered due to preexisting invalidations due to dependencies and templates in present template and IdSet. cacheName=" + cacheName + " url=" + externalCacheFragment.getUri());
			}
			return null;
		}
		if (timeStamp > lastTimeCleared) {
			return externalCacheFragment;
		}
		//else check past values as well
		if (collision(invalidationTableList.pastTemplateSet, externalCacheFragment.getInvalidationIds(), timeStamp) || collision(invalidationTableList.pastIdSet, externalCacheFragment.getTemplates(), timeStamp)) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "internalFilterExternalCacheFragment(): Filtered due to preexisting invalidations due to dependencies and templates in past template and IdSet. cacheName=" + cacheName + " url=" + externalCacheFragment.getUri());
			}
			return null;
		}
		return externalCacheFragment;
	}

	/**
	 * This is a helper method that checks for a collision.
	 *
	 * @param hashtable A Hashtable of invalidation events.
	 * @param enumeration An Enumeration of 
	 * @return True if there is an item in the enumeration
	 * that is specified in the hashtable such that
	 * the hashtable entry is newer than the timeStamp.     
	 */
	private final boolean collision(Map<Object, InvalidationEvent> hashtable, Enumeration enumeration, long timeStamp) {
		while (enumeration.hasMoreElements()) {
			Object key = enumeration.nextElement();	
			InvalidationEvent invalidationEvent = (InvalidationEvent) hashtable.get(key);	
			if ((invalidationEvent != null) && (invalidationEvent.getTimeStamp() > timeStamp)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Retrieve the InvalidationTableList by the specified cacheName.
	 */
	private InvalidationTableList getInvalidationTableList(String cacheName) {  
		InvalidationTableList  invalidationTableList = cacheinvalidationTables.get(cacheName);
		if (invalidationTableList == null) {
			synchronized (this) {
				invalidationTableList = new InvalidationTableList();
				cacheinvalidationTables.put(cacheName, invalidationTableList);
			}
		}
		return invalidationTableList;
	}
	
	/**
	 * This notifies this daemon that a specified cache instance has cleared. It
	 * clears the internal tables
	 * 
	 * @param cache The cache instance.
	 */
	public void cacheCleared(String cacheName) {
            InvalidationTableList list = (InvalidationTableList) cacheinvalidationTables.get(cacheName);
            if (list != null ) 
                    list.clear();
	}
	
	/**
	 * The InvalidationTableList structure. Each cache instance has its own InvalidationTableList.
	 */
	static class InvalidationTableList {  
		
		public Map<Object, InvalidationEvent> pastIdSet = new HashMap<Object,  InvalidationEvent>(500);
		public Map<Object, InvalidationEvent> presentIdSet = new HashMap<Object, InvalidationEvent>(500);
		public Map<Object, InvalidationEvent> pastTemplateSet = new HashMap<Object, InvalidationEvent>(100);
		public Map<Object, InvalidationEvent> presentTemplateSet = new HashMap<Object, InvalidationEvent>(100);		
		public Map<Object, InvalidationEvent> futureIdSet = new HashMap<Object,  InvalidationEvent>(500);
		public Map<Object, InvalidationEvent> futureTemplateSet = new HashMap<Object, InvalidationEvent>(100);
		
		public ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
		
		public void clear() {
			try {
				readWriteLock.writeLock().lock();
				pastIdSet.clear();
				presentIdSet.clear();
				pastTemplateSet.clear();
				presentTemplateSet.clear();
			} finally {
				readWriteLock.writeLock().unlock();
			}
		}
    }
}
