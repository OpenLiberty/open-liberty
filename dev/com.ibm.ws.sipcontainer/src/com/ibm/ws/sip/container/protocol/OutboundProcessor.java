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
package com.ibm.ws.sip.container.protocol;

import jain.protocol.ip.sip.ListeningPoint;
import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.SipProvider;
import jain.protocol.ip.sip.address.NameAddress;
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.header.ContactHeader;
import jain.protocol.ip.sip.header.HeaderIterator;
import jain.protocol.ip.sip.header.HeaderParseException;
import jain.protocol.ip.sip.header.NameAddressHeader;
import jain.protocol.ip.sip.header.RouteHeader;
import jain.protocol.ip.sip.header.ViaHeader;
import jain.protocol.ip.sip.message.Request;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;

import javax.crypto.Mac;
import javax.servlet.sip.Address;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.ListeningPointImpl;
import com.ibm.ws.jain.protocol.ip.sip.extensions.PathHeader;
import com.ibm.ws.sip.container.proxy.SipProxyInfo;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.servlets.SipServletsFactoryImpl;
import com.ibm.ws.sip.parser.util.Base64Parser;
import com.ibm.ws.sip.stack.transaction.util.SIPStackUtil;
import com.ibm.ws.sip.stack.util.AddressUtils;
import com.ibm.ws.sip.stack.util.SipStackUtil;
//TODO Liberty consider not using this class as it's in our HA component
//import com.ibm.ws.sip.hamanagment.util.SipClusterUtil;

/**
 * Singleton class that manages the "outbound" protocol extension for all
 * messages, incoming and outgoing, according to RFC 5626.
 * 
 * @author ran
 */
public class OutboundProcessor
{
	/** class logger */
	private static final LogMgr s_logger = Log.get(OutboundProcessor.class);

	/** singleton instance */
	private static final OutboundProcessor s_instance = new OutboundProcessor();

	/** true if running standalone, false if fronted by a WAS proxy */
	private final boolean m_standalone;

	/**
	 * a thread-local copy holding an array of 2 Route headers, used by the
	 * method {@link #getTopRoute(SipServletRequestImpl)} to return a couple
	 * of Route headers in one call.
	 */
	private static final ThreadLocal<Address[]> s_topRoutes =
		new ThreadLocal<Address[]>() {
			protected Address[] initialValue() {
				return new Address[2];
			}
		};

	/**
	 * @return the singleton instance
	 */
	public static OutboundProcessor instance() {
		return s_instance;
	}

	/**
	 * private constructor
	 */
	private OutboundProcessor() {
		m_standalone = true;/*TODO Liberty SipContainer.isRunningInWAS()
			? !SipClusterUtil.isServerInCluster()
			: true;*/
	}

