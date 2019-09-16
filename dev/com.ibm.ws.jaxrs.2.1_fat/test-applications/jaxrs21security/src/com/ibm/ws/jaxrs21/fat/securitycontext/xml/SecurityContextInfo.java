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
package com.ibm.ws.jaxrs21.fat.securitycontext.xml;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SecurityContextInfo {

    private String authScheme;
    private String userPrincipal;
    private boolean isSecure;
    private boolean isUserInRoleAdmin;
    private boolean isUserInRoleNull;
    private boolean isUserInRoleUser;

    public String getAuthScheme() {
        return authScheme;
    }

    public void setAuthScheme(String authScheme) {
        this.authScheme = authScheme;
    }

    public String getUserPrincipal() {
        return userPrincipal;
    }

    public void setUserPrincipal(String userPrincipal) {
        this.userPrincipal = userPrincipal;
    }

    public boolean isSecure() {
        return isSecure;
    }

    public void setSecure(boolean isSecure) {
        this.isSecure = isSecure;
    }

    public boolean isUserInRoleAdmin() {
        return isUserInRoleAdmin;
    }

    public void setUserInRoleAdmin(boolean isUserInRoleAdmin) {
        this.isUserInRoleAdmin = isUserInRoleAdmin;
    }

    public boolean isUserInRoleNull() {
        return isUserInRoleNull;
    }

    public void setUserInRoleNull(boolean isUserInRoleNull) {
        this.isUserInRoleNull = isUserInRoleNull;
    }

    public boolean isUserInRoleUser() {
        return isUserInRoleUser;
    }

    public void setUserInRoleUser(boolean isUserInRoleUser) {
        this.isUserInRoleUser = isUserInRoleUser;
    }

}
