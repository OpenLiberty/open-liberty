/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.sip.converge;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.sip.ConvergedHttpSession;
import javax.servlet.sip.SipApplicationSession;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.websphere.servlet.session.IBMApplicationSession;
import com.ibm.websphere.servlet.session.IBMSession;
import com.ibm.ws.session.HttpSessionFacade;
import com.ibm.ws.session.IBMApplicationSessionImpl;
import com.ibm.ws.session.SessionContext;
import com.ibm.ws.session.SessionManager;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.sip.container.converged.WsHttpSessionFacade;
import com.ibm.ws.sip.container.converged.session.ApplicationSessionCreator;
import com.ibm.ws.sip.container.servlets.WASXSipApplicationSessionFactory;
import com.ibm.ws.webcontainer.session.impl.HttpSessionContextImpl;
import com.ibm.ws.webcontainer.session.impl.HttpSessionImpl;
import com.ibm.wsspi.session.ISession;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

/**
 * Implements ConvergedHttpSession API
 */
public class ConvergedHttpSessionImpl extends HttpSessionImpl implements ConvergedHttpSession{

	String sipCookieInfo=null;
	static ApplicationSessionCreator mAppSessCreator = null; 
	
	/**
	 * Class Logger.
	 */
	private static final transient LogMgr c_logger = Log.get(ConvergedHttpSessionImpl.class);
	
		
	/*
     * Constructor
     */
    public ConvergedHttpSessionImpl(ISession session, SessionContext sessCtx, ServletContext servCtx) {
    	super(session, sessCtx, servCtx);
    	 if (c_logger.isTraceEntryExitEnabled()) {
      		c_logger.traceEntry(this, "returnFacade", "returnFacade");
  		}
        WASXSipApplicationSessionFactory.getInstance().createSipApplicationSession();
        if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceExit(this, "ConvergedHttpSessionIml", "New Converged created");
		}
    }
    
    protected HttpSessionFacade returnFacade() {
    	 if (c_logger.isTraceDebugEnabled()) {
     		c_logger.traceDebug(this, "returnFacade", "returnFacade");
 		}
        return new WsHttpSessionFacade(this);
    }
    
  	public void setSIPCookieInfo(HttpServletRequest _request) {
  		 if (c_logger.isTraceEntryExitEnabled()) {
      		c_logger.traceEntry(this, "setSIPCookieInfo", "setSIPCookieInfo");
  		}
        String sipLogicalServerName=null;
        //only handle 1 ibmappid cookie
        byte[] byteSipLogicalServerName = ((IExtendedRequest)_request).getCookieValueAsBytes(SessionManagerConfig.sipSessionCookieName);
        if (byteSipLogicalServerName != null) {
            sipLogicalServerName = new String(byteSipLogicalServerName);
        }
        if (sipLogicalServerName!=null && sipLogicalServerName.length()>0) {
            sipCookieInfo = sipLogicalServerName;
        }
        if (c_logger.isTraceEntryExitEnabled()) {
      		c_logger.traceExit(this, "setSIPCookieInfo", "sipCookieInfo = " + sipCookieInfo);
  		}
    }
	
	/*
     * Get the IBM application session - create boolean controls whether one will be created
     * @see com.ibm.websphere.servlet.session.IBMSession#getIBMApplicationSession(boolean)
     */
    public IBMApplicationSession getIBMApplicationSession(boolean create) {
    	if (c_logger.isTraceDebugEnabled()) {
      		c_logger.traceDebug(this, "getIBMApplicationSession", "getIBMApplicationSession, create = " + create);
  		}
        return getIBMApplicationSession(create, sipCookieInfo);
    }
    
    /*
     * Get the IBM application session - create boolean controls whether one will be created
     * @see com.ibm.websphere.servlet.session.IBMSession#getIBMApplicationSession(boolean)
     */
    public IBMApplicationSession getIBMApplicationSession(boolean create, String logicalServerName) {
    	if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceEntry(this, "getIBMApplicationSession", "for app " + appName + " create " +  create + " IBMApplicationSession " + mIBMApplicationSession);
		}
        
        ServletContext sc = getServletContext();        

        boolean isSIP = this.getSessCtx().isSIPApplication();
        if ((mIBMApplicationSession == null) && (mAppSessCreator != null) && (isSIP) && (create)) {
            String scDisplayName = null;
            if (sc != null) {
                scDisplayName = sc.getServletContextName();           
            } else {
            	if (c_logger.isTraceDebugEnabled()) {
            		c_logger.traceDebug(this, "getIBMApplicationSession", "ServletContext is null");
                }
            }
            if (c_logger.isTraceDebugEnabled()) {
            	c_logger.traceDebug(this, "getIBMApplicationSession", "calling createApplicationSession with ServletContextName " + scDisplayName);
            }
            // cmd 363269 passing "display name" returned from getServletContextName.  This may be null.
            //            SIP team says they will handle it, and even return an app session, even if I pass in a null name.
            // cmd 372189 pass pathInfo to createApplicationSession so incoming ibmappid is used -- this may result in existing
            //            application session being returned so there might not be one actually created, contrary to the method name            
            mIBMApplicationSession = mAppSessCreator.createApplicationSession((IBMSession)this._httpSessionFacade, scDisplayName, pathInfoForAppSession);
        }
        if (!isSIP) { // && (mIBMApplicationSession == null)) {
            // what do we do for non-sip app session?
//            ApplicationSessionManager sm = (ApplicationSessionManager) this.getSessCtx()._coreHttpAppSessionManager;
            SessionManager sm = (SessionManager) this.getSessCtx()._coreHttpAppSessionManager;
            if (sm!=null) {
                //this.getSessCtx()._sam.getInUseSessionVersion(req, sac)
                mIBMApplicationSession = (IBMApplicationSession) sm.getSession(this.getId(), this.getISession().getVersion(), true, null);
                //usingAppSessions & invalidateAll
                if (SessionManagerConfig.getUsingApplicationSessionsAndInvalidateAll()) {
                    if (mIBMApplicationSession!=null && ((IBMApplicationSessionImpl)mIBMApplicationSession).getISession()!=null && ((IBMApplicationSessionImpl)mIBMApplicationSession).getISession().getMaxInactiveInterval() == 0)  {
                        // Max Inact of 0 implies session is invalid -- set by remote invalidateAll processing
                        // we expect invalidator thread to clean it up, but if app requests the session before that
                        // happens, invalidate it here so it isn't given back out to app.
                        mIBMApplicationSession.invalidate();
                        mIBMApplicationSession = null;
                    }
                }
                //we will create this NOW
                if (mIBMApplicationSession==null && create) {
                    mIBMApplicationSession = (IBMApplicationSession)sm.createSession(this.getId(), this.getISession().getVersion(), true);
                }
            }
        }
        if (c_logger.isTraceEntryExitEnabled()) {
        	c_logger.traceExit(this, "getIBMApplicationSession",mIBMApplicationSession);
        }
        return mIBMApplicationSession;
    }
    

    /*The following 3 methods are added to support SIP JSR 289
     * public String encodeURL(String url) 
     * public String encodeURL(String relativePath, String scheme)
     * public IBMApplicationSession getApplicationSession()
     */
    //should only be called by a SIP app
    public String encodeURL(String url) {
    	if (c_logger.isTraceEntryExitEnabled()) {
        	c_logger.traceEntry(this, "encodeURL", "encodeURL" );
        }
    	String returnUrl = null;
        
        if (mAppSessCreator!=null) {
            //append jsessionid
            String httpEncoded = ((HttpSessionContextImpl)this.getSessCtx()).encodeURLForSipConvergedApps(this, url);
            if (c_logger.isTraceDebugEnabled()) {
            	c_logger.traceDebug(this, "encodeURL", "url encoded with httpsession: " + httpEncoded);
            }
            //call sip method to append ibmappid
            returnUrl = getIBMApplicationSession(true).encodeURI(httpEncoded); 
        } 
        
        if (c_logger.isTraceEntryExitEnabled()) {
        	c_logger.traceExit(this, "encodeURL", "returned url: " + returnUrl);
        }
        return returnUrl;
    }
    
    /**
     * should only be called by a SIP app
     */
    public String encodeURL(String relativePath, String scheme) {
    	if (c_logger.isTraceDebugEnabled()) {
        	c_logger.traceDebug(this, "encodeURL", "relativePath = " + relativePath + 
        											" scheme = " + scheme );
        }
        if (mAppSessCreator!=null) {
        	if (c_logger.isTraceEntryExitEnabled()) {
            	c_logger.traceEntry(this,"encodeURL", "encoding url with relative path = " + relativePath +" and scheme = " + scheme);
            }
            String contextPath = getServletContext().getContextPath();
            ConvergedHttpSessionContextImpl sessCtx = (ConvergedHttpSessionContextImpl)this.getSessCtx();
            String fullyQualifiedUrl = sessCtx.getSipBaseUrlForEncoding(contextPath, relativePath, scheme);
            if (c_logger.isTraceEntryExitEnabled()) {
            	c_logger.traceExit(this, "encodeURL", "going to encode fully qualified url = " + fullyQualifiedUrl);
            }
            return encodeURL(fullyQualifiedUrl);
        } else {
            return null;
        }
    }
    //should only be called by a SIP app
    public SipApplicationSession getApplicationSession() {
    	if (c_logger.isTraceDebugEnabled()) {
        	c_logger.traceDebug(this, "getApplicationSession", "getApplicationSession");
        }
        if (mAppSessCreator!=null) {
            return (SipApplicationSession)getIBMApplicationSession(true);
        } else {
            return null;
        }
    }
    
    /*
     * Set the application session creator
     */
    public static void setAppSessCreator(ApplicationSessionCreator appSessCreator) {
    	if (c_logger.isTraceDebugEnabled()) {
        	c_logger.traceDebug(ConvergedHttpSessionImpl.class,"setAppSessCreator", "setting " + appSessCreator);
        }
        mAppSessCreator = appSessCreator;
    }
    
}
