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
package com.ibm.ws.sip.stack.util;

import jain.protocol.ip.sip.ListeningPoint;
import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.address.AddressFactory;
import jain.protocol.ip.sip.address.NameAddress;
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.address.URI;
import jain.protocol.ip.sip.header.HeaderIterator;
import jain.protocol.ip.sip.header.HeaderParseException;
import jain.protocol.ip.sip.header.MaxForwardsHeader;
import jain.protocol.ip.sip.header.NameAddressHeader;
import jain.protocol.ip.sip.header.RouteHeader;
import jain.protocol.ip.sip.header.ToHeader;
import jain.protocol.ip.sip.header.ViaHeader;
import jain.protocol.ip.sip.message.Message;
import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

import java.io.IOException;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.ListeningPointImpl;
import com.ibm.ws.jain.protocol.ip.sip.SipJainFactories;
import com.ibm.ws.jain.protocol.ip.sip.extensions.SupportedHeader;
import com.ibm.ws.jain.protocol.ip.sip.header.GenericNameAddressHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.message.MessageImpl;

public class SipStackUtil {

	/** Class Logger. */
	private static final LogMgr c_logger = Log.get(SipStackUtil.class);

	/**
	 * Parameter added to the top route header in outgoing
	 * requests, to indicate that the next hop is a strict router, and therefore
	 * the message should be routed to the request-uri instead of the top route,
	 * and the final destination is the bottom route, instead of the
	 * request-uri. the component sending the request should remove this
	 */
	public final static String STRICT_ROUTING_PARAM = "ibmsr";

	/**
	 * Defines the header name for the destination URI. Stack will use this URL
	 * to send the request directly to.
	 */
	public final static String DESTINATION_URI = "IBM-Destination";

	public static final String SIP_SCHEME = "sip";

	public static final String SIPS_SCHEME = "sips";

	public static final String TLS_TRANSPORT = "tls";

	public static final int DEFAULT_TLS_PORT = 5061;

	/** the standard "ob" parameter as defined in RFC 5626 */
	public static final String OB_PARAM = "ob";

	/**
	 * the proprietary "ibm-ob" parameter is only set in the (proprietary)
	 * IBM-Destination header, unlike "ob" which is standard, and may appear
	 * in the Route header as well. "ibm-ob" tells the stack (in either
	 * cluster or standalone) that this URI contains a locally-generated
	 * flow token. it protects the scenario where an external Route header
	 * contains an (external) flow token and "ob" - in which case it is
	 * likely that such a Route will be copied, by the container code, to
	 * the IBM-Destination header. if the stack sees "ob" but no "ibm-ob"
	 * in IBM-Destination, then it knows the flow token is not
	 * locally-generated, and that it should not apply special "outbound"
	 * extension processing.
	 * in cluster, the stack also takes care of removing "ob" from
	 * IBM-Destination in case there is no "ibm-ob" before forwarding the
	 * request to the WAS proxy. this spares the WAS proxy from having to
	 * worry about "ibm-ob".
	 */
	public static final String IBM_OB_PARAM = "ibm-ob";

	/**
	 * the proprietary "ibm-proxyhost" parameter is only set in the
	 * IBM-Destination header, and it is only read by the stack
	 * (that is, it is not read by the WAS proxy). it tells the stack that
	 * the request should be routed through a specific WAS proxy instance.
	 */
	public static final String IBM_PROXY_HOST_PARAM = "ibm-proxyhost";

	/**
	 * the proprietary "ibm-proxyport" parameter is only set in the
	 * IBM-Destination header, and it is only read by the stack
	 * (that is, it is not read by the WAS proxy). it tells the stack that
	 * the request should be routed through a specific WAS proxy instance.
	 */
	public static final String IBM_PROXY_PORT_PARAM = "ibm-proxyport";

	/**
	 * the proprietary "ibm-tampered" parameter is only set in the
	 * IBM-Destination header, and it is only read by the stack
	 * (that is, it is not read by the WAS proxy). it tells the stack that
	 * the flow token (when using the "outbound" protocol extension) has
	 * been tamepered with, and the request should be rejected with a
	 * 403 (Forbidden) error.
	 */
	public static final String IBM_TAMPERED_PARAM = "ibm-tampered";

