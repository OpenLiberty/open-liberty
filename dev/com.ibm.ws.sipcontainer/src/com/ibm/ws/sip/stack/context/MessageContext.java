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
package com.ibm.ws.sip.stack.context;

import jain.protocol.ip.sip.message.Message;

import java.io.IOException;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;
import com.ibm.ws.sip.stack.transaction.transactions.SIPTransaction;
import com.ibm.ws.sip.stack.transaction.transport.IBackupMessageSender;
import com.ibm.ws.sip.stack.transaction.transport.TransportCommLayerMgr;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection;
import com.ibm.ws.sip.stack.transaction.transport.connections.SipMessageByteBuffer;
import com.ibm.ws.sip.stack.transaction.transport.connections.SipStreamConnectionWriteListener;
import com.ibm.ws.sip.stack.util.StackExternalizedPerformanceMgr;
import com.ibm.ws.sip.stack.util.StackTaskDurationMeasurer;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

/**
 * the context holding all relevant information for sending a message
 * the context will be passed through the various elements
 * involved in the sending process and will be notified of
 * events that occur.
 * 
 * @author nogat
 */
public abstract class MessageContext implements TCPWriteCompletedCallback, SipStreamConnectionWriteListener{

	/** class logger */
	private static final LogMgr s_logger = Log.get(MessageContext.class);

	/**
	 * the sip connection
	 */
	protected SIPConnection sipConnection;

	/**
	 * the sip jain message
	 */
	protected Message sipMessage;

	/**
	 * the sip message byte buffer
	 */
	protected SipMessageByteBuffer sipMessageByteBuffer;

	/**
	 * the flag indicating the naptr request has already been made for this context
	 */
	protected boolean naptrCalled = false;

	/**
	 * the sip transaction
	 */
	protected SIPTransaction sipTransaction = null;

	/**
	 * the naptr sender in use - 
	 * we need it to return it to the factory at the end 
	 */
	private IBackupMessageSender sender = null;

	/**
	 * the ws byte buffer
	 */
	private WsByteBuffer wsByteBuffer;
	
	/** 
	 * Performance Manager Interface
	 */
	private static StackExternalizedPerformanceMgr s_perfMgr= null;

	/**
	 * Object that measures the duration of the task in the stack outbound queue
	 */
	private StackTaskDurationMeasurer _sipContainerQueueDuration= null;
	

	/**
	 * true if this request switched from UDP to TCP due to crossing the
	 * MTU boundary. if TCP fails, the transport falls back to UDP.
	 * this is only used in standalone, because only in standalone the TCP
	 * failure can be detected locally. when running in a cluster, the TCP
	 * failure is detected by the proxy, and handled by a PROXYERROR message,
	 * which results in creating a new MessageContext instance for the UDP resend.
	 */
	private boolean m_mtuSwitch = false;

	/**
	 * helper flag used for debugging only (when tracing is enabled).
	 * a non-null value indicates that this object is in the pool, and should
	 * not be accessed. it is reset to null when the instance is borrowed from
	 * the pool. an exception is used instead of a boolean, to provide the
	 * complete call stack of the recycling code point in case the error is
	 * detected. 
	 */
	private RuntimeException m_recycled = null;
	
	/**
	 * True if MessageContext pooling debug is enabled
	 */
	private static final boolean s_messageContextPoolingDebug =
		SIPTransactionStack.instance().getConfiguration().messageContextPoolingDebug();

	/**
	 * constructor
	 */
	public MessageContext(){
	}
	
	/**
	 * @param sipMessage the sipMessage to set
	 */
	public void setSipMessage(Message sipMessage) {			
		// this is always called first thing after allocating from the pool
		m_recycled = null;
		this.sipMessage = sipMessage;
	}

	/**
	 * @return the sipConnection
	 */
	public SIPConnection getSipConnection() {
		assertNotRecycled();
		return sipConnection;
	}

	/**
	 * @param sipConnection
	 *            the sipConnection to set
	 */
	public void setSipConnection(SIPConnection sipConnection) {
		assertNotRecycled();
		this.sipConnection = sipConnection;
	}

	/**
	 * @return the sipMessage
	 */
	public Message getSipMessage() {
		assertNotRecycled();
		return sipMessage;
	}

	/**
	 * @return the sipMessageByteBuffer
	 */
	public SipMessageByteBuffer getSipMessageByteBuffer() {
		assertNotRecycled();
		return sipMessageByteBuffer;
	}

	/**
	 * @param sipMessageByteBuffer
	 *            the sipMessageByteBuffer to set
	 */
	public void setSipMessageByteBuffer(
			SipMessageByteBuffer sipMessageByteBuffer) {
		assertNotRecycled();
		this.sipMessageByteBuffer = sipMessageByteBuffer;
	}

	/**
	 * @return the naptrCalled
	 */
	public boolean isNaptrCalled() {
		assertNotRecycled();
		return naptrCalled;
	}

	/**
	 * @param naptrCalled the naptrCalled to set
	 */
	public void setNaptrCalled(boolean naptrCalled) {
		assertNotRecycled();
		this.naptrCalled = naptrCalled;
	}

	
	/**
	 * @return the sipTransaction
	 */
	public SIPTransaction getSipTransaction() {
		assertNotRecycled();
		return sipTransaction;
	}

	/**
	 * @param sipTransaction the sipTransaction to set
	 */
	public void setSipTransaction(SIPTransaction sipTransaction) {
		assertNotRecycled();
		this.sipTransaction = sipTransaction;
	}

	
	/**
	 * @return the sender
	 */
	public IBackupMessageSender getSender() {
		assertNotRecycled();
		return sender;
	}

