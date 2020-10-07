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
import jain.protocol.ip.sip.SipEvent;
import jain.protocol.ip.sip.SipException;
import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.SipProvider;
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.address.URI;
import jain.protocol.ip.sip.header.CallIdHeader;
import jain.protocol.ip.sip.header.Header;
import jain.protocol.ip.sip.header.HeaderIterator;
import jain.protocol.ip.sip.header.HeaderParseException;
import jain.protocol.ip.sip.header.MaxForwardsHeader;
import jain.protocol.ip.sip.header.NameAddressHeader;
import jain.protocol.ip.sip.header.RetryAfterHeader;
import jain.protocol.ip.sip.header.ViaHeader;
import jain.protocol.ip.sip.message.Message;
import jain.protocol.ip.sip.message.MessageFactory;
import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.jain.protocol.ip.sip.ListeningPointImpl;
import com.ibm.ws.jain.protocol.ip.sip.SipJainFactories;
import com.ibm.ws.jain.protocol.ip.sip.header.ViaHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.message.MessageImpl;
import com.ibm.ws.jain.protocol.ip.sip.message.RequestImpl;
import com.ibm.ws.jain.protocol.ip.sip.message.ResponseImpl;
import com.ibm.ws.jain.protocol.ip.sip.message.SipResponseCodes;
import com.ibm.ws.sip.container.pmi.PerformanceMgr;
import com.ibm.ws.sip.parser.MessageParser;
import com.ibm.ws.sip.properties.StackProperties;
import com.ibm.ws.sip.stack.context.MessageContext;
import com.ibm.ws.sip.stack.context.MessageContextFactory;
import com.ibm.ws.sip.stack.transaction.SIPTransactionConstants;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack.UACommLayer;
import com.ibm.ws.sip.stack.transaction.common.BadRequestException;
import com.ibm.ws.sip.stack.transaction.transactions.SIPTransaction;
import com.ibm.ws.sip.stack.transaction.transactions.ct.SIPClientTranaction;
import com.ibm.ws.sip.stack.transaction.transactions.st.SIPServerTransaction;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPListenningConnection;
import com.ibm.ws.sip.stack.transaction.transport.connections.SipMessageByteBuffer;
import com.ibm.ws.sip.stack.transaction.transport.connections.udp.SIPListenningConnectionImpl;
import com.ibm.ws.sip.stack.transaction.transport.routers.SLSPRouter;
import com.ibm.ws.sip.stack.transaction.util.ApplicationProperties;
import com.ibm.ws.sip.stack.transaction.util.SIPStackUtil;
import com.ibm.ws.sip.stack.util.SipStackUtil;
import com.ibm.ws.sip.stack.util.StackExternalizedPerformanceMgr;
import com.ibm.ws.sip.stack.util.ThreadLocalStorage;

/**
 * @author Amirk
 */
public class TransportCommLayerMgr
{
	
	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(TransportCommLayerMgr.class);
	
	//model for connections
	private SIPConnectionsModel m_connectionsModel;
	
	// false, until we get the first STARTUP message from the SLSP
	private boolean m_startupReceived;

	//router for messages
	private SLSPRouter m_router;
	
	//loopback address Q
	private LoopBackAddressThread m_loopBackAddressQ;

	/** the IBM MTU parameter name*/
	private static final String IBM_MTU_PARAM = "ibmmtu";
	
	/** The "IBM-PO" header name */
	private static final String IBM_PO_HEADER = "IBM-PO";
	
	/**
	 * The "ibmsid" parameter. 
	 */
	private static final String IBM_SID_PARAM = "ibmsid";
	
	/** 
	 * Separator used in the ibmsid parameter.
	 */
	public static final char SESSION_ID_SEPARATOR = '_';
		
	/** singleton instance */
	private static TransportCommLayerMgr s_instance = new TransportCommLayerMgr();
	
	/** interface to singleton instance */
	public static TransportCommLayerMgr instance() {
		return s_instance;
	}
	
	/**
	 * private constructor
	 */
	private TransportCommLayerMgr()
	{
		m_connectionsModel = SIPConnectionsModel.instance();
		m_startupReceived = false;
		
		initRouter();
		
		m_loopBackAddressQ = new LoopBackAddressThread();
		Thread loopback = new Thread( m_loopBackAddressQ , "SIP Stack LoopBack Address Thread");
		loopback.start();
	}
	
	/**
	 * init router object
	 */
	private void initRouter()
	{
		m_router = SLSPRouter.getInstance();
			
		String outproxy =  ApplicationProperties.getProperties().
			getString( StackProperties.OUTBOUND_PROXY );
		
		try
		{
			//check if the router has an outbound proxy
				
			if( !outproxy.equals(StackProperties.OUTBOUND_PROXY_DEFAULT) && outproxy.trim().length() > 0)
			{
				SipURL proxy = SIPStackUtil.parseNameAdressFromConfig( outproxy );
				Hop proxyHop = Hop.getHop( proxy );
				m_router.setOutboundProxy( proxyHop );
			}					
		}
		catch( SipParseException exp )
		{
			if( c_logger.isTraceDebugEnabled())
			{
				c_logger.traceDebug(this,"TransportCommLayerMgr","could not parse outbound proxy:" + outproxy + " set to null");
			}
		}		
	}
	
	/** send the message to the transport */
	public void sendMessage(MessageContext sipMsgContext, SipProvider provider, SIPConnection sipConnection)
		throws SIPTransportException
	{
		sendMessage(sipMsgContext, provider, sipConnection, null);
	}

