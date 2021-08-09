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
package com.ibm.ws.sip.container.failover.repository;

import java.util.Map;

import javax.servlet.sip.SipSession;

public interface SSAttrRepository extends TransactionSupport 
{
	/**
	 * Add a new SIP session attribute to the repository. 
	 * @param session - SIP session 
	 * @param name - the name (key) of the attribute we want to save.
	 * @param value - the value of the attribute we want to save
	 * @return previous value (if exists) , maybe null if new key inserted.
	 */
	public abstract Object put(SipSession session , String name , Object value) ;
	
	/**
	 * retrieve a a SIP session attribute from the repository. 
	 * @param session - SIP session 
	 * @param name - the name (key) of the attribute we want to save.
	 * @return the value of the attribute , maybe null if not found.
	 */
	public abstract Object get(SipSession session , String name);
	
	/**
	 * removes a a SIP session attribute from the repository. 
	 * @param session - SIP session object from which we remove an attribute 
	 * @param name - the name (key) of the attribute we want to save.
	 * @return the value of the attribute , maybe null if not found.
	 */
	public abstract Object remove(SipSession session , String name);
	
	/**
	 * @param session - SIP session id
	 * @return all attributes related to the session
	 */
	public abstract Map getAttributes(String session);
}
