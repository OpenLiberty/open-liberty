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

import javax.servlet.sip.SipApplicationSession;

import com.ibm.ws.sip.container.timer.BaseTimer;

/**
 * This class defines a storage for SIP timers
 * @author mordechai
 *
 */
public interface TimerRepository extends TransactionSupport 
{
	/**
	 * Add a timer instance to the repository of BaseTimer.
	 * @param sipAppSession - the owner of the timer.
	 * @param timer
	 * @return  previous value (if exists)
	 */
	public BaseTimer put(SipApplicationSession sipAppSession , BaseTimer timer);
	
	/**
	 * search for a timer
	 * @param appSessionId
	 * @param timerId
	 * @return  A timer instance which matches the Application Session Id and the Timer ID.
	 * may return NULL if nothing was found.
	 */
	public BaseTimer get(String appSessionId , Integer timerId);
	
	/**
	 * delete a timer from the repository
	 * @param appSessionId
	 * @param timerId
	 * @return the delete object (or NULL if such timerId doesn't exsist)
	 */
	public BaseTimer remove(String appSessionId , Integer timerId);

}
