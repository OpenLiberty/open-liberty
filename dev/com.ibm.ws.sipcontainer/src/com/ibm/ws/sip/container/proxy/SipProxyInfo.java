/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.proxy;

import jain.protocol.ip.sip.ListeningPoint;
import jain.protocol.ip.sip.SipProvider;
import jain.protocol.ip.sip.address.SipURL;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipURI;
import com.ibm.ws.sip.container.parser.SipAppDesc;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.SipContainer;
import com.ibm.ws.sip.container.servlets.SipServletMessageImpl;
import com.ibm.ws.sip.parser.util.InetAddressCache;
import com.ibm.ws.sip.stack.transaction.util.SIPStackUtil;
//TODO Liberty replace the fuctionality of this methos, as it's in  our HA component which is not supported in Liberty
//import com.ibm.ws.sip.hamanagment.util.SipClusterUtil;
import com.ibm.ws.sip.stack.transport.virtualhost.SipVirtualHostAdapter;

/**
 * This class is needed in order to implement MultiHome host support (part of JSR 289):
 * The SIP container should be able to accept from the SIP Proxy a list of outbound 
 * interfaces and expose it to any SIP application (i.e. Siplets) via new API defined in JSR 289.
 * SIP proxy will provide the SIP container a list of those interfaces.

 * When the SIP proxy starts, it sends a STARTUP SIP message (not standard , but we have it).
 * The SIP Proxy embedded outbound interface list into some special header of that STARTUP message. 
 * The SIP container will parse that special header and save the results in a (singleton) 
 * class for later use. And this is the class.
 * This class is initialized on SIP container startup and does not change later.
 * How will it be used?
 * The SIP container, upon future outgoing requests will include a new header 
 * ,[PreferedOutbound] or PO in short, with an integer (an index to the SIP Proxy interface list).
 * This is done to preserve memory footprint of the SIP Sessions and Requests.
 * The SIP proxy will parse that integer and will send the message out 
 * via the relevant outbound interface.
 * 
 * @author mordechai
 */
public class SipProxyInfo {

	/**
	 * this header will be found only in inter-connection messages between
	 * SIP proxy and SIP container. Currently expected to be found in the STARTUP SIP
	 * message.
	 */
	 public static final String PEREFERED_OUTBOUND_HDR_NAME = "IBM-PO";
	 
	 /**
	  * This header is used to pass the list of outbound interfaces from the proxy to the
	  * containers in the STARTUP message.
	  */
	 public static final String PROXY_OUTBOUND_HDR_NAME = "OutboundIfList";
	 
	 /**
	  * This header will be embedded in each Record-Route when the outgoing transport is different than the incoming transport.
	  */
	 public static final String IBM_DOUBLE_RECORD_ROUTE_PARAMETER = "ibmdrr";

