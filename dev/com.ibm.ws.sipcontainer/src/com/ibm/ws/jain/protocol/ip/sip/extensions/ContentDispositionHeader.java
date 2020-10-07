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
package com.ibm.ws.jain.protocol.ip.sip.extensions;

import jain.protocol.ip.sip.header.ParametersHeader;

/**
 * The Content-Disposition header field describes how the message body or, for
 * multipart messages, a message body part is to be interpreted by the UAC or 
 * UAS. This SIP header field extends the MIME Content- Type (RFC 2183 [18]). 
 * Several new "disposition-types" of the Content-Disposition header are 
 * defined by SIP. The value "session" indicates that the body part describes 
 * a session, for either calls or early (pre-call) media. The value "render" 
 * indicates that the body part should be displayed or otherwise rendered to 
 * the user. Note that the value "render" is used rather than "inline" to 
 * avoid the connotation that the MIME body is displayed as a part of the 
 * rendering of the entire message (since the MIME bodies of SIP messages 
 * oftentimes are not displayed to users). For backward-compatibility, if 
 * the Content-Disposition header field is missing, the server SHOULD assume 
 * bodies of Content-Type application/sdp are the disposition "session", 
 * while other content types are "render". 
 * The disposition type "icon" indicates that the body part contains an image 
 * suitable as an iconic representation of the caller or callee that could be 
 * rendered informationally by a user agent when a message has been received, 
 * or persistently while a dialog takes place. The value "alert" indicates that
 *  the body part contains information, such as an audio clip, that should be 
 * rendered by the user agent in an attempt to alert the user to the receipt of
 *  a request, generally a request that initiates a dialog; this alerting body
 *  could for example be rendered as a ring tone for a phone call after a 180 
 * Ringing provisional response has been sent. 
 *
 * Any MIME body with a "disposition-type" that renders content to the user should only 
 * be processed when a message has been properly authenticated. 
 * 
 * The handling parameter, handling-param, describes how the UAS should react 
 * if it receives a message body whose content type or disposition type it does 
 * not understand. The parameter has defined values of "optional" and 
 * "required". If the handling parameter is missing, the value "required" 
 * SHOULD be assumed. The handling parameter is described in RFC 3204 [19]. 
 *
 * If this header field is missing, the MIME type determines the default 
 * content disposition. If there is none, "render" is assumed. 
 *
 * Example: 
 *
 *    Content-Disposition: session
 *
 * @author Assaf Azaria
 */
public interface ContentDispositionHeader extends ParametersHeader
{
	//
	// Constants.
	//
	
	/**
	 * Name of Content disposition Header.
	 */
	public final static String name = "Content-Disposition";
	
	/**
	 * The render type.
	 */
	public static String RENDER = "render";
	
	/**
     * The session type.
	 */
	public static String SESSION = "session";
	
	/**
	 * The icon type.
	 */
	public static String ICON = "icon";
	
	/**
	 * The alert type.
	 */
	public static String ALERT = "alert";
	
	/**
	 * The handling type.
	 */
	public static String HANDLING = "handling";
	
	/**
	 * The required 'handling' value.
	 */ 
	public static String REQUIRED = "required";
	
	/**
	 * The optional 'handling value.
	 */
	public static String OPTIONAL = "optional";
	
	//
	// Operations.
	//
	
	/**
	 * Set the disposition type.
	 * 
	 * @param type the disposition type.
	 * @throws IllegalArgumentException if type is null or invalid.
	 */
	public void setDispositionType(String type) throws IllegalArgumentException;

	/**
	 * Get the disposition type.
	 */
	public String getDispositionType();
}
