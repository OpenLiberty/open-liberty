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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.parser.MessageParser;
import com.ibm.ws.sip.parser.StreamMessageParser;
import com.ibm.ws.sip.parser.util.InetAddressCache;
import com.ibm.ws.sip.stack.context.MessageContext;
import com.ibm.ws.sip.stack.dispatch.Dispatcher;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;
import com.ibm.ws.sip.stack.transaction.transport.UseCompactHeaders;
import com.ibm.ws.sip.stack.transaction.util.SIPStackUtil;
/**
 * @author Amirk
 * 
 *  this is a base class for connection oriented protocols - TCP,TLS
 */
public abstract class SIPStreamConectionAdapter extends SIPConnectionAdapter
{
	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger =
		Log.get(SIPStreamConectionAdapter.class);
	
	/** 
	 * out stream 
	 **/
	protected Socket m_socket;
	
	/** 
	 * the listenning connection the connection was created on
	 **/
	private SIPListenningConnection m_listenningConnection;
	
	/** 
	 * a thread to read from the network 
	 **/
	private NetworkReader m_netReader;
	
	/** 
	 * a thread to write to the network 
	 **/
	private NetworkWriter m_netWriter;
	
	/** stateful message parser dedicated to this stream connection */
	private MessageParser m_messageParser;
	
	/**
	 * true if connect() should block the caller
	 * false if done from the network writer thread 
	 */
	private static boolean s_blockingConnect = false;
	
	/** timeout in milliseconds for creating outbound connections */
	private static int s_connectTimeout = SIPTransactionStack.instance().getConfiguration().getConnectTimeout();
	
	/**
	 * constructor for inbound connection
	 * @param listenningConnection
	 * @param socket
	 */
	public SIPStreamConectionAdapter(
		SIPListenningConnection listenningConnection,
		Socket socket)
	{
		this(
			listenningConnection,
			SIPStackUtil.getHostAddress(socket.getInetAddress()),
			socket.getPort());
		m_socket = socket;
		connectionEstablished();
	}
	
	/**
	 * constructor for outbound connection.
	 * normally followed by a call to connect()
	 * @param listenningConnection
	 */
	public SIPStreamConectionAdapter(
			SIPListenningConnection listenningConnection,
			String peerHost,
			int peerPort)
	{
		super(peerHost, peerPort);
		//m_byteBuferPools = new ObjectPool( SipMessageByteBuffer.class );
		m_listenningConnection = listenningConnection;
		m_socket = null;
		m_connectionStatus = ConnectionStatus.PRE_CONNECT;
		m_messageParser = new StreamMessageParser(this);
	}
	
	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#write(jain.protocol.ip.sip.message.Message)
	 */
	public void write(MessageContext messageContext, boolean considerMtu, UseCompactHeaders useCompactHeaders) throws IOException {
		prepareBuffer(messageContext, considerMtu, useCompactHeaders);
		write(messageContext);
	}

	private void write(MessageContext messageContext) throws IOException
	{
		//we must check that the netwriter is available
		//and that the connection is not closed
		if( m_netWriter!=null && !isClosed())
		{
			m_netWriter.send(messageContext);
		}
		else
		{
			if( c_logger.isTraceDebugEnabled())
			{
			c_logger.traceDebug(this,"write","m_netwriter is null!!!");
			}
			Exception e = new IOException("m_netwriter is null!!!");
			connectionError(e);
			messageContext.writeError(e);
		}
	}
	
	/** the local connection on which the connection was created */
	public SIPListenningConnection getSIPListenningConnection()
	{
		return m_listenningConnection;
	}
	
	public synchronized void connect() throws IOException
	{
		if (s_blockingConnect) {
			establishConnection();
		}
		// otherwise establish the connection in the writer thread,
		// just before sending the first packet
	}
	
