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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Result of the query on a server instance.
 * 
 * Instance of this class is return from a remote server instance specific queries.
 * It contains header information for this instance and possibly subset of the full query
 * result depending on the restrictions implied by the transport over the wire.
 * 
 * @ibm-api
 */
public class RemoteInstanceResult implements Serializable {
	private static final long serialVersionUID = 2486399602489790552L;
	private final Date startTime;
	private final Properties header;
	private final HashSet<String> subProcs;
	private final ArrayList<RepositoryLogRecord> records = new ArrayList<RepositoryLogRecord>();
	private RemoteListCache cache = null;
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
	     in.defaultReadObject();
	}
	
	/**
	 * creates an empty result for an instance.
	 * 
	 * @param header header information for the queried instance
	 * @param subProcs list of keys in the children map.
	 */
	public RemoteInstanceResult(Date startTime, Properties header, Set<String> subProcs) {
		this.startTime = startTime;
		this.header = header;
		this.subProcs = new HashSet<String>(subProcs);
	}
	
	/**
	 * adds record to the result set.
	 * 
	 * @param record this instance's record satisfying the query criteria
	 */
	public void addRecord(RepositoryLogRecord record) {
		records.add(record);
	}
	
	/**
	 * returns the set of records in this result.
	 * 
	 * @return possibly subset of all the records satisfying query criteria in this instance.
	 */
	public ArrayList<RepositoryLogRecord> getLogList() {
		return this.records;
	}
	
	/**
	 * returns the time of the first log record of the instance
	 * 
	 * @return time of the first log record as a Date object
	 */
	public Date getStartTime() {
		return this.startTime;
	}
	
	/**
	 * returns header information of the instance
	 * 
	 * @return header information as Properties
	 */
	public Properties getLogHeader() {
		return this.header;
	}
	
	/**
	 * returns array of sub-process keys.
	 * 
	 * @return keys to be used in the map retrieved in {@link ServerInstanceLogRecordList#getChildren()} call.
	 */
	public Set<String> getSubProcs() {
		return subProcs;
	}
	
	/**
	 * gets cache for the query result on this instance
	 * @return this instance's value of cache
	 */
	public RemoteListCache getCache() {
		return cache;
	}
	
	/**
	 * sets cache for the query result on this instance
	 * @param cache new instance's value of cache
	 */
	public void setCache(RemoteListCache cache) {
		this.cache = cache;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cache == null) ? 0 : cache.hashCode());
		result = prime * result + ((header == null) ? 0 : header.hashCode());
		result = prime * result + ((records == null) ? 0 : records.hashCode());
		result = prime * result
				+ ((subProcs == null) ? 0 : subProcs.hashCode());
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
		RemoteInstanceResult other = (RemoteInstanceResult) obj;
		if (cache == null) {
			if (other.cache != null)
				return false;
		} else if (!cache.equals(other.cache))
			return false;
		if (header == null) {
			if (other.header != null)
				return false;
		} else if (!header.equals(other.header))
			return false;
		if (records == null) {
			if (other.records != null)
				return false;
		} else if (!records.equals(other.records))
			return false;
		if (subProcs == null) {
			if (other.subProcs != null)
				return false;
		} else if (!subProcs.equals(other.subProcs))
			return false;
		return true;
	}
}
