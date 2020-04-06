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
package com.ibm.ws.sip.container.sessions;

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.header.HeaderParseException;
import jain.protocol.ip.sip.header.ParametersHeader;
import jain.protocol.ip.sip.header.RouteHeader;
import jain.protocol.ip.sip.header.ViaHeader;
import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

import java.util.List;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.sip.container.failover.repository.SessionRepository;
import com.ibm.ws.sip.container.proxy.RecordRouteProxy;
import com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl;
import com.ibm.ws.sip.container.servlets.SipServletMessageImpl.MessageType;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.tu.TUKey;
import com.ibm.ws.sip.container.tu.TransactionUserImpl;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.container.util.SipUtil;
import com.ibm.ws.sip.container.was.ThreadLocalStorage;

/**
 * @author Dedi Hirschfeld, Aug 11, 2003
 * 
 * A session table for the SIP container.
 */
public class SipTransactionUserTable {
	/**
	 * Class Logger.
	 */
	private static final LogMgr c_logger = Log.get(SipTransactionUserTable.class);

	/**
	 * Single instance of session table.
	 */
	private static SipTransactionUserTable c_sipTransactionUserTable = new SipTransactionUserTable();

	/**
	 * Constructor for singleton instance.
	 */
	private SipTransactionUserTable() {
	}

