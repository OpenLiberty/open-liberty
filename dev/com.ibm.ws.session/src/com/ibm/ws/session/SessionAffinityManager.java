/*******************************************************************************
 * Copyright (c) 1997, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session;

import java.util.List;
import java.util.logging.Level;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.wsspi.session.IProtocolAdapter;
import com.ibm.wsspi.session.ISession;
import com.ibm.wsspi.session.ISessionAffinityManager;
import com.ibm.wsspi.session.IStore;
import com.ibm.wsspi.session.SessionAffinityContext;

/**
 * This class provides an implementation for the default Affinity Manager in
 * Websphere.
 */
public class SessionAffinityManager implements ISessionAffinityManager {

    // ----------------------------------------
    // Private Members
    // ----------------------------------------

    private static final String methodClassName = "SessionAffinityManager";

    /*
     * String cloneID of this server
     */
    protected String _cloneID = "";

    /*
     * Version prefix
     */
    private final String _versionPrefix = "0000";
    protected int _versionPrefixLength = 4;
    protected int _versionPlusIdLength;

    protected SessionManagerConfig _smc;
    protected SessionContext _sessCtx;
    protected IStore _store;

    protected String whichCookie = "";

    protected static final int GET_REQUESTED_SESSION_ID_FROM_URL = 0;
    protected static final int ANALYZE_REQUEST = 1;
    protected static final int SET_NEXT_ID = 2;
    protected static final int ENCODE_URL = 3;
    protected static final int SET_COOKIE = 4;
    protected static final int SET_SIP_COOKIE = 5;
    protected static final int ANALYZE_SSL_REQUEST = 6;
    private static final int UPDATE_CLONE_INFO = 7;
    protected final static String methodNames[] = { "getRequestedSessionIdFromURL", "analyzeRequest", "setNextId", "encodeURL", "setCookie", "setSIPCookie", "analyzeSSLRequest",
                                                   "UpdateCloneInfo" };

    // ----------------------------------------
    // Public Constructor
    // ----------------------------------------
    public SessionAffinityManager(SessionManagerConfig smc, SessionContext sessCtx, IStore store) {

        _smc = smc;
        _sessCtx = sessCtx;
        _store = store;
        _versionPlusIdLength = _versionPrefixLength + SessionManagerConfig.getSessionIDLength();

    }

    // ----------------------------------------
    // Public Methods
    // ----------------------------------------

