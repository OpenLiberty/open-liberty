/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.parser;

import jain.protocol.ip.sip.message.Message;
import jain.protocol.ip.sip.message.Response;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.parser.util.CharsBuffer;
import com.ibm.ws.sip.parser.util.CharsBuffersPool;
import com.ibm.ws.sip.stack.transaction.transport.connections.SipMessageByteBuffer;
import com.ibm.ws.sip.stack.transaction.util.Debug;

/**
 * a parser that reads one SIP message from a network packet.
 * such a parser can be accessed either by constructing
 * a new one, or by calling getGlobalInstance()
 * 
 * @author ran
 */
public class DatagramMessageParser extends MessageParser
{
	/** class Logger */
	private static final LogMgr s_logger = Log.get(DatagramMessageParser.class);

	/**
	 * global, per-thread instance that can be used for parsing messages
	 * coming in from any UDP connection
	 */
	private static final ThreadLocal<DatagramMessageParser> s_instances =
		new ThreadLocal<DatagramMessageParser>() {
			protected DatagramMessageParser initialValue() {
				return new DatagramMessageParser();
			}
		};

	/**
	 * @return the instance that can be used by any UDP connection
	 */
	public static MessageParser getGlobalInstance() {
		return s_instances.get();
	}

	/**
	 * constructs a new datagram message parser
	 */
	public DatagramMessageParser() {
	}
	
	/**
	 * parses one message from the given stream
	 * @param buffer incoming byte stream
	 * @return a complete message, or null if not enough bytes
	 */
	public Message parse(SipMessageByteBuffer buffer) {
		CharsBuffer line = CharsBuffersPool.getBuffer();
		Message message = parse(buffer, line);
		buffer.reset();
		CharsBuffersPool.putBufferBack(line);
		return message;
	}
	
	/**
	 * parses one message from the given stream
	 * @param buffer incoming byte stream
	 * @param line temporary work buffer for parsing each line
	 * @return a complete message, or null if not enough bytes
	 */
	private Message parse(SipMessageByteBuffer buffer, CharsBuffer line) {
		// 1. start-line
		Message message = parseStartLine(buffer, line);
		if (message == null) {
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug("Error: datagram "
					+ "does not contain a full start-line");
			}
			if (s_logger.isTraceFailureEnabled()) {
				StringBuffer failure = new StringBuffer();
				failure.append("bad start-line in packet:\n");
				Debug.hexDump(buffer.getBytes(), 0, buffer.getMarkedBytesNumber(), failure);
				s_logger.traceFailure(failure.toString());
			}
			return null;
		}
		
		// 2. headers
		if (!parseHeaders(buffer, message, line)) {
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug("Error: datagram "
					+ "does not contain a full header section");
			}
			if (s_logger.isTraceFailureEnabled()) {
				StringBuffer failure = new StringBuffer();
				failure.append("bad header at offset [");
				failure.append(Integer.toHexString(buffer.getReadPos()));
				failure.append("] in packet:\n");
				Debug.hexDump(buffer.getBytes(), 0, buffer.getMarkedBytesNumber(), failure);
				s_logger.traceFailure(failure.toString());
			}
			setError(Response.BAD_REQUEST, "Bad Message. Incomplete header");
			return null;
		}
		
		// 3. body
		if (!parseBody(buffer, message)) {
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug("Error: datagram "
					+ "received with partial body");
			}
			if (s_logger.isTraceFailureEnabled()) {
				StringBuffer failure = new StringBuffer();
				failure.append("incomplete body in packet:\n");
				Debug.hexDump(buffer.getBytes(), 0, buffer.getMarkedBytesNumber(), failure);
				s_logger.traceFailure(failure.toString());
			}
			// return error response (if this is a request) per rfc3261 18.3
			setError(Response.BAD_REQUEST, "Bad Message. Incomplete body");
		}
		return message;
	}
	
	/**
	 * a datagram parser never has more than one message per packet
	 * 
	 * @return always false
	 */
	public boolean hasMore() {
		return false;
	}

	/**
	 * datagram messages don't require a content-length header
	 * @return false
	 * @see com.ibm.ws.sip.parser.MessageParser#contentLengthHeaderRequired()
	 */
	protected boolean contentLengthHeaderRequired() {
		return false;
	}

	/**
	 * @see com.ibm.ws.sip.parser.MessageParser#crlfReceived(boolean)
	 */
	protected void crlfReceived(boolean empty) {
	}
}
