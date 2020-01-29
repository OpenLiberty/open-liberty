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
package com.ibm.ws.sip.stack.transaction.transport.routers;

import jain.protocol.ip.sip.ListeningPoint;
import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.address.URI;
import jain.protocol.ip.sip.header.NameAddressHeader;
import jain.protocol.ip.sip.header.RouteHeader;
import jain.protocol.ip.sip.header.ViaHeader;
import jain.protocol.ip.sip.message.Message;
import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

import java.util.HashSet;
import java.util.Iterator;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.header.GenericNameAddressHeaderImpl;
import com.ibm.ws.sip.stack.transaction.transport.Hop;
import com.ibm.ws.sip.stack.transaction.transport.SIPConnectionsModel;
import com.ibm.ws.sip.stack.util.SipStackUtil;

/**
 * a router that is aware of SLSPs.
 * at a given point in time, it operates in one of two modes:
 * 1. standalone mode.
 *    in this mode, a request is routed either to the OUTBOUND_PROXY,
 *    or based on the message contents if no OUTBOUND_PROXY defined.
 * 2. slsp mode.
 *    in this mode, a request is routed to one of the slsps
 *
 * the router starts in standalone mode.
 * it switches to proxy mode on the first successful call to addSLSP().
 * from this point it can never revert back to standalone mode.
 * 
 * if a static OUTBOUND_PROXY is defined in configuration,
 * any attempt to addSLSP() will fail.  
 * 
 * multiple requests to the same UAS may traverse different SLSPs
 * (no affinity)
 * 
 * @author Amirk
 */
public class SLSPRouter
{
	/** class Logger */
	private static final LogMgr c_logger = Log.get(SLSPRouter.class);

	/**
	 * singleton instance to the SLSP router.
	 * null in case a different type of router is specified in configuration
	 */
	private static SLSPRouter s_instance = new SLSPRouter();
	
	/** static outbound proxy from configuration. may be null */
	private Hop m_outboundProxy;

	/** dynamic list of known SLSPs. as long as this is null, we are in standalone mode */
	private HashSet m_slsps;

	/** iterator to the next SLSP in round-robin */
	private Iterator m_roundRobin;

	/**
	 * @return singleton instance to the SLSP router.
	 *  null in case a different type of router is specified in configuration.
	 */
	public static SLSPRouter getInstance() {
		return s_instance;
	}

