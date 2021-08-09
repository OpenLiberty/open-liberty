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
package com.ibm.wsspi.rest.handler.helper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ibm.wsspi.rest.handler.RESTHandler;

/**
 * This error is thrown by the {@link RESTHandlerForbiddenError} when an unauthorized request is encountered by a {@link RESTHandler}.
 *
 * @ibm-spi
 */
public class RESTHandlerForbiddenError extends RuntimeException {

    private static final long serialVersionUID = -3647481857680022528L;

    private final int statusCode = 403;

    private Set<String> requiredRoles;

    /**
     * Constructs a new {@link RESTHandlerForbiddenError}.
     *
     * @param requiredRoles Any roles that would be sufficient to perform the operation.
     * @param message the detail message.
     * @param cause the cause.
     */
    public RESTHandlerForbiddenError(Set<String> requiredRoles, String message, Throwable cause) {
        super(message, cause);
        if (requiredRoles != null && !requiredRoles.isEmpty()) {
            this.requiredRoles = new HashSet<String>(requiredRoles);
        }
    }

    /**
     * Constructs a new {@link RESTHandlerForbiddenError}.
     *
     * @param requiredRoles Any roles that would be sufficient to perform the operation.
     * @param message the detail message.
     */
    public RESTHandlerForbiddenError(Set<String> requiredRoles, String message) {
        super(message);
        if (requiredRoles != null && !requiredRoles.isEmpty()) {
            this.requiredRoles = new HashSet<String>(requiredRoles);
        }
    }

    /**
     * Constructs a new {@link RESTHandlerForbiddenError}.
     *
     * @param requiredRoles Any roles that would be sufficient to perform the operation.
     */
    public RESTHandlerForbiddenError(Set<String> requiredRoles) {
        super();
        if (requiredRoles != null && !requiredRoles.isEmpty()) {
            this.requiredRoles = new HashSet<String>(requiredRoles);
        }
    }

    /**
     * The status code to return for this error. This is always 403.
     *
     * @return 403.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Get the roles that would be sufficient for perform the operation.
     *
     * @return The required roles to perform the operation.
     */
    public Set<String> getRequiredRoles() {
        return requiredRoles == null ? null : Collections.unmodifiableSet(requiredRoles);
    }
}
