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
package com.ibm.ws.sip.stack.transaction.transport;

public enum UseCompactHeaders {

	//headers will never be sent in compact form
	NEVER("Never"),
	//headers will be sent in compact form only when MTU Exceeds
	MTU_EXCEEDS("MtuExceeds"),
	//headers will always be sent in compact form
	ALWAYS("Always"),
	//headers are sent according to {@link javax.servlet.sip.SipServletMessage#setHeaderForm(javax.servlet.sip.SipServletMessage.HeaderForm)}
	API("API");

	/**
	 * the field representing the string value
	 */
	private String _strValue = null;
	
	/**
	 * constructor
	 */
	private UseCompactHeaders(String strValue) {
		_strValue = strValue;
	}

	/**
	 * return string representation of enum
	 */
	public String toString(){
		return _strValue;
	}
	
	public static UseCompactHeaders fromString(String strValue){
		if (strValue == null){
			return MTU_EXCEEDS;
		}
		if (strValue.equalsIgnoreCase(NEVER.toString())){
			return NEVER;
		}
		if (strValue.equalsIgnoreCase(MTU_EXCEEDS.toString())){
			return MTU_EXCEEDS;
		}
		if (strValue.equalsIgnoreCase(ALWAYS.toString())){
			return ALWAYS;
		}
		if (strValue.equalsIgnoreCase(API.toString())){
			return API;
		}
		return MTU_EXCEEDS;
	}
}
