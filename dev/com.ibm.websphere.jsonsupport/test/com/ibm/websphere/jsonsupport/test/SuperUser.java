/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.jsonsupport.test;

import java.util.List;
import java.util.Map;

public class SuperUser extends User {
    public User[] managedUsers;
    List<User> managedUsersList;
    Map<Object, User> userMap;
    Map<Object, Object> testMap;

    public List<User> getManagedUsersList() {
        return managedUsersList;
    }

    @SuppressWarnings("unused")
    private void setManagedUsersList(List<User> value) {
        managedUsersList = value;
    }

    public Map<Object, User> getUserMap() {
        return userMap;
    }

    @SuppressWarnings("unused")
    private void setUserMap(Map<Object, User> value) {
        userMap = value;
    }

    public Map<Object, Object> getTestMap() {
        return testMap;
    }

    @SuppressWarnings("unused")
    private void setTestMap(Map<Object, Object> value) {
        testMap = value;
    }
}