	/**
	 * @param sender the sender to set
	 */
	public void setSender(IBackupMessageSender sender) {
		assertNotRecycled();
		this.sender = sender;
	}

	/**
	 * @return the wsByteBuffer
	 */
	public WsByteBuffer getWsByteBuffer() {
		assertNotRecycled();
		return wsByteBuffer;
	}

	/**
	 * @param wsByteBuffer the wsByteBuffer to set
	 */
	public void setWsByteBuffer(WsByteBuffer wsByteBuffer) {
		assertNotRecycled();
		this.wsByteBuffer = wsByteBuffer;
	}

	/**
	 * called when switching transport from UDP to TCP in case the request
	 * is too large for UDP
	 */
	public void transportSwitch() {
		m_mtuSwitch = true;
	}

	/**
	 * determines if this message has already switched transport from UDP
	 * to TCP. this is necessary to prevent switching twice to TCP.
	 * @return true only if switched in the past to TCP
	 */
	public boolean transportSwitched() {
		return m_mtuSwitch;
	}

	
	@Override
	/*
	 * (non-Javadoc)
	 * @see com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback#complete(com.ibm.wsspi.channelfw.VirtualConnection, com.ibm.wsspi.tcpchannel.TCPWriteRequestContext)
	 */
	public void complete(VirtualConnection vc, TCPWriteRequestContext wsc) {
		writeComplete();
		
	}

	/**
	 * @see com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback#error(com.ibm.wsspi.channelfw.VirtualConnection, com.ibm.wsspi.tcpchannel.TCPWriteRequestContext, java.io.IOException)
	 */
	public void error(VirtualConnection vc, TCPWriteRequestContext wsc, IOException ioe) {
		assertNotRecycled();
		// this method is called back after adding the outbound message context
		// to the connection's queue. this results in the next line calling
		// writeError() on all pending messages, including this instance.
		sipConnection.connectionError(ioe);
		
	}

	/**
	 * a write error occurred.
	 * the connection is notified, and the transport is notified, so it may resend it to alternative destination
	 * 
	 * @param e the {@link Exception} that occurred.
	 */
	public void writeError(Exception e) {
		assertNotRecycled();
		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug(this,"writeError","an error occurred while " +
					"writing message to destination: " + sipConnection + 
					". message: "+ sipMessage, e);
		}
		TransportCommLayerMgr transportLayer = TransportCommLayerMgr.instance();
		if (m_mtuSwitch) {
			transportLayer.handleMtuError(this, sipMessage, sipConnection);
		}
		else {
			transportLayer.onMessageSendingFailed(this);
		}
	}

	/**
	 * a write action completed successfully
	 */
	public void writeComplete(){
		assertNotRecycled();
		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug("MessageContext - write is complete to destination: " + 
				sipConnection.getRemoteHost() + ":" + sipConnection.getRemotePort());
		}
		sipConnection.writeComplete(this);
		doneWithContext(this);
	}

	
	/**
	 * an interface to handle faliures when they occur while sending messages 
	 *
	 */
	public abstract void handleFailure();

	/**
	 * an interface to notify when this context is done with 
	 *
	 */
	protected abstract void doneWithContext();
	
	
	/**
	 * an method to notify the message context that it finished its way. 
	 */
	public static void doneWithContext(MessageContext messageContext){
		if (messageContext == null){
			return;
		}
		messageContext.doneWithContext();

		if (s_messageContextPoolingDebug) {
			// in debug mode, bookmark the recycling source
			messageContext.assertNotRecycled();
			messageContext.m_recycled = new RuntimeException("recycled ["
				+ System.identityHashCode(messageContext) + ']');
		}
		MessageContextFactory.instance().finishToUseContext(messageContext);
	}

	/**
	 * called by any method that tries to access the state of this instance,
	 * to verify that the calling method is not accessing a dangling reference
	 * to a recycled object.
	 * @throws IllegalStateException in case of attempt to access a recycled
	 *  instance. it contains information about the last method that recycled
	 *  the object.
	 */
	private void assertNotRecycled() {
		if (m_recycled != null) {
			// in debug mode, someone is trying to access this recycled instance
			IllegalStateException e = new IllegalStateException(
				"recycled object access fault ["
					+ System.identityHashCode(this) + ']');
			s_logger.traceDebug(this, "assertNotRecycled",
				"recycled object access fault", e);
			
			if(s_messageContextPoolingDebug) {
				s_logger.traceDebug(this, "assertNotRecycled",
						"recycled object access fault was caused due to the following stack call", m_recycled);
			}
			
			throw e;
		}
	}

	/**
	 * clean all attributes
	 *
	 */
	public void cleanItself() {
		sipConnection = null;
		sipMessage = null;
		sipMessageByteBuffer = null;
		naptrCalled = false;
		sipTransaction = null;
		sender = null;
		wsByteBuffer = null;
		m_mtuSwitch = false;
		_sipContainerQueueDuration = null;
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString(){
		StringBuffer buf = new StringBuffer();
		buf.append("MessageContext: ");
		buf.append("class [").append(getClass().getName());
		buf.append("] connection [").append(sipConnection);
		buf.append("] transaction [").append(sipTransaction).append(']');
		return buf.toString();
	}
	
	public StackTaskDurationMeasurer getSipContainerQueueDuration() {
		return _sipContainerQueueDuration;
	}
	
	public void setStackTaskDurationMeasurer(StackTaskDurationMeasurer tm) {
		_sipContainerQueueDuration = tm;
	}
	
}