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
package com.ibm.ws.sip.stack.transport.sip;

import jain.protocol.ip.sip.ListeningPoint;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sip.container.pmi.PerformanceMgr;
import com.ibm.ws.sip.parser.util.InetAddressCache;
import com.ibm.ws.sip.properties.SipPropertiesMap;
import com.ibm.ws.sip.properties.StackProperties;
import com.ibm.ws.sip.stack.context.MessageContext;
import com.ibm.ws.sip.stack.dispatch.Dispatcher;
import com.ibm.ws.sip.stack.transaction.transport.Hop;
import com.ibm.ws.sip.stack.transaction.transport.UseCompactHeaders;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection;
import com.ibm.ws.sip.stack.transaction.transport.connections.SipMessageByteBuffer;
import com.ibm.ws.sip.stack.transaction.transport.routers.SLSPRouter;
import com.ibm.ws.sip.stack.transaction.util.ApplicationProperties;
import com.ibm.ws.sip.stack.util.StackTaskDurationMeasurer;
import com.ibm.wsspi.anno.info.Info;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.ConnectionReadyCallback;
import com.ibm.wsspi.channelfw.OutboundConnectionLink;
import com.ibm.wsspi.channelfw.OutboundProtocol;
import com.ibm.wsspi.channelfw.OutboundVirtualConnection;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.VirtualConnectionFactory;
import com.ibm.wsspi.channelfw.base.OutboundProtocolLink;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.udpchannel.UDPBuffer;
import com.ibm.wsspi.udpchannel.UDPConfigConstants;
import com.ibm.wsspi.udpchannel.UDPContext;
import com.ibm.wsspi.udpchannel.UDPReadCompletedCallback;
import com.ibm.wsspi.udpchannel.UDPReadRequestContext;
import com.ibm.wsspi.udpchannel.UDPRequestContext;
import com.ibm.wsspi.udpchannel.UDPRequestContextFactory;
import com.ibm.wsspi.udpchannel.UDPWriteCompletedCallback;
import com.ibm.wsspi.udpchannel.UDPWriteRequestContext;

//TODO Liberty import com.ibm.ws.management.AdminHelper;

/**
 * singleton class for either inbound or outbound udp messages
 * 
 * TODO this class and SipConnLink have a lot in common, and should have a common base class.
 * 
 * @author ran
 */
