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
package com.ibm.ws.sip.security.auth;

import jain.protocol.ip.sip.header.SecurityHeader;

import java.io.IOException;
import java.util.Random;
import java.util.StringTokenizer;

import javax.servlet.sip.SipServletResponse;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.sip.container.servlets.SipServletMessageImpl;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.servlets.SipServletResponseImpl;

/**
 * A single authentication header.
 * @author dedi
 */
class AuthHeader {

	/**
     * Our logger object.
     */
    private static final LogMgr c_logger = Log.get(AuthHeader.class);
	
	/**
	 * The constant for the nonce-count value. Since we currently don't support
	 * using the same nonce several times (and, acording to Dror, no SIP server
	 * currently does), this can safetly be hard-coded to 1.
	 */
	private final static String NONCE_COUNT_VALUE = "00000001";
	
	/**
	 * A random generator for creating the client-side nonce. TBD: should this
	 * be a secure random generator?
	 */
	private static Random s_rndGenerator = new Random();
	
	/**
	 * The status code of the response that generated the need for 
	 * authentication (401 or 407).
	 */
	private int _statusCode;

	/**
	 * The authentication realm.
	 */
	private String _realm;
	
	/**
	 * The auth username.
	 */
	private String _username;
	
	/**
	 * The auth password.
	 */
	private String _password;

