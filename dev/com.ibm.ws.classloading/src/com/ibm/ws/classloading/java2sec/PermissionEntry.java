/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.java2sec;


/**
 *
 */
public class PermissionEntry {
    
    public String permissionType;
    public String name;
    public String action;
    public String signedBy;
    
    final static String NEW_LINE = System.getProperty("line.separator");
    final static String QUOTED_STRING = "quoted string";
    final static String PERMISSION_TYPE = "permission type";
    final static String GRANT_KEYWORD = "grant";
    final static String KEYSTORE_KEYWORD = "keystore";
    final static String CODEBASE_KEYWORD = "codeBase";
    final static String PERMISSION_KEYWORD = "permission";
    final static String SIGNEDBY_KEYWORD = "signedBy";
    final static String FILTER_KEYWORD = "filterMask";


    public PermissionEntry() {
    }

    public PermissionEntry(String p, String n, String a, String s) {
        permissionType = p;
        name = n;
        action = a;
        signedBy = s;
    }

    public int hashCode() {
        int i = permissionType.hashCode();
        if (name != null) {
            i ^= name.hashCode();
        }
        if (action != null) {
            i ^= action.hashCode();
        }
        return i;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof PermissionEntry)) {
            return false;
        }

        PermissionEntry other = (PermissionEntry) obj;

        if (permissionType == null) {
            if (other.permissionType != null) {
                return false;
            }
        } else {
            if (!permissionType.equals(other.permissionType)) {
                return false;
            }
        }

        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else {
            if (!name.equals(other.name)) {
                return false;
            }
        }

        if (action == null) {
            if (other.action != null) {
                return false;
            }
        } else {
            if (!action.equals(other.action)) {
                return false;
            }
        }

        if (signedBy == null) {
            if (other.signedBy != null) {
                return false;
            }
        } else {
            if (!signedBy.equals(other.signedBy)) {
                return false;
            }
        }

        return true;
    }

    public String toString() {
        if (permissionType == null) {
            return "";
        }

        StringBuffer buf = new StringBuffer("  ");
        buf.append(PERMISSION_KEYWORD).append(' ').append(permissionType);
        if (name != null) {
            buf.append(" \"").append(name).append('"');
        }
        if (action != null) {
            buf.append(", \"").append(action).append('"');
        }
        if (signedBy != null) {
            buf.append(", ").append(SIGNEDBY_KEYWORD).append(" \"").append(signedBy).append('"');
        }
        buf.append(';').append(NEW_LINE);
        return buf.toString();
    }

    public String getPermissionType() {
        return permissionType;
    }

    public String getName() {
        return name;
    }

    public String getAction() {
        return action;
    }

    public String getSignatures() {
        return signedBy;
    }

}