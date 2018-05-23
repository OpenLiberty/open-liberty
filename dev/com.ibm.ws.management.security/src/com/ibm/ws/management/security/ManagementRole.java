/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.management.security;

import java.util.Set;

/**
 * A management role is a collection of users and groups which are
 * mapped to a specific role name.
 */
public interface ManagementRole {
    /**
     * {@link #MANAGEMENT_ROLE_NAME} expected value type of String, unique for each
     * type of ManagementRole implementation.
     */
    final static String MANAGEMENT_ROLE_NAME = "com.ibm.ws.management.security.role.name";

    /**
     * Answers the name of the role.
     *
     * @return The role name. Must not be {@code null}.
     */
    String getRoleName();

    /**
     * Answers the set of users or accessIds assigned this role.
     * If no users and accessIds are assigned, an empty set shall be returned.
     *
     * @return A Set of user names. Must not be {@code null}.
     */
    Set<String> getUsers();

    /**
     * Answers the set of groups or accessIds assigned this role.
     * If no groups and accessIds are assigned, an empty set shall be returned.
     *
     * @return A Set of group names. Must not be {@code null}.
     */
    Set<String> getGroups();
}
