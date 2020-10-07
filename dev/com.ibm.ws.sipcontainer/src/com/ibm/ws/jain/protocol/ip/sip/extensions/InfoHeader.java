/*******************************************************************************
 * Copyright (c) 2003,2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jain.protocol.ip.sip.extensions;

import jain.protocol.ip.sip.address.URI;
import jain.protocol.ip.sip.header.ParametersHeader;

/**
 * The base interface for all headers containing information in the 
 * format:
 * XXX-Info = "XXX-Info" HCOLON XXX-uri *(COMMA XXX-uri) 
 * XXX-uri = LAQUOT absoluteURI RAQUOT *( SEMI generic-param ) 
 *
 * @see ErrorInfoHeader
 * @see AlertInfoHeader
 * @see CallInfoHeader
 * @author Assaf Azaria, May 2003.
 */
public interface InfoHeader extends ParametersHeader
{
	/**
	* Gets the uri of this header.
	* @return URI
	*/
	public URI getURI();

	/**
	* Sets uri of this header.
	* @param <var>uri</var> uri
	* @throws IllegalArgumentException if uri is null or not from same
	* JAIN SIP implementation
	*/
	public void setURI(URI addr) throws IllegalArgumentException;
}
