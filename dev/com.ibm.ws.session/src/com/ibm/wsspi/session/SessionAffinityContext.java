/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.session;

import java.util.List;

/**
 * An instance of this class is associated with each incoming request. The
 * Session Manager
 * populates its contents when parsing the incoming request and then later uses
 * this object to
 * then access affinity related information regarding this request.
 * 
 */
public class SessionAffinityContext {

    // Dummy placeholder id for SSL sessions
    public static final String SSLSessionId = "SESSIONMANAGEMENTAFFINI";

    // ----------------------------------------
    // Private Members
    // ----------------------------------------
    /**
     * boolean member whose value is set to
     * true if the requested session id is in the cookie. Its value
     * is set to false if the requested session id is part of
     * a rewritten URL or in the absence of a JSESSION id
     * cookie.
     */
    private boolean _reqFromCookie = false;

    /**
     * String member whose value is set by the rebalancer filter
     * to effect a redirect of this request to another server, by
     * influencing the value of the cloneid in the outgoing cookie.
     */
    private String _redirectCloneId = null;

    /**
     * boolean member whose value is set to true
     * if the requested session id is part of a
     * rewritten URL. Note that this value <i>may</i> be set
     * to false even if this is a URL rewrite due to the exact
     * text of the rewritten URL being inaccessible from the
     * HTTPRequest object.
     */
    private boolean _reqFromURL = false;

    /**
     * boolean member whose value is set to true
     * if the requested session id is part of a
     * SSL Request and the SessionManager is configured to use SSL Sessions.
     */
    private boolean _reqFromSSL = false;
    
    
    /**
     * boolean member whose value is set to true
     * if the requested session id is part of a
     * request from a clinet but using whichever of cookie
     * or URL rewriting which is not configured.
     */
    private boolean _reqFromClient = false;

    /**
     * Requested session id as perceived by the
     * Affinity context from the incoming message
     */
    private String _requestedSessionID = null;
    /**
     * First requested session id as perceived by the
     * Affinity context from the incoming message.
     * This is returned by getRequestedSessionId if we
     * get multiple id's and none of them are valid.
     */
    private String _firstRequestedSessionId = null;
    /**
     * The number of sessionIds that come in on request.
     * Normally this will be 0 or 1. But since there may
     * situations where multiple session cookies arrive
     * due to differences in cookie path or domain.
     */
    private int _numSessIds = 0;
    /**
     * Variable which contains a list of all SessionIds for the appropriate name.
     * ie. There can be multiple cookies stored with the same name and it is
     * required to
     * make sure none of them hold the information we are looking for
     */
    private List _allSessionIds = null;

    private boolean _allSessionIdsSetViaSet = false;
    /**
     * Variable which holds the input Clone Information
     */
    private String _inputCloneInfo = "";

    /**
     * Variable which holds the output Clone Information
     * This is set if we change the input clone information
     */
    private String _outputCloneInfo = null;
    /**
     * Requested session version as perceived by the
     * Affinity context from the incoming message
     */
    private int _requestedSessionVersion = 0;

    /**
     * Session id that is to be set on the outgoing
     * response message
     */
    private String _responseSessionID = null;

    /**
     * Session version that is to be set on the outgoing
     * response message.
     */
    private int _responseSessionVersion = -1;

    // if a cookie has already been set for this SAC
    private boolean sessionCookieSet = false;
    
    //PM89885
    private boolean firstSessionIdValid = true;

    // ----------------------------------------
    // Class Constructor
    // ----------------------------------------
    /**
     * Class constructor
     * 
     * @param boolean reqFromCookie
     * @param boolean reqFromURL
     * @param String
     *            reqSessionID
     * @param int reqSessionVersion
     */
    public SessionAffinityContext(boolean reqFromCookie, boolean reqFromURL, String reqSessionID, int reqSessionVersion) {
        _reqFromCookie = reqFromCookie;
        _reqFromURL = reqFromURL;
        _requestedSessionID = reqSessionID;
        _requestedSessionVersion = reqSessionVersion;
        if (reqSessionID != null) {
            _firstRequestedSessionId = reqSessionID;
            _numSessIds = 1;
        }

    }

