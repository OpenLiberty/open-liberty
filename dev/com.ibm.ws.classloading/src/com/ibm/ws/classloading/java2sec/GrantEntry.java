/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.classloading.java2sec;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

/**
 *  
 *  Each grant entry includes one or more "permission entries" preceded by optional codeBase, 
 *  signedBy, and principal name/value pairs that specify which code you want to grant the 
 *  permissions. The basic format of a grant entry is the following:
 *
 * grant signedBy "signer_names", codeBase "URL",
 *       principal principal_class_name "principal_name",
 *       principal principal_class_name "principal_name",
 *       ... {
 *
 *     permission permission_class_name "target_name", "action", 
 *         signedBy "signer_names";
 *     permission permission_class_name "target_name", "action", 
 *         signedBy "signer_names";
 *     ...
 * };
 * 
 * In this implementation, codeBase and signedBy map accordingly.  
 * permissionEntries is an arraylist of permissions granted to this codeBase.
 */

public class GrantEntry {
    
    String codeBase;
    String signedBy;
    private List permissionEntries = new ArrayList();

    

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
        buf.append(Constants.GRANT_KEYWORD).append(' ');
        if (codeBase != null) {
            buf.append(Constants.CODEBASE_KEYWORD).append(" \"").append(codeBase).append("\" ");
            if (signedBy != null) {
                buf.append(", ");
            }
        }
        if (signedBy != null) {
            buf.append(Constants.SIGNEDBY_KEYWORD).append(" \"").append(signedBy).append("\" ");
        }
        buf.append('{').append(Constants.NEW_LINE);
        Iterator it = permissionEntries.iterator();
        while (it.hasNext()) {
            buf.append(it.next().toString());
        }
        buf.append("};").append(Constants.NEW_LINE);
        return buf.toString();
    }

}