	/**
	 * establishes connection with peer, blocking the caller until
	 * connection is established or error occurs, or timeout, if set, is exceeded.
	 * @throws IOException
	 */
	private void establishConnection() throws IOException {
		String host = getRemoteHost();
		int port = getRemotePort();
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "establishConnection", host + ":" + port);
		}
		m_socket = createSocket();
		InetSocketAddress address = InetAddressCache.getInetSocketAddress(host, port);
		try {		
			m_socket.connect(address, s_connectTimeout);
			connectionEstablished();
		} catch (IOException e) {
			connectionError(e);
			throw e;
		}
	}
	
	public synchronized void close()
	{
		try
		{
			m_connectionStatus = ConnectionStatus.CLOSED;
			if (m_socket != null) {
				m_socket.close();
			}
			m_netWriter.wakeUp();						
		}
		catch (IOException exp)
		{
			if( c_logger.isTraceDebugEnabled())
			{
			c_logger.traceDebug(this,"close",exp.getMessage(),exp);
			}
		} finally {
			super.close();
		}
	}


	public void start() throws IOException
	{
		//thread to read from socket 
		if (m_socket != null) {
			startReader();
		}
		//thread to write to socket 
		startWriter();
	}

	/**
	 * starts the thread that receives messages
	 */
	private void startReader() throws IOException {
		m_netReader = new NetworkReader(m_socket.getInputStream(), this);
		StringBuffer reader = new StringBuffer("SIP Reader ");
		getPeer(reader);
		Thread networkListeningThread = new Thread(m_netReader, reader.toString() );
		networkListeningThread.start();
	}

	/**
	 * starts the thread that sends messages
	 */
	private void startWriter() {
		m_netWriter = new NetworkWriter(this);
		StringBuffer writer = new StringBuffer("SIP Writer ");
		getPeer(writer);
		Thread networkWritingThread = new Thread(m_netWriter, writer.toString() );
		networkWritingThread.start();
	}
	
	public int hashCode()
	{
		return m_socket.hashCode();
	}
	
	public boolean isReliable()
	{
		return true;
	}
	
	/**
	 * gets the message parser associated with this connection.
	 * each stream connection has its own message parser
	 * 
	 * @return the message parser associated with this connection
	 */
	public MessageParser getMessageParser() {
		return m_messageParser;
	}
	
	/**
	 * @return a string representation of the connection
	 */
	private void getPeer(StringBuffer buf) {
		buf.append(getRemoteHost());
		buf.append(':');
		buf.append(getRemotePort());
		buf.append('/');
		buf.append(getTransport());
	}
	
	/**
	 * read data from network and flushes into pipe
	 * @author Amirk
	 */
	static class NetworkReader implements Runnable
	{
		/** the socket input stream */
		private InputStream m_networkInputStream;
		
		/** the source connection */
		private SIPStreamConectionAdapter m_connection;
		
		NetworkReader(InputStream networkInputStream, SIPStreamConectionAdapter connection)
		{
			m_networkInputStream = networkInputStream;
			m_connection = connection;

		}
		
		/** 
		 * keeps reading data from network until some error occurs 
		 */
		public void run() {
			Dispatcher dispatch = Dispatcher.instance();
			
			try {
				SIPStreamConectionAdapter connection = m_connection;
				Socket socket = connection.m_socket;
				final int packetSize = socket.getReceiveBufferSize();
				final String peerHost = connection.getRemoteHost();
				final int peerPort = connection.getRemotePort();
				byte[] buf = new byte[packetSize];
				
				while (connection.isConnected()) {
					// wait for incoming data
					int nBytes = m_networkInputStream.read(buf, 0, packetSize);
					if (nBytes == -1) {
						throw new IOException("Socket broken" + this);
					}
					// copy network bytes to a buffer that is safely
					// passed to the parser thread
					SipMessageByteBuffer byteBuffer = SipMessageByteBuffer.fromNetwork(
						buf,
						nBytes,
						peerHost,
						peerPort);
					dispatch.queueIncomingDataEvent(byteBuffer, connection);
				}
			}
			catch (IOException e)
			{
				// Assaf: print the damn exceptions!
				//dont just dump it - I got 2 question about the stack throwing exception
				//but yes
				//print the message
				if( c_logger.isTraceDebugEnabled())
				{
				c_logger.traceDebug(this,"run", "Connection closed" + e.getMessage(),e);
				}
				m_connection.connectionError(e);
			}
		}
	}
		
	/**
	 * write data from network and flushes to socket
	 * @author Amirk
	 */
	static class NetworkWriter implements Runnable
	{
		/**
		 * Queue for Sip mesages as bytes that are waiting to be dispatched to the network 
		 */
		// @PMD:REVIEWED:SizeNotAllocatedAtInstantiation: by Amirk on 9/19/04 11:45 AM
		private LinkedList<MessageContext> m_msgQueue = new LinkedList<MessageContext>();
		
		/** the source connection */
		private SIPStreamConectionAdapter m_connection;
		
		NetworkWriter(SIPStreamConectionAdapter connection)
		{
			m_connection = connection;
		}
		
		
		/**
		 * release the wait block
		 */
		public void wakeUp()
		{
			//	Add the message to the list		
			synchronized (m_msgQueue)
			{
				m_msgQueue.notify();
			}			
		}
		
		public LinkedList<MessageContext> getMessages(){
			return m_msgQueue;
		}
		
		/**
		 * add the message to the queue to be dispached
		 * @param msg
		 */
		public void send(MessageContext messageSendingContext)
		{
			//	Add the message to the list		
			synchronized (m_msgQueue)
			{
				m_msgQueue.addLast(messageSendingContext);
				m_msgQueue.notify();
			}
		}
		
		/** 
		 * keep wait for messages and send them to the network
		 * untill error occurs 
		 **/
		public void run()
		{
			if (m_connection.m_socket == null) {
				// connection is not established yet
				try {
					m_connection.establishConnection();
				}
				catch (IOException e) {
					if (c_logger.isTraceDebugEnabled()) {
						StringBuffer buf = new StringBuffer();
						buf.append("Failed connecting to [");
						m_connection.getPeer(buf);
						buf.append(']');
						c_logger.traceDebug(buf.toString());
						c_logger.traceDebug(this, "run", "IOException", e);
					}
					return;
				}
				// now that we have a connection, we can start the reader thread
				try {
					m_connection.startReader();
				}
				catch (IOException e) {
					if (c_logger.isTraceDebugEnabled()) {
						StringBuffer buf = new StringBuffer();
						buf.append("Failed starting reader thread with [");
						m_connection.getPeer(buf);
						buf.append(']');
						c_logger.traceDebug(buf.toString());
						c_logger.traceDebug(this, "run", "IOException", e);
					}
					return;
				}
			}
			
			OutputStream outStream = null;
			try
			{
				outStream = m_connection.m_socket.getOutputStream();
			}
			catch (IOException e1)
			{
				m_connection.connectionError(e1);
			}
			SipMessageByteBuffer msg = null;
			MessageContext messageSendingContext = null;
			while (m_connection.isConnected()){
			    messageSendingContext = null;
				msg = null;
				try
				{
					synchronized (m_msgQueue)
					{
						if (m_msgQueue.isEmpty())
						{
							m_msgQueue.wait();
						}
						if (!m_msgQueue.isEmpty())
						{
							messageSendingContext = (MessageContext) m_msgQueue.removeFirst();
						}
					}
				}
				catch (InterruptedException e)
				{
					if( c_logger.isTraceDebugEnabled())
					{
						c_logger.traceDebug(this,"run",e.getMessage(),e);
					}
				}
				if (messageSendingContext != null){
					msg = messageSendingContext.getSipMessageByteBuffer();
					try
					{
					    outStream.write(msg.getBytes(), 0, msg.getMarkedBytesNumber() );												
						outStream.flush();
						msg.reset();
						msg = null;
					    messageSendingContext.writeComplete();
					}
					catch (Exception e)
					{
						m_connection.connectionError(e);
						messageSendingContext.writeError(e);
					}
				}
			}
		}
	}
	
	public void connectionError(Exception e){
		//we aviod loop everything was already handled
		if (!isClosed()) {
			super.connectionError(e);
		}
		List<MessageContext> pendingMessageContextList = null;
		if (m_netWriter != null){
			pendingMessageContextList = m_netWriter.getMessages();
		}
		synchronized (pendingMessageContextList) {
			cleanPendingMessages(pendingMessageContextList,e);
		}
	}
	/**
	 * get the instance of the socket ( SSLSocket or PlainSocket )
	 * @return - the created socket
	 * @throws IOException - if the socket could not be created
	 */
	public abstract Socket createSocket()
	 	throws IOException;
	
	/**
	 * applies only to non-reliable sockets.
	 * caller should have checked isReliable() before calling this.
	 * 
	 * @return always -1, meaning unlimited MTU
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#getPathMTU()
	 */
	public int getPathMTU() {
		return -1;
	}

}
