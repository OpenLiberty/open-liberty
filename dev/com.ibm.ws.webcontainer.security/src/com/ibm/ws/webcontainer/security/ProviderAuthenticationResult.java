/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security;

import java.util.Hashtable;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.annotation.Sensitive;

/**
 * Simple wrapper for provider authentication result
 */

public class ProviderAuthenticationResult {
    private final AuthResult status;
    private final int httpStatusCode;
    private final String userName;
    private final Subject subject;
    private final String redirectUrl;
    private final Hashtable<String, Object> customProperties;

    public ProviderAuthenticationResult(AuthResult status,
                                        int httpStatusCode) {
        this.status = status;
        this.httpStatusCode = httpStatusCode;
        this.userName = null;
        this.subject = null;
        this.redirectUrl = null;
        this.customProperties = null;
    }

    public ProviderAuthenticationResult(AuthResult status,
                                        int httpStatusCode,
                                        String userName,
                                        Subject subject,
                                        Hashtable<String, Object> customProperties,
                                        @Sensitive String redirectUrl) {
        this.status = status;
        this.httpStatusCode = httpStatusCode;
        this.userName = userName;
        this.subject = subject;
        this.redirectUrl = redirectUrl;
        this.customProperties = customProperties;
    }

    public AuthResult getStatus() {
        return status;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public String getUserName() {
        return userName;
    }

    public Subject getSubject() {
        return subject;
    }

    public Hashtable<String, Object> getCustomProperties() {
        return customProperties;
    }

    @Sensitive
    public String getRedirectUrl() {
        return redirectUrl;
    }

}
