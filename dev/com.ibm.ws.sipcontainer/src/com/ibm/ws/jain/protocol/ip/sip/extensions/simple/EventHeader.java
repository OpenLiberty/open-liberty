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
package com.ibm.ws.jain.protocol.ip.sip.extensions.simple;

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.header.ParametersHeader;

/**
 * A header for the purposes of matching responses and NOTIFY messages with
 * SUBSCRIBE messages, the event-type portion of the "Event" header is
 * compared byte-by-byte, and the "id" parameter token (if present) is
 * compared byte-by-byte.  An "Event" header containing an "id"
 * parameter never matches an "Event" header without an "id" parameter.
 * No other parameters are considered when performing a comparison.
 *
 * Note that the forgoing text means that "Event: foo; id=1234" would
 * match "Event: foo; param=abcd; id=1234", but not "Event: foo" (id
 * does not match) or "Event: Foo; id=1234" (event portion does not
 * match).
 *
 * @author Assaf Azaria, May 2003.
 */
public interface EventHeader extends ParametersHeader
{
	/**
	 * Name of Event Header.
	 */
	public final static String name = "Event";
	
	/**
	 * The event id constant.
	 */
	public final static String ID = "id";
	
	//
	// Operations.
	//
	/**
	 * Set the event type.
	 * 
	 * @param type the event type.
	 * @throws IllegalArgumentException in case the type is null.
     */
	public void setEventType(String type) throws IllegalArgumentException;
		
	
	/**
	 * Set the event type.
	 * 
	 * @param id the event id.
	 * @throws IllegalArgumentException in case the id is null. 
	 * @throws SipParseException if type is not accepted by implementation
	 */
	public void setEventId(String id) throws IllegalArgumentException,
		SipParseException;
	
	/**
	 * Get the event type.
	 */
	public String getEventType();
	
	/**
	 * Get the event id, if it exists.
	 */
	public String getEventId();
	
	/**
	 * Check whether this header contains an Id parameter.
	 */
	public boolean hasId();
	
}
