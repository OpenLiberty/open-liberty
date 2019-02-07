/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.fat.common.jwtsso.apps.formlogin;

import java.security.Principal;
import java.util.Set;

import javax.security.auth.Subject;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.ibm.websphere.security.auth.WSSubject;

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
        Subject subject = getRunAsSubject();
        if (subject == null) {
            writeLine(sb, "subject is null");
            return;
        }
        printJwtCookieName(subject, sb);
        printJsonWebTokenPrincipal(subject, sb);
    }

    public Subject getRunAsSubject() {
        Subject subject = null;
        try {
            subject = WSSubject.getRunAsSubject();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return subject;
    }

    public void printJwtCookieName(Subject subject, StringBuffer sb) {

        try {
            writeLine(sb, "JWT cookie name: " + com.ibm.websphere.security.web.WebSecurityHelper.getJwtCookieName());
        } catch (Exception e) {
            writeLine(sb, "JWT cookie name: " + e.getMessage());
        }
    }

    public void printJsonWebTokenPrincipal(Subject subject, StringBuffer sb) {

        try {
            Set<JsonWebToken> jsonWebTokenPrincipal = subject.getPrincipals(JsonWebToken.class);

            if (!jsonWebTokenPrincipal.isEmpty()) {
                Principal principal = jsonWebTokenPrincipal.iterator().next();
                writeLine(sb, "JWT principal: " + principal);
            }
        } catch (NoClassDefFoundError e) {
            writeLine(sb, "JWT principal: " + e.getMessage());
        }

    }

}
