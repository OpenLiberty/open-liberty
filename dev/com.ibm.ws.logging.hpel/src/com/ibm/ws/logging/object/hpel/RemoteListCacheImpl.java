/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.object.hpel;

import java.io.IOException;

import com.ibm.websphere.logging.hpel.reader.RemoteListCache;


/**
 * Implementation for the combined log and trace cache result.
 */
public class RemoteListCacheImpl implements RemoteListCache {
	private static final long serialVersionUID = 5769609124765147250L;
	
	private final RemoteRepositoryCache logCache;
	private final RemoteRepositoryCache traceCache;
	private transient int size = -1;
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
	     in.defaultReadObject();
	     size = -1;
	}
	/**
	 * constructs new remote list cache object with specified values
	 * @param logCache cache info for log repository
	 * @param traceCache cache info for trace repository
	 */
	public RemoteListCacheImpl(RemoteRepositoryCache logCache, RemoteRepositoryCache traceCache) {
		this.logCache = logCache;
		this.traceCache = traceCache;
	}
	
	/**
	 * gets cache info for log repository
	 * @return current log cache info
	 */
	public RemoteRepositoryCache getLogCache() {
		return logCache;
	}
	
	/**
	 * gets cache info for trace repository
	 * @return current trace cache info
	 */
	public RemoteRepositoryCache getTraceCache() {
		return traceCache;
	}
	
	/**
	 * gets indicator that this instance contain statistics for all files in the result
	 * @return <code>true</code> if both log and trace cache info is complete.
	 */
	public boolean isComplete() {
		return (logCache == null || logCache.isComplete()) &&
			(traceCache == null || traceCache.isComplete());
	}

	/**
	 * gets number of records in the result based on the information available in this cache.
	 * @return sum of sizes recorded in both log and trace caches
	 */
	public int getSize() {
		if (size < 0) {
			size = logCache==null ? 0 : logCache.getSize();
			size += traceCache==null ? 0 : traceCache.getSize();
		}
		return size;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((logCache == null) ? 0 : logCache.hashCode());
		result = prime * result
				+ ((traceCache == null) ? 0 : traceCache.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RemoteListCacheImpl other = (RemoteListCacheImpl) obj;
		if (logCache == null) {
			if (other.logCache != null)
				return false;
		} else if (!logCache.equals(other.logCache))
			return false;
		if (traceCache == null) {
			if (other.traceCache != null)
				return false;
		} else if (!traceCache.equals(other.traceCache))
			return false;
		return true;
	}
}