	/**
	 * constructor
	 */
	protected SLSPRouter() {
		m_outboundProxy = null;
		m_slsps = null;
		m_roundRobin = null;
		
		if (s_instance != null && s_instance != this) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug("Warning: SLSPRouter instantiated again");
			}
		}
		s_instance = this;
	}

	/**
	 * called when a new request comes in
	 * 
	 * @param request the new incoming request
	 * @throws SipParseException
	 */
	public void processRequest(Request request) throws SipParseException {
		if (m_slsps == null) {
			return;
		}
		
		// slsp mode
		ViaHeader topVia = (ViaHeader)request.getHeader(ViaHeader.name, true);
		Hop slsp = new Hop(topVia);

		boolean known;
		synchronized (this) {
			known = m_slsps.contains(slsp);
		}
		if (!known) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug("Warning: incoming request from unknown SLSP ["
					+ slsp.toString() + ']');
			}
		}
	}

	/**
	 * called when a connection to an SLSP was closed
	 * 
	 * @param connectionHop SLSP connection that was closed
	 */
	public synchronized void removeConnectionHop(Hop connectionHop) {
		if (m_slsps == null) {
			return;
		}

		if (m_slsps.remove(connectionHop)) {
			resetRoundRobin();
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug("removed SLSP ["
					+ connectionHop.toString() + ']');
			}
		}
		else {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug("Warning: attempt to remove unknown SLSP ["
					+ connectionHop.toString() + ']');
			}
		}
	}

	/**
	 * @return the static outbound proxy
	 * @see com.ibm.ws.sip.stack.transaction.transport.routers.Router#getOutboundProxy()
	 */
	public Hop getOutboundProxy() {
		return m_outboundProxy;
	}

	/**
	 * normally called when the stack is initialized by a client
	 * @param proxy the static outbound proxy
	 * @see com.ibm.ws.sip.stack.transaction.transport.routers.Router#setOutboundProxy(com.ibm.ws.sip.stack.transaction.transport.Hop)
	 */
	public void setOutboundProxy(Hop proxy) {
		m_outboundProxy = proxy;
	}
	
	/**
	 * gets the next hop for the request using round-robin
	 * @param request outbound request
	 * @param originalHop the original destination this request was sent to,
	 *  in case this request is a retransmission.
	 *  null if this request is not a retransmission.
	 * @return the hop to send the request to
	 */
	public Hop getNextHop(Request request, Hop originalHop) {
		if (m_outboundProxy != null) {
			// fixed outbound proxy
			return m_outboundProxy;
		}
		if (m_slsps == null && originalHop != null) {
			// retransmission in standalone mode is always sent to the same
			// hop as the original request.
			return originalHop;
		}
		SipURL url = getRemoteTarget(request);
		Hop hop = url == null
			? null // getRemoteTarget failed and logged an error message
			: getNextHop(url);
		return hop;
	}
	
	/**
	 * gets the next hop for a response, based on the top Via header.
	 * @param topVia top Via header in response
	 * @return the next hop to send the response to
	 */
	public Hop getNextHop(Response response) {
		Hop hop = null;

		//try to get the remote target
		SipURL url = getRemoteTarget(response);
		
		// getRemoteTarget
		if (url != null){
			hop = getNextHop(url);
		}
		
		//return the hop, if found
		if (hop != null){
			return hop;
		}
		
		// try to send the response on the same connection as the request.
		// if there is no connection, use the top via.
		ViaHeader topVia = null;
		try {
			topVia = (ViaHeader)response.getHeader(ViaHeader.name, true);
		}
		catch (SipParseException e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(
					this,
					"getNextHop",
					"Error parsing top Via",
					e);
			}
		}
		if (topVia == null) {
			// no connection and no via. cannot respond.
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(
					this,
					"getNextHop",
					"Error: cannot reply with no Via");
			}
			return null;
		}

		hop = new Hop(topVia);
		if (m_slsps != null && !m_slsps.contains(hop)) {
			// in cluster, try to route the response through the same proxy
			// that delivered the original request.
			// only do round-robin if that proxy crashed during the transaction.
			if (c_logger.isTraceFailureEnabled()) {
				c_logger.traceFailure(this, "getNextHop",
					"Warning: routing response by round-robin " +
						"because original request proxy [" + hop +
						"] is gone");
			}
			String transport = topVia.getTransport().toUpperCase();
			hop = getRoundRobin(transport);
		}
		return hop;
	}
	
	/**
	 * gets the next hop for the url using round-robin
	 * @param url remote target
	 * @return next hop to send the request, null on error
	 */
	protected Hop getNextHop(SipURL url) {
		Hop hop;
		if (m_slsps == null) {
			// standalone mode. route to url
			hop = Hop.getHop(url);
		}
		else {
			// slsp mode. route to some slsp
			String transport = getTransport(url).toUpperCase();
			boolean outboundExtension = url.hasParameter(SipStackUtil.IBM_OB_PARAM);
			if (outboundExtension) {
				// don't round-robin. route to the specific slsp that accepted
				// the original connection.
				String host = url.getParameter(SipStackUtil.IBM_PROXY_HOST_PARAM);
				String portStr = url.getParameter(SipStackUtil.IBM_PROXY_PORT_PARAM);
				int port = Integer.parseInt(portStr);
				hop = getSLSP(transport, host, port);
			}
			else {
				if (url.hasParameter(SipStackUtil.OB_PARAM)) {
					// the destination URI contains the standard "ob" parameter,
					// but not the proprietary "ibm-ob" parameter. this implies
					// that the flow token (if there is one) is not locally-
					// generated, and we remove it so that the proxy does not
					// apply "outbound" extension processing to this request.
					url.removeParameter(SipStackUtil.OB_PARAM);
				}
				hop = getRoundRobin(transport);
			}
		}
		return hop;
	}

	/**
	 * gets an slsp that matches the given transport, using round-robin,
	 * or one that doesn't match if none are listening to given transport
	 * @param transport given transport
	 * @return an slsp hop
	 */
	private synchronized Hop getRoundRobin(String transport) {
		if (m_slsps.isEmpty()) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "getNextHop", "Error: No known SLSPs");
			}
			return null;
		}

		// round-robin
		if (!m_roundRobin.hasNext()) {
			resetRoundRobin();
			if (!m_roundRobin.hasNext()) {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "getNextHop", "Error: No round-robin SLSP");
				}
				return null;
			}
		}

		// try to find an SLSP that listens to the same transport
		// as the destination
		Hop firstSlsp = (Hop)m_roundRobin.next();
		Hop hop = firstSlsp;
		boolean found = false;

		do {
			if (hop.getTrasport().equals(transport)) {
				found = true;
				break; // found with matching transport
			}
			if (!m_roundRobin.hasNext()) {
				resetRoundRobin();
			}
			hop = (Hop)m_roundRobin.next();
		} while (hop != firstSlsp);
		
		if (!found) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug("No SLSP for transport [" + transport + ']');
			}
			return null;
		}
		
		if (c_logger.isTraceDebugEnabled()) {
			StringBuffer buf = new StringBuffer("Round-robin SLSP for [");
			buf.append(transport);
			buf.append("] is [");
			buf.append(hop);
			buf.append(']');
			c_logger.traceDebug(this, "getRoundRobinSLSP", buf.toString());
		}
		return hop;
	}

	/**
	 * determines the remote target given a request
	 * this is either the request-uri, top-route, or (seldom) the bottom-route
	 * @param message outbound request to examine
	 * @return url of remote target, null on error
	 */
	protected SipURL getRemoteTarget(Message message) {
		SipURL url;

		try {
			URI uri = null;
			GenericNameAddressHeaderImpl nameAddress =
				(GenericNameAddressHeaderImpl)message.getHeader(SipStackUtil.DESTINATION_URI, true);
			if (nameAddress == null) {
				if (message instanceof Request){
					// no proprietary header
					NameAddressHeader topRoute = (NameAddressHeader)message.getHeader(RouteHeader.name, true);
					if (topRoute == null) {
						// no route header, so the target is the request-uri
						uri = ((Request)message).getRequestURI();
					}
					else {
						// use top route
						uri = topRoute.getNameAddress().getAddress();
					}
				}else{
					return null;
				}
			}
			else {
				// use proprietary address header
				uri = nameAddress.getNameAddress().getAddress();
			}
			if (uri instanceof SipURL) {
				url = (SipURL)uri;
			}
			else {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug("Error: getRemoteTarget - uri is not a SipURL");
				}
				url = null;
			}
		}
		catch (SipParseException e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(SLSPRouter.class.getName(), "getNextHops", e.getMessage(), e);
			}
			url = null;
		}
		return url;
	}

	/**
	 * utility to extract the transport from a given url
	 * @param url a sip url
	 * @return the transport parameter of this url, or a default if not specified
	 */
	private static String getTransport(SipURL url) {
		String transport = url.getTransport();
		if (transport == null) {
			transport = url.getScheme().equalsIgnoreCase("sips")
				? SIPConnectionsModel.instance().getDefaultSecureTransport()
				: ListeningPoint.TRANSPORT_UDP;
		}
		return transport;
	}

	/**
	 * return round-robin pointer to beginning of list.
	 */
	private void resetRoundRobin() {
		m_roundRobin = m_slsps.iterator();
	}

	/**
	 * gets an SLSP from the list of known SLSPs
	 * @param transport transport protocol
	 * @param host the proxy's container-facing host IP address
	 * @param port the proxy's container-facing port number
	 */
	private Hop getSLSP(String transport, String host, int port) {
		if (m_slsps == null) {
			return null;
		}
		Hop hop = new Hop(transport, host, port);
		return m_slsps.contains(hop)
			? hop
			: null;
	}

	/**
	 * adds another SLSP
	 * @param slsp the SLSP to be added
	 */
	public synchronized void addSLSP(Hop slsp) {
		if (m_outboundProxy != null) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug("Error: attempt to dynamically add SLSP "
					+ "with static OUTBOUND_PROXY configuration");
			}
			return;
		}
		if (m_slsps == null) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug("SLSPRouter switching to SLSP mode");
			}
			m_slsps = new HashSet();
		}
		if (m_slsps.add(slsp)) {
			resetRoundRobin();
		}
	}
	
	/**
	 * removes an SLSP from the list of known SLSPs
	 * @param slsp the SLSP to be removed
	 */
	public synchronized void removeSLSP(Hop slsp) {
		if (m_slsps == null) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug("Error: attempt to remove SLSP ["
					+ slsp.toString() + "] in standalone mode");
			}
			return;
		}
		removeConnectionHop(slsp);
	}

	/**
	 * removes all known SLSPs from the list
	 */
	public synchronized void removeSLSPs() {
		if (m_slsps == null) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug("Error: removeSLSPs called in standalone mode");
			}
			return;
		}
		m_slsps.clear();
		resetRoundRobin();
	}
	
	/**
	 * switches from UDP to TCP when MTU exceeded
	 * @param udpHop old UDP hop
	 * @return TCP hop for sending out the request
	 */
	public Hop switchTransport(Hop udpHop) {
		if (m_slsps == null) {
			// standalone mode. ok to create outbound connection
			Hop tcpHop = new Hop("TCP", udpHop.getHost(), udpHop.getPort());
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "switchTransport", "created new hop ["
					+ tcpHop + "] in standalone mode");
			}
			return tcpHop;
		}
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "switchTransport", "finding suitable TCP hop in SLSP mode for [" + udpHop + ']'); 
		}
		// slsp mode. use existing tcp connection. or tls. or udp if there is no other choice.
		// the important part is never to create an outbound connection in this mode.

		// iterate all known hops. find the best hop, using this priority in decending order:
		// 1. tcp on same host as given udp
		// 2. tls on same host
		// 3. tcp on different host
		// 4. tls on different host
		// 5. udp
		Hop tcpHopOnSameHost = null;
		Hop tlsHopOnSameHost = null;
		Hop tcpHopOnDifferentHost = null;
		Hop tlsHopOnDifferentHost = null;
		Hop hop;
		String host = udpHop.getHost();

		synchronized (this) {
			if (m_slsps.isEmpty()) {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "switchTransport", "Warning: no known SLSPs");
				}
				return udpHop;
			}

			if (!m_roundRobin.hasNext()) {
				resetRoundRobin();
				if (!m_roundRobin.hasNext()) {
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "switchTransport", "Error: No round-robin SLSP");
					}
					return udpHop;
				}
			}
			Hop firstSlsp = (Hop)m_roundRobin.next();
			hop = firstSlsp;
			do {
				if (hop.getTrasport().equals("TCP")) {
					if (hop.getHost().equals(host)) {
						tcpHopOnSameHost = hop;
						break;
					}
					else {
						tcpHopOnDifferentHost = hop;
					}
				}
				else if (hop.getTrasport().equals("TLS")) {
					if (hop.getHost().equals(host)) {
						tlsHopOnSameHost = hop;
					}
					else {
						tlsHopOnDifferentHost = hop;
					}
				}
				if (!m_roundRobin.hasNext()) {
					resetRoundRobin();
				}
				hop = (Hop)m_roundRobin.next();
			} while (hop != firstSlsp);
		}
		hop = null;
		if (tcpHopOnSameHost != null) {
			hop = tcpHopOnSameHost;
		}
		else if (tlsHopOnSameHost != null) {
			hop = tlsHopOnSameHost;
		}
		else if (tcpHopOnDifferentHost != null) {
			hop = tcpHopOnDifferentHost;
		}
		else if (tlsHopOnDifferentHost != null) {
			hop = tlsHopOnDifferentHost;
		}
		else {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "switchTransport", "Warning: no TCP/TLS-enabled SLSPs for switching transport"); 
			}
			hop = udpHop;
		}
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "switchTransport",
				"switched from [" + udpHop + "] to [" + hop + ']'); 
		}
		return hop;
	}
	
	/**
	 * utility method used to determine if we are running in standalone mode
	 * (or clustered env)
	 *  
	 * @return true if standalone, otherwise, return false
	 */
	public boolean isStandAloneMode(){
		return m_slsps == null;
	}
}
