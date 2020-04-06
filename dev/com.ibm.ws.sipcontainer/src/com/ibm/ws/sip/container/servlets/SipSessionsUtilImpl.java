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
package com.ibm.ws.sip.container.servlets;

import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipSessionsUtil;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.SipContainer;
import com.ibm.ws.sip.container.failover.repository.SessionRepository;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;

public class SipSessionsUtilImpl implements SipSessionsUtil {
    /**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log.get(SipSessionsUtilImpl.class);
    
    /**
     * Application name which this SipSessionUtil is working on
     */
    private String m_appName = null;
    
    /**
     * Exmpty constructor for Injection usage
     */
    public SipSessionsUtilImpl() { }
    
    /**
     * @param appName
     */
    public SipSessionsUtilImpl(String appName) {
    	m_appName = appName;

    	SipAppDesc m_sipApp = null;
    	if (appName != null) {
    		m_sipApp = SipContainer.getInstance().getSipApp(appName);
    	} else {
    		m_appName = SipServletsFactoryImpl.UNKNOWN_APPLICATION;
    	}

    	if (m_sipApp == null) {
    		if (c_logger.isErrorEnabled()){
    			c_logger.error("Error retirieving application descriptor for application name: " + appName);
    		}
    	}
    }
	
	/**
	 * @see javax.servlet.sip.SipSessionsUtil#getApplicationSessionById(String)
	 */	
	public SipApplicationSession getApplicationSessionById(String applicationSessionId) {
		 return SipApplicationSessionImpl.getAppSession(applicationSessionId);
	}

	/**
	 * @see javax.servlet.sip.SipSessionsUtil#getApplicationSessionByKey(String, boolean)
	 */
	public SipApplicationSession getApplicationSessionByKey(String applicationSessionKey, boolean create) throws NullPointerException {
		return ((SipServletsFactoryImpl) SipServletsFactoryImpl.getInstance(m_appName)).createApplicationSessionByKey(applicationSessionKey, create);
	}

	/**
	 * @see javax.servlet.sip.SipSessionsUtil#getCorrespondingSipSession(SipSession, String)
	 */	
	public SipSession getCorrespondingSipSession(SipSession session, String headerName) {
		if (session == null){
    		if (c_logger.isTraceDebugEnabled()){
    			c_logger.traceDebug(this, "getCorrespondingSipSession", "session is null");
    		}
    		
    		return null;
		}
				
		TransactionUserWrapper tuWrapper = ((SipSessionImplementation)session).getTransactionUser();
		String correspondingSessionHeader = tuWrapper.getRelatedSipSessionHeader();
		String correspondingSessionId = tuWrapper.getRelatedSipSessionId();
		
		if (c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug(this, "getCorrespondingSipSession", "correspondingSessionId=" + correspondingSessionId 
										+ " correspondingSessionHeader=" +  correspondingSessionHeader);
		}
		
		//check if the requested corresponding header is the one that set the related session
		if (correspondingSessionHeader != null && correspondingSessionHeader.equals(headerName)){
			return SessionRepository.getInstance().getSipSession(correspondingSessionId);
		}
		
		if (c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug(this, "getCorrespondingSipSession", "headerName=" + headerName 
										+ ", The related session was not set using this header");
		}
		
		return null;
	}

	
}
