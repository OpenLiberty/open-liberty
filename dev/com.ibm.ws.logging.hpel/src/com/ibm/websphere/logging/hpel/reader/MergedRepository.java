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

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.logging.Level;

/**
 * Helper class to merge separate {@link RepositoryLogRecord} collections together.
 * For merging it uses record's timestamp retrieved via {@link RepositoryLogRecordHeader#getMillis()}
 * method to put records in ascending order in the output. It assumes that records in input streams
 * have already been sorted into ascending order.
 * <p>
 * For example, to merge log records from the recent run of servers storing their
 * log records in repositories the following
 * code can be used:
 * <code>
 * <pre>
 *			// One argument per repository base, each will be merged. Example of arg0:  /opt/IBM/WasX/profiles/AppSrv01/logs/server1
 *	public static void main(String[] args) {
 *		ServerInstanceLogRecordList [] silrlArray = new ServerInstanceLogRecordList[args.length] ;
 *		for (int i = 0; i &lt args.length; i++) {
 *					// Create a repository reader (requires base directory of repository 
 *			RepositoryReader logRepository = new RepositoryReaderImpl(args[i]) ;		
 *					// Pull from just the current instance of the server for merging on appropriate times
 *					// Could pull from all or use filter criteria that would select several, this is for simplicity
 *			silrlArray[i] = logRepository.getLogListForCurrentServerInstance() ;
 *		}
 *		MergedRepository result = new MergedRepository(silrlArray) ;  	// Merge the current serverInstance from each server
 *		for (RepositoryLogRecord repositoryLogRecord : result) {		// For each record in new merged set
 *			String pid = result.getHeader(repositoryLogRecord).getProperty(ServerInstanceLogRecordList.HEADER_PROCESSID) ;
 *				// Just printing some key information here.  Note that the repositoryRecord exposes all fields
 *				// with simple get methods and gives access to the header information associated with each as well
 *			System.out.println("Pid: "+pid+" Rec:  "+repositoryLogRecord.getFormattedMessage());
 *		}
 *	}
 * </pre>
 * </code>
 * 
 * A similar sample, but merging the logs of a z/OS controller with all of the associated servants:
 * <code>
 * <pre>
 * 	public static void main(String[] args) {
 *			// The string arg here is just to the directory where the log files reside (ie: &lt;profileHome&gt;/logs/server1)
 *		RepositoryReader logRepository = new RepositoryReaderImpl(args[0]) ;		
 *				// Get iterator of server instances (start/stop of the server) extracting all log messages with
 *				// severity between INFO and SEVERE.  Lots of different filtering options, this is just one sample
 *		Iterable&lt;ServerInstanceLogRecordList&gt repResults = logRepository.getLogLists(Level.INFO, Level.SEVERE) ;
 *				// Go through each server instance
 *		for (ServerInstanceLogRecordList silrl: repResults) {		// For each list (server lifeCycle)
 *
 *	    	Map &lt;String, ServerInstanceLogRecordList&gt servantMap = silrl.getChildren() ;
 *	        Iterator &lt;String&gt; servantKeys = servantMap.keySet().iterator() ;
 *	        		// Array of lists will be one for each servant + 1 for the controller
 *	        ServerInstanceLogRecordList [] silrlArray = new ServerInstanceLogRecordList[servantMap.size()+1] ;
 *	        int curIdx = 0 ;		// Which index into the array of record lists
 *	        silrlArray[curIdx++] = silrl ;
 *	        while (servantKeys.hasNext()) {
 *	        	String label = servantKeys.next() ;
 *	        	silrlArray[curIdx++] = servantMap.get(label) ;
 *	        }
 *	        System.out.println("\n\n\nPrinting results for a serverInstance\n");
 *			MergedRepository result = new MergedRepository(silrlArray) ;  	// Merge this controller with all servants
 *			for (RepositoryLogRecord repositoryLogRecord : result) {		// For each record in new merged set
 *				String pid = result.getHeader(repositoryLogRecord).getProperty(ServerInstanceLogRecordList.HEADER_PROCESSID) ;
 *					// Just printing some key information here.  Note that the repositoryRecord exposes all fields
 *					// with simple get methods and gives access to the header information associated with each as well
 *				System.out.println(pid+"  "+repositoryLogRecord.getFormattedMessage());
 *			}
 *		}
 *	}
 * </pre>
 * </code>
 * 
 * @ibm-api
 */
public class MergedRepository implements Iterable<RepositoryLogRecord> {
	private static class IteratorState {
		final Iterator<RepositoryLogRecord> it;
		RepositoryLogRecord next;
		IteratorState(Iterable<RepositoryLogRecord> list) {
			it = list.iterator();
			next = it.next();
		}
	}
	
	private class MergedIterator implements Iterator<RepositoryLogRecord> {
		private final IteratorState[] list;
		private int count;
		