	/** send the message to the transport */
	public void sendMessage(
		MessageContext messageContext, SipProvider provider, SIPConnection connection,
		SIPTransaction transaction)
		throws SIPTransportException
	{
		MessageImpl msg = (MessageImpl)messageContext.getSipMessage();
		try {
			if (msg.isLoopback()) {
				//if this is for loopback , send it to the loopback q and exit
				LoopBackMessage loopBackMessage = new LoopBackMessage(msg, provider, connection);
				m_loopBackAddressQ.add(loopBackMessage);
				MessageContext.doneWithContext(messageContext);
				return;
			}
			boolean connectionNotExists = connection == null;
			boolean connectionAlreadyClosed = connectionNotExists ? false : connection.isClosed();
				
			
			
			if (connectionNotExists || !connection.isReliable() || connectionAlreadyClosed) {
				// if there is no connection, or we are on UDP, or the connection is closed - create a new one
				try {
					
					connection = getConnection(messageContext.getSipMessage(), transaction, provider);
					if(!connectionNotExists && connectionAlreadyClosed) {
						transaction.setTransportConnection(connection);
					}

				
				}
				catch (FlowFailedException e) {
					// no existing connection for sending request by flow token
					handleFlowFailed((Request)msg, provider, transaction);
					return;
				}
				catch (FlowTamperedException e) {
					// no existing connection for sending request by flow token
					handleFlowTampered((Request)msg, provider, transaction);
					return;
				}
			}
			if (connection == null) {
				SIPTransportException ex = new SIPTransportException("No connection for sending the message"); 
				// the catcher is responsible for cleaning up the messageContext
				throw ex;
			}
			if (c_logger.isTraceDebugEnabled()) {
				String msgString = messageContext.getSipMessage().toString();
				c_logger.traceDebug( this,
					"TransportCommLayerMgr","\r\nOut Message:\r\n" + 
					msgString);
			}
			
			if (SIPTransactionStack.instance().getConfiguration().isTraceOutMsg()) {
				String msgString = messageContext.getSipMessage().toString();
				System.out.println("Out Message:\r\n" + msgString);
			}

			//get the use compact headers string custom property
			String useCompactHeadersStrValue = ApplicationProperties.getProperties().getString(StackProperties.COMPACT_HEADERS);
			//get the actaul enum value of when to use the compact headers
			UseCompactHeaders useCompactHeaders = UseCompactHeaders.fromString(useCompactHeadersStrValue); 
			// be sensitive to the MTU, if this is a request on UDP,
			// which did not fall back to UDP
			boolean considerMtu = messageContext.getSipMessage().isRequest()
				&& !connection.isReliable()
				&& !messageContext.transportSwitched();
			
			//In standalone mode, remove the IBM-Destination header.
			if (m_router.isStandAloneMode()) {
				messageContext.getSipMessage().removeHeader(SipStackUtil.DESTINATION_URI, true);
			}
			try {
				messageContext.setSipConnection(connection);
				// write message to the connection
				connection.write(messageContext, considerMtu, useCompactHeaders);
			}
			catch (PathMtuExceeded pathMtuExceeded) {
				// don't inspect any state in the exception, it's a singleton instance
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(
						this,
						"sendMessage",
						"request is too large for MTU of size ["
							+ SIPListenningConnectionImpl.getPathMTU() + ']');
				}

				// switch from UDP to TCP
				connection = handlePathMtuExceeded(messageContext, connection, provider);
				try {
					connection.write(messageContext, false, useCompactHeaders);
				}
				catch (IOException e) {
					messageContext.writeError(e);
					// give up
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(
							this,
							"sendMessage",
							"failed sending out large request",
							e);
					}
				}
			}
			catch (IOException e) {
				throw new SIPTransportException(e.getMessage());
			}
		}
		catch (SipException e) {
			throw new SIPTransportException(e.getMessage());
		}
	}
	
	/**
	 * gets a connection for sending out the given message
	 * @param message message to send out
	 * @param transaction transaction sending the message. may be null.
	 * @return the connection for sending out the message, null on error
	 * @throws SipException 
	 * @throws FlowFailedException if message is a request, and the
	 *  IBM-Destination in the request contains the "ibm-ob" parameter, and
	 *  there is no connection matching the IBM-Destination flow token.
	 */
	private SIPConnection getConnection(Message message,
		SIPTransaction transaction, SipProvider provider)
		throws SipException
	{
		SIPConnection connection;
		try {
			if (message.isRequest()) {
				Request request = (Request)message;
				SIPClientTranaction clientTransaction = (SIPClientTranaction)transaction;
				connection = getRequestConnection(request, clientTransaction, provider);
			}
			else {
				Response response = (Response)message;
				SIPServerTransaction serverTransaction = (SIPServerTransaction)transaction;
				connection = getResponseConnection(response, serverTransaction, provider);
			}
		}
		catch (IOException e) {
			// could not create the connection
			if (c_logger.isTraceDebugEnabled()) {
				StringBuffer str = new StringBuffer("could not create connection. ");
				str.append(" reason [");
				str.append(e.getMessage());
				str.append("]\nMessage:");
				str.append(message);							
				c_logger.traceDebug(this, "sendMessage", str.toString(), e); 
            }
			connection = null;
		}
		return connection;
	}
	
	/**
	 * gets a connection for sending out the given request
	 * @param request request to send out
	 * @param transaction client transaction. may be null.
	 * @return the connection for sending out the request, null on error
	 * @throws SipException
	 * @throws IOException
	 * @throws FlowFailedException if the IBM-Destination in the request
	 *  contains the "ibm-ob" parameter, and there is no connection matching
	 *  the IBM-Destination flow token.
	 */
	private SIPConnection getRequestConnection(Request request,
		SIPClientTranaction transaction, SipProvider provider)
		throws SipException, IOException
	{
		// throw an exception if request is invalid
		validateOutgoingRequest(request);

		// determine whether this request is bound to a specific connection
		boolean outboundExtension;
		NameAddressHeader destination = (NameAddressHeader)request.getHeader(SipStackUtil.DESTINATION_URI, true);
		if (destination == null) {
			outboundExtension = false;
		}
		else {
			URI destinationURI = destination.getNameAddress().getAddress();
			SipURL destinationSipURL = (SipURL)destinationURI;
			outboundExtension = destinationSipURL.hasParameter(SipStackUtil.IBM_OB_PARAM);
			if (outboundExtension && destinationSipURL.hasParameter(SipStackUtil.IBM_TAMPERED_PARAM)) {
				// the "outbound" extension logic flagged this flow as tampered,
				// and this is the right place to generate an error response.
				FlowTamperedException.throwIt();
			}
		}

		// the next synchronized block is querying/updating the transaction with
		// the "hop" for sending request retransmissions. this is synchronized
		// for the (rare) case where the original request thread is updating the
		// transaction hop, and removing the IBM-Destination header, while the
		// retransmission (timer) thread is querying the hop.
		Hop nextHop;
		synchronized (this) {
			// determine the original hop. this is null in the first transmission.
			Hop originalHop = transaction == null ? null : transaction.getHop();
			
			// consult the router
			nextHop = m_router.getNextHop(request, originalHop);
			// now we know exactly where this request is going to

			// update the transaction for potential retransmissions in the future
			if (transaction != null && originalHop == null) {
				transaction.setHop(nextHop);
			}
		}
		
		// get a connection to the hop
		String transport = nextHop.getTrasport();
		boolean create = !outboundExtension
			|| transport.equalsIgnoreCase(ListeningPoint.TRANSPORT_UDP);
		ListeningPoint listeningPoint = getRequestListeningPoint(
			request, provider, nextHop, outboundExtension);
		SIPConnection connection = getHopConnection(
			nextHop, request, provider, listeningPoint, create);

		if (connection == null) {
			if (outboundExtension) {
				// RFC 5626-5.3.1:
				// "If the flow no longer exists, the proxy SHOULD
				// send a 430 (Flow Failed) response to the request"
				if (c_logger.isTraceFailureEnabled()) {
					c_logger.traceFailure(this, "getRequestConnection",
						"no connection for outbound extension request");
				}
				FlowFailedException.throwIt();
			}
		}
		else {
			// if the provider was changed, for any reason, update the top Via
			ListeningPoint lp = connection.getSIPListenningConnection().getListeningPoint();
			ViaHeader topVia = (ViaHeader)request.getHeader(ViaHeader.name, true);
			if (!equals(topVia, lp)) {
				// need to modify the Via
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "getRequestConnection",
						"changing Via from [" + topVia
						+ "] to [" + lp + ']');
				}
				modifyVia(topVia, lp);
			}
		}
		return connection;
	}

	/**
	 * gets the local listening point to use as the source address when
	 * sending out a request
	 * @param request request to send out
	 * @param provider the local SipProvider. may be null.
	 * @param nextHop the request hop, as calculated by the router
	 * @param outboundExtension true if this request is targeted to an RFC 5626
	 *  ("outbound" protocol extension) user agent.
	 * @throws FlowFailedException if the IBM-Destination in the request
	 *  contains the "ibm-ob" parameter, and there is no connection matching
	 *  the IBM-Destination flow token.
	 * @return the local listening point to send the request from, or null
	 *  if this request may be sent from any local listening point
	 * @throws FlowFailedException if outboundExtension is true and this is
	 *  standalone deployment and there is no matching local listening point.
	 * @throws HeaderParseException if a header value in the message is invalid
	 */
	private ListeningPoint getRequestListeningPoint(Request request,
		SipProvider provider, Hop nextHop, boolean outboundExtension)
		throws FlowFailedException, HeaderParseException
	{
		ListeningPoint listeningPoint;
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "getRequestListeningPoint", " provider = " + provider + " outboundExtension = " + outboundExtension);
		}
		if (isStandAloneMode()) {
			// in standalone, find a listening point that matches the IBM-PO
			Header ibmPO = request.getHeader(IBM_PO_HEADER, true);
			if (ibmPO == null) {
				if (SIPTransactionStack.instance().getConfiguration().getSentByHost() != null) {
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "getRequestListeningPoint","no ibmPO but using getSentByHost, return null ");
					}
					return null;
				}
				listeningPoint = null;
			}
			else {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "getRequestListeningPoint",
							"ibmPO = " + ibmPO + " provider = " + provider + " request = " + request );
				}
				// send from a specific local listening point
				request.removeHeader(IBM_PO_HEADER, true);
				String po = ibmPO.getValue();
				String transport = nextHop.getTrasport();
				int index;
				try {
					index = Integer.parseInt(po);
					listeningPoint = m_connectionsModel.getListeningPoint(index, transport);
				}
				catch (NumberFormatException e) {
					if (c_logger.isTraceFailureEnabled()) {
						c_logger.traceFailure(this, "getRequestListeningPoint", "", e);
					}
					listeningPoint = null;
				}
			}
			if (outboundExtension && listeningPoint == null) {
				// "ibm-ob" specified and no matching listening point in standalone
				if (c_logger.isTraceFailureEnabled()) {
					c_logger.traceFailure(this, "getRequestListeningPoint",
						"no listening point for outbound extension request");
				}
				FlowFailedException.throwIt();
			}
		}
		else {
			// in cluster, use any local listening point
			listeningPoint = null;
		}
		
		if (listeningPoint == null && provider != null &&
				SIPTransactionStack.instance().getConfiguration().strictOutboundLocalPort())
		{
			// configuration forces sending a request from specific provider
			listeningPoint = provider.getListeningPoint();
		}
		
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "getRequestListeningPoint",
					"return listeningPoint = " + listeningPoint);
		}
		return listeningPoint;
	}
	
	/**
	 * compares a Via header with a listening point
	 * @param via the Via header
	 * @param lp the listening point
	 * @return true if they are equal
	 */
	private static boolean equals(ViaHeader via, ListeningPoint lp) {
		String viaTransport = via.getTransport();
		String lpTransport = lp.getTransport();
		if (!viaTransport.equalsIgnoreCase(lpTransport)) {
			return false;
		}
		String lpHost = lp.getSentBy();
		String viaHost = via.getHost();
		if (!viaHost.equalsIgnoreCase(lpHost)) {
			return false;
		}
		int lpPort = lp.getPort();
		int viaPort = via.getPort();
		if (viaPort == -1) {
			viaPort = viaTransport.equalsIgnoreCase(ListeningPointImpl.TRANSPORT_TLS)
				? 5061
				: 5060;
		}
		if (viaPort != lpPort) {
			return false;
		}
		return true;
	}

	/**
	 * modifies the given Via's transport, host, and port, to match the given
	 * listening point
	 * @param via the Via header value to modify
	 * @param lp the listening point
	 * @throws SipParseException 
	 */
	private static void modifyVia(ViaHeader via, ListeningPoint lp)
		throws SipParseException
	{
		String viaTransport = via.getTransport();
		String lpTransport = lp.getTransport();
		if (!viaTransport.equalsIgnoreCase(lpTransport)) {
			via.setTransport(lpTransport.toUpperCase());
		}
		String lpHost = lp.getSentBy();
		String viaHost = via.getHost();
		if (!viaHost.equalsIgnoreCase(lpHost)) {
			via.setHost(lpHost);
		}
		int lpPort = lp.getPort();
		int viaPort = via.getPort();
		if (viaPort == -1) {
			viaPort = viaTransport.equalsIgnoreCase(ListeningPointImpl.TRANSPORT_TLS)
				? 5061
				: 5060;
		}
		if (viaPort != lpPort) {
			via.setPort(lpPort);
		}
	}
	
	/**
	 * gets a connection for sending out the given response
	 * @param response response to send out
	 * @param transaction server transaction. may be null.
	 * @param provider the provider that is attempting to send the response
	 * @return the connection for sending out the response, null on error
	 * @throws SipException
	 * @throws IOException
	 */
	private SIPConnection getResponseConnection(Response response,
			SIPServerTransaction transaction, SipProvider provider)
			throws SipException, IOException {
		
		SIPConnection connection;
		synchronized (m_connectionsModel) {
			Hop nextHop = m_router.getNextHop(response);
			if (nextHop == null) {
				// router.getNextHop failed and logged an error message
				return null;
			}
			ListeningPoint listeningPoint = provider == null
					|| !SIPTransactionStack.instance().getConfiguration().strictOutboundLocalPort() ? null
					: provider.getListeningPoint();
			connection = m_connectionsModel.getConnection(listeningPoint,
					nextHop);
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "getResponseConnection",
						"listeningPoint [" + listeningPoint + "] connection ["
								+ connection + "] nexthop [" + nextHop + ']');
			}
			if (connection == null) {
				// if creating a connection, the client is listening on the
				// "sent-by" port, not the rport
				String transport = nextHop.getTrasport();
				if (!transport.equals("UDP")) {
					ViaHeader topVia = (ViaHeader) response.getHeader(
							ViaHeader.name, true);
					int rport = topVia.getRPort();
					if (rport != -1 && nextHop.getPort() == rport) {
						int port = topVia.getPort();
						if (port == -1) {
							port = transport.equals("TLS") ? 5061 : 5060;
						}
						nextHop.setPort(port);

						// we changed the nextHop port so we need to figure out
						// again if this connection
						// already exists in the maps before trying to create a
						// new connection
						connection = m_connectionsModel.getConnection(
								listeningPoint, nextHop);
						if (c_logger.isTraceDebugEnabled()) {
							c_logger.traceDebug(this, "getResponseConnection",
									"connection lookup after replacing the port of the next hop, "
											+ "listeningPoint ["
											+ listeningPoint + "] connection ["
											+ connection + "] nexthop ["
											+ nextHop + ']');
						}
					}
				}
			}
			if (connection == null) {
				// before creating a connection, try to use the same local
				// listening
				// point that received the original request. do not use the same
				// connection instance - create a new instance to allow
				// round-robin
				// of SLSPs in case this container is part of a cluster.
				ListeningPoint lp = null;
				if (transaction != null) {
					SIPConnection originalConn = transaction
							.getTransportConnection();
					if (originalConn != null) {
						lp = originalConn.getSIPListenningConnection()
								.getListeningPoint();
					}
				}
				if (lp == null) {
					// no listening point, just use a default one for this
					// transport
					lp = getDefaultListenningPoint(response);
					if (lp == null) {
						return null; // probably a bad top Via in the response
					}
				}
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "getResponseConnection",
							"Try to get connection for, listening point: " + lp
									+ " ,nexthop: " + nextHop);
				}

				connection = m_connectionsModel.getConnection(lp, nextHop);

				if (connection == null) {
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "getResponseConnection",
								"Create new connection for, listening point: "
										+ lp + " ,nexthop: " + nextHop);
					}
					connection = createConnection(nextHop, lp);
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "getResponseConnection",
								"new connection was created, "
										+ " listeningPoint [" + listeningPoint
										+ "] nexthop [" + nextHop
										+ "] connection [" + connection + ']');
					}
				}

			}
		}
		return connection;
	}
	
	/**
	 * 
	 * @param hop hop to connect to
	 * @param request the request to send
	 * @param listeningPoint the local listening point from which to create
	 *  the connection, null for any listening point
	 * @param create true to create a connection if one does not exist,
	 *  false to only return existing connections
	 * @return the connection, or null if create=false and there is no
	 *  existing connection to the hop
	 * @throws IOException - create=true and no connection was created
	 */
	private SIPConnection getHopConnection(Hop hop, Request request,
		SipProvider provider, ListeningPoint listeningPoint, boolean create)
		throws IOException
	{
		if (hop == null) {
			throw new IOException("No hop for sending request");
		}
		// connection to return
		SIPConnection connection;
		
		try {
			synchronized (m_connectionsModel) {
				// synchronize between get and create connection, to avoid
				// a race between 2 threads attempting to create a connection
				// to the same target.
				connection = m_connectionsModel.getConnection(listeningPoint, hop);
				if (create && connection == null) {
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(
							this,
							"getHopConnection",
							"creating connection to [" + hop + ']');
					}
					if(ApplicationProperties.getProperties().getBoolean(StackProperties.CREATE_CONNECTION_USE_LP_FROM_OUTBOUND)){
						
						if(listeningPoint == null){
							listeningPoint = provider.getListeningPoint();
							if (c_logger.isTraceDebugEnabled()) {
								c_logger.traceDebug(
									this,
									"getHopConnection",
									"replace NULL LP with LP from Provider  " + provider);
							}	
						}
						if (c_logger.isTraceDebugEnabled()) {
							c_logger.traceDebug(
								this,
								"getHopConnection",
								"creating connection from  [" + listeningPoint + ']' + " to [" + hop + ']');
						}
						connection = createConnection(hop, listeningPoint );
					}
					else{
						ListeningPoint lp = provider.getListeningPoint();
						connection = createConnection(hop, lp );
					}
					//if there is no exception here, the connection was created
				}
			}
		}
		catch (IOException e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(
					this,
					"getHopConnection",
					"cannot create connection to [" + hop + ']',
					e);
			}
			connection = null;
		}
		
		// no connection created, throw the exception
		if (create && connection == null) {
			throw new IOException("could not connect to [" + hop + ']');
		}
		
		return connection;
	}
	
	
	
	/**
	 * create a connection from the local listening point , to the next hop
	 * @param nextHop - where to connect to
	 * @param lp - from where
	 * @return SIPConnection - transport connection
	 * @throws BadRequestException - 
	 * @throws IOException - thrown when connection could not be established
	 * @throws SipParseException - thrown if a parse exception occurs
	 */
	public SIPConnection createConnection( Hop nextHop , ListeningPoint lp ) 
		throws IOException
	{
		String host = nextHop.getHost();
		int port = nextHop.getPort();
		if (port < 0 || port > 0xFFFF) {
			// IOException instead of IllegalArgumentException
		    throw new IOException("port out of range: " + port);
		}
		SIPConnection connection = m_connectionsModel.createConnection(lp, host, port);
		connection.setOutbound(true);
		
		//add to model
		Hop key;
		String transport = lp.getTransport();
		if (nextHop.getTrasport().equalsIgnoreCase(transport)) {
			key = nextHop;
		}
		else {
			// destination transport is different than source
			key = new Hop(transport, nextHop.getHost(), nextHop.getPort());
		}
		connection.setKey(key);
		if (connection.isReliable()) {
			m_connectionsModel.addConnection(connection);
		}
		
		//create the socket
		try {
			connection.connect();
		}
		catch (IOException e) {
			// the attempt to connect failed immediately (synchronously)
			// so we will not get a call to onConnectionClosed()
			if (connection.isReliable()) {
				m_connectionsModel.removeConnection(connection);
			}
			throw e;
		}
		
		//start listenning
		connection.start();
		
		//Amirp for performance reasons use debug instead of info message
		if(c_logger.isTraceDebugEnabled())
        {
			c_logger.traceDebug(this, "createConnection",
				"from [" + lp + "] to [" + nextHop.toString()+ ']');
        }

		return connection;				
	}
	
	/**
	 * called after determining that a large request needs to be sent out on UDP,
	 * to switch the transport from UDP to TCP, per rfc 3261-18.1.1
	 * @param messageContext the large message to send out
	 * @param connection the UDP connection
	 * @param udpProvider the UDP SipProvider
	 * @return the TCP connection to use, or UDP if cannot establish TCP
	 * @throws SipParseException if there is no Via header in the message (?)
	 */
	private SIPConnection handlePathMtuExceeded(MessageContext messageContext,
		SIPConnection connection, SipProvider udpProvider)
		throws SipParseException
	{
		Message message = messageContext.getSipMessage();
		Hop nextHop = connection.getKey();
		if (nextHop.getTrasport().equals("UDP")) {
			nextHop = m_router.switchTransport(nextHop);
		}
		String newTransport = nextHop.getTrasport();
		if (!newTransport.equals("UDP")) {
			// 3261-18.1.1
			// If this causes a change in the transport protocol from the
			// one indicated in the top Via, the value in the top Via MUST be
			// changed.

			// clone the message before modifying the top Via header. we don't
			// want to touch the transaction's copy, because that would cause
			// the timers (A and E) to retransmit a TCP message on a separate
			// MessageContext which doesn't know about the switch, and, if TCP
			// fails, would notify of failure instead of falling back to UDP.
			boolean standalone = isStandAloneMode();
			if (standalone) {
				message = (Message)message.clone();
				messageContext.setSipMessage(message);
			}
			ViaHeader topVia = (ViaHeader)message.getHeader(ViaHeader.name, true);
			if (topVia.getTransport().equalsIgnoreCase("UDP")) {
				topVia.setTransport(newTransport);
			}
			
			//The value of the IBM-Destination transport will also changed according to the new transport
			NameAddressHeader destination = (NameAddressHeader) message.getHeader(SipStackUtil.DESTINATION_URI, true);
			if (destination != null){
				//get the transport from the destination header
				URI detinationUri = destination.getNameAddress().getAddress();
				if (!(detinationUri instanceof SipURL)) {
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug("Error: getTargetSipUri - IBM destination uri is not a SipURL");
					}
				}else{
					SipURL detinationSipUrl = (SipURL)detinationUri;
					if (detinationSipUrl.getTransport().equalsIgnoreCase("UDP")) {
						detinationSipUrl.setTransport(newTransport);
						if (c_logger.isTraceDebugEnabled()) {
							c_logger.traceDebug("IBM-Destination header transport value was changed to: " + newTransport);
						}
					}
				}
			}
			
			ListeningPoint udpListeningPoint = connection.getSIPListenningConnection().getListeningPoint();
			ListeningPointImpl tcpListeningPoint = null;
			if(!SIPTransactionStack.instance().getConfiguration().strictOutboundLocalPort()) {
				tcpListeningPoint = getTcpListeningPoint(udpListeningPoint); 
			}
				
			if (tcpListeningPoint == null) {
				// there are no TCP listening points
				IOException e = new IOException("TCP is disabled,strictOutboundLocalPort value = " 
						+ SIPTransactionStack.instance().getConfiguration().strictOutboundLocalPort());
				revertToUdp(topVia, newTransport, e);
				return connection;
			}
			
			SipProvider tcpProvider = tcpListeningPoint.getProvider();
			
			try {
				connection = getHopConnection(
					nextHop, (Request)message, tcpProvider, tcpListeningPoint, true);
				
				if (standalone) {
					// mark the message context as switching transport to TCP.
					// this is needed later in case TCP fails, so we know
					// that it needs to fall back to UDP.
					// this is only done in standalone, because in a cluster we
					// don't get a TCP failure.
					messageContext.transportSwitch();
				}
				else {
					//add "ibmmtu" parameter - this will be kept on the message
					//in case we are working with a Sip Proxy. 
					//if the message will fail there it will be returned as the body
					//of a PROXYERROR message. there the stack will be able to 
					//identify that this was switched due to MTU
					//its only relevant when running in clustered mode
					topVia.setParameter(IBM_MTU_PARAM, "");
				}

				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(
						this,
						"handlePathMtuExceeded",
						"transport successfully switched for large request");
				}
			}
			catch (IOException e) {
				revertToUdp(topVia,newTransport,e);
			}
		}
		return connection;
	}
	
	/**
	 * in this scenario the MTU switch to TCP failed
	 * and now we should retry to use UDP.
	 * 
	 * @param topVia - the top via header of the message
	 * @param e - the exception that cause TCP failure 
	 * @param newTransport - the new transport 
	 * 
	 * @throws SipParseException  
	 */
	private void revertToUdp(ViaHeader topVia, String newTransport, IOException e) throws SipParseException {
		// 3261-18.1.1
		// If an element sends a request over TCP because of these message size
		// constraints, and that request would have otherwise been sent over
		// UDP, if the attempt to establish the connection generates either an
		// ICMP Protocol Not Supported, or results in a TCP reset, the element
		// SHOULD retry the request, using UDP
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(
				this,
				"handlePathMtuExceeded",
				"failed establishing [" + newTransport + "] connection, falling back to fragmented UDP",
				e);
		}
		
		// connection remains UDP, and that's final
		topVia.setTransport("UDP");
	}

	/**
	 * called when switching transport from UDP to TCP due to a large request.
	 * finds a TCP listening point given a UDP listening point.
	 * this method tries to return a TCP listening point that listens to the
	 * same address and port number as the given UDP listening point.
	 * if no such ListeningPoint exists, it returns any TCP ListeningPoint arbitrarily.
	 * @param udpListeningPoint the given UDP listening point.
	 * @return the best matching TCP ListeningPoint, null if there are no TCP
	 *  ListeningPoints at all.
	 */
	private ListeningPointImpl getTcpListeningPoint(ListeningPoint udpListeningPoint) {
		String udpHost = udpListeningPoint.getHost();
		int udpPort = udpListeningPoint.getPort();
		List listeningPoints = m_connectionsModel.getListeningPoints();
		Iterator i = listeningPoints.iterator();
		ListeningPointImpl tcpListeningPoint = null;

		while (i.hasNext()) {
			ListeningPointImpl listeningPoint = (ListeningPointImpl)i.next();
			String transport = listeningPoint.getTransport();
			if (!transport.equalsIgnoreCase(ListeningPoint.TRANSPORT_TCP)) {
				continue;
			}
			tcpListeningPoint = listeningPoint;
			int tcpPort = tcpListeningPoint.getPort();
			String tcpHost = tcpListeningPoint.getHost();
			if (tcpPort == udpPort && tcpHost.equals(udpHost)) {
				return tcpListeningPoint; // found exact match
			}
		}
		return tcpListeningPoint; // did not find exact match
	}

	/**
	 * create the listenning connection
	 * @param lp - the listenning point
	 * @return - the listenning connection
	 * @throws IOException - if we cannot listen
	 */
	public synchronized SIPListenningConnection createSIPListenningConnection( ListeningPointImpl lp ) throws IOException
	{
		SIPListenningConnection listenConnection = m_connectionsModel.createSIPListenningConnection(lp);
		// No need, createSIPListenningConnection calls listen
		// listenConnection.listen();
		//listenConnection.addListener( this );
		return listenConnection;		 				
	}
	
	/**
	 * remove the listenning connection
	 * @param lp - the listenning point to stop
	 * @throws IOException - cannot stop listenning
	 */
	public synchronized void removeSIPListenningConnection( ListeningPointImpl lp )
	{
		m_connectionsModel.removeSIPListenningConnection(lp);		
	}
	
	/**
	 * local listenning point - trio of host , port and transport we are 
	 * listenning on 
	 * @return List - list of listenning point
	 */
	public List getListeningPoints()
	{
		return m_connectionsModel.getListeningPoints();
	}

	/**
	 * TODO - check if new 
	 * @param req
	 * @return ListeningPoint
	 * @throws BadRequestException
	 */
	ListeningPointImpl getDefaultListenningPoint( Message msg )
		throws BadRequestException
	{
		try
		{
			ViaHeader topVia = ( ViaHeader )msg.getHeader( ViaHeader.name , true );
			String transport = topVia.getTransport();			 
			ListeningPointImpl retVal = m_connectionsModel.getDefaultListenningPoint(transport);						
			return retVal;
		}
		catch( SipException exp )
		{
			throw new BadRequestException( "bad via header",SIPTransactionConstants.RETCODE_CLIENT_BAD_REQUEST );					
		}
	}

	/** validate message before sending */
	protected  void validateOutgoingRequest(Request sipRequest)
		throws BadRequestException
	{
		// MaxForward header
		if (!sipRequest.hasMaxForwardsHeader())
		{
			try
			{
				MaxForwardsHeader maxForwardHeader = SipJainFactories.getInstance().getHeaderFactory().createMaxForwardsHeader( 70 );
				sipRequest.addHeader( maxForwardHeader , true );
			}
			catch (SipParseException e)
			{
				throw new BadRequestException( Response.BAD_REQUEST );
			}
		}
	
		HeaderIterator headerIterator = sipRequest.getViaHeaders();
		if (headerIterator == null || !headerIterator.hasNext())
		{
			throw new BadRequestException(Response.BAD_REQUEST);
		}					
	}
	
	/**
	 * validates an incoming request
	 * @param request incoming request to be tested
	 * @throws BadRequestException if request is invalid
	 */
	private void validateIncomingRequest(Request request)
		throws BadRequestException
	{
		// check mandatory headers per RFC3261 8.1.1
		if (request.getCallIdHeader() == null) {
			throw new BadRequestException("Missing Call-ID header field", Response.BAD_REQUEST);
		}
		if (request.getCSeqHeader() == null) {
			throw new BadRequestException("Missing CSeq header field", Response.BAD_REQUEST);
		}
		if (request.getFromHeader() == null) {
			throw new BadRequestException("Missing From header field", Response.BAD_REQUEST);
		}
		if (request.getToHeader() == null) {
			throw new BadRequestException("Missing To header field", Response.BAD_REQUEST);
		}
		if (!request.hasViaHeaders()) {
			throw new BadRequestException("Missing Via header field", Response.BAD_REQUEST);
		}
		
		// check max-forwards header
		// todo this should be moved to the proxy code, per rfc 3261 16.3
		MaxForwardsHeader maxForwardsHeader;
		try {
			maxForwardsHeader = request.getMaxForwardsHeader();
		}
		catch (HeaderParseException e) {
			throw new BadRequestException("Bad Request, Malformed Max-Forwards header", Response.BAD_REQUEST);
		}
		if (maxForwardsHeader != null && maxForwardsHeader.getMaxForwards() < 0) {
			throw new BadRequestException(
				SipResponseCodes.getResponseCodeText(Response.TOO_MANY_HOPS),
				Response.TOO_MANY_HOPS);
		}
	}

	/**
	 * top Via "received" and "rport" processing according to 3581-4
	 * which overrides 3261-18.2.1. 
	 * @param topVia to via header from incoming request
	 * @param connection transport connection that carried this request here
	 */
	private void validateTopViaAddress(ViaHeader topVia, SIPConnection connection) {
		try {
			// "the server MUST insert a "received" parameter
			// containing the source IP address that the request came from, even if
			// it is identical to the value of the "sent-by" component"
			String netHost = connection.getRemoteHost();
			topVia.setReceived(netHost);

			// "If this Via header field value contains an "rport" parameter
			// with no value, it MUST set the value of the parameter to the source
			// port of the request"
			// in standalone, force "rport" in TCP/TLS connections, to guarantee the
			// response is sent over the same connection.
			// not needed in cluster, where SLSPs are identified by their listening port.
			if ((connection.isReliable() && isStandAloneMode())
				|| topVia.hasParameter(ViaHeaderImpl.RPORT))
			{
				int netPort = connection.getRemotePort();
				topVia.setParameter(ViaHeaderImpl.RPORT, String.valueOf(netPort));
			}
		}
		catch (IllegalArgumentException e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "Exception in validateTopViaAddress", e.getMessage());
			}
		}
		catch (SipParseException e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "Exception in validateTopViaAddress", e.getMessage());
			}
		}
	}

	/**
	 * called when an existing connection was broken,
	 * or the attempt to establish an outbound connection has failed asynchronously
	 * @param connection the failed connection
	 */
	public void onConnectionClosed(SIPConnection connection ) 
	{
		cleanUpConnection( connection );
	}
	
	public	void	onConnectionCreated(SIPListenningConnection	listenConnection,
										SIPConnection			connection)
	{
		//start reading , if we cant , close the connection
		try
		{			
			//amirk
			//add just if this is reliable transport since we want to hold the connection
			if( connection.isReliable() )
			{
				m_connectionsModel.addConnection(  connection );
			}
			else
			{
			}			
			//start the connection read and parsing read
			//connection.addListener( this );
			connection.start();
			
			if(c_logger.isTraceDebugEnabled())
            {
            	c_logger.traceDebug(this, "onConnectionCreated" , connection.toString());
            }
		}
		catch( IOException exp )
		{
			if( c_logger.isTraceDebugEnabled())
			{
				c_logger.traceDebug(this,"onConnectionCreated",exp.getMessage(),exp);				
			}
			cleanUpConnection( connection );			
		}		
	}
	
	private void cleanUpConnection( SIPConnection connection )
	{
		if(c_logger.isTraceDebugEnabled())
		{
			c_logger.traceDebug(this,"onConnectionClosed",
								"connection " + connection + " closed" );
		}
		
		//clean up from connections table
		m_connectionsModel.removeConnection( connection );
	}
		
	public	void onListeningConnectionClosed(SIPListenningConnection	listenConnection )
	{
		//TODO
	}
	
	
	
	/** error on the connection */
	public void onIOError( SIPConnection connection )
	{
		cleanUpConnection( connection );
	}

	/**
	 * called when new bytes arrive from the network
	 * @param buffer buffer with a copy of the received bytes
	 * @param connection the connection that carried these bytes here
	 */
	public void onRead(SipMessageByteBuffer buffer, SIPConnection connection) {
		MessageParser parser = connection.getMessageParser();
		Message message;
		
		do {
			message = parser.parse(buffer);
			String parseError = parser.getError();
			if (message == null) {
				if (parseError != null) {
					// invalid message
					connection.incrementNumberOfParseErrors();
					// don't try to print the buffer, it is recycled by now
					messageDropped(null, parseError);
					
					boolean reliable = connection.isReliable();
					if (reliable) {
						if (connection.shouldDropConnection()) {
							if (c_logger.isTraceDebugEnabled()) {
								c_logger.traceDebug(this, "onRead",
									"closing connection [" + connection
										+ "] on parse error");
							}
							cleanUpConnection(connection);
							break;
							
						} else if (connection.isAParseErrorAllowed()) {
							parser.setStartLineHuntingMode(true);
						}
						
					} else if (isStandAloneMode()) {
						cleanUpConnection(connection);
						break;
					}
				}
			}
			else {
				try {
					setLogExtOnThread(message);
					int errorCode = parser.getErrorCode();
					
					// It has a complete message, turn off start line hunting mode.
					parser.setStartLineHuntingMode(false);
					onMessage(message, connection, parser, parseError, errorCode);
					if (connection.isClosed()) {
						// Suppose the maximum parse errors allowed is 5, and the number of received messages is 7.
						// Each of the 6 first messages has a parse error.
						// Thus, the reliable connection is closed.
						// However, there are more bytes for the parser. The bytes should not be processed.
						// In order to avoid it, need to break the while-loop.
						
						break;
					}
				} catch (Throwable t) {
					if (c_logger.isTraceFailureEnabled()) {
						c_logger.traceFailure(this, "onRead", "exception occured " + t.getLocalizedMessage());
						t.printStackTrace();
					}
				} finally {
					//Remove the call & SAS IDs from the current thread
					ThreadLocalStorage.setCallID(null);
					ThreadLocalStorage.setSasID(null);
				}
			}
		} while (parser.hasMore());
	}
	
	/**
	 * Sets some SIP information on thread local for 
	 * HPEl log extension feature.
	 * 
	 * @param message the SIP message
	 */
	private void setLogExtOnThread(Message message) {
		//Retrieve the call ID from the message
		CallIdHeader callIdHeader = message.getCallIdHeader();
		if (callIdHeader != null) {
			String callID = callIdHeader.getCallId();
			//Set the call ID on the current thread (for log debugging)
			ThreadLocalStorage.setCallID(callID);
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setLogExtOnThread", "storing the call ID on the current thread: " + callID);
			}
		}
		//Set the SAS ID on the current thread (for log debugging)
		String sasID = retrieveSasId(message);
		ThreadLocalStorage.setSasID(sasID);
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setLogExtOnThread", "storing the SAS ID on the current thread: " + sasID);
		}
	}
	
	/**
	 * Retrieves the SIP Application ID from the given message.
	 * Retrieves the 'ibmsid' parameter from the top Via header,
	 * and retrieves the SAS ID from it.
	 *  
	 * @param message the message to look in
	 * 
	 * @return the SAS ID
	 */
	private String retrieveSasId(Message message) {
		String sid = null;
		String sasId = null;
		try {
			ViaHeader topVia = (ViaHeader)message.getHeader(ViaHeader.name, true);
			if (topVia != null) {
				sid = topVia.getParameter(IBM_SID_PARAM);
			}
		} catch (HeaderParseException e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "retrieveSasId", e.getLocalizedMessage(), e);
			}
		} catch (IllegalArgumentException e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "retrieveSasId", e.getLocalizedMessage(), e);
			}
		}
		if (sid != null) {
			int first = sid.indexOf(SESSION_ID_SEPARATOR);
			int second = sid.indexOf(SESSION_ID_SEPARATOR, first+1);
			if (second != -1) {
				sasId = sid.substring(0, second);				
			}
		} 
		
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "retrieveSasId", "returning " + sasId);
		}
		return sasId;
	}
	
	/**
	 * creates a response based on some invalid request
	 * @param request malformed request
	 * @return error response
	 */
	private ResponseImpl createResponse(RequestImpl request) {
		ResponseImpl response = new ResponseImpl();

		

		// copy necessary headers from request to response.
		// note that some of these headers may not parse correctly,
		// so we should just copy them as they are
		request.copyCriticalHeaders(response);

		//if this is an initial request (does not have a to tag) we need to add the to tag
		//according to RFC3261 12.1.1
		SipStackUtil.addToTag(response);
		
		if (request.isLoopback()) {
			response.setLoopback(true);
		}
		return response;
	}

