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
package com.ibm.wsspi.sip.converge;

import javax.servlet.http.HttpSession;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.session.SessionApplicationParameters;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.webcontainer.httpsession.SessionManager;
import com.ibm.ws.webcontainer.session.IHttpSessionContext;
import com.ibm.ws.webcontainer.session.impl.SessionContextRegistryImpl;

/**
 * Extends the SessionContextRegistryImpl and providing ConvergedApplication (SIP and HTTP)
 * ability.
 * @author anatf
 *
 */
public class SessionContextRegistryConvergedImpl extends SessionContextRegistryImpl{

	/**
	 * Class Logger.
	 */
	private static final transient LogMgr c_logger = Log.get(SessionContextRegistryConvergedImpl.class);
	
	
	/**
     * @param smgr
     */
    public SessionContextRegistryConvergedImpl(SessionManager smgr) {
        super(smgr);
    }
    
    /*
     * (non-Javadoc)
     * @see com.ibm.ws.webcontainer.session.impl.SessionContextRegistryImpl#createSessionContextObject(com.ibm.ws.session.SessionManagerConfig, com.ibm.ws.session.SessionApplicationParameters)
     */
    protected IHttpSessionContext createSessionContextObject(SessionManagerConfig smc, SessionApplicationParameters sap)
    {
    	if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceDebug(this, "createSessionContextObject", "For APP = " + sap.getApplicationSessionName());
		}
        return new ConvergedHttpSessionContextImpl(smc, sap, this.smgr.getSessionStoreService());
    } 
    
    /*
     *  (non-Javadoc)
     * @see com.ibm.ws.session.SessionContextRegistry#getHttpSessionById(java.lang.String, java.lang.String, java.lang.String)
     */
    public HttpSession getHttpSessionById(String virtualHost, String contextRoot, String sessionId) {
    	
    	if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceEntry(this, "getHttpSessionById", "sessionID = " + sessionId);
		}
        HttpSession sess = null;
        ConvergedHttpSessionContextImpl wsCtx = getSessionContextByName(virtualHost + contextRoot);
        if (wsCtx ==null) {
            if (contextRoot.startsWith("/")) {
                wsCtx = getSessionContextByName(virtualHost + contextRoot.substring(1));
            } else {
                wsCtx = getSessionContextByName(virtualHost + "/" + contextRoot);
            }
        }
        if (wsCtx != null) {
            sess =  wsCtx.getHttpSessionById(sessionId);
        }
        if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceExit(this, "getHttpSessionById", "sessionID = " + sess.getId());
		}
        return sess;
    }  
    
    /*
     * Tries to get the WsSessionContext with the given appName as the key
     */
    static private ConvergedHttpSessionContextImpl getSessionContextByName(String appname) {
        return (ConvergedHttpSessionContextImpl)scrSessionContexts.get(appname);
    }    
    
}
