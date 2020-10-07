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
package com.ibm.ws.sip.stack.transaction.transport;

import jain.protocol.ip.sip.ListeningPoint;
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.header.ViaHeader;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.stack.transaction.SIPTransactionConstants;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection;
import com.ibm.ws.sip.stack.transaction.util.SIPStackUtil;

/**
 * @author Amirk
 *
 * The Hop interface defines a location a request can transit on the way to its destination, i.e. a route. 
 * It defines the host, port and transport of the location. 
 * This interface is used to identify locations in the Router interface. 
 */
public class Hop 
{
	/**
	 * class logger 
	 */
	private static final LogMgr s_logger = Log.get(Hop.class);

	/**
	 * hop host
	 */
	String m_host;
	
	/**
	 * hop transport
	 */
	String m_trasport;
	
	/**
	 * hop port
	 */
	int m_port;
	
	public Hop( String transport , String host , int port  )
	{		
		m_trasport = transportToUpperCase(transport);
		m_host = SIPStackUtil.getHostAddress( host );
		m_port = port;
	}
	
	
	public Hop( ViaHeader via  ) 
	{                
		if( via.hasMAddr() )
		{				 
			m_host = via.getMAddr();
		}
		else 
		{
			//check for the received parameter , see 18.2.1 in the RFC
			String receivedParameter = via.getParameter(SIPTransactionConstants.RECEIVED);
			if( receivedParameter!=null )
			{
				m_host = receivedParameter;
			}
			else
			{
				m_host = SIPStackUtil.getHostAddress(  via.getHost() );
			}			 
		}
	
		m_trasport = transportToUpperCase(via.getTransport());
		if (m_trasport == null) 
			m_trasport = transportToUpperCase(SIPTransactionConstants.UDP);
		m_port = via.getRPort();
		if (m_port < 0) {
			m_port = via.getPort();
			if (m_port < 0) {
				m_port = m_trasport.equalsIgnoreCase(SIPTransactionConstants.TLS)
					? 5061
					: 5060;																						    
			}
		}
	}
	

	public Hop( SIPConnection connection  ) 
	{                
		m_host = connection.getRemoteHost();	
		m_trasport = transportToUpperCase(connection.getTransport());
		m_port = connection.hasAliacePort() ? connection.getAliacePort() :  connection.getRemotePort();
	}	
	
	
	/**
	 * @return String
	 */
	public String getHost() 
	{
		return m_host;
	}
	
	/**
	 * set the host name of this hop
	 * @param host host name of this hop
	 */
	public void setHost(String host) {
		m_host = host;
	}

	/**
	 * @return int
	 */
	public int getPort() 
	{
		return m_port;
	}
	
	/**
	 * set the port number of this hop
	 * @param port number of this hop
	 */
	public void setPort(int port) {
		m_port = port;
	}

	/**
	 * @return String
	 */
	public String getTrasport() 
	{
		return m_trasport;
	}
	
	/**
	 * set transport parameter of this hop 
	 * @param transport transport parameter, such as UDP, TCP, or TLS
	 */
	public void setTransport(String transport) {
		m_trasport = transportToUpperCase(transport);
	}
	
	public String toString()
	{
		return toString( this );
	}	
	
	public static String toString( Hop hop )
	{
		StringBuffer buf = new StringBuffer(hop.getHost());
		buf.append(":");
		buf.append(hop.getPort());
		buf.append("/");
		buf.append(transportToUpperCase(hop.getTrasport()));
		return  buf.toString();
	}
	
	
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof Hop)) {
			return false;
		}
		Hop otherHop = (Hop)other;
		if (!getTrasport().equalsIgnoreCase(otherHop.getTrasport())) {
			return false;
		}
		if (!getHost().equalsIgnoreCase(otherHop.getHost())) {
			return false;
		}
		
		// a port number of zero means ignore the port
		int port = getPort();
		int otherPort = otherHop.getPort();
		if (port > 0 && otherPort > 0 && port != otherPort) {
			return false;
		}
		return true;
	}
	
	public int hashCode() {
		// don't count the port number, because port 0 should equal any port
		return m_trasport.hashCode() * m_host.hashCode();
	}

	/**
	 * converts a SipURL object to a Hop object.
	 * if the transport is too weak for the scheme,
	 * it is upgraded to TLS. in this case, if the port is not specified,
	 * it is set to 5061.
	 * 
	 * @param address the source URL
	 * @return a new Hop object representing the URL
	 */
	public static Hop getHop(SipURL address) {
		String scheme = address.getScheme();
		String h = address.hasMAddr()
			? address.getMAddr()
			: address.getHost();
		String host = SIPStackUtil.getHostAddress(h);
		int port = address.hasPort()
			? address.getPort()
			: ListeningPoint.DEFAULT_PORT;
		String transport = address.hasTransport()
			? address.getTransport()
			: ListeningPoint.TRANSPORT_UDP;

		if (scheme.equalsIgnoreCase("sips")) {
			// must send using a secure transport
			if (!SIPConnectionsModel.instance().isTransportSecure(transport)) {
				transport = SIPConnectionsModel.instance().getDefaultSecureTransport();
			}
			if (!address.hasPort()) {
				// change implicit port to 5061
				port = SIPConnectionsModel.instance().getDefaultSecurePort();
			}
		}

		Hop hop = new Hop(transport, host, port);
		return hop;
	}

	/**
	 * converts given transport string to uppercase
	 * @param transport transport string
	 * @return the same string, uppercase
	 */
	private static String transportToUpperCase(String transport) {
		if (transport.equalsIgnoreCase("UDP")) {
			return "UDP";
		}
		if (transport.equalsIgnoreCase("TCP")) {
			return "TCP";
		}
		if (transport.equalsIgnoreCase("TLS")) {
			return "TLS";
		}
		return transport.toUpperCase();
	}
}