	/**
	 * Helper method that sets the target Header in the request. This header
	 * contains the actual address where stack should send the request.
	 * 
	 * @param request
	 * @param targetAddressUri
	 *            The actual address where the request should be sent.
	 * 
	 */
	public static void setDestinationHeader(SipURL targetAddressUri,
			Message message) throws SipParseException {
		// specify transport and port number if those are not explicitly set by
		// now
		String scheme = targetAddressUri.getScheme();
		if (scheme != null) {
			String transport = targetAddressUri.getTransport();
			int port = targetAddressUri.getPort();

			if (scheme.equals(SIP_SCHEME)) {
				if (transport == null) {
					targetAddressUri.setTransport(ListeningPoint.TRANSPORT_UDP);
					transport = ListeningPoint.TRANSPORT_UDP;
				}

				if (port < 0) {
					//in case that the transport is tls we should use the tls default transport
					if (transport.equalsIgnoreCase(TLS_TRANSPORT)){
						targetAddressUri.setPort(DEFAULT_TLS_PORT);
					}else{
						targetAddressUri.setPort(ListeningPoint.DEFAULT_PORT);
					}
				}
			} else if (scheme.equals(SIPS_SCHEME)) {
				// The IBM-Destination header is different than other headers
				// that contain a URI. It does not need to comply with the RFC,
				// and its
				// transport parameter is used for specifying the transport
				// protocol.
				// In other words, transport=tls is legal in the IBM-Destination
				// header,
				// while deprecated in standard URIs.

				if (transport == null || !transport.equalsIgnoreCase(TLS_TRANSPORT)) {
					targetAddressUri.setTransport(TLS_TRANSPORT);
				}
				if (port < 0) {
					targetAddressUri.setPort(DEFAULT_TLS_PORT);
				}
			}
		}
		NameAddressHeader ibmDestination = (NameAddressHeader) message
				.getHeader(DESTINATION_URI, true);
		if (ibmDestination == null) {
			AddressFactory addressFactory = SipJainFactories.getInstance()
					.getAddressFactory();
			NameAddress address = addressFactory
					.createNameAddress(targetAddressUri);
			ibmDestination = new GenericNameAddressHeaderImpl(DESTINATION_URI);
			ibmDestination.setNameAddress(address);
			message.setHeader(ibmDestination, true);
		} else {
			// change existing header
			NameAddress address = ibmDestination.getNameAddress();
			address.setAddress(targetAddressUri);
		}

		if (isOutOfDialogRequest(message)) {
			fixHeaders(message);
		}
	}

	/**
	 * Helper method that investigates the request and discover the target
	 * domain that this request should be sent to.
	 * 
	 * @param request
	 * @return
	 * @throws SipParseException
	 * @throws IOException
	 */
	private static SipURL createTargetFromRequest(Request message)
			throws IOException {
		// Find the target host to send the request.

		NameAddressHeader topRoute;
		SipURL sipUrlToSend = null;
		try {
			// try the IBM-Destination header
			URI uriToSend;
			NameAddressHeader destination = (NameAddressHeader) message
					.getHeader(SipStackUtil.DESTINATION_URI, true);
			if (destination == null) {
				// no IBM-Destination, try top Route
				topRoute = (NameAddressHeader) message.getHeader(RouteHeader.name,
						true);
				if (topRoute == null) {
					// no top Route, use request-URI
					uriToSend = message.getRequestURI();
				} else {
					// use top Route
					uriToSend = topRoute.getNameAddress().getAddress();
					if (!(uriToSend instanceof SipURL)) {
						if (c_logger.isTraceDebugEnabled()) {
							c_logger
									.traceDebug("Error: getTargetSipUri - top route uri is not a SipURL");
						}
						throw new IOException("top Route URI is not a SIP URI");
					}
					sipUrlToSend = (SipURL) uriToSend;
					boolean strictRouting = sipUrlToSend
							.hasParameter(SipStackUtil.STRICT_ROUTING_PARAM);

					if (strictRouting) {
						// This is the case of the strict routing and we should
						// use
						// the request URI to send the message to.
						uriToSend = message.getRequestURI();
					}
				}
			} else {
				// use IBM-Destination
				uriToSend = destination.getNameAddress().getAddress();
			}
			if (!(uriToSend instanceof SipURL)) {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger
							.traceDebug("Error: getTargetSipUri - top route uri is not a SipURL");
				}
				throw new IOException("URI is not a SIP URI");
			}
			sipUrlToSend = (SipURL) uriToSend;
		} catch (SipParseException e) {
			throw new IOException(e.getMessage());
		}

