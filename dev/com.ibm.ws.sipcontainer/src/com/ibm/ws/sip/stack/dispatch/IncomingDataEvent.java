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
package com.ibm.ws.sip.stack.dispatch;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.stack.transaction.transport.TransportCommLayerMgr;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection;
import com.ibm.ws.sip.stack.transaction.transport.connections.SipMessageByteBuffer;
import com.ibm.ws.sip.stack.transaction.util.Debug;

/**
 * event triggered when data arrives from the network
 * 
 * @author ran
 */
class IncomingDataEvent implements Event
{
	/** class logger */
	private static final LogMgr s_logger = Log.get(IncomingDataEvent.class);
	
	/** incoming data contents */
	private SipMessageByteBuffer m_buffer;

	/** transport source */
	private SIPConnection m_source;
	
	/** unique event identifier, for debugging */
	private int m_id;

	/** event identifier counter */
	private static int s_id = 0;
	
	/**
	 * constructor
	 * @param buffer buffer with a copy of the received bytes.
	 *  this buffer should be returned to the pool when no longer needed.
	 * @param source transport source
	 */
	IncomingDataEvent(SipMessageByteBuffer buffer, SIPConnection source) {
		m_buffer = buffer;
		m_source = source;
		m_id = s_id++;
		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug("IncomingDataEvent [" + m_id + "] queued");
		}
	}
	
	/**
	 * called from the dispatch thread to execute this event
	 */
	public void onExecute() {
		if (s_logger.isTraceDebugEnabled()) {
			int len = m_buffer.getMarkedBytesNumber();
			StringBuffer msg = new StringBuffer();
			msg.append("IncomingDataEvent [");
			msg.append(m_id);
			msg.append("] dispatched. [");
			msg.append(len);
			msg.append("] bytes received from [");
			msg.append(m_source == null ? "?" : m_source.toString());
			msg.append("]\n");
			Debug.hexDump(m_buffer.getBytes(), 0, len, msg);
			s_logger.traceDebug(msg.toString());
		}
		TransportCommLayerMgr.instance().onRead(m_buffer, m_source);
	}
}
