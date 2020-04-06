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

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.header.ParametersHeader;

/**
 * The Join header as defined in RFC 3911:
 * 
 * Join = "Join" HCOLON callid *(SEMI join-param)
 * join-param = to-tag / from-tag / generic-param
 * to-tag = "to-tag" EQUAL token
 * from-tag = "from-tag" EQUAL token
 * 
 * @author ran
 */
public interface JoinHeader extends ParametersHeader
{
	/**
	 * sets the Call-ID part of the Join header
	 * @param callId the Call-ID part of the Join header
	 */
	public void setCallId(String callId);
	
	/**
	 * gets the Call-ID part of the Join header
	 * @return the Call-ID part of the Join header, or null if not set
	 */
	public String getCallId();
	
	/**
	 * sets the To tag of the Join header
	 * @param toTag the To tag of the Join header
     * @throws SipParseException if toTag syntax is invalid
	 */
	public void setToTag(String toTag) throws SipParseException;
	
	/**
	 * gets the To tag of the Join header
	 * @return the To tag of the Join header, or null if not set
	 */
	public String getToTag();
	
	/**
	 * sets the From tag of the Join header
	 * @param fromTag the From tag of the Join header
     * @throws SipParseException if fromTag syntax is invalid
	 */
	public void setFromTag(String fromTag) throws SipParseException;
	
	/**
	 * gets the From tag of the Join header
	 * @return the From tag of the Join header, or null if not set
	 */
	public String getFromTag();
	
    /**
     * header name
     */
    public final static String name = "Join";
}