		MergedIterator() {
			list = new IteratorState[servers.length];
			count = 0;
			for (int i=0; i<servers.length; i++) {
				list[i] = new IteratorState(servers[i]);
				if (list[i].next != null) {
					count++;
				}
			}
		}

		public boolean hasNext() {
			return count > 0;
		}

		public RepositoryLogRecord next() {
			RepositoryLogRecord next = null;
			
			if (count > 0) {
				int index=0;
				for (int i=1; i<list.length; i++) {
					if (list[index].next == null ||
						(list[i].next != null &&
						list[index].next.getMillis() > list[i].next.getMillis())) {
						index = i;
					}
				}
				
				next = list[index].next;
				list[index].next = list[index].it.next();
				if (list[index].next == null) {
					count--;
				}
				
				if (next != null) {
					next = new RepositoryLogRecordUnique(next);
					headerMap.put(next, servers[index].getHeader());
				}
			}
			
			return next;
		}

		public void remove() {
			throw new UnsupportedOperationException("Method is not applicable to this class");
		}
	}
	
	private final WeakHashMap<RepositoryLogRecord, Properties> headerMap = new WeakHashMap<RepositoryLogRecord, Properties>();
	
	private final ServerInstanceLogRecordList[] servers;
	
	/**
	 * Returns header information for the server this record was created on.
	 * 
	 * @param record instance previously return by an iterator over this merged list.
	 * @return header corresponding to the <code>record</code>.
	 */
	public Properties getHeader(RepositoryLogRecord record) {
		if (!headerMap.containsKey(record)) {
			throw new IllegalArgumentException("Record was not return by an iterator over this instance");
		}
		return headerMap.get(record);
	}
	
	/**
	 * Creates new iterable instance from the list of results obtained from
	 * different servers.
	 * 
	 * @param servers list of log record lists from different servers.
	 */
	public MergedRepository(Iterable<ServerInstanceLogRecordList> servers) {
		if (servers == null) {
			this.servers = new ServerInstanceLogRecordList[0];
		} else {
			int count = 0;
			for (@SuppressWarnings("unused") ServerInstanceLogRecordList server: servers) {
				count++;
			}
			this.servers = new ServerInstanceLogRecordList[count];
			count = 0;
			for (ServerInstanceLogRecordList server: servers) {
				this.servers[count++] = server;
			}
		}
	}
	
	/**
	 * Creates new iterable instance from the list of results obtained from
	 * different servers.
	 * 
	 * @param servers array of log record lists from different servers.
	 */
	public MergedRepository(ServerInstanceLogRecordList[] servers) {
		if (servers == null) {
			this.servers = new ServerInstanceLogRecordList[0];
		} else {
			this.servers = servers;
		}
	}
	
	public Iterator<RepositoryLogRecord> iterator() {
		return new MergedIterator();
	}
	
	/**
	 * Implementation of the {@link RepositoryLogRecord} with {@link #equals(Object)} and
	 * {@link #hashCode()} method inherited from {@link Object} class itself to avoid loosing
	 * entries in the {@link WeakHashMap} used for the <code>headerMap</code> field.
	 */
	private final static class RepositoryLogRecordUnique implements RepositoryLogRecord {
		private static final long serialVersionUID = -2454049300014355679L;
		private final RepositoryLogRecord delegate;
		RepositoryLogRecordUnique(RepositoryLogRecord delegate) {
			this.delegate = delegate;
		}
		public String getExtension(String name) {
			return delegate.getExtension(name);
		}
		public Map<String, String> getExtensions() {
			return delegate.getExtensions();
		}
		public String getFormattedMessage() {
			return delegate.getFormattedMessage();
		}
		public Level getLevel() {
			return delegate.getLevel();
		}
		public int getLocalizable() {
			return delegate.getLocalizable();
		}
		public String getLocalizedMessage() {
			return delegate.getLocalizedMessage();
		}
		public String getLoggerName() {
			return delegate.getLoggerName();
		}
		public String getMessageID() {
			return delegate.getMessageID();
		}
		public String getMessageLocale() {
			return delegate.getMessageLocale();
		}
		public long getMillis() {
			return delegate.getMillis();
		}
		public Object[] getParameters() {
			return delegate.getParameters();
		}
		public byte[] getRawData() {
			return delegate.getRawData();
		}
		public String getRawMessage() {
			return delegate.getRawMessage();
		}
		public RepositoryPointer getRepositoryPointer() {
			return delegate.getRepositoryPointer();
		}
		public String getResourceBundleName() {
			return delegate.getResourceBundleName();
		}
		public long getSequence() {
			return delegate.getSequence();
		}
		public String getSourceClassName() {
			return delegate.getSourceClassName();
		}
		public String getSourceMethodName() {
			return delegate.getSourceMethodName();
		}
		public String getStackTrace() {
			return delegate.getStackTrace();
		}
		public int getThreadID() {
			return delegate.getThreadID();
		}
	}
	
}
