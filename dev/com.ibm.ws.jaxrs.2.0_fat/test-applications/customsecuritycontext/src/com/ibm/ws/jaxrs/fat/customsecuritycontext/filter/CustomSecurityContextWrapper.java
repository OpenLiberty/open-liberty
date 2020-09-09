/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.customsecuritycontext.filter;

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;

public class CustomSecurityContextWrapper implements SecurityContext {

    private SecurityContext securityContext = null;

    public CustomSecurityContextWrapper(SecurityContext sc) {
        securityContext = sc;
    }

    @Override
    public String getAuthenticationScheme() {
        System.out.println("CustomSecurityContextWrapper.getAuthenticationScheme()");
        return securityContext.getAuthenticationScheme();
    }

    @Override
    public Principal getUserPrincipal() {
        System.out.println("CustomSecurityContextWrapper.getUserPrincipal()");

        return new Principal() {

            @Override
            public String getName() {
                return "Bob";
            }
        };
    }

    @Override
    public boolean isSecure() {
        System.out.println("CustomSecurityContextWrapper.isSecure()");
        return securityContext.isSecure();
    }

    @Override
    public boolean isUserInRole(String role) {
        System.out.println("CustomSecurityContextWrapper.isSecure()");
        return true;
    }

}
