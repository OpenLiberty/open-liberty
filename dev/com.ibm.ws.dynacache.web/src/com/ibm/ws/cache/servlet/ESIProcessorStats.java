/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.servlet;

import java.io.IOException;
import java.util.LinkedList;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class ESIProcessorStats extends ESIProcessorRequest {
	private static final TraceComponent _tc = Tr.register(ESIProcessorStats.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
	private int _gatherWhat;
	private String _hostName = null;
	private int _pid = -1;
	private int _cacheHits = -1;
	private int _cacheMissesByUrl = -1;
	private int _cacheMissesById = -1;
	private int _cacheExpires = -1;
	private int _cachePurges = -1;
	private LinkedList _cacheEntryStats = new LinkedList();

	/**
	 * Construct a snapshot of the ESIProcessor statistics.
	 * 
	 * @param name
	 *            The ESIProcessor object.
	 * @param gatherEntries
	 *            The ESIProcessor object.
	 * @return none
	 */
	public ESIProcessorStats(ESIProcessor processor, int gatherWhat) {
		super(processor);
		_gatherWhat = gatherWhat;
		_hostName = processor.getHostName();
		_pid = processor.getPID();
		if (_tc.isDebugEnabled())
			Tr.debug(_tc, "constructor " + processor);
	}

	/**
	 * Return the host name which the ESI processor is running on.
	 * 
	 * @return The host name on which the ESI processor is running.
	 */
	public String getHostName() {
		return _hostName;
	}

	/**
	 * Return the PID of the ESI processor.
	 * 
	 * @return The PID of the ESI processor.
	 */
	public int getPID() {
		return _pid;
	}

	/**
	 * Return the total cache hits.
	 * 
	 * @return The PID of the ESI processor.
	 */
	public int getCacheHits() {
		return _cacheHits;
	}

	/**
	 * Set the total cache hits.
	 * 
	 * @param cacheHits
	 *            The total number of cache hits.
	 */
	public void setCacheHits(int hits) {
		_cacheHits = hits;
		if (_tc.isDebugEnabled())
			Tr.debug(_tc, "setCacheHits " + hits);
	}

	/**
	 * Return the total cache misses because the URL had no entry (i.e. there
	 * was no rule entry for the URL).
	 * 
	 * @return The total cache misses due to a URL miss.
	 */
	public int getCacheMissesByUrl() {
		return _cacheMissesByUrl;
	}

	/**
	 * Set the cache misses because the URL had no entry.
	 * 
	 * @param misses
	 *            The number of cache misses by URL.
	 */
	public void setCacheMissesByUrl(int misses) {
		_cacheMissesByUrl = misses;
		if (_tc.isDebugEnabled())
			Tr.debug(_tc, "setCacheMissesByUrl " + misses);
	}

	/**
	 * Return the total cache misses because the cache id was not found (i.e.
	 * there was a rule entry but there was no entry for the computed cache id).
	 * 
	 * @return The total cache misses due to a cache id miss.
	 */
	public int getCacheMissesById() {
		return _cacheMissesById;
	}

	/**
	 * Set the cache misses because the cache id was not found.
	 * 
	 * @param misses
	 *            The number of cache misses by cache id.
	 */
	public void setCacheMissesById(int misses) {
		_cacheMissesById = misses;
		if (_tc.isDebugEnabled())
			Tr.debug(_tc, "setCacheMissesById " + misses);
	}

	/**
	 * Return the total cache expirations.
	 * 
	 * @return The total number of cache expirations since the last reset.
	 */
	public int getCacheExpires() {
		return _cacheExpires;
	}

	/**
	 * Set the total cache expirations.
	 * 
	 * @param cacheHits
	 *            The total number of cache expirations.
	 */
	public void setCacheExpires(int expires) {
		_cacheExpires = expires;
		if (_tc.isDebugEnabled())
			Tr.debug(_tc, "setCacheExpires " + expires);
	}

	/**
	 * Return the total cache purges.
	 * 
	 * @return The total number of cache purges since the last reset.
	 */
	public int getCachePurges() {
		return _cachePurges;
	}

	/**
	 * Set the total cache purges.
	 * 
	 * @param cacheHits
	 *            The total number of cache purges.
	 */
	public void setCachePurges(int purges) {
		_cachePurges = purges;
		if (_tc.isDebugEnabled())
			Tr.debug(_tc, "setCachePurges " + purges);
	}

	/**
	 * Add stats for a specific cache entry.
	 * 
	 * @param cacheEntry
	 *            A cache entry statistics object.
	 */
	public void addCacheEntryStats(ESICacheEntryStats cacheEntry) {
		_cacheEntryStats.add(cacheEntry);
	}

	/**
	 * Return the ESICacheEntryStats objects gathered by gather().
	 * 
	 * @return The total cache misses due to a cache id miss.
	 */
	public ESICacheEntryStats[] getCacheEntryStats() {
		return (ESICacheEntryStats[]) _cacheEntryStats
				.toArray(new ESICacheEntryStats[0]);
	}

	/**
	 * Submit and await a response for a gather request.
	 * 
	 * @return none
	 */
	public void handle() throws IOException {
		if (_tc.isDebugEnabled())
			Tr.debug(_tc, "submitting gather request for " + _gatherWhat);
		
		
			writeInt(MSG_GATHER);
			writeInt(_gatherWhat);
			flush();
				// Read the response

		int repType; // @PQ91098A
		repType = readInt(); // @PQ91098C NOW get the response type - should be
		if (repType != MSG_GATHER) {
			throw new IOException("expecting gather reply from " + this
					+ "; read " + repType);
		}
		if (_tc.isDebugEnabled())
			Tr.debug(_tc, "waiting for gather response from " + this);
		for (;;) {
			repType = readInt();
			switch (repType) {
			case CACHE_HITS:
				setCacheHits(readInt());
				break;
			case CACHE_MISSES_BY_URL:
				setCacheMissesByUrl(readInt());
				break;
			case CACHE_MISSES_BY_ID:
				setCacheMissesById(readInt());
				break;
			case CACHE_EXPIRES:
				setCacheExpires(readInt());
				break;
			case CACHE_PURGES:
				setCachePurges(readInt());
				break;
			case CACHE_ENTRY:
				ESICacheEntryStats ces = new ESICacheEntryStats();
				ces.setCacheId(readString());
				addCacheEntryStats(ces);
				int endType = readInt();
				if (endType != MSG_END)
					throw new IOException("expecting end of cache entry; read "
							+ endType);
				break;
			case MSG_END:
				if (_tc.isDebugEnabled())
					Tr.debug(_tc, "successful gather response from " + this);
				return;
			default:
				throw new IOException("invalid gather response from " + this
						+ ": " + repType);
			}
		}
	}

}
