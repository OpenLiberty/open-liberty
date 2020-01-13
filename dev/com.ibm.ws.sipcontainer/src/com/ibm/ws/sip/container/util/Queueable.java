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
package com.ibm.ws.sip.container.util;

import javax.servlet.sip.SipApplicationSession;

import com.ibm.ws.sip.container.pmi.TaskDurationMeasurer;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;

/**
 * 
 * Objects that will implement this interface will be
 * override the:
 *  getHashId - method that returns the ID that should be used during 
 *  dispatching messages, timers and events to the WAS server.
 *  
 * @author Anat Fradin, Aug 27, 2006
 *
 */
public interface Queueable extends Runnable {
	
	public static int PRIORITY_CRITICAL = 100;
	public static int PRIORITY_NORMAL = 50;
	public static int PRIORITY_LOW = 10;
	/**
	 * Method that returns the hash code of the ID that represents this
	 * object for dispatching in common all other related objects.
	 * Usually it will return ID of the applicationSession which this
	 * object belongs to.
	 * @return
	 */
	public int getQueueIndex();
	
	/**
	 * Returns the priority level of the queueable element
	 * @return
	 */
	public int priority();
	
	/**
	 * @deprecated
	 * Returns this Queueable parent SipApplicationSession 
	 * @return
	 */
	public Object getServiceSynchronizer();
	
	/**
	 * Returns the TaskDurationMeasurer object that is used to measure task duration in the container queue
	 */
	public TaskDurationMeasurer getSipContainerQueueDuration();
	
	/**
	 * Returns the TaskDurationMeasurer object that is used to measure task duration in the application code
	 */
	public TaskDurationMeasurer getApplicationCodeDuration();
	
	/**
	 * set the TaskDurationMeasurer object that is used to measure task duration in the container queue
	 */
	public void setSipContainerQueueDuration(TaskDurationMeasurer tm);
	
	/**
	 * set the TaskDurationMeasurer object that is used to measure task duration in the application code
	 */
	public void setApplicationCodeDuration(TaskDurationMeasurer tm);
	
	/**
	 * Returns queueable app name
	 * @return
	 */
	public String getAppName();
	
	/**
	 * Returns queueable app index
	 */
	public Integer getAppIndexForPMI();
	
	/**
	 * @return the SIP Application Session.
	 */
	public SipApplicationSession getApplicationSession();
	
	/**
	 * @return the TransactionUserWrapper.
	 */
	public TransactionUserWrapper getTuWrapper();
}
