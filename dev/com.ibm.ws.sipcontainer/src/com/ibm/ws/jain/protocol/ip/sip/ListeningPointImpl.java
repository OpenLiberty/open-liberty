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
package com.ibm.ws.jain.protocol.ip.sip;

import jain.protocol.ip.sip.ListeningPoint;
import jain.protocol.ip.sip.SipProvider;
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.header.ViaHeader;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.parser.util.InetAddressCache;
import com.ibm.ws.sip.stack.transaction.SIPTransactionConstants;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;
import com.ibm.ws.sip.stack.util.SipStackUtil;

/**
 * @author Amir kalron
 * 
 * This interface represents a unique IP network listening point,
 * and consists of host, port and transport
 *
 * @version 1.0
 */
public class ListeningPointImpl
	implements ListeningPoint	
{
	/** class Logger */
	private static final LogMgr s_logger = Log.get(ListeningPointImpl.class);

	/** Serialization UID (do not change) */
	private static final long serialVersionUID = -2276413619963717197L;
	
	/** default TLS port. literally 5061 */
	public static final int DEFAULT_SECURE_PORT = ListeningPoint.DEFAULT_PORT + 1;

	/** TLS Transport constant */
	public static final String TRANSPORT_TLS = "tls";	

	/** the listening host */
	private String m_host;
	
	/** listening port */
	private int m_port;
	
	/** transport type */
	private String m_transport;
	
	/** is secure */	
	private boolean m_isSecure;
	
	/** is reliable */	
	private boolean m_isReliable;
	
	private String m_channelName;

	/**
	 * true if configuration specified "0.0.0.0" for this listening point.
	 * in this case, m_host holds the actual IP address or host name that
	 * this listening point is bound to (so that our Via contains a real
	 * address) and this flag indicates that the original configuration was
	 * "0.0.0.0". 
	 */
	private boolean m_isAny;

	/**
	 * there is a one to one relationsship between
	 * the listener and the provider
	 */
	private transient SipProvider m_provider;

	/**
	 * Sent-by host in the top via header of outbound requests.
	 * May be null, in which case the IP address is used.
	 */
	private String m_sentBy;
	
	/**
	 * a value to add to the Call-Id taken from the custom property.
	 * If null, a randomly generated value will be used for Call-Id
	 */
	private String m_callIdValue;

	/** shared constructor code */
	private final void init(String host, int port, String transport, String channelName) {
		try {
			if (SipStackUtil.isHostName(host)) {
				// admin specified a host name.
				// if it resolves to a single IP, use that IP.
				// otherwise, just use the host name as is.
				m_host = getListeningPointHost(host);
				m_isAny = false;
			}
			else {
				// admin specified an IP address
				InetAddress address = InetAddressCache.getByName(host);
				if (address.isAnyLocalAddress()) {
					// admin specified 0.0.0.0 (or *)
					// this is a problem, because we cannot put 0.0.0.0 in our Via header.
					// we must use the local IP address instead.
					// if there are multiple local IP addresses, just use the host name
					String localHostName = InetAddress.getLocalHost().getCanonicalHostName();
					m_host = getListeningPointHost(localHostName);
					m_isAny = true;
				}
				else {
					m_host = InetAddressCache.getHostAddress(address);
					m_isAny = false;
				}
			}
		}
		catch (UnknownHostException e) {
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug(this, "init", e.getMessage(), e);
			}
			m_host = host;
			m_isAny = false;
		}

		// determine transport and security level
		if (transport == null) {
			m_transport = TRANSPORT_UDP;
			m_isSecure = false;
			m_isReliable = false;
		}
		else if (transport.equalsIgnoreCase(TRANSPORT_UDP)) {
			m_transport = TRANSPORT_UDP;
			m_isSecure = false;
			m_isReliable = false;
		}
		else if (transport.equalsIgnoreCase(TRANSPORT_TCP)) {
			m_transport = TRANSPORT_TCP;
			m_isSecure = false;
			m_isReliable = true;
		}
		else if (transport.equalsIgnoreCase(TRANSPORT_TLS)) {
			m_transport = TRANSPORT_TLS;
			m_isSecure = true;
			m_isReliable = true;
		}
		else {
			m_transport = transport.toLowerCase();
			m_isSecure = false;
			m_isReliable = true;
		}

		// port number
		m_port = port > 0
			? port
			: (m_isSecure ? DEFAULT_SECURE_PORT : DEFAULT_PORT);

//		TODO Liberty Multihome - sendByHost should be removed ?!?!?!
		m_sentBy = SIPTransactionStack.instance().getConfiguration().getSentByHost();
		if (m_sentBy == null) {
			m_sentBy = m_host;
		}
		
		// this value is used when generate a Call-Id. Use m_host to keep using a local IP.
//		TODO Liberty Anat - removed because it throw NPE in init of SIPTransactionStack... Should be unremarked
		m_callIdValue = SIPTransactionStack.instance().getConfiguration().getCallIdValue();
		if (m_callIdValue == null) {
			m_callIdValue = m_host;
		}
		m_channelName = channelName;
	}
	
	/**
	 * returns the host name for the listening point
	 * given the host name in configuration
	 * @param hostName host name in configuration
	 * @return the host name for the listening point
	 * @throws UnknownHostException
	 */
	private final String getListeningPointHost(String hostName) throws UnknownHostException {
		InetAddress[] addresses = InetAddress.getAllByName(hostName);
		if (addresses.length == 1) {
			// name resolves to a single IP - use that IP
			return InetAddressCache.getHostAddress(addresses[0].getHostAddress());
		}
		// name resolves to multiple IPs - use the name
		return hostName;
	}

	/**
	 * constructor
	 * 
	 * @param host - host to listen on
	 * @param port - port to listen on
	 * @param transport - transport type
	 */
	public ListeningPointImpl(String host, int port, String transport) {
		init(host, port, transport, null);
	}
	
	/**
	 * constructor
	 * 
	 * @param host - host to listen on
	 * @param port - port to listen on
	 * @param transport - transport type
	 * @param channelName
	 */
	public ListeningPointImpl(String host, int port, String transport, String channelName ) {
		init(host, port, transport, channelName);
	}

	/**
	 * constructor
	 * @param via - the via header will have all parameters -
	 *  host,port,transport and security, to create the Listenning point
	 */
	public ListeningPointImpl(ViaHeader via) {
		String host = via.getHost();
		int port = via.getPort();
		String transport = via.getTransport();
		init(host, port, transport, null);
	}

	/**
	 * constructor
	 * @param address - the SipURL will have all parameters -
	 *  host, port, transport and security, to create the Listenning point
	 */
	public ListeningPointImpl(SipURL address) {
		String host = address.hasMAddr()
			? address.getMAddr()
			: address.getHost();
		boolean isSecure = address.getScheme().equalsIgnoreCase(SIPTransactionConstants.SIPS);
		String transport = address.getTransport();
		if (transport == null) {
			transport = isSecure
				? ListeningPointImpl.TRANSPORT_TLS
				: ListeningPointImpl.TRANSPORT_UDP;
		}
		int port = address.hasPort() ? address.getPort() : 0;
		init(host, port, transport, null);
	}
	
	/** returns true if tls */
	public boolean isSecure() {
		return m_isSecure;
	}
	
	/** returns the host */
	public String getHost() {
		return m_host;
	}

	/** returns the port */
	public int getPort() {
		return m_port;
	}

	/** returns transport */
	public String getTransport() {
		return m_transport;
	}
	
	/** returns channel name */
	public String getChannelName() {
		return m_channelName;
	}
	
	/**
	 * @return the sent-by host, as specified in configuration,
	 *  or the value of {@link #getHost()} if not set
	 */
	public String getSentBy() {
		return m_sentBy;
	}
	
	/**
	 * @return the value to add the the Call-Id
	 */
	public String getCallIdValue() {
		return m_callIdValue;
	}
	
	/** string representation */
	public String toString() {
		StringBuffer buf = new StringBuffer(64);
		buf.append(( m_isSecure ? SIPTransactionConstants.SIPS : SIPTransactionConstants.SIP ));
		buf.append(":");
		buf.append(m_host);
		buf.append(":");
		buf.append(m_port);
		buf.append(";");
		buf.append(m_transport);
		return  buf.toString();
	}

	/** sets the port */
	public void setPort(int port) {
		m_port = port;
	}

	/** clone this listening point */
	public Object clone()
	{
		try {
			ListeningPointImpl o = (ListeningPointImpl)super.clone();
			o.m_host = m_host;
			o.m_port = m_port;
			o.m_transport = m_transport;
			o.m_isSecure = m_isSecure;
			o.m_isReliable = m_isReliable;
			o.m_provider = m_provider;
			return o;
		}
		catch (CloneNotSupportedException e)
		{
			// Can't happen.
			// @PMD:REVIEWED:AvoidThrowingCertainExceptionTypesRule: by Amirk on 9/19/04 4:57 PM
			throw new Error("Clone not supported?");
		}
	}

	/**
	 * @return SipProviderImpl
	 */
	public SipProvider getProvider()
	{
		return m_provider;
	}

	/**
	 * sets the provider.
	 * @param provider The provider to set
	 */
	public void setProvider(SipProvider provider)
	{
		m_provider = provider;
	}
	
	/** compares with another listening point */
 	public boolean equals(Object obj)
	{
		boolean retVal;
		if ( obj == this) {
			retVal = true;
		}		
		else if(obj instanceof ListeningPoint) {
			ListeningPointImpl lp = (ListeningPointImpl)obj;
			retVal = lp.m_host.equals(m_host) &&
			         lp.m_port == m_port &&
			         lp.m_transport.equals( m_transport ) &&
			         lp.m_isSecure == m_isSecure;
		}
		else {
			retVal = false;
		}
		return retVal;
	}

 	/** calculates the hash code for this listening point */
	public int hashCode() {
		int hash = m_isSecure ? ~0 : 0;
		hash ^= m_host.hashCode();
		hash ^= m_port;
		hash ^= m_transport.hashCode();
		return hash;
	}
	
	
	/**
	 * @return true if tcp or tls, false for udp
	 */
	public boolean isReliable()
	{
		return m_isReliable;
	}

	/**
	 * @return true if configuration specified "0.0.0.0" for this listening point.
	 * in this case, getHost() returns the actual IP address or host name that
	 * this listening point is bound to (so that our Via contains a real
	 * address) and this flag indicates that the original configuration was
	 * "0.0.0.0".
	 */
	public boolean isAnyAddress() {
		return m_isAny;
	}
}
