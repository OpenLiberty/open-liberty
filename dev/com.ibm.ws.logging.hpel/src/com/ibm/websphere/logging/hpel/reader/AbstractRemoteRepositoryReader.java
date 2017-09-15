/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.logging.hpel.reader;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.logging.hpel.impl.RemoteLogRecordIterator;


/**
 * Abstract implementation of the RepositoryReader for remote reading of log records.
 * 
 * Extensions of this class need need to provide transportation of parameters and results over the wire.
 * On the server side of the call {@link #readLogLists(LogQueryBean, RepositoryPointer)} should result in
 * {@link RemoteResultCollector#getLogLists(LogQueryBean, RepositoryPointer)} to be called and
 * {@link #readLogRecords(RemoteInstanceDetails, RepositoryPointer, int, int, Locale)} - in {@link RemoteResultCollector#getLogListForServerInstance(RemoteInstanceDetails, RepositoryPointer, int, int, Locale)}.
 * 
 * @ibm-api
 */
public abstract class AbstractRemoteRepositoryReader implements RepositoryReader {
	private final static String thisClass = AbstractRemoteRepositoryReader.class.getName();
	private final static Logger logger = Logger.getLogger(thisClass);
	
	// Language all records should be translated to before return.
	private final Locale locale;
	private final int maxRecords;
	
	/**
	 * constructs instance returning log records translated into specified Locale.
	 * 
	 * @param locale language all records should be translated to. <code>null</code> means not translation required.
	 */
	protected AbstractRemoteRepositoryReader(Locale locale) {
		this(-1, locale);
	}
	
	/**
	 * constructs instance returning log records translated into specified Locale and with a
	 * limit on number of records requested in each call to server.
	 * 
	 * @param maxRecords limit on number of records in each request
	 * @param locale language all records should be translated to. <code>null</code> means not translation required.
	 */
	protected AbstractRemoteRepositoryReader(int maxRecords, Locale locale) {
		this.locale = locale;
		this.maxRecords = maxRecords;
	}
	
	/**
	 * Returns limit on records set for this instance.
	 * @return maxRecords value
	 */
	public int getMaxRecords() {
		return maxRecords;
	}

	private Iterator<RepositoryLogRecord> getIterator(RemoteInstanceDetails indicator, RepositoryPointer after, int offset, int length) {
		return new RemoteLogRecordIterator(this, indicator, after, offset, length, locale);
	}
	
