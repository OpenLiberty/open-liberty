/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.security.authorization.jacc;

public class RoleInfo {
    private String roleName;
    private boolean isDenyAll;
    private boolean isPermitAll;

    public static RoleInfo DENY_ALL = new RoleInfo(null, true, false);
    public static RoleInfo PERMIT_ALL = new RoleInfo(null, false, true);

    private RoleInfo(String roleName, boolean isDenyAll, boolean isPermitAll) {
        this.roleName = roleName;
        this.isDenyAll = isDenyAll;
        this.isPermitAll = isPermitAll;
    }

    public RoleInfo(String roleName) {
        this.roleName = roleName;
        this.isDenyAll = false;
        this.isPermitAll = false;
    }

    public RoleInfo() {
        roleName = null;
        isDenyAll = false;
        isPermitAll = false;
    }
    
    public void setDenyAll() {
        roleName = null;
        isDenyAll = true;
        isPermitAll = false;
    }
    public void setPermitAll() {
        roleName = null;
        isDenyAll = false;
        isPermitAll = true;
    }

    public String getRoleName() {
        return roleName;
    }

    public boolean isDenyAll() {
        return isDenyAll;
    }

    public boolean isPermitAll() {
        return isPermitAll;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("role : " ).append(roleName).append(" DenyAll : ").append(isDenyAll).append(" PermitAll : ").append(isPermitAll);
        return buf.toString();
    }

}
