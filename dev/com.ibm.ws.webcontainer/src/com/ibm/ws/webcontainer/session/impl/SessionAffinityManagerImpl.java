/*******************************************************************************
 * Copyright (c) 2010, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.session.impl;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.logging.Level;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import com.ibm.websphere.security.WSSecurityHelper;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.session.SameSiteCookie;
import com.ibm.ws.session.SessionAffinityManager;
import com.ibm.ws.session.SessionContext;
import com.ibm.ws.session.SessionCrossoverStackTrace;
import com.ibm.ws.session.SessionIDGeneratorImpl;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.webcontainer.osgi.webapp.WebAppConfiguration;
import com.ibm.wsspi.session.IStore;
import com.ibm.wsspi.session.SessionAffinityContext;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.servlet.IExtendedResponse;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

public class SessionAffinityManagerImpl extends SessionAffinityManager {

    private static final String methodClassName = "SessionAffinityManagerImpl";

    public SessionAffinityManagerImpl(SessionManagerConfig smc, SessionContext sctx, IStore istore) {
        super(smc, sctx, istore); 
        _cloneID = SessionManagerConfig.getCloneId();
        
        // turn off cachid (aka version) if in-mem and prop set
        if (_smc.isUsingMemory() && SessionManagerConfig.isTurnOffCacheId()) {
            _versionPrefixLength = 0;
            _versionPlusIdLength = SessionManagerConfig.getSessionIDLength();
        }        
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodClassName, "Clone ID of this server=" + _cloneID);        
        }
    }

    public String getRequestedSessionIdFromURL(ServletRequest request) {
        
        return getRequestedSessionIdFromURL(request, false);
        
    }
    /**
     * Requested session id from URL
     */
    public String getRequestedSessionIdFromURL(ServletRequest request, boolean force) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[GET_REQUESTED_SESSION_ID_FROM_URL],"force="+force);
        }
        String sessionID = null;
        if (_smc.getEnableUrlRewriting() || force) {
            String requestURI = ((IExtendedRequest) request).getEncodedRequestURI();
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[GET_REQUESTED_SESSION_ID_FROM_URL], "request uri:" + requestURI);
            }
            int prefix = requestURI.indexOf(_smc.getSessUrlRewritePrefix());
            if (prefix != -1) {
                int sessionIDstart = requestURI.indexOf("=", prefix + 1);
                int i = requestURI.indexOf(";", prefix + 1);
                int j = requestURI.indexOf("?", prefix + 1);
                int postfix = -1;
                if (i == -1) {
                    postfix = j;
                } else if (j == -1) {
                    postfix = i;
                } else
                    postfix = (i > j) ? j : i;

                if (postfix != -1) {
                    sessionID = requestURI.substring(sessionIDstart + 1, postfix);
                } else {
                    sessionID = requestURI.substring(sessionIDstart + 1);
                }
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[GET_REQUESTED_SESSION_ID_FROM_URL], sessionID);
        }
        return sessionID;
    }

    public List getAllCookieValues(ServletRequest request) {
        return ((IExtendedRequest) request).getAllCookieValues(_smc.getSessionCookieName());
    }

    /**
     * Method analyzeRequest
     * <p>
     * 
     * @param request
     * @return SessionAffinityContext
     * @see com.ibm.wsspi.session.ISessionAffinityManager#analyzeRequest(javax.servlet.ServletRequest)
     */
    public SessionAffinityContext analyzeRequest(ServletRequest request) {
        // create local variable - JIT performance improvement
        final boolean isTraceOn = com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[ANALYZE_REQUEST]);
        }

        String sessionID = null;
        boolean reqFromCookie = false;
        boolean reqFromURL = false;
        boolean reqFromSSL = false;
        boolean reqFromClient = false;
        SessionAffinityContext sessionAffinityContext = null;
        List allSessionIds = null;

        // tries to get the sac from SSL (if applicable)
        if (_smc.useSSLId()) {
            String sessionId = getActualSSLSessionId(request);
            sessionAffinityContext = analyzeSSLRequest(request, sessionId);
        }
        // we found a sessionAffinityContext for SSL ... return
        if (sessionAffinityContext != null) {
            if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[ANALYZE_REQUEST], "Affinity Context found from SSL");
            }
            return sessionAffinityContext;
        }

        // try a non-SSL request
        if (_smc.getEnableCookies()) {
            // allSessionIds =
            // ((IExtendedRequest)request).getAllCookieValues(_smc.getSessionCookieName());
            byte[] byteSessId = ((IExtendedRequest) request).getCookieValueAsBytes(_smc.getSessionCookieName());
            if (byteSessId != null) {
                allSessionIds = new ArrayList(1);
                allSessionIds.add(0, new String(byteSessId));
                if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[ANALYZE_REQUEST], "Found session id(s) in cookie");
                }
                reqFromCookie = true;
            } else { // 105740: Good message when cookies doesn't contain the sessionID
                if (isTraceOn&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[ANALYZE_REQUEST], "No session id(s) found in cookie");
                }
            }


            /*
             * allSessionIds =
             * ((IExtendedRequest)request).getAllCookieValues(_smc.getSessionCookieName
             * ());
             * if ((allSessionIds != null) && (!allSessionIds.isEmpty())) {
             * if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.
             * SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
             * LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName,
             * methodNames[ANALYZE_REQUEST], "Found session id(s) in cookie");
             * }
             * reqFromCookie = true;
             * }
             */
        }

        if ((!reqFromCookie) && (_smc.getEnableUrlRewriting())) {
            sessionID = getRequestedSessionIdFromURL(request);
            if (sessionID != null) {
                reqFromURL = true;
                allSessionIds = new ArrayList(1);
                allSessionIds.add(0, sessionID);
            }
            if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[ANALYZE_REQUEST], "Found session id in URL: " + sessionID);
            }
        } 
        
        
        // Maybe a cookie was sent using the wrong means - ok it should be treated as
        // invalid but should be returned from ServletRequest.getRequestedSessionId()
        if (!reqFromCookie && !reqFromURL) {            
            if (_smc.getEnableCookies()) {
                // If cookies were enable now look in the URL
                sessionID = getRequestedSessionIdFromURL(request,true);
                if (sessionID != null) {
                    if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[ANALYZE_REQUEST], "Found client session id in URL: " + sessionID);
                    }
                    reqFromClient = true;
                    allSessionIds = new ArrayList(1);
                    allSessionIds.add(0, sessionID);
                }              
            } else {               
                byte[] byteSessId = ((IExtendedRequest) request).getCookieValueAsBytes(_smc.getSessionCookieName());
                if (byteSessId != null) {
                    allSessionIds = new ArrayList(1);
                    allSessionIds.add(0, new String(byteSessId));
                    if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[ANALYZE_REQUEST], "Found client session id(s) in cookie");
                    }
                    reqFromClient = true;
                } else { // 105740: Good message when cookies doesn't contain the sessionID
                    if (isTraceOn&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[ANALYZE_REQUEST], "No session id(s) found in cookie");
                    }
                }
            }    
        }
        
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[ANALYZE_REQUEST], "allSessionIds size = " + (allSessionIds == null ? "0" : allSessionIds.size()));
        }

        sessionAffinityContext = new SessionAffinityContext(allSessionIds, reqFromCookie, reqFromURL, reqFromSSL,reqFromClient);
        setNextId(sessionAffinityContext);

        // Since we didn't have a sac, but we have session id string...we've been
        // remotely dispatched
        // We need to set the version, id, and clones from the request string into
        // the response fields
        // of our sac.
        String idFromRequest = ((IExtendedRequest) request).getUpdatedSessionId();
        if (idFromRequest != null) { // if yes, modify newly created sac with that data
            setResponseData(idFromRequest, sessionAffinityContext);
        }
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[ANALYZE_REQUEST]);
        }
        return sessionAffinityContext;
    }

    /*
     * analyzeSSLRequest - taken from WsSessionAffinityManager in WAS7
     */
    public SessionAffinityContext analyzeSSLRequest(ServletRequest request, String sslSessionId) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[ANALYZE_SSL_REQUEST]);
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[ANALYZE_SSL_REQUEST], "SSL Id from Request = " + sslSessionId);
        }

        String sessionId = sslSessionId;
        boolean reqFromCookie = false;
        boolean reqFromURL = false;
        boolean reqFromSSL = false;
        SessionAffinityContext sessionAffinityContext = null;
        List allSessionIds = null;

        IExtendedRequest sessReq = (IExtendedRequest) request;
        if (sessionId != null) {
            reqFromSSL = true;
            String tempDummyId;
            String tempCacheId;
            if (_smc.isUsingMemory()) {
                tempCacheId = "0000";
            } else {
                tempCacheId = "0001";
            }
            String tempCloneInfo = "";
            // look for cacheid and clone info in the cookie or Rewritten URL

            // cmd 213330 start
            String extendedId = null;
            byte[] byteExtendedId = sessReq.getCookieValueAsBytes(SessionManagerConfig.dcookieName);
            if (byteExtendedId != null) {
                extendedId = new String(byteExtendedId);
            } // cmd 213330 end

            if (extendedId == null) {
                extendedId = getRequestedSessionIdFromURL(request);
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[ANALYZE_SSL_REQUEST], "getRequestedSessionId - encoded URL contains: "
                                                                                                                        + extendedId);
                }
            }

            if (extendedId != null) {
                tempCacheId = extendedId.substring(0, 4); // cacheid is always first
                int index = extendedId.indexOf(SessionManagerConfig.getCloneSeparator());
                if (index != -1) {
                    tempDummyId = extendedId.substring(4, index);
                    tempCloneInfo = extendedId.substring(index);
                }
            }
            sessionId = tempCacheId + SessionAffinityContext.SSLSessionId + tempCloneInfo;
            allSessionIds = new ArrayList(1);
            allSessionIds.add(0, sessionId);
            sessionAffinityContext = new SessionAffinityContext(allSessionIds, reqFromCookie, reqFromURL, reqFromSSL);
            setNextId(sessionAffinityContext);
        }
        // Use SSL Sessionid NOT dummy id
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[ANALYZE_SSL_REQUEST], "getRequestedSessionId - massaged long SSL id is now: " + sessionId);
        }
        // returns null if no SSL context
        return sessionAffinityContext;
    }

    /*
     * Method used to get at the SSL Id rather than the displayed Id
     */
    public String getActualSSLSessionId(ServletRequest request) {
        String sessionID = null;
        byte[] sslBytes = ((IExtendedRequest) request).getSSLId();
        if (sslBytes != null) {
            sessionID = SessionIDGeneratorImpl.convertSessionIdBytesToSessionId(sslBytes, SessionManagerConfig.getSessionIDLength());
        }
        return sessionID;
    }
    
    protected WebAppConfig getWebAppConfig() {
        WebAppConfig wac = null;
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cmd instanceof WebComponentMetaData) { // Only get the header for web modules, i.e. not for EJB
            WebModuleMetaData wmmd = (WebModuleMetaData) ((WebComponentMetaData) cmd).getModuleMetaData();
            wac = wmmd.getConfiguration();
            if (!(wac instanceof com.ibm.ws.webcontainer.osgi.webapp.WebAppConfiguration)) {
                wac = null;
            }
        }
        return wac;
    }

    public String getFeatureAuthzRoleHeaderValue() {
        String name = null;
        WebAppConfig wac = getWebAppConfig();
        if (wac != null && wac instanceof WebAppConfiguration) {
            Dictionary<String, String> headers =
                            ((WebAppConfiguration) wac).getBundleHeaders();
            if (headers != null)
                name = headers.get("IBM-Authorization-Roles");
        }
        return name;
    }    
    
    public void setCookie(ServletRequest request, ServletResponse response,
                          SessionAffinityContext affinityContext, Object session) {

        //create local variable - JIT performance improvement
        final boolean isTraceOn = com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[SET_COOKIE]);
        }

        // check if server will allow setting of cookies ... if not this function returns
        if (!_smc.getEnableCookies()) {
            if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[SET_COOKIE], "Cookies not enabled.");
            }
            return;
        }

        int sessionVersion = affinityContext.getResponseSessionVersion();
        String sessionID = affinityContext.getResponseSessionID();
        String incomingCloneIDs = affinityContext.getInputCloneInfo();
        String incomingSessionID = affinityContext.getRequestedSessionID();
        int incomingSessionVersion = affinityContext.getRequestedSessionVersion();
        String cloneSeparatorPlusID = getSeparatorPlusAffinityToken(sessionID);
        //if this particular cookie has been set during this request, we shouldn't have to reset it
        boolean previouslySetSessionCookie = affinityContext.isSessionCookieSet();
        /*
         * Determine if we need to return a cookie - 4 cases:
         * 1) no incoming ID
         * 2) incoming ID different than the current session id
         * 3) version/cacheid mismatch
         * 4) our clone id isn't in the incoming clones
         */
        if ((null == incomingSessionID) || // no incoming id  OR
            (!incomingSessionID.equals(sessionID)) || // incoming id doesn't match this id  OR
            (incomingSessionVersion != sessionVersion) || // versions don't match  OR
            (incomingCloneIDs.indexOf(cloneSeparatorPlusID) == -1)) { // our clone wasn't in input clones

            if (_smc.isDebugSessionCrossover() && _sessCtx.crossoverCheck((HttpSession) session)) {
                Object parms[] = new Object[] { _sessCtx.getAppName(), ((HttpSession) session).getId(), _sessCtx.getCurrentSessionId() };
                //Needed to create a LogRecord so we could have parameters and a throwable in the same log
                LoggingUtil.logParamsAndException(LoggingUtil.SESSION_LOGGER_CORE, Level.SEVERE, methodClassName, methodNames[SET_COOKIE], "SessionContext.CrossoverOnReturn",
                                                  parms, new SessionCrossoverStackTrace());
            } else {
                String sessionVersionString = getVersionString(sessionVersion);

                final StringBuffer cookieString = new StringBuffer();
                cookieString.append(sessionVersionString).append(sessionID);
                String cloneInfo = updateCloneInfo(affinityContext, cloneSeparatorPlusID);
                cookieString.append(cloneInfo);

                if ((cookieString.length()) > SessionManagerConfig.getMaxSessionIdentifierLength()) {
                    if (LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.WARNING)) {
                        String parms[] = { cookieString.toString(), new Integer(SessionManagerConfig.getMaxSessionIdentifierLength()).toString() };
                        LoggingUtil.SESSION_LOGGER_CORE.logp(Level.WARNING, methodClassName, methodNames[SET_COOKIE], "SessionContext.maxSessionIdLengthExceeded", parms);
                    }
                }

                whichCookie = _smc.getSessionCookieName();
                //handle SSL - ID will never change
                /*
                 * using value defined in whichCookie - if (affinityContext.isRequestedSessionIDFromSSL()) {
                 * whichCookie = SessionManagerConfig.dcookieName;
                 * }
                 */
                StringBuffer logStringBuffer = new StringBuffer();
                
                Cookie cookie = AccessController.doPrivileged(new PrivilegedAction<Cookie>() {
                    public Cookie run() {
                        return new Cookie(whichCookie, cookieString.toString());
                    }
                });
                cookie.setPath(_smc.getSessionCookiePath());
                cookie.setComment(_smc.getSessionCookieComment());
                cookie.setMaxAge(_smc.getSessionCookieMaxAge());
                
                if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[SET_COOKIE], "AppName = " + getFeatureAuthzRoleHeaderValue() +
                                                            "; Global Security = " + WSSecurityHelper.isGlobalSecurityEnabled());
                }
                if (getFeatureAuthzRoleHeaderValue() != null)
                    cookie.setSecure(WSSecurityHelper.isGlobalSecurityEnabled());
                else
                    cookie.setSecure(_smc.getSessionCookieSecure());
                
                cookie.setHttpOnly(_smc.getSessionCookieHttpOnly());

                if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                    logStringBuffer.append("Setting cookie: ").append(whichCookie)
                                    .append(";Path: ").append(_smc.getSessionCookiePath())
                                    .append(";Comment: ").append(_smc.getSessionCookieComment())
                                    .append(";MaxAge: ").append(_smc.getSessionCookieMaxAge())
                                    .append(";Secure: ").append(cookie.getSecure())
                                    .append(";HttpOnly: ").append(_smc.getSessionCookieHttpOnly());
                }
                if (_smc.getSessionCookieDomain() != null && !_smc.getSessionCookieDomain().equals("")) {
                    cookie.setDomain(_smc.getSessionCookieDomain());
                    if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                        logStringBuffer.append(";Domain: ").append(_smc.getSessionCookieDomain());
                    }
                }
                if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[SET_COOKIE], logStringBuffer.toString());
                }
                /*
                 * Removed with the addition of 542491
                 * if (response.isCommitted()) {
                 * // PK53762: Check if cookie is within the same request object
                 * boolean cookieSetInSameRequest = new Boolean( (String) request.getAttribute("com.ibm.ws.session.cookie.already.set") ).booleanValue();
                 * if ( isTraceOn&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE) ) {
                 * LoggingUtil.SESSION_LOGGER_CORE.logp( Level.FINE, methodClassName, methodNames[SET_COOKIE], "request.getAttribute(com.ibm.ws.session.cookie.already.set): " +
                 * request.getAttribute("com.ibm.ws.session.cookie.already.set") );
                 * }
                 * if (!cookieSetInSameRequest) {
                 * LoggingUtil.SESSION_LOGGER_CORE.logp(Level.WARNING, methodClassName, methodNames[SET_COOKIE], "SessionContext.responseAlreadyCommitted");
                 * // PK53762: Add header to keep track that we have set this message once
                 * request.setAttribute( "com.ibm.ws.session.cookie.already.set", "true" );
                 * if ( isTraceOn&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE) ) {
                 * LoggingUtil.SESSION_LOGGER_CORE.logp( Level.FINE, methodClassName, methodNames[SET_COOKIE], "Set request attribute com.ibm.ws.session.cookie.already.set to true"
                 * );
                 * }
                 * //return;
                 * } else {
                 * if ( isTraceOn&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE) ) {
                 * LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[SET_COOKIE], "responseAlreadyCommitted already logged");
                 * }
                 * return;
                 * }
                 * }
                 */
                if (!previouslySetSessionCookie || !response.isCommitted()) {
                    affinityContext.setSessionCookieSet(true); //changes previouslySetSessionCookie the next time through
                    if (response.isCommitted()) { //this means that previouslySetSessionCookie has to be false
                        LoggingUtil.SESSION_LOGGER_CORE.logp(Level.WARNING, methodClassName, methodNames[SET_COOKIE], "SessionContext.responseAlreadyCommitted");
                        
                        if (isTraceOn&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) { //only log if tracing is on, do not want overhead of ffdc during normal operation
                            try { //start tWAS 755981 (Liberty - SCWI 135422)
                                    throw new Exception("Stack trace for SESN0066E:"); // stack trace for when responseAlreadyCommitted message shows up
                            } catch (Throwable th) {
                                    com.ibm.ws.ffdc.FFDCFilter.processException(th, "com.ibm.ws.webcontainer.session.impl.SessionAffinityManagerImpl", "398", null);
                            } // end tWAS 755981 (Liberty - SCWI 135422)
                        }
                    }
                    // Get the appropriate SameSite value from the configuration and pass to the WebContainer using the RequestState 
                    SameSiteCookie sessionSameSiteCookie = _smc.getSessionCookieSameSite();
                    if (sessionSameSiteCookie != SameSiteCookie.DISABLED) {
                        // If SameSite=None set Secure if not already set.
                        if(sessionSameSiteCookie == SameSiteCookie.NONE && !cookie.getSecure()) {
                            if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[SET_COOKIE],
                                                                     "Setting the Secure attribute for SameSite=None");
                            }
                            cookie.setSecure(true);
                        }

                        WebContainerRequestState requestState = WebContainerRequestState.getInstance(true);
                        String sameSiteCookieValue = sessionSameSiteCookie.getSameSiteCookieValue();
                        requestState.setCookieAttributes(cookie.getName(), "SameSite=" + sameSiteCookieValue);
                    }
                    ((IExtendedResponse) response).addSessionCookie(cookie);
                } else {
                    if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[SET_COOKIE],
                                                             "The session cookie was already set for this request and the response has already been committed.");
                    }
                }
            }
        }
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[SET_COOKIE]);
        }
    }

    public void setSIPCookie(ServletRequest request, ServletResponse response, String sipCookieString) {

        // create local variable - JIT performance improvement
        final boolean isTraceOn = com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[SET_SIP_COOKIE]);
        }

        // check if server will allow setting of cookies ... if not this function
        // returns
        if (!_smc.getEnableCookies()) {
            if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[SET_SIP_COOKIE], "Cookies not enabled.");
            }
            return;
        }

        // we assume if you call this then we are going to set the SIP logical name
        // cookie
        String whichSIPCookie = _smc.getSipSessionCookieName();
        Cookie cookie = new Cookie(whichSIPCookie, sipCookieString);
        // cookie.setPath(_smc.getSessionCookiePath());
        // cookie.setComment(_smc.getSessionCookieComment());
        // cookie.setMaxAge(_smc.getSessionCookieMaxAge());
        // cookie.setSecure(_smc.getSessionCookieSecure());

        if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[SET_SIP_COOKIE], "Setting cookie: " + whichSIPCookie);
        }
        if (response.isCommitted()) {
            // PK53762: Check if cookie is within the same request object
            boolean cookieSetInSameRequest = new Boolean((String) request.getAttribute("com.ibm.ws.session.cookie.already.set")).booleanValue();
            if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE,
                                                     methodClassName,
                                                     methodNames[SET_SIP_COOKIE],
                                                     "request.getAttribute(com.ibm.ws.session.cookie.already.set): "
                                                                     + request.getAttribute("com.ibm.ws.session.cookie.already.set"));
            }
            if (!cookieSetInSameRequest) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.WARNING, methodClassName, methodNames[SET_SIP_COOKIE], "SessionContext.responseAlreadyCommitted");
                // PK53762: Add header to keep track that we have set this message once
                request.setAttribute("com.ibm.ws.session.cookie.already.set", "true");
                if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[SET_SIP_COOKIE],
                                                         "Set request attribute com.ibm.ws.session.cookie.already.set to true");
                    
                    try { //start tWAS 755981 (Liberty - SCWI 135422)
                        throw new Exception("Stack trace for SESN0066E:"); // stack trace for when responseAlreadyCommitted message shows up
                    } catch (Throwable th) {
                        com.ibm.ws.ffdc.FFDCFilter.processException(th, "com.ibm.ws.webcontainer.session.impl.SessionAffinityManagerImpl", "467", null);
                    } // end tWAS 755981 (Liberty - SCWI 135422)
                }
                // return;
            } else {
                if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[SET_SIP_COOKIE], "responseAlreadyCommitted already logged");
                }
                return;
            }
        }
        // ok to call this since the cookie has a different name than the regular
        // JSESSIONID cookie
        ((IExtendedResponse) response).addSessionCookie(cookie);
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[SET_SIP_COOKIE]);
        }
    }
}