public class SipUdpConnLink extends OutboundProtocolLink implements OutboundProtocol,
	UDPReadCompletedCallback, UDPWriteCompletedCallback, UdpSender
{
	/** class logger */
	private static final TraceComponent tc = Tr.register(SipUdpConnLink.class);
	private static final LogMgr c_logger = Log.get(SipUdpConnLink.class);
	
	/**
	 * map of inbound udp conn links.
	 * there is one udp conn link per host:port.
	 */
	private static HashMap s_instances = new HashMap();

	/** instance that is currently trying to create a virtual connection */
	private static SipUdpConnLink s_connectingInstance = null;
	
	/** layer below this link */
	private ConnectionLink m_linkOnDeviceSide; //UDPConnLink m_linkOnDeviceSide;
	
	/** layer above this link */
	private ConnectionReadyCallback m_linkOnApplicationSide;
	
	/** virtual connection associated with this connection link */
	private VirtualConnection m_vc;
	
	/** switched to true in first send or receive */
	private boolean m_connected;
	
	/** channel that created this connection */
	private SipUdpInboundChannel m_channel;

	/** thread sending outbound messages */
	private SendThread m_sendThread;
	
	/** the buffer that is currently being sent out */
	private WsByteBuffer m_outboundBuffer;

	/** outbound - receive buffer size */
	private String m_receiveBufferSizeSocket;

	/** outbound - send buffer size */
	private String m_sendBufferSizeSocket;

	/** outbound - receive buffer size of the channel */
	private String m_receiveBufferSizeChannel;
	
	/**
	 * zOS specific: indicates that our message router (SLSPRouter class)
	 * has not yet been initialized with the endpoint information about
	 * the sip-router running in the control region
	 */ 
	private boolean m_needToLearnRouterEndpoint;
	
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
				Tr.debug(this, tc,"SipUdpConnLink$SendThread.queue: " + System.identityHashCode(messageContext));
			}
			synchronized (m_outMessages) {
				if (PerformanceMgr.getInstance().isTaskDurationOutboundQueuePMIEnabled() && messageContext != null) {
					//Start measuring time duration in stack time
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
						Tr.debug(this, tc, "SipUdpConnLink$SendThread.queue", "start measuring task duration");
					}
					messageContext.setStackTaskDurationMeasurer(new StackTaskDurationMeasurer());
					messageContext.getSipContainerQueueDuration()
							.startMeasuring();
				}
				
				if(messageContext !=null) {
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
				Tr.debug(this, tc,"SipUdpConnLink$SendThread.sendComplete");
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
				Tr.debug(this, tc,"SipUdpConnLink thread started");
			}
			try {
				while (m_running) {
					// wait here until there's something to send
					MessageContext messageContext = null;
					synchronized (m_outMessages) {
						if (m_outMessages.isEmpty() || !m_connLink.m_connected) {
//							if (m_outMessages.isEmpty()) {
							m_outMessages.wait();
						}
						if (!m_running) {
							break;
						}
						
						try {
							messageContext = (MessageContext) m_outMessages.removeFirst();
							
							if(messageContext !=null) {
								if (PerformanceMgr.getInstance().isTaskDurationOutboundQueuePMIEnabled()) {									
									//measure time duration in sip stack queue
									PerformanceMgr.getInstance().measureTaskDurationOutboundQueue(
											messageContext.getSipContainerQueueDuration().takeTimeMeasurement());
							
									if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
										Tr.debug(this, tc,"run", "update QueueMonitoring outbound queue statistics - task dequeued");
									}
									PerformanceMgr.getInstance().updateQueueMonitoringTaskDequeuedFromOutboundQueue();
								}
							}
							
						} catch (NoSuchElementException e) {
							//according to the Object wait() API: "spurious wakeups are possible"
							//we saw this issue with SUN JVM, so if we get here and list is empty we just need 
							//to wait again
							if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
								Tr.debug(this, tc,"run", "Trying to remove message from empty list");
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
					}
					catch (IOException e) {
						if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
							Tr.debug(this, tc,"run", "IOException", e);
						}
						messageContext.writeError(e);
						immediate = true;
					}
					if (immediate) {
						// completed immediately
						sendComplete();
					}
					else {
						// wait for completion
					}
				}
			}
			catch (InterruptedException e) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(this, tc,"run", "InterruptedException", e);
				}
			}
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc,"SipUdpConnLink thread terminated");
			}
		}
	}
	
	/**
	 * @param channel inbound channel that is creating or accessing this conn link
	 * @return conn link instance given the listening point
	 */
	static SipUdpConnLink instance(SipUdpInboundChannel channel) {
		ListeningPoint lp = channel.getListeningPoint();
		SipUdpConnLink connLink = (SipUdpConnLink)s_instances.get(lp);
		if (connLink == null) {
			connLink = new SipUdpConnLink(channel);
			s_instances.put(lp, connLink);
		}
		return connLink;
	}

	/**
	 * private constructor
	 * @param channel channel that is creating this connection
	 */
	private SipUdpConnLink(SipUdpInboundChannel channel) {
		m_linkOnDeviceSide = null;
		m_linkOnApplicationSide = null;
		m_vc = null;
		m_channel = channel;
		/*TODO Liberty m_needToLearnRouterEndpoint = AdminHelper.getPlatformHelper().isZOS();*/
		m_sendThread = new SendThread(this);
		m_connected = false;
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
			Tr.debug(this, tc,"<init>",
				"receiveBufferSizeSocket [" + m_receiveBufferSizeSocket
				+ "] sendBufferSizeSocket [" + m_sendBufferSizeSocket
				+ "] receiveBufferSizeChannel [" + m_receiveBufferSizeChannel + ']');
		}
	}

	/**
	 * called by the channel while the channel factory instantiates
	 * the new OutboundVirtualConnection object
	 * @return the conn link instance associated with this connection
	 * @see SipUdpOutboundChannel#getConnectionLink(VirtualConnection)
	 */
	static SipUdpConnLink getPendingConnection() {
		SipUdpConnLink current = s_connectingInstance;
		s_connectingInstance = null;
		return current;
	}
	
	/**
	 * establishes the outbound virtual connection
	 */
	private void connect() throws IOException {
		String outboundChainName = m_channel.getOutboundChainName();
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this,  tc,"<connect>","outboundChainName = "  + outboundChainName);
		}
		try {
			ChannelFramework cf = ChannelFrameworkFactory.getChannelFramework();
			VirtualConnectionFactory factory = cf.getOutboundVCFactory(outboundChainName);
			VirtualConnection vc;
			
			synchronized (SipUdpConnLink.class) {
				s_connectingInstance = this;
				vc = factory.createConnection();
				// now s_connectingInstance is back to null
			}
			setVirtualConnection(vc);

			if (!(vc instanceof OutboundVirtualConnection)) {
				throw new IllegalStateException("Not an OutboundVirtualConnection");
			}
			setConnectionProperties(vc);
			OutboundVirtualConnection outboundConnection = (OutboundVirtualConnection)vc;
			UDPRequestContext connectRequestContext =
				UDPRequestContextFactory.getRef().createUDPRequestContext(
					null,  // local side address. can't use the real host:port because we're already listening to it
					0); // local side port number
			
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this,  tc,"<connect>","connectAsynch...");
			}
			outboundConnection.connectAsynch(connectRequestContext, this);
		}
		catch (ChannelException e) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc,"connect", "ChannelException", e);
			}
			throw new IOException(e.getMessage());
		}
		catch (ChainException e) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc,"connect", "ChainException", e);
			}
			throw new IOException(e.getMessage());
		}
		catch (Exception e) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc,"connect", "Exception", e);
			}
			throw new IOException(e.getMessage());
		}
	}
	
	/**
	 * sends outbound message
	 * @param message datagram to be sent
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.channelframework.UdpSender#send(com.ibm.ws.sip.stack.transaction.transport.connections.SipMessageByteBuffer)
	 */
	public void send(MessageContext messageContext, UseCompactHeaders useCompactHeaders) throws IOException {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this, tc,"SipUdpConnLink.send: " + 
								System.identityHashCode(messageContext) + 
								" isconnected = " + m_connected);
		}
		if (!m_connected) {
			connect();
		}
		m_sendThread.queue(messageContext);
	}
	
	/**
	 * sends out one message. always called from the send thread.
	 * @param message datagram to be sent
	 * @return true if completed immediately
	 */
	protected boolean sendNow(MessageContext messageContext) throws IOException {
		SipMessageByteBuffer message = messageContext.getSipMessageByteBuffer();
		messageContext.setSipMessageByteBuffer(null);
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this, tc,"SipUdpConnLink.sendNow: " + System.identityHashCode(message));
		}
		if (message == null) {
			throw new IOException("message is null in SipUdpConnLink.sendNow");
		}
		if (m_outboundBuffer != null) {
			throw new IOException("previous send not completed in SipUdpConnLink.sendNow");
		}
		
		UDPContext connectionContext = (UDPContext) m_linkOnDeviceSide.getChannelAccessor();
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this, tc,"sendNow", "SipUdpConnLink = " + this.hashCode()+ 
												"connectionContext = " + connectionContext.hashCode() +  
												" localAddress = " + connectionContext.getLocalAddress()+
												" localPort = " + connectionContext.getLocalPort()+
												" m_linkOnDeviceSide = " + m_linkOnDeviceSide.hashCode());
		}
		
		UDPWriteRequestContext writeCtx = connectionContext.getWriteInterface();
		
		if (writeCtx == null) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc,"sendNow", "Error: no write context");
			}
			return false;
		}
		SIPConnection sipConnection = messageContext.getSipConnection();
		String remoteHost = sipConnection.getRemoteHost();
		int remotePort = sipConnection.getRemotePort();
		InetSocketAddress address = InetAddressCache.getInetSocketAddress(remoteHost, remotePort);

		m_outboundBuffer = BaseConnection.stackBufferToWsBuffer(message);
		writeCtx.setBuffer((WsByteBuffer) m_outboundBuffer);
		VirtualConnection connection = writeCtx.write(address, this, false);
		boolean complete = connection != null;
		return complete;
	}
	
	// ------------------------------
	// SIPReadCallback implementation
	// ------------------------------
	
	/**
	 * called by the channel framework when a new message arrives
	 * @see com.ibm.wsspi.udp.channel.UDPReadCompletedCallback#complete(com.ibm.wsspi.channelfw.framework.VirtualConnection, com.ibm.wsspi.udp.channel.UDPReadRequestContext)
	 */
	public void complete(
		VirtualConnection connection,
		UDPReadRequestContext readCtx)
	{
		do {
			boolean drop = Dispatcher.instance().isOverLoaded();
			if (drop) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(this, tc,"complete", "Warning: dropping request under overloaded situation");
				}
			}
			else {
				// with UDP we don't get the remote address in SipConnLink.ready()
				// so this is our chance to get it
				UDPBuffer packet = readCtx.getUDPBuffer();
				WsByteBuffer buffer = packet.getBuffer();
				SocketAddress address = packet.getAddress();
				if (address == null) {
					if (c_logger.isInfoEnabled()) {
						c_logger.info("complete", null, "A message in order to use UDP connection link");
					}
				}
				else if (address instanceof InetSocketAddress) {
					InetSocketAddress inetAddress = (InetSocketAddress)address;
					BaseConnection datagram = new SipUdpConnection(m_channel, this);
					datagram.setRemoteAddress(inetAddress);
					datagram.messageReceived((WsByteBuffer) buffer);
					
					// on zOS, use the first message to learn about the listening points
					// of the sip-router (slsp) running in the control region
					if (m_needToLearnRouterEndpoint) {
						m_needToLearnRouterEndpoint = false;
						Hop slsp = datagram.getKey();
						if (slsp != null) {
							SLSPRouter.getInstance().addSLSP(slsp);
						}
					}
				}
				else {
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
						Tr.debug(this, tc,"complete",
							"Error: expected InetSocketAddress, got [" +
								(address == null ? "null" : address.getClass().getName()) + ']');
					}
				}
			}
			// peek for more messages. if nothing, prepare for next read
			connection = readCtx.read(this, false);
		} while (connection != null);
	}
	
	/**
	 * @see com.ibm.wsspi.udp.channel.UDPReadCompletedCallback#error(com.ibm.wsspi.channelfw.framework.VirtualConnection, com.ibm.wsspi.udp.channel.UDPReadRequestContext, java.io.IOException)
	 */
	public void error(VirtualConnection vc, UDPReadRequestContext rsc, IOException e) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this, tc,"SipUdpConnLink(read).error", "IOException", e);
		}
		close(vc, e);
	}

	// -------------------------------
	// SIPWriteCallback implementation
	// -------------------------------
	
	/**
	 * called by the channel framework when previous send completed
	 * @see com.ibm.wsspi.udp.channel.UDPWriteCompletedCallback#complete(com.ibm.wsspi.channelfw.framework.VirtualConnection, com.ibm.wsspi.udp.channel.UDPWriteRequestContext)
	 */
	public void complete(VirtualConnection connection, UDPWriteRequestContext writeCtx) {
		m_sendThread.sendComplete();
	}
	
	/**
	 * called when send completed to release the outbound buffer,
	 * either from the send thread (immediate) or from the channel thread (asynchronous)
	 */
	void releaseOutboundBuffer() {
		m_outboundBuffer.release();
		m_outboundBuffer = null;
	}
	
	/**
	 * send failed
	 * @see com.ibm.wsspi.udp.channel.UDPWriteCompletedCallback#error(com.ibm.wsspi.channelfw.framework.VirtualConnection, com.ibm.wsspi.udp.channel.UDPWriteRequestContext, java.io.IOException)
	 */
	public void error(VirtualConnection vc, UDPWriteRequestContext writeCtx, IOException e) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			if (e == null) {
				Tr.debug(this, tc,"SipUdpConnLink(write).error (no exception)");
			}
			else {
				Tr.debug(this, tc,"SipUdpConnLink(write).error", "IOException", e);
			}
		}
		close(vc, e);
	}

	// -------------------------------------
	// OutboundConnectionLink implementation
	// -------------------------------------

	/**
	 * @see com.ibm.wsspi.channelfw.OutboundConnectionLink#connect(java.lang.Object)
	 */
	public void connect(Object address) throws Exception {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Object[] params = { address };
			Tr.debug(this, tc, "connect", params);
		}
		 super.connect(address);
	}

	/**
	 * @see com.ibm.wsspi.channelfw.OutboundConnectionLink#connectAsynch(java.lang.Object)
	 */
	public void connectAsynch(Object address) {
		// copied from OutboundProtocolLink
		((OutboundConnectionLink)getDeviceLink()).connectAsynch(address);
	}

	/**
	 * called before connect() or connectAsync() to set properties of
	 * the outbound UDP connection according to configuration
	 */
	private void setConnectionProperties(VirtualConnection vc) {
		Map stateMap = vc.getStateMap();
		if (m_sendBufferSizeSocket != null) {
			stateMap.put(UDPConfigConstants.SEND_BUFF_SIZE, m_sendBufferSizeSocket);
		}
		if (m_receiveBufferSizeSocket != null) {
			stateMap.put(UDPConfigConstants.RCV_BUFF_SIZE, m_receiveBufferSizeSocket);
		}
		if (m_receiveBufferSizeChannel != null) {
			stateMap.put(UDPConfigConstants.CHANNEL_RCV_BUFF_SIZE, m_receiveBufferSizeChannel);
		}
	}

	// -----------------------------
	// ConnectionLink implementation
	// -----------------------------

	/**
	 * never called
	 * @see com.ibm.wsspi.channelfw.ConnectionLink#getChannelAccessor()
	 */
	public Object getChannelAccessor() {
		// copied from OutboundApplicationLink/InboundApplicationLink
        throw new IllegalStateException("Not implemented and should not be used");
	}

	/**
	 * @see com.ibm.wsspi.channelfw.ConnectionLink#close(com.ibm.wsspi.channelfw.framework.VirtualConnection, java.lang.Exception)
	 */
	public void close(VirtualConnection vc, Exception e) {
		if (!m_connected) {
			// avoid double-close
			return;
		}
		m_connected = false;

		// remove this conn link from the global table
		s_instances.remove(m_channel.getListeningPoint());

		// tell the channel that we're closed. this will cause the next message
		// to create a new instance of this class.
		m_channel.connectionClosed();

		// close the underlying connection and stop the thread
		ConnectionLink device = getDeviceLink();
		if (device != null) {
			device.close(vc, e);
		}
		
		m_sendThread.terminate();
	}

	/**
	 * @see com.ibm.wsspi.channelfw.ConnectionReadyCallback#destroy(java.lang.Exception)
	 */
	public void destroy(Exception e) {
		// copied from BaseConnectionLink.destroy()
		m_vc = null;
		m_linkOnDeviceSide = null;
		m_linkOnApplicationSide = null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wsspi.channelfw.base.OutboundProtocolLink#getVirtualConnection()
	 */
	public VirtualConnection getVirtualConnection() {
		return m_vc;
	}
	
	/**
	 * associates this conn link with a virtual connection.
	 * @param vc virtual connection to associate with this conn link
	 */
	protected void setVirtualConnection(VirtualConnection vc) {
		m_vc = vc;
	}

	/**
	 * @see com.ibm.wsspi.channelfw.ConnectionLink#getApplicationCallback()
	 */
	public ConnectionReadyCallback getApplicationCallback() {
		return m_linkOnApplicationSide;
	}

	/**
	 * @see com.ibm.wsspi.channelfw.ConnectionLink#setApplicationCallback(com.ibm.wsspi.channelfw.ConnectionReadyCallback)
	 */
	public void setApplicationCallback(ConnectionReadyCallback next) {
		m_linkOnApplicationSide = next;
	}

	/**
	 * @see com.ibm.wsspi.channelfw.ConnectionLink#getDeviceLink()
	 */
	public ConnectionLink getDeviceLink() {
		return m_linkOnDeviceSide;
	}

	/**
	 * @see com.ibm.wsspi.channelfw.ConnectionLink#setDeviceLink(com.ibm.wsspi.channelfw.ConnectionLink)
	 */
	public void setDeviceLink(ConnectionLink next) {
		m_linkOnDeviceSide = next;
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this, tc, "setDeviceLink, link = ", m_linkOnDeviceSide.hashCode());
		}
	}

	// --------------------------------------
	// ConnectionReadyCallback implementation
	// --------------------------------------
	
	/**
	 * called by the channel framework when a new connection is established
	 */
	public void ready(VirtualConnection vc) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this, tc,"ready", "this ["
				+ this + "] vc [" + vc + ']');
		}
		if (!m_connected) {
			m_connected = true;
		}
		
		UDPContext connectionContext = (UDPContext) m_linkOnDeviceSide.getChannelAccessor();
		UDPReadRequestContext readCtx = connectionContext.getReadInterface();
		
		complete(vc, readCtx);
		
//		VirtualConnection connection = readCtx.read(this, false)....
		
		VirtualConnection connection = readCtx.read(this, false);
		if (connection != null) {
			complete(connection, readCtx);
		}		
	}

	@Override
	protected void postConnectProcessing(VirtualConnection conn) {
		
	}

	@Override
	public String getProtocol() {
		return null;
	}

}
