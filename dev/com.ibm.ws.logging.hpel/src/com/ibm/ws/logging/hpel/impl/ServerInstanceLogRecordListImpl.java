/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.hpel.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import com.ibm.websphere.logging.hpel.reader.RemoteListCache;
import com.ibm.websphere.logging.hpel.reader.RepositoryLogRecord;
import com.ibm.websphere.logging.hpel.reader.ServerInstanceLogRecordList;
import com.ibm.ws.logging.hpel.LogRepositoryBrowser;
import com.ibm.ws.logging.hpel.impl.LogRecordBrowser.OnePidRecordListImpl;
import com.ibm.ws.logging.object.hpel.RemoteListCacheImpl;
import com.ibm.ws.logging.object.hpel.RemoteRepositoryCache;
import com.ibm.ws.logging.object.hpel.RepositoryLogRecordImpl;

/**
 * Base class for {@link ServerInstanceLogRecordList} implementations. It provides implementation
 * for the list being used more than once before being discareded.
 */
public abstract class ServerInstanceLogRecordListImpl implements
		ServerInstanceLogRecordList {
	
	/**
	 * Browser the result is retrieved from.
	 */
	protected final LogRepositoryBrowser logBrowser;
	protected final LogRepositoryBrowser traceBrowser;
	private final boolean switched;
	
	protected OnePidRecordListImpl logResult = null;
	protected OnePidRecordListImpl traceResult = null;
	private Properties header = null;
	
	public ServerInstanceLogRecordListImpl(LogRepositoryBrowser logBrowser, LogRepositoryBrowser traceBrowser, boolean switched) {
		if (logBrowser == null && traceBrowser == null) {
			throw new IllegalArgumentException("Either logBrowser or traceBrowser should have value other than 'null'");
		}
		this.logBrowser = logBrowser;
		this.traceBrowser = traceBrowser;
		this.switched = switched;
	}
	
	protected OnePidRecordListImpl getLogResult() {
		if (logResult == null && logBrowser != null) {
			logResult = queryResult(logBrowser);
		}
		return logResult;
	}
	
	protected OnePidRecordListImpl getTraceResult() {
		if (traceResult == null && traceBrowser != null) {
			traceResult = queryResult(traceBrowser);
		}
		return traceResult;
	}
	
	/**
	 * returns this result cache usable for remote transport
	 * 
	 * @return cache instance used in remote log reading
	 */
	public RemoteListCache getCache() {
		RemoteRepositoryCache logCache = getLogResult()==null ? null : getLogResult().getCache();
		RemoteRepositoryCache traceCache = getTraceResult()==null ? null : getTraceResult().getCache();
		return switched ? new RemoteListCacheImpl(traceCache, logCache) : new RemoteListCacheImpl(logCache, traceCache);
	}
	
	/**
	 * sets cache for this result based on the provided one
	 * 
	 * @param cache cache instance received in a remote call
	 */
	public void setCache(RemoteListCache cache) {
		if (cache instanceof RemoteListCacheImpl) {
			RemoteListCacheImpl cacheImpl = (RemoteListCacheImpl)cache;
			if (getLogResult() != null) {
				RemoteRepositoryCache logCache = switched ? cacheImpl.getTraceCache() : cacheImpl.getLogCache();
				if (logCache != null) {
					getLogResult().setCache(logCache);
				}
			}
			if (getTraceResult() != null) {
				RemoteRepositoryCache traceCache = switched ? cacheImpl.getLogCache() : cacheImpl.getTraceCache();
				if (traceCache != null) {
					getTraceResult().setCache(traceCache);
				}
			}
		} else {
			throw new IllegalArgumentException("Unknown implementation of the RemoteListCache instance");
		}
	}
	
	/**
	 * Query for internal format of results. If there's not records in result it return
	 * an empty instance and not an <code>null</code>.
	 * 
	 * @param browser location to browser result
	 * @return OnePidRecordListImpl instance
	 */
	public abstract OnePidRecordListImpl queryResult(LogRepositoryBrowser browser);
	
	/* (non-Javadoc)
	 * @see com.ibm.websphere.logging.hpel.reader.ServerInstanceLogRecordList#getChildren()
	 */
	public Map<String, ServerInstanceLogRecordList> getChildren() {
		HashMap<String, ServerInstanceLogRecordList> map = new HashMap<String, ServerInstanceLogRecordList>();
		if (traceBrowser == null) {
			for(Map.Entry<String, LogRepositoryBrowser> entry: logBrowser.getSubProcesses().entrySet()) {
				ServerInstanceLogRecordList value = new ServerInstanceLogRecordListImpl(entry.getValue(), null, switched) {
					@Override
					public OnePidRecordListImpl queryResult(LogRepositoryBrowser browser) {
						return ServerInstanceLogRecordListImpl.this.queryResult(browser);
					}
				};
				map.put(entry.getKey(), value);
			}
		} else if (logBrowser == null) {
			for(Map.Entry<String, LogRepositoryBrowser> entry: traceBrowser.getSubProcesses().entrySet()) {
				ServerInstanceLogRecordList value = new ServerInstanceLogRecordListImpl(entry.getValue(), null, !switched) {
					@Override
					public OnePidRecordListImpl queryResult(LogRepositoryBrowser browser) {
						return ServerInstanceLogRecordListImpl.this.queryResult(browser);
					}
				};
				map.put(entry.getKey(), value);
			}
		} else {
			Map<String, LogRepositoryBrowser> logSubProcs = logBrowser.getSubProcesses();
			Map<String, LogRepositoryBrowser> traceSubProcs = traceBrowser.getSubProcesses();
			HashSet<String> keys = new HashSet<String>();
			keys.addAll(logSubProcs.keySet());
			keys.addAll(traceSubProcs.keySet());
			for (String key: keys) {
				ServerInstanceLogRecordList value = new ServerInstanceLogRecordListImpl(logSubProcs.get(key), traceSubProcs.get(key), switched) {
					@Override
					public OnePidRecordListImpl queryResult(LogRepositoryBrowser browser) {
						return ServerInstanceLogRecordListImpl.this.queryResult(browser);
					}
				};
				map.put(key, value);
			}
		}
		return map;
	}

	/* (non-Javadoc)
	 * @see com.ibm.websphere.logging.hpel.reader.ServerInstanceLogRecordList#getHeader()
	 */
	public Properties getHeader() {
		if (header == null) {
			OnePidRecordListImpl logResult = getLogResult();
			if (logResult != null) {
				header = logResult.getHeader();
			}
			if (header == null) {
				OnePidRecordListImpl traceResult = getTraceResult();
				if (traceResult != null) {
					header = traceResult.getHeader();
				}
			}
			// If not real header anywhere get the parsed one
			if (header == null && logResult != null) {
				header = logResult.getParsedHeader();
			}
			if (header == null && traceResult != null) {
				header = traceResult.getParsedHeader();
			}
		}
		return header;
	}
	
	public Date getStartTime() {
		if (logBrowser != null && traceBrowser != null) {
			if (logBrowser.getTimestamp() < traceBrowser.getTimestamp()) {
				return new Date(logBrowser.getTimestamp());
			} else {
				return new Date(traceBrowser.getTimestamp());
			}
		} else if (logBrowser != null) {
			return new Date(logBrowser.getTimestamp());
		} else if (traceBrowser != null) {
			return new Date(traceBrowser.getTimestamp());
		} else {
			return null;
		}
	}
	
	public Iterator<RepositoryLogRecord> iterator() {
		return getNewIterator(0, -1);
	}
	
	public Iterable<RepositoryLogRecord> range(final int offset, final int length) {
		return new Iterable<RepositoryLogRecord>() {
			public Iterator<RepositoryLogRecord> iterator() {
				return getNewIterator(offset, length);
			}
		};
	}
	
	/**
	 * Creates new OnePidRecordIterator returning records in the range.
	 * 
	 * @param offset the number of records skipped from the beginning of the list.
	 * @param length the number of records to return.
	 * 
	 * @return OnePidRecordIterator instance.
	 */
	protected Iterator<RepositoryLogRecord> getNewIterator(int offset, int length) {
		OnePidRecordListImpl logResult = getLogResult();
		OnePidRecordListImpl traceResult = getTraceResult();
		if (logResult == null && traceResult == null) {
			return EMPTY_ITERATOR;
		} else if (traceResult == null) {
			return logResult.getNewIterator(offset, length);
		} else if (logResult == null) {
			return traceResult.getNewIterator(offset, length);
		} else {
			MergedServerInstanceLogRecordIterator result = new MergedServerInstanceLogRecordIterator(logResult, traceResult);
			result.setRange(offset, length);
			return result;
		}
	}

	public static final Iterator<RepositoryLogRecord> EMPTY_ITERATOR = new Iterator<RepositoryLogRecord>() {
		public boolean hasNext() {
			return false;
		}
		public RepositoryLogRecord next() {
			return null;
		}
		public void remove() {
			throw new UnsupportedOperationException("Method is not applicable to this class");
		}
	};
	
	/**
	 * Implementation of a {@link RepositoryLogRecord} iterator combining log and trace records recorded
	 * by the same process.
	 */
	private static class MergedServerInstanceLogRecordIterator implements Iterator<RepositoryLogRecord> {
		private final LogRecordBrowser.OnePidRecordIterator it1;
		private final LogRecordBrowser.OnePidRecordIterator it2;
		private RepositoryLogRecordImpl next1 = null;
		private RepositoryLogRecordImpl next2 = null;
		private RepositoryLogRecord next = null;
		private int countDown = -1;
		
		MergedServerInstanceLogRecordIterator(OnePidRecordListImpl list1, OnePidRecordListImpl list2) {
			this.it1 = (LogRecordBrowser.OnePidRecordIterator)list1.getNewIterator(0,-1);
			this.it2 = (LogRecordBrowser.OnePidRecordIterator)list2.getNewIterator(0,-1);
		}
		
		void setRange(int offset, int length) {
			if (offset == 0 && length < 0) {
				return;
			}
			int total1 = it1.total();
			int total2 = it2.total();
			int index1;
			int index2;
			if (offset < 0) {
				index1 = total1 - 1;
				index2 = total2 - 1;
				while (index1 >= 0 && index2 >= 0) {
					// Skip files with no result records
					int size1 = it1.size(index1);
					if (size1 == 0) {
						index1--;
						continue;
					}
					int size2 = it2.size(index2);
					if (size2 == 0) {
						index2--;
						continue;
					}
					long firstId1 = it1.getFirstId(index1);
					long lastId1 = it1.getLastId(index1);
					long firstId2 = it2.getFirstId(index2);
					long lastId2 = it2.getLastId(index2);

					// If all records in it1 file come before all records in it2 file
					if (lastId1 < firstId2) {
						// Check if we can skip it2 file completely.
						if (size2 <= -offset) {
							offset += size2;
							index2--;
							continue;
						}

						break;
					}

					// If all record in it1 file come after all record in it2 file
					if (lastId2 < firstId1) {
						// Check if we can skip it1 file completely.
						if (size1 <= -offset) {
							offset += size1;
							index1--;
							continue;
						} 
						
						break;
					}

					if (size1 + size2 > -offset) {
						break;
					}

					if (firstId1 < firstId2) {
						offset += size2;
						index2--;
					} else {
						offset += size1;
						index1--;
					}

				}
				
				if (index1 < 0) {
					it1.setRange(0, 0, -1);
					while (index2 >= 0 && it2.size(index2) < -offset) {
						offset += it2.size(index2--);
					}
					if (index2 >= 0) {
						it2.setRange(index2, it2.size(index2)+offset, -1);
					} else {
						it2.setRange(0, 0, -1);
						// offset is bigger than whatever we have in files, adjust returned number of records
						length = length>-offset ? length+offset : 0;
					}
				} else if (index2 < 0) {
					it2.setRange(0, 0, -1);
					while (index1 >= 0 && it1.size(index1) < -offset) {
						offset += it1.size(index1--);
					}
					if (index1 >= 0) {
						it1.setRange(index1, it1.size(index1)+offset, -1);
					} else {
						it1.setRange(0, 0, -1);
						// offset is bigger than whatever we have in files, adjust returned number of records
						length = length>-offset ? length+offset : 0;
					}
				} else {
					it1.setRange(index1, 0, -1);
					it2.setRange(index2, 0, -1);
					int size = it1.size(index1) + it2.size(index2) + offset;
					while (size > 0 && next() != null) {
						size--;
					}
				}
			} else {
				index1 = 0;
				index2 = 0;
				while (index1 < total1 && index2 < total2) {
					// Skip files with no result records
					int size1 = it1.size(index1);
					if (size1 == 0) {
						index1++;
						continue;
					}
					int size2 = it2.size(index2);
					if (size2 == 0) {
						index2++;
						continue;
					}
					long firstId1 = it1.getFirstId(index1);
					long lastId1 = it1.getLastId(index1);
					long firstId2 = it2.getFirstId(index2);
					long lastId2 = it2.getLastId(index2);

					// If all records in it1 file come before all records in it2 file
					if (lastId1 < firstId2) {
						// Check if we can skip it1 file completely.
						if (size1 <= offset) {
							offset -= size1;
							index1++;
							continue;
						} 
						
						break;
					}

					// If all records in it1 file come after all records in it2 file
					if (lastId2 < firstId1) {
						// Check if we can skip it2 file completely.
						if (size2 <= offset) {
							offset -= size2;
							index2++;
							continue;
						}
						break;
					}

					if (size1 + size2 > offset) {
						break;
					}

					if (lastId1 < lastId2) {
						offset -= size1;
						index1++;
					} else {
						offset -= size2;
						index2++;
					}

				}
				
				if (index1 == total1) {
					it1.close();
					while (index2 < total2 && it2.size(index2) <= offset) {
						offset -= it2.size(index2++);
					}
					if (index2 < total2) {
						it2.setRange(index2, offset, length);
					} else {
						it2.close();
					}
				} else if (index2 == total2) {
					it2.close();
					while (index1 < total1 && it1.size(index1) <= offset) {
						offset -= it1.size(index1++);
					}
					if (index1 < total1) {
						it1.setRange(index1, offset, length);
					} else {
						it1.close();
					}
				} else {
					it1.setRange(index1, 0, -1);
					it2.setRange(index2, 0, -1);
					while (offset > 0 && next() != null) {
						offset--;
					}
				}
			}
			countDown = length;
		}
		
		public boolean hasNext() {
			if (next == null) {
				next = getNext();
			}
			
			return next != null;
		}
		
		private RepositoryLogRecord getNext() {
			if (countDown == 0) {
				it1.close();
				it2.close();
				return null;
			}
			// Search one file at a time in each iterator
			while (next1 == null && next2 == null && !it1.isDone() && !it2.isDone()) {
				// Search iterator with the earlier timestamp
				if (it1.compareTo(it2) < 0) {
					next1 = (RepositoryLogRecordImpl)it1.findNext(-1);
				} else {
					next2 = (RepositoryLogRecordImpl)it2.findNext(-1);
				}
			}
			
			if (next1 != null && next2 == null) {
				// We have internal sequence numberfrom it1, see if it2 has records with smaller number
				next2 = (RepositoryLogRecordImpl)it2.findNext(next1.getInternalSeqNumber());
			} else if (next1 == null && next2 != null) {
				// We have internal sequence numberfrom it2, see if it1 has records with smaller number
				next1 = (RepositoryLogRecordImpl)it1.findNext(next2.getInternalSeqNumber());
			} else if (next1 == null && next2 == null) {
				// No records means that one of the iterators ran out of records. Try to get a record from another one.
				if (!it1.isDone()) {
					next1 = (RepositoryLogRecordImpl)it1.next();
				} else {
					next2 = (RepositoryLogRecordImpl)it2.next();
				}
			}

			RepositoryLogRecord result;
			if (next1 == null && next2 == null) {
				result = null;
			} else if (next1 == null) {
				result = next2;
				next2 = null;
			} else if (next2 == null) {
				result = next1;
				next1 = null;
			} else {
				if (next1.getInternalSeqNumber() < next2.getInternalSeqNumber()) {
					result = next1;
					next1 = null;
				} else {
					result = next2;
					next2 = null;
				}
			}
			
			if (countDown > 0) {
				countDown--;
			}
			
			return result;
		}
		
		public RepositoryLogRecord next() {
			if (next == null) {
				next = getNext();
			}
			RepositoryLogRecord result = next;
			next = null;
			return result;
		}
		
		public void remove() {
			throw new UnsupportedOperationException("Method is not applicable to this class");
		}
	}

}
