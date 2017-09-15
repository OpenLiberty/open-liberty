/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authorization.jacc.web.impl;

/*
 *   This is a helper class used by the URLMap class to associate the
 *   correct permissions to each of the servlet methods (GET, PUT etc)
 */
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MethodConstraint {
    HashSet<String> roleSet;
    boolean excluded;
    boolean unchecked;
    String constraintType = null;
    // userDataNone is set to true if userData = NONE is implicitly set.
    boolean userDataNone;

    public MethodConstraint() {
        roleSet = new HashSet<String>();
        excluded = false;
        unchecked = false;
        userDataNone = false;
    }

    public void setExcluded() {
        excluded = true;
    }

    public void setUnchecked() {
        unchecked = true;
    }

    public void setRole(String roleName) {
        roleSet.add(roleName);
    }

    public void setUserData(String userData) {
        if ("NONE".equalsIgnoreCase(userData)) {
            userDataNone = true;
        } else {
            constraintType = userData;
        }
    }

    public boolean isExcluded() {
        return excluded;
    }

    public boolean isUnchecked() {
        return unchecked;
    }

    public boolean isRoleSetEmpty() {
        return roleSet.isEmpty();
    }

    public List<String> getRoleList() {
        return new ArrayList<String>(roleSet);
    }

    public String getUserData() {
        return constraintType;
    }

    public boolean isUserDataNone() {
        return userDataNone;
    }
}