    /**
     * Class constructor
     * 
     * @param List
     *            allSessionIds
     * @param boolean reqFromCookie
     * @param boolean reqFromURL
     * @param boolean reqFromSSL
     */
    public SessionAffinityContext(List allSessionIds, boolean reqFromCookie, boolean reqFromURL, boolean reqFromSSL) {
        _allSessionIds = allSessionIds;
        _reqFromCookie = reqFromCookie;
        _reqFromURL = reqFromURL;
        _reqFromSSL = reqFromSSL;
        if ((allSessionIds != null) && (!allSessionIds.isEmpty())) {
            _firstRequestedSessionId = (String) allSessionIds.get(0);
            _numSessIds = allSessionIds.size();
        }
    }
    
    public SessionAffinityContext(List allSessionIds, boolean reqFromCookie, boolean reqFromURL, boolean reqFromSSL, boolean reqFromClient) {
        _allSessionIds = allSessionIds;
        _reqFromCookie = reqFromCookie;
        _reqFromURL = reqFromURL;
        _reqFromSSL = reqFromSSL;
        _reqFromClient = reqFromClient;
        if ((allSessionIds != null) && (!allSessionIds.isEmpty())) {
            _firstRequestedSessionId = (String) allSessionIds.get(0);
            _numSessIds = allSessionIds.size();
        }
        if (_reqFromClient) {
            setFirstSessionIdValid(false);
        }
    }

    // ----------------------------------------
    // Public Methods
    // ----------------------------------------

    /**
     * Accessor for the _reqFromCookie
     * <p>
     * 
     * @return boolean true, if a JSESSIONID cookie is encountered.
     */
    public final boolean isRequestedSessionIDFromCookie() {
        return _reqFromCookie;
    }

    /**
     * Accessor for the _reqFromURL
     * <p>
     * 
     * @return boolean
     */
    public final boolean isRequestedSessionIDFromURL() {
        return _reqFromURL;
    }

    /**
     * Accessor for the _reqFromSSL
     * <p>
     * 
     * @return boolean
     */
    public final boolean isRequestedSessionIDFromSSL() {
        return _reqFromSSL;
    }

    /**
     * Accessor for the _requestedSessionID
     * <p>
     * 
     * @return String
     */
    public final String getRequestedSessionID() {
        return _requestedSessionID;
    }

    /**
     * Accessor for the _firstRequestedSessionId
     * <p>
     * 
     * @return String
     */
    public final String getFirstRequestedSessionID() {
        return _firstRequestedSessionId;
    }

    /**
     * Accessor for the _numSessIds
     * <p>
     * 
     * @return int
     */
    public final int getNumSessionIds() {
        return _numSessIds;
    }

    /**
     * Mutator for the _requestSessionID
     * <p>
     * 
     * @param String
     *            sessionID
     */
    public final void setRequestedSessionID(String id) {
        _requestedSessionID = id;
    }

    /**
     * Accessor for the _inputCloneInfo
     * <p>
     * 
     * @return String
     */
    public final String getInputCloneInfo() {
        return _inputCloneInfo;
    }

    /**
     * Mutator for the _inputCloneInfo
     * <p>
     * 
     * @param String
     *            cloneInfo
     */
    public final void setInputCloneInfo(String cloneInfo) {
        _inputCloneInfo = cloneInfo;
    }

    /**
     * Mutator for the _outputCloneInfo
     * <p>
     * 
     * @param String
     *            cloneInfo
     */
    public final void setOutputCloneInfo(String outputCloneInfo) {
        _outputCloneInfo = outputCloneInfo;
    }

    /**
     * Accessor for the _outputCloneInfo
     * <p>
     * 
     * @return String
     */
    public final String getOutputCloneInfo() {
        return _outputCloneInfo;
    }

    /**
     * Accessor for the _allSessionIds
     * <p>
     * 
     * @return List
     */
    public final List getAllSessionIds() {
        return _allSessionIds;
    }

    /**
     * Accessor for the _requestedSessionVersion
     * <p>
     * 
     * @return int
     */
    public final int getRequestedSessionVersion() {
        return _requestedSessionVersion;
    }

    /**
     * Mutator for the _requestedSessionVersion
     * <p>
     * 
     * @param int version
     */
    public final void setRequestedVersion(int version) {
        _requestedSessionVersion = version;
    }

