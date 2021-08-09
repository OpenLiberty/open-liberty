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
package com.ibm.ws.logging.hpel.handlers;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import com.ibm.ejs.ras.hpel.HpelHelper;
import com.ibm.ws.logging.hpel.LogRecordSerializer;
import com.ibm.ws.logging.hpel.LogRepositoryManager;
import com.ibm.ws.logging.hpel.LogRepositoryWriter;
import com.ibm.ws.logging.hpel.SerializationObject;
import com.ibm.ws.logging.hpel.impl.LogRecordWrapper;
import com.ibm.ws.logging.hpel.impl.LogRepositorySubManagerImpl;
import com.ibm.ws.logging.hpel.impl.SerializationObjectPool;

/**
 * Implementation of the {@link Handler} interface using {@link LogRepositoryWriter} instances
 * to store log records. Log records are routed to the corresponding repository writer based
 * on their log level.
 */
public class LogRecordHandler extends Handler {
	private final SerializationObjectPool pool;
	private final byte[] headerBytes;
	private final int traceThreshold;
			// 671059   ... used since orb actions are now done async
	private LogRepositorySubManagerImpl logSubManager = null ;
	private LogRepositorySubManagerImpl traceSubManager = null ;
	
	private LogRepositoryWriter logWriter = null;
	private LogRepositoryWriter traceWriter = null;
	private ThreadLocal<Integer> logStackCount = new ThreadLocal<Integer>() {
		@Override
		public Integer initialValue() {
			return 0 ;
		}
	} ;
	
	
	/**
	 * Creates the LogRepositoryHandler instance with <code>formatter</code> as a repository formatter.
	 * 
	 * @param traceThreshold the threshold used to route log records to the trace writer.
	 * @param formatter the LogRecordSerializer assigned to convert log records into byte arrays.
	 * @see LogRepositoryWriter
	 */
	public LogRecordHandler(int traceThreshold, final LogRecordSerializer formatter) {
		this(traceThreshold, formatter, HpelHelper.getHeaderAsProperties()) ;
	}
	
	/**
	 * Creates the LogRepositoryHandler instance with <code>formatter</code> as a repository formatter.
	 * 
	 * @param traceThreshold the threshold used to route log records to the trace writer.
	 * @param formatter the LogRecordSerializer assigned to convert log records into byte arrays.
	 * @param headerProps Properties used to construct headers for log files this handler generates
	 * @see LogRepositoryWriter
	 */
	public LogRecordHandler(int traceThreshold, final LogRecordSerializer formatter, Properties headerProps) throws IllegalArgumentException {
		if (headerProps == null)  throw new IllegalArgumentException("Null properties object to populate headers") ;
		this.pool = new SerializationObjectPool() {
			public SerializationObject createNewObject() {
				return new SerializationBuffer(formatter);
			}
		};
		SerializationObject serializationObject = pool.getSerializationObject();
		try {
			headerBytes = serializationObject.serializeFileHeader(headerProps);
		} finally {
			pool.returnSerializationObject(serializationObject);
		}
		this.traceThreshold = traceThreshold;
	}
	
	/* (non-Javadoc)
	 * @see com.ibm.ws.logging.hpel.WsHandler#processEvent(java.util.logging.LogRecord)
	 */
	public void processEvent(LogRecord record) {
		byte[] bytes;

		SerializationObject serializationObject = pool.getSerializationObject();
		try {
			bytes = serializationObject.serialize(record);
		} finally {
			pool.returnSerializationObject(serializationObject);
		}

		synchronized(this) {
			if (traceWriter != null && record.getLevel().intValue() < traceThreshold) {
				traceWriter.logRecord(record.getMillis(), bytes);
			} else if (logWriter != null) {
				logWriter.logRecord(record.getMillis(), bytes);
			}
		}
	}
	
	/**
	 * Stops this handler and close its output streams.
	 */
	public void stop() {
		if (this.logWriter != null) {
			this.logWriter.stop();
			this.logWriter.getLogRepositoryManager().stop();
			this.logWriter = null;
		}
		if (this.traceWriter != null) {
			this.traceWriter.stop();
			this.traceWriter.getLogRepositoryManager().stop();
			this.traceWriter = null;
		}
	}
	/**
	 * @return the logWriter
	 */
	public synchronized LogRepositoryWriter getLogWriter() {
		return logWriter;
	}

