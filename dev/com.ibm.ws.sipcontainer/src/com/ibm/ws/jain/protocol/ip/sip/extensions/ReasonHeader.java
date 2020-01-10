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
 * 
 * The Reason header as defined in RFC 3326:
 * 
 * Reason = "Reason" HCOLON 
 * reason-value COMMA reason-value) 
 * reason-value = protocol *(SEMI reason-params) 
 * protocol = "SIP" / "Q.850" / token
 * reason-params = protocol-cause / reason-text / reason-extension
 * protocol-cause = "cause" EQUAL cause 
 * cause = 1*DIGIT reason-text = "text"
 * reason-text       =  "text" EQUAL quoted-string
 * reason-extension  =  generic-param
 * 
 * @author anat
 * 
 */
public interface ReasonHeader extends ParametersHeader{

	/**
     * header name
     */
    public final static String name = "Reason";
    
	/**
	 * Sets the protocol of ReasonHeaser
	 *
	 */
	public void setProtocol(String protocol);
	
	/**
	 * Returns Protocol value;
	 * @return
	 */
	public String getProtocol();
	
	/**
	 * Sets the cause of ReasonHeaser
	 *
	 */
	public void setCause(String cause) throws SipParseException;
	
	/**
	 * Returns Cause value;
	 * @return
	 */
	public String getCause();
	
	/**
	 * Sets the cause of ReasonHeaser
	 *
	 */
	public void setText(String text) throws SipParseException;
	
	/**
	 * Returns Cause value;
	 * @return
	 */
	public String getText();
}
