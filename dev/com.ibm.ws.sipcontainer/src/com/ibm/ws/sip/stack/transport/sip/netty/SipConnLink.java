/*******************************************************************************
 * Copyright (c) 2008, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.sip.stack.transport.sip.netty;

import java.io.IOException;
import java.util.*;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sip.container.pmi.PerformanceMgr;
import com.ibm.ws.sip.parser.MessageParser;
import com.ibm.ws.sip.parser.StreamMessageParser;
import com.ibm.ws.sip.stack.context.MessageContext;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;
import com.ibm.ws.sip.stack.transaction.transport.UseCompactHeaders;
import com.ibm.ws.sip.stack.transaction.transport.connections.SipMessageByteBuffer;
import com.ibm.ws.sip.stack.util.StackTaskDurationMeasurer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;

/**
 * base class for any sip stream connection that is managed by the channel
 * framework
 * 
 * @author ran
 */
public abstract class SipConnLink extends BaseConnection implements ChannelFutureListener {
	/** class logger */
	 /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(SipConnLink.class);

	/**
	 * queue of messages waiting to be sent out to the network. the first
	 * element in this list is currently being sent.
	 */
	final LinkedList<MessageContext> m_outMessages;

	/** true if the last sent message has not completed yet */
	private boolean m_sendPending;

	/** stateful message parser dedicated to this stream connection */
	private MessageParser m_messageParser;

	/** buffer size for reading inbound messages */
	private static final int READ_BUFFER_SIZE = 2048;

	/**
	 * exception provided by the error() callback, indicating the connection is
	 * broken, and at least one message is waiting for writeComplete() or
	 * writeError(). if writeError() is called, it takes care of cleanup. if
	 * writeComplete() is called, it calls connectionError() with this
	 * exception.
	 */
	private IOException m_readError;

	/** true if waiting for pending outbound messages to go before closing down */
	private boolean m_closing;

	/** true if an error occurred on this connection, that requires closing it down */
	private boolean m_broken;
	
	/** Netty channel associated with that SIP connection */
	protected Channel m_channel;

	/** the maximum number of messages waiting in the outbound queue */
	private static final int s_maxOutboundPendingMessages =
		SIPTransactionStack.instance().getConfiguration().getMaxOutboundPendingMessages();

	/**
	 * constructor for inbound connections, or for udp
	 * 
	 * @param sipInboundChannel
	 *            channel that created this connection
	 * @param channel
	 *            Netty channel associated with that SIP connection
	 */
	public SipConnLink(SipInboundChannel sipInboundChannel, Channel channel) {
		this(null, 0, sipInboundChannel, channel);
	}

	/**
	 * constructor for outbound connections
	 * 
	 * @param peerHost
	 *            remote host address in dotted form
	 * @param peerPort
	 *            remote port number
	 * @param channel
	 *            channel that created this connection
	 */
	public SipConnLink(String peerHost, int peerPort, SipInboundChannel sipInboundChannel, Channel channel) {
		super(peerHost, peerPort, sipInboundChannel);
		m_channel = channel;
		m_outMessages = new LinkedList<MessageContext>();
		m_sendPending = false;
		m_messageParser = new StreamMessageParser(this);
		m_readError = null;
		m_closing = false;
		m_broken = false;
	}

