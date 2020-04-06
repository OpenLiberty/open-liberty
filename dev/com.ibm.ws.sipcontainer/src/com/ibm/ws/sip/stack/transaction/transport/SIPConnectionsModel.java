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
package com.ibm.ws.sip.stack.transaction.transport;

import jain.protocol.ip.sip.ListeningPoint;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jain.protocol.ip.sip.ListeningPointImpl;
import com.ibm.ws.sip.parser.util.InetAddressCache;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnectionFactory;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPListenningConnection;
import com.ibm.ws.sip.stack.transport.sip.SIPConnectionFactoryImplWs;

public class SIPConnectionsModel 
 {

	/**
	 * Class Logger. 
	 */
	protected static final TraceComponent tc = Tr.register(SIPConnectionsModel.class);
	private static final LogMgr c_logger = Log.get(SIPConnectionsModel.class);
	
	/** 
	 * listening connections 
	 **/
	private HashMap<ListeningPoint, SIPListenningConnection> m_listeningConnections = new HashMap<ListeningPoint, SIPListenningConnection>(4);
	
	
	/** 
	 * list of listening points 
	 **/
	private ArrayList<ListeningPoint> m_listeningPoints = new ArrayList<ListeningPoint>(4);
	
	/**
	 * map of connections. each entry in this map is a listening point,
	 * and each listening point contains a map of connections create to
	 * or from that listening point. 
	 */
	private final HashMap<ListeningPoint, HashMap<Hop, SIPConnection>> m_connections =
		new HashMap<ListeningPoint, HashMap<Hop, SIPConnection>>(4);
	
	/**
	 * entry in static table of transports supported by this stack implementation
	 */
	private static class SupportedTransport {
		private final String m_transport;
		private final SIPConnectionFactory m_connectionFactory;
		private final boolean m_secure;
		
		/** constructor */
		SupportedTransport(String transport, SIPConnectionFactory connectionFactory, boolean secure) {
			m_transport = transport;
			m_connectionFactory = connectionFactory;
			m_secure = secure;
		}
	}
	
	/** table of transports supported by this stack implementation */
	private ArrayList<SupportedTransport> m_supportedTransports;

	
	/** singleton instance */
	private static SIPConnectionsModel s_instance = new SIPConnectionsModel();
	
	/**
	 * @return the singleton instance
	 */
	public static SIPConnectionsModel instance() {
		return s_instance;
	}
	
	/**
	 * private constructor
	 */
	private SIPConnectionsModel() {
		initSupportedTransports();
	}
	
	/**
	 * initialize static table of supported transports
	 */
	private final void initSupportedTransports() {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(
				this,tc,
				"initSupportedTransports","useChannelFramework");
		}
		SIPConnectionFactory factory = SIPConnectionFactoryImplWs.instance();
		
		// dynamically load SIPConnectionFactoryImplWs from component sip.stack.ws
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(
				this,tc,
				"initSupportedTransports",
				"loading connection factory class");
		}
		
		SIPConnectionFactory udpConnectionFactory;
		SIPConnectionFactory tcpConnectionFactory;
		SIPConnectionFactory tlsConnectionFactory;
		
		//TODO Liberty remove non CF stack functionality
//		if (factory == null) {
//			// use plain sockets
//			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//				Tr.debug(
//					this,tc,
//					"initSupportedTransports",
//					"connection factory uses plain sockets");
//			}
//			udpConnectionFactory = new com.ibm.ws.sip.stack.transaction.transport.connections.udp.SIPConnectionFactoryImpl();
//			tcpConnectionFactory = new com.ibm.ws.sip.stack.transaction.transport.connections.tcp.SIPConnectionFactoryImpl();
//			tlsConnectionFactory = new com.ibm.ws.sip.stack.transaction.transport.connections.tls.SIPConnectionFactoryImpl();
//		}
//		else {
			// use channel framework
			udpConnectionFactory = factory;
			tcpConnectionFactory = factory;
			tlsConnectionFactory = factory;
