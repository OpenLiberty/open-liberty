/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.fat.common.social.apps.formlogin;

import java.util.Map.Entry;
import java.util.NoSuchElementException;

import javax.security.auth.Subject;

import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.websphere.security.social.UserProfile;

/**
 * Social login-specific base servlet to help print values pertaining to social login
 */
public abstract class BaseServlet extends com.ibm.ws.security.fat.common.apps.formlogin.BaseServlet {
    private static final long serialVersionUID = 1L;

    public static final String OUTPUT_PREFIX = "UserInfo: ";

    protected BaseServlet(String servletName) {
        super(servletName);
    }

    @Override
    protected void printPrivateCredentials(Subject callerSubject, StringBuffer sb) {
        printJwtCredential(callerSubject, sb);
        printUserProfileCredential(callerSubject, sb);
    }

    private void printJwtCredential(Subject callerSubject, StringBuffer sb) {

        Claims jwtClaims = null;
        try {
            JwtToken jwtCredential = callerSubject.getPrivateCredentials(JwtToken.class).iterator().next();
            jwtClaims = jwtCredential.getClaims();
        } catch (NoSuchElementException e) {
            writeLine(sb, "NoSuchElementException for JwtToken: " + e);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        writeLine(sb, "JWT Claims: " + jwtClaims);
    }

    private void printUserProfileCredential(Subject callerSubject, StringBuffer sb) {

        UserProfile userProfileCredential = null;
        try {
            userProfileCredential = callerSubject.getPrivateCredentials(UserProfile.class).iterator().next();
            if (userProfileCredential == null) {
                writeLine(sb, "UserInfo: null");
                return;
            }
        } catch (NoSuchElementException e) {
            writeLine(sb, "NoSuchElementException for UserInfo: " + e);
            writeLine(sb, "UserInfo: null");
            return;
        } catch (Throwable t) {
            t.printStackTrace();
            writeLine(sb, "UserInfo: null");
            return;
        }

        String userInfo = userProfileCredential.getUserInfo();
        writeLine(sb, OUTPUT_PREFIX + "string: " + userInfo);
        String accessToken = userProfileCredential.getAccessToken();
        writeLine(sb, OUTPUT_PREFIX + "accessToken: " + accessToken);
        Claims claims = userProfileCredential.getClaims();
        printUserProfileClaims(sb, claims);
        JwtToken idToken = userProfileCredential.getIdToken();
        writeLine(sb, OUTPUT_PREFIX + "ID token: " + (idToken == null ? null : idToken.compact()) + " ");
        String refreshToken = userProfileCredential.getRefreshToken();
        writeLine(sb, OUTPUT_PREFIX + "refresh_token: " + refreshToken + " ");
        String scopes = userProfileCredential.getScopes();
        writeLine(sb, OUTPUT_PREFIX + "scopes: " + scopes);
        String socialMediaName = userProfileCredential.getSocialMediaName();
        writeLine(sb, OUTPUT_PREFIX + "socialMediaName: " + socialMediaName);
    }

    private void printUserProfileClaims(StringBuffer sb, Claims claims) {
        writeLine(sb, OUTPUT_PREFIX + "claims: " + claims);
        if (claims == null) {
            return;
        }
        for (Entry<String, Object> claim : claims.entrySet()) {
            writeLine(sb, OUTPUT_PREFIX + "claims: name: " + claim.getKey() + " value: " + claim.getValue());
        }
    }

}
