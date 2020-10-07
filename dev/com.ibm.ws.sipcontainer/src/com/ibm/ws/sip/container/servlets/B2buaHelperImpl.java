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
package com.ibm.ws.sip.container.servlets;

import jain.protocol.ip.sip.header.ContactHeader;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.sip.Address;
import javax.servlet.sip.B2buaHelper;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.TooManyHopsException;
import javax.servlet.sip.UAMode;
import javax.servlet.sip.SipSession.State;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.properties.CoreProperties;

/**
 * Helper class providing support for B2BUA applications. 
 * Fully compliant to JSR289.
 * 
 * @author Assya Azrieli
 */
public class B2buaHelperImpl implements B2buaHelper {
	
	/**
     * Singleton instance of this class
     */
    private static B2buaHelperImpl _instance;
    
    
    /**
     * Object to be locked on when creating single instance
     */
    private static Object _lock = new Object();
    
    /**
     * Class Logger.
     */
    private static final transient LogMgr c_logger = Log
            .get(B2buaHelperImpl.class);
    
    /**
     * Ctor
     */
	private  B2buaHelperImpl(){
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(B2buaHelperImpl.class.getName(),
					"B2buaHelperImpl");
		}
	}
	
    /**
     * Get singleton instance 
     * @param failover
     * @return
     */
    public static B2buaHelperImpl getInstance(){
    	if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(B2buaHelperImpl.class.getName(), "getInstance");
		}
    	
    	if( _instance == null){
	    	synchronized (_lock) {
	    		if( _instance == null){
	    			_instance = new B2buaHelperImpl();
	    		}
			}
    	}
    	return _instance;
    }
    
	
	/**
	 * @see javax.servlet.sip.B2buaHelper#createRequest(javax.servlet.sip.SipServletRequest, boolean, java.util.Map)
	 */
	public SipServletRequest createRequest(SipServletRequest origRequest, 
											boolean linked, 
											Map<String,List<String>> headerMap)
		throws IllegalArgumentException, TooManyHopsException{
		
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { origRequest.getClass().getName() + "@" + Integer.toHexString(origRequest.hashCode())
					, linked, headerMap };
			c_logger.traceEntry(B2buaHelperImpl.class.getName(),
					"createRequest", params);
		}

		verifyNotTooManyHops(origRequest);		
		
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "createRequest", 
					"passed validation, creating new request");
		}
		
		OutgoingSipServletRequest outRequest = createOutgoingRequest(origRequest);
		// Noam: marking new TU as b2b, this will cause incoming messages to be added to the pending
		// messages.
		outRequest.getTransactionUser().setIsB2bua(true);
		
		
		if(linked){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "createRequest", 
						"linking newly created session with old one.");
			}
			linkSipSessions(origRequest.getSession(), outRequest.getSession());
			linkSipRequests((SipServletRequestImpl)origRequest,(SipServletRequestImpl)outRequest);
		}
		else {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "createRequest", 
						"NOT linking newly created session with old one.");
			}
		}
		
		decrementMaxForwards(origRequest, outRequest);
		
		copyHeadersMap(headerMap, outRequest, true);
		copyBody(origRequest, outRequest);
		
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(B2buaHelperImpl.class.getName(), "createRequest",
					outRequest);
		}
		return outRequest;
	}

	
	/**
	 * The value of the new requests Max-Forwards header is set to 
	 * that of the original request minus one.
	 *  
	 * @param origRequest
	 * @param outRequest
	 */
	private void decrementMaxForwards(SipServletRequest origRequest, 
							   SipServletRequest outRequest) {
		
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(B2buaHelperImpl.class.getName(),
					"decrementMaxForwards");
		}
			outRequest.setMaxForwards(origRequest.getMaxForwards()-1);
	}

	/**
	 * @see javax.servlet.sip.B2buaHelper#createRequest
	 * (javax.servlet.sip.SipSession, javax.servlet.sip.SipServletRequest, java.util.Map)
	 */
	public SipServletRequest createRequest(SipSession session, 
										SipServletRequest origRequest, 
										Map<String,List<String>> headerMap)
	throws IllegalArgumentException {
		
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { session,
				origRequest.getClass().getName() + "@" + Integer.toHexString(origRequest.hashCode()),
				headerMap };
			c_logger.traceEntry(B2buaHelperImpl.class.getName(),
					"createRequest", params);
		}
		
		verifySameAppSession(session.getApplicationSession().getId(),
					origRequest.getSession().getApplicationSession().getId());
		
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "createRequest", 
					"passed validation, creating new request");
		}
	
		OutgoingSipServletRequest outRequest = (OutgoingSipServletRequest)session.createRequest(origRequest.getMethod());

		if(!isSessionsLinked(origRequest.getSession(),outRequest.getSession())){
			linkSipSessions(origRequest.getSession(), outRequest.getSession());
		}
		
		// since this method is called to create a subsequent request, we should always link the two SipServletRequests
		linkSipRequests((SipServletRequestImpl)origRequest, (SipServletRequestImpl)outRequest);
		
		SipServletsFactoryImpl.getInstance().copyNonSystemHeaders(origRequest, outRequest);
		copyHeadersMap(headerMap, outRequest,false);
		copyBody(origRequest, outRequest);
		
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(B2buaHelperImpl.class.getName(), "createRequest",
					outRequest);
		}
		return outRequest;
	}

	/**
	 * Check if the requests are already linked.
	 *  
	 * @param req1
	 * @param req2
	 * 
	 * @return isRequestsLinked
	 */
	private boolean isRequestsLinked(SipServletRequestImpl req1, SipServletRequestImpl req2)
	{
		boolean isRequestsLinked = false;
		if(req1!= null && req2!=null){
			
			if (req1.getLinkedRequest() == req2) {
				isRequestsLinked = true;
			}
		}
		
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(B2buaHelperImpl.class.getName(), "isRequestsLinked",isRequestsLinked);
		}
		return isRequestsLinked;
	}
	
	/**
	 * Check if the sessions are already linked.
	 *  
	 * @param sessionImpl1
	 * @param sessionImpl2
	 * 
	 * @return isSessionsLinked
	 */
	private boolean isSessionsLinked(SipSession sessionImpl1, 
			SipSession sessionImpl2)
	{
		boolean isSessionsLinked = false;
		if(sessionImpl1!= null && sessionImpl2!=null){
			SipSessionImplementation sess1 = (SipSessionImplementation)sessionImpl1;
			SipSessionImplementation sess2 = (SipSessionImplementation)sessionImpl2;
			
			if(sess1.getLinkedSession()==sess2){
				isSessionsLinked = true;
			}
		}
		
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(B2buaHelperImpl.class.getName(), "isSessionsLinked",isSessionsLinked);
		}
		return isSessionsLinked;
	}

	/**
	 * @see javax.servlet.sip.B2buaHelper#createResponseToOriginalRequest
	 * (javax.servlet.sip.SipSession, int, java.lang.String)
	 */
	public SipServletResponse createResponseToOriginalRequest(SipSession session, 
				int status, String reasonPhrase)
		throws IllegalStateException, IllegalArgumentException {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { session, status, reasonPhrase };
			c_logger.traceEntry(B2buaHelperImpl.class.getName(),
					"createResponseToOriginalRequest", params);
		}	
		
		// @throws IllegalArgumentException - if the session is invalid
		verifyValid(session);
		verifyOriginal(session);
		
		SipSessionImplementation origSession = (SipSessionImplementation) session;		
		verifyConsistentResponse(origSession, status);
		
		TransactionUserWrapper tuToUseForUpdate = origSession.getTransactionUser();
		
		//	This response should create new DerivedSession and Application is the 
		//	one that responsible to link between the sessions because it passed 
		//	original SipSession and not created one when additional response received.
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceDebug(this,"createResponseToOriginalRequest",
					"B2BHelper have created DerivedSession and Application" +
					" is responsible to link it to the correct UAC session");
		}
		
		TransactionUserWrapper derivedTU = tuToUseForUpdate.createDerivedTU(null,
				"B2BuaHelperImpl - createResponse to original request");
		tuToUseForUpdate = derivedTU;
		
		SipServletRequest origRequest = tuToUseForUpdate.getSipMessage();
		
		if(origRequest == null || ! (origRequest instanceof IncomingSipServletRequest)){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "createResponseToOriginalRequest", "ERROR, can't get the original request");
			}
			
			throw new NullPointerException("Can not retrieve the original request");
		}
		
		// set flag that this response should be sent over stack immediate (no server transaction) 
		// call for tu.onSendingResponse() when sending.
		SipServletResponse outResponse = 
			((IncomingSipServletRequest)origRequest).
						createResponseForCommitedRequest(status,reasonPhrase);
		
		return outResponse;
	} 

	
	/**
	 * @see javax.servlet.sip.B2buaHelper#getLinkedSession
	 * (javax.servlet.sip.SipSession)
	 */
	public SipSession getLinkedSession(SipSession session)
		throws IllegalArgumentException {
		
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { session };
			c_logger.traceEntry(B2buaHelperImpl.class.getName(),
					"getLinkedSession", params);
		}
		
		verifyValid(session);
		
		
		SipSession linkedSession = ((SipSessionImplementation)session).getLinkedSession();
						
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(B2buaHelperImpl.class.getName(), "getLinkedSession",linkedSession);
		}
		return linkedSession;
	}
	
	/**
	 * @see javax.servlet.sip.B2buaHelper#getLinkedSipServletRequest
	 * 	(javax.servlet.sip.SipServletRequest)
	 */
	public SipServletRequest getLinkedSipServletRequest(SipServletRequest req){
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { req.getClass().getName() + "@" + Integer.toHexString(req.hashCode()) };
			c_logger.traceEntry(B2buaHelperImpl.class.getName(),
					"getLinkedSipServletRequest", params);
		}
		SipServletRequest linkedReq = 
			((SipServletRequestImpl)req).getLinkedRequest();
		
		if(c_logger.isTraceEntryExitEnabled()){
			if(linkedReq!=null){
				c_logger.traceExit(B2buaHelperImpl.class.getName(), 
						"getLinkedSipServletRequest",linkedReq.getClass().getName() + "@" + Integer.toHexString(linkedReq.hashCode()));
			}else {
				c_logger.traceExit(B2buaHelperImpl.class.getName(), 
						"getLinkedSipServletRequest",linkedReq);
			}
		}
		return linkedReq;
	}

	 
	/**
	 * @see javax.servlet.sip.B2buaHelper#getPendingMessages
	 * (javax.servlet.sip.SipSession, javax.servlet.sip.UAMode)
	 */
	public List<SipServletMessage> getPendingMessages(SipSession session, 
			UAMode mode) throws IllegalArgumentException {
		
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { session, mode };
			c_logger.traceEntry(B2buaHelperImpl.class.getName(),
					"getPendingMessages", params);
		}
		
		verifyValid(session);
		
		SipSessionImplementation sessionImpl = (SipSessionImplementation)session;
		
		List<SipServletMessage> messages = sessionImpl.getPendingMessages(mode);
		
		//we need to clone the pending list before we return it to the application.
		//the application can change the underline list as a side effect while
		//iterating over it and we want to prevent such situation.
		List<SipServletMessage> clonedMessages = null;
		if (messages != null){
			if(c_logger.isTraceEntryExitEnabled()){
				c_logger.traceExit(B2buaHelperImpl.class.getName(), "getPendingMessages","cloneing pending messages " + messages);
			}
			clonedMessages = (List<SipServletMessage>) ((LinkedList<SipServletMessage>)messages).clone();
		}
		
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(B2buaHelperImpl.class.getName(), 
					"getPendingMessages",clonedMessages);
		}
		
		return clonedMessages;
	} 

	/**
	 * @see javax.servlet.sip.B2buaHelper#linkSipSessions
	 * (javax.servlet.sip.SipSession, javax.servlet.sip.SipSession)
	 */
	public void linkSipSessions(SipSession session1, SipSession session2)
		throws IllegalArgumentException {
			
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { session1, session2 };
			c_logger.traceEntry(B2buaHelperImpl.class.getName(),
					"linkSipSessions", params);
		}
		if (session1.getState() == SipSessionImplementation.State.TERMINATED || session2.getState() == SipSessionImplementation.State.TERMINATED) {
			throw new IllegalArgumentException(
					"The session: " + session1 + " is terminated");
		}
		
		
		SipSessionImplementation sessionImpl1 = (SipSessionImplementation)session1;
		SipSessionImplementation sessionImpl2 = (SipSessionImplementation)session2;
		
		verifySameAppSession(sessionImpl1.getApplicationSessionId(),
				sessionImpl2.getApplicationSessionId());

		
		verifyNotTerminated(sessionImpl1, sessionImpl2);
		verifyNotAlreadyLinked(sessionImpl1, sessionImpl2);
		
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "linkSipSessions", "linking the 2 sessions");
		}
		sessionImpl1.linkSipSession(sessionImpl2);
		sessionImpl2.linkSipSession(sessionImpl1);
		
