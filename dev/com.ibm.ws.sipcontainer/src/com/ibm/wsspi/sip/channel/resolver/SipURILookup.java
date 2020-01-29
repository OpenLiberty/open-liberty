/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.sip.channel.resolver;

import java.util.ArrayList;

/**
 * Inteface for RFC 3263 support.
 * <p>
 * In order to lookup a SIPUri via DNS NAPTR/SRV, a user 
 * need merely implement the SipURILookupCallback interface, 
 * contstuct a SipURILookup object with SipURILookupCallback and SIPUri
 * as parameters, and invoke the lookup()
 * method.
 * <p>
 * The result may be returned synchronously or asynchronously, 
 * and will be contained as a member of this object.
 * <p>
 * Once the SIPUri has been resolved via the underlying naming 
 * service, the user invokes getAnswer(), which returns an 
 * ArrayList of SIPUri objects.  These SIPUri objects have there host, 
 * port, and transport members filled in, thereby identifying a contactable
 * node on the internet.
 * 
 *
 */
public interface SipURILookup{
	
	
	/** 
	 * getter for SIPUri being resolved
	 * @return SIPUri
	 */
	public SIPUri getSipURI();
	
	/**
	 * method which invokes the naming service 
	 * @return true if answer is available right away, 
	 * 		   false if answer will be returned asynchronously
	 * @throws SipURILookupException if synchronous exception in resolving SIPUri
	 */
	public boolean lookup() throws SipURILookupException;
			
	/**
	 * getter to return the array list of SIPUri objects which the 
	 * original SIPUri resolved to
	 * 
	 * @return the array list of SIPUri objects
	 */
	public ArrayList<SIPUri> getAnswer();
	
	}
