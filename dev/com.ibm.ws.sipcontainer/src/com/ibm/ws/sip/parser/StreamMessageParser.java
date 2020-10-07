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

import java.io.IOException;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.message.KeepalivePong;
import com.ibm.ws.sip.parser.util.CharsBuffer;
import com.ibm.ws.sip.parser.util.CharsBuffersPool;
import com.ibm.ws.sip.stack.context.MessageContext;
import com.ibm.ws.sip.stack.context.MessageContextFactory;
import com.ibm.ws.sip.stack.transaction.transport.UseCompactHeaders;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection;
import com.ibm.ws.sip.stack.transaction.transport.connections.SipMessageByteBuffer;

/**
 * a parser that reads as many SIP messages as available
 * from a network connection.
 * this parser is stateful, which means it can receive a partial
 * message, parse as much as it can, save the partial message,
 * and continue parsing it when more bytes arrive.
 * 
 * @author ran
 */
public class StreamMessageParser extends MessageParser
{
	/** class Logger */
	private static final LogMgr s_logger = Log.get(StreamMessageParser.class);

	/** state when expecting a start-line */
	private static final int STATE_STARTLINE = 0;
	
	/** state when expecting a header */
	private static final int STATE_HEADERS = 1;
	
	/** state when expecting a body */
	private static final int STATE_BODY = 2;
	
	/** the internal message state, indicating what we expect next */
	private int m_state;

	/** buffer holding unparsed message bytes */
	private SipMessageByteBuffer m_buffer;
	
	/** a partially-parsed message waiting for more network bytes */
	private Message m_message;
	
	/** indicates whether we are waiting for more network bytes */
	private boolean m_waiting;
	
	/** indicates whether to check the remaining bytes in buffer for a message
	 * in case the returned message, by the parsing process, is null. */
	private boolean m_checkRemainingBytesInBuffer;
	
	/**
	 * number of CR+LF sequences received before the start line.
	 * reset to 0 when receiving the start line.
	 * this is used for keeping track of keep-alives, and answering each
	 * "ping" with a "pong" according to RFC 5626 5.4 
	 */
	private int m_crlfReceived;
	private boolean m_isMessageCrlf;

	/** the connection that passes incoming packets to this parser */
	private final SIPConnection m_connection;

	/** global, stateless, keep-alive pong */
	private static final KeepalivePong s_keepalivePong = new KeepalivePong();
	
	/**
	 * constructs a new stream message parser
	 * @param connection the connection that passes received packets to this parser
	 */
	public StreamMessageParser(SIPConnection connection) {
		m_buffer = null;
		m_message = null;
		m_state = STATE_STARTLINE;
		m_waiting = false;
		m_crlfReceived = 0;
		m_connection = connection;
		m_checkRemainingBytesInBuffer = false;
		m_isMessageCrlf = false;
	}
	
