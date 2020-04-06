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
 * The Replaces header as defined in RFC 3891:
 * Replaces = "Replaces" HCOLON callid *(SEMI replaces-param)
 * replaces-param = to-tag / from-tag / early-flag / generic-param
 * to-tag = "to-tag" EQUAL token
 * from-tag = "from-tag" EQUAL token
 * early-flag = "early-only"
 * 
 * @author ran
 */
public interface ReplacesHeader extends ParametersHeader
{
	/**
	 * sets the Call-ID part of the Replaces header
	 * @param callId the Call-ID part of the Replaces header
	 */
	public void setCallId(String callId);
	
	/**
	 * gets the Call-ID part of the Replaces header
	 * @return the Call-ID part of the Replaces header, or null if not set
	 */
	public String getCallId();
	
	/**
	 * sets the To tag of the Replaces header
	 * @param toTag the To tag of the Replaces header
     * @throws SipParseException if toTag syntax is invalid
	 */
	public void setToTag(String toTag) throws SipParseException;
	
	/**
	 * gets the To tag of the Replaces header
	 * @return the To tag of the Replaces header, or null if not set
	 */
	public String getToTag();
	
	/**
	 * sets the From tag of the Replaces header
	 * @param fromTag the From tag of the Replaces header
     * @throws SipParseException if fromTag syntax is invalid
	 */
	public void setFromTag(String fromTag) throws SipParseException;
	
	/**
	 * gets the From tag of the Replaces header
	 * @return the From tag of the Replaces header, or null if not set
	 */
	public String getFromTag();
	
	/**
	 * toggles the early flag on or off
	 * @param early the early flag. a value of false turns off the flag
	 *  (removes the parameter). a value of true turns on the flag (sets the
	 *  parameter without a value).
     * @throws SipParseException if the implementation fails to add/remove the flag
	 */
	public void setEarly(boolean early) throws SipParseException;
	
	/**
	 * gets the value of the early flag
	 * @return the value of the early flag. a value of false indicates the flag
	 *  is turned off (parameter is missing). a value of true indicates the
	 *  flag is turned on (parameter is present).
	 */
	public boolean isEarly();
	
    /**
     * header name
     */
    public final static String name = "Replaces";
}
