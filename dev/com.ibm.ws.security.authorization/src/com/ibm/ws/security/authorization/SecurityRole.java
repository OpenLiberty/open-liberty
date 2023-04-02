/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ws.security.authorization;

import java.util.Set;

/**
 * A security role is a collection of users and groups which are
 * mapped to a specific role name.
 */
public interface SecurityRole {

    /**
     * Answers the name of the role.
     * 
     * @return The role name. Must not be {@code null}.
     */
    String getRoleName();

    /**
     * Answers the set of users assigned this role.
     * If no users are assigned, an empty set shall be returned.
     * 
     * @return A Set of group names. Must not be {@code null}.
     */
    Set<String> getUsers();

    /**
     * Answers the set of groups assigned this role.
     * If no groups are assigned, an empty set shall be returned.
     * 
     * @return A Set of group names. Must not be {@code null}.
     */
    Set<String> getGroups();

    /**
     * Answers the set of special subjects assigned this role.
     * If no special subjects are assigned, an empty set shall be returned.
     * 
     * @return A Set of special subject names. Must not be {@code null}.
     */
    Set<String> getSpecialSubjects();

    /**
     * Answers the set of access ids assigned this role.
     * If no access ids are assigned, an empty set shall be returned.
     * 
     * @return A Set of access ids. Must not be {@code null}.
     */
    Set<String> getAccessIds();
}
