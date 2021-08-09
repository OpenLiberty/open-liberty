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
package com.ibm.ws.sip.container.servlets;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.sip.ConvergedHttpSession;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.SipContainer;
import com.ibm.ws.sip.container.events.EventsDispatcher;
import com.ibm.ws.sip.container.was.WASHttpSessionListener;

/**
 * This listener is used to update the SAS in the event of a http session invalidation. 
 * 
 * @author galina
 *
 */
public class WASXHttpSessionListener implements WASHttpSessionListener {
	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(WASXHttpSessionListener.class);
	
	@Override
	public void sessionCreated(HttpSessionEvent session) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "sessionCreated", "http session ["+session.getSession().getId()+"] created");
		}
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent session) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "sessionDestroyed", "http session ["+session.getSession().getId()+"] destroyed");
		}
		
		if (session.getSession() instanceof ConvergedHttpSession) {
			// dispatch this event to the sip container thread
			EventsDispatcher.httpSessionDestroyed(SipContainer.getHttpSessionListener(), session.getSession());
		}
		else {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "sessionDestroyed", "Unknown http session ["+session.getSession().getId()+"], ignoring...");
			}
		}
	}

	@Override
	public void handleHttpSessionDestoyEvent(HttpSession session) {
		ConvergedHttpSession convergedHTTPSession = (ConvergedHttpSession)session;
		WASXSipApplicationSessionImpl appSession = (WASXSipApplicationSessionImpl)convergedHTTPSession.getApplicationSession();
		
		if (appSession == null) {
			if (c_logger.isTraceDebugEnabled()) {
    			c_logger.traceDebug(this, "handleHttpSessionDestoyEvent", "Unable to remove http session: " + session.getId());
    		}
		}
		else {
			appSession.removeHttpSession(session.getId());
			
			if (appSession.isReadyToInvalidate()){
				appSession.invalidate();
			}
		}
	}
}
