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
 * The AccessDecisionService defines the service interface for performing the
 * ultimate decision to determine if a user has access to a resource.
 * <p>
 * This will be called by the AuthorizationService.
 * 
 * @see AuthorizationService
 */
public interface AccessDecisionService {

    /**
     * Check if the Subject is allowed to access the specified resource. The
     * exact criteria used to make this determination depends on the implementation.
     * 
     * @param resourceName the name of the resource being accessed. Must not be {@code null}.
     * @param requiredRoles the roles required to be granted access to the resource.
     *            Must not be {@code null}.
     * @param assignedRoles the roles mapped to the given subject, {@code null} is tolerated.
     * @param subject the Subject which is trying to access the resource, {@code null} is tolerated.
     * @return {@code true} if the Subhject is granted access, {@code false} otherwise.
     */
    boolean isGranted(String resourceName, Collection<String> requiredRoles,
                      Collection<String> assignedRoles, Subject subject);
}