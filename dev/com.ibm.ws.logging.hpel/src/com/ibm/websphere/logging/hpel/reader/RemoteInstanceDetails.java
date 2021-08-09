/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.logging.hpel.reader;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

/**
 * Object representing a server instance in a query context.
 * 
 * This is the object return in a list from {@link RemoteAllResults} and which is used
 * in remote server instance specific queries to specify the instance to be queried.
 * 
 * @ibm-api
 */
public class RemoteInstanceDetails implements Serializable {
	private static final long serialVersionUID = -2815429026378868108L;
	
	final LogQueryBean query;
	final Date startTime;
	final String[] procPath;
	private RemoteListCache cache = null;
	
	/**
	 * creates instance with a specified time and query.
	 * 
	 * @param query criteria to use for queries on the instance
	 * @param startTime time at which that instance was active
	 * @param subProcs list of keys leading to the child sub-process.
	 */
	public RemoteInstanceDetails(LogQueryBean query, Date startTime, String[] subProcs) {
		this.query = query;
		this.startTime = startTime;
		this.procPath = subProcs;
	}
	
	/**
	 * gets start time of this instance
	 * @return this instance's value of startTime
	 */
	public Date getStartTime() {
		return this.startTime;
	}
	
	/**
	 * gets list of keys leading to the child sub-process
	 * @return this instance's value of subProcs
	 */
	public String[] getProcPath() {
		return this.procPath;
	}
	
	/**
	 * gets query used on this instance
	 * @return this instance's value of query
	 */
	public LogQueryBean getQuery() {
		return this.query;
	}
	
	/**
	 * gets cache for the query result on this instance
	 * @return this instance's value of cache
	 */
	public synchronized RemoteListCache getCache() {
		return cache;
	}
	
	/**
	 * sets cache for the query result on this instance
	 * @param cache new instance's value of cache
	 */
	public synchronized void setCache(RemoteListCache cache) {
		if (cache != null && (this.cache == null || !this.cache.isComplete())) {
			this.cache = cache;
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cache == null) ? 0 : cache.hashCode());
		result = prime * result + ((query == null) ? 0 : query.hashCode());
		result = prime * result
				+ ((startTime == null) ? 0 : startTime.hashCode());
		result = prime * result + Arrays.hashCode(procPath);
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
		RemoteInstanceDetails other = (RemoteInstanceDetails) obj;
		if (cache == null) {
			if (other.cache != null)
				return false;
		} else if (!cache.equals(other.cache))
			return false;
		if (query == null) {
			if (other.query != null)
				return false;
		} else if (!query.equals(other.query))
			return false;
		if (startTime == null) {
			if (other.startTime != null)
				return false;
		} else if (!startTime.equals(other.startTime))
			return false;
		if (!Arrays.equals(procPath, other.procPath))
			return false;
		return true;
	}
}
