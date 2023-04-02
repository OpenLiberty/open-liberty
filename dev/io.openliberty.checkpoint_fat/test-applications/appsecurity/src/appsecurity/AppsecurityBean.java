/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package appsecurity;

import java.io.Serializable;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.security.enterprise.SecurityContext;

@RequestScoped
public class AppsecurityBean implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String ADMIN = "admin";
    public static final String USER = "user";

    @Inject
    private SecurityContext securityContext;

    public String getUsername() {
        return securityContext.getCallerPrincipal().getName();
    }

    public String getRoles() {
        String roles = "";
        if (securityContext.isCallerInRole(ADMIN)) {
            roles = ADMIN;
        }
        if (securityContext.isCallerInRole(USER)) {
            if (!roles.isEmpty()) {
                roles += ", ";
            }
            roles += USER;
        }
        return roles;
    }
}