    /**
     * Requested session id from URL
     */
    public String getRequestedSessionIdFromURL(ServletRequest request) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[GET_REQUESTED_SESSION_ID_FROM_URL]);
        }
        String sessionID = null;
        // LIBERTY Cannot reach IExtendedRequest from here anymore
        // overridden in SessionAffinityManagerImpl
        return null;
    }

    @Override
    public List getAllCookieValues(ServletRequest request) {
        // LIBERTY Cannot reach IExtendedRequest from here
        // overridden in SessionAffinityManagerImpl
        return null;
    }

    /**
     * Method analyzeRequest
     * <p>
     * 
     * @param request
     * @return SessionAffinityContext
     * @see com.ibm.wsspi.session.ISessionAffinityManager#analyzeRequest(javax.servlet.ServletRequest)
     */
    @Override
    public SessionAffinityContext analyzeRequest(ServletRequest request) {
        // LIBERTY Cannot reach IExtendedRequest from here.
        // overridden in SessionAffinityManagerImpl
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[ANALYZE_REQUEST], "");
        }
        return null;
    }

    @Override
    public boolean setNextId(SessionAffinityContext sac) {
        boolean rc = false;
        String versionString = "";
        String id = null;
        String cloneInfo = ""; // empty string, not null
        // create local variable - JIT performance improvement
        final boolean isTraceOn = com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[SET_NEXT_ID]);
        }
        List allSessionIds = sac.getAllSessionIds();
        if ((allSessionIds != null) && (!allSessionIds.isEmpty()) && !sac.isResponseIdSet()) {
            String nextId = (String) allSessionIds.remove(0); // removes and returns
            
            if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[SET_NEXT_ID], "Next id is " + (nextId!=null ? nextId : "Null"));
            }
            if (sac.isFirstSessionIdValid()) {
                if ((nextId != null) && ((nextId.length() >= _versionPlusIdLength) || _smc.useSSLId())) {
                    rc = true; // we found another id
                    sac.setRequestedVersion(0); // if NoAdditionalSessionInfo is set, this
                    // won't change. Otherwise, it will change.
                    if (_versionPrefixLength > 0) {
                        versionString = nextId.substring(0, _versionPrefixLength);
                        try {
                            int sessionVersion = Integer.parseInt(versionString);
                            sac.setRequestedVersion(sessionVersion);
                        } catch (NumberFormatException e) {
                            if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[SET_NEXT_ID], "Caught exception when parsing JSessionID in cookie:", e);
                            }
                        }
                    }
                    int index = nextId.indexOf(SessionManagerConfig.getCloneSeparator());
                    if (index == -1) { // no clones
                        id = nextId.substring(_versionPrefixLength);
                    } else if (index < _versionPrefixLength) { //PK86131
                        LoggingUtil.SESSION_LOGGER_CORE.logp(Level.WARNING, methodClassName, methodNames[SET_NEXT_ID], "Symbol(:) exception caught in JSessionID, setting: " + 
                                        id + " to null.");
                        id = null;
                    } else {
                        // cmd we are including leading separator just like 6.1 and earlier
                        cloneInfo = nextId.substring(index);
                        id = nextId.substring(_versionPrefixLength, index);
                    }
                    if (id != null) { // START PI10958
                        int idLength = id.length();
                        int expectedIdLength = SessionManagerConfig.getSessionIDLength();
                        if(idLength>expectedIdLength) {
                            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.WARNING, methodClassName, methodNames[SET_NEXT_ID], "Detected JSESSIONID with invalid length; expected length of "+expectedIdLength+", found "+idLength+", setting: " + id + " to null.");
                            id = null;                        
                        }
                    } // END PI10958
                    sac.setRequestedSessionID(id);// id is SESSIONMANAGEMENTAFFINI FOR SSL
                    // HERE!!!!

                    // PI18177
                    if (SessionManagerConfig.isCloneIdPropertySet() && SessionManagerConfig.isExpectedCloneIdsPropertySet() && index > 0) { // HttpCloneIdProperty set, ExpectedCloneIdsProperty is set, and we have a clone

                        String cloneSeparatorAsString = String.valueOf(SessionManagerConfig.getCloneSeparator());

                        String [] cloneIds = cloneInfo.split(cloneSeparatorAsString);

                        for (int i = 0; i < cloneIds.length; i++) {
                            if (cloneIds[i].length() > 0 && !SessionManagerConfig.getExpectedCloneIds().contains(cloneIds[i])) {
                                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.WARNING, methodClassName, methodNames[SET_NEXT_ID], "Detected suspicious cloneId associated with the incoming request. The" +
                                                " list of incoming cloneIds "+cloneInfo+" is no longer valid and will be cleared.");

                                cloneInfo = "";
                                break; // bail out processing after encountering a clone id that is not expected
                            }
                        }

                    }  // PI18177

                    sac.setInputCloneInfo(cloneInfo);

                    if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                        StringBuffer sb = new StringBuffer("Version:").append(versionString).append(":Id:").append(id).append(":CloneInfo:").append(cloneInfo);
                        LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[SET_NEXT_ID], sb.toString());
                    } 
                } else if (nextId != null && nextId.length() < _versionPlusIdLength) { //PM89885 if a request has multiple JSESSIONIDs and the first id does not have additional session information, 
                    // add? sac.setRequestedSessionID(nextId);   
                    sac.setFirstSessionIdValid(false);                      // we need to flag that occurrence so that we know we need to call getAllCookieValues later on
                }
            }

        }
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[SET_NEXT_ID], Boolean.valueOf(rc));
        }
        return rc;
    }

    /**
     * to check if given uri is safe to encode
     */
    final static String[] unSecStrs = { "<", ">", "&", "!", "'", "(", ")", "\"" };

    private static boolean isSafe(String id) {
        for (int i = 0; i < unSecStrs.length; i++)
            if (id.indexOf(unSecStrs[i]) != -1)
                return false;
        return true;
    }

    //called from ConvergedHttpSession.encodeURL path
    public String encodeURL(HttpSession session, String url) {
        return encodeURL(session, null, url, null);
    }

    private String encodeURL(HttpSession sessForSip, ServletRequest request, String uri, SessionAffinityContext affinityContext) {
        String sessIdOnly;
        String sessionVersionString;
        String sessionID;
        if (affinityContext != null) { //a normal Response.encodeURL method
            int sessionVersion = affinityContext.getResponseSessionVersion();
            sessIdOnly = affinityContext.getResponseSessionID();
            sessionVersionString = getVersionString(sessionVersion);
        } else { //when called using the ConvergedHttpSession
            ISession isess = ((SessionData) sessForSip).getISession();
            sessIdOnly = isess.getId();
            sessionVersionString = getVersionString(isess.getVersion());
        }
        sessionID = sessionVersionString.concat(sessIdOnly);

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[ENCODE_URL]);
        }
        // do a security check of id for any malicious characters
        if (!isSafe(sessionID)) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[ENCODE_URL], "URL is not safe");
            }
            return uri;
        }

        int postStart = -1; // position to start appending back after encoded
                            // session id
        int preEnd = uri.indexOf(_smc.getSessUrlRewritePrefix());

        if (preEnd != -1) { // sessid already encoded, must replace
            postStart = uri.indexOf(";", preEnd + 1);// look for semi after encoded
                                                     // session id
            // if found, another path parm exists - must append back
        }
        if (postStart == -1) { // no session id already encoded, or no semicolon following...
            int i = uri.indexOf("?"); // look for start of query string.....
            int j = uri.indexOf("#"); // not sure why we are looking for this....
            if ((i != -1) && (j != -1) && (j < i))
                i = j; // get the index of the first occurence of ? or #
            if ((i == -1) && (j != -1))
                i = j;
            postStart = i;
        }

        if (preEnd == -1) { // encoded session id not included in input uri
            preEnd = postStart; // so both of these will be -1 or else position of
                                // query string
        }

        String pre = null;
        String post = null;
        StringBuffer sb = null;
        if (preEnd != -1) { // we found sessUrlRewritePrefix, and/or "?" and/or "#"
            pre = uri.substring(0, preEnd);
            sb = new StringBuffer(pre);
            if (postStart != -1) {
                post = uri.substring(postStart, uri.length());
            }
        } else { // here we found no "?", no "#", and no sessURLRewritePrefix
            sb = new StringBuffer(uri);
        }

        sb.append(_smc.getSessUrlRewritePrefix());
        sb.append(sessionID);
        String cloneSeparatorPlusID = getSeparatorPlusAffinityToken(sessIdOnly);
        if (affinityContext != null) { //update the clones when the affinityContext was passed in
            String cloneInfo = updateCloneInfo(affinityContext, cloneSeparatorPlusID);
            // do a security check of affinity clone id for any malicious characters
            if (!isSafe(cloneInfo)) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[ENCODE_URL], "AffinityContext cloneID is not safe");
                }
                return uri;
            }            
            sb.append(cloneInfo);
        } else {
            // do a security check of clone id for any malicious characters
            if (!isSafe(cloneSeparatorPlusID)) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[ENCODE_URL], "CloneID in URL is not safe");
                }
                return uri;
            }            
            //affinityContext is null in the case when we're coming from a ConvergedHttpSession
            //only append the current clones (shouldn't matter since we're going to maintain affinity using the SIP application session id
            sb.append(cloneSeparatorPlusID);
        }
        if ((sb.length()) > SessionManagerConfig.getMaxSessionIdentifierLength()) { // cmd 159438
            if (LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.WARNING)) {
                String parms[] = { sb.toString(), new Integer(SessionManagerConfig.getMaxSessionIdentifierLength()).toString() };
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.WARNING, methodClassName, methodNames[ENCODE_URL], "SessionContext.maxSessionIdLengthExceeded", parms);
            }
        }

        if (postStart != -1)
            sb.append(post);
        uri = sb.toString();
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[ENCODE_URL], uri);
        }
        return uri;
    }

    /**
     * Method encodeURL
     * <p>
     * 
     * @param request
     * @param uri
     * @param affinityContext
     * @return String encoded URI
     * @see com.ibm.wsspi.session.ISessionAffinityManager#encodeURL(javax.servlet.ServletRequest, java.lang.String, SessionAffinityContext)
     */
    @Override
    public String encodeURL(ServletRequest request, String uri, SessionAffinityContext affinityContext) {
        return encodeURL(null, request, uri, affinityContext);
    }

    @Override
    public void setCookie(ServletRequest request, ServletResponse response, SessionAffinityContext affinityContext, Object session) {
        // LIBERTY IExtendedResponse is unreachable from here now.
        // overridden in SessionAffinityManagerImpl
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[SET_COOKIE], "");
        }
    }

    // setTheSipApplicationSessionCookie
    public void setSIPCookie(ServletRequest request, ServletResponse response, String sipCookieString) {
        // LIBERTY Cannot reach IExtendedResponse from here.
        // overridden in SessionAffinityManagerImpl
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[SET_SIP_COOKIE], "");
        }
    }

    // ----------------------------------------
    // Private methods
    // ----------------------------------------

    protected String getVersionString(int sessionVersion) {
        String sessionVersionString = _versionPrefix;
        if (_versionPrefixLength > 0) {
            if (sessionVersion > 0) {
                StringBuffer sb = new StringBuffer();
                sb.append(_versionPrefix).append(sessionVersion);
                sessionVersionString = sb.substring(sb.length() - _versionPrefixLength, sb.length());
            }
        } else {
            sessionVersionString = "";
        }
        return sessionVersionString;
    }

    /*
     * Method called to get the affinity token separator followed by the
     * affinity token. By default this token is a clone id, but it may
     * be overridden by the store.
     */
    protected String getSeparatorPlusAffinityToken(String sessId) {
        StringBuffer separatorPlusAffToken = new StringBuffer();
        String affToken = _store.getAffinityToken(sessId, _sessCtx.getAppName());
        if (affToken == null) { // store has not overridden token impl
            affToken = _cloneID;
        }
        if (affToken != null && affToken.length() > 0) {
            separatorPlusAffToken.append(SessionManagerConfig.getCloneSeparator()).append(affToken);
        }
        return separatorPlusAffToken.toString();
    }

    /*
     * Method to update (if necessary) and return the current clone info string.
     * Gets the clone info from _outputCloneInfo, if already set, or else the
     * _inputCloneInfo. If thisClone is not in the string, it either prepends
     * or appends it based on the isNoAffinitySwitchBack setting.
     */
    protected String updateCloneInfo(SessionAffinityContext sac, String thisClone) {
        final boolean isTraceOn = com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled();
        String cloneString;
        if (sac.isOutputCloneInfoSet()) {
            cloneString = sac.getOutputCloneInfo();
        } else {
            cloneString = sac.getInputCloneInfo();
        }
        // Begin PM02781: Compare if incoming id and outgoing id are different.  If so then we throw 
        // away past cloneInfo and just append thisCloneId as the new cloneInfo.
        String incomingSessId = sac.getRequestedSessionID();
        String outgoingSessId = sac.getResponseSessionID();
        if ((outgoingSessId != null) && !(outgoingSessId.equals(incomingSessId))) {
            sac.setInputCloneInfo("");
            cloneString = thisClone;
            if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE))
                LoggingUtil.SESSION_LOGGER_CORE.logp
                                (Level.FINE, "SessionAffinityManager", methodNames[UPDATE_CLONE_INFO],
                                 (new StringBuilder("Setting cloneID:")).append(cloneString).append(":").toString());
        }

        int index = cloneString.indexOf(thisClone);
        if (index == -1) // our clone isn't included
        {
            StringBuffer newClones = new StringBuffer();
            if (SessionManagerConfig.isNoAffinitySwitchBack()) {
                newClones.append(thisClone).append(cloneString);
            } else {
                newClones.append(cloneString).append(thisClone);
            }
            cloneString = newClones.toString();
            sac.setOutputCloneInfo(cloneString);
        } else {
            if (SessionManagerConfig.isNoAffinitySwitchBack() && index > 0) {
                cloneString = thisClone + cloneString.substring(0, index) + cloneString.substring(index + thisClone.length());
                if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE))
                    LoggingUtil.SESSION_LOGGER_CORE.logp
                                    (Level.FINE, "SessionAffinityManager", methodNames[UPDATE_CLONE_INFO],
                                     (new StringBuilder("New cloneString : ")).append(cloneString).append(".").toString());
            }
        }

        return cloneString;
    }

    /*
     * Method to get the requestedId. If the id is from a SSL request, we need to
     * get the actual SSL id as this id shouldn't
     * be stored on the request or Affinity Context
     * 
     * @see
     * com.ibm.wsspi.session.ISessionAffinityManager#getRequestSessionID(javax
     * .servlet.ServletRequest, com.ibm.wsspi.session.SessionAffinityContext)
     */
    @Override
    public String getInUseSessionID(ServletRequest req, SessionAffinityContext sac) {
        String id = null;
        if (sac.isRequestedSessionIDFromSSL() && req != null) {
            id = getActualSSLSessionId(req);
        } else {
            // We may have been dispatched and a previous app created a session.
            // If so, the Id will be in the response id, and we need to use it
            id = sac.getResponseSessionID();
            if (id == null) {
                id = sac.getRequestedSessionID();
            }
        }
        return id;
    }

    /*
     * Method to get the appropriate version to use for a new session. May be the
     * response session id
     * if the request has been dispatched adn the response version is already set
     */
    @Override
    public int getInUseSessionVersion(ServletRequest req, SessionAffinityContext sac) {
        int version = sac.getResponseSessionVersion();
        if (version == -1) { // not set, use request version
            version = sac.getRequestedSessionVersion();
        }
        return version;
    }

    /*
     * Method used to get at the SSL Id rather than the displayed Id
     */
    public String getActualSSLSessionId(ServletRequest request) {
        return null; // no SSL session for web common component
    }

    // analyze the SSL request
    public SessionAffinityContext analyzeSSLRequest(ServletRequest request, String sslSessionId) {
        return null; // no SSL sessions for web common component
    }

    public void setSessionId(HttpServletRequest _request, SessionAffinityContext sac) {
        // only for RRD - not supported on web common component
    }

    public void setResponseData(String idFromRequest, SessionAffinityContext sac) {
        // only for RRD - not supported in Web Common Component
    }

    // this is for the toHTML method in SessionContext
    public String getCloneId() {
        return _cloneID;
    }

    // XD methods
    @Override
    public String encodeURL(ServletRequest request, String url, SessionAffinityContext affinityContext, IProtocolAdapter adapter) {
        return null;
    }

    @Override
    public String getLocalCloneID() {
        return null;
    }

    @Override
    public void setCookie(ServletRequest request, ServletResponse response, SessionAffinityContext affinityContext, IProtocolAdapter adapter, Object session) {}

    @Override
    public void setCookieName(String cookieName) {}

    @Override
    public void setUseURLEncoding(boolean useURLEncoding) {}
    // end of XD methods
}