	/**
	 * Create a new auth header.
	 * @param statusCode The status code of the response (401 or 407)
	 * @param realm The authentication realm.
	 * @param username The auth username.
	 * @param password The auth password.
	 */
	public AuthHeader(int statusCode, String realm, String username,
			String password)
	{
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "<init>", new Object[] {statusCode, realm, 
					username, "*****"});
		}
		_statusCode = statusCode;
		_realm = realm;
		_username = username;
		_password = password;

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "<init>");
		}
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return _password;
	}

	/**
	 * @return the realm
	 */
	public String getRealm() {
		return _realm;
	}

	/**
	 * @return the statusCode
	 */
	public int getStatusCode() {
		return _statusCode;
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return _username;
	}
	
	/**
	 * Write this auth header to a sip request.
	 * @param request The SIP Request to write to
	 * @param response The SIP response that contained the auth challange.
	 */
	public void writeToRequest(SipServletRequestImpl request, 
			SipServletResponseImpl response)
	{
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "writeToRequest", 
					new Object[] {request, response});
		}
		// The auth challange header we should use is the one for our realm.
		SecurityHeader challange = response.getAuthHeader(_realm);
		
		String nonce = challange.getParameter(DigestConstants.PROPERTY_NONCE);
		String qopParam = challange.getParameter(DigestConstants.PROPERTY_QOP);
		String qop = chooseBestQop(qopParam);
		String algorithm = 
			challange.getParameter(DigestConstants.PROPERTY_ALGORITHM);	
		String opaque = challange.getParameter(DigestConstants.PROPERTY_OPAQUE);
		
		String sipMethod = request.getMethod();
		String uri = request.getRequestURI().toString();

		// Trying to get the message body could (in theory) result in an error,
		// so only get it if we really need to embed the body hash in the 
		// digest.
		byte[] body = null;
		if (qop != null && qop.equals(DigestConstants.QOP_AUTH_INT)) {
			body = getBody(request);
		}
		
		String nc = NONCE_COUNT_VALUE;  

		// The cnonce value should just be an arbitrary value chosen by the 
		// client to avoid server-initiated replay attacks. We use both
		// the current time and a random to make it harder for the remote side
		// to guess this value in advance.
		long cnonceVal = System.currentTimeMillis() | s_rndGenerator.nextLong();
		String cnonce = String.valueOf(cnonceVal);

		// Is this a proxy or a server authentication request?
		boolean isServerAuthentication = 
			(response.getStatus() == SipServletResponse.SC_UNAUTHORIZED);
		String headerName = 
			(isServerAuthentication ? AuthorizationConstants.AUTHORIZATION_HEADER
				: AuthorizationConstants.PROXY_AUTHORIZATION_HEADER);
		String headerValue = createHeaderString(sipMethod, nonce, qop, nc, 
				cnonce, uri, algorithm, opaque, body);
		request.addHeader(headerName, headerValue);

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "writeToRequest");
		}
	}
	
	/**
	 * Choose the strongest supported QOP value from a list of qOP values passed
	 * as a comma-separated string.
	 * 
	 * @param qopList
	 *            a comma-separated list of known QOP values.
	 * @return The strongest supported method from the list, or null if no
	 *         method is supported.
	 */
	private String chooseBestQop(String qopList) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "chooseBestQop", qopList);
		}
		if (qopList == null) {
			return null;
		}

		String retVal = null;
		StringTokenizer tokenizer = new StringTokenizer(qopList, ",");
		while (tokenizer.hasMoreTokens()) {
			String currValue = tokenizer.nextToken();
			if (currValue.equals(DigestConstants.QOP_AUTH_INT)) {
				retVal = currValue;
				// This is the strongest value we support, so look no further.
				break;
			} else if (currValue.equals(DigestConstants.QOP_AUTH)) {
				retVal = currValue;
			}
		}		

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "chooseBestQop", retVal);
		}

		return retVal;
	}

	/**
	 * Helper method: get the message body of the given SIP message, handling
	 * (i.e. logging) any error that occures in the process.
	 * 
	 * @param message The message to get the body from
	 * @return A String, or null if something went wrong in getting it.
	 */
	private byte[] getBody(SipServletMessageImpl message) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "getBody", message);
		}

		byte[] body = null;
		try {
			body = message.getRawContent();
		} catch (IOException e) {
    		if (c_logger.isErrorEnabled()) {
    			c_logger.error("error.exception.ioeinbody",
    					Situation.SITUATION_UNKNOWN, null, e);
    		}
		}

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "getBody", body);
		}
		return body;
	}

	/**
	 * Create the actual auth header string, using the given additional 
	 * parameters from the auth challange. 
	 * @param sipMethod The SIP method to use (needed because it is being hashed
	 * as part of the digest.
	 * @param nonce The nonce sent as part of the authentication challange.
	 * @param qop The QOP sent as part of the authentication challange, or null
	 * if not present (old protocol).
	 * @param nc The nonce count
	 * @param cnonce The client-side nonce
	 * @param uri The request URI
	 * @param algorithm The algorithm requested in the challange 
	 * (either 'MD5' or 'MD5-SESS').
	 * @param opaque An opaque value from the challange response, to be embeded 
	 * in the header. null for none.
	 * @param body The message body. Only needed if qop="auth-int"
	 * @return An auth header string. 
	 */
	private String createHeaderString(String sipMethod, String nonce, 
			String qop, String nc, String cnonce, String uri, 
			String algorithm, String opaque, byte[] body)
	{
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "createHeaderString", 
					new Object[] {sipMethod, nonce, qop, nc, cnonce, 
					uri, algorithm, opaque, body});
		}

		StringBuffer header = new StringBuffer(DigestConstants.DIGEST);
		header.append(' ');
		String params = 
			getAuthParamString(sipMethod, nonce, qop, nc, cnonce, 
					uri, algorithm, opaque, body);
		header.append(params);
		
		String headerString = header.toString();
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "createHeaderString", headerString);
		}

		return headerString;
	}
	
	/**
	 * Create the parameters part of the auth header, based on the 
	 * authentication data and the given additional parameters. 
	 * @param sipMethod The SIP method to use (needed because it is being hashed
	 * as part of the digest.
	 * @param nonce The nonce sent as part of the authentication challange.
	 * @param qop The QOP sent as part of the authentication challange, or null
	 * if not present (old protocol).
	 * @param nc The nonce count
	 * @param cnonce The client-side nonce
	 * @param uri The request URI
	 * @param algorithm The algorithm requested in the challange 
	 * (either 'MD5' or 'MD5-SESS').
	 * @param opaque An opaque value from the challange response, to be embeded 
	 * in the header. null for none.
	 * @param body The message body. Only needed if qop="auth-int"
	 * @return the calculated param string
	 */
	private String getAuthParamString(String sipMethod, String nonce, 
			String qop, String nc, String cnonce, String uri, 
			String algorithm, String opaque, byte[] body)
	{
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "getAuthParamString", 
					new Object[] {sipMethod, nonce, qop, nc, cnonce, 
					uri, algorithm, opaque, body});
		}
		String digest = 
			getDigestAsString(sipMethod, nonce, qop, nc, cnonce, uri, 
					algorithm, body);

		StringBuffer header = new StringBuffer();
		addParam(header, DigestConstants.PROPERTY_REALM, _realm, true);
		addParam(header, DigestConstants.PROPERTY_USER_NAME, _username, true);
		addParam(header, DigestConstants.PROPERTY_URI, uri, true);
		addParam(header, DigestConstants.PROPERTY_NONCE, nonce, true);
		algorithm = (algorithm == null ? DigestConstants.ALG_MD5 : algorithm);
		addParam(header, DigestConstants.PROPERTY_ALGORITHM, algorithm, true);
		if (qop != null) {
			addParam(header, DigestConstants.PROPERTY_QOP, qop, true);
			addParam(header, DigestConstants.PROPERTY_CNONCE, cnonce, true);
			addParam(header, DigestConstants.PROPERTY_NC, nc, false);
		}
		if (opaque != null) {
			addParam(header, DigestConstants.PROPERTY_OPAQUE, opaque, true);
		}
		addParam(header, DigestConstants.PROPERTY_RESPONSE, digest, true);

		String headerString = header.toString();
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "getAuthParamString", headerString);
		}

		return headerString;
	}

	/**
	 * Calculate the authentication digest, based on the authentication data
	 * and the given additional parameters. 
	 * @param sipMethod The SIP method to use (needed because it is being hashed
	 * as part of the digest.
	 * @param nonce The nonce sent as part of the authentication challange.
	 * @param qop The QOP sent as part of the authentication challange.
	 * @param nc The nonce count
	 * @param cnonce The client-side nonce
	 * @param uri The request URI
	 * @param algorithm The algorithm requested in the challange 
	 * (either 'MD5' or 'MD5-SESS').
	 * @param body The message body. Only needed if qop="auth-int"
	 * @return the calculated digest
	 */
	private String getDigestAsString(String sipMethod, String nonce, 
			String qop, String nc, String cnonce, String uri, 
			String algorithm, byte[] body) 
	{
		String digest = 
			DigestUtils.createDigestFromAuthParams(_username, _realm, 
					_password, nonce, qop, nc, cnonce, uri, 
					algorithm, sipMethod, body);
		return digest;
		
	}
	
	/**
	 * Helper method: append a security parameter to the given string buffer
	 */
	private void addParam(StringBuffer buffer, String paramName, 
			String value, boolean quoted)
	{
		// All but the first parameter get a comma
		if (buffer.length() > 0)
			buffer.append(',');
		buffer.append(paramName);
		buffer.append('=');
		if (quoted)
		{
			buffer.append('\"');
		}
		buffer.append(value);
		if (quoted)
		{
			buffer.append('\"');
		}
	}
}
