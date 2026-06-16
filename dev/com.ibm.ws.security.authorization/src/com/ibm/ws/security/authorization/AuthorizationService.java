/*******************************************************************************
 * Copyright (c) 2011, 2026 IBM Corporation and others.
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;

/**
 * This class defines the interface for checking if a user is authorized.
 * Authorization is granted when the Subject (either an authenticated entity
 * or special subject) has been assigned one of the required roles for the
 * given resource. Role assignment is typically done on a per-resource basis,
 * hence the resourceName is used to determine the role to Subject mapping.
 * <p>
 * The AuthorizationService collaborates with the AuthorizationTableService and
 * AccessDecisionService to perform the authorization checks.
 *
 * @see AuthorizationTableService {@link AuthorizationTableService} - defines the special subjects
 * @see AccessDecisionService
 */
public interface AuthorizationService {

    /**
     * The {@link #AUTHORIZATION_TYPE} must be unique for each type
     * of AuthorizationService implementation. The value must be of type String.
     */
    String AUTHORIZATION_TYPE = "com.ibm.ws.security.authorization.type";

    /**
     * First checks if the special subject Everyone is mapped to one of the requiredRoles. If it is, access is granted.
     * If it's not, it checks if the Subject is authorized to access the specified resource.
     *
     * Access requires that the Subject is in at least one of the required roles.
     * The assigned roles for the Subject are determined by the specified resource.
     * If multiple resources have the same name, access will be denied and an
     * error message should be printed.
     *
     * @param resourceName  the name of the resource being accessed, used to
     *                          look up the corresponding authorization table. Must not be {@code null}.
     * @param requiredRoles the roles required in order to determine authorization.
     *                          Must not be {@code null}.
     * @param subject       the Subject which is trying to access the resource. May be {@code null}.
     *                          If {@code null}, the Subject on the thread is used.
     * @return {@code true} if the Subject is authorized (or the requiredRoles is empty), {@code false} otherwise.
     */
    boolean isAuthorized(String resourceName, Collection<String> requiredRoles, Subject subject);

    /**
     * Check if the special subject Everyone is mapped to one of the requiredRoles.
     *
     * @param resourceName  the name of the resource being accessed, used to
     *                          look up the corresponding authorization table. Must not be {@code null}.
     * @param requiredRoles the roles required in order to determine authorization.
     *                          Must not be {@code null}.
     * @return {@code true} if the special subject Everyone is authorized (or the requiredRoles is empty), {@code false} otherwise.
     */
    boolean isEveryoneGranted(String resourceName, Collection<String> requiredRoles);

    /**
     * Gets the set of roles that are associated with the WSCredential in the provided Subject
     *
     * @param resourceName the application name
     * @param subject      the Subject whose roles are being queried
     * @return the Set of roles for the provided Subject
     */
    default Set<String> getMappedRoles(String resourceName, Subject subject) {
        return Collections.emptySet();
    }

    /**
     * Determines if ** role is mapped by the application so cannot be treated as any authenticated user
     *
     * @param resourceName the application name
     * @return {@code true} if ** is a role that is mapped to specific users / groups by the application
     */
    default boolean isStarStarRoleMapped(String resourceName) {
        return false;
    }

    /**
     * Retrieve all declared roles in the application that subject is assigned
     *
     * Any dynamically assigned roles should not be returned.
     *
     * @param resourceName the name of the resource being accessed (i.e. the application), used to
     *                         look up the corresponding authorization table. Must not be {@code null}.
     * @param subject      the Subject which roles we are trying to find in the resource. Must not be {@code null}.
     *
     * @return {@code Set<String>} of all the roles assigned to the subject in the resource.
     *         Returns an empty set if no roles for
     */
    default Set<String> getAllDeclaredRolesForResourceForSubject(String resourceName, Subject subject) {
        return new HashSet<String>(Collections.EMPTY_SET);
    }

}
