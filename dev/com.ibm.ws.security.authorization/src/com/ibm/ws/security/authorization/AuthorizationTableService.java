/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authorization;

/**
 * This class defines the interface for determining the identity to role
 * mappings when making an authorization decision. The identity can be a
 * user, group or special subject.
 * <p>
 * AuthorizationTableService providers may be consumed by the
 * AuthorizationService to perform access decisions.
 * 
 * @see AuthorizationService
 * @see RoleSet
 */
public interface AuthorizationTableService {

    /**
     * The {@link #AUTHORIZATION_TABLE_NAME} must be unique for each type
     * of AuthorizationTableService implementation. The value must be of type String.
     */
    String AUTHORIZATION_TABLE_NAME = "com.ibm.ws.security.authorization.table.name";

    /**
     * Special subject: {@link #EVERYONE}.
     * Used to represent any user (both authorized and unauthorized users).
     */
    String EVERYONE = "EVERYONE";

    /**
     * Special subject: {@link #ALL_AUTHENTICATED_USERS}.
     * Used to represent any authorized user.
     */
    String ALL_AUTHENTICATED_USERS = "ALL_AUTHENTICATED_USERS";

    /**
     * Special subject: {@link #ALL_AUTHENTICATED_IN_TRUSTED_REALMS}.
     * Used to represent any authorized user which originated from a trusted realm.
     */
    String ALL_AUTHENTICATED_IN_TRUSTED_REALMS = "ALL_AUTHENTICATED_IN_TRUSTED_REALMS";

    /**
     * Get the roles that are mapped to the special subject for the given
     * resource.
     * 
     * @param resourceName the name of the resource being accessed, used to
     *            look up the corresponding authorization table. Must not be {@code null}.
     * @param specialSubject the special subject, can be one of the following values:
     *            <ul>
     *            <li>{@link #EVERYONE}</li>
     *            <li>{@link #ALL_AUTHENTICATED_USERS}</li>
     *            <li>{@link #ALL_AUTHENTICATED_IN_TRUSTED_REALMS}</li>
     *            </ul>
     * @return a RoleSet of the roles mapped to the special subject. If no roles are mapped
     *         to that special subject for the given resource, an empty RoleSet is returned.
     *         If there is no resource by the specified resourceName, {@code null} is returned.
     */
    RoleSet getRolesForSpecialSubject(String resourceName, String specialSubject);

    /**
     * Get the roles that are mapped to the specified accessId for a given resource.
     * 
     * @param resourceName the name of the resource being accessed, used to
     *            look up the corresponding authorization table. Must not be {@code null}.
     * @param accessId the accessId in the Subject attempting to access the resource
     * @return a RoleSet of the roles mapped to the accessId. If no roles are mapped
     *         to that accessId for the given resource, an empty RoleSet is returned.
     *         If there is no resource by the specified resourceName, {@code null} is returned.
     */
    RoleSet getRolesForAccessId(String resourceName, String accessId);

    /**
     * Get the roles that are mapped to the specified accessId for a given resource.
     * 
     * @param resourceName the name of the resource being accessed, used to
     *            look up the corresponding authorization table. Must not be {@code null}.
     * @param accessId the accessId in the Subject attempting to access the resource
     * @param realmName the realm name in the wscredential attempting to access the resource
     * @return a RoleSet of the roles mapped to the accessId. If no roles are mapped
     *         to that accessId for the given resource, an empty RoleSet is returned.
     *         If there is no resource by the specified resourceName, {@code null} is returned.
     */
    RoleSet getRolesForAccessId(String resourceName, String accessId, String realmName);

    /**
     * Is an authorization table available for the application.
     * 
     * @param resourceName the name of the resource being accessed, used to
     *            look up the corresponding authorization table. Must not be {@code null}.
     * @return true if authorization table available for the application. Otherwise, return false.
     * 
     */
    boolean isAuthzInfoAvailableForApp(String resourceName);
}
