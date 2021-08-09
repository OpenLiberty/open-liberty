/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.wim.scim20.exceptions;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The principal was not authorized to perform the requested operation.
 */
public class AuthorizationException extends SCIMException {

    private Set<String> requiredRoles = null;

    /**
     * Construct a new {@link AuthorizationException} instance with the specified message and
     * required roles.
     *
     * @param msg The message.
     * @param requiredRoles Roles that would be sufficient to perform the operation.
     */
    public AuthorizationException(String msg, Set<String> requiredRoles) {
        super(403, null, msg);
        this.requiredRoles = requiredRoles == null ? null : Collections.unmodifiableSet(new HashSet<String>(requiredRoles));
    }

    /**
     * Get the roles that would be sufficient to perform the operation.
     *
     * @return Roles that would be sufficient to perform the operation.
     */
    public Set<String> getRequiredRoles() {
        return requiredRoles;
    }
}
