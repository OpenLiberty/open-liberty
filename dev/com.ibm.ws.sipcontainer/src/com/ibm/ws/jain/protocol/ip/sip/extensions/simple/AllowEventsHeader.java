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

import jain.protocol.ip.sip.header.Header;

/**
 * The "Allow-Events" header, if present, includes a list of tokens
 * which indicates the event packages supported by the client (if sent
 * in a request) or server (if sent in a response).  In other words, a
 * node sending an "Allow-Events" header is advertising that it can
 * process SUBSCRIBE requests and generate NOTIFY requests for all of
 * the event packages listed in that header.
 *
 * This information is very useful, for example, in allowing user agents
 * to render particular interface elements appropriately according to
 * whether the events required to implement the features they represent
 * are supported by the appropriate nodes.
 * 
 * @author Assaf Azaria, May 2003.
 */
public interface AllowEventsHeader extends Header
{
	/**
	 * Name of Allow Events Header.
	 */
	public final static String name = "Allow-Events";
	
	/**
	 * Set the event type.
	 * 
	 * @param type the event type.
	 * @throws IllegalArgumentException in case the type is null.
     */
	public void setEventType(String type) throws IllegalArgumentException;
	
	/**
	 * Get the event type.
	 */
	public String getEventType();	
}
