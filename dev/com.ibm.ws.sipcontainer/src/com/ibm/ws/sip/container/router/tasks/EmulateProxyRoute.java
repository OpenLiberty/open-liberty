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
package com.ibm.ws.sip.container.router.tasks;

import javax.servlet.sip.Proxy;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.TooManyHopsException;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;

public class EmulateProxyRoute extends RoutedTask{
	
	private SipServletRequestImpl request = null;
    private static final LogMgr c_logger = Log.get(EmulateProxyRoute.class);

    /**
     * Singletone instance
     * @param request
     * @return
     */
	public static EmulateProxyRoute getInstance( SipServletRequestImpl request) {
		return new EmulateProxyRoute(request);
	}

	/**
	 * Ctor
	 * @param request
	 */
	public EmulateProxyRoute(SipServletRequestImpl request){
		this.request = request;
		setForDispatching(false);
	}
	
	/**
	 * @see com.ibm.ws.sip.container.router.tasks.RoutedTask#doTask()
	 */
	protected void doTask() {
		try {
			if (c_logger.isTraceDebugEnabled()) {
	            c_logger.traceDebug(this, "doTask","Sending to external route, request:"+ request);
	        }
			Proxy proxy = request.getProxy();
			proxy.setSupervised(false);
			proxy.proxyTo(request.getRequestURI());
			
		} catch (TooManyHopsException e) {
	        if (c_logger.isErrorEnabled()) {
	            c_logger.error("error.exception", Situation.SITUATION_CREATE,
	                           null, e);
	        }
		} catch (IllegalStateException e) {
	        if (c_logger.isErrorEnabled()) {
	            c_logger.error("error.exception", Situation.SITUATION_CREATE,
	                           null, e);
	        }
		}		
	}

	/**
	 * @see com.ibm.ws.sip.container.router.tasks.RoutedTask#getMethod()
	 */
	public String getMethod() {
		return "External routing";
	}

	/**
	 * @see com.ibm.ws.sip.container.util.Queueable#getApplicationSession()
	 */
	@Override
	public SipApplicationSession getApplicationSession() {
		return request.getApplicationSession(false);
	}
	
	/**
	 * @see com.ibm.ws.sip.container.util.Queueable#getTuWrapper()
	 */
	@Override
	public TransactionUserWrapper getTuWrapper() {
		return request.getTransactionUser();
	}
}
