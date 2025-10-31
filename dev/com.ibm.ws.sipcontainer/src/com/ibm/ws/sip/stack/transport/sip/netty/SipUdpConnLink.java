/*******************************************************************************
 * Copyright (c) 2008, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.sip.stack.transport.sip.netty;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sip.container.pmi.PerformanceMgr;
import com.ibm.ws.sip.parser.util.InetAddressCache;
import com.ibm.ws.sip.properties.SipPropertiesMap;
import com.ibm.ws.sip.properties.StackProperties;
import com.ibm.ws.sip.stack.context.MessageContext;
import com.ibm.ws.sip.stack.dispatch.Dispatcher;
import com.ibm.ws.sip.stack.transaction.transport.UseCompactHeaders;
import com.ibm.ws.sip.stack.transaction.transport.connections.SipMessageByteBuffer;
import com.ibm.ws.sip.stack.transaction.util.ApplicationProperties;
import com.ibm.ws.sip.stack.util.StackTaskDurationMeasurer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.socket.DatagramPacket;
import jain.protocol.ip.sip.ListeningPoint;

/**
 * singleton class for either inbound or outbound udp messages
 * 
 * TODO this class and SipConnLink have a lot in common, and should have a common base class.
 * 
 */
public class SipUdpConnLink implements UdpSender, ChannelFutureListener {
	/** class logger */
	private static final TraceComponent tc = Tr.register(SipUdpConnLink.class);
	private static final LogMgr c_logger = Log.get(SipUdpConnLink.class);

	/**
	 * map of inbound udp conn links. there is one udp conn link per host:port.
	 */
	private static HashMap s_instances = new HashMap();

	/** instance that is currently trying to create a virtual connection */
	private static SipUdpConnLink s_connectingInstance = null;

	/** channel that created this connection */
	private SipUdpInboundChannel m_channel;

	/** thread sending outbound messages */
	private SendThread m_sendThread;

	/** the buffer that is currently being sent out */
	private ByteBuf m_outboundBuffer;

	/** outbound - receive buffer size */
	private String m_receiveBufferSizeSocket;

	/** outbound - send buffer size */
	private String m_sendBufferSizeSocket;

	/** outbound - receive buffer size of the channel */
	private String m_receiveBufferSizeChannel;

	/**
	 * zOS specific: indicates that our message router (SLSPRouter class) has not
	 * yet been initialized with the endpoint information about the sip-router
	 * running in the control region
	 */
	private boolean m_needToLearnRouterEndpoint;

	private boolean closeAlreadyCalled = false;

	/** nested class that sends outbound messages on a separate thread */
	private static class SendThread extends Thread {
		/** back reference to the conn link */
		private final SipUdpConnLink m_connLink;

		/** queue of datagrams waiting to be sent out to the network */
		private final LinkedList<MessageContext> m_outMessages;

		/** locked as long as there is a pending send waiting for completion */
		private volatile boolean m_locked;

		/** true until the connection gets closed */
		private volatile boolean m_running;

		/** constructor */
		SendThread(SipUdpConnLink connLink) {
			super("SipUdpConnLink.SendThread");
			m_connLink = connLink;
			m_outMessages = new LinkedList<MessageContext>();
			m_locked = false;
			m_running = true;
		}