	/**
	 * Get the sip session associated with the given request.
	 * 
	 * @param request
	 *            It is mandatory that the request must be an inbound request.
	 * @return a sip session impl object, or null if not found.
	 */
	public final TransactionUserWrapper getTransactionUserForInboundRequest(
			SipServletRequestImpl request) {
		TransactionUserWrapper transactionUser = null; 
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this, "getTransactionUserForInboundRequest", request);
		}
		try{
			//Look for a session identifier in the request uri or top route header
			//we expect to find a session identifier in these location in case 
			//the application serves as a proxy
			String sessionId = geTransactionnUserIdAccordingToRoute(request);

			TUKey key = ThreadLocalStorage.getTUKey();

			if(sessionId != null){
				key.setParams(request,sessionId,MessageType.INCOMING_REQUEST);
			}
			else{
				//In case this is an incoming request - toTag is a local tag.
				key.setParams(request,MessageType.INCOMING_REQUEST);
			}

			transactionUser = SessionRepository.getInstance().getTuWrapper(key);

			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "getTransactionUserForInboundRequest", "Request received=" + request.getMethod() +
						", sessionId=" + sessionId + ", TransactionUser="+transactionUser);
			}

			if( transactionUser != null){
				return transactionUser;
			}

			// There is an option of UPDATE request to 
			// be received on dialog in EARLY state.
			// in that case we should check for TU according 
			// to tag only
			if (request.getMethod().equals("UPDATE") || request.getMethod().equals("ACK")){

				String savedTag2 = key.get_tag_2();
				key.setTag_2(null);

				transactionUser = SessionRepository.getInstance().getTuWrapper(key);
				key.setTag_2(savedTag2);
			}

			if( transactionUser != null){
				return transactionUser;
			}

			String tag_2 = null;
			//Anat: maybe NOTIFY received before 2XX response
			if(request.getMethod().equals("NOTIFY")){
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "getTransactionUserForInboundRequest", "Trying to find TU without RemoteTag");
				}
				key.setTag_2(null);
				transactionUser = SessionRepository.getInstance().getTuWrapper(key);

				if( transactionUser != null){
					return transactionUser;
				}
			}
			
			if(sessionId == null){
				sessionId = getTransactionUserdIDFrom_ToTag(request);
			}

			if(sessionId != null){
				if((SipUtil.hasBothTags(request.getRequest()) == false || request.getMethod().equals("NOTIFY"))){
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this,"getTransactionUserForInboundRequest",
								"Trying to find TU by session ID = " + sessionId + " for method = " + request.getMethod());
					}
					
					transactionUser = SessionRepository.getInstance().getTuWrapper(sessionId);
				}
			}

			if( transactionUser != null){
				return transactionUser;
			}
		}
		finally{
			if(c_logger.isTraceEntryExitEnabled()){
				c_logger.traceExit(this, "getTransactionUserForInboundRequest", transactionUser);
			}
		}
		return transactionUser;
	}
	/**
	 * Get the sip session associated with the given request.
	 * 
	 * @param request
	 *            It is mandatory that the request must be an inbound request.
	 * @return a sip session impl object, or null if not found.
	 */
	public final TransactionUserWrapper getTransactionUserForOutboundRequest(
			SipServletRequestImpl request) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "getTransactionUserForOutboundRequest",request.getMethod());
		}
		
		TransactionUserWrapper transactionUser = null; 

		//Look for a session identifier in the request uri or top route header
		//we expect to find a session identifier in these location in case 
		//the application serves as a proxy
		String sessionId = geTransactionnUserIdAccordingToRoute(request);

		TUKey key = ThreadLocalStorage.getTUKey();

		if(sessionId != null){
			key.setParams(request,sessionId,MessageType.OUTGOING_REQUEST);
		}

		if(null == sessionId)
		{
			// patch for huawei
			RouteHeader topRoute = RecordRouteProxy.getTopRoute(request);
			if (topRoute == null) {
				//Look for the identifier in the to tag. We expect to find a session
				//identifier in this location in case the application is a UAC or
				//UAS. 
				//  In case this is an outgoing request - fromTag is a local tag.
				key.setParams(request,MessageType.OUTGOING_REQUEST);
			}
			else {
				key = null;
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "getTransactionUserForOutboundRequest", "Patch is working! - do not look for loopback");
				}                	
			}
		}

		if(key != null){
			transactionUser = SessionRepository.getInstance().getTuWrapper(key);
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "getTransactionUserForOutboundRequest", "Request received=" + request.getMethod() +
						", sessionId=" + sessionId + ", TransactionUser="+transactionUser);
			}

			if (transactionUser == null && request.getMethod().equals("NOTIFY")) {
				// special case when sending a NOTIFY to another application in
				// the same server. if the target application has not received the
				// 200 response for SUBSCRIBE yet, then the session of the target app
				// is stored in the repository without the remote tag.
				String tag2 = key.get_tag_2(); // save for later restore
				key.setTag_2(null);
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "getTransactionUserForOutboundRequest",
							"looking up TU by key " + key);
				}
				transactionUser = SessionRepository.getInstance().getTuWrapper(key);
				key.setTag_2(tag2); // restore
			}
			
		}

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "getTransactionUserForOutboundRequest",transactionUser);
		}
		return transactionUser;
	}


	/**
	 * Extract the session identifier from the To tag in the incoming request.
	 * @param request
	 * @return The session identifier or null if not available
	 */
	private String getTransactionUserdIDFrom_ToTag(SipServletRequestImpl request) {
		String id = null; 

		Request req = request.getRequest(); 
		String tag = req.getToHeader().getTag(); 
		id = extractTransactionUserIDByTag(tag);

		return id;
	}


	/**
	 * Extract the Session Identifier from the given String which can be either
	 * a from or to tag. 
	 * @param tag
	 * @return The session id if available otherwise null
	 */
	private String extractTransactionUserIDByTag(String tag) {
		String id = null;
		if(tag != null)
		{
			for (int i=0; i < tag.length(); i++)
			{
				if(tag.charAt(i) == TransactionUserImpl.SESSION_ID_TAG_SEPARATOR)
				{
					id = tag.substring(i + 1); 

					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "extractSessionIdFromTag", "Id" + id);
					}

					break;
				}
			}
		}
		return id; 
	}

	/**
	 * Looks up the session Id in Route header or in the request URI in the case
	 * of Strict Router in front of us (RFC 2543).
	 * 
	 * @param request
	 * @return The session id on null if not available.
	 */
	private final static String geTransactionnUserIdAccordingToRoute(
			SipServletRequestImpl request) {
		String transactionUserId = null;

		//Start by search for the session identifier in the route header
		//in the case we had strict proxy in front of us that pushed the
		//request to the uri.
		transactionUserId = geTransactionnUserIdAccordingToUri(request);

		if (null == transactionUserId) {
			RouteHeader topRoute = RecordRouteProxy.getTopRoute(request);
			if (topRoute != null) {
				//If we have a route header then we need to look for a session
				//in RR proxying mode.
				transactionUserId = RecordRouteProxy
				.getSessionIdParamFromRoute(topRoute);
			}
		}

		return transactionUserId;
	}

	/**
	 * Helper method which gets the TU according to the URI
	 * @param request
	 * @return
	 */
	private final static String geTransactionnUserIdAccordingToUri(
			SipServletRequestImpl request) {
		String transactionUserId = null;

		// Start by search for the session identifier in the route header
		// in the case we had strict proxy in front of us that pushed the
		// request to the uri.
		Request jainReq = request.getRequest();
		try {
			transactionUserId = RecordRouteProxy
			.getSessionIdParamFromURI(jainReq.getRequestURI());
		} catch (SipParseException e) {
			logException(e);
		}

		return transactionUserId;
	}

	/**
	 * Looks up the session Id "Replace" " or "Join" headers.
	 * Those headers has an template : callId_to_replace;totag=test;from-tag=test
	 * 
	 * @param request
	 * @return The session id on null if not available.
	 */
	public final TransactionUserWrapper geTUFromJoinReplace(ParametersHeader val) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "geTUFromJoinReplace",val);
		}
		TransactionUserWrapper transactionUser = null;
		try{
			TUKey key = new TUKey();
	
			if (val.getValue() == null) return null;
	
			int firstSemicolon = val.getValue().indexOf(';');
			String callId = val.getValue().substring(0, firstSemicolon);
	
			key.setup(val.getParameter(SipUtil.TO_TAG), val.getParameter(SipUtil.FROM_TAG), callId, false);
	
			transactionUser = SessionRepository.getInstance().getTuWrapper(key);
	
			// Look up for transaction user with a opposite from to tags.
			if (transactionUser == null) {
				key.setup(val.getParameter(SipUtil.FROM_TAG), val.getParameter(SipUtil.TO_TAG), callId, false);
				transactionUser = SessionRepository.getInstance().getTuWrapper(key);
			}
			return transactionUser;
		}finally{
			if (c_logger.isTraceEntryExitEnabled()) {
				c_logger.traceExit(this, "geTUFromJoinReplace",transactionUser);
			}
		}
	}

	/**
	 * Helper method which gets the TransactionUser according to the URI (if
	 * URI contains the sessionId.
	 * @param request
	 * @return
	 */
	public final TransactionUserWrapper getTuAccordingToUri(SipServletRequestImpl request){
		String sessionId = geTransactionnUserIdAccordingToUri(request);
		TransactionUserWrapper tu = null;
		if(sessionId != null){
			tu = SessionRepository.getInstance().getTuWrapper(sessionId);
		}
		return tu;
	}


	/**
	 * Get a sip session associated with the given response. This assumes that
	 * there's a way to get the transaction associated with the response.
	 * 
	 * @return a sip session impl object, or null if not found.
	 */
	public TransactionUserWrapper getTransactionUserInboundResponse(Response response) {
		if (c_logger.isTraceEntryExitEnabled()) {
			try {
				c_logger.traceEntry(this, "getTransactionUserInboundResponse", 
						"response=" + response.getStatusCode() + response.getReasonPhrase());
			} catch (SipParseException e) {
				c_logger.error(null, "getTransactionUserInboundResponse", e);
			}
		}
		
		TransactionUserWrapper transactionUser = null;

		String sessionId = null;
		TUKey key = ThreadLocalStorage.getTUKey();
		// Look for a session identifier in the uri of the top via header
		// we expect to find a session identifier in these location always
		sessionId = getTransactionUserIdAccordingToTopVia(response);
//		Proxy mode
		key.setParams(response, sessionId, true);
		transactionUser = SessionRepository.getInstance().getTuWrapper(key);
		// UAC mode
		if(transactionUser == null) {
			key.setParams(response, response.getCallIdHeader().getCallId(), false);
			transactionUser = SessionRepository.getInstance().getTuWrapper(key);
		}

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "getTransactionUserInboundResponse", 
					"Getting Session For Id: " + sessionId + 
					" TransactionUser = " + transactionUser);
		}
		return transactionUser;
	}

	/**
	 * This method will look for TransactionUser that should
	 * be a base for DerivedTransactionUser.
	 * Use this method when getTransactionUserInboundResponse() returns null;
	 * 
	 * @return Base TransactionUser impl object, or null if not found.
	 */
	public TransactionUserWrapper getBaseTUForDerived(Response response) {
		if (c_logger.isTraceEntryExitEnabled()) {
			try {
				c_logger.traceEntry(this, "getBaseTUForDerived", 
						"response=" + response.getStatusCode() + response.getReasonPhrase());
			} catch (SipParseException e) {
				c_logger.error(null, "getBaseTUForDerived", e);
			}
		}
		
		TransactionUserWrapper transactionUser = null;
		String sessionId = null;
		
		sessionId = getTransactionUserIdAccordingToTopVia(response);
		if(sessionId != null){
			transactionUser = SessionRepository.getInstance().getTuWrapper(sessionId);
		}

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "getBaseTUForDerived", 
					"Getting Session For Id: " + sessionId + 
					" TransactionUser = " + transactionUser);
		}

		return transactionUser;
	}

	/**
	 * @param response
	 * @return
	 */
	public String getTransactionUserIdAccordingToTopVia(Response response) {
		String sid = null;
		if (c_logger.isTraceEntryExitEnabled()) {
			try {
				c_logger.traceEntry(this, "getTransactionUserIdAccordingToTopVia", 
						"response=" + response.getStatusCode() + response.getReasonPhrase());
			} catch (SipParseException e) {
				c_logger.error(null, "getTransactionUserIdAccordingToTopVia", e);
			}
		}
		
		try {
			ViaHeader topVia = (ViaHeader) response.getHeader(ViaHeader.name, true);
			if(topVia != null) {
				sid = topVia.getParameter(TransactionUserImpl.SESSION_RR_PARAM_KEY);
			}
		}
		catch (HeaderParseException e) {
			logException(e);
		}
		catch (IllegalArgumentException e) {
			logException(e);
		}
		
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "getTransactionUserIdAccordingToTopVia", sid);
		}
		return sid;
	}

	/**
	 * Create a new TransactionUserWrapper, using the given request as the session's initial
	 * request.
	 * 
	 * @param request
	 *            The initial request that caused this session's creation.
	 * @param isServerTransaction
	 *            A flag to indicate whether the initial request is an incoming
	 *            request. If true, the siplet is acting as a UAS.
	 */
	public final TransactionUserWrapper createTransactionUserWrapper(SipServletRequestImpl request,
			boolean isServerTransaction,
			SipApplicationSessionImpl appSession,
			boolean pendingMessageExists) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { request.getMethod(),
					new Boolean(isServerTransaction), appSession };
			c_logger.traceEntry(this, "createTransactionUserWrapper", params);
		}

		TransactionUserWrapper transactionUser = new TransactionUserWrapper(request,isServerTransaction,appSession, pendingMessageExists);

		// Transfer parameters from the request to the transaction user.
		// subscriberURI, region - is received from application router and 
		// saved in the request until the transaction user will be ready to 
		// save them
		transactionUser.setSubscriberUri(request.getSubscriberURI());
		transactionUser.setRegion(request.getRegion());

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "createTransactionUserWrapper", transactionUser);
		}
		return transactionUser;
	}

	/**
	 * Get the singleton instance of the Sip Session Table.
	 * 
	 * @return
	 */
	public final static SipTransactionUserTable getInstance() {
		return c_sipTransactionUserTable;
	}

	/**
	 * Invalidate the specified session. Session is removed from table and
	 * removed from its application session.
	 * 
	 * @param transactionUser
	 * @param removeFromAppSession
	 *            Defines if remove this session from related
	 *            SipApplicationSession
	 */
	public final void removeTransactionUserForOutgoingRequest(SipServletRequestImpl req) {

		TUKey key = ThreadLocalStorage.getTUKey();
		key.setParams(req,MessageType.OUTGOING_REQUEST);

		SessionRepository.getInstance().removeTuWrapper(key,true);

		if (c_logger.isTraceDebugEnabled()) {
			StringBuffer b = new StringBuffer(64);
			b.append("Session removed from Sessions Table, id: ");
			b.append(" transactionUserId =");
			b.append(req.getTransactionUser().getId());
			c_logger.traceDebug(this, "removeTransactionUser", b.toString());
		}
	}

	/**
	 * Utility function for logging exceptions.
	 * 
	 * @param e
	 */
	protected static void logException(Exception e) 
	{
		if(c_logger.isErrorEnabled())
		{
			c_logger.error("error.exception", 
					Situation.SITUATION_CREATE, 
					null, e);
		}
	}



	/**
	 * Returns a view of all TransactionUsers at the time of the call to this method.
	 * The list returned is safe for iterating over, since changes to the c_appSessions
	 * map will not affect it. We use this snapshot view for replicating all the TUs
	 * on a bootstrap event.   
	 * @param list The list of TUs to fill
	 */
	public void getSnapshotView( List list){
		if(c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug("SipTransactionUserTable.getSnapshotView");
		}
		List l = SessionRepository.getInstance().getAllTuWrappers();
		for (int i = 0 ; i < l.size() ; i++)
		{
			list.add(l.get(i));
		}
	}
}
