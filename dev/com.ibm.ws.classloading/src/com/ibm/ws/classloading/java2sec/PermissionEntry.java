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


/**
 * A permission entry must begin with the word permission. The word permission_class_name 
 * in the template above would actually be a specific permission type, such as java.io.FilePermission 
 * or java.lang.RuntimePermission.  
 * The "action" is required for many permission types, such as java.io.FilePermission 
 * (where it specifies what type of file access is permitted). It is not required for categories 
 * such as java.lang.RuntimePermission where it is not necessary--you either have the permission 
 * specified by the "target_name" value following the permission_class_name or you don't.  
 * The signedBy name/value pair for a permission entry is optional. If present, it indicates a 
 * signed permission. That is, the permission class itself must be signed by the given alias(es) 
 * in order for the permission to be granted. For example, suppose you have the following grant entry:
 *   grant {
 *     permission Foo "foobar", signedBy "FooSoft";
 * };
 * Then this permission of type Foo is granted if the Foo.class permission was placed in a JAR file 
 * and the JAR file was signed by the private key corresponding to the public key in the certificate 
 * specified by the "FooSoft" alias, or if Foo.class is a system class, since system classes are not 
 * subject to policy restrictions.  Items that appear in a permission entry must appear in the 
 * specified order (permission, permission_class_name, "target_name", "action", and 
 * signedBy "signer_names"). An entry is terminated with a semicolon.   
 * Case is unimportant for the identifiers (permission, signedBy, codeBase, etc.) but is 
 * significant for the permission_class_name or for any string that is passed in as a value.
 * 
 * permissionType refers to the name of the Java or custom permission
 * name refers to the resource being granted access to
 * action refers to what action(s) can be allowed on that resource
 * signedBy refers to the signer of the permission
 * 
 */
public class PermissionEntry {
    
    public String permissionType;
    public String name;
    public String action;
    public String signedBy;
    

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
        buf.append(Constants.PERMISSION_KEYWORD).append(' ').append(permissionType);
        if (name != null) {
            buf.append(" \"").append(name).append('"');
        }
        if (action != null) {
            buf.append(", \"").append(action).append('"');
        }
        if (signedBy != null) {
            buf.append(", ").append(Constants.SIGNEDBY_KEYWORD).append(" \"").append(signedBy).append('"');
        }
        buf.append(';').append(Constants.NEW_LINE);
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