	/**
	 * RFC 5626 5.1 processing of an incoming request.
	 * This method inspects the request to check if it qualifies for processing
	 * according to section 5.1 of RFC 5626.
	 * @param inRequest the incoming request
	 * @return true if RFC 5626 5.1 processing was applied by this call
	 */
	public boolean processRequest(SipServletRequestImpl inRequest) {
		try {
			if (!isOutboundExtensionRegisterRequest(inRequest)) {
				return false;
			}
		}
		catch (SipParseException e) {
			if (s_logger.isTraceFailureEnabled()) {
				s_logger.traceFailure(this, "processRequest", "", e);
			}
			return false;
		}
		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug(this, "processRequest",
				"request qualifies for outbound extension processing");
		}
		processRegisterRequest(inRequest);
		return true;
	}

	/**
	 * Inspects the incoming message to determine if it qualifies for RFC 5626
	 * ("outbound" extension) processing. Returns true if:
	 * 1. the request method is REGISTER, and
	 * 2. the container is the first hop, and
	 * 3. the Contact header contains a reg-id parameter
	 * @param inRequest incoming request
	 * @return true if the request qualifies for "outbound" processing
	 * @throws SipParseException if the message is invalid
	 */
	private boolean isOutboundExtensionRegisterRequest(
		SipServletRequestImpl inRequest)
		throws SipParseException
	{
		// 1. check method
		Request request = inRequest.getRequest();
		if (!request.getMethod().equals(Request.REGISTER)) {
			return false;
		}

		// 2. check number of Via headers
		if (!isDirect(request)) {
			return false;
		}

		// 3. check for reg-id parameter in the Contact header
		HeaderIterator contacts = request.getContactHeaders();
		if (contacts == null) {
			return false;
		}
		while (contacts.hasNext()) {
			ContactHeader contact = (ContactHeader)contacts.next();
			if (contact.getParameter("reg-id") == null) {
				return false;
			}
		}
		return true;
	}

	/**
	 * RFC 5626 5.1 processing of an incoming REGISTER request.
	 * The container acts as an "edge proxy" and adds a Path header.
	 * @param inRequest the incoming request
	 * @return true if RFC 5626 5.1 processing was applied by this call
	 */
	private void processRegisterRequest(SipServletRequestImpl inRequest) {
		String flowToken = createFlowToken(inRequest);

		boolean sips = inRequest.getRequestURI().getScheme().equalsIgnoreCase("sips");
		String transport = inRequest.getTransport();
		String selfHost = getSelfHost(inRequest);
		int selfPort = getSelfPort(inRequest);
		SipFactory sipFactory = SipServletsFactoryImpl.getInstance();
		SipURI uri = sipFactory.createSipURI(flowToken, selfHost);
		uri.setSecure(sips);
		uri.setPort(selfPort);
		if (sips && transport.equals("tls")) {
			transport = "tcp";
		}
		uri.setTransportParam(transport);
		uri.setParameter(SipStackUtil.OB_PARAM, "");
		uri.setLrParam(true); // RFC 3327-4: "MUST include the loose-routing indicator"

		Address address = sipFactory.createAddress(uri);
		inRequest.addAddressHeader(PathHeader.name, address, true);

		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug(this, "processRegisterRequest",
				"path header added to request [" + address + ']');
		}
	}

	/**
	 * Creates a flow token that identifies the connection of the incoming request.
	 * This flow token is then added to the forwarded request.
	 * It is added as a Path when forwarding a REGISTER request, or as a 
	 * Record-Route when forwarding an "outgoing" dialog-forming request.
	 * @param request the incoming request
	 * @return the newly generated token
	 */
	private String createFlowToken(SipServletRequestImpl request) {
		String transport = request.getTransport();
		String localHost = request.getLocalAddr();
		int localPort = request.getLocalPort();
		String remoteHost = request.getRemoteAddr();
		int remotePort = request.getRemotePort();

		String proxyHost;
		int proxyPort;
		if (m_standalone) {
			proxyHost = null;
			proxyPort = 0;
		}
		else {
			// get the WAS proxy identity from the top Via.
			// this represents the proxy's internal (container-facing) address.
			Request jainRequest = request.getRequest();
			try {
				ViaHeader via = (ViaHeader)jainRequest.getHeader(ViaHeader.name, true);
				proxyHost = via.getReceived();
				if (proxyHost == null) {
					proxyHost = via.getHost();
				}
				proxyPort = via.getPort();
			}
			catch (Exception e) {
				if (s_logger.isTraceFailureEnabled()) {
					s_logger.traceFailure(this, "createFlowToken", "", e);
				}
				proxyHost = null;
				proxyPort = 0;
			}
		}
		// the localHost might be a host name, and must be converted to an IP address.
		localHost = SIPStackUtil.getHostAddress(localHost);

		String flowToken = encodeFlowToken(transport,
			remoteHost, remotePort,
			localHost, localPort,
			proxyHost, proxyPort);
		return flowToken;
	}

	/**
	 * Encodes connection information into a new flow token, according to
	 * RFC 5626 5.2
	 * @param transport the transport protocol
	 * @param remoteHost client IP address
	 * @param remotePort client port number
	 * @param localHost local listening point IP address
	 * @param localPort local listening point port number
	 * @param proxyHost the proxy's internal IP address, or null in standalone
	 * @param proxyPort the proxy's internal port number, or 0 in standalone
	 * @return the newly encoded flow token
	 */
	private String encodeFlowToken(String transport,
		String remoteHost, int remotePort,
		String localHost, int localPort,
		String proxyHost, int proxyPort)
	{
		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug(this, "encodeFlowToken",
				"remote [" + remoteHost + ':' + remotePort +
				"] local [" + localHost + ':' + localPort +
				"] proxy [" + proxyHost + ':' + proxyPort + ']');
		}
		// create the byte array. format:
		// 3 bytes header signature
		// 1 byte transport (0=UDP,1=TCP,2=TLS)
		// 1 byte remote host size
		// 4/16 bytes remote host
		// 2 bytes remote port number
		// 1 byte local host size
		// 4/16 bytes local host
		// 2 bytes local port number
		// 1 byte proxy host size
		// 4/16 bytes proxy host
		// 2 bytes proxy port number
		// 1 byte MAC (message authentication code) size
		// n bytes MAC signature
		int remoteHostSize = remoteHost.indexOf(':') == -1 ? 4 : 16;
		int localHostSize = localHost.indexOf(':') == -1 ? 4 : 16;
		int proxyHostSize = proxyHost == null
			? 0
			: proxyHost.indexOf(':') == -1 ? 4 : 16;
		FlowTokenSecurity flowTokenSecurity = FlowTokenSecurity.instance();
		FlowTokenSecurity.Secret secret = flowTokenSecurity.getLatestSecret();
		Mac mac = secret == null ? null : secret.m_mac;
		int macSize = mac == null ? 0 : mac.getMacLength();
		if (macSize > 255) {
			macSize = 255; // truncate the MAC if it's too large (RFC 2104-5)
		}
		int tokenSize = 3 + 1 +
			1 + remoteHostSize + 2 +
			1 + localHostSize + 2 +
			1 + proxyHostSize + 2 +
			1 + macSize;
		byte[] byteArray = new byte[tokenSize];

		// serialize header signature
		int byteArrayIndex = 0;
		byteArray[byteArrayIndex++] = 'i';
		byteArray[byteArrayIndex++] = 'b';
		byteArray[byteArrayIndex++] = 'm';

		// serialize transport
		byte transportValue;
		if (transport.equalsIgnoreCase(ListeningPoint.TRANSPORT_UDP)) {
			transportValue = 0;
		}
		else if (transport.equalsIgnoreCase(ListeningPoint.TRANSPORT_TCP)) {
			transportValue = 1;
		}
		else if (transport.equalsIgnoreCase(ListeningPointImpl.TRANSPORT_TLS)) {
			transportValue = 2;
		}
		else {
			throw new RuntimeException("invalid transport [" + transport + ']');
		}
		byteArray[byteArrayIndex++] = transportValue;

		// serialize remote host
		if (remoteHostSize != 4 && remoteHostSize != 16) {
			throw new RuntimeException("invalid remote host [" + remoteHost + ']');
		}
		byte[] remoteHostBytes = AddressUtils.convertIP(remoteHost);
		byteArray[byteArrayIndex++] = (byte)(remoteHostSize & 0xff);
		System.arraycopy(remoteHostBytes, 0, byteArray, byteArrayIndex,
			remoteHostSize);
		byteArrayIndex += remoteHostSize;

		// serialize remote port
		byteArray[byteArrayIndex++] = (byte)((remotePort & 0xff00) >> 8);
		byteArray[byteArrayIndex++] = (byte)(remotePort & 0xff);

		// serialize local host
		if (localHostSize != 4 && localHostSize != 16) {
			throw new RuntimeException("invalid local host [" + localHost + ']');
		}
		byteArray[byteArrayIndex++] = (byte)(localHostSize & 0xff);
		byte[] localHostBytes = AddressUtils.convertIP(localHost);
		System.arraycopy(localHostBytes, 0, byteArray, byteArrayIndex,
			localHostSize);
		byteArrayIndex += localHostSize;

		// serialize local port
		byteArray[byteArrayIndex++] = (byte)((localPort & 0xff00) >> 8);
		byteArray[byteArrayIndex++] = (byte)(localPort & 0xff);

		// serialize proxy host
		byteArray[byteArrayIndex++] = (byte)(proxyHostSize & 0xff);
		if (proxyHostSize > 0) {
			if (proxyHostSize != 4 && proxyHostSize != 16) {
				throw new RuntimeException("invalid proxy host [" + proxyHost + ']');
			}
			byte[] proxyHostBytes = AddressUtils.convertIP(proxyHost);
			System.arraycopy(proxyHostBytes, 0, byteArray, byteArrayIndex,
				proxyHostSize);
			byteArrayIndex += proxyHostSize;
		}

		// serialize proxy port
		byteArray[byteArrayIndex++] = (byte)((proxyPort & 0xff00) >> 8);
		byteArray[byteArrayIndex++] = (byte)(proxyPort & 0xff);

		// serialize MAC size
		byteArray[byteArrayIndex++] = (byte)(macSize & 0xff);

		// serialize the MAC
		if (macSize > 0) {
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug(this, "encodeFlowToken",
					"calculating MAC using [" + secret + ']');
			}
			byte[] macBytes = flowTokenSecurity.calculateMac(
				byteArray, 0, byteArrayIndex, mac, macSize);
			System.arraycopy(macBytes, 0, byteArray, byteArrayIndex, macSize);
			byteArrayIndex += macSize;
		}

		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug(this, "encodeFlowToken",
				"encoding " + Arrays.toString(byteArray));
		}

		// encode byte array as a string
		String flowToken = Base64Parser.encode(byteArray);
		return flowToken;
	}

	/**
	 * Decodes connection information from a given flow token
	 * @param flowToken the flow token taken from the user part in a URI
	 * @return a Flow containing the transport, local address, local port,
	 *  remote address, and remote port, as previously encoded.
	 *  returns null if this is not a locally generated flow token.
	 *  if the flow token is tampered, a tampered flow token is returned.
	 */
	public Flow decodeFlowToken(String flowToken) {
		// decode string into a byte array
		byte[] byteArray = Base64Parser.decode(flowToken);
		int byteArrayLength = byteArray == null
			? 0
			: byteArray.length;
		if (byteArrayLength == 0) {
			// this is not an error, it's not meant to be a flow token
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug(this, "decodeFlowToken",
					"not a flow token [" + flowToken + ']');
			}
			return null;
		}
		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug(this, "decodeFlowToken",
				"decoding " + Arrays.toString(byteArray));
		}

		// de-serialize header signature
		int byteArrayIndex = 0;
		if (byteArrayIndex+3 > byteArrayLength) {
			// this is not an error, it's just not a locally-generated flow token
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug(this, "decodeFlowToken",
					"no flow token signatue in [" + flowToken + ']');
			}
			return null;
			
		}
		if (byteArray[byteArrayIndex++] != 'i' ||
			byteArray[byteArrayIndex++] != 'b' ||
			byteArray[byteArrayIndex++] != 'm')
		{
			// this is not an error, it's just not a locally-generated flow token
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug(this, "decodeFlowToken",
					"not a locally-generated flow token [" + flowToken + ']');
			}
			return null;
		}

		// de-serialize transport
		if (byteArrayIndex >= byteArrayLength) {
			if (s_logger.isTraceFailureEnabled()) {
				s_logger.traceFailure(this, "decodeFlowToken",
					"expected transport but got end of flow token ["
						+ flowToken + ']');
			}
			return null;
		}
		String transport;
		byte transportValue = byteArray[byteArrayIndex++];
		switch (transportValue) {
		case 0:
			transport = ListeningPoint.TRANSPORT_UDP;
			break;
		case 1:
			transport = ListeningPoint.TRANSPORT_TCP;
			break;
		case 2:
			transport = ListeningPointImpl.TRANSPORT_TLS;
			break;
		default:
			if (s_logger.isTraceFailureEnabled()) {
				s_logger.traceFailure(this, "decodeFlowToken",
					"expected transport (0-2) but got [" + transportValue +
					"] in flow token [" + flowToken + ']');
			}
			return null;
		}

		// de-serialize remote host size
		if (byteArrayIndex >= byteArrayLength) {
			if (s_logger.isTraceFailureEnabled()) {
				s_logger.traceFailure(this, "decodeFlowToken",
					"expected remote host size but got end of flow token ["
						+ flowToken + ']');
			}
			return null;
		}
		int remoteHostSize = byteArray[byteArrayIndex++] & 0xff;

		// de-serialize remote host
		if (byteArrayIndex+remoteHostSize > byteArrayLength) {
			if (s_logger.isTraceFailureEnabled()) {
				s_logger.traceFailure(this, "decodeFlowToken",
					"expected remote host but got end of flow token ["
						+ flowToken + ']');
			}
			return null;
		}
		String remoteHost = AddressUtils.convertIP(byteArray, byteArrayIndex,
			remoteHostSize);
		if (remoteHost == null) {
			if (s_logger.isTraceFailureEnabled()) {
				s_logger.traceFailure(this, "decodeFlowToken",
					"bad remote host in [" + Arrays.toString(byteArray) + ']');
			}
			return null;
		}
		byteArrayIndex += remoteHostSize;

		// de-serialize remote port
		if (byteArrayIndex+2 > byteArrayLength) {
			if (s_logger.isTraceFailureEnabled()) {
				s_logger.traceFailure(this, "decodeFlowToken",
					"expected remote port but got end of flow token ["
						+ flowToken + ']');
			}
			return null;
		}
		int remotePort = ((byteArray[byteArrayIndex++] & 0xff) << 8) |
			(byteArray[byteArrayIndex++] & 0xff);

		// de-serialize local host size
		if (byteArrayIndex >= byteArrayLength) {
			if (s_logger.isTraceFailureEnabled()) {
				s_logger.traceFailure(this, "decodeFlowToken",
					"expected local host size but got end of flow token ["
						+ flowToken + ']');
			}
			return null;
		}
		int localHostSize = byteArray[byteArrayIndex++] & 0xff;

		// de-serialize local host
		if (byteArrayIndex+localHostSize > byteArrayLength) {
			if (s_logger.isTraceFailureEnabled()) {
				s_logger.traceFailure(this, "decodeFlowToken",
					"expected local host but got end of flow token ["
						+ flowToken + ']');
			}
			return null;
		}
		String localHost = AddressUtils.convertIP(byteArray, byteArrayIndex,
			localHostSize);
		if (localHost == null) {
			if (s_logger.isTraceFailureEnabled()) {
				s_logger.traceFailure(this, "decodeFlowToken",
					"bad local host in [" + Arrays.toString(byteArray) + ']');
			}
			return null;
		}
		byteArrayIndex += localHostSize;

		// de-serialize local port
		if (byteArrayIndex+2 > byteArrayLength) {
			if (s_logger.isTraceFailureEnabled()) {
				s_logger.traceFailure(this, "decodeFlowToken",
					"expected local port but got end of flow token ["
						+ flowToken + ']');
			}
			return null;
		}
		int localPort = ((byteArray[byteArrayIndex++] & 0xff) << 8) |
			(byteArray[byteArrayIndex++] & 0xff);

		// de-serialize proxy host size
		if (byteArrayIndex >= byteArrayLength) {
			if (s_logger.isTraceFailureEnabled()) {
				s_logger.traceFailure(this, "decodeFlowToken",
					"expected proxy host size but got end of flow token ["
						+ flowToken + ']');
			}
			return null;
		}
		int proxyHostSize = byteArray[byteArrayIndex++] & 0xff;

		// de-serialize proxy host
		if (byteArrayIndex+proxyHostSize > byteArrayLength) {
			if (s_logger.isTraceFailureEnabled()) {
				s_logger.traceFailure(this, "decodeFlowToken",
					"expected proxy host but got end of flow token ["
						+ flowToken + ']');
			}
			return null;
		}
		String proxyHost;
		if (proxyHostSize == 0) {
			proxyHost = null;
		}
		else {
			proxyHost = AddressUtils.convertIP(byteArray, byteArrayIndex, proxyHostSize);
			if (proxyHost == null) {
				if (s_logger.isTraceFailureEnabled()) {
					s_logger.traceFailure(this, "decodeFlowToken",
						"bad proxy host in [" + Arrays.toString(byteArray) + ']');
				}
				return null;
			}
		}
		byteArrayIndex += proxyHostSize;

		// de-serialize proxy port
		if (byteArrayIndex+2 > byteArrayLength) {
			if (s_logger.isTraceFailureEnabled()) {
				s_logger.traceFailure(this, "decodeFlowToken",
					"expected proxy port but got end of flow token ["
						+ flowToken + ']');
			}
			return null;
		}
		int proxyPort = ((byteArray[byteArrayIndex++] & 0xff) << 8) |
			(byteArray[byteArrayIndex++] & 0xff);

		// de-serialize MAC size
		int expectedMacSize = byteArrayIndex < byteArrayLength
			? (byteArray[byteArrayIndex++] & 0xff)
			: 0;

		// de-serialize the MAC and validate it
		boolean tampered;
		FlowTokenSecurity flowTokenSecurity = FlowTokenSecurity.instance();
		ArrayList<FlowTokenSecurity.Secret> secrets = flowTokenSecurity.getSecretSet();
		if (expectedMacSize > 0) {
			if (secrets == null) {
				if (s_logger.isTraceFailureEnabled()) {
					s_logger.traceFailure(this, "decodeFlowToken",
						"encoded MAC but no local MAC");
				}
				tampered = true;
			}
			else {
				int bytesLeft = byteArrayLength - byteArrayIndex;
				if (bytesLeft < expectedMacSize) {
					if (s_logger.isTraceFailureEnabled()) {
						s_logger.traceFailure(this, "decodeFlowToken",
							"expected MAC of [" + expectedMacSize + "] but only ["
							+ bytesLeft + "] bytes left");
					}
					tampered = true;
				}
				else {
					tampered = !flowTokenSecurity.authenticateMac(
						secrets, byteArray, 0, byteArrayIndex, expectedMacSize);
					byteArrayIndex += expectedMacSize;
				}
			}
		}
		else {
			// no MAC encoded in the message. match only if no local MAC.
			if (secrets == null) {
				tampered = false; // because flow token security is disabled
			}
			else {
				if (s_logger.isTraceFailureEnabled()) {
					s_logger.traceFailure(this, "decodeFlowToken",
						"no encoded MAC");
				}
				tampered = true;
			}
		}
		if (tampered) {
			if (s_logger.isTraceFailureEnabled()) {
				s_logger.traceFailure(this, "decodeFlowToken", "MAC rejected");
			}
		}
		else {
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug(this, "decodeFlowToken", "MAC validation passed");
			}
		}

		Flow flow = new Flow(transport,
			remoteHost, remotePort,
			localHost, localPort,
			proxyHost, proxyPort,
			tampered);
		return flow;
	}

	/**
	 * Called when forwarding a request as a proxy, to apply RFC 5626 5.3
	 * processing. This method inspects the inbound request to check if it
	 * qualifies for processing according to section 5.3 of RFC 5626. In case
	 * it does, it proceeds to following either 5.3.1 or 5.3.2, depending on
	 * the request forwarding direction.
	 * 
	 * @param inRequest inbound request
	 * @param outRequest outbound request
	 * @param recordRoute the Record-Route URI added by the calling proxy,
	 *  onto the outbound request, or null if the calling proxy did not push
	 *  a Record-Route 
	 */
	public void forwardingRequest(
		SipServletRequestImpl inRequest, SipServletRequestImpl outRequest,
		SipURI recordRoute)
	{
		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug(this, "forwardingRequest", inRequest.getMethod());
		}
		// "If the edge proxy receives a request where the edge proxy is the
		// host in the topmost Route header field value, and the Route header
		// field value contains a flow token, the proxy follows the procedures
		// of this section. Otherwise the edge proxy skips the procedures in
		// this section"
		// note there is a hole in this section of the RFC. it is possible that
		// the request is "outgoing" (coming from the client behind NAT/FW) and
		// it contains the "ob" parameter in the Contact, but no flow token in
		// the Route. this should be treated as "outgoing" (section 5.3.2)
		// rather than getting skipped.
		Address[] routes = getTopRoute(inRequest); // the two top Routes
		int iRoute = 0;
		Address route;
		boolean incoming = false;
		SipURI routeURI = null;
		Flow flow = null;

		// we must check the two top Routes. if the application acts as an
		// "edge proxy" then it might have added a Path on top of our
		// flow-token-enabled Path when it forwarded the original REGISTER.
		while (iRoute < routes.length && ((route = routes[iRoute++]) != null)) {
			URI uri = route.getURI();
			if (!(uri instanceof SipURI)) {
				continue; // not a locally-generated top Route
			}
			routeURI = (SipURI)uri;

			// only inspect a flow token if it was locally-generated
			boolean topRouteIsSelf = isSelfRoute(routeURI, inRequest);
			if (!topRouteIsSelf) {
				routeURI = null;
				continue; // not a locally-generated top Route
			}
			String flowToken = routeURI.getUser();
			if (flowToken != null) {
				flow = decodeFlowToken(flowToken);
				if (flow != null) {
					// "If the flow in the flow token identified by the topmost
					// Route header field value matches the source IP address
					// and port of the request, the request is an "outgoing"
					// request; otherwise, it is an "incoming" request."
					String flowTokenRemoteHost = flow.getRemoteHost();
					int flowTokenRemotePort = flow.getRemotePort();
					int remotePort = inRequest.getRemotePort();
					String remoteHost = inRequest.getRemoteAddr();
					incoming = 								flowTokenRemotePort != remotePort ||
!SIPStackUtil.isSameHost(flowTokenRemoteHost,remoteHost);

					break;
				}
			}
		}
		if (incoming) {
			processIncomingRequest(inRequest, outRequest, routeURI, recordRoute, flow);
		}
		else {
			processOutgoingRequest(inRequest, outRequest, routeURI, recordRoute);
		}
	}

	/**
	 * RFC 5626 5.3.1 "incoming" request processing.
	 * The term "incoming" in this RFC section applies to the request
	 * direction relative to the client who resides behind the NAT/FW.
	 * If the request is targeted TO a client behind a NAT/FW, it is "incoming".
	 * @param inRequest the inbound "incoming" request
	 * @param outRequest the outbound "incoming" request
	 * @param topRoute the topmost Route URI in inRequest, containing a flow token
	 * @param recordRoute the Record-Route URI added by the calling proxy,
	 *  or null if the calling proxy did not push a Record-Route
	 * @param flow the decoded flow token
	 */
	private void processIncomingRequest(
		SipServletRequestImpl inRequest, SipServletRequestImpl outRequest,
		SipURI topRoute, SipURI recordRoute, Flow flow)
	{
		// "whether the Route header field contained an "ob" URI parameter
		// or not, the proxy removes the Route header field value and forwards
		// the request over the 'logical flow' identified by the flow token"
		String flowToken = topRoute.getUser();
		removeRoute(outRequest, flowToken);

		// forward the request over the flow by setting it in IBM-Destination.
		// but don't override the IBM-Destination if there is one already.
		// the undocumented IBM-Destination policy is "first wins".
		if (outRequest.getHeader(SipStackUtil.DESTINATION_URI) != null) {
			if (s_logger.isTraceFailureEnabled()) {
				s_logger.traceFailure(this, "processIncomingRequest",
					"not copying flow token because there already is an "
					+ "IBM-Destination in the forwarded request");
			}
			return;
		}

		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug(this, "processIncomingRequest",
				"copying flow [" + flow + "] token [" + flowToken
				+ "] to forwarded request");
		}
		String transport = flow.getTransport();
		String remoteHost = flow.getRemoteHost();
		int remotePort = flow.getRemotePort();
		SipURI destinationURI = (SipURI)topRoute.clone();
		destinationURI.setUser(""); // remove the flow token
		destinationURI.setTransportParam(transport);
		destinationURI.setHost(remoteHost);
		destinationURI.setPort(remotePort);

		// make sure there is an "ob" parameter in the IBM-Destination, it is
		// needed by the stack (standalone) / proxy (cluster) for returning
		// a 430 (Flow Failed) error in case the client connection has broken.
		// see SipStackUtil.OB_PARAM / SipStackUtil.IBM_OB_PARAM.
		// only for stream transports.
		if (!transport.equalsIgnoreCase(ListeningPoint.TRANSPORT_UDP)) {
			destinationURI.setParameter(SipStackUtil.IBM_OB_PARAM, "");
			destinationURI.setParameter(SipStackUtil.OB_PARAM, "");
		}

		if (m_standalone) {
			// in standalone:
			// IBM-Destination: <remote host+port+transport>;ob
			// IBM-PO: <stack local listening point>
		}
		else {
			// in cluster:
			// IBM-Destination: <remote host+port+transport>;ob;ibm-proxyhost=<proxy internal host>;ibm-proxyport=<proxy internal port>
			// IBM-PO: <proxy external interface>
			String proxyHost = flow.getProxyHost();
			int proxyPort = flow.getProxyPort();
			if (proxyHost != null) {
				destinationURI.setParameter(SipStackUtil.IBM_PROXY_HOST_PARAM,
					proxyHost);
				destinationURI.setParameter(SipStackUtil.IBM_PROXY_PORT_PARAM,
					String.valueOf(proxyPort));
			}
		}

		if (flow.isTampered()) {
			// "If the flow token has been tampered with, the proxy SHOULD send a 403
			// (Forbidden) response"
			// we cannot generate a response here, because the client transaction
			// was not created yet. we just flag the URI as "ibm-tampered" and the
			// stack will generate the 403 response.
			destinationURI.setParameter(SipStackUtil.IBM_TAMPERED_PARAM, "");
		}

		SipFactory sipFactory = SipServletsFactoryImpl.getInstance();
		Address destination = sipFactory.createAddress(destinationURI);
		outRequest.addAddressHeader(SipStackUtil.DESTINATION_URI, destination, true);

		// convert localHost/localPort to IBM-PO and add it to the outbound request
		String localHost = flow.getLocalHost();
		int localPort = flow.getLocalPort();
		SipProxyInfo sipProxyInfo = SipProxyInfo.getInstance();
		int nInterfaces = sipProxyInfo.getNumberOfInterfaces(transport);
		int iface = nInterfaces == 1
			? 0
			: sipProxyInfo.getIndexOfIface(transport, localHost, localPort);
		if (iface >= 0) {
			// for debugging
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug(this, "processIncomingRequest",
						(outRequest.getHeader(SipProxyInfo.PEREFERED_OUTBOUND_HDR_NAME) == null) ? 
								"setting [": "replacing [" + iface + "] in IBM-PO header");
			}
			sipProxyInfo.addPreferedOutboundHeader(outRequest, iface);
		}
		else {
			// no matching listening point. by not adding an IBM-PO (and
			// (keeping the "ob" parameter) this will result in a 430 response.
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug(this, "processIncomingRequest",	"no matching listening point");
			}
		}

		// "If the Route header value contains an "ob" URI parameter, and the
		// request is a new dialog-forming request, the proxy needs to adjust
		// the route set to ensure that subsequent requests in the dialog can be
		// delivered over a valid flow to the UA instance identified by the flow
		// token"
		// we determine this is a dialog-forming request if the calling proxy
		// has pushed a Record-Route.
		if (recordRoute != null && topRoute.getParameter(SipStackUtil.OB_PARAM) != null) {
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug(this, "processIncomingRequest",
					"setting [" + flowToken + "] in Record-Route [" + recordRoute + ']');
			}
			recordRoute.setUser(flowToken);
		}
	}

	/**
	 * RFC 5626 5.3.2 "outgoing" request processing.
	 * The term "outgoing" in this RFC section applies to the request
	 * direction relative to the client who resides behind the NAT/FW.
	 * If the request is coming FROM a client behind a NAT/FW, it is "outgoing".
	 * @param inRequest the inbound "outgoing" request
	 * @param outRequest the outbound "outgoing" request
	 * @param topRoute the topmost Route URI in inRequest, which matches the
	 *  self interface. null if there is no Route header field in inRequest,
	 *  or if the top Route is not the self interface.
	 * @param recordRoute the Record-Route URI added by the calling proxy,
	 *  or null if the calling proxy did not push a Record-Route 
	 */
	private void processOutgoingRequest(
		SipServletRequestImpl inRequest, SipServletRequestImpl outRequest,
		SipURI topRoute, SipURI recordRoute)
	{
		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug(this, "processOutgoingRequest", inRequest.getMethod());
		}
		// "If the edge proxy receives an outgoing dialog-forming request, the
		// edge proxy can use the presence of the "ob" URI parameter in the
		// UAC's Contact URI (or topmost Route header field) to determine if the
		// edge proxy needs to assist in mid-dialog request routing"

		if (recordRoute == null) {
			// no Record-Route, nothing to do here
			return;
		}
		// special note: if we'd followed RFC 5626 straight forward, we could
		// only get here if the top Route was self-created. but we get here
		// also in other cases, to work-around a bug in the RFC, where the
		// client behind NAT/FW sends a request without a Route header, in
		// which case we (as an edge proxy) still want to assist in mid-dialog
		// request routing. so, the only way to tell if this is really an
		// "outgoing" request, is to count Vias, just like doing when
		// processing REGISTER requests.
		// in other words, we define "outgoing" request as a request that
		// contains one Via, and an "ob" parameter in either the top Route
		// or the Contact.
		if (!isDirect(inRequest.getRequest())) {
			return;
		}

		boolean ob = topRoute != null && topRoute.getParameter(SipStackUtil.OB_PARAM) != null;
		if (ob) {
			// the top route contains the "ob" parameter.
		}
		else {
			// no "ob" parameter in the top Route. look for "ob" in the Contact.
			Address contact;
			try {
				contact = inRequest.getAddressHeader(ContactHeader.name);
				if (contact != null) {
					URI uri = contact.getURI();
					if (uri instanceof SipURI) {
						SipURI sipURI = (SipURI)uri;
						ob = sipURI.getParameter(SipStackUtil.OB_PARAM) != null;
					}
				}
			}
			catch (ServletParseException e) {
				if (s_logger.isTraceFailureEnabled()) {
					s_logger.traceFailure(this, "processOutgoingRequest", "", e);
				}
			}
			if (!ob) {
				return;
			}
		}
		// get here with "ob" parameter in either the top Route or the Contact
		String flowToken = createFlowToken(inRequest);
		recordRoute.setUser(flowToken);
	}

	/**
	 * inspects the given request to see if it comes directly from a client
	 * (with no mid-way proxies in between) by counting Via header field values.
	 * in cluster, a request comes directly from a client if there are 2.
	 * in standalone, a request comes directly from a client if there is 1.
	 * @param request the incoming request
	 * @return true if this request comes directly from a client, false otherwise
	 */
	private boolean isDirect(Request request) {
		int expectedVias = m_standalone
			? 1  // in standalone, only the UAC's via
			: 2; // in cluster, the UAC's via plus the proxy's via
		int requestVias = 0;
		HeaderIterator vias = request.getViaHeaders();

		if (vias != null) {
			while (vias.hasNext()) {
				requestVias++;
				try {
					vias.next();
				}
				catch (HeaderParseException e) {
					if (s_logger.isTraceFailureEnabled()) {
						s_logger.traceFailure(this, "isDirect", "", e);
					}
					return false;
				}
			}
		}
		return requestVias <= expectedVias;
	}

	/**
	 * helper method that gets the two topmost Route header field values
	 * in the message
	 * @param inRequest inbound request to inspect
	 * @return an array of 2 elements:
	 *  item at index 0: the top Route, or null if none
	 *  item at index 1: the second-top Route, or null if none
	 *  this array is only valid from the calling thread, and only until
	 *  the next call to this method
	 */
	private Address[] getTopRoute(SipServletRequestImpl inRequest) {
		Address route0 = inRequest.getPoppedRoute();
		Address route1;
		if (route0 == null) {
			try {
				route0 = inRequest.getAddressHeader(RouteHeader.name);
			}
			catch (ServletParseException e) {
				if (s_logger.isTraceFailureEnabled()) {
					s_logger.traceFailure(this, "getTopRoute", "", e);
				}
				route0 = null;
			}
			if (route0 == null) {
				route1 = null;
			}
			else try {
				ListIterator<Address> routeI = inRequest.getAddressHeaders(RouteHeader.name);
				route1 = (routeI.hasNext() && routeI.next() != null && routeI.hasNext())
					? routeI.next()
					: null;
			}
			catch (ServletParseException e) {
				if (s_logger.isTraceFailureEnabled()) {
					s_logger.traceFailure(this, "getTopRoute", "", e);
				}
				route1 = null;
			}
		}
		else try {
			route1 = inRequest.getAddressHeader(RouteHeader.name);
		}
		catch (ServletParseException e) {
			if (s_logger.isTraceFailureEnabled()) {
				s_logger.traceFailure(this, "getTopRoute", "", e);
			}
			route1 = null;
		}
		Address[] routes = s_topRoutes.get();
		routes[0] = route0;
		routes[1] = route1;
		return routes;
	}

	/**
	 * called when inspecting an incoming Route to determine if it is a
	 * locally-generated Route
	 * @param route a Route URI
	 * @param inRequest the incoming request
	 * @return true if the URI is locally-generated, false otherwise
	 */
	private boolean isSelfRoute(SipURI route, SipServletRequestImpl inRequest) {
		String selfHost = getSelfHost(inRequest);
		int selfPort = getSelfPort(inRequest);
		String routeHost = route.getHost();
		int routePort = route.getPort();
		String routeTransport;
		if (route.isSecure()) {
			routeTransport = "tls";
		}
		else {
			routeTransport = route.getTransportParam();
			if (routeTransport == null) {
				routeTransport = "udp";
			}
		}
		if (routePort == -1) {
			routePort = routeTransport.equalsIgnoreCase("tls")
				? 5061
				: 5060;
		}
		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug(this, "isSelfRoute",
				"selfHost [" + selfHost + "] selfPort [" + selfPort
				+ "] route [" + route + ']');
		}
		boolean self = routePort == selfPort
			&& routeHost.equalsIgnoreCase(selfHost);
		if (self) {
			// the Route matches the local inbound interface. this is either
			// originated by a self Path, or by a self Record-Route
		}
		else {
			// try to match the Route to one of the local outbound interfaces.
			// these are originated by a self Record-Route.
			SipProxyInfo sipProxyInfo = SipProxyInfo.getInstance();
			int index = sipProxyInfo.getIndexOfIface(routeTransport, routeHost, routePort);
			self = index >= 0;
		}
		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug(this, "isSelfRoute", "self [" + self + ']');
		}
		return self;
	}

	/**
	 * called when creating a Path header field value, to return the address
	 * to place in the self-created Path URI.
	 * this method is also called when evaluating in incoming Route in order
	 * to tell if this Route was earlier created here as a Path, and then
	 * translated by a downstream registrar to a Route.
	 * @param inRequest the incoming request
	 * @return the host address that is added to the self-generated Path.
	 */
	private String getSelfHost(SipServletRequestImpl inRequest) {
		SipProvider provider = inRequest.getSipProvider();
		ListeningPoint listeningPoint = provider.getListeningPoint();
		return listeningPoint.getSentBy();
	}

	/**
	 * called when creating a Path header field value, to return the port
	 * number to place in the self-created Path URI.
	 * this method is also called when evaluating in incoming Route in order
	 * to tell if this Route was earlier created here as a Path, and then
	 * translated by a downstream registrar to a Route.
	 * @param inRequest the incoming request
	 * @return the port number that is added to the self-generated Path.
	 */
	private int getSelfPort(SipServletRequestImpl inRequest) {
		SipProvider provider = inRequest.getSipProvider();
		ListeningPoint listeningPoint = provider.getListeningPoint();
		return listeningPoint.getPort();
	}

	/**
	 * removes the route header field value from the message,
	 * which matches the given flow token.
	 * only looks the two top Route header field values.
	 * @param outRequest outbound request to modify
	 * @param flowToken the flow token to match
	 * @return true if found a Route with the given flow token, and removed it,
	 *  false otherwise.
	 */
	private boolean removeRoute(SipServletRequestImpl outRequest, String flowToken) {
		jain.protocol.ip.sip.message.Message message = outRequest.getMessage();
		try {
			NameAddressHeader route0 = (NameAddressHeader)message.getHeader(
				RouteHeader.name, true);
			if (route0 == null) {
				return false; // no Route header
			}
			// remove the top Route, whether or not it matches
			message.removeHeader(RouteHeader.name, true);

			if (hasFlowToken(route0, flowToken)) {
				// bingo. the top Route that we just removed is the correct one.
				return true;
			}
			// we just removed the wrong top Route. look down at the second one.
			NameAddressHeader route1 = (NameAddressHeader)message.getHeader(
				RouteHeader.name, true);
			if (route1 == null) {
				// no match. put back the top Route that was removed
				message.addHeader(route0, true);
				return false;
			}
			if (hasFlowToken(route1, flowToken)) {
				// bingo. the second-top Route is the correct one.
				// remove the second-top Route, and put back the top Route.
				message.removeHeader(RouteHeader.name, true);
				message.addHeader(route0, true);
				return true;
			}
		}
		catch (HeaderParseException e) {
			if (s_logger.isTraceFailureEnabled()) {
				s_logger.traceFailure(this, "removeRoute", "", e);
			}
		}
		return false;
	}

	/**
	 * helper that checks if the given address header contains the given flow token
	 * @param header the address header to check
	 * @param flowToken the flow token to compare with
	 * @return true if the given address header contains the flow token,
	 *  false otherwise
	 */
	private static boolean hasFlowToken(NameAddressHeader header, String flowToken) {
		NameAddress nameAddress = header.getNameAddress();
		jain.protocol.ip.sip.address.URI uri = nameAddress.getAddress();
		if (!(uri instanceof SipURL)) {
			return false;
		}
		SipURL sipURI = (SipURL)uri;
		String user = sipURI.getUserName();
		return user != null && user.equals(flowToken);
	}
}
