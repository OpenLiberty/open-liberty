/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.client.security.fat;

import java.security.Principal;
import java.security.PrivilegedAction;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import com.ibm.websphere.security.auth.WSSubject;

/**
 * A managed bean for our test app. It needs to be a bean so we can inject the Principal into it.
 */
@ApplicationScoped
public class AppBean {

    @Inject
    private Principal principal;

    public void run() {
        System.out.println("Logging in...");

        try {
            // When we do this login call, getting the username and password
            // should be delegated to our AppCallbackHandler class.
            LoginContext ctx = new LoginContext("ClientContainer");
            ctx.login();

            System.out.println("Logged in");
            Subject subject = ctx.getSubject();

            System.out.println("Logged in subject: " + subject);
            for (Principal p : subject.getPrincipals()) {
                System.out.println("Principal: " + p.getName());
            }

            // Within the privileged action block, the injected principal should be
            // the logged in user.
            WSSubject.doAs(subject, new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    System.out.println("Testing injected principal:");
                    System.out.println("Injected principal: " + principal.getName());
                    return null;
                }
            }, true);

        } catch (LoginException e) {
            System.out.println("Login failed");
            e.printStackTrace();
        }
    }
}
