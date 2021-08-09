/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security;

import java.util.ArrayList;
import java.util.List;

import javax.security.auth.Subject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * An AuthenticationResult is returned by WebAuthenticator as a result of a
 * authenticate request.
 * The result encapsulates the status:
 * success or failure based on authentication data
 * need authentication data to make the above determination
 * On success, the result will also contain all credential related information
 * so that it can be used to set the security context or even to set single
 * sign-on headers.
 */
public class AuthenticationResult {
    private final AuthResult status;
    private int taiChallengeCode = HttpServletResponse.SC_UNAUTHORIZED; // default to 401
    // These fields intentionally left default
    public String realm = null;
    public String username = null;
    public String password = null;
    public String certdn = null;
    private Subject subject = null;
    private String targetRealm = null;

    public boolean passwordExpired = false;
    /**
     * @return the passwordExpired
     */
    @Trivial
    public boolean getPasswordExpired() {
        return passwordExpired;
    }

    /**
     * @param passwordExpired the passwordExpired to set
     */
    @Trivial
    public void setPasswordExpired(boolean passwordExpired) {
        this.passwordExpired = passwordExpired;
    }

    /**
     * @return the userRevoked
     */
    @Trivial
    public boolean getUserRevoked() {
        return userRevoked;
    }

    /**
     * @param userRevoked the userRevoked to set
     */
    @Trivial
    public void setUserRevoked(boolean userRevoked) {
        this.userRevoked = userRevoked;
    }

    public boolean userRevoked = false;

    // Credentials-related variables
    private String reason;
    private String redirectURL;
    private final List<Cookie> cookieList = new ArrayList<Cookie>();
    private String auditCredType = null;
    private String auditCredValue = null;
    private String auditOutcome = null;
    private Subject auditLogoutSubject = null;
    private String auditAuthConfigProviderName = null;
    private String auditAuthConfigProviderAuthType = null;

    public AuthenticationResult(AuthResult status, Subject _subject) {
        this.status = status;
        subject = _subject;
    }

    public AuthenticationResult(AuthResult status,
                                Subject _subject,
                                String credType,
                                String credValue,
                                String outcome) {
        this.status = status;
        subject = _subject;
        this.setAuditData(credType, credValue, outcome);
    }

    public AuthenticationResult(AuthResult status,
                                String str,
                                String credType,
                                String credValue,
                                String outcome) {
        this(status, str);
        this.setAuditData(credType, credValue, outcome);
    }

    public AuthenticationResult(AuthResult status, String str) {
        this.status = status;
        subject = null;
        switch (status) {
            case FAILURE:
                reason = str;
                break;
            case SEND_401:
                realm = str;
                break;
            case REDIRECT:
                redirectURL = str;
                break;
            case RETURN:
            case OAUTH_CHALLENGE:
                reason = str;
                break;
            default:
                break;
        }
    }

    /**
     * @param taiChallenge
     * @param status
     */
    public AuthenticationResult(AuthResult status, String str, int taiChallengeCode) {
        this(status, str);
        this.taiChallengeCode = taiChallengeCode;
    }

    public int getTAIChallengeCode() {
        return taiChallengeCode;
    }

    public AuthResult getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public Subject getSubject() {
        return subject;
    }

    public String getRealm() {
        if (realm != null) {
            return realm;
        } else {
            return "DEFAULT";
        }
    }

    public String getUserName() {
        if (username != null) {
            return username;
        } else {
            return null;
        }
    }

    public String getCertificateDN() {
        return certdn;
    }

    public String getRedirectURL() {
        return redirectURL;
    }

    public void clearCookieList() {
        cookieList.clear();
    }

    public void setCookie(Cookie cookie) {
        cookieList.add(cookie);
    }

    public List<Cookie> getCookies() {
        return cookieList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuffer str = new StringBuffer("AuthenticationResult status=");
        str.append(status.toString());
        switch (status) {
            case FAILURE:
                str.append(" reason=");
                str.append(reason);
                break;
            case SEND_401:
                str.append(" realm=");
                str.append(realm);
                break;
            case REDIRECT:
                str.append(" redirectURL=");
                str.append(redirectURL);
                break;
            case RETURN:
            case OAUTH_CHALLENGE:
                str.append(" reason=");
                str.append(reason);
                break;
            default:
                break;
        }
        return str.toString();
    }

    /**
     * @param type
     */
    @Trivial
    public void setAuditCredType(String type) {
        auditCredType = type;
    }

    /**
     *
     * @return
     */
    @Trivial
    public String getAuditCredType() {
        return auditCredType;
    }

    /**
     * @param type
     */
    public void setTargetRealm(String value) {
        targetRealm = value;
    }

    /**
     *
     * @return
     */
    public String getTargetRealm() {
        return targetRealm;
    }

    /**
     * @param type
     */
    @Trivial
    public void setAuditCredValue(String value) {
        auditCredValue = value;
    }

    /**
     * @ return
     */
    @Trivial
    public String getAuditAuthConfigProviderName() {
        return auditAuthConfigProviderName;
    }

    /**
     * @param type
     */
    @Trivial
    public void setAuditAuthConfigProviderName(String name) {
        auditAuthConfigProviderName = name;
    }

    /**
     * @ return
     */
    @Trivial
    public String getAuditAuthConfigProviderAuthType() {
        return auditAuthConfigProviderAuthType;
    }

    /**
     * @param type
     */
    @Trivial
    public void setAuditAuthConfigProviderAuthType(String authType) {
        auditAuthConfigProviderAuthType = authType;
    }

    /**
     *
     * @return
     */
    @Trivial
    public String getAuditCredValue() {
        return auditCredValue;
    }

    /**
     * @param type
     */
    @Trivial
    public void setAuditOutcome(String outcome) {
        auditOutcome = outcome;
    }

    /**
     *
     * @return
     */
    @Trivial
    public String getAuditOutcome() {
        return auditOutcome;
    }

    /**
     *
     */
    @Trivial
    public void setAuditData(String credType, String credValue, String outcome) {
        auditCredType = credType;
        auditCredValue = credValue;
        auditOutcome = outcome;
    }

    /**
     *
     * @return
     */
    @Trivial
    public void setAuditLogoutSubject(Subject subject) {
        auditLogoutSubject = subject;
    }

    /**
     *
     * @ return Subject
     */
    @Trivial
    public Subject getAuditLogoutSubject() {
        return auditLogoutSubject;
    }

}