		/** adds another message to the queue */
		void queue(MessageContext messageContext) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc, "SipUdpConnLink$SendThread.queue: " + System.identityHashCode(messageContext));
			}
			synchronized (m_outMessages) {
				if (PerformanceMgr.getInstance().isTaskDurationOutboundQueuePMIEnabled() && messageContext != null) {
					// Start measuring time duration in stack time
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
						Tr.debug(this, tc, "SipUdpConnLink$SendThread.queue", "start measuring task duration");
					}
					messageContext.setStackTaskDurationMeasurer(new StackTaskDurationMeasurer());
					messageContext.getSipContainerQueueDuration().startMeasuring();
				}

				if (messageContext != null) {
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
						Tr.debug(this, tc, "SipUdpConnLink$SendThread.queue",
								"update QueueMonitoring outbound queue statistics - task queued");
					}
					PerformanceMgr.getInstance().updateQueueMonitoringTaskQueuedInOutboundQueue();
				}

				m_outMessages.addLast(messageContext);
				m_outMessages.notify();
			}
		}

		/** unlocks the send */
		void sendComplete() {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc, "SipUdpConnLink$SendThread.sendComplete");
			}
			synchronized (this) {
				// recycle the outbound buffer
				m_connLink.releaseOutboundBuffer();

				// unlock the send to allow another send
				m_locked = false;
				notify();
			}
		}

		/** terminates the thread */
		void terminate() {
			synchronized (m_outMessages) {
				m_running = false;
				m_outMessages.notify();
			}
		}

		/** @see java.lang.Runnable#run() */
		public void run() {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc, "SipUdpConnLink thread started");
			}
			try {
				while (m_running) {
					// wait here until there's something to send
					MessageContext messageContext = null;
					synchronized (m_outMessages) {
						if (m_outMessages.isEmpty()) {
							m_outMessages.wait();
						}
						if (!m_running) {
							break;
						}

						try {
							messageContext = (MessageContext) m_outMessages.removeFirst();

							if (messageContext != null) {
								if (PerformanceMgr.getInstance().isTaskDurationOutboundQueuePMIEnabled()) {
									// measure time duration in sip stack queue
									PerformanceMgr.getInstance().measureTaskDurationOutboundQueue(
											messageContext.getSipContainerQueueDuration().takeTimeMeasurement());

									if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
										Tr.debug(this, tc, "run",
												"update QueueMonitoring outbound queue statistics - task dequeued");
									}
									PerformanceMgr.getInstance().updateQueueMonitoringTaskDequeuedFromOutboundQueue();
								}
							}

						} catch (NoSuchElementException e) {
							// according to the Object wait() API: "spurious wakeups are possible"
							// we saw this issue with SUN JVM, so if we get here and list is empty we just
							// need
							// to wait again
							if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
								Tr.debug(this, tc, "run", "Trying to remove message from empty list");
							}
							continue;
						}
					}
					// there's something to send.
					// wait here until it's safe to send
					synchronized (this) {
						if (m_locked) {
							wait();
						}
					}
					if (!m_running) {
						break;
					}
					// there's something to send, and it's safe to send it
					m_locked = true;
					boolean immediate;
					try {
						immediate = m_connLink.sendNow(messageContext);
						messageContext.writeComplete(); // the message context is no longer referenced
					} catch (IOException e) {
						if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
							Tr.debug(this, tc, "run", "IOException", e);
						}
						messageContext.writeError(e);
						immediate = true;
					}
					if (immediate) {
						// completed immediately
						sendComplete();
					} else {
						// wait for completion
					}
				}
			} catch (InterruptedException e) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(this, tc, "run", "InterruptedException", e);
				}
			}
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc, "SipUdpConnLink thread terminated");
			}
		}
	}

	/**
	 * @param channel inbound channel that is creating or accessing this conn link
	 * @return conn link instance given the listening point
	 */
	static SipUdpConnLink instance(SipUdpInboundChannel channel) {
		ListeningPoint lp = channel.getListeningPoint();
		SipUdpConnLink connLink = (SipUdpConnLink) s_instances.get(lp);
		if (connLink == null) {
			connLink = new SipUdpConnLink(channel);
			s_instances.put(lp, connLink);
		}
		return connLink;
	}

	/**
	 * private constructor
	 * 
	 * @param channel channel that is creating this connection
	 */
	private SipUdpConnLink(SipUdpInboundChannel channel) {
		m_channel = channel;
		m_sendThread = new SendThread(this);
		m_outboundBuffer = null;
		m_sendThread.start();

		// read configuration settings for outbound UDP
		SipPropertiesMap config = ApplicationProperties.getProperties();
		m_receiveBufferSizeSocket = config.getString(StackProperties.RECEIVE_BUFFER_SIZE_SOCKET);
		if (m_receiveBufferSizeSocket.equals("") || m_receiveBufferSizeSocket.length() == 0) {
			m_receiveBufferSizeSocket = null;
		}
		m_sendBufferSizeSocket = config.getString(StackProperties.SEND_BUFFER_SIZE_SOCKET);
		if (!m_sendBufferSizeSocket.equals("") || m_sendBufferSizeSocket.length() == 0) {
			m_sendBufferSizeSocket = null;
		}
		m_receiveBufferSizeChannel = config.getString(StackProperties.RECEIVE_BUFFER_SIZE_CHANNEL);
		if (m_receiveBufferSizeChannel.equals("") || m_receiveBufferSizeChannel.length() == 0) {
			m_receiveBufferSizeChannel = null;
		}
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this, tc, "<init>",
					"receiveBufferSizeSocket [" + m_receiveBufferSizeSocket + "] sendBufferSizeSocket ["
							+ m_sendBufferSizeSocket + "] receiveBufferSizeChannel [" + m_receiveBufferSizeChannel
							+ ']');
		}
	}

	/**
	 * called by the channel while the channel factory instantiates the new
	 * OutboundVirtualConnection object
	 * 
	 * @return the conn link instance associated with this connection
	 */
	static SipUdpConnLink getPendingConnection() {
		SipUdpConnLink current = s_connectingInstance;
		s_connectingInstance = null;
		return current;
	}

    /**
     * No Op -- Should already be connected and active. 
     */
    private void connect(MessageContext messageContext) throws IOException {
        String outboundChainName = m_channel.getOutboundChainName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Boolean isActive =  ((SipUdpConnection)messageContext.getSipConnection()).getChannel().isActive();
            Tr.debug(this, tc, "<connect>", "outboundChainName = " + outboundChainName + " isActive = " + isActive);
        }
    }


	/**
	 * send queued outbound message
	 * 
	 * @param message datagram to be sent
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.channelframework.UdpSender#send(com.ibm.ws.sip.stack.transaction.transport.connections.SipMessageByteBuffer)
	 */
	public void send(MessageContext messageContext, UseCompactHeaders useCompactHeaders) throws IOException {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this, tc, "SipUdpConnLink.send: " + System.identityHashCode(messageContext));
		}
		m_sendThread.queue(messageContext);
	}

	/**
	 * send out one message now. always called from the send thread.
	 * 
	 * @param message datagram to be sent
	 * @return true if completed immediately
	 */
	protected boolean sendNow(MessageContext messageContext) throws IOException {
		SipMessageByteBuffer message = messageContext.getSipMessageByteBuffer();
		messageContext.setSipMessageByteBuffer(null);
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this, tc, "SipUdpConnLink.sendNow: " + System.identityHashCode(message));
		}
		if (message == null) {
			throw new IOException("message is null in SipUdpConnLink.sendNow");
		}
		if (m_outboundBuffer != null) {
			throw new IOException("previous send not completed in SipUdpConnLink.sendNow");
		}

		SipUdpConnection sipConnection = (SipUdpConnection) messageContext.getSipConnection();
		String remoteHost = sipConnection.getRemoteHost();
		int remotePort = sipConnection.getRemotePort();
		InetSocketAddress address = InetAddressCache.getInetSocketAddress(remoteHost, remotePort);

		m_outboundBuffer = BaseConnection.stackBufferToByteBuf(message);
		
		DatagramPacket pkt = new DatagramPacket(m_outboundBuffer, address);
		final ChannelFuture writeFuture = sipConnection.getChannel().writeAndFlush(pkt);
		boolean complete = writeFuture.isDone();
		
		writeFuture.addListener(this);
		
		return complete;
	}

	// ------------------------------
	// SIPReadCallback implementation
	// ------------------------------

	/**
	 * called by the channel framework when a new message arrives
	 */
	public void complete(SipMessageByteBuffer buffer, InetSocketAddress senderAddr) {
		boolean drop = Dispatcher.instance().isOverLoaded();
		if (drop) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc, "complete", "Warning: dropping request under overloaded situation");
			}
		}
		if (senderAddr == null) {
			if (c_logger.isInfoEnabled()) {
				c_logger.info("complete", null, "A message in order to use UDP connection link");
			}
		} else  {
			BaseConnection bc = new SipUdpConnection(m_channel, this, m_channel.getChannel());
			bc.setRemoteAddress(senderAddr);
			bc.messageReceived(buffer);
		} 
		
	}

	public void error(Throwable e) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this, tc, "SipUdpConnLink(read).error", e);
		}
		close(e);
	}

	// -------------------------------
	// SIPWriteCallback implementation
	// -------------------------------

	/**
	 * called by the channel framework when previous send completed
	 * 
	 */
	public void complete() {
		m_sendThread.sendComplete();
	}

	/**
	 * called when send completed to release the outbound buffer, either from the
	 * send thread (immediate) or from the channel thread (asynchronous)
	 */
	void releaseOutboundBuffer() {
		// No need to release the ByteBuf as the reference count should be 0 by this point. 
		// Reference count should have been handled by the writeandflush method
		m_outboundBuffer = null;
	}

	public void close(Throwable e) {
		// avoid double close
		if(closeAlreadyCalled){ 
			return;
		}
		closeAlreadyCalled = true;

		// remove this conn link from the global table
		s_instances.remove(m_channel.getListeningPoint());

		// tell the channel that we're closed. this will cause the next message
		// to create a new instance of this class.
		m_channel.connectionClosed();

		// close the underlying connection and stop the thread

		m_sendThread.terminate();
	}
	
	public void close() {
		this.close(null);
	}

	@Override
	public void operationComplete(ChannelFuture future) throws Exception {
        if (future.isSuccess()) {
        	complete();
        } else {
            Throwable t = future.cause();
            // see com.ibm.ws.udpchannel.internal.WorkQueueManager.doPhysicalWrite(UDPWriteRequestContextImpl)
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Caught exception " + t.toString() + " while sending data.  Packet is lost.");
            }
            FFDCFilter.processException(t, getClass().getName(), "1", this);
            complete();
            
        }
	}

}
