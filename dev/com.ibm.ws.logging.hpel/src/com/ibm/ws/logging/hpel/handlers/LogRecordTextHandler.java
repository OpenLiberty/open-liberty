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
package com.ibm.ws.logging.hpel.handlers;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import com.ibm.ejs.ras.hpel.HpelHelper;
import com.ibm.websphere.logging.hpel.reader.HpelFormatter;
import com.ibm.ws.logging.hpel.LogRepositoryWriter;
import com.ibm.ws.logging.hpel.SerializationObject;
import com.ibm.ws.logging.hpel.impl.SerializationObjectPool;

/**
 * Implementation of the {@link Handler} interface storing log records in text format.
 */
public class LogRecordTextHandler extends Handler {
	private HpelFormatter formatter = HpelFormatter.getFormatter(HpelFormatter.FORMAT_BASIC);
	private LogRepositoryWriter writer = null;
	private final SerializationObjectPool pool;
	private final byte[] headerBytes;
	private final int traceThreshold;
	private boolean includeTrace = false;
	
	/**
	 * Creates a new instance of the LogRecordTextHandler.
	 * 
	 * @param traceThreshold the threshold used to route log records to the trace writer.
	 */
	public LogRecordTextHandler(int traceThreshold) {
		this.traceThreshold = traceThreshold;
		this.pool = new SerializationObjectPool() {
			public SerializationObject createNewObject() {
				return new SerializationBuffer();
			}
		};
		SerializationObject serializationObject = pool.getSerializationObject();
		try {
			headerBytes = serializationObject.serializeFileHeader(HpelHelper.getHeaderAsProperties());
		} finally {
			pool.returnSerializationObject(serializationObject);
		}
	}

	public void processEvent(LogRecord record) {
		byte[] bytes;

		synchronized(this) {
			if (!includeTrace && record.getLevel().intValue() < traceThreshold) {
				return;
			}
		}
		
		SerializationObject serializationObject = pool.getSerializationObject();
		try {
			bytes = serializationObject.serialize(record);
		} finally {
			pool.returnSerializationObject(serializationObject);
		}
		
		synchronized(this) {
			if (writer != null) {
				writer.logRecord(record.getMillis(), bytes);
			}
		}
	}
	
	/**
	 * @param formatType the String indicating the format to use.
	 */
	public synchronized void setFormat(String formatType) {
		formatter = HpelFormatter.getFormatter(formatType);
	}
	
	/**
	 * @param includeTrace
	 */
	public synchronized void setIncludeTrace(boolean includeTrace) {
		this.includeTrace = includeTrace;
	}
	
	/**
	 * Stops this handler and close its output streams.
	 */
	public void stop() {
		if (this.writer != null) {
			this.writer.stop();
			this.writer.getLogRepositoryManager().stop();
			this.writer = null;
		}
	}
	
	/**
	 * Copy header into provided writer
	 * 
	 * @param writer repository writer to verify
	 * @throws IllegalArgumentException if writer is 'null'
	 */
	public void copyHeader(LogRepositoryWriter writer) throws IllegalArgumentException {
		if (writer == null) {
			throw new IllegalArgumentException("Parameter writer can't be null");
		}
		writer.setHeader(headerBytes);
	}
	
	/**
	 * @return the writer.
	 */
	public synchronized LogRepositoryWriter getWriter() {
		return this.writer;
	}
	
	/**
	 * @param writer the writer to set.
	 */
	public synchronized void setWriter(LogRepositoryWriter writer) {
		if (this.writer != null) {
			this.writer.stop();
		}
		this.writer = writer;
		if (this.writer != null) {
			this.writer.setHeader(headerBytes);
		}
	}
	
	private synchronized HpelFormatter getFormatterType() {
		return formatter;
	}
	
	private class SerializationBuffer implements SerializationObject {
		private final static int BYTE_ARRAY_INITIAL_SIZE = 1024;
		private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(BYTE_ARRAY_INITIAL_SIZE);

		public byte[] serialize(LogRecord record) {
			buffer.reset();
			PrintStream stream = new PrintStream(buffer);
			stream.println(getFormatterType().formatRecord(record));
			stream.flush();
			return buffer.toByteArray();
		}

		public byte[] serializeFileHeader(Properties header) {
			buffer.reset();
			PrintStream stream = new PrintStream(buffer);
			HpelHelper.printHeader(stream, header);
			stream.flush();
			return buffer.toByteArray();
		}
		
	}

    @Override
    public void close() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void flush() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void publish(LogRecord record) {
        processEvent(record);
        
    }
}