	private ServerInstanceLogRecordList getLogListForServerInstance(RemoteInstanceDetails instance, final RepositoryPointer after) throws LogRepositoryException {
		// Just bring header back. May want to optimize later and read header when the first
		// iterator bring records back.
		final RemoteInstanceResult result = readLogRecords(instance, after, 0, 0, locale);
		// Use indicator with the real start time.
		final RemoteInstanceDetails indicator = new RemoteInstanceDetails(instance.query, result.getStartTime(), instance.procPath);
		// Update cache stored in the indicator
		indicator.setCache(result.getCache());
		final Properties header = result.getLogHeader();
		final Set<String> subProcs = result.getSubProcs();
		return new ServerInstanceLogRecordList() {
			public Properties getHeader() {
				return header;
			}

			public Iterable<RepositoryLogRecord> range(final int offset, final int length) {
				return new Iterable<RepositoryLogRecord>() {
					public Iterator<RepositoryLogRecord> iterator() {
						return getIterator(indicator, after, offset, length);
					}
				};
			}

			public Iterator<RepositoryLogRecord> iterator() {
				return getIterator(indicator, after, 0, -1);
			}
			
			public Map<String, ServerInstanceLogRecordList> getChildren() {
				return new AbstractMap<String, ServerInstanceLogRecordList>() {
					@Override
					public Set<java.util.Map.Entry<String, ServerInstanceLogRecordList>> entrySet() {
						return new AbstractSet<Entry<String,ServerInstanceLogRecordList>>() {
							@Override
							public Iterator<java.util.Map.Entry<String, ServerInstanceLogRecordList>> iterator() {
								return new Iterator<Entry<String,ServerInstanceLogRecordList>>() {
									final Iterator<String> it = subProcs.iterator();
									public boolean hasNext() {
										return it.hasNext();
									}

									public java.util.Map.Entry<String, ServerInstanceLogRecordList> next() {
										final String key = it.next();
										return new java.util.Map.Entry<String, ServerInstanceLogRecordList>() {
											public String getKey() {
												return key;
											}
											public ServerInstanceLogRecordList getValue() {
												String[] subKeys = Arrays.copyOf(indicator.getProcPath(), indicator.getProcPath().length+1);
												subKeys[subKeys.length-1] = (String)key;
												RemoteInstanceDetails kidIndicator = new RemoteInstanceDetails(indicator.query, indicator.startTime, subKeys);
												try {
													return getLogListForServerInstance(kidIndicator, null);
												} catch (LogRepositoryException e) {
													throw new LogRepositoryRuntimeException(e);
												}
											}

											public ServerInstanceLogRecordList setValue(
													ServerInstanceLogRecordList object) {
												throw new UnsupportedOperationException("This class is a read-only implementation");
											}
										};
									}

									public void remove() {
										throw new UnsupportedOperationException("This class is a read-only implementation");
									}
								};
							}
							@Override
							public int size() {
								return subProcs.size();
							}
						};
					}
					
				};
			}

			@Override
			public Date getStartTime() {
				return indicator.startTime;
			}
			
		};
	}
	
	/* (non-Javadoc)
	 * @see com.ibm.websphere.logging.hpel.reader.RepositoryReader#getLogListForCurrentServerInstance()
	 */
	public ServerInstanceLogRecordList getLogListForCurrentServerInstance() throws LogRepositoryException {
		return getLogListForServerInstance((Date)null);
	}

	/* (non-Javadoc)
	 * @see com.ibm.websphere.logging.hpel.reader.RepositoryReader#getLogListForServerInstance(java.util.Date)
	 */
	public ServerInstanceLogRecordList getLogListForServerInstance(Date time) throws LogRepositoryException {
		logger.entering(thisClass, "getLogListForServerInstance", time);
		
		LogQueryBean query = new LogQueryBean();
		ServerInstanceLogRecordList result = getLogListForServerInstance(new RemoteInstanceDetails(query, time, new String[0]), null);
		
		logger.exiting(thisClass, "getLogListForServerInstance", result);
		return result;
	}

	/* (non-Javadoc)
	 * @see com.ibm.websphere.logging.hpel.reader.RepositoryReader#getLogListForServerInstance(com.ibm.websphere.logging.hpel.reader.RepositoryPointer)
	 */
	public ServerInstanceLogRecordList getLogListForServerInstance(
			RepositoryPointer after) throws LogRepositoryException {
		logger.entering(thisClass, "getLogListForServerInstance", after);
		
		LogQueryBean query = new LogQueryBean();
		ServerInstanceLogRecordList result = getLogListForServerInstance(new RemoteInstanceDetails(query, null, new String[0]), after);
		
		logger.exiting(thisClass, "getLogListForServerInstance", result);
		return result;
	}

	/* (non-Javadoc)
	 * @see com.ibm.websphere.logging.hpel.reader.RepositoryReader#getLogListForServerInstance(java.util.Date, java.util.logging.Level, java.util.logging.Level)
	 */
	public ServerInstanceLogRecordList getLogListForServerInstance(Date time,
			Level minLevel, Level maxLevel) throws LogRepositoryException {
		logger.entering(thisClass, "getLogListForServerInstance", new Object[]{time, minLevel, maxLevel});
		
		LogQueryBean query = new LogQueryBean();
		query.setLevels(minLevel, maxLevel);
		ServerInstanceLogRecordList result = getLogListForServerInstance(new RemoteInstanceDetails(query, time, new String[0]), null);
		
		logger.exiting(thisClass, "getLogListForServerInstance", result);
		return result;
	}