//		// The linkage at the SipServletRequest level is implicit whenever a new request is created based on the original with link argument as true. 
//		// There is no explicit linking/unlinking of SipServletRequests.
		linkSipRequests((SipServletRequestImpl) sessionImpl1.getTransactionUser().getSipServletRequest(), 
				(SipServletRequestImpl) sessionImpl2.getTransactionUser().getSipServletRequest());
		
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(B2buaHelperImpl.class.getName(), "linkSipSessions");
		}
	}
	
	/**
	 * Link together the two SipServletRequests.
	 * 
	 * @param req1 - the first SipServletRequest to link
	 * @param req2 - the other SipServletRequest to link
	 */
	private void linkSipRequests(SipServletRequestImpl req1, SipServletRequestImpl req2) {
		
		if (req1 == null || req2 == null) {
			// TODO: we saw some case when one of the SIP requests is null (for example create INFO request)
			// need to check when and if this is not a problem that a SIP request is null
			// meanwhile don't link the requests 
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug("linkSipRequest for a null request invoked");
			}
			return;
		}
			
		if(isRequestsLinked(req1, req2)) {
			return;
		}
		
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { 
				req1.getClass().getName() + "@" + Integer.toHexString(req1.hashCode()),
				req2.getClass().getName() + "@" + Integer.toHexString(req2.hashCode())};
			c_logger.traceEntry(B2buaHelperImpl.class.getName(),
					"linkSipRequests", params);
		}
		
		req1.linkSipRequest(req2);
		req2.linkSipRequest(req1);
		
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(B2buaHelperImpl.class.getName(), "linkSipRequests");
		}
	}
	
	/**
	 * unlink SipServletRequest.
	 * 
	 * @param req - the SipServletRequest to unlink
	 */
	private void unlinkSipRequest(SipServletRequestImpl req) {
		
		if (req == null) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug("unlinkSipRequest for a null request invoked");
			}
			return;
		}
				
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { 
				req.getClass().getName() + "@" + Integer.toHexString(req.hashCode())};
			c_logger.traceEntry(B2buaHelperImpl.class.getName(),
					"unlinkSipRequests", params);
		}
		
		req.unLinkSipRequest();
		
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(B2buaHelperImpl.class.getName(), "unlinkSipRequests");
		}
	}

	/**
	 * @see javax.servlet.sip.B2buaHelper#unlinkSipSessions
	 * (javax.servlet.sip.SipSession)
	 */
	public void unlinkSipSessions(SipSession session)
		throws IllegalArgumentException {
		
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { session };
			c_logger.traceEntry(B2buaHelperImpl.class.getName(),
					"unlinkSipSessions", params);
		}
		if (session.getState() == SipSessionImplementation.State.TERMINATED) {
			throw new IllegalArgumentException(
					"The session: " + session + " is terminated");
		}
		
		SipSessionImplementation session1 = ((SipSessionImplementation)session);
		SipSessionImplementation session2 = session1.getLinkedSession();
		
		// throws IllegalArgumentException - if the session is not currently linked
		// to another session 
		if (session2 == null) {
			throw new IllegalArgumentException(
					"The session: " + session1 + " is not currently linked to another session");
		}
		
		if (session2.isValid()) {
			session2.unlinkSipSession();
			if (!session2.getTransactionUser().isInvalidating()) {
				unlinkSipRequest((SipServletRequestImpl) session2.getTransactionUser().getSipServletRequest());
			}
		}
		else {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "unlinkSipSession was not invoked for session: ", session2.getId());
			}
		}
		session1.unlinkSipSession();
		unlinkSipRequest((SipServletRequestImpl) session1.getTransactionUser().getSipServletRequest());
		
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(B2buaHelperImpl.class.getName(), "unlinkSipSessions");
		}
	}
		
	/**
	 * Copy headers map to the outgoing message.
	 * 
	 * @param headerMap headerMap - a simple map containing header names 
	 * 		and their values to be overridden in the new request. 
	 * 		The values can be a Set to accomodate for multi-valued headers 
	 * @param outMsg
	 * @param isToFromAlowed indicate is it allowed to insert To From tags
	 */
	private void copyHeadersMap(Map<String,List<String>> headerMap, 
				OutgoingSipServletRequest outMsg, boolean isToFromAllowed ) {
		if (headerMap == null)
			return;
		
		for (Entry <String,List<String>> entry: headerMap.entrySet()) {
			String headerName = entry.getKey();
			List <String> values = entry.getValue();

			if (!ContactHeader.name.equalsIgnoreCase(headerName)){
				// copy headers for all values
				if (values != null && values.size() > 0){
					outMsg.addHeaderToFromAllowed(headerName, values, isToFromAllowed);
				}			
			} else {
				String contact = values.get(0/*values.size() - 1*/);
				
				//setting contact allowed parts
				try {
					AddressImpl addr = (AddressImpl)outMsg.getAddressHeader(ContactHeader.name);
					//ContactHeaderImpl header = (ContactHeaderImpl)StackProperties.getInstance().getHeadersFactory().createHeader(ContactHeader.name, contact);
					Address receivedContact = SipServletsFactoryImpl.getInstance().createAddress(contact);
					addr.setURI(receivedContact.getURI());//This will set only the allowed parts. 
				} catch (ServletParseException e) {
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "Error: B2buaHelperImpl copyHeadersMap could not create contact header ",
								e.getMessage());
					}
				}				
			}
		}
	}

	
	/**
	 * Copy body contents from one message into another
	 * 
	 * @param src source message to copy from
	 * @param dst destination message to copy to
	 * 
	 */
	private void copyBody(SipServletMessage src, SipServletMessage dst) {
		if (src.getContentLength() > 0) {
			try {
				dst.setContent(src.getContent(), src.getContentType());
				return;
			}
			catch (UnsupportedEncodingException e) {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "B2buaHelperImpl copyBody got UnsupportedEncodingException: ",
							e.getMessage());
				}
			}
			catch (IOException e) {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "B2buaHelperImpl copyBody got IOException: ",
							e.getMessage());
				}
			}
		}
	}
	
	/**
	 * Throw IllegalArgumentException for invalid session
	 * 
	 * @param session The session object to verify
	 * 
	 * @throws IllegalArgumentException
	 */
	private void verifyValid(SipSession session) throws IllegalArgumentException {
		if (!((SipSessionImplementation)session).isValid())
		{
			throw new IllegalArgumentException("The session is invalid");
		}
	}
	
	/**
	 * Throw IllegalArgumentException if the session is derived
	 * 
	 * @param session The session object to verify
	 * 
	 * @throws IllegalArgumentException
	 */
	private void verifyOriginal(SipSession session) throws IllegalArgumentException {
		
		// verify that the passed session is not derived
		TransactionUserWrapper tu = ((SipSessionImplementation)session).getTransactionUser();
		if (tu != null && tu.isDerived()) {
			throw new IllegalArgumentException("The passed session is not the original one");
		}
	}
	
	/**
	 * This method perform loop detection. 
	 * If the value of the original request's Max-Forwards header field is 0, 
	 * then TooManyHopsException is thrown and a 483 (Too many hops) response
	 * is sent for the original request.
	 * 
	 * @param origRequest The original request
	 * @throws TooManyHopsException in case the maxForwards value is 0
	 */
	private void verifyNotTooManyHops(SipServletRequest origRequest) 
		throws TooManyHopsException {
		
		if(origRequest.getMaxForwards()==0){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "verifyNotTooManyHops", "loop detected");
			}
