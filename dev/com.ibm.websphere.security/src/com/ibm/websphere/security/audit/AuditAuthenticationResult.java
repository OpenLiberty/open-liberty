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
package com.ibm.websphere.security.audit;

import javax.security.auth.Subject;

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
public class AuditAuthenticationResult {
    private final AuditAuthResult status;
    private int taiChallengeCode = 401;
    // These fields intentionally left default
    public String realm = null;
    public String username = null;
    public String password = null;
    public String certdn = null;
    private Subject subject = null;
    private String targetRealm = null;

    // Credentials-related variables
    private String reason;
    private String redirectURL;
    private String auditCredType = null;
    private String auditCredValue = null;
    private String auditOutcome = null;
    private Subject auditLogoutSubject = null;
    private String auditAuthConfigProviderName = null;

    public AuditAuthenticationResult(AuditAuthResult status, Subject _subject) {
        this.status = status;
        subject = _subject;
    }

    public AuditAuthenticationResult(AuditAuthResult status,
                                     Subject _subject,
                                     String credType,
                                     String credValue,
                                     String outcome) {
        this.status = status;
        subject = _subject;
        this.setAuditData(credType, credValue, outcome);
    }

    public AuditAuthenticationResult(AuditAuthResult status,
                                     String str,
                                     String credType,
                                     String credValue,
                                     String outcome) {
        this(status, str);
        this.setAuditData(credType, credValue, outcome);
    }

    public AuditAuthenticationResult(AuditAuthResult status, String str) {
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
    public AuditAuthenticationResult(AuditAuthResult status, String str, int taiChallengeCode) {
        this(status, str);
        this.taiChallengeCode = taiChallengeCode;
    }

    public int getTAIChallengeCode() {
        return taiChallengeCode;
    }

    public AuditAuthResult getStatus() {
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
