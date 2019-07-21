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
package com.ibm.wsspi.sip.channel.resolver;

import java.io.IOException;

public class SipURILookupException extends IOException {
	
	private static final long serialVersionUID = 2192808554543917596L;
	/** Error resolving SipURI */
	public static String DEFAULTMSG         			= "Error resolving SipURI.";
	public static String NAMING_ERROR       			= "Nameserver unable to resolve SipURI.";
	public static String SIPURI_NULL        			= "Null SipURI.";
	public static String TARGET_UNDEFINED   			= "Undefined TARGET for SipURI.";
	public static String TARGET_INVALID		   	        = "Invalid TARGET for SipURI. target = ";
	public static String PORT_INVALID       			= "Invalid port for SipURI. port = ";
	public static String TRANSPORT_INVALID  			= "Invalid transport for SipURI. transport = ";
	public static String SCHEME_UNDEFINED   			= "Undefined scheme for SipURI.";
	public static String SCHEME_INVALID     			= "Invalid  scheme for SipURI. scheme = ";
	public static String TRANSPORT_SCHEME_INVALID                   = "Invalid  transport/scheme combo.";
	public static String LOOKUP_IN_PROGRESS                         = "Lookup in progress";
	public static String LOOKUP_TIMEOUT                             = "Lookup timed out for = ";
	
	public SipURILookupException(){
		super(DEFAULTMSG);
	}
	
	public SipURILookupException(String s){
		super(s);
	}

}