// There was no requirment in the JSR to send such response, so it is commented out for now.			
//			try {
//				SipServletResponse response = origRequest.createResponse(
//					SipServletResponse.SC_TOO_MANY_HOPS,"The value of the " +
//					"original request's Max-Forwards header field is 0");
//				response.send();
//			} catch (IOException e) {
//				if (c_logger.isTraceDebugEnabled()) {
//					c_logger.traceDebug(this, "verifyNotTooManyHops", "got IOException " + e);
//				}
//			}
			
//			throw new TooManyHopsException("The value of the original request's" +
//					" Max-Forwards header field is 0");
			
			// Hide Java_289 API
			throw new TooManyHopsException();
		}
	}
	
	/**
	 * Throw IllegalStateException in case the id1 is not the same as id2
	 * 
	 * @param id1
	 * @param id2
	 * 
	 * @throws IllegalStateException in case the id1 is not the same as id2
	 */
	private void verifySameAppSession(String id1, String id2)
		throws IllegalStateException {
		
		if (!id1.equals(id2)){
			throw new IllegalStateException("The objects are not belong to the " +
					"same SipApplicationSession");
		}
	}
	
	/**
	 * Throw IllegalStateException in case if a subsequent response is inconsistent
	 * with an already sent response.
	 * For example, a 400 response following a 200 OK response.
	 *  
	 * @param session The session object
	 * @param status the status of the subsequent response
	 * @throws IllegalStateException
	 */
	private void verifyConsistentResponse(SipSessionImplementation sessionImpl, int status)
		throws IllegalStateException {
		
		if(sessionImpl.isFailedResponseSent()){
			throw new IllegalStateException("The subsequent response is " +
				"inconsistent with an already sent response.");
		}
		
		// @throws IllegalStateException - if a subsequent response is inconsistent	with an already sent response. 
		// For example, a 400 response following a 200 OK response.
		if (sessionImpl.getState() == State.CONFIRMED && status >= 300) {
			throw new IllegalStateException("The subsequent response is " +
					"inconsistent with an already sent response. The session was already confirmed.");
		}
		
		// verify the original request which caused to create the transaction is not ACKed yet
		// The object m_sipMessage on TUWrapper represents the original SIP request,
		// thus m_sipMessage will be null if we already received ACK on a previously sent response 
		TransactionUserWrapper tu = sessionImpl.getTransactionUser();
		if (tu.getSipMessage() == null) {
			throw new IllegalStateException("The subsequent response is " +
					"inconsistent with an already sent response. ACK was already received.");
		}

	}
	
	/**
	 * Throw IllegalArgumentException in case one of the session is terminated
	 *  
	 * @param sessionImpl1
	 * @param sessionImpl2
	 * 
	 * @throws IllegalArgumentException
	 */
	private void verifyNotTerminated(SipSessionImplementation sessionImpl1, 
			SipSessionImplementation sessionImpl2) 
	throws IllegalArgumentException {
		if ( sessionImpl1.isTerminated() || sessionImpl2.isTerminated())
		{
			throw new IllegalArgumentException(
					"The specified sessions	has been terminated");
		}
	}
	
	/**
	 * Throw IllegalArgumentException one or both the sessions are already
	 *  linked with some other session(s)
	 *  
	 * @param sessionImpl1
	 * @param sessionImpl2
	 * 
	 * @throws IllegalArgumentException
	 */
	private void verifyNotAlreadyLinked(SipSessionImplementation sessionImpl1, 
			SipSessionImplementation sessionImpl2) 
	throws IllegalArgumentException {
		SipSessionImplementation alreadyLinked1 = sessionImpl1.getLinkedSession();
		SipSessionImplementation alreadyLinked2 = sessionImpl2.getLinkedSession();
		if (alreadyLinked1 != null || alreadyLinked2 != null){
			if (alreadyLinked1 != alreadyLinked2)
			{
				throw new IllegalArgumentException("one or both the sessions" +
						" are already linked with some other session(s) ");
			}
		}
	}

	/**
	 * @see javax.servlet.sip.B2buaHelper#createCancel(javax.servlet.sip.SipSession)
	 */
	public SipServletRequest createCancel(SipSession sipSession) throws NullPointerException {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { sipSession };
			c_logger.traceEntry(null, "createCancel", params);
		}
		if (sipSession == null) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "createCancel", "null session was passed in.  Throwing NullPointerException");
			}
			throw new NullPointerException("SipSession is null");
		}
		SipSessionImplementation session = (SipSessionImplementation)sipSession;
		SipServletRequest request = (SipServletRequest)session.getTransactionUser().getSipServletRequest();
		//TODO why is it returning message and not request??
		
		SipServletRequest cancel = request.createCancel();
		
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(null, "createCancel");
		}
		return cancel;
	}

	/**
	 * @see javax.servlet.sip.B2buaHelper#createRequest(javax.servlet.sip.SipServletRequest)
	 */
	public SipServletRequest createRequest(SipServletRequest origRequest) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { origRequest };
			c_logger.traceEntry(null, "createRequest", params);
		}
		/* 
		 * TODO some issues are still not clear:
		 * 1. Should the sessions be linked here
		 * 2. how to handle TooManyHopsException , if at all
		 * 3. How to enforce this javadoc instruction:
		 *  "It is used only for the initial request. 
		 * Subsequent requests in either 
		 * leg must be created using SipSession.createRequest(java.lang.String) 
		 * or createRequest(SipSession, SipServletRequest, java.util.Map) 
		 * as usual. "
		 * (throw exception, ignore ?)
		 */
		SipServletRequest req = createOutgoingRequest(origRequest);
		
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(B2buaHelperImpl.class.getName(), "createRequest",
					req);
		}
		return req;
	}
	
	/**
	 * Creating an outgoing SIP servlet request for the outbound leg
	 * @param origRequest
	 * @return
	 */
	private OutgoingSipServletRequest createOutgoingRequest(SipServletRequest origRequest){
		boolean useInboundCallID = 
			PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.B2BUAHELPER_USE_INBOUND_CALL_ID_FOR_OUTBOUND_REQUEST);
		SipServletsFactoryImpl factory = SipServletsFactoryImpl.getInstance();
		OutgoingSipServletRequest req = (OutgoingSipServletRequest)factory.createRequest(origRequest, useInboundCallID);
		return req;
	}
}

