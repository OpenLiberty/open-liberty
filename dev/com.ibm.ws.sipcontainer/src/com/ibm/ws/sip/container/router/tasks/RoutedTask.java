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
package com.ibm.ws.sip.container.router.tasks;

import javax.servlet.sip.SipApplicationSession;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.pmi.TaskDurationMeasurer;
import com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.container.util.Queueable;

/**
 * 
 * @update mordechai 17/Dec/2007 : added logger and fixed defect 487485
 *
 */
public abstract class RoutedTask implements Queueable {

	protected int _index = -1;
	protected TransactionUserWrapper _transactionUser;
	private boolean _forDispatching = true;
	
	
	/**
	 * Object that measures the duration of the task in the container queue
	 */
	private TaskDurationMeasurer _sipContainerQueueDuration= null;
	
	/**
	 * Object that measures the duration of the task in the application code
	 */
	private TaskDurationMeasurer _sipContainerApplicationCodeDuration= null;
	
	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(RoutedTask.class);

	
	public RoutedTask(){
	}
	
	public RoutedTask(TransactionUserWrapper transactionUser){
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "RoutedTask",transactionUser);
		}
		if( transactionUser != null){
			SipApplicationSessionImpl appSession =  transactionUser.getAppSessionForInternalUse();
			if (appSession != null) {
				_index = appSession.extractAppSessionCounter();
			} else {
				String sessId = transactionUser.getApplicationId();
				_index = SipApplicationSessionImpl.extractAppSessionCounter(sessId);
			}
			_transactionUser = transactionUser;
		}
	}
	
	public RoutedTask(String sessionId){
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "RoutedTask",sessionId);
		}
		_index = SipApplicationSessionImpl.extractAppSessionCounter(sessionId);
	}	
	
	/**
	 * @see com.ibm.ws.sip.container.util.Queueable#getQueueIndex()
	 */
	public int getQueueIndex() {
		return _index;
	}

	public int priority() {
		return PRIORITY_NORMAL;
	}

	public void run() {
		if (getQueueIndex() == -1){
			throw new RuntimeException("Dispatching error, transaction-user not found!");
		}
		
		doTask();

	}
	
	public boolean forDispatching(){
		return _forDispatching;
	}
	
	/**
	 * 
	 * @param forDispatching
	 */
	public final void setForDispatching(boolean forDispatching){
		_forDispatching = forDispatching;
	}
	
	public void executeOnCurrentThread(){
		doTask();
	}
		
	abstract protected void doTask();
	
	abstract public String getMethod();
	
	/**
	 * @see com.ibm.ws.sip.container.util.Queueable#getServiceSynchronizer()
	 */
	public Object getServiceSynchronizer(){
		if(_transactionUser == null){
			return null;
		}
		return _transactionUser.getServiceSynchronizer();
	}
	
	/**
	 * @see com.ibm.ws.sip.container.util.Queueable#getSipContainerQueueDuration()
	 */
	public TaskDurationMeasurer getSipContainerQueueDuration() {
		return _sipContainerQueueDuration;
	}
	
	/**
     * @see com.ibm.ws.sip.container.util.Queueable#getApplicationCodeDuration()
     */
	public TaskDurationMeasurer getApplicationCodeDuration() {
		return _sipContainerApplicationCodeDuration;
	}

	/**
     * @see com.ibm.ws.sip.container.util.Queueable#getAppName()
     */
	public String getAppName() {
		if(_transactionUser != null && _transactionUser.getSipServletDesc() != null) {
			return _transactionUser.getSipServletDesc().getSipApp().getAppName();
		}
		else return null;
	}

	/**
     * @see com.ibm.ws.sip.container.util.Queueable#getAppIndexForPMI()
     */
	public Integer getAppIndexForPMI() {
		if(_transactionUser != null && _transactionUser.getSipServletDesc() != null) {
			return _transactionUser.getSipServletDesc().getSipApp().getAppIndexForPmi();
		}
		return null;
	}	
	
	/**
     * @see com.ibm.ws.sip.container.util.Queueable#setSipContainerQueueDuration(TaskDurationMeasurer)
     */
	public void setSipContainerQueueDuration(TaskDurationMeasurer tm) {
		_sipContainerQueueDuration = tm;
		
	}

	/**
     * @see com.ibm.ws.sip.container.util.Queueable#setApplicationCodeDuration(TaskDurationMeasurer)
     */
	public void setApplicationCodeDuration(TaskDurationMeasurer tm) {
		_sipContainerApplicationCodeDuration = tm;
		
	}
	
	/**
	 * @see com.ibm.ws.sip.container.util.Queueable#getApplicationSession()
	 */
	@Override
	public SipApplicationSession getApplicationSession() {
		if (_transactionUser != null) {
			return _transactionUser.getApplicationSession(false);
		}
		return null;
	}
	
	/**
	 * @see com.ibm.ws.sip.container.util.Queueable#getTuWrapper()
	 */
	@Override
	public TransactionUserWrapper getTuWrapper() {
		return _transactionUser;
	}
}