	/* (non-Javadoc)
	 * @see com.ibm.websphere.logging.hpel.reader.RepositoryReader#getLogListForServerInstance(com.ibm.websphere.logging.hpel.reader.RepositoryPointer, java.util.logging.Level, java.util.logging.Level)
	 */
	public ServerInstanceLogRecordList getLogListForServerInstance(
			RepositoryPointer after, Level minLevel, Level maxLevel) throws LogRepositoryException {
		logger.entering(thisClass, "getLogListForServerInstance", new Object[]{after, minLevel, maxLevel});
		
		LogQueryBean query = new LogQueryBean();
		query.setLevels(minLevel, maxLevel);
		ServerInstanceLogRecordList result = getLogListForServerInstance(new RemoteInstanceDetails(query, null, new String[0]), after);
		
		logger.exiting(thisClass, "getLogListForServerInstance", result);
		return result;
	}

	/* (non-Javadoc)
	 * @see com.ibm.websphere.logging.hpel.reader.RepositoryReader#getLogListForServerInstance(java.util.Date, int)
	 */
	public ServerInstanceLogRecordList getLogListForServerInstance(Date time,
			int threadID) throws LogRepositoryException {
		logger.entering(thisClass, "getLogListForServerInstance", new Object[]{time, threadID});
		
		LogQueryBean query = new LogQueryBean();
		query.setThreadIDs(new String[] {Integer.toHexString(threadID)});
		ServerInstanceLogRecordList result = getLogListForServerInstance(new RemoteInstanceDetails(query, time, new String[0]), null);
		
		logger.exiting(thisClass, "getLogListForServerInstance", result);
		return result;
	}

	/* (non-Javadoc)
	 * @see com.ibm.websphere.logging.hpel.reader.RepositoryReader#getLogListForServerInstance(com.ibm.websphere.logging.hpel.reader.RepositoryPointer, int)
	 */
	public ServerInstanceLogRecordList getLogListForServerInstance(
			RepositoryPointer after, int threadID) throws LogRepositoryException {
		logger.entering(thisClass, "getLogListForServerInstance", new Object[]{after, threadID});
		
		LogQueryBean query = new LogQueryBean();
		query.setThreadIDs(new String[] {Integer.toHexString(threadID)});
		ServerInstanceLogRecordList result = getLogListForServerInstance(new RemoteInstanceDetails(query, null, new String[0]), after);
		
		logger.exiting(thisClass, "getLogListForServerInstance", result);
		return result;
	}

	/**
 	 * returns log records from the repository of a server instance running
	 * at a specified time, according to the criteria specified by the log query
	 * bean.
	 * 
	 * @param  time      {@link Date} value used to determine the
	 *                   server instance where the server start time occurs
	 *                   before this value and the server stop time occurs
	 *                   after this value
	 * @param query      {@link LogQueryBean} instance representing set of criteria
	 *                   each of which need to be met by the return records.
	 * @return           the iterable list of log records
	 * 					 If no records meet the criteria, the list is empty.
	 * @see LogQueryBean
	 */
	public ServerInstanceLogRecordList getLogListForServerInstance(Date time, LogQueryBean query) throws LogRepositoryException {
		logger.entering(thisClass, "getLogListForServerInstance", new Object[]{time, query});
		
		ServerInstanceLogRecordList result = getLogListForServerInstance(new RemoteInstanceDetails(query, time, new String[0]), null);
		
		logger.exiting(thisClass, "getLogListForServerInstance", result);
		return result;
	}
	
