/*******************************************************************************
 * Copyright (c) 2003,2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.security.auth;

public interface DigestConstants {
	public static final short DIGEST_LENGTH=6;
	public static final String DIGEST = "Digest";
	public static final String DIGEST_REALM = "realm=";
	public static final String DIGEST_FIRST_REQUEST = "Digest qop=\"auth\",charset=utf-8,algorithm=md5,nonce=";
	public static final String DIGEST_FIRST_REQUEST_WITH_AUTH_INT = "Digest qop=\"auth-int\",charset=utf-8,algorithm=md5,nonce=";
	public static final String DIGEST_AUTH_INFO_RESPONSE = "qop=\"auth\",nextnonce=";
	
	public static final String PROPERTY_USER_NAME = "username";
	public static final String PROPERTY_REALM = "realm";
	public static final String PROPERTY_NONCE = "nonce";
	public static final String PROPERTY_URI = "uri";
	public static final String PROPERTY_RESPONSE = "response";
	public static final String PROPERTY_QOP = "qop";
	public static final String PROPERTY_NC= "nc";
	public static final String PROPERTY_CNONCE = "cnonce";
	public static final String PROPERTY_ALGORITHM="algorithm";
	public static final String PROPERTY_OPAQUE = "opaque";
	public static final String PROPERTY_STALE = "stale";
	
	public static final String QOP_AUTH = "auth";
	public static final String QOP_AUTH_INT = "auth-int";
	public static final String ALG_MD5 = "MD5";
	public static final String ALG_MD5_SESS = "MD5-sess";
	public static final String METHOD_DEFAULT="AUTHENTICATE";
}