/**
	 * handles an incoming message
	 * @param msg the incoming message
	 * @param connection the connection on which this message arrived.
	 * @param parser the message parser associated with this connection.
	 * @param parseError error while parsing the message.
	 *  null indicates successful parse.
	 * @param errorCode the error code to return in response to the bad request,
	 *  relevant only if parseError != null
	 */
	public void onMessage(
		Message msg,
		SIPConnection connection,
		MessageParser parser,
		String parseError,
		int errorCode)
	{
		SipProvider provider = ((ListeningPointImpl)connection.getSIPListenningConnection().getListeningPoint()).getProvider();
		try {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "onMessage", "In Message:\r\n" + msg.toString());
			}
			
			if (SIPTransactionStack.instance().getConfiguration().isTraceInMsg()) {
				// @PMD:REVIEWED:UnprotectedSystemOutOrErr: by Amirk on 9/19/04 11:19 AM
				System.out.println("In Message:\n" + msg.toString());
			}                                                
			if (msg.isRequest()) {
				RequestImpl request = (RequestImpl)msg;
				if (parseError != null) {
					connection.incrementNumberOfParseErrors();
					
					// failed parsing request
					if (request.getMethod().equals(Request.ACK)) {
						// don't respond to ACK
						if (c_logger.isTraceDebugEnabled()) {
							c_logger.traceDebug(this, "onMessage", "Error parsing ACK: " + parseError);
						}
						messageDropped(request, parseError);
					}
					else if (request.getMethod().equals(RequestImpl.PROXYERROR)) {
						// don't respond to PROXYERROR
						if (c_logger.isTraceDebugEnabled()) {
							c_logger.traceDebug(this, "onMessage", "Error parsing PROXYERROR: " + parseError);
						}
						messageDropped(request, parseError);
					}
					else {
						MessageContext messageContext = null;
						// respond according to RFC3261 21.4.1
						try {
							Response response = createResponse(request);
							response.setStatusCode(errorCode);
							response.setReasonPhrase(parseError);
							messageContext = MessageContextFactory.instance().getMessageContext(response);
							sendMessage(messageContext, provider, connection);
							messageDropped(request, parseError);
						}
						catch (SIPTransportException transportX) {
							if (c_logger.isTraceDebugEnabled()) {
								c_logger.traceDebug(
									this,
									"failed sending response to unparsed request",
									transportX.getMessage());
							}
							messageDropped(request, parseError);
							if (messageContext!= null){
								messageContext.writeError(transportX);
							}
						}
					}
					if (connection.isReliable()) {
						reliableConnectionOnError(connection, parser);
					} else if (isStandAloneMode()) {
						cleanUpConnection(connection);
					}
					return;
				}
				else if (provider == null) {
					handlePartialInitialization(request, connection);
				}
				
				if (request.getMethod().equals(RequestImpl.PROXYERROR)){
					handleProxyError(msg,connection);
					return;
				}
				
				try {
					validateIncomingRequest(request);
				}
				catch (BadRequestException e) {
					// received an invalid incoming request.
					// respond immediately with error code.
					try {
						Response response = createResponse(request);
						response.setStatusCode(e.getStatusCode());
						String reason = e.getMessage();
						if (reason != null && reason.length() > 0) {
							response.setReasonPhrase(reason);
						}
						MessageContext messageContext = MessageContextFactory.instance().getMessageContext(response);
						sendMessage(messageContext, provider, connection);
						messageDropped(request, reason);
					}
					catch (SipParseException parseX) {
						if (c_logger.isTraceDebugEnabled()) {
							c_logger.traceDebug(
								this,
								"failed creating response to invalid incoming request",
								parseX.getMessage());
						}
						messageDropped(request, "could not create response");
					}
					catch (SIPTransportException transportX) {
						if (c_logger.isTraceDebugEnabled()) {
							c_logger.traceDebug(
								this,
								"failed sending response to invalid incoming request",
								transportX.getMessage());
						}
						messageDropped(request, "could not send response");
					}
					return; // don't bother the application about bad incoming requests
				}
				
				// get here with a request that looks fine
				m_startupReceived = m_startupReceived || SipStackUtil.isSlspStartup(request);
				try {
					ViaHeader topVia = (ViaHeader)request.getHeader(ViaHeader.name, true);
					validateTopViaAddress(topVia, connection);
					
					processConnectionReuseExtension(topVia, connection, request);
				}
				catch (HeaderParseException e) {
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "onMessage", "failed parsing top via");
					}
				}
				m_router.processRequest(request);
			}
			else { // message is a response.
				if (parseError != null) {
					// failed parsing response
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(
							this,
							"got parse error(s) for response",
							parseError);
					}
					connection.incrementNumberOfParseErrors();
					if (connection.isReliable()) {
						reliableConnectionOnError(connection, parser);
					} else if (isStandAloneMode()) {
						cleanUpConnection(connection);
					}
					messageDropped(msg, "bad response: " + parseError);
					return;
				}
			}

			//get to what listening connection is this connection associated with
			SIPListenningConnection listenningConnection = connection.getSIPListenningConnection();

			// populate source address if running in standalone
			SIPStackUtil.addIbmClientAddressHeader(msg, connection, listenningConnection.getListeningPoint());

			//get the listening point of the listening connection
			ListeningPointImpl lpImpl = (ListeningPointImpl)listenningConnection.getListeningPoint();
			
			//forward to the stack
			SIPTransactionStack.instance().prossesTransportSipMessage( msg , lpImpl.getProvider() , connection );
		}
		catch (Exception e) {
			// catch generic exceptions to keep this thread running
			if (c_logger.isErrorEnabled()) {
				c_logger.error("error.exception.stack", Situation.SITUATION_CREATE, null, e);
			}
		}
	}
	
	/**
	 * Decides what to perform in case a parse error occurred on the reliable connection.
	 * @param connection the connection on which this message arrived.
	 * @param parser the message parser associated with this connection.
	 */
	private void reliableConnectionOnError(SIPConnection connection, MessageParser parser) {
		if (connection.shouldDropConnection()) {
			cleanUpConnection(connection);
		} else if (connection.isAParseErrorAllowed()) {
			parser.setStartLineHuntingMode(true);
		}
	}

	/**
	 * called when a message is sent into the stack before the stack is
	 * fully initialized. that is, the application (container) called
	 * SipFactory.createSipStack() but has not yet called
	 * SipStack.createSipProvider(). in between, the message came in.
	 * @param request incoming request
	 * @param connection the transport connection
	 */
	private void handlePartialInitialization(RequestImpl request, SIPConnection connection) {
		if (c_logger.isTraceFailureEnabled()) {
			c_logger.traceFailure(this, "handlePartialInitialization",
				"stack is not fully initialized");
		}
		Response response = createResponse(request);
		try {
			response.setStatusCode(Response.SERVICE_UNAVAILABLE);
			RetryAfterHeader retryAfterHeader = SipJainFactories.getInstance().getHeaderFactory().createRetryAfterHeader(5);
			retryAfterHeader.setComment("Initializing");
			response.setRetryAfterHeader(retryAfterHeader);
			MessageContext messageContext = MessageContextFactory.instance().getMessageContext(response);
			sendMessage(messageContext, null, connection);
		}
		catch (Exception e) {
			if (c_logger.isTraceFailureEnabled()) {
				c_logger.traceFailure(this, "handlePartialInitialization",
					"failed generating response", e);
			}
		}
	}
	
	/**
	 * handle the PROXYERROR message, meaning:
	 * 1. extracting and parsing the original message 
	 *    inside the PROXYERROR message's body.
	 * 2. creating a message context for the original message
	 * 3. call "Message sending failed" event in transport 
	 *   
	 * @param proxyErrorMessage
	 * @param connection
	 */
	private void handleProxyError(Message proxyErrorMessage, SIPConnection connection) {

		//parse the original message
		Message originalMessage = parseOriginalMessage(proxyErrorMessage,connection);
		
		if (originalMessage == null){
			// notify by log that original message could not be parsed
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "handleProxyError", "Original message of PROXYERROR was not created: " + proxyErrorMessage);
			}
			return;
		}

		boolean isProxyErrorDueToMtu = false;
		
		//check for "ibmmtu" param on top via to determine what to do with the message
		try {
			ViaHeader topVia = (ViaHeader)originalMessage.getHeader(ViaHeader.name, true);
			String param = topVia.getParameter(IBM_MTU_PARAM);
			if (param != null){
				isProxyErrorDueToMtu = true;
			}
		} catch (Exception e) {
			// notify by log that original message could not be parsed
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "handleProxyError", "Could not obtain top via of original message of PROXYERROR: " + proxyErrorMessage);
			}
			return;
		}


		if (isProxyErrorDueToMtu){
			//this proxy error was due to mtu sending error
			handleMtuError(null, originalMessage,connection);
		}else{
			//if we got here it means this was a general message sending error
			handleGeneralProxyError(originalMessage,connection);
		}
		
	}


	/**
	 * handles the case of a TCP failure after switching from UDP to TCP.
	 * this is needed for falling back to UDP.
	 * it can be called either by receiving a PROXYERROR message from the
	 * proxy (in cluster) or by a local TCP failure (standalone).
	 * 
	 * @param messageContext in standalone this is the same message context
	 *  as the one that switch transport.
	 *  in cluster, this is null.
	 * @param originalMessage
	 * @param connection
	 */
	public void handleMtuError(MessageContext messageContext, Message originalMessage, SIPConnection connection) {
		try {
			//get the top Via header
			ViaHeader topVia = (ViaHeader)originalMessage.getHeader(ViaHeader.name, true);
			//get the new transport that was assigned to the message (from connection)
			Hop nextHop = connection.getKey();
			String previousTransport = nextHop.getTrasport();
			//revert the message to udp
			revertToUdp(topVia,previousTransport,null);
			//get the actual transport
			String finalTransport = topVia.getTransport();
			//create the message context for the message resend
			if (messageContext == null) {
				// cluster.
				// remove the mtu parameter from the top via
				messageContext = MessageContextFactory.instance().getMessageContext(originalMessage);
				topVia.removeParameter(IBM_MTU_PARAM);
			}
			//get the provider
			SipProvider provider = com.ibm.ws.sip.stack.properties.StackProperties.getInstance().getProvider(finalTransport);
			//resend the message
			sendMessage(messageContext, provider, null);
		} catch (Exception e) {
			// notify exception
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "handleMtuProxyError", 
						"Exception occurred while handling MTU proxy error of Message: " + originalMessage);
			}
			// TODO is this the right time to clean up the message context?
		}
	}

	/**
	 * handles cases where PROXYERRORs were sent to due to 
	 * regular failure (not MTU related)
	 *  
	 * @param originalMessage
	 * @param connection
	 */
	private void handleGeneralProxyError(Message originalMessage, SIPConnection connection) {
		//prepare the message context
		MessageContext messageContext = MessageContextFactory.instance().getMessageContext(originalMessage);

		//messageContext was not created
		if (messageContext == null){
			// notify by log that messageContext was not created
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "handleGeneralProxyError", "Message context for original message of PROXYERROR was not created: " + originalMessage);
			}
			return;
		}

		//set the connection
		messageContext.setSipConnection(connection);
		//report failure of message
		onMessageSendingFailed(messageContext);
	}

	/**
	 * attempt to parse the original message found inside the PROXYERROR message
	 * 
	 * @param proxyErrorMessage
	 * @param connection
	 * @return the parse message if success, otherwise null
	 */
	private Message parseOriginalMessage(Message proxyErrorMessage, SIPConnection connection) {
		//extract the byte array of the original message
		byte[] originalMsgBytes = proxyErrorMessage.getBodyAsBytes();
		//checking if the body is empty
		if (originalMsgBytes == null || originalMsgBytes.length <= 0){
			// notify by log that original message could not be parsed
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "parseOriginalMessage", "Original message not found inside PROXYERROR: " + proxyErrorMessage);
			}
			return null;
		}
		//get the connection parser
		MessageParser parser = connection.getMessageParser();
		//prepare necessay fields for parse
		int origMsgLength = originalMsgBytes.length;
		String peerHost = connection.getRemoteHost();
		int peerPort = connection.getRemotePort();
		//create the byte buffer
		SipMessageByteBuffer buffer = SipMessageByteBuffer.fromNetwork(originalMsgBytes, origMsgLength, peerHost, peerPort);
		//parse to get the original message;
		Message originalMessage = parser.parse(buffer);
		//check for parse errors
		String parseError = parser.getError();
		if (parseError != null){
			// notify by log that original message could not be parsed
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "parseOriginalMessage", "Error parsing original message inside PROXYERROR: " + parseError + " Message: " + proxyErrorMessage);
			}
			return null;
		}
		return originalMessage;
	}

	/**
	 * Connection Reuse in the Session Initiation Protocol (SIP)
	 * "draft-ietf-sip-connect-reuse-00.txt".
	 * A connection is re-used by changing the port number in connection key,
	 * from the remote ephemeral number to the remote listening port number.
	 * This causes future requests that are bound to that remote address to use
	 * this inbound connection instead of creating a new outbound connection.
	 * A connection is "re-used" under any one of the following conditions:
	 * o the custom property javax.sip.force.connection.reuse is true
	 * o the top Via contains the standard "alias" parameter
	 * o the container is running in cluster (fronted by a WAS proxy), and
	 *   the incoming request is a STARTUP from the WAS proxy. the reason for
	 *   this check, is that sometimes the STARTUP comes in before a notification
	 *   from UCF, meaning we get a STARTUP before we know we are in cluster.
	 *   note that the STARTUP is always the first message per connection that
	 *   comes from the WAS proxy. we do not alias non-STARTUP messages because
	 *   the application might accidentally address a request to the container's
	 *   local listening point - this will cause the proxy to create a new
	 *   connection to the container as if it's creating a connection to a client,
	 *   and aliasing that would remove the reference to the STARTUP connection.
	 * @param topVia - top via to analyze
	 * @param connection - the connection from which the message was received
	 * @param request - the incoming request
	 */
	private void processConnectionReuseExtension(ViaHeader topVia,
		SIPConnection connection, RequestImpl request)
	{
		if (connection.hasAliacePort()) {
			// already re-used
			return;
		}
		if (connection.isOutbound()) {
			// don't attempt to alias connections created out
			return;
		}
		if (topVia.getTransport().equalsIgnoreCase(ListeningPoint.TRANSPORT_UDP)) {
			// never re-use a UDP "connection"
			return;
		}
		boolean isSlspStartup = false;
		if (SIPTransactionStack.instance().getConfiguration().forceConnectionReuse() || // custom property
			topVia.getParameter(SIPTransactionConstants.ALIAS) != null || // peer supports connection-reuse
			(isSlspStartup = SipStackUtil.isSlspStartup(request))) // first message from proxy in cluster
		{
			// change the connection key to use the port from the top via
			int aliasPort;
			if (isSlspStartup && !topVia.hasPort()) {
				// backward compatibility with a 7.0.0.13 proxy, which doesn't state the port in the STARTUP Via
				aliasPort = 0; // port 0 matches any connection with the same transport and host
			}
			else {
				boolean outboundSupported = isOutboundSupported(request);
				aliasPort = SipStackUtil.getPortFromTopVia(topVia, outboundSupported);
			}
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "processConnectionReuseExtension",
					"replacing connection key of [" + connection
						+ "] from port [" + connection.getRemotePort()
						+ "] to port [" + aliasPort + ']');
			}
			m_connectionsModel.updateConnection(connection, aliasPort);
		}
	}

	/**
	 * this method checks if the request comes directly from an "outbound"
	 * (RFC 5626) enabled client. if it does, the connection is aliased
	 * (indexed) by the physical remote port,
	 * otherwise it is aliased by the Via port number.
	 * @param request incoming request
	 */
	private boolean isOutboundSupported(RequestImpl request) {
		// 1. check if the request is sent directly (no mid-way proxies)
		try {
			Header topVia = request.getHeader(ViaHeader.name, true);
			Header botVia = request.getHeader(ViaHeader.name, false);
			if (topVia != botVia) {
				// more than one Via - not a direct connection with the UAC
				return false;
			}
		}
		catch (Exception e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "isOutboundExtension", "", e);
			}
			return false;
		}
		// 2. inspect the Supported header for "outbound" option tag
		if (SipStackUtil.isSupported(request, "outbound")) {
			return true;
		}
		return false;
	}

	/**
	 * generates a 430 (Flow Failed) response and delivers it up to the application
	 * 
	 * @param request the outbound failed request
	 * @param provider the associated provider
	 * @param transaction the client transaction failing to send out the request,
	 *  null if no transaction context
	 */
	private void handleFlowFailed(Request request, SipProvider provider,
		SIPTransaction transaction)
	{
		MessageFactory messageFactory = SipJainFactories.getInstance().getMesssageFactory();
		try {
			Response response = messageFactory.createResponse( 
				FlowFailedException.FLOW_FAILED_STATUS_CODE,
				request);
			response.setReasonPhrase(FlowFailedException.FLOW_FAILED_REASON_PHRASE);
			long transactionId = transaction == null ? -1 : transaction.getId();
			SipEvent event = new SipEvent(provider, transactionId, response);
			UACommLayer uaLayer = SIPTransactionStack.instance().getUACommLayer();
			uaLayer.sendEventToUA(event);
		}
		catch (Exception e) {
			if (c_logger.isTraceFailureEnabled()) {
				c_logger.traceFailure(this, "handleFlowFailed", "", e);
			}
		}
	}

	/**
	 * generates a 403 (Forbidden) response and delivers it up to the application
	 * 
	 * @param request the outbound failed request
	 * @param provider the associated provider
	 * @param transaction the client transaction failing to send out the request,
	 *  null if no transaction context
	 */
	private void handleFlowTampered(Request request, SipProvider provider,
		SIPTransaction transaction)
	{
		MessageFactory messageFactory = SipJainFactories.getInstance().getMesssageFactory();
		try {
			Response response = messageFactory.createResponse( 
				Response.FORBIDDEN, request);
			response.setReasonPhrase("Forbidden Flow");
			long transactionId = transaction == null ? -1 : transaction.getId();
			SipEvent event = new SipEvent(provider, transactionId, response);
			UACommLayer uaLayer = SIPTransactionStack.instance().getUACommLayer();
			uaLayer.sendEventToUA(event);
		}
		catch (Exception e) {
			if (c_logger.isTraceFailureEnabled()) {
				c_logger.traceFailure(this, "handleFlowTampered", "", e);
			}
		}
	}
					
	/**
	 * @return
	 */
	public SIPConnectionsModel getConnectionsModel()
	{
		return m_connectionsModel;
	}

	/**
	 * write loopBack from network and flushes to socket
	 * @author Amirk
	 */
	static class LoopBackAddressThread implements Runnable
	{
		/**
		 * Queue for Sip mesages as bytes that are waiting to be dispatched to the network 
		 */
		// @PMD:REVIEWED:SizeNotAllocatedAtInstantiation: by Amirk on 9/19/04 11:18 AM
		private LinkedList m_msgQueue = new LinkedList();
		
		LoopBackAddressThread(){}
						
		/**
		 * add the message to the queue to be dispached
		 * @param msg
		 */
		public void add( LoopBackMessage msg)
		{
			//	Add the message to the list		
			synchronized (m_msgQueue)
			{
				m_msgQueue.addLast(msg);
				m_msgQueue.notify();
			}
		}
		
		public void run()
		{
			readLoopBackMessages();
		}
		
		/** 
		 * keep wait for messages and send them to the network
		 * untill error occurs 
		 **/
		private void readLoopBackMessages()
		{
			while (true)
			{
				LoopBackMessage msg = null;
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
							msg = (LoopBackMessage) m_msgQueue.removeFirst();
						}
					}
				}
				catch (InterruptedException e)
				{
					if( c_logger.isTraceDebugEnabled())
					{
						c_logger.traceDebug(this,"readLoopBackMessages",e.getMessage());
					}
				}
				if( msg!=null)
				{						
					if( c_logger.isTraceDebugEnabled())
					{
						c_logger.traceDebug(  this,"readLoopBackMessages","\nLoopback Message:\n" + msg.toString() );
					}
					//remove the ibm destination header
					msg.getMsg().removeHeader(SipStackUtil.DESTINATION_URI, true);
					//forward to the stack			
					try {
						SIPTransactionStack.instance().prossesTransportSipMessage( msg.getMsg() , msg.getProvider() , msg.getConnection() );
					}
					catch (Exception e) {
						if (c_logger.isTraceDebugEnabled()) {
							c_logger.traceDebug(this, "readLoopBackMessages", e.getMessage());
						}
					}
				}						
			}
		}
	}
			
	/**
	 * takes care of situations where sending message to the 
	 * primary destination failed.
	 *  
	 * @param messageSendingContext - the context handling the message
	 */
	public void onMessageSendingFailed(MessageContext messageSendingContext) {
		messageSendingContext.handleFailure();
	}

	/**
	 * called when an incoming message gets dropped, for any reason. this can happen for a malformed request
	 * that has a bad Via, or a malformed ACK, or a malformed response.
	 * @param dropped the dropped message
	 * @param reason the reason why this message is being dropped
	 */
	private static void messageDropped(Message dropped, String reason) {
		if (c_logger.isTraceFailureEnabled()) {
			StringBuilder buffer = new StringBuilder(256);
			buffer.append("message dropped"); 
			if(reason != null && reason.length() > 0) {
				buffer.append(" reason [");
				buffer.append(reason);
				buffer.append("]");
			}
			if(dropped != null) {
				buffer.append(" message:\r\n");
				buffer.append(dropped);
			}
			c_logger.traceFailure(buffer.toString());
		}
		
		updateRejectedMessagesPMICounter();
	}
	
	private static void updateRejectedMessagesPMICounter() {
		StackExternalizedPerformanceMgr perfMgr = PerformanceMgr.getInstance();
		if(perfMgr != null) {
			perfMgr.updateRejectedMessagesCounter();
		}
	}
	
	/**
	 * @return true if standalone, false if cluster
	 */
	public boolean isStandAloneMode() {
		if (m_startupReceived) {
			return false;
		}
		return m_router.isStandAloneMode();
	}
}