//		}
		
		SupportedTransport udp = new SupportedTransport("udp", udpConnectionFactory, false);
		SupportedTransport tcp = new SupportedTransport("tcp", tcpConnectionFactory, false);
		SupportedTransport tls = new SupportedTransport("tls", tlsConnectionFactory, true);
		
		m_supportedTransports = new ArrayList<SupportedTransport>(3);
		m_supportedTransports.add(udp);
		m_supportedTransports.add(tcp);
		m_supportedTransports.add(tls);
	}
	
	/**
	 * Adding a new SIP listening point and create a new listening connection
	 */
	public void addedListeningPoint(ListeningPoint lp) throws IOException {
		createSIPListenningConnection((ListeningPointImpl)lp);
	}
	
	/**
	 * get default listening point for transport type
	 * 
	 * @param transport
	 * @return
	 */
	public ListeningPointImpl getDefaultListenningPoint( String transport )
	{
		ListeningPoint retVal = null;
		Set<ListeningPoint> keys =  m_listeningConnections.keySet();
		for (Iterator<ListeningPoint> iter = keys.iterator(); iter.hasNext(); ) 
		{
        	ListeningPoint lp = iter.next();
        	if( lp.getTransport().equalsIgnoreCase(transport))
        	{
				retVal = lp;
				break;
        	}
        	
        }
        return (ListeningPointImpl)retVal;
	}
	
	/**
	 * gets, and creates if does not exist, the table of connections that are
	 * created to, or accepted from the given listening point.
	 * @param listeningPoint the local listening point
	 * @return the table of connections for the given listening point
	 */
	private synchronized HashMap<Hop, SIPConnection> getConnections(
		ListeningPoint listeningPoint)
	{
		HashMap<Hop, SIPConnection> connections = m_connections.get(listeningPoint);
		if (connections == null) {
			connections = new HashMap<Hop, SIPConnection>(16);
			m_connections.put(listeningPoint, connections);
		}
		return connections;
	}

	/**
	 * finds a connection that was previously created or accepted,
	 * that connects between the local address (that matches the given
	 * listening point) and the remote address (that matches the given hop) 
	 * @param listeningPoint the local listening point. if this is null, a
	 *  connection is returned for any local listening point.
	 * @param hop the remote hop
	 * @return the matching connection, null if no such connection
	 */
	public synchronized SIPConnection getConnection(
		ListeningPoint listeningPoint, Hop hop)
	{
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this,tc,"getConnection",
				"connection listeningPoint = " + listeningPoint + " hop = " + hop);
		}
		SIPConnection connection = null;
		if (listeningPoint == null) {
			// if listening point not specified, try all known
			// listening points, and recurse (one level) into each.
			int n = m_listeningPoints.size();
			for (int i = 0; i < n; i++) {
				listeningPoint = m_listeningPoints.get(i);
				connection = getConnection(listeningPoint, hop);
				if (connection != null) {
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
						Tr.debug(this,tc,"getConnection", "Found Connection = " + connection);
					}
					return connection;
				}
			}
		}
		else {
			HashMap<Hop, SIPConnection> connections = getConnections(listeningPoint);
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this,tc,"getConnection",
					"connections = " + connections.toString());
			}
			connection = connections.get(hop);
		}
		return connection;
	}	

	/**
	 * add connection
	 * @param connection - the connection to add
	 */
	public synchronized void addConnection(SIPConnection connection) {
		SIPListenningConnection listening = connection.getSIPListenningConnection();
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this,tc,"addConnection",
				"listening " + listening);
		}
		ListeningPoint listeningPoint = listening.getListeningPoint();
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this,tc,"addConnection", "listeningPoint " + listeningPoint);
		}
		HashMap<Hop, SIPConnection> connections = getConnections(listeningPoint);
		SIPConnection removed = connections.put(connection.getKey(), connection);
		if (removed != null && removed != connection) {
			// never close inbound connections. only the peer does that.
			if (removed.isOutbound()) {
				// removed an old outbound connection. close it to prevent a leak.
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(this,tc,"addConnection",
						"closing replaced connection " + removed);
				}
				removed.close();
			}
			logConnection(removed, true);
		}
		logConnection(connection, false);
	}
	
	
	/**
	 * this is used when supporting connection reuse - update the connection with
	 * the new port key
	 * @param connection - the connection that is set
	 * @param aliasPort - the new port to associate with this connection 
	 */
	public synchronized void updateConnection(SIPConnection connection, int aliasPort) {
		SIPListenningConnection listening = connection.getSIPListenningConnection();
		ListeningPoint listeningPoint = listening.getListeningPoint();
		HashMap<Hop, SIPConnection> connections = getConnections(listeningPoint);
		Hop key = connection.getKey();
		connections.remove(key);
		connection.setAliacePort(aliasPort);
		key.setPort(aliasPort);
		connection.setKey(key);
		SIPConnection removed = connections.put(key, connection);
		if (removed != null && removed != connection) {
			// never close inbound connections. only the peer does that.
			if (removed.isOutbound()) {
				// also, never close a loopback connection, because that
				// would trigger closure of the inbound socket as well.
				ListeningPoint lp = removed.getSIPListenningConnection().getListeningPoint();
				String lpHost = lp.getHost();
				int lpPort = lp.getPort();
				String remoteHost = removed.getRemoteHost();
				int remotePort = removed.getRemotePort();

				if (!lpHost.equals(remoteHost) || lpPort != remotePort) {
					// removed an old outbound connection. close it to prevent a leak.
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
						Tr.debug(this,tc,"updateConnection",
							"closing replaced connection " + removed);
					}
					removed.close();
				}
			}
			logConnection(removed, true);
		}
		logConnection(connection, false);
	}
	
	public void removeConnection(SIPConnection connection) {
		synchronized (this) {
			SIPListenningConnection listening = connection.getSIPListenningConnection();
			ListeningPoint listeningPoint = listening.getListeningPoint();
			HashMap<Hop, SIPConnection> connections = getConnections(listeningPoint);
	
			// before attempting to remove this connection, make sure we are
			// removing the correct one
			Hop key = connection.getKey();
			if (connections.get(key) == connection) {
				connections.remove(key);		
				logConnection(connection, true);
			}
			else {
				// the map contains a different connection instance for this key.
				// don't remove it. this can happen when calling updateConnection()
				// to replace an old connection with a new one, and both have the
				// same key. updateConnection() already removed the old instance.
				// if we removed it again, the new one would get deleted too.
			}
		}
		connection.close();
	}
	
	public synchronized SIPConnection createConnection(  ListeningPoint lp , String remoteHost , int remotePort )
		throws IOException
	{
		// verify listening connection is different than target
		if (lp.getHost().equals(remoteHost) && lp.getPort() == remotePort) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc, "Warning: loopback connection detected");
			}
		}
		SIPConnection retVal;
		SIPListenningConnection listenningConnection = getListeningConnection(lp);
		if( listenningConnection==null )
		{
				throw new IOException("no listenning connection found to listenning point " + lp );
		}		
		InetAddress remoteAddress = InetAddressCache.getByName(remoteHost);	
		retVal = listenningConnection.createConnection(remoteAddress, remotePort);
		return retVal;
	}
	
	/**
	 * finds a local listening connection given a listening point
	 * @param lp a listening point to find
	 * @return a matching listening connection, or null if not found
	 */
	private SIPListenningConnection getListeningConnection(ListeningPoint lp) {
		ListeningPointImpl lpA = (ListeningPointImpl)lp;
		String transportA = lpA.getTransport();
		String hostA = lpA.getHost();
		String sentByA = lpA.getSentBy();
		int portA = lpA.getPort();
		boolean secureA = lpA.isSecure();

		Set<?> entries = m_listeningConnections.entrySet();
		Iterator<?> i = entries.iterator();

		while (i.hasNext()) {
			Map.Entry<?,?> entry = (Map.Entry<?,?>)i.next();
        	ListeningPointImpl lpB = (ListeningPointImpl)entry.getKey();
    		String transportB = lpB.getTransport();
    		String hostB = lpB.getHost();
    		String sentByB = lpB.getSentBy();
    		int portB = lpB.getPort();
    		boolean secureB = lpB.isSecure();

    		if (secureA == secureB &&
    			portA == portB &&
    			transportA.equals(transportB))
    		{
    			// so far so good. now compare the host.
    			// if the hosts are not equal, try comparing the sent-by.
    			if (hostA.equals(hostB) ||
    				sentByA.equalsIgnoreCase(sentByB))
    			{
    				return (SIPListenningConnection)entry.getValue();
    			}
        	}
        	
        }
		return null;
	}
			
	public synchronized void removeListeningConnectionClosed(SIPListenningConnection	listenning )
	{
		listenning.stopListen();
		ListeningPoint lp = ( ListeningPoint )listenning.getListeningPoint();
		m_listeningConnections.remove( lp );
		m_listeningPoints.remove( lp );		
	}
	

	public List<ListeningPoint> getListeningPoints()
	{	
		return m_listeningPoints; 
	}
	
	
	public synchronized Collection<SIPListenningConnection> getListeningConnections()
	{	
		return m_listeningConnections.values(); 
	}

	synchronized SIPListenningConnection createSIPListenningConnection( ListeningPointImpl lp ) throws IOException
	{
		String transportFactoryKey = lp.getTransport().equals("tcp") && lp.isSecure() ? "tls" : lp.getTransport();
		SIPConnectionFactory factory = getConnectionFactory(transportFactoryKey);

		SIPListenningConnection listenConnection = factory.createListeningConnection( lp );
		if( listenConnection!=null)
		{
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this,tc,"createSIPListenningConnection","connection " + listenConnection + " created,trying to listen");
			}
			
			listenConnection.listen();
			
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this,tc,"createSIPListenningConnection","connection " + listenConnection + " started to listen");
			}
			m_listeningConnections.put( lp , listenConnection );
		
			//the listenning point might be already inside!!!
			//add it only if it's not
			if( !m_listeningPoints.contains( lp ) )
			{
				m_listeningPoints.add( lp );
			}
		
			Object[] params = {lp};
			if( c_logger.isInfoEnabled())
			{
			c_logger.info("info.com.ibm.ws.sip.stack.transaction.transport.initListeningPoints",
					  	Situation.SITUATION_START_INITIATED,
					  	params);
			}
		}
		else
		{			
			throw new IOException("could not instansiate Listenning connection!");
		}			  
		return 	listenConnection;			
	}
	
	synchronized void removeSIPListenningConnection( ListeningPointImpl lp )
	{
		SIPListenningConnection listenConnection = (SIPListenningConnection)m_listeningConnections.remove( lp );
		if( listenConnection!=null )
		{
			listenConnection.stopListen();
		}
	}
	
    /**
     * gets the supported transport instance given transport string
     * @param transport transport string
     * @return the corresponding supported transport instance,
     *  null if given transport is not supported
     */
    private SupportedTransport getSupportedTransport(String transport) {
    	for (int i = 0; i < m_supportedTransports.size(); i++) {
    		SupportedTransport t = (SupportedTransport)m_supportedTransports.get(i);
    		if (t.m_transport.equalsIgnoreCase(transport)) {
    			return t;
    		}
    	}
    	return null;
    }
    
    /**
     * determines if given transport is supported by this
     * stack implementation
     * 
     * @param transport transport to test, may be null
     * @return true if supported
     */
    public boolean isTransportSupported(String transport) {
    	return getSupportedTransport(transport) != null;
    }
    
    /**
     * given a transport, finds the corresponding connection factory
     * @param transport given transport, may be null
     * @return the corresponding connection factory,
     *  null if transport not supported
     */
    private SIPConnectionFactory getConnectionFactory(String transport) {
    	SupportedTransport t = getSupportedTransport(transport);
    	if (t == null) {
    		return null;
    	}
    	return t.m_connectionFactory;
    }

    /**
     * determines if given transport is secure
     * @param transport transport to test, may be null
     * @return true if secure, false if not secure or if not supported
     */
    public boolean isTransportSecure(String transport) {
    	SupportedTransport t = getSupportedTransport(transport);
    	if (t == null) {
    		return false;
    	}
    	return t.m_secure;
    }
    
    /**
     * returns the default transport to be used for sips: message
     * @return tls
     */
    public String getDefaultSecureTransport() {
    	return ListeningPointImpl.TRANSPORT_TLS;
    }
    
    /**
     * returns the default port to be used for sips: message
     * @return 5061
     */
    public int getDefaultSecurePort() {
    	return ListeningPointImpl.DEFAULT_SECURE_PORT;
    }

    /**
     * finds a listening point given the host port and transport
     * @param host local address
     * @param port local port number
     * @param transport transport protocol
     * @return a matching listening point, null if no match
     */
    public synchronized ListeningPointImpl getListeningPoint(
    	String host, int port, String transport)
    {
    	int n = m_listeningPoints.size();
    	for (int i = 0; i < n; i++) {
    		ListeningPointImpl listeningPoint = (ListeningPointImpl)m_listeningPoints.get(i);
    		String listeningPointHost = listeningPoint.getHost();
    		int listeningPointPort = listeningPoint.getPort();
    		String listeningPointTransport = listeningPoint.getTransport();
    		if (listeningPointPort == port &&
    			listeningPointHost.equalsIgnoreCase(host) &&
    			listeningPointTransport.equalsIgnoreCase(transport))
    		{
    			return listeningPoint;
    		}
    	}
    	return null;
    }
	
	/**
	 * gets the listening point given the listening point index.
	 * @param index the index, as assigned by SipProxyInfo.setLocalOutboundInterface
	 * @param transport the desired listening point transport
	 * @return the matching listening point, or null if no such.
	 * @see com.ibm.ws.sip.container.proxy.SipProxyInfo
	 */
	public ListeningPointImpl getListeningPoint(int index, String transport) {
    	int n = m_listeningPoints.size();
    	int counter = 0;

    	if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this,tc,"getListeningPoint",
				"listeningPoints = " + m_listeningPoints);
		}
    	for (int i = 0; i < n; i++) {
    		ListeningPointImpl listeningPoint = (ListeningPointImpl)m_listeningPoints.get(i);
    		String listeningPointTransport = listeningPoint.getTransport();
    		if (!listeningPointTransport.equalsIgnoreCase(transport)) {
    			continue;
    		}
    		// counter is only incremented for matching transport
    		if (counter++ == index) {
    			return listeningPoint;
    		}
    	}
    	return null;
	}

	/**
	 * called whenever connection state changes to log the new state
	 * @param connection the modified connection
	 * @param remove true if removed, false if added or updated
	 */
	private void logConnection(SIPConnection connection, boolean remove) {
		SIPListenningConnection listening = connection.getSIPListenningConnection();
		ListeningPoint listeningPoint = listening.getListeningPoint();
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this,tc,"logConnection",
				"[" + (remove ? "removed" : "changed")
				+ "] connection [" + System.identityHashCode(connection)	
				+ "] listening point [" + listeningPoint
				+ "] key [" + connection.getKey()
				+ "] remote [" + connection.getRemoteHost()
				+ ':' + connection.getRemotePort() + ']');
		}
	}
}
