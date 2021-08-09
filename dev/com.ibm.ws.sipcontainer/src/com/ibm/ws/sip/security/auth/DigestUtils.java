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

import java.security.MessageDigest;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;

/**
 * Digest-generation utilities for the digest code. This class contains code
 * to generate the various digest values (A1, A2, KD), as described in RFC2617
 * and section 22 of RFC3261.
 *
 * @author dedi
 */
public class DigestUtils {

	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(DigestUtils.class);

    /**
	 * Initial size for the buffers used to for creating digest values. 
	 */
    private static final int BUFFER_INITIAL_SIZE = 100;

    /**
	 * hex digit characters.
	 */
	private static final char[] HEX_CHARS = 
		{ '0', '1', '2', '3', '4', '5', '6', '7', 
		  '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	/**
	 * convert an array of bytes to an hexadecimal string
	 * @return a string
	 * @param b bytes array to convert to a hexadecimal
	 * string
	 */    
	private static String toHexString(byte b[]) {
		if (c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(null, "toHexString");        	
		}

		int pos = 0;
	    char[] c = new char[b.length*2];
	    for (int i=0; i< b.length; i++) {
	        c[pos++] = HEX_CHARS[(b[i] >> 4) & 0x0F];
	        c[pos++] = HEX_CHARS[b[i] & 0x0f];
	    }

		if (c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(null, "toHexString");        	
		}
	    return new String(c);
	}
	
	/**
	 * Caculate the MD5 digest of the given byte array. 
	 * @param msg The message to hash
	 * @param digester The digester object to use.
	 * @return A hashed representation of the message.
	 */
	private static byte[] digest(byte[] msg,MessageDigest digester) {
		if (c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(null, "digest");        	
		}

		byte[] retVa;
		digester.update(msg);
		retVa = digester.digest();
		digester.reset();
		
		if (c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(null, "digest");        	
		}

		return retVa;
	}

	/**
	 * Create a md-5 hash of the given string, and return it as a hex-encoded 
	 * string. 
	 */
	private static String textDigest(String text, MessageDigest digester)
	{
		return textDigest(text.getBytes(), digester);
	}
	
	/**
	 * Create a md-5 hash of the given string, and return it as a hex-encoded 
	 * string. 
	 */
	private static String textDigest(byte[] textBytes, MessageDigest digester)
	{
		if (c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(null, "textDigest");        	
		}
		byte[] hash = digest(textBytes,digester);
		String hexHash = toHexString(hash); 

		if (c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(null, "textDigest", hexHash);        	
		}
		return hexHash;
	}	

	/**
	 * Calculate the A1 (username-realm-password) value for the digest.
	 * This is the 'plain md5' version, as opposed to the 'md5-sess' value,
	 * calculated by the method createA1MD5Sess.
	 * 
	 * @param user The username
	 * @param realm The authentication realm.
	 * @param passwd The user password.
	 * @return A1 value as a string.
	 * @see #createA1MD5Sess(String, String, String, String, String, 
	 * MessageDigest)
	 */
	public static String createHashedA1(String user, String realm, 
			String passwd) {
        
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(null, "createHA1", 
					new Object[] {user, realm, "*****"});        	
		}
		StringBuffer buff = new StringBuffer(BUFFER_INITIAL_SIZE);
		buff.append(user);
		buff.append(":");
		buff.append(realm);
		buff.append(":");
		buff.append(passwd);

		String a1 = buff.toString();

		MessageDigest digester = ThreadLocalStorage.getMessageDigest();
		String ha1 = textDigest(a1, digester);

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(null, "createHA1", ha1);        	
		}
		return ha1;
	}
	
	/**
	 * Calculate the A1 (username-realm-password) value for the digest.
	 * This is the 'MD5-sess' version, which hashes the value username and
	 * password along with the server and client nonces
	 *
	 * @param ha1 The non-md5-sess value for a1 (hashed once)
	 * @param nonce The server-side nonce.
	 * @param cnonce The client-side nonce.
	 * @param digester The digester to use for hashing.
	 * 
	 * @return A1 value as a string.
	 * @see #createA1(String, String, String)
	 */
	private static String createA1MD5Sess(String ha1, String nonce, 
			String cnonce, MessageDigest digester)
	{
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(null, "createA1MD5Sess", 
					new Object[] {ha1, nonce, cnonce, digester});        	
		}
		StringBuffer buff = new StringBuffer(BUFFER_INITIAL_SIZE);
		buff.append(ha1);
		buff.append(":");
		buff.append(nonce);
		buff.append(":");
		buff.append(cnonce);
		String hexHash = buff.toString(); 

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(null, "createA1MD5Sess", hexHash);
		}
		return  hexHash;
	}
	
	/**
	 * Create an A2 value for use with the auth-int QOP value. I.e. one that
	 * also hashes the actual method body.
	 * @param sipMethod The sip method of the request.
	 * @param uri The request URI
	 * @param hashedBody Hash code for the request body.
	 * @return An A2 value.
	 */
	private static String createA2WithBody(String sipMethod, String uri, 
			String hashedBody) 
	{
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(null, "createA2WithBody", 
					new Object[] {sipMethod, uri, hashedBody});        	
		}

		StringBuffer buff=new StringBuffer(BUFFER_INITIAL_SIZE);
		if (hashedBody == null)
			hashedBody = "";

		buff.append(sipMethod.toUpperCase());
		buff.append(":");
		buff.append(uri);
		buff.append(":");
		buff.append(hashedBody);

		String A2 = buff.toString(); 
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(null, "createA2WithBody", A2);        	
		}
		return A2;
	}

	/**
	 * Calculate the A2 (requestMethod-digestURI) value for the digest.
	 * 
	 * @param method The request method.
	 * @param digestUri The digest URI
	 * @return The A2 value for the digest.
	 */
	private static String createA2(String method, String digestUri) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(null, "createA2", 
					new Object[] {method, digestUri});        	
		}

		StringBuffer buff = new StringBuffer(BUFFER_INITIAL_SIZE);
		
		buff.append(method.toUpperCase());
		buff.append(":");
		buff.append(digestUri);
		String A2 = buff.toString(); 
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(null, "createA2", A2);        	
		}
		return A2;
	}
	
	/**
	 * Create the KD value (i.e. the digest). This is the 'new-style' 
	 * KD value calculation, which should be used when the challange
	 * contains a 'qop' parameter.
	 * 
	 * @param HA1 The hashed 'A1' value.
	 * @param nonce The server-side nonce.
	 * @param cnonce The client-side nonce.
	 * @param HA2 The hashed 'A2' value.
	 * @param digester The digester to use for hashing.
	 * @return The KD value.
	 * @see #createKD(String, String, String, MessageDigest)
	 */
	private static String createKD(String HA1, String nonce, String nc,
			String cnonce, String qop, String HA2, MessageDigest digester)
	{
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(null, "createKD", 
					new Object[] {HA1, nonce, nc, cnonce, qop, HA2, digester});        	
		}

		StringBuffer kdBuff = new StringBuffer(BUFFER_INITIAL_SIZE);
		kdBuff.append(HA1);
		kdBuff.append(":");
		kdBuff.append(nonce);
		
        if (qop != null && 
        	(qop.equals(DigestConstants.QOP_AUTH) || 
        	 qop.equals(DigestConstants.QOP_AUTH_INT)))
        {
		    kdBuff.append(":");
            kdBuff.append(nc);
        
            kdBuff.append(":");
            kdBuff.append(cnonce);
        
            kdBuff.append(":");
            kdBuff.append(qop);
        }
		kdBuff.append(":");
		kdBuff.append(HA2);
		String response = textDigest(kdBuff.toString(), digester);
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(null, "createKD", response);
		}
		return response;
	}

	
	
	/**
	 * Calculate the digest based on the given authentication params.
	 * @param username The username to authenticate with
	 * @param realm The challange realm
	 * @param password The password to authenticate with
	 * @param nonce The challange nonce
	 * @param qop The QOP to use. Could be either 'auth', 'auth-int', or null
	 * @param nc The nonce-count from the client request
	 * @param cnonce The client-side nonce
	 * @param uri The request URI
	 * @param algorithm The algorithm to use. Could be either 'MD5' or 
	 * 'MD5-sess'
	 * @param sipMethod The sip method of the client request.
	 * @param body The message body to authenticate. Only needed if 
	 * qop="auth-int"
	 * @return A digest.
	 */
	public static String createDigestFromAuthParams(String username,
			String realm, String password, String nonce, String qop, String nc,
			String cnonce, String uri, String algorithm, String sipMethod, 
			byte[] body) 
	{
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(null, "createDigestFromAuthParams", 
					new Object[] {username, realm, "*****", nonce, qop, nc,
					cnonce, uri, algorithm, sipMethod, body});        	
		}

		MessageDigest digester = ThreadLocalStorage.getMessageDigest();
        
        String HA1 = createHashedA1(username, realm, password);

		if (c_logger.isTraceDebugEnabled()){
			  c_logger.traceDebug(null, 
					  "createDigestFromAuthParams", 
					  "HA1[" + HA1 + "]");        	
		}	
		
		String digest = createDigestFromAuthParams(HA1, nonce, qop, nc, cnonce, 
				uri, algorithm, sipMethod, body);

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(null, "createDigestFromAuthParams", digest);        	
		}
		
		return digest;
	}

	/**
	 * Calculate the digest based on the given authentication params. This
	 * version of the method takes a pre-calculated hashed value of 
	 * username-ream-password (which the RFC called H(A1)), instead of plaintext
	 * values. 
	 * @param nonce The challange nonce
	 * @param qop The QOP to use. Could be either 'auth', 'auth-int', or null
	 * @param nc The nonce-count from the client request
	 * @param cnonce The client-side nonce
	 * @param uri The request URI
	 * @param algorithm The algorithm to use. Could be either 'MD5' or 
	 * 'MD5-sess'
	 * @param sipMethod The sip method of the client request.
	 * @param body The message body to authenticate. Only needed if 
	 * qop="auth-int"
	 * @return A digest.
	 */
	public static String createDigestFromAuthParams(String ha1, String nonce,
			String qop, String nc, String cnonce, String uri, String algorithm,
			String sipMethod, byte[] body) 
	{
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(null, "createDigestFromAuthParams", 
					new Object[] {ha1, nonce, qop, nc,
					cnonce, uri, algorithm, sipMethod, body});        	
		}
        MessageDigest digester = ThreadLocalStorage.getMessageDigest();
        if (algorithm != null && algorithm.equals(DigestConstants.ALG_MD5_SESS)) {
        	String A1 = createA1MD5Sess(ha1, nonce, cnonce, digester);
			ha1 = textDigest(A1, digester);
		}

		String A2 = null;
		if (sipMethod == null)
			sipMethod = DigestConstants.METHOD_DEFAULT;
		boolean isAuthInt = 
			(qop != null && qop.equals(DigestConstants.QOP_AUTH_INT)); 
		if (isAuthInt) {
			String hashedBody = textDigest(body, digester);
			A2 = createA2WithBody(sipMethod, uri, hashedBody);
		} else {
			A2 = createA2(sipMethod, uri);
		}
		String HA2 = textDigest(A2, digester);
		if (c_logger.isTraceDebugEnabled()){
			  c_logger.traceDebug(null, 
					  "createDigestFromAuthParams",
					  "A2[" + A2 + "],HA2[" + HA2 + "]");        	
		}
		
		String KD = 
			createKD(ha1, nonce, nc, cnonce, qop, HA2, digester);

		if (c_logger.isTraceDebugEnabled()){
			  c_logger.traceDebug(null, 
					  "createDigestFromAuthParams",
					  "KD[" + KD + "]");        	
		}
		return KD;
	}
}