	/**
 	 * returns log records from the repository for one server instance that are
	 * beyond a given repository location, according to the criteria specified by the log query
	 * bean.
	 * 
	 * @param  after     pointer to a record the list will start after
	 * @param query      {@link LogQueryBean} instance representing set of criteria
	 *                   each of which need to be met by the return records.
	 * @return           the iterable list of log records
	 * 					 If no records meet the criteria, the list is empty.
	 * @see LogQueryBean
	 */
	public ServerInstanceLogRecordList getLogListForServerInstance(RepositoryPointer after, LogQueryBean query) throws LogRepositoryException {
		logger.entering(thisClass, "getLogListForServerInstance", new Object[]{after, query});
		
		ServerInstanceLogRecordList result = getLogListForServerInstance(new RemoteInstanceDetails(query, null, new String[0]), after);
		
		logger.exiting(thisClass, "getLogListForServerInstance", result);
		return result;
	}

	/**
	 * returns log records from the repository according to the criteria specified
	 * by the log query bean.
	 * 
	 * @param  query     {@link LogQueryBean} instance representing set of criteria
	 *                   each of which need to be met by the return records.
	 * @return           the iterable instance of a list of log records within
	 *                   a process that are within the parameter range 
	 *                   If no records meet the criteria, an Iterable is returned with no entries
	 */	
	public Iterable<ServerInstanceLogRecordList> getLogLists(LogQueryBean query) throws LogRepositoryException {
		return getLogLists(null, query);
	}
	
