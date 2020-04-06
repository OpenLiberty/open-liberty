/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.timer;

/**
 * 
 * @author anat, Dec 1, 2005
 *
 * Interface that will be implemented by Object that wants to receive 
 * Timer expiration notification
 * 
 */
abstract public class ExpirationTimerListener {
	
	/**
	 * Timer invoked
	 */
	abstract protected void invokeExpiredTimer();

	/**
	 * Returns ID of the Invoker
	 * @return
	 */
	abstract public String getId();
	
	/**
	 * Returns ID of the Application Session that belongs to this invoker
	 * @return
	 */
	abstract public String getApplicationId();
	
	abstract public Integer getApplicationIdForPMI();
	
	abstract public String getApplicationName();

	abstract public Object getServiceSynchronizer();
	/**
	 * Cancel the timer
	 */
	abstract public void cancel();

	/**
	 * Schedule the timer
	 * @param isPersistent
	 * @param delay
	 */
	abstract public void schedule(boolean isPersistent, long delay); 

}
