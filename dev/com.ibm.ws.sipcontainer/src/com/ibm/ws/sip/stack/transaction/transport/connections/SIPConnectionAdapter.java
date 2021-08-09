/*******************************************************************************
 * Copyright (c) 2003,2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.transaction.transport.connections;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.message.MessageImpl;
import com.ibm.ws.jain.protocol.ip.sip.message.MessageImpl.HeaderForm;
import com.ibm.ws.sip.container.timer.TimerServiceImpl;
import com.ibm.ws.sip.properties.StackProperties;
import com.ibm.ws.sip.stack.context.MessageContext;
import com.ibm.ws.sip.stack.dispatch.Dispatcher;
import com.ibm.ws.sip.stack.transaction.transport.Hop;
import com.ibm.ws.sip.stack.transaction.transport.PathMtuExceeded;
import com.ibm.ws.sip.stack.transaction.transport.UseCompactHeaders;
import com.ibm.ws.sip.stack.transaction.util.ApplicationProperties;
import com.ibm.ws.sip.stack.transaction.util.SIPStackUtil;

/**
 * @author amirk
 * base class for a SIPConnection
 */
public abstract class SIPConnectionAdapter
	implements  SIPConnection 
{
	/** true if connection with peer is established */
		protected enum ConnectionStatus{
			PRE_CONNECT,
			CONNECTED,
			CLOSED
		}

	/**
	 * Class Logger. 
	 */
	static final LogMgr c_logger =
		Log.get(SIPConnectionAdapter.class);
	
	/**
	 * A constant indicates to never close a connection on a parse error.
	 */
	private static final int NEVER_CLOSE_CONNECTION_ON_A_PARSE_ERROR = -1;
	
	/** 
	 * aliace port for this connection 
	 **/
	private int m_aliacePort;
	
	/** 
	 * the peer host 
	 **/
	private String m_peerHost;
	
	/** 
	 * peer port 
	 **/
	private int m_peerPort;
	
	/** 
	 * opaque parser object 
	 **/
	private Object m_parser;
	
	/**
	 * the connection key
	 */
	private Hop m_key;

	/**
	 * The connection status
	 */
	protected ConnectionStatus m_connectionStatus = ConnectionStatus.PRE_CONNECT; 

	/**
	 * true if this connection is outbound, false if inbound.
	 * the default is false. any code that creates an outbound connection
	 * is responsible to set this to true explicitly.
	 */
	private boolean m_outbound;
	
	/**
	 * A counter for parse errors.
	 */
	private int m_numberOfParseErrors;
	
	/**
	 * The maximum parse errors allowed.
	 */
	private int m_maxParseErrorsAllowed;
	
	/**
	 * The interval for counting the parse errors.
	 */
	private int m_timerParseErrorsInterval;
	
    /**
     * The container timer service
     */
	private ScheduledExecutorService m_scheduledExecutorService;

    /**
     * Represents a scheduled {@link ScheduledFuture} for {@link TimerParseErrorsListener}.
     */
    private ScheduledFuture<?> m_parseErrorsTimer;
    
    /**
     * A locking object in order to access to m_numberOfParseErrors.
     */
    private Object m_parseErrorsSynchronizer = new Object() {};

	/**
	 * constructor
	 */
	public SIPConnectionAdapter(String peerHost, int peerPort) {
		m_peerHost = peerHost;
		m_peerPort = peerPort;
		m_aliacePort = -1;
		m_parser = null;
		m_key = null;
		m_outbound = false;
		m_numberOfParseErrors = 0;
		m_timerParseErrorsInterval = ApplicationProperties.getProperties().
				getInt(StackProperties.TIMER_PARSE_ERRORS_INTERVAL);
		m_maxParseErrorsAllowed = ApplicationProperties.getProperties().
				getInt(StackProperties.NUMBER_OF_PARSE_ERRORS_ALLOWED);
	}
	
	/** set aliace port */
	public void setAliacePort( int port )
	{
		m_aliacePort = port;
	}
	
	/** return the aliace port */
	public int getAliacePort()
	{
		return m_aliacePort;
	}
	
	public boolean hasAliacePort()
	{
		return m_aliacePort != -1;
	}
		
	/** set the peer port */
	public void setRemotePort( int peerPort )
	{
		m_peerPort = peerPort;
	}
	
	/** get the peer port */
	public int getRemotePort()
	{
		return m_peerPort;
	}
	
	/** set the peer host */
	public void setRemoteHost( String peerHost )
	{
		//try to translate the host name to IP representation
		//if not , host equeles the given host
		try 
		{
		    //Moti :  using SipStackUtils (saves allocations)
		    m_peerHost = SIPStackUtil.getHostAddress(peerHost);
		} 
		catch ( Throwable t ) 
		{
			m_peerHost = peerHost;
		}
	}
	
	/** get the peer host */
	public String getRemoteHost()
	{
		return m_peerHost;
	}

	public Hop getKey()
	{
		return m_key;
	}
	
	
	public void setKey(Hop key)
	{
		m_key = key;
	}
	
	
	public String toString() {
		Hop key = getKey();
		return key == null ? "?" : key.toString();
	}
	
	
	public Object getParsingObject(  )
	{
		return m_parser;
	}


	public void setParsingObject( Object parser )
	{
		m_parser = parser;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#connectionError(java.lang.Exception)
	 */
	public void connectionError(Exception e) {
		if (isClosed()) {
			// the first call to this event called close(),
			// which triggered this event a second time.
			// don't print the exception again.
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this,"connection Error","connection Error on closed connection");
			}
			return;
		}
		if( c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug(this, "connectionError", "error", e);
		}
		Dispatcher.instance().queueConnectionClosedEvent(this);
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#writeComplete()
	 */
	public void writeComplete(MessageContext messageContext) {
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#readComplete()
	 */
	public void readComplete() {
	}
	
	/**
	 * this will be called when a connection is destroyed
	 * and pending messages must be cleaned
	 *  
	 * @param e - the exception that was thrown
	 */
	protected void cleanPendingMessages(List<MessageContext> pendingMessages, Exception e) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "cleanPendingMessages",
				"entry");
		}
		if (pendingMessages == null){
			return;
		}
		for (Iterator<MessageContext> itr = pendingMessages.iterator(); itr.hasNext();){
			MessageContext messageSendingContext = itr.next();
			itr.remove();
			messageSendingContext.writeError(e);
		}
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "cleanPendingMessages",
				"exit");
		}
	}

	/**
	 * Prepares the outbound buffer before sending it out to the network.
	 * Handles MTU boundary and header compacting as needed.
	 * 
	 * @param messageContext the outbound message context
	 * @param considerMtu true if the MTU boundary should be considered
	 * @param useCompactHeaders compact header configuration policy
	 * @throws IOException this is PathMtuExceeded exception in case of a large
	 *  message on UDP, or other IOException under severe circumstances.
	 */
	protected void prepareBuffer(
		MessageContext messageContext,
		boolean considerMtu, UseCompactHeaders useCompactHeaders)
		throws IOException
	{
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "prepareBuffer",
				"entry. considerMtu [" + considerMtu
				+ "] useCompactHeaders [" + useCompactHeaders + ']');
		}

		// 1. serialize message, and apply header compacting if needed
		HeaderForm headerForm;
		switch (useCompactHeaders) {
		case NEVER:
			// header compacting is forcibly disabled
			headerForm = HeaderForm.LONG;
			break;
		case ALWAYS:
			// header compacting is forcibly enabled
			headerForm = HeaderForm.COMPACT;
			break;
		case MTU_EXCEEDS:
		case API:
		default:
			// header compacting is controlled by application
			headerForm = HeaderForm.DEFAULT;
			break;
		}
		MessageImpl message = (MessageImpl)messageContext.getSipMessage();
		SipMessageByteBuffer sipMessageByteBuffer;
		try {
			sipMessageByteBuffer = SipMessageByteBuffer.fromMessage(message, headerForm);
		}
		catch (Exception e) {
			messageContext.writeError(e);
			throw (e instanceof IOException)
				? (IOException)e
				: new IOException(e.getMessage());
		}
		messageContext.setSipMessageByteBuffer(sipMessageByteBuffer);

		// 2. handle MTU
		if (considerMtu && !isReliable()) {
			//check if we exceed the MTU limit
			//if we do, we have 2 options
			if (mtuExceeds(sipMessageByteBuffer)){
				//if this message's headers are already compact,there's nothing we can do
				if (headerForm == HeaderForm.LONG ||
					(headerForm == HeaderForm.DEFAULT && message.isCompact()))
				{
					// message is already as compact as can be
					PathMtuExceeded.throwIt();
				}else{
					//we try to compact the Headers, if they should be compact only  when MTU Exceeds
					if (useCompactHeaders == UseCompactHeaders.MTU_EXCEEDS){
						//recreate the buffer in compact form
						headerForm = HeaderForm.COMPACT;
						try {
							sipMessageByteBuffer = SipMessageByteBuffer.fromMessage(
								message, headerForm);
						}
						catch (Exception e) {
							messageContext.writeError(e);
							throw (e instanceof IOException)
								? (IOException)e
								: new IOException(e.getMessage());
						}
						//set the new buffer in the context
						messageContext.setSipMessageByteBuffer(sipMessageByteBuffer);
						//recheck the buffer for MTU
						if (mtuExceeds(sipMessageByteBuffer)){
							// still too large
							PathMtuExceeded.throwIt();
						}
					}
				}
			}
		}
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "prepareBuffer",
				"exit. headerForm [" + headerForm
				+ "] message size ["
				+ sipMessageByteBuffer.getMarkedBytesNumber() + ']');
		}
	}

	/**
	 * this method checks if a certain buffer exceeds the mtu limit
	 *  
	 * @param sipMessageByteBuffer
	 * @return
	 */
	protected boolean mtuExceeds(SipMessageByteBuffer sipMessageByteBuffer) {
		int size = sipMessageByteBuffer.getMarkedBytesNumber();
		int limit = getPathMTU() - 200;
		if (size > limit) {
			return true;
		}
		return false;
	}

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#isClosed()
	 */
	public boolean isClosed() {
		return m_connectionStatus == ConnectionStatus.CLOSED;
	}

	public boolean isConnected() {
		return m_connectionStatus == ConnectionStatus.CONNECTED;
	}

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#isOutbound()
	 */
	public boolean isOutbound() {
		return m_outbound;
	}

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#setOutbound(boolean)
	 */
	public void setOutbound(boolean outbound) {
		m_outbound = outbound;
	}
	
	/**
	 * called whenever connection state changes to log the new state
	 * @param connection the modified connection
	 * @param remove true if removed, false if added or updated
	 */
	protected void logConnection() {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "logConnection",
				"connection [" + System.identityHashCode(this)	
				+ "] connection status [" + m_connectionStatus
				+ "] key [" + getKey()
				+ "] remote [" + getRemoteHost()
				+ ':' + getRemotePort() + ']');
		}
	}
	
	/**
	 * Prints the parse errors counter per the connection.
	 * @param method the calling method
	 */
	private void logParseErrorCounter(String method) {
		c_logger.traceDebug(this, method,
			"connection [" + System.identityHashCode(this)	
			+ "] key [" + getKey()
			+ "] parse errors counter = " + m_numberOfParseErrors);
	}
	
	/**
	 * called by the derived class when connection established, either inbound
	 * or outbound
	 */
	protected void connectionEstablished() {
		m_connectionStatus = ConnectionStatus.CONNECTED;
		initNumberOfParseErrors();
		
		if (m_maxParseErrorsAllowed > 0) {
			// Only if the maximum parse error allowed is above 0, 
			// it might use a timer interval.
			
			if (m_timerParseErrorsInterval > 0) {
				// Create and run a timer for count parse errors in an interval.
				m_scheduledExecutorService = TimerServiceImpl.getTimerSerivce();
				m_parseErrorsTimer = m_scheduledExecutorService.scheduleAtFixedRate(new TimerParseErrorsListener(), 
						m_timerParseErrorsInterval, m_timerParseErrorsInterval, TimeUnit.MILLISECONDS);				
				
			} else {
				// The interval is infinite i.e. counting the number of parse errors is global 
				// and it does not depend on a defined interval.
				// Only need to inform the parse error counter.
				if (c_logger.isTraceDebugEnabled()) {
					synchronized (m_parseErrorsSynchronizer) {
						logParseErrorCounter("connectionEstablished, no interval for parse errors");
					}
				}
			}
		}
	}
	
	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#incrementNumberOfParseErrors()
	 */
	public void incrementNumberOfParseErrors() {
    	synchronized (m_parseErrorsSynchronizer) {
    		m_numberOfParseErrors++;
    		if (c_logger.isTraceDebugEnabled()) {
    			logParseErrorCounter("incrementNumberOfParseErrors");
    		}
    	}
	}
	
	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#shouldDropConnection()
	 */
	public boolean shouldDropConnection() {
		if (m_maxParseErrorsAllowed == NEVER_CLOSE_CONNECTION_ON_A_PARSE_ERROR) {
			return false;
			
		} else if (m_maxParseErrorsAllowed > NEVER_CLOSE_CONNECTION_ON_A_PARSE_ERROR) {
	    	synchronized (m_parseErrorsSynchronizer) {
	    		if (m_numberOfParseErrors > m_maxParseErrorsAllowed) {
	    			return true;
	    		}
	    	}
		}
		return false;
	}

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#isAParseErrorAllowed()
	 */
	public boolean isAParseErrorAllowed() {
		if (m_maxParseErrorsAllowed == -1 || 
				m_maxParseErrorsAllowed > 0) {
			return true;
		}
		return false;
	}
	
	/**
	 * When the timer is started, the counter for parse errors is initialized.
	 * @author Tamir Faibish
	 */
	private class TimerParseErrorsListener implements Runnable {
		
		@Override
		public void run() {
			initNumberOfParseErrors();
		}
	}
	
	/**
	 * Initializes the parse error counter.
	 */
	private void initNumberOfParseErrors() {
    	synchronized (m_parseErrorsSynchronizer) {
    		m_numberOfParseErrors = 0;
    		if (c_logger.isTraceDebugEnabled()) {
    			logParseErrorCounter("initializes parse errors counter");
    		}
    	}		
	}
	
	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#close()
	 */
	public void close() {
		cancelTimer(m_parseErrorsTimer, TimerParseErrorsListener.class.getName());
	}
	
    /**
     * Cancels a specific timer. 
     * 
     * @param timerToCancel the timer to cancel
     * @param timerClass    the name of the class that represents the timer (for trace)
     * 
     */
    private void cancelTimer(ScheduledFuture<?> timerToCancel, String timerClass) {
    	if (timerToCancel != null) {
    		if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "cancelTimer", "Canceling " + timerClass);
			}
			timerToCancel.cancel(true);				
		}
    }
}