	 /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SipProxyInfo.class);
	
    /**
     * singleton instance
     */
    private static final SipProxyInfo s_singleton = new SipProxyInfo();
    
    private List<SipURI> m_udpOutboundIfList = new ArrayList<SipURI>();
    private List<SipURI> m_tcpOutboundIfList = new ArrayList<SipURI>();
    private List<SipURI> m_tlsOutboundIfList = new ArrayList<SipURI>();
    private List<SipURI> m_fullOutboundIfList = null;
    
    private boolean	firstProxyStartupReceived = false;
    
    /**
     * helper variable to remember the default outbond interface.
     * The JSR says that even if the user hasn't used the multi-homed host
     * feature, some default must exists.
     */
    SipURI m_udpDefault = null;
    SipURI m_tcpDefault = null;
    SipURI m_tlsDefault = null;
    
    
    private SipProxyInfo() 
    {
    }
	
    public static SipProxyInfo getInstance()
    {
    	return s_singleton;
    }
    
    /**
     * Add new UDP interface
     * @param newUri
     */
    public synchronized void addUdpInterface(SipURI newUri){
    	m_udpOutboundIfList.add(newUri);
    	m_fullOutboundIfList = null;// will require a rebuild of this list
    }
    
    /**
     * Add new TCP interface
     * @param newUri
     */
    public synchronized void addTcpInterface(SipURI newUri){
    	m_tcpOutboundIfList.add(newUri);
    	m_fullOutboundIfList = null;// will require a rebuild of this list
    }
    
    /**
     * Add new TLS interface
     * @param newUri
     */
    public synchronized void addTlsInterface(SipURI newUri){
    	m_tlsOutboundIfList.add(newUri);
    	m_fullOutboundIfList = null;// will require a rebuild of this list
    }
    
    /**
     * 
     * @return - a list of outbound interfaces. List<Hop>
     * @throws UnknownHostException 
     */
    public synchronized List<SipURI> getOutboundInterfaceList()
    {
    	if(c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug("getOutboundInterfaceList");
		}
    	
    	if(m_fullOutboundIfList == null){
    		buildFullOutboundIfList(m_udpOutboundIfList, m_tcpOutboundIfList, m_tcpOutboundIfList);
    	}
    	return m_fullOutboundIfList;
    }
    
    /**
     * @return - a list of outbound interfaces. List<Hop>
     */
    public synchronized SipURI getOutboundInterface(int index, String transport)
    {
    	if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this, "getOutboundInterface", new Object[]{Integer.valueOf(index), transport});
		}
    	SipURI returnValue = null;
    	
    	if ((transport ==  null)&& (m_udpOutboundIfList != null))
    		returnValue = m_udpOutboundIfList.get(index);
    	else
    	{
	    	if (transport.equalsIgnoreCase("udp")&& (m_udpOutboundIfList != null))
	    		returnValue = m_udpOutboundIfList.get(index);
	    	else if (transport.equalsIgnoreCase("tcp") && (m_tcpOutboundIfList != null))
	        	returnValue = m_tcpOutboundIfList.get(index);
	    	else if (transport.equalsIgnoreCase("tls") && (m_tlsOutboundIfList != null))
	    		returnValue = m_tlsOutboundIfList.get(index);
    	}
    	if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(this, "getOutboundInterface", returnValue);
		}
        return (returnValue);
    }

    /**
     * 
     * @return - a list of outbound interfaces. List<Hop>
     */
    public synchronized int getNumberOfInterfaces(String transport)
    {
    	int returnValue = 0;
    	
        if (transport.equalsIgnoreCase("tcp") && (m_tcpOutboundIfList != null))
        	returnValue = m_tcpOutboundIfList.size();
    	else if (transport.equalsIgnoreCase("udp") && (m_udpOutboundIfList != null))
    		returnValue = m_udpOutboundIfList.size();
    	else if (transport.equalsIgnoreCase("tls") && (m_tlsOutboundIfList != null))
    		returnValue = m_tlsOutboundIfList.size();
        
        return (returnValue);
    }

    /**
     * JSR 289 says that every SIP Container should have a default outbound interface.
     * (even if the user hasn't activated the Multihome host support).
     * @param - can be : tls , tcp or udp . for each transport will try to find a default. 
     * @return - the first interface stored in the inner list.
     */
    public SipURI getDefaultOutboundIface(String transport) 
    {
    	SipURI returnValue = null;
    	if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this, "getDefaultOutboundIface", transport);
		}
    	if (transport.equalsIgnoreCase("tcp"))
    		returnValue = m_tcpDefault;
    	else if (transport.equalsIgnoreCase("udp"))
    		returnValue = m_udpDefault;
    	else if (transport.equalsIgnoreCase("tls"))
    		returnValue = m_tlsDefault;

    	if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(this, "getDefaultOutboundIface", returnValue);
		}
    	return returnValue;
    }
    
    /**
     * JSR 289 says that every SIP Container should have a default outbound interface.
     * (even if the user hasn't activated the Multihome host support).
     * @param - can be : tls , tcp or udp . for each transport will try to find a default. 
     * @return - the first interface stored in the inner list.
     */
    public int getDefaultOutboundIfaceIndex(String transport) 
    {
    	int returnValue = -1;
    	if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this, "getDefaultOutboundIface", transport);
		}
    	if (transport.equalsIgnoreCase("tcp"))
    		returnValue = getIndexOfIface(m_tcpDefault);
    	else if (transport.equalsIgnoreCase("udp"))
    		returnValue = getIndexOfIface(m_udpDefault);
    	else if (transport.equalsIgnoreCase("tls"))
    		returnValue = getIndexOfIface(m_tlsDefault);

    	if (returnValue == -1)
    	{
        	if(c_logger.isTraceDebugEnabled()){
    			c_logger.traceExit(this, "getDefaultOutboundIface: No default found");
    		}
    	}
    	
    	if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(this, "getDefaultOutboundIface", returnValue);
		}
    	return returnValue;
    }

    /**
     * Finds an outbound interface given the local transport, host, and port
     * @param transport the local transport protocol
     * @param host the local host IP address
     * @param port the local port number
     * @return the index of the matching interface, or -1 if there is no match.
     */
    public synchronized int getIndexOfIface(String transport, String host, int port) {
    	
    	if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this, "getIndexOfIface for transport = " + transport + ", " + host + ":" + port);
		}
    	
    	List<SipURI> outboundIfList;

    	if (transport.equalsIgnoreCase("udp")) {
    		outboundIfList = m_udpOutboundIfList;
    	}
    	else if (transport.equalsIgnoreCase("tcp")) {
    		outboundIfList = m_tcpOutboundIfList;
    	}
    	else if (transport.equalsIgnoreCase("tls")) {
    		outboundIfList = m_tlsOutboundIfList;
    	}
    	else {
    		outboundIfList = null;
    	}
    	if (outboundIfList == null) {
    		if(c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug("getIndexOfIface: outboundIfList is null");
			}	   
    		return -1;
    	}
    	int size = outboundIfList.size();
    	for (int i = 0; i < size; i++) {
    		SipURI iface = outboundIfList.get(i);
    		int ifacePort = iface.getPort();
    		if (ifacePort == port) {
	    		String ifaceHost = iface.getHost(); 
	    		String ifaceHostAddress = SIPStackUtil.getHostAddress(ifaceHost);
		    	if(c_logger.isTraceDebugEnabled()){
					c_logger.traceDebug("getIndexOfIface: comparing: " + host + " to host:" + ifaceHost + " and to host addr:" + ifaceHostAddress);
				}	    		
	    		if (ifaceHost.equalsIgnoreCase(host) || ifaceHostAddress.equalsIgnoreCase(host)) {
	    			if(c_logger.isTraceDebugEnabled()){
	    	    		c_logger.traceExit(this, "getIndexOfIface", i);
	    	    	}
			    	return i;
	    		}
    		}
    		else {
    			if(c_logger.isTraceDebugEnabled()){
					c_logger.traceDebug("getIndexOfIface: port wasn't found");
				}	
    		}
    	}
    	
    	if(c_logger.isTraceDebugEnabled()){
    		c_logger.traceExit(this, "getIndexOfIface: No index found");
    	}
    	return -1;
    }

    /**
     * Will search if the uri has some matching in the inner lists of this class.
     * @param uri
     * @return - if found a match , will return the index in the list of that match.
     * will return negative (-1) numebr if nothing was found.
     */
    public synchronized int getIndexOfIface(InetSocketAddress socketAddress, String transport)
    {
    	if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this, "getIndexOfIface", socketAddress + ":" + transport);
		}
    	int 	result = -1;
    	List	outboundIfList = null;
    	
    	if (transport.equalsIgnoreCase("udp") == true)
    		outboundIfList = m_udpOutboundIfList;
    	else if (transport.equalsIgnoreCase("tcp") == true)
    		outboundIfList = m_tcpOutboundIfList;
    	else if (transport.equalsIgnoreCase("tls") == true)
    		outboundIfList = m_tlsOutboundIfList;
    	
    	if (outboundIfList != null)
    	{
	    	final int size = outboundIfList.size();
	    	for (int i = 0; i < size ; i++) {
	    		SipURI current = (SipURI)outboundIfList.get(i);
	    		
		    	if(c_logger.isTraceDebugEnabled()){
					c_logger.traceDebug("getIndexOfIface: comparing: " + current.getHost() + " to host:" + socketAddress.getHostName() + " and to host addr:" + InetAddressCache.getHostAddress(socketAddress.getAddress()));
				}

		    	if (((current.getHost().equalsIgnoreCase(socketAddress.getHostName()) == true) || 
	    			(current.getHost().equals(InetAddressCache.getHostAddress(socketAddress.getAddress())) == true)) &&
	    			current.getPort() == socketAddress.getPort())
	    		{
			    	result = i;
					break; 
	    		}
	    	}
    	}
    	else {
    		if(c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug("getIndexOfIface: outboundIfList is null");
			}
    	}
    	      	    	
    	if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(this, "getIndexOfIface",result);
		}
    	return result;
    }

    /**
     * Will search if the uri has some matching in the inner lists of this class.
     * @param uri
     * @return - if found a match , will return the index in the list of that match.
     * will return negative (-1) numebr if nothing was found.
     */
    public synchronized int getIndexOfIface(InetAddress address, String transport)
    {
    	if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this, "getIndexOfIface", address);
		}
    	int 	result = -1;
    	List	outboundIfList = null;
    	
    	if (transport.equalsIgnoreCase("udp") == true)
    		outboundIfList = m_udpOutboundIfList;
    	else if (transport.equalsIgnoreCase("tcp") == true)
    		outboundIfList = m_tcpOutboundIfList;
    	else if (transport.equalsIgnoreCase("tls") == true)
    		outboundIfList = m_tlsOutboundIfList;
    	
    	if (outboundIfList != null)
    	{
	    	final int size = outboundIfList.size();
	    	if(c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug("getIndexOfIface: searching for address:"+ address + " list size:" + size);
			}
	    	
	    	for (int i = 0; i < size ; i++) {
	    		SipURI current = (SipURI)outboundIfList.get(i);
	    		
		    	if(c_logger.isTraceDebugEnabled()){
					c_logger.traceDebug("getIndexOfIface: comparing: " + current.getHost() + " to host:" + address.getHostName() + " and to host addr:" + InetAddressCache.getHostAddress(address));
				}

		    	if ((current.getHost().equals(InetAddressCache.getHostAddress(address)) == true) ||
		    		(current.getHost().equals(address.getHostName()) == true))
	    		{
			    	result = i;
					break; 
	    		}
	    	}
    	}
    	else {
    		if(c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug("getIndexOfIface: outboundIfList is null");
			}
    	}
            	
    	if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(this, "getIndexOfIface",result);
		}
    	return result;
    }
    
    /**
     * Will search if the uri has some matching in the inner lists of this class.
     * @param uri
     * @return - if found a match , will return the index in the list of that match.
     * will return negative (-1) numebr if nothing was found.
     */
    public synchronized int getIndexOfIface(SipURI uri)
    {
    	if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this, "getIndexOfIface",uri);
		}
    	int 	result = -1;
    	List	outboundIfList = null;
    	
    	//	If a transport is not configured this uri may be null.
    	if (uri != null)
    	{
	    	if (uri.getTransportParam().equalsIgnoreCase("udp") == true)
	    		outboundIfList = m_udpOutboundIfList;
	    	else if (uri.getTransportParam().equalsIgnoreCase("tcp") == true)
	    		outboundIfList = m_tcpOutboundIfList;
	    	else if (uri.getTransportParam().equalsIgnoreCase("tls") == true)
	    		outboundIfList = m_tlsOutboundIfList;
	    	
	    	if (outboundIfList != null)
	    	{
		    	final int size = outboundIfList.size();
		    	if(c_logger.isTraceDebugEnabled()){
					c_logger.traceDebug("getIndexOfIface: searching for uri:"+uri+ " list size:"+size);
				}
		    	for (int i = 0; i < size ; i++) {
		    		SipURI current = (SipURI)outboundIfList.get(i);
		    		if (current.equals(uri)) {
		    			    	result = i;
		    					break; 
		    		}
		    	}
	    	}
	    	else {
	    		if(c_logger.isTraceDebugEnabled()){
					c_logger.traceDebug("getIndexOfIface: outboundIfList is null");
				}
	    	}
		}
         	    	
    	if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(this, "getIndexOfIface",result);
		}
    	return result;
    }
    
    /**
     * 
     */
    public synchronized int getIndexOfIface(SipURL uri)
    {
    	if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this, "getIndexOfIface",uri);
		}
    	int 	result = -1;
    	List	outboundIfList = null;
    	
    	if (uri.getTransport().equalsIgnoreCase("udp") == true)
    		outboundIfList = m_udpOutboundIfList;
    	else if (uri.getTransport().equalsIgnoreCase("tcp") == true)
    		outboundIfList = m_tcpOutboundIfList;
    	else if (uri.getTransport().equalsIgnoreCase("tls") == true)
    		outboundIfList = m_tlsOutboundIfList;
    	
    	if (outboundIfList != null)
    	{
	    	final int size = outboundIfList.size();
	    	if(c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug("getIndexOfIface: searching for uri:"+uri+ " list size:"+size);
			}

	    	String host = uri.getHost();
	    	int port = uri.getPort();
	    	
	    	for (int i = 0; i < size ; i++) {
	    		SipURI current = (SipURI)outboundIfList.get(i);
	    		if (current.getHost().equals(host) &&
		    		(current.getPort() == port)){
	    			    	result = i;
	    					break; 
	    		}
	    	}
    	}
    	else {
    		if(c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug("getIndexOfIface: outboundIfList is null");
			}
    	}
   	
    	if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(this, "getIndexOfIface",result);
		}
    	return result;
    }
    
    /**
     * A utility method to embedd the new header discussed in this class documentation.
     * @param request
     * @param indexOfInterface
     */
    synchronized public void addPreferedOutboundHeader(SipServletRequest request, int indexOfInterface)
    {
    	if(c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug("addPreferedOutboundHeader: indexOfInterface = " + indexOfInterface);
		}
    	
    	//	This code will replace a PO header if one already exist. This is important for 
    	//	application composition so that the last app in the chain can set the correct PO.
    	if (indexOfInterface >= 0) {
    		request.setHeader(PEREFERED_OUTBOUND_HDR_NAME, Integer.toString(indexOfInterface));
    	}
    }
    
    /**
     * cleans the current interfaces list.
     */
    public synchronized void resetList()
    {
    	if (m_udpOutboundIfList != null)
    		m_udpOutboundIfList.clear();
    	
    	if (m_tcpOutboundIfList != null)
    		m_tcpOutboundIfList.clear();
    	
    	if (m_tlsOutboundIfList != null)
    		m_tlsOutboundIfList.clear();
    	
    	m_udpDefault = null;
    	m_tcpDefault = null;
    	m_tlsDefault = null;
    }
    
    /**
     * tries to set a default value to one of the tcp/udp/tls members.
     * @param newUri
     */
	private void setNewDefaultInterfaces(SipURI newUri) {
    	if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this, "setNewDefaultInterfaces");
		}
		String currentTransport = newUri.getTransportParam();
		if (currentTransport.equalsIgnoreCase("tcp")) {
			m_tcpDefault = newUri;
			return;
		}

		if (currentTransport.equalsIgnoreCase("tls")) {
			m_tlsDefault = newUri;
			return;
		}

		if (currentTransport.equalsIgnoreCase("udp")) {
			m_udpDefault = newUri;
			return;
		}
    	if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(this, "setNewDefaultInterfaces");
		}
	}
	
	/*
	 * 
	 */
	private void buildFullOutboundIfList (	List<SipURI> udpOutboundIfList, 
											List<SipURI> tcpOutboundIfList, 
											List<SipURI> tlsOutboundIfList)
	{
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this, "buildFullOutboundIfList", Arrays.toString(udpOutboundIfList.toArray()), 
					Arrays.toString(tcpOutboundIfList.toArray()), Arrays.toString(tlsOutboundIfList.toArray()));
		}
		m_fullOutboundIfList = new ArrayList<SipURI>();

		if (udpOutboundIfList != null)
			m_fullOutboundIfList.addAll(udpOutboundIfList);

		if (tcpOutboundIfList != null)
			m_fullOutboundIfList.addAll(tcpOutboundIfList);
			
		if (tlsOutboundIfList != null)
			m_fullOutboundIfList.addAll(tlsOutboundIfList);
		
		LinkedList<SipAppDesc> apps = SipContainer.getInstance().getRouter().getAllApps();
		if(apps != null){
		for (SipAppDesc app : apps) {
			app.getServletContext().setAttribute(SipServlet.OUTBOUND_INTERFACES, m_fullOutboundIfList);
    	}
		}
		
		//Will not publish interfaces that are part of the SIP Connector to a SipServlets application
		for(Iterator<SipURI> itr = m_fullOutboundIfList.iterator(); itr.hasNext();){
			if(c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug("buildFullOutboundIfList: removing SIP connector interfaces, if any exists");
			}	
			SipURI uri = itr.next();
			
			try {
				if(SipVirtualHostAdapter.isSipUriAConnectorInterface(uri)){
					itr.remove();
				}
			} catch (UnknownHostException e) {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(null, "buildFullOutboundIfList", "e.getLocalizedMessage()="
							+ e.getLocalizedMessage() + ", uri=" + uri);
				}
				com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sip.container.proxy.SipVirtualHostAdapter", "1", this);
			}
		}
		
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(this, "buildFullOutboundIfList", Arrays.toString(m_fullOutboundIfList.toArray()));
		}
	}

	/**
	 * Determines what interface the original request was received on. 
	 * 
	 * @param req the SIP request
	 * 
	 * @return the interface that the original request was received on,
	 * if none is found - returns the default interface according to the transport.
	 */
	public synchronized SipURI extractReceivedOnInterface(SipServletRequest req)	{
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this, "extractReceivedOnInterface", req);
		}
		
		List<SipURI> outboundIfList = null;
		SipURI returnValue = null;
    	
    	if (req == null){
    		return null;
    	}

    	SipURI defaultInterface = null;
		String transport = req.getTransport();
		String host = req.getLocalAddr();
		
		boolean standalone = true;/*TODO Liberty SipContainer.isRunningInWAS()
			? !SipClusterUtil.isServerInCluster()
			: true;*/
		if (standalone) {
			// a standalone deployment might have set the custom property
			// com.ibm.ws.sip.sent.by.host, in which case the outbound interface
			// is set to a custom host name rather than the physical IP address.
			// in such case, need to compare against the custom host name.
			SipServletMessageImpl msgImpl = (SipServletMessageImpl)req;
			SipProvider provider = msgImpl.getSipProvider();
			if (provider != null) {
				ListeningPoint listeningPoint = provider.getListeningPoint();
				host = listeningPoint.getSentBy();
			}
		}
		
    	//	First find the correct list to search from
		if (transport != null){
			if (transport.equalsIgnoreCase("tcp")){
				outboundIfList = m_tcpOutboundIfList;
				defaultInterface = m_tcpDefault;
			} else if (transport.equalsIgnoreCase("udp")){
				outboundIfList = m_udpOutboundIfList;
				defaultInterface = m_udpDefault;
			} else if (transport.equalsIgnoreCase("tls")){
				outboundIfList = m_tlsOutboundIfList;
				defaultInterface = m_tlsDefault;
			}
			
			if (outboundIfList != null) {
				Iterator iterator = outboundIfList.iterator();
	    		String hostAddress = SIPStackUtil.getHostAddress(host);
				
				while (iterator.hasNext()) {
					SipURI interfaceURI = (SipURI)iterator.next();
					String ifaceHost = interfaceURI.getHost();
					String ifaceHostAddress = SIPStackUtil.getHostAddress(ifaceHost);
		    		if(c_logger.isTraceDebugEnabled()){
						c_logger.traceDebug("extractReceivedOnInterface: comparing <" + ifaceHostAddress + "> to <" + hostAddress + ">, host = " + host);
					}	
					if (ifaceHostAddress.equalsIgnoreCase(hostAddress) && 
							interfaceURI.getPort() == req.getLocalPort()) {
						returnValue = interfaceURI;
						break;
					}
				}
			}
			
			if (returnValue == null) {
				returnValue = defaultInterface;
				if(c_logger.isTraceDebugEnabled()){
					c_logger.traceDebug("extractReceivedOnInterface: Returning default value since received on interface was NOT found for: " + req.toString());
				}
			}
		} else {
			if(c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug("extractReceivedOnInterface: Transport is null. Can't extract interface for: " + req.toString());
			}
		}
		
    	if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(this, "extractReceivedOnInterface", returnValue);
		}

    	return returnValue;
	}
}
