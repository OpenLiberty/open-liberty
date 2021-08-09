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

import javax.servlet.sip.SipApplicationSession;

public interface SASAttrRepository extends TransactionSupport {

	/**
	 * Add a new Application session attribute to the repository. 
	 * @param appSession - application session 
	 * @param name - the name (key) of the attribute we want to save.
	 * @param value - the value of the attribute we want to save
	 * @return previous value (if exists) , maybe null if new key inserted.
	 */
	public Object put(SipApplicationSession appSession , String name , Object value) ;

	/**
	 * retrieve a a SIP Application session attribute from the repository. 
	 * @param appSession - application session 
	 * @param name - the name (key) of the attribute we want to save.
	 * @return the value of the attribute , maybe null if not found.
	 */
	public Object get(SipApplicationSession appSession , String name);
	
	/**
	 * removes a a SIP Application session attribute from the repository. 
	 * @param appSession - application session id 
	 * @param name - the name (key) of the attribute we want to save.
	 * @return the value of the attribute , maybe null if not found.
	 */
	public Object remove(SipApplicationSession appSession , String name);
	
	/**
	 * Get all attributes for sip application session
	 * 
	 * @param Sip Application session
	 * @return
	 */
	public Map getAttributes(String appSessionId);
}