		// Ensure that target is correct. If there is sips scheme - transport
		// should be tcp, as tls is deprecated in RFC 3261 26.2.2:
		// "The use of transport=tls has consequently been deprecated, partly
		// because it was specific to a single hop of the request"
		String scheme = sipUrlToSend.getScheme();

		if (scheme != null) {
			String transport = sipUrlToSend.getTransport();
			if (scheme.equalsIgnoreCase(SipStackUtil.SIPS_SCHEME)) {
				if (transport != null) {
					if (transport
							.equalsIgnoreCase(ListeningPoint.TRANSPORT_UDP)) {
						// sips and udp - cannot send securely over udp
						throw new AmbiguousUriException(sipUrlToSend.toString());
					}
					if (!transport
							.equalsIgnoreCase(ListeningPoint.TRANSPORT_TCP)) {
						try {
							sipUrlToSend
									.setTransport(ListeningPoint.TRANSPORT_TCP);
						} catch (SipParseException e) {
							throw new IOException(e.getMessage());
						}
					}
				}
			}
			else {
				// removed the code that changes sip:..transport=tls to sips:..transport=tcp
			}
		}

		return sipUrlToSend;
	}

	public static String transportToScheme(String transport) {
		if (transport.equalsIgnoreCase(ListeningPoint.TRANSPORT_UDP)) {
			return SipStackUtil.SIP_SCHEME;
		}
		if (transport.equalsIgnoreCase(ListeningPoint.TRANSPORT_TCP)) {
			return SipStackUtil.SIP_SCHEME;
		}
		if (transport.equalsIgnoreCase(SipStackUtil.TLS_TRANSPORT)) {
			return SipStackUtil.SIPS_SCHEME;
		}
		return null;
	}

	/**
	 * creates the target destination from message
	 *   
	 * @param message
	 * @return the target
	 * 
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public static SipURL createTargetFromMessage(Message message)
			throws IllegalArgumentException, IOException {
		//TODO this method needs to return a clone cause it is returning an internal uri from the message
		// for now only the ClientTransaction.sendRequest calls for clone on this method result, but there might be
		// a problem with the other callers
		try {
			if (message instanceof Response) {
				Response response = (Response) message;
				return createTargetFromResponse(response);
			}
			if (message instanceof Request) {
				Request request = (Request) message;
				return createTargetFromRequest(request);
			}
		} catch (SipParseException e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug("Error: createTargetFromMessage - could not create target due to exception: " + e.getMessage());
			}
		}
		return null;
	}

	private static SipURL createTargetFromResponse(Response response)
			throws IllegalArgumentException, SipParseException {
		
		int port;
		String host;
		String transport;
		String scheme;
		
		ViaHeader topVia = (ViaHeader) response.getHeader(ViaHeader.name,true);
		AddressFactory addressFactory = SipJainFactories.getInstance()
				.getAddressFactory();

		//we determine the host
		host = topVia.getHost();
		
		//we determine the transport
		transport = topVia.getTransport();
		
		//we determine the scheme
		scheme = SipStackUtil.transportToScheme(transport);
		
		//we determine the port
		//if the host is a host name, we take the port as is (even if missing)
		if (isHostName(host)){
			port = topVia.getPort(); 
		}else{
			//if the host is an ip address there are 2 options:
			//port appears in Via header - we take it
			//port doesn't appear in Via header - we take the default port of transport
			port = SipStackUtil.getPortFromTopVia(topVia, false);
		}
		
		//creating the url
		SipURL resultUrl = addressFactory.createSipURL(host);
		if (port > 0){
			resultUrl.setPort(port);
		}
		resultUrl.setTransport(topVia.getTransport());
		resultUrl.setScheme(scheme);

		return resultUrl;
	}

	/**
	 * this method determines which port to use by the top Via header
	 * if the Via Header contains a port, this will be used.
	 * 
	 * If it doesn't, the port is selected to be the default
	 * port according to the transport found in the Via Header
	 * 
	 * @param topVia the top Via header field value
	 * @param mindRport, if true, and the "rport" parameter is present,
	 *  it is used. if false, the "rport" is ignored.
	 * @return the top Via port number
	 */
	public static int getPortFromTopVia(ViaHeader topVia, boolean mindRport) {
		int rport = mindRport
			? topVia.getRPort()
			: 0;
		int aliasPort = rport > 0
			? rport
			: topVia.getPort();
		if (aliasPort < 0) {
			String transport = topVia.getTransport();
			aliasPort =
				transport != null &&
				transport.equalsIgnoreCase(ListeningPointImpl.TRANSPORT_TLS)
					? 5061
					: 5060;
		}
		return aliasPort;
	}

	/**
	 * this method determines if a message has the received
	 * tag set by the cotainer in its top via header
	 * 
	 * @param message
	 * @return true if it has the tag and false otherwise
	 */
	public static boolean  topViaHasReceivedTag(Message message){
		ViaHeader topVia;
		try {
			topVia = (ViaHeader) message.getHeader(ViaHeader.name,true);
			return topVia.hasReceived();
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * determines if give string is a host name or an IP address
	 * @param address string to evaluate
	 * @return true if host name, false if IP address
	 */
	public static boolean isHostName(String address) {
		char hint = address.charAt(0);
	
		switch (hint) {
		case '0': case '1': case '2': case '3': case '4':
		case '5': case '6': case '7': case '8': case '9':
			// definitely IP address
			return false;
		case ':': case '[':
			// definitely IPv6 address
			return false;
		case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
			// either IPv6 or hostname
			return address.indexOf(':') == -1;
		default:
			// definitely host name
			return true;
		}
	}
	
	/**
	 * this method fixes the request uri and top route as per RFC 3261 26.2.2:
	 * Ensure that target is correct. If there is sips scheme - transport
	 * should be tcp, as tls is deprecated in RFC 3261 26.2.2:
	 * "The use of transport=tls has consequently been deprecated,partly
	 * because it was specific to a single hop of the request"
	 * 
	 * @param request
	 * @throws IllegalArgumentException
	 * @throws SipParseException 
	 */
	public static void fixHeaders(Message message) throws IllegalArgumentException, SipParseException {
		URI uriToSend;
		
		//fix top route
		RouteHeader topRoute = (RouteHeader) message.getHeader(RouteHeader.name, true);
		if (topRoute != null) {
			uriToSend = topRoute.getNameAddress().getAddress();
			if (uriToSend != null){
				fixTargetSipUri(uriToSend);
			}
		}
		
		if (message instanceof Request) {
			Request req = (Request) message;
			//fix request uri
			uriToSend = req.getRequestURI();
			if (uriToSend != null){
				fixTargetSipUri(uriToSend);
			}
		}
	}
	
	public static SipURL fixTargetSipUri(URI uriToSend) {
		SipURL sipUrlToSend = null;
		
		if (uriToSend instanceof SipURL){
			sipUrlToSend = (SipURL) uriToSend;
		}else{
			return null;
		}

		try {

			// Ensure that target is correct. If there is sips scheme -
			// transport
			// should be tcp, as tls is deprecated in RFC 3261 26.2.2:
			// "The use of transport=tls has consequently been deprecated,
			// partly
			// because it was specific to a single hop of the request"
			String scheme = sipUrlToSend.getScheme();

			if (scheme != null) {
				String transport = sipUrlToSend.getTransport();
				if (scheme.equalsIgnoreCase(SIPS_SCHEME)) {
					if (transport != null) {
						if (transport
								.equalsIgnoreCase(ListeningPoint.TRANSPORT_UDP)) {
							// sips and udp - cannot send securely over udp
							throw new AmbiguousUriException(sipUrlToSend
									.toString());
						}
						if (!transport
								.equalsIgnoreCase(ListeningPoint.TRANSPORT_TCP)) {
							try {
								sipUrlToSend
										.setTransport(ListeningPoint.TRANSPORT_TCP);
							} catch (SipParseException e) {
								throw new IOException(e.getMessage());
							}
						}
					}
				} 
				//PK89478 - the else section was removed - we should not change secured URIs to non-secured URIs:
				//incase of sip scheme we should not change TLS to TCP
			}

		} catch (Exception e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger
						.traceDebug("Exception ocurred while trying to fix uri: "
								+ uriToSend);
			}
		}
		return sipUrlToSend;

	}

	/**
	 * @param message a request message
	 * @return true if this request is sent out-of-dialog. that is, any request
	 *  without a To tag. this is needed to determine whether or not this type
	 *  of request allows "transport parameter fixing" (changing the transport
	 *  parameter from "tls" to "tcp"). such a change is only allowed for
	 *  initial requests. subsequent requests (sent within a dialog) copy
	 *  the URI (either in the request URI or in the Route header) from an
	 *  earlier message that was received from peer elements on the initial
	 *  transaction, and must not get changed.
	 */
	public static boolean isOutOfDialogRequest(Message message) {
		boolean hasTag;
		try {
			hasTag = message.getToHeader().hasTag();
		}
		catch (Exception e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(
					c_logger.getClass(),
					"isOutOfDialogRequest",
					"error in To header",
					e);
			}
			hasTag = false;
		}
		return !hasTag;
	}

	/**
	 * checks whether the incoming request is the proprietary STARTUP message
	 * sent from the SLSP. 
	 * @param request incoming request to test
	 * @return true if the request is the proprietary STARTUP message
	 */
	public static boolean isSlspStartup(Request request) {
		// 1. the request method
		// 2. no username in the request-URI
		// 3. a single Via header
		// 4. Max-Forwards is 0

		try {
			String method = request.getMethod();
			if (!method.equals("STARTUP")) {
				return false;
			}
			URI uri = request.getRequestURI();
			if (!(uri instanceof SipURL)) {
				return false;
			}
			SipURL url = (SipURL)uri;
			if (url.hasUserName()) {
				return false;
			}
			ViaHeader topVia = (ViaHeader)request.getHeader(ViaHeader.name, true);
			ViaHeader botVia = (ViaHeader)request.getHeader(ViaHeader.name, false);
			if (topVia != botVia) {
				return false;
			}
			MaxForwardsHeader maxForwardsHeader;
			try {
				maxForwardsHeader = request.getMaxForwardsHeader();
			}
			catch (HeaderParseException e) {
				return false;
			}
			if (maxForwardsHeader == null || maxForwardsHeader.getMaxForwards() != 0) {
				return false;
			}
			return true;
		}
		catch (SipParseException e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(SipStackUtil.class.getName(),
					"isSlspStartup", "parse error", e);
			}
			return false;
		}
	}

	/**
	 * determines if the extension is listed as a supported value
	 * in the given message.
	 * see 3261-8.1.1.9
	 *
	 * @param message the message to inspect
	 * @param optionTag the extension in question
	 * @return true if supported, false otherwise
	 */
	public static boolean isSupported(MessageImpl message, String optionTag) {
		HeaderIterator i = message.getHeaders(SupportedHeader.name);
		if (i == null) {
			return false;
		}
		while (i.hasNext()) {
			try {
				SupportedHeader supported = (SupportedHeader)i.next();
				String supportedOptionTag = supported.getOptionTag();
				if (supportedOptionTag.equalsIgnoreCase(optionTag)) {
					return true;
				}
			}
			catch (Exception e) {
				if (c_logger.isTraceFailureEnabled()) {
					c_logger.traceFailure(SipStackUtil.class, "isSupported", "", e);
				}
			}
		}
		return false;
	}
	
	/**
	 * adding random To-Tag to responses if to-tag does not exist.
	 * this is used for automatic error response creation by the container/stack
	 * according to the RFC error response should include to tag.
	 * RFC3261 12.1.1
	 * 
	 * @param response
	 */
	public static void addToTag(Response response){
		if (c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(SipStackUtil.class, "addToTag");
		}
		//if this is an initial request (does not have a to tag) we need to add the to tag
		//according to RFC3261 12.1.1
		ToHeader toHeader = response.getToHeader();
		String toTag = null;
		if (toHeader != null){
			toTag = toHeader.getTag();
		}else{
			if (c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug(SipStackUtil.class, "addToTag", "To-header is not found on the response");
			}
		}
		
		if (toHeader != null && (toTag == null || toTag.length() == 0)){
			//add to tag to the response before sending the error response
			String tag = null;
			//generate a random number tag
			StringBuilder builder = new StringBuilder();
			builder.append(Math.random());
			//trim the 0. from the tag
			tag = builder.substring(2);
			try {
				toHeader.setTag(tag);
				if (c_logger.isTraceDebugEnabled()){
					c_logger.traceDebug(SipStackUtil.class, "addToTag", "To-Tag was added to an error initial request, tag:" + tag);
				}
			} catch (IllegalArgumentException e) {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(SipStackUtil.class, "Exception in addToTag", e.getMessage());
				}
			} catch (SipParseException e) {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(SipStackUtil.class, "Exception in addToTag", e.getMessage());
				}
			}
		}else{
			if (c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug(SipStackUtil.class, "addToTag", "To-Tag was found, no need to add to-tag, tag:" + toTag);
			}
		}
	}
}