	/**
	 * parses one message from the given stream
	 * @param buffer incoming byte stream
	 * @return a complete message, or null if not enough bytes
	 */
	public Message parse(SipMessageByteBuffer buffer) {
		if (m_buffer != buffer) {
			if (m_waiting) {
				// there are leftover bytes from previous call
				if (m_buffer == null) {
					// todo - is this possible?
				}
				
				// append new buffer to existing one
				m_buffer.put(buffer.getBytes(), buffer.getReadPos(), buffer.getRemaining());
				
				// reset the new buffer and use it for future use where it 
				// contains both leftovers from previous cycle and new bytes
				buffer.init();
				buffer.put(m_buffer.getBytes(), m_buffer.getReadPos(), m_buffer.getRemaining());
			}
			
			if (m_buffer != null) {
				// return previous buffer back to pool
				m_buffer.reset();
			}
			m_buffer = buffer;
		}
		
		CharsBuffer line = null;
		
		// 1. start-line
		if (m_state == STATE_STARTLINE) {
			line = CharsBuffersPool.getBuffer();
			m_message = parseStartLine(m_buffer, line);
			if (m_startLineHuntingMode) {
				// Ignores any error until a start line is found or no line is left in the buffer.
				// Note that any parse error in this case is not counted per the connection.
				while ( (m_message == null) && (getErrorCode() != 0) ) {
					m_message = parseStartLine(m_buffer, line);
				}
			}
			if (m_message == null) {
				if (m_isMessageCrlf) {
					m_isMessageCrlf = false;
					m_checkRemainingBytesInBuffer = false;
					m_waiting = false;
				} else {
					m_waiting = true;
					if (m_checkRemainingBytesInBuffer) {
						// All the remaining bytes in buffer were checked
						// but no SIP start-line is found.
						// A SIP start-line is a start-line which 
						// is legal or almost legal (the parse error is not fatal).
						m_checkRemainingBytesInBuffer = false;
					} else {
						m_checkRemainingBytesInBuffer = true;
					}				
				}
				CharsBuffersPool.putBufferBack(line);
				return null;
			}
			m_state = STATE_HEADERS;
		}
		
		// 2. headers
		if (m_state == STATE_HEADERS) {
			if (line == null) {
				line = CharsBuffersPool.getBuffer();
			}
			if (!parseHeaders(m_buffer, m_message, line)) {
				m_waiting = true;
				CharsBuffersPool.putBufferBack(line);
				return null;
			}
			m_state = STATE_BODY;
		}
		if (line != null) {
			CharsBuffersPool.putBufferBack(line);
		}
		
		// 3. body
		if (m_state == STATE_BODY) {
			if (!parseBody(m_buffer, m_message)) {
				m_waiting = true;
				return null;
			}
			m_state = STATE_STARTLINE;
			m_waiting = false;
			m_checkRemainingBytesInBuffer = false;
		}
		return m_message;
	}
	
	/**
	 * determines if there's any point in calling parse() again
	 * before new bytes arrive from the network.
	 * there is no chance parse() will succeed if the previous call
	 * created a partial message, or if the buffer is empty.
	 * @return true if the previous call returned a complete message,
	 *  and there are more unparsed bytes available to read
	 */
	public boolean hasMore() {
		if (!m_waiting) { 
			return m_buffer != null && m_buffer.hasMore();			
		}
		return m_checkRemainingBytesInBuffer && m_buffer != null && m_buffer.hasMore();
	}
	
	/**
	 * stream messages require a content-length header
	 * @return true
	 * @see com.ibm.ws.sip.parser.MessageParser#contentLengthHeaderRequired()
	 */
	protected boolean contentLengthHeaderRequired() {
		return true;
	}

	/**
	 * @see com.ibm.ws.sip.parser.MessageParser#crlfReceived(boolean)
	 */
	protected void crlfReceived(boolean empty) {
		// RFC 5626 5.4:
		// "When a server receives a double CRLF sequence between SIP messages on
		// a connection-oriented transport such as TCP or SCTP, it MUST
		// immediately respond with a single CRLF over the same connection"

		if (empty) {
			if (++m_crlfReceived == 2) {
				if (s_logger.isTraceDebugEnabled()) {
					s_logger.traceDebug(this, "crlfReceived",
						"received a keepalive ping - sending pong");
				}
				m_crlfReceived = 0;

				m_isMessageCrlf = !m_buffer.hasMore();
				MessageContextFactory messageContextFactory = MessageContextFactory.instance();
				MessageContext messageContext = messageContextFactory.getMessageContext(s_keepalivePong);
				messageContext.setSipConnection(m_connection);
				try {
					m_connection.write(messageContext, false, UseCompactHeaders.NEVER);
				}
				catch (IOException e) {
					if (s_logger.isTraceFailureEnabled()) {
						s_logger.traceFailure(this, "crlfReceived", "", e);
					}
				}
			}
		}
		else {
			// received the first line in the message
			m_crlfReceived = 0;
		}
	}
}
