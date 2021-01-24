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

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

/**
 *
 */
public class GrantEntry {
    
    String codeBase;
    String signedBy;
    private List permissionEntries = new ArrayList();

    
    final static String NEW_LINE = System.getProperty("line.separator");
    final static String QUOTED_STRING = "quoted string";
    final static String PERMISSION_TYPE = "permission type";
    final static String GRANT_KEYWORD = "grant";
    final static String KEYSTORE_KEYWORD = "keystore";
    final static String CODEBASE_KEYWORD = "codeBase";
    final static String PERMISSION_KEYWORD = "permission";
    final static String SIGNEDBY_KEYWORD = "signedBy";
    final static String FILTER_KEYWORD = "filterMask";

    public GrantEntry() {
    }

    public GrantEntry(String c, String s) {
        codeBase = c;
        signedBy = s;
    }
    
    public String getCodeBase() {
        return codeBase;
    }
    
    public String getSignedBy() {
        return signedBy;
    }

    public void add(PermissionEntry p) {
        if (!permissionEntries.contains(p)) {
            permissionEntries.add(p);
        }
    }

    public boolean remove(PermissionEntry p) {
        return permissionEntries.remove(p);
    }

    public boolean contains(PermissionEntry p) {
        return permissionEntries.contains(p);
    }

    public Iterator getPermissions() {
        return permissionEntries.iterator();
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(permissionEntries.size() * 40);
        buf.append(GRANT_KEYWORD).append(' ');
        if (codeBase != null) {
            buf.append(CODEBASE_KEYWORD).append(" \"").append(codeBase).append("\" ");
            if (signedBy != null) {
                buf.append(", ");
            }
        }
        if (signedBy != null) {
            buf.append(SIGNEDBY_KEYWORD).append(" \"").append(signedBy).append("\" ");
        }
        buf.append('{').append(NEW_LINE);
        Iterator it = permissionEntries.iterator();
        while (it.hasNext()) {
            buf.append(it.next().toString());
        }
        buf.append("};").append(NEW_LINE);
        return buf.toString();
    }

}