	/**
	 * called by the derived class when connection established, either inbound
	 * or outbound
	 */
	protected void connectionEstablished() {
		
	  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "connectionEstablished");
        }
		
		logConnection();
		// in case outbound messages queued while trying to connect, send
		// them now
		synchronized (m_outMessages) {
			super.connectionEstablished();
			sendPendingMessages();
		}

		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc,  "connectionEstablished", "exit");
        }
	}

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.channelframework.BaseConnection#write(com.ibm.ws.sip.stack.transaction.transport.connections.SipMessageByteBuffer,
	 *      boolean, UseCompactHeaders)
	 */
	public void write(MessageContext messageContext, boolean considerMtu, UseCompactHeaders useCompactHeaders) throws IOException {
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc,  "connectionEstablished", "entry [" + System.identityHashCode(messageContext) + ']');
        }
		
		logConnection();
		prepareBuffer( messageContext, considerMtu, useCompactHeaders);

		// send the message, or queue it if cannot send right now
		try {
			synchronized (m_outMessages) {
				if (PerformanceMgr.getInstance().isTaskDurationOutboundQueuePMIEnabled() && messageContext != null) {
					//start measuring message duration in stack queue. It will be either t==0 if sent immediately or t>0 if message is queued
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			            Tr.debug(tc,  "write", "start measuring task duration");
			        }
					messageContext.setStackTaskDurationMeasurer(new StackTaskDurationMeasurer());
					messageContext.getSipContainerQueueDuration()
							.startMeasuring();
				}
				
				if(messageContext !=null) {
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			            Tr.debug(tc,  "write", "update QueueMonitoring outbound queue statistics - task queued");
			        }
					
					PerformanceMgr.getInstance().updateQueueMonitoringTaskQueuedInOutboundQueue();
				}
			
				boolean empty = !m_sendPending && m_outMessages.isEmpty();
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
		            Tr.debug(tc,  "write", "m_sendPending = " + m_sendPending + ", m_outMessages.isEmpty() = " +  m_outMessages.isEmpty()
							+ ", isConnected() = " + isConnected() + ", isClosed() = " + isClosed()
							+ ", m_closing = " + m_closing);
		        }
				
				if (empty && isConnected() && !m_closing) {
					if (sendNow((MessageContext) messageContext)) {
						// completed immediately. call completion code manually.
						messageContext.writeComplete();
					}
				} else {
					if (isClosed()) {
						String exceptionMessage = "connection is closed: " + this + " could not send messsage: "
							+ messageContext.getSipMessage();
						throw new IOException(exceptionMessage);
					} else if (m_closing) {
						String exceptionMessage = "connection is closing: " + this + " could not send messsage: "
							+ messageContext.getSipMessage();
						throw new IOException(exceptionMessage);
					} else {
						// cannot send right now. either a previous write is pending,
						// or the outbound connection has not been established yet.
						// queue the message for later.
						if (s_maxOutboundPendingMessages > 0 &&
							m_outMessages.size() >= s_maxOutboundPendingMessages)
						{
							String exceptionMessage = "too many [" + m_outMessages.size()
								+ "] outbound messages pending on [" + this + ']';
							throw new IOException(exceptionMessage);
						}
						if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				            Tr.debug(tc,  "write", "adding messageContext = " + messageContext + "\n to m_outMessages");
				        }
						m_outMessages.addLast((MessageContext) messageContext);
					}
				}
			}
		}
		catch (IOException e) {
			connectionError(e);
		}
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc,  "write",  "exit [" + System.identityHashCode(messageContext) + ']');
        }
	}

	/**
	 * sends out one message
	 * 
	 * @param buffer
	 *            message to be sent
	 * @return true if completed immediately
	 */
	private boolean sendNow(MessageContext messageContext) throws IOException {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "sendNow", "entry [" + System.identityHashCode(messageContext) + ']');
        }

		if (PerformanceMgr.getInstance().isTaskDurationOutboundQueuePMIEnabled() && messageContext != null) {
			// Message is out of the queue - measure how much time it was queued
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	            Tr.debug(tc, "sendNow", "measure task duration");
	        }
			
			PerformanceMgr.getInstance().measureTaskDurationOutboundQueue(messageContext.getSipContainerQueueDuration().takeTimeMeasurement());
		}
		
		if(messageContext !=null) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc,  "sendNow", "update QueueMonitoring outbound queue statistics - task dequeued");
			}
			
			PerformanceMgr.getInstance().updateQueueMonitoringTaskDequeuedFromOutboundQueue();
		}
		
		SipMessageByteBuffer sipBuffer = messageContext.getSipMessageByteBuffer();
		messageContext.setSipMessageByteBuffer(null);

		ByteBuf buffer = stackBufferToByteBuf(sipBuffer);
		final ChannelFuture writeFuture = m_channel.writeAndFlush(buffer);	

		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc,  "sendNow", "exit [" + System.identityHashCode(messageContext) + ']');
		}
		
		boolean complete = writeFuture.isDone();
		
		if (!complete) {
			writeFuture.addListener(messageContext);
		}
		
		return complete;
	}

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#getMessageParser()
	 */
	public MessageParser getMessageParser() {
		return m_messageParser;
	}

	// -----------------------------
	// ConnectionLink implementation
	// -----------------------------


	/**
	 * @see com.ibm.wsspi.channelfw.ConnectionReadyCallback#destroy(java.lang.Exception)
	 */
	public void destroy(Exception e) {
		connectionError(e);
	}
	
	public void destroy() {
		connectionError(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnectionAdapter#writeComplete()
	 */
	public void writeComplete(MessageContext messageContext) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc,  "writeComplete", "entry [" + System.identityHashCode(messageContext) + ']');
		}

		// TODO should we release a ByteBuf created by Unpooled.buffer ?

		synchronized (m_outMessages) {
			m_sendPending = false;
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc,  "writeComplete", "m_sendPending = " + m_sendPending);
			}
			if (m_readError == null) {
				sendPendingMessages();
			} else {
				// the connection is closing down due to a previous read error.
				IOException readError = m_readError;
				m_readError = null;
				connectionError(readError);
			}
		}
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc,  "writeComplete", "exit");
		}
	}

	/**
	 * sends as many messages as possible from the outbound queue without
	 * blocking the calling thread
	 */
	private void sendPendingMessages() {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "sendPendingMessages");
		}
		
		logConnection();
		synchronized (m_outMessages) {
			try {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc,  "sendPendingMessages", "m_sendPending = " + m_sendPending + "m_outMessages.isEmpty() = " + m_outMessages.isEmpty());
				}
				while (!m_sendPending && !m_outMessages.isEmpty()) {
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
						Tr.debug(tc,  "sendPendingMessages", "sending pending messages");
					}
					MessageContext messageSendingContext = m_outMessages.removeFirst();
					if (!sendNow(messageSendingContext)) {
						if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
							Tr.debug(tc,  "sendPendingMessages", "send will complete later");
						}
						// send will complete later
						break;
					}
					
					// send completed immediately, so try to send more
					messageSendingContext.writeComplete();
				}
				if (m_closing && !m_sendPending && m_outMessages.isEmpty()) {
					// last message sent out - time to close.
					close();
				}
			} catch (IOException e) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc,  "sendPendingMessages", "IOException", e);
				}
				connectionError(e);
			}
		}
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "sendPendingMessages");
		}
	}

	/**
	 * called when new data arrives
	 * 
	 */
	public void complete(SipMessageByteBuffer buffer) {
		messageReceived(buffer);
	}


	@Override
	public void operationComplete(ChannelFuture arg0) throws Exception {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.wsspi.tcp.channel.TCPReadCompletedCallback#error(com.ibm.wsspi.channelfw.framework.VirtualConnection,
	 *      com.ibm.wsspi.tcp.channel.TCPReadRequestContext,
	 *      java.io.IOException)
	 */
	public void error(IOException e) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc,  "error", "error received from TCP");
		}
		boolean empty;
		synchronized (m_outMessages) {
			empty = m_outMessages.isEmpty();
		}
		if (empty) {
			// clean up
			connectionError(e);
		} else {
			// at least one message is waiting for write callback.
			// its future writeError() will determine the connection is
			// broken and handle the cleanup.
			// if, for any reason, it gets a writeComplete() instead
			// of a writeError(), then writeComplete() will take care
			// of cleaning up.
			m_readError = e;
		}
	}

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#close()
	 */
	public void close() {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc,  "close",
				"[" + this + "] closed [" + isClosed()
				+ "] closing [" + m_closing + "] broken [" + m_broken + ']');
		}
		logConnection();

		// don't close twice
		if (isClosed()) {
			return;
		}

		// don't close if there are outbound messages pending, unless there
		// was an error that prevents sending them out.
		if (!m_broken) {
			synchronized (m_outMessages) {
				if (m_sendPending || !m_outMessages.isEmpty()) {
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
						Tr.debug(tc,  "close",
							"Waiting for outbound messages pending on [" + this + ']');
					}
					// mark this connection as closing, so future write() attempts fail immediately
					m_closing = true;
					return;
				}
			}
		}

		// mark this connection as closed, so future write() attempts fail immediately
		super.close();

        // close down the TCP connection (channel)
        if (m_channel != null) {
            m_channel.close();
        }

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnectionAdapter#connectionError(java.lang.Exception)
	 */
	public void connectionError(Exception e) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "connectionError");
		}
		logConnection();
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc,  "connectionError", "error", e);
		}
		

		m_broken = true;

		// we avoid loop everything was already handled
		if (!isClosed()) {
			super.connectionError(e);
		}
		// move all elements from m_outMessages to a temporary list
		List<MessageContext> pendingMessageContextList;
		synchronized (m_outMessages) {
			pendingMessageContextList = new ArrayList<MessageContext>(m_outMessages);
			m_outMessages.clear();
		}
		// work on the temp list outside the sync block
		cleanPendingMessages(pendingMessageContextList, e);
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "connectionError");
		}
	}


}
