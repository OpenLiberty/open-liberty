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
package com.ibm.ws.sip.stack.transaction.transport.connections.tcp;

import jain.protocol.ip.sip.ListeningPoint;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.ListeningPointImpl;
import com.ibm.ws.sip.parser.util.InetAddressCache;
import com.ibm.ws.sip.stack.dispatch.Dispatcher;
import com.ibm.ws.sip.stack.transaction.transport.Hop;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPListenningConnection;

public class SIPListenningConnectionImpl
	implements SIPListenningConnection  
{

	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger =
		Log.get(SIPListenningConnectionImpl.class);


	/** is running */
	private boolean isRunning;

	/** the listening socket */
	private ServerSocket m_sock;
		
	/** the local litenning point to listen on */
	private ListeningPointImpl m_lp;
	 	
	SIPListenningConnectionImpl( ListeningPointImpl lp  )
	{
		m_lp = lp;		
	}
	
	public SIPConnection createConnection( InetAddress remoteAdress , int remotePort )
		throws IOException
	{
		return new SIPConnectionImpl( this , remoteAdress , remotePort );	
	}
	
	public synchronized void	listen() throws IOException
	{
		try {
			InetAddress address = InetAddressCache.getByName(m_lp.getHost());
			m_sock = new ServerSocket(m_lp.getPort(), 0, address);

			//if the port was 0 , we will get a listenning port by default.
			//then we should set back the port to the Listenning Point Object.
			//if the port was not 0 , it will just set the same port
			m_lp.setPort(m_sock.getLocalPort());

			Thread thread = new Thread(new ConnectionsListener(this),
					"SIP TCP Connections Listener on " + m_lp);
			isRunning = true;
			thread.start();
		}
		catch (IOException ex)
		{
			if( c_logger.isTraceDebugEnabled())
			{
			    c_logger.traceDebug(this,"listen",ex.getMessage());
			}
			throw ex;
		}			
	}
	
	public synchronized void stopListen()
	{
		isRunning = false;
		close();
	}
	
	public synchronized void close()
	{		
		try 
		{
			m_sock.close();
			notifyClosed();
		} 
		catch (IOException e) 
		{
			if( c_logger.isTraceDebugEnabled())
			{
			c_logger.traceDebug(this,"close",e.getMessage());
			}
		}
	}
	
	private void notifyConnectionCreated( SIPConnection connection )
	{
		Dispatcher.instance().queueConnectionAcceptedEvent(this, connection);
	}
	
	protected  void notifyClosed()
	{
		// todo
	}
	
	
	public ListeningPoint getListeningPoint()
	{
		return m_lp;	
	}
	
	public String toString()
	{
		return m_lp.toString();
	}
	
	
		
	/** listens to new connections */
	public class ConnectionsListener implements Runnable
	{
		SIPListenningConnection m_parent;
		
		ConnectionsListener( SIPListenningConnection parent )
		{
			m_parent = parent;
		}
		/** 
		 * Run method for the thread that gets created for each accept
		* socket.
		*/
		public void run()
		{
			// Accept new connectins on our socket.
			while (isRunning)
			{
				try
				{
					Socket sock = m_sock.accept();
					InetAddress address = sock.getInetAddress();
					int port = sock.getPort();					
					Hop key = new Hop("TCP", InetAddressCache.getHostAddress(address), port);
					SIPConnectionImpl connection = new SIPConnectionImpl(m_parent, sock);
					connection.setKey(key);
					notifyConnectionCreated(connection);														
				}
				catch (IOException ex)
				{
					if( c_logger.isTraceDebugEnabled())
					{
						c_logger.traceDebug(this,"run",ex.getMessage());
					}
					stopListen();
				}
				catch (Throwable t )
				{
					if( c_logger.isTraceDebugEnabled())
					{
						c_logger.traceDebug(this,"run",t.getMessage());
					}
					stopListen();
				}
			}
		}		
	}	
}
