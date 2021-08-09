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

import java.util.Collection;

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
     * @param resourceName the name of the resource being accessed, used to
     *            look up the corresponding authorization table. Must not be {@code null}.
     * @param requiredRoles the roles required in order to determine authorization.
     *            Must not be {@code null}.
     * @param subject the Subject which is trying to access the resource. May be {@code null}.
     *            If {@code null}, the Subject on the thread is used.
     * @return {@code true} if the Subject is authorized (or the requiredRoles is empty), {@code false} otherwise.
     */
    boolean isAuthorized(String resourceName, Collection<String> requiredRoles, Subject subject);

    /**
     * Check if the special subject Everyone is mapped to one of the requiredRoles.
     * 
     * @param resourceName the name of the resource being accessed, used to
     *            look up the corresponding authorization table. Must not be {@code null}.
     * @param requiredRoles the roles required in order to determine authorization.
     *            Must not be {@code null}.
     * @return {@code true} if the special subject Everyone is authorized (or the requiredRoles is empty), {@code false} otherwise.
     */
    boolean isEveryoneGranted(String resourceName, Collection<String> requiredRoles);

}
