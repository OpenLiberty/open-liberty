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

/**
 * @author dror
 */
public interface AuthorizationConstants {
	public static final String PROPERTY_NAME_DISABLE_SIP_BASIC_AUTH="com.ibm.ws.sip.tai.DisableSIPBasicAuth";
	public static final String AUTHORIZATION_HEADER="Authorization";
	public static final String AUTH_INFO_HEADER="Authentication-Info";
	public static final String WWW_AUTH_HEADER="WWW-Authenticate";
	public static final String BASIC_HEADER="Basic";
	public static final String SIP_RESPONSE="SIP RESPONSE";
	public static final String SIP_REQUEST="SIP REQUEST";
	public static final String PROXY_AUTHORIZATION_HEADER="Proxy-Authorization";
	public static final String PROXY_AUTH_HEADER="Proxy-Authenticate";
	
	/**
	 * Key for the field for a response. Used for authentication of responses.
	 */
	public static final String SIP_RESPONSE_TO_HEADER = "SIP_RESPONSE_TO_HEADER";
 	
}
