/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package javax.servlet.sip;

import java.util.List;
import java.util.Map;

/**
 * Helper class providing support for B2BUA applications. 
 *  
 * @author Assya Azrieli
 */
public interface B2buaHelper {


	/**
	 * Creates a new CANCEL request to cancel the initial request sent on the other leg. 
	 * The CANCEL is created by the container using the initial request stored in the session corresponding to the other leg. 
	 *
	 * @param sipSession - the session whose initial request is to be cancelled. 
	 * @return the new CANCEL request to be sent on the other leg
	 * 
	 * @throws NullPointerException - if the session is null 
	 */
	SipServletRequest createCancel(SipSession sipSession) throws NullPointerException ; 

	
	
	/**
	 * Creates a new request object belonging to a new SipSession. 
	 * The new request is similar to the specified origRequest  in that 
	 * the method and the majority of header fields are copied from 
	 * origRequest to the new request. The SipSession created for the 
	 * new request also shares the same SipApplicationSession associated 
	 * with the original request.
	 * 
	 * This method satisfies the following rules:
	 * 
	 * - The From header field of the new request has a new tag chosen by the container.
	 * - The To header field of the new request has no tag.
	 * - The new request (and the corresponding SipSession)is assigned a new Call-ID.
	 * - Record-Route and Via header fields are not copied. As usual, the container will 
	 * add its own Via header field to the request when it's actually sent outside the application server.
	 * - For non-REGISTER requests, the Contact header field is not copied but 
	 * is populated by the container as usual. 
	 * 
	 * This method provides a convenient and efficient way of constructing 
	 * a new "leg" of a B2BUA application. It is used only for the initial request. 
	 * Subsequent requests in either leg must be created using 
	 * SipSession.createRequest(java.lang.String) or 
	 * createRequest(SipSession, SipServletRequest, java.util.Map) as usual. 
	 * 
	 *
	 * @param origRequest - request to be "copied"
	 * 
	 * @return the "copied" request object
	 * 
	 * @throws IllegalArgumentException - if the headerMap contains a system 
	 * header other than From/To/Contact or some other header not relevant for 
	 * the context, or the origRequest and its session is linked to some other 
	 * request/session and the linked flag is true
	 * header value is 0
	 */
	SipServletRequest createRequest(SipServletRequest origRequest); 
	

	
	/**
	 * Creates a new request object belonging to a new SipSession. 
	 * The new request is similar to the specified origRequest in that the 
	 * method and the majority of header fields are copied from origRequest 
	 * to the new request. 
	 * The headerMap parameter can contain From and To headers and any non 
	 * system header. 
	 * The header field map is then used to override the headers in the newly
	 * created request. 
	 * The SipSession created for the new request also shares the same 
	 * SipApplicationSession associated with the original request.
	 * This method satisfies the following rules: 
	 * - Whether the From header is overridden through the headerMap or not the
	 * From header field of the new request has a new tag chosen by the container.
	 * - If the From header is included in the headerMap and has a tag then it 
	 * is ignored and container chosen tag is inserted instead.
	 * - The To header field of the new request has no tag.
	 * - Record-Route and Via header fields are not copied. 
	 * As usual, the container will add its own Via header field to the request
	 * when it's actually sent outside the application server.
	 * - For non-REGISTER requests, the Contact header field is not copied but
	 * is populated by the container as usual but if Contact header is present
	 * in the headerMap then relevant portions of Contact header is to be used
	 * in the request created, in accordance with section 4.1.3 of the specification.
	 * - This method provides a convenient and efficient way of constructing 
	 * the second "leg" of a B2BUA application, giving the additional 
	 * flexibility of changing the headers including To and From.
	 * - This method will also perform loop detection. 
	 * If the value of the original request's Max-Forwards header field is 0, 
	 * then TooManyHopsException is thrown and a 483 (Too many hops) response 
	 * is sent for the original request. 
	 * Otherwise, the value of the new requests Max-Forwards header is set to 
	 * that of the original request minus one. 
	 *
	 * @param origRequest - request to be "copied"
	 * @param linked - indicating if the resulting session should be linked with
	 *  original or not
	 * @param headerMap - a simple map containing header names and their values
	 *  to be overridden in the new request. 
	 *  The values can be a Set to accomodate for multi-valued headers
	 * 
	 * @return the "copied" request object
	 * 
	 * @throws IllegalArgumentException - if the headerMap contains a system 
	 * header other than From/To/Contact or some other header not relevant for 
	 * the context, or the origRequest and its session is linked to some other 
	 * request/session and the linked flag is true

	 * @throws NullPointerException - if the original request is null
	 * @throws TooManyHopsException - if the original request's Max-Forwards 
	 * header value is 0
	 */
	
	SipServletRequest createRequest(SipServletRequest origRequest, 
									boolean linked, 
									Map<String,List<String>> headerMap)
							throws IllegalArgumentException, TooManyHopsException; 