	/**
	 * returns log records from the repository that are beyond a given
	 * repository location, according to the criteria specified by the log query
	 * bean.
	 * 
	 * @param  after     pointer to a record the list will start after
	 * @param  query     {@link LogQueryBean} instance representing set of criteria
	 *                   each of which need to be met by the return records.
	 * @return           the iterable instance of a list of log records within
	 *                   a process that are within the parameter range 
	 *                   If no records meet the criteria, an Iterable is returned with no entries
	 */	
	public Iterable<ServerInstanceLogRecordList> getLogLists(RepositoryPointer after, LogQueryBean query) throws LogRepositoryException {
		logger.entering(thisClass, "getLogLists", new Object[]{after, query});
		
		RemoteAllResults lists = readLogLists(query, after);
		ArrayList<ServerInstanceLogRecordList> result = new ArrayList<ServerInstanceLogRecordList>();
		if (lists != null) {
			boolean firstInstance = true;
			for (RemoteInstanceDetails indicator: lists.getLogLists()) {
				result.add(getLogListForServerInstance(indicator, firstInstance ? after : null));
				firstInstance = false;
			}
		}
		
		logger.exiting(thisClass, "getLogLists", result);
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.ibm.websphere.logging.hpel.reader.RepositoryReader#getLogLists()
	 */
	public Iterable<ServerInstanceLogRecordList> getLogLists() throws LogRepositoryException {
		logger.entering(thisClass, "getLogLists");
		
		LogQueryBean query = new LogQueryBean();
		Iterable<ServerInstanceLogRecordList> result = getLogLists(null, query);
		
		logger.exiting(thisClass, "getLogLists", result);
		return result;
	}

	/* (non-Javadoc)
	 * @see com.ibm.websphere.logging.hpel.reader.RepositoryReader#getLogLists(com.ibm.websphere.logging.hpel.reader.RepositoryPointer)
	 */
	public Iterable<ServerInstanceLogRecordList> getLogLists(
			RepositoryPointer after) throws LogRepositoryException {
		logger.entering(thisClass, "getLogLists", new Object[]{after});
		
		LogQueryBean query = new LogQueryBean();
		Iterable<ServerInstanceLogRecordList> result = getLogLists(after, query);
		
		logger.exiting(thisClass, "getLogLists", result);
		return result;
	}

	/* (non-Javadoc)
	 * @see com.ibm.websphere.logging.hpel.reader.RepositoryReader#getLogLists(java.util.logging.Level, java.util.logging.Level)
	 */
	public Iterable<ServerInstanceLogRecordList> getLogLists(Level minLevel,
			Level maxLevel) throws LogRepositoryException {
		logger.entering(thisClass, "getLogLists", new Object[]{minLevel, maxLevel});
		
		LogQueryBean query = new LogQueryBean();
		query.setLevels(minLevel, maxLevel);
		Iterable<ServerInstanceLogRecordList> result = getLogLists(null, query);
		
		logger.exiting(thisClass, "getLogLists", result);
		return result;
	}

	/* (non-Javadoc)
	 * @see com.ibm.websphere.logging.hpel.reader.RepositoryReader#getLogLists(com.ibm.websphere.logging.hpel.reader.RepositoryPointer, java.util.logging.Level, java.util.logging.Level)
	 */
	public Iterable<ServerInstanceLogRecordList> getLogLists(
			RepositoryPointer after, Level minLevel, Level maxLevel) throws LogRepositoryException {
		logger.entering(thisClass, "getLogLists", new Object[]{after, minLevel, maxLevel});
		
		LogQueryBean query = new LogQueryBean();
		query.setLevels(minLevel, maxLevel);
		Iterable<ServerInstanceLogRecordList> result = getLogLists(after, query);
		
		logger.exiting(thisClass, "getLogLists", result);
		return result;
	}

	/* (non-Javadoc)
	 * @see com.ibm.websphere.logging.hpel.reader.RepositoryReader#getLogLists(java.util.Date, java.util.Date)
	 */
	public Iterable<ServerInstanceLogRecordList> getLogLists(Date minTime,
			Date maxTime) throws LogRepositoryException {
		logger.entering(thisClass, "getLogLists", new Object[]{minTime, maxTime});
		
		LogQueryBean query = new LogQueryBean();
		query.setTime(minTime, maxTime);
		Iterable<ServerInstanceLogRecordList> result = getLogLists(null, query);
		
		logger.exiting(thisClass, "getLogLists", result);
		return result;
	}

	/* (non-Javadoc)
	 * @see com.ibm.websphere.logging.hpel.reader.RepositoryReader#getLogLists(com.ibm.websphere.logging.hpel.reader.RepositoryPointer, java.util.Date)
	 */
	public Iterable<ServerInstanceLogRecordList> getLogLists(
			RepositoryPointer after, Date maxTime) throws LogRepositoryException {
		logger.entering(thisClass, "getLogLists", new Object[]{after, maxTime});
		
		LogQueryBean query = new LogQueryBean();
		query.setTime(null, maxTime);
		Iterable<ServerInstanceLogRecordList> result = getLogLists(after, query);
		
		logger.exiting(thisClass, "getLogLists", result);
		return result;
	}

	/**
	 * retrieves results for all server instances in the repository.
	 * Implementation should usually result in {@link RemoteResultCollector#getLogLists(LogQueryBean, RepositoryPointer)}
	 * to be invoked with the same parameters on the server side.
	 * 
	 * @param query  log query bean indicator
	 * @param after  reference point after which we need log records.
	 * @return Set of all server instances satisfying the query request.
	 * @throws LogRepositoryException indicating that an error occurred while reading list of instances from the server.
	 */
	public abstract RemoteAllResults readLogLists(LogQueryBean query, RepositoryPointer after) throws LogRepositoryException;

	/**
	 * retrieves records and header for one server instance.
	 * Implementation should usually result in {@link RemoteResultCollector#getLogListForServerInstance(RemoteInstanceDetails, RepositoryPointer, int, int, Locale)}
	 * to be invoked with the same parameters on the server side.
	 * 
	 * @param indicator server instance identifier.
	 * @param after reference point after which we need log records.
	 * @param offset number of records to skip after the reference point.
	 * @param size maximum number of records to return.
	 * @param locale language records should be translated to in the result. <code>null</code> means no translation required.
	 * @return Set of instance log records satisfying the original query request.
	 * @throws LogRepositoryException indicating that an error occurred while reading records from the server.
	 */
	public abstract RemoteInstanceResult readLogRecords(RemoteInstanceDetails indicator, RepositoryPointer after, int offset, int size, Locale locale) throws LogRepositoryException;
	
}
