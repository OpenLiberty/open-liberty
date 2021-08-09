/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.monitor.pmi.application;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.sip.container.pmi.ApplicationModuleInterface;
import com.ibm.ws.sip.container.pmi.ApplicationTaskDurationModuleInterface;
import com.ibm.ws.sip.container.pmi.RequestModuleInterface;
import com.ibm.ws.sip.container.pmi.ResponseModuleInterface;
import com.ibm.ws.sip.container.pmi.SessionInterface;
import com.ibm.ws.sip.container.pmi.SessionModule;
import com.ibm.ws.sip.container.pmi.basic.SessionsCounter;
import com.ibm.ws.sip.container.pmi.taskduration.ApplicationTaskDurationModule;

public class ApplicationModule implements ApplicationModuleInterface,
									RequestModuleInterface, 
									ResponseModuleInterface {
	/**
	 * Class Logger. 
	 */
	private static final Logger s_logger = Logger.getLogger(ApplicationModule.class.getName());
	
	/**
	* Handle SessionsCounter for this Application Module
	*/
	private SessionsCounter _appCounter = new SessionsCounter();
	
	/** Session module */
	private SessionModule _sessionModule;
	
	/** Application Task DurationModule */
	private ApplicationTaskDurationModule _applicationTaskDurationModule;

	/**
	* CTOR
	* 
	* @param appFullName 
	*            name of the application that module belongs to
	
	*/
	public ApplicationModule(String appFullName) {
		
		if (s_logger != null && s_logger.isLoggable(Level.FINEST)) {
			s_logger.logp(Level.FINEST, ApplicationModule.class.getName(), "ApplicationModule", " new");
		}
		
		_sessionModule = new SessionModule(appFullName, _appCounter);
		_applicationTaskDurationModule = new ApplicationTaskDurationModule(appFullName);  	
	}
	
	/**
	 * @see com.ibm.ws.sip.container.pmi.RequestModuleInterface#incrementInRequest(java.lang.String)
	 */
	public void incrementInRequest(String method) {
		_appCounter.inboundRequest(method);
	}
	
	public void incrementOutRequest(String method) {
		_appCounter.outboundRequest(method);
	}
	
	public void incrementInResponse(int code) {
		_appCounter.inboundResponse(code);
	}
	
	public void incrementOutResponse(int code) {
		_appCounter.outboundResponse(code);
	}
	
	public void updateCounters() {
		if (s_logger != null && s_logger.isLoggable(Level.FINEST)) {
            s_logger.logp(Level.FINEST, ApplicationModule.class.getName(),"updateCounters", "");
        }
		_sessionModule.updateCounters();
		_appCounter.updateCounters();
		_applicationTaskDurationModule.updatePMICounters();
	}
	
	public void destroy() {
		_sessionModule.destroy();
		_applicationTaskDurationModule.destroy();
	}
	
	/**
	* @return Returns the session module.
	*/
	public SessionInterface getSessionModule() {
		return _sessionModule;
	}
	/**
	* @return Returns the response module.
	*/
	public ResponseModuleInterface getResponseModule() {
		return this;
	}
	/**
	* @return Returns the session module.
	*/
	public RequestModuleInterface getRequestModule() {
		return this;
	}
	/**
	* @return Returns the application task duration module.
	*/
	public ApplicationTaskDurationModuleInterface getApplicationTaskDurationModule() {
		return _applicationTaskDurationModule;
	}
	
	/**
	 * @return Returns the application sessions counter
	 */
	public SessionsCounter getAppSessionsCounter() {
		return _appCounter;
	}
}
