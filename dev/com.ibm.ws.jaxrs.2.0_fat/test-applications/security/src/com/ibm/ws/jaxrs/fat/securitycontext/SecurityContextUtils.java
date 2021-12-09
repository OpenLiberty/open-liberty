/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.securitycontext;

import javax.ws.rs.core.SecurityContext;

import com.ibm.ws.jaxrs.fat.securitycontext.xml.SecurityContextInfo;

final public class SecurityContextUtils {

    public static SecurityContextInfo securityContextToJSON(SecurityContext secContext) {
        if (secContext == null) {
            System.out.println("SecurityContextUtils.securityContextToJSON return null");
            return null;
        }
        SecurityContextInfo secInfo = new SecurityContextInfo();
        secInfo.setAuthScheme(secContext.getAuthenticationScheme());
        secInfo.setUserPrincipal(secContext.getUserPrincipal() == null ? "null" : secContext
                        .getUserPrincipal().getName());
        secInfo.setSecure(secContext.isSecure());
        secInfo.setUserInRoleAdmin(secContext.isUserInRole("admin"));
        secInfo.setUserInRoleNull(secContext.isUserInRole(null));
        secInfo.setUserInRoleUser(secContext.isUserInRole("user"));
        System.out.println("SecurityContextUtils.securityContextToJSON return secInfo " + secInfo);
        return secInfo;
    }
}
