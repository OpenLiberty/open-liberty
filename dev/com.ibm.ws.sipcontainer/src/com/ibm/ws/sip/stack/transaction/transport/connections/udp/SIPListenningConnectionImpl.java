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
package com.ibm.ws.sip.stack.transaction.transport.connections.udp;

import jain.protocol.ip.sip.ListeningPoint;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.ListeningPointImpl;
import com.ibm.ws.sip.parser.util.InetAddressCache;
import com.ibm.ws.sip.properties.StackProperties;
import com.ibm.ws.sip.stack.context.MessageContext;
import com.ibm.ws.sip.stack.dispatch.Dispatcher;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPListenningConnection;
import com.ibm.ws.sip.stack.transaction.transport.connections.SipMessageByteBuffer;
import com.ibm.ws.sip.stack.transaction.util.ApplicationProperties;
import com.ibm.ws.sip.stack.transaction.util.SIPStackUtil;

public class SIPListenningConnectionImpl
	implements SIPListenningConnection, Runnable
{
	/** class Logger */
	private static final LogMgr s_logger = Log.get(SIPListenningConnectionImpl.class);

	/**
	 * maximum transmission unit for outgoing UDP requests
	 */
	private static int s_pathMTU = ApplicationProperties.getProperties().getInt(StackProperties.PATH_MTU);
			
	/** true as long as the listening thread is running */
	private boolean m_isRunning;

	/** the listening socket */
	private DatagramSocket m_sock;

	/** the thread that is listening to the socket and raising events */
	private Thread m_listeningThread;

	/** the local listening point to listen on */
	private ListeningPointImpl m_lp;

	/** UDP Sender thread. Sending is done via a dedicated thread */
	private UDPSenderThread m_sender;
	
	/**
	 * constructor.
	 * caller should call listen() explicitly to listen on the port,
	 * and to start the listener thread.
	 * the sender thread is started implicitly.
	 * @param lp listening point with address and port number
	 * @throws IOException
	 */
	SIPListenningConnectionImpl(ListeningPointImpl lp) throws IOException {
		try {
			init(lp);
		}
		catch (IOException e) {
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug(this, "SIPListenningConnectionImpl", e.getMessage(), e);
			}
			throw e;
		}
	}

	/**
	 * called from the constructor to initialize this listener.
	 * caller should call listen() explicitly to listen on the port,
	 * and to start the listener thread.
	 * the sender thread is started implicitly.
	 * @param lp listening point with address and port number
	 * @throws IOException
	 */
	private void init(ListeningPointImpl lp) throws IOException {
		m_lp = lp;
		InetAddress address = InetAddressCache.getByName(m_lp.getHost());
		m_sock = new DatagramSocket(m_lp.getPort(), address);

		//if the port was 0 , we will get a listenning port by default.
		//then we should set back the port to the Listenning Point Object.
		//if the port was not 0 , it will just set the same port 
		m_lp.setPort(m_sock.getLocalPort());
		
		int bufferSize = ApplicationProperties.getProperties().
			getInt(StackProperties.UDP_RECEIVE_BUFFER_SIZE);
		
		m_sock.setReceiveBufferSize(bufferSize);
		m_sock.setSendBufferSize(bufferSize);
		m_sender = new UDPSenderThread(m_sock);
		m_sender.start();
	}

	/**
	 * public interface for creating an outbound connection
	 */
	public SIPConnection createConnection(InetAddress remoteAddress, int remotePort) {
		String ip = InetAddressCache.getHostAddress(remoteAddress);
		return new SIPConnectionImpl(this, ip, remotePort);
	}

	/**
	 * starts the listener thread
	 */
	public synchronized void listen() throws IOException {
		m_listeningThread = new Thread(this, "Listenning Thread on " + m_lp);
		m_listeningThread.start();
	}

	/**
	 * tells the listener thread to quit
	 */
	public synchronized void stopListen() {
		m_isRunning = false;
	}

	/**
	 * stops listenning
	 */
	public synchronized void close() {
		m_isRunning = false;
		notifyClosed();
	}

	/**
	 * asynchronously notifies the application about a connection that got closed
	 */
	private synchronized void notifyClosed() {
		// todo
	}

	/**
	 * @return the JAIN listening point
	 */
	public ListeningPoint getListeningPoint() {
		return m_lp;
	}

	/**
	 * returns the path MTU (maximum transmission unit)
	 * - maximum number of bytes that can be sent in a single packet. 
	 * @return the path MTU in bytes, or 1500 if unknown (see 3261-18.1.1)
	 */
	public static int getPathMTU() {
		return s_pathMTU;
	}

	/**
	 * asynchronously creates a packet and sends it
	 * 
	 * @param packet message to send
	 * @param peerHost destination address
	 * @param peerPort destination port number
	 */
	public void write(MessageContext messageSendingContext, String peerHost, int peerPort) {
		m_sender.addToQ(messageSendingContext);
	}

	public List<MessageContext> getMessages(){
		return m_sender.getMessagesFromQ();
	}
	
	/**
	 * listens for incoming data
	 */
	public void run() {
		Dispatcher dispatch = Dispatcher.instance();
		
		try {
			final int packetSize = m_sock.getReceiveBufferSize();
			byte buf[] = new byte[packetSize];
			DatagramPacket packet = new DatagramPacket(buf, packetSize);
			m_isRunning = true;
			
			while (m_isRunning) {
				// wait for incoming data
				m_sock.receive(packet);
				if (dispatch.isOverLoaded()) {
				    handleOverLoading();
				}
				else {
					// copy network bytes to a buffer that is safely
					// passed to the parser thread (dispatch)
					int messageSize = packet.getLength();
					String peerHost = SIPStackUtil.getHostAddress(packet.getAddress());
					int peerPort = packet.getPort();
					SipMessageByteBuffer byteBuffer = SipMessageByteBuffer.fromNetwork(
						buf,
						messageSize,
						peerHost,
						peerPort);
					
					// every incoming datagram is a new "connection"
					SIPConnectionImpl connection = new SIPConnectionImpl(
						this,
						peerHost,
						peerPort);
					connection.setRemoteHost(peerHost);
					connection.setRemotePort(peerPort);
					connection.setConnected();
	
					dispatch.queueConnectionAcceptedEvent(this, connection);
					dispatch.queueIncomingDataEvent(byteBuffer, connection);
				}
			}
		}
		catch (IOException e) {
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug(this, "run", e.getMessage());
			}
		}
		close();
	}

	/**
	 * handles cases when dispatcher reaches its max capacity and is overloaded. 
	 * this could happen in cases where the stack or container are not capable
	 * of processing the required load. in that case there is no use in adding 
	 * new udp packets to queue and we better drop them. if the queue is not 
	 * cleared in 1-4 seconds then it will most likey get filled with 
	 * retransmissions. 
	 */
	private void handleOverLoading() {
		// do nothing - just drop packet
	}
}
