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

import java.util.HashSet;
import java.util.Locale;

import com.ibm.ws.logging.hpel.impl.ServerInstanceLogRecordListImpl;
import com.ibm.ws.logging.object.hpel.RepositoryLogRecordImpl;

/**
 * Utility class to collect query results into Serializable form convenient for passing
 * over the wire.
 * 
 * @ibm-api
 */
public class RemoteResultCollector {
	private final RepositoryReader logReader;
	
	/**
	 * constructs collector retrieving results from the provided RepositoryReader implementation.
	 * 
	 * @param logReader log record source to query for the requested data.
	 */
	public RemoteResultCollector(RepositoryReader logReader) {
		this.logReader = logReader;
	}
	
	/**
	 * retrieves results for all server instances in the repository.
	 * 
	 * @param logQueryBean query indicator
	 * @param after starting location of instances to be return.
	 * @return Set of all server instances satisfying the query request.
	 * @throws LogRepositoryException indicating that an error occurred while reading list of instances from the server.
	 */
	public RemoteAllResults getLogLists(LogQueryBean logQueryBean, RepositoryPointer after) throws LogRepositoryException {
		RemoteAllResults result = new RemoteAllResults(logQueryBean);
		Iterable<ServerInstanceLogRecordList> lists;
		if (after == null) {
			lists = logReader.getLogLists(logQueryBean);
		} else {
			lists = logReader.getLogLists(after, logQueryBean);
		}
		for (ServerInstanceLogRecordList instance: lists) {
			result.addInstance(instance.getStartTime());
		}
		return result;
	}

	/**
	 * retrieves records and header for one server instance.
	 * 
	 * @param indicator server instance identifier.
	 * @param after starting location after which records are return.
	 * @param offset number of records to skip.
	 * @param maxRecords maximum number of records to return.
	 * @param locale language into which record messages are translated.
	 * @return Set of instance log records satisfying the original query request.
	 * @throws LogRepositoryException indicating that an error occurred while reading records from the server.
	 */
	public RemoteInstanceResult getLogListForServerInstance(RemoteInstanceDetails indicator, RepositoryPointer after, int offset, int maxRecords, Locale locale) throws LogRepositoryException {
		ServerInstanceLogRecordList instance;
		// Pointer should be used only if start time is null. This way the same query object can be reused
		// for different server instances and start time value will indicate if pointer should be used.
		if (after == null) {
			instance = logReader.getLogListForServerInstance(indicator.getStartTime(), indicator.getQuery());
			for (String key: indicator.getProcPath()) {
				instance = instance.getChildren().get(key);
				if (instance == null) {
					// Specified subprocess is not found (most probably it was purged already). Return an empty result.
					return new RemoteInstanceResult(null, null, new HashSet<String>());
				}
			}
		} else {
			instance = logReader.getLogListForServerInstance(after, indicator.getQuery());
		}
		RemoteInstanceResult result;
		// Return start time, header, and subprocess info only on the first request (where cache is not set yet).
		if (indicator.getCache() == null) {
			result = new RemoteInstanceResult(instance.getStartTime(), instance.getHeader(), instance.getChildren().keySet());
		} else {
			result = new RemoteInstanceResult(null, null, new HashSet<String>());
			if (instance instanceof ServerInstanceLogRecordListImpl) {
				((ServerInstanceLogRecordListImpl)instance).setCache(indicator.getCache());
			}
		}
		// Allow maxRecords to be 0 if caller is interested in instance header only.
		if (maxRecords != 0) {
			for (RepositoryLogRecord record: instance.range(offset, maxRecords)) {
				if (locale != null && !locale.toString().equals(record.getMessageLocale()) && record instanceof RepositoryLogRecordImpl) {
					RepositoryLogRecordImpl recordImpl = (RepositoryLogRecordImpl)record;
					recordImpl.setLocalizedMessage(HpelFormatter.translateMessage(record, locale));
					recordImpl.setMessageLocale(locale.toString());
				}
				result.addRecord(record);
			}
		}
		// Return cache back only if caller does not have complete set
		if (instance instanceof ServerInstanceLogRecordListImpl &&
				(indicator.getCache()==null  || !indicator.getCache().isComplete())) {
			result.setCache(((ServerInstanceLogRecordListImpl)instance).getCache());
		}
		return result;
	}

}