    /**
     * Accessor for the _responseSessionID
     * <p>
     * 
     * @return String sessionID
     */
    public final String getResponseSessionID() {
        return _responseSessionID;
    }

    /**
     * Mutator for the _responseSessionID
     * <p>
     * 
     * @param String
     *            sessionID
     */
    public void setResponseSessionID(String sessionID) {
        if (this.isRequestedSessionIDFromSSL()) {
            // sets the responseId to the dummy SSL id
            _responseSessionID = SessionAffinityContext.SSLSessionId;
        } else {
            _responseSessionID = sessionID;
        }
    }

    /**
     * Accessor for the _responseSessionVersion
     * <p>
     * 
     * @return int version
     */
    public final int getResponseSessionVersion() {
        return _responseSessionVersion;
    }

    /**
     * Mutator for the _responseSessionVersion
     * <p>
     * 
     * @param version
     */
    public final void setResponseSessionVersion(int version) {
        _responseSessionVersion = version;
    }

    /**
     * Mutator for the _redirectCloneID
     * 
     * @param String
     *            cloneId
     */
    public final void setRedirectCloneID(String cloneId) {
        _redirectCloneId = cloneId;
    }

    /**
     * Accessor for the _redirectCloneID
     * 
     * @return String redirectCloneID
     */
    public final String getRedirectCloneID() {
        return _redirectCloneId;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("# SessionAffinityContext # \n { ").
                        append("\n _reqFromCookie=").append(this._reqFromCookie).
                        append("\n _reqFromURL=").append(this._reqFromURL).
                        append("\n _requestedSessionID=").append(this._requestedSessionID).
                        append("\n _requestedSessionVersion=").append(this._requestedSessionVersion).
                        append("\n _redirectCloneId=").append(this._redirectCloneId).
                        append("\n _reqFromSSL=").append(this._reqFromSSL).
                        append("\n _allSessionIds=").append(this._allSessionIds).
                        append("\n _inputCloneInfo=").append(this._inputCloneInfo).
                        append("\n _responseSessionVersion=").append(this._responseSessionVersion).
                        append("\n _responseSessionID=").append(this._responseSessionID).
                        append("\n _outputCloneInfo=").append(this._outputCloneInfo).
                        append("\n } \n");
        return sb.toString();
    }

    /**
     * Determines if the response session id has been set
     * 
     * @return boolean
     */
    public final boolean isResponseIdSet() {
        return (_responseSessionID != null);
    }

    /**
     * Determines if the requested session id has been set
     * 
     * @return boolean
     */
    public final boolean isRequestedIdSet() {
        return (_requestedSessionID != null);
    }

    public final boolean isAllSessionIdsSetViaSet() {
        return _allSessionIdsSetViaSet;
    }

    /**
     * Determines if the output clone info has been set. This is set
     * if we modify the incoming clone ifo
     * 
     * @return boolean
     */
    public final boolean isOutputCloneInfoSet() {
        return (_outputCloneInfo != null);
    }

    public final void setAllSessionIds(List allSessionIds) {
        // if we are calling this, that means the first session id didn't
        // match and we need to parse through all of the Ids from getAllCookieValues
        // - the first one in the list should already have been checked, so skip it
        if (allSessionIds != null && allSessionIds.size() > 0) {
            //if we got here, then allSessionIds should not be null and should be non empty
            _allSessionIds = allSessionIds;
            //REG don't want to change the _firstRequestedSessionId! and want to keep the numSessIds to the complete size
            _numSessIds = allSessionIds.size();
            // if we got here, then _firstRequestedSessionId should not be null either
            if (_firstRequestedSessionId.equals((String) allSessionIds.get(0))) {
                allSessionIds.remove(0);
            }
        }
        _allSessionIdsSetViaSet = true;
    }

    public boolean isSessionCookieSet() {
        return sessionCookieSet;
    }

    public void setSessionCookieSet(boolean sessionCookieSet) {
        this.sessionCookieSet = sessionCookieSet;
    }
    
    //PM89885
    public void setFirstSessionIdValid(boolean firstSessionIdValid) {
        this.firstSessionIdValid = firstSessionIdValid;
    }
    public boolean isFirstSessionIdValid() {
        return firstSessionIdValid;
    }

}