	/**
	 * @param logWriter the logWriter to set
	 */
	public synchronized void setLogWriter(LogRepositoryWriter logWriter) {
		if (this.logWriter != null) {
			this.logWriter.stop();
		}
		this.logWriter = logWriter;
		if (this.logWriter == null) {
			logSubManager = null ;
		} else {
			this.logWriter.setHeader(headerBytes);
			logSubManager = getSubManager(logWriter) ;
		}
	}

	private LogRepositorySubManagerImpl getSubManager(LogRepositoryWriter logWriter) {
		LogRepositoryManager logMgr = logWriter.getLogRepositoryManager() ;
		return (logMgr instanceof LogRepositorySubManagerImpl) ? (LogRepositorySubManagerImpl)logMgr : null ;
	}
	
	/**
	 * @return the traceWriter
	 */
	public synchronized LogRepositoryWriter getTraceWriter() {
		return traceWriter;
	}

	/**
	 * @param traceWriter the traceWriter to set
	 */
	public synchronized void setTraceWriter(LogRepositoryWriter traceWriter) {
		if (this.traceWriter != null) {
			this.traceWriter.stop();
		}
		this.traceWriter = traceWriter;
		if (this.traceWriter == null) {
			traceSubManager = null ;
		} else {
			this.traceWriter.setHeader(headerBytes);
			traceSubManager = getSubManager(traceWriter) ;
		}
	}
	
	private static class SerializationBuffer implements SerializationObject {
		private final static int BYTE_ARRAY_INITIAL_SIZE = 1024;
		private final LogRecordSerializer formatter;
		
		SerializationBuffer(LogRecordSerializer formatter) {
			this.formatter = formatter;
		}

		private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(BYTE_ARRAY_INITIAL_SIZE);

		public byte[] serialize(LogRecord record) {
			buffer.reset();
			DataOutputStream stream = new DataOutputStream(buffer);
			
			try {
				formatter.serialize(new LogRecordWrapper(record), stream);
			
				stream.flush();
				stream.close();
			} catch (IOException ex) {
				// We are writting into in memory buffer. If we got IOException something is very wrong in runtime.
				throw new RuntimeException("failed to write into memory buffer", ex);
			}
			
			return buffer.toByteArray();
		}

		public byte[] serializeFileHeader(Properties header) {
			buffer.reset();
			DataOutputStream stream = new DataOutputStream(buffer);
			
			try {
				formatter.serializeFileHeader(header, stream);
				stream.flush();
				stream.close();
			} catch (IOException ex) {
				// We are writting into in memory buffer. If we got IOException something is very wrong in runtime.
				throw new RuntimeException("failed to write into memory buffer", ex);
			}
			
			return buffer.toByteArray();
		}
		
	}

    @Override
    public void close() {
        stop();
        
    }

    @Override
    public void flush() {
    }

    @Override
    public void publish(LogRecord record) {
    	Integer stackLogLevel = -1;
		if (logSubManager != null || traceSubManager != null)
			stackLogLevel = logStackCount.get();
		logStackCount.set(stackLogLevel + 1);
        processEvent(record);
        if (stackLogLevel == 0) {
	        boolean handleQueuedActions = true ;
	        while (handleQueuedActions) {
	        	handleQueuedActions = false ;
	    		if (traceSubManager != null) {
					handleQueuedActions = traceSubManager.sendNotifications() || handleQueuedActions ;
				} 
	    		if (logSubManager != null) {
					handleQueuedActions = logSubManager.sendNotifications() || handleQueuedActions ;
				}
			}
        }
	        
		if (logSubManager != null || traceSubManager != null) {
			if (logSubManager != null) {
				logSubManager.purgeOldFilesAsync();
			}
			if (traceSubManager != null) {
				traceSubManager.purgeOldFilesAsync();
			}
			logStackCount.set(stackLogLevel);
		}
    }
}
