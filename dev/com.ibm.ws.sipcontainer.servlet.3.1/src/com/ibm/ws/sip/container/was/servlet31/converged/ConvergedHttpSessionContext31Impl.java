/*******************************************************************************
 * Copyright (c) 2014, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.was.servlet31.converged;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.session.SessionApplicationParameters;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.SessionStoreService;
import com.ibm.ws.webcontainer.session.impl.HttpSessionImpl;
import com.ibm.ws.webcontainer31.session.impl.HttpSessionContext31Impl;
import com.ibm.wsspi.session.ISession;
import com.ibm.wsspi.session.IStore;
import com.ibm.wsspi.sip.converge.ConvergedHttpSessionContextImpl;
import com.ibm.wsspi.sip.converge.ConvergedHttpSessionImpl;
import com.ibm.wsspi.sip.converge.IConvergedHttpSessionContext;

public class ConvergedHttpSessionContext31Impl extends HttpSessionContext31Impl implements IConvergedHttpSessionContext {
    
    /**
	 * Class Logger.
	 */
	private static final transient LogMgr c_logger = Log.get(ConvergedHttpSessionContext31Impl.class);

	/**
     * @param smc
     * @param sap
     * @param sessionStoreService
     */
    public ConvergedHttpSessionContext31Impl(SessionManagerConfig smc, SessionApplicationParameters sap, SessionStoreService sessionStoreService) {
        super(smc, sap, sessionStoreService);
    }

    /*
     * createSessionObject
     */
    public Object createSessionObject(ISession isess, ServletContext servCtx){
      return new ConvergedHttpSessionImpl(isess, this, servCtx);
    }
    
    /**
     * 
     * @param session
     * @param contextPath
     * @param relativePath
     * @param scheme
     * @return
     */
    public String getSipBaseUrlForEncoding(String contextPath, String relativePath, String scheme) {
        return ConvergedHttpSessionContextImpl.getSipBaseUrlForEncoding(_smc, contextPath, relativePath, scheme, this);
    }

    /*
     * Added for SIP/HTTP Converged App Support. SIP container calls this method via
     * com.ibm.wsspi.servlet.session.ConvergedAppUtils to get an HTTP session reference
     * for those HTTP sessions that belong to application sessions.
     * 
     * NOTE: This was copied from com.ibm.ws.sipcontainer/src/com/ibm/wsspi/sip/converge/ConvergedHttpSessionContextImpl.java
     * Any changes should be applied to both methods. 
     */
    public HttpSession getHttpSessionById(String sessId) {
    	HttpSessionImpl sd = null;
        IStore iStore = _coreHttpSessionManager.getIStore();

        if (c_logger.isTraceEntryExitEnabled()) {
        	StringBuffer sb = new StringBuffer(sessId).append(" ").append(iStore.getId());
            c_logger.traceEntry(this, "getHttpSessionById",sb.toString());
        }
        try {
            iStore.setThreadContext();
            sd = (HttpSessionImpl)_coreHttpSessionManager.getSession(sessId, true);
        } finally {
            iStore.unsetThreadContext();
        }
        
        if (sd!=null) {
        	if (c_logger.isTraceEntryExitEnabled()) {
                	 c_logger.traceExit(this, "getHttpSessionById","got a session");
            }
            return (HttpSession)sd.getFacade();
        } 
    	
        if (c_logger.isTraceEntryExitEnabled()) {
    		 c_logger.traceExit(this, "getHttpSessionById", null);
        }
        return null;
    } 
}
