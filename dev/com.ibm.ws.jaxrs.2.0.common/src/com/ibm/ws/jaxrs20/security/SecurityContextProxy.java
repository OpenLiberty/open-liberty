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
package com.ibm.ws.jaxrs20.security;

import java.security.Principal;

import org.apache.cxf.security.SecurityContext;

class SecurityContextProxy implements SecurityContext {
    
    private final javax.ws.rs.core.SecurityContext jaxrsSecurityContext;
    private final SecurityContext cxfSecurityContext;
    
    SecurityContextProxy(javax.ws.rs.core.SecurityContext jaxrsSecurityContext) {
        this.jaxrsSecurityContext = jaxrsSecurityContext;
        this.cxfSecurityContext = null;
        
        if (jaxrsSecurityContext == null) {
            throw new IllegalArgumentException();
        }
    }
    
    SecurityContextProxy(SecurityContext cxfSecurityContext) {
        this.jaxrsSecurityContext = null;
        this.cxfSecurityContext = cxfSecurityContext;
        
        if (cxfSecurityContext == null) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public Principal getUserPrincipal() {
        return jaxrsSecurityContext != null ? jaxrsSecurityContext.getUserPrincipal() : cxfSecurityContext.getUserPrincipal();
    }

    @Override
    public boolean isUserInRole(String role) {
        return jaxrsSecurityContext != null ? jaxrsSecurityContext.isUserInRole(role) : cxfSecurityContext.isUserInRole(role);
    }
}
