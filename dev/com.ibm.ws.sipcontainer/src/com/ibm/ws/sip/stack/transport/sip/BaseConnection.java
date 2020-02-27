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

import java.io.IOException;
import java.net.InetSocketAddress;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sip.parser.util.InetAddressCache;
import com.ibm.ws.sip.stack.dispatch.Dispatcher;
import com.ibm.ws.sip.stack.transaction.transport.Hop;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnectionAdapter;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPListenningConnection;
import com.ibm.ws.sip.stack.transaction.transport.connections.SipMessageByteBuffer;
import com.ibm.ws.sip.stack.transaction.util.SIPStackUtil;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;

/**
 * base class for any connection object.
 * there is one instance of this class per any connection,
 * inbound, outbound, udp, tcp, or tls
 * 
 * @author ran
 */
public abstract class BaseConnection extends SIPConnectionAdapter
{
	
	private static final TraceComponent tc = Tr.register(BaseConnection.class);
	
	
	/** peer address and port number */
	private InetSocketAddress m_peer;
	
	/** the channel that created this connection */
	private SIPListenningConnection m_channel;
	
	/**
	 * constructor for inbound channels
	 * @param channel channel that created this connection
	 */
	public BaseConnection(SIPListenningConnection channel) {
		this(null, 0, channel);
	}
	
	/**
	 * constructor
	 * @param peerHost remote host address in dotted form, null if inbound or udp
	 * @param peerPort remote port number, 0 if inbound or udp
	 * @param channel channel that created this connection
	 */
	public BaseConnection(String peerHost, int peerPort, SIPListenningConnection channel) {
		super(peerHost, peerPort);
		m_peer = peerHost == null
			? null // inbound, will be set when connection is accepted
			: InetAddressCache.getInetSocketAddress(peerHost, peerPort); // outbound
		m_connectionStatus = ConnectionStatus.PRE_CONNECT;
		m_channel = channel;
	}
	
	/**
	 * called by the derived class when connection established, either inbound
	 * or outbound
	 */
	protected void connectionEstablished() {
		super.connectionEstablished();
	}

	/**
	 * converts a SipMessageByteBuffer to a WsByteBuffer,
	 * and recycles the SipMessageByteBuffer
	 * @param stackBuffer a SipMessageByteBuffer
	 * @return a WsByteBuffer allocated from the pool
	 */
	protected static WsByteBuffer stackBufferToWsBuffer(SipMessageByteBuffer stackBuffer) {
		byte[] bytes = stackBuffer.getBytes();
		int length = stackBuffer.getMarkedBytesNumber();
		WsByteBuffer buffer = ChannelFrameworkFactory.getBufferManager().allocate(length);
		buffer.put(bytes, 0, length);
		stackBuffer.reset();
		buffer.flip();
		return buffer;
	}

	/**
	 * called when new data arrives
	 */
	protected void messageReceived(WsByteBuffer buffer) {
		buffer.flip();
		int length = buffer.remaining();
		SipMessageByteBuffer data = SipMessageByteBuffer.fromPool();
		data.ensureCapacity(length);
		byte[] bytes = data.getBytes();
		buffer.get(bytes, 0, length); // move data from WsByteBuffer to SipMessageByteBuffer
		buffer.clear(); // for some reason it doesn't work without this
		buffer.release(); // recycle old buffer
		data.setContentSize(length);

		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "messageReceived " + data);
		
		Dispatcher.instance().queueIncomingDataEvent(data, this);
	}

	/**
	 * called by inbound channels when a new connection is accepted
	 * @param address peer address and port
	 */
	protected void setRemoteAddress(InetSocketAddress address) {
		if (m_peer == null) {
			m_peer = address;
			String peerHost = SIPStackUtil.getHostAddress(address.getAddress());
			int peerPort = address.getPort();
			setRemoteHost(peerHost);
			setRemotePort(peerPort);
			Hop hop = new Hop(getTransport(), peerHost, peerPort);
			setKey(hop);
		}
		else {
			throw new IllegalStateException("setRemoteAddress: already connected");
		}
	}

	// ----------------------------
	// SIPConnection implementation
	// ----------------------------

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#start()
	 */
	public void start() throws IOException {
	}

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#close()
	 */
	public void close() {
		m_connectionStatus = ConnectionStatus.CLOSED;
		super.close();
	}

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#getSIPListenningConnection()
	 */
	public SIPListenningConnection getSIPListenningConnection() {
		return m_channel;
	}
}