	/**
	 * Creates a new subsequent request based on the specified original request.
	 * This results in automatically linking the two SipSessions 
	 * (if they are not already linked) and the two SipServletRequests.
	 * This method, though similar to the factory method of creating the 
	 * request for a B2BUA for initial requests, is to be used for subsequent 
	 * requests. 
	 * The semantics are similar to SipSession.createRequest(String) except that
	 * here it copies non system headers from the original request onto the new 
	 * request, the system headers are created based on the session that this 
	 * request is created on. 
	 * Further the Route headers are set as based on the session route set. 
	 * The method of the new request is same as that of the origRequest. 
	 * If Contact header is present in the headerMap then relevant portions of 
	 * Contact header is to be used in the request created, 
	 * in accordance with section 4.1.3 of the specification.
	 * 
	 * @param session - the session on which this request is to be created
	 * @param origRequest - the original request
	 * @param headerMap - the header map used to override the headers in the 
	 * next request that created
	 * 
	 * @return the new request
	 * 
	 * @throws IllegalArgumentException - if the header map contains a system 
	 * header other than Contact or other header which does not makes sense 
	 * in the context, or in case when the session does not belong to the 
	 * same SipApplicationSession as the origRequest, 
	 * or the original request or session is already linked with some other request/session
	 * @throws NullPointerException - if the original request or the session is null
	 * @throws TooManyHopsException - - if the original request's Max-Forwards header value is 0
	 */ 
	SipServletRequest createRequest(SipSession session, 
									SipServletRequest origRequest,
									Map<String,List<String>> headerMap) 
							throws IllegalArgumentException, NullPointerException;

	/**
	 * The request that results in creation of a SipSession is termed as the 
	 * original request, a response to this original request can be created by 
	 * the application even if the request was committed and application does 
	 * not have a reference to this Request.
	 * 
	 * @param session - the SipSession for the original request
	 * @param status - the status code for the response
	 * @param reasonPhrase - the reason phrase for the response, 
	 *  					or null to use a default reason phrase
	 * @return the resulting SipServletResponse 
	 * @throws IllegalStateException - if a subsequent response is inconsistent
	 * 		 		with an already sent response. 
	 * 				For example, a 400 response following a 200 OK response. 
	 * @throws IllegalArgumentException - if the session is invalid 
	 */ 
	SipServletResponse createResponseToOriginalRequest(SipSession session, 
				int status, String reasonPhrase) throws IllegalStateException, IllegalArgumentException; 

	/**
	 * Returns the other SipSession that is linked to the specified SipSession,
	 * or null if none.
	 * 
	 * @param session - the SipSession from which to obtain the linked session
	 * @return the linked SipSession, or null if none
	 * 
	 * @throws IllegalArgumentException - if the session is invalid 
	 */ 
	SipSession getLinkedSession(SipSession session) throws IllegalArgumentException;
	
	/**
	 * This method is to be used to retrieve the implicitly linked request. 
	 * 
	 * @param req - other request
	 * @return the linked request, or null if there is no valid 
	 * 				linked session or request
	 */
	SipServletRequest getLinkedSipServletRequest(SipServletRequest req);

	/**
	 * For the specified SipSession, returns a List of all uncommitted messages
	 * in the order of increasing CSeq number for the given mode of the session.
	 * 
	 * @param session - the SipSession to check for pending messages
	 * @param mode - the mode for which the pending messages are required, 
	 * 				one of UAC or UAS 
	 * 
	 * @return the list of SipServletMessage objects representing pending 
	 * 			messages for the session, or the empty list if none
	 *  
	 * @throws IllegalArgumentException - if the session is invalid 
	 */ 
	List<SipServletMessage> getPendingMessages(SipSession session, 
			UAMode mode) throws IllegalArgumentException; 

	/**
	 * Links the specified sessions, such that there is a 1-1 mapping between them.
	 * Each session can retrieved from the other by calling 
	 * getLinkedSession(javax.servlet.sip.SipSession). 
	 * One SipSession at any given time can be linked to only one other 
	 * SipSession belonging to the same SipApplicationSession.
	 * 
	 * @param session1 - the first SipSession to link
	 * @param session2 - the other SipSession to link
	 *  
	 * @throws IllegalArgumentException - 
	 * 	if either of the specified sessions has been terminated
	 * 	or the sessions do not belong to the same application session 
	 * 	or one or both the sessions are already linked with some other session(s) 
	 * 
	 * @throws NullPointerException - if any of the arguments is null
	 */ 
	void linkSipSessions(SipSession session1, SipSession session2) 
						throws IllegalArgumentException, NullPointerException;

	/**
	 * If the specified SipSession is linked to another session, 
	 * then unlinks the two sessions from each other.
	 * 
	 * @param session - the session to be unlinked
	 * @throws IllegalArgumentException - if the session is not currently linked
	 *  to another session or it has been terminated 
	*/ 
	void unlinkSipSessions(SipSession session) throws IllegalArgumentException;
}