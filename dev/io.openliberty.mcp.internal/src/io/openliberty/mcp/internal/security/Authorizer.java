/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.security;

import io.openliberty.mcp.internal.McpTransport;
import io.openliberty.mcp.internal.ToolMetadata;
import io.openliberty.mcp.internal.exceptions.jsonrpc.HttpResponseException;
import io.openliberty.mcp.internal.security.SecurityRequirement.SecurityAnnotation;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Performs MCP resource authorization checks
 */
public class Authorizer {

    private static final boolean AUTHORIZED = true;
    private static final boolean NOT_AUTHORIZED = false;

    /**
     * Checks whether the user is authorized to access a particular MCP Tool.
     *
     * @param transport the McpTransport
     * @param tmd the tool to check access for
     * @return {@code true} if the user is authorized to access the tool
     */
    public static boolean isAuthorized(McpTransport transport, ToolMetadata tmd) {
        return passedAuthorization(transport, tmd);
    }

    /**
     *
     * Throws an exception if the user is not authorized to access a particular MCP tool *
     *
     * @param transport
     * @param tmd
     * @return true if the user has been authorized
     */
    public static void requireAuthorized(McpTransport transport, ToolMetadata tmd) {

        SecurityRequirement authData = tmd.securityRequirement();
        boolean authorized = passedAuthorization(transport, tmd);

        /*
         * @DenyAll always results in 403 Forbidden
         */
        if (authData.securityAnnotation() == SecurityAnnotation.DENY) {
            throw new HttpResponseException(HttpServletResponse.SC_FORBIDDEN);
        }

        if (!authorized) {
            if (transport.isAuthenticated()) {
                // User is known but not permitted
                throw new HttpResponseException(HttpServletResponse.SC_FORBIDDEN);
            } else {
                // No user identity present authentication required
                throw new HttpResponseException(HttpServletResponse.SC_UNAUTHORIZED);
            }
        }

    }

    /**
     * Checks whether the user is authorized to access a particular MCP Tool.
     *
     * Class-level annotations set default security policy for all business methods in that class.
     * Method-level annotations override class-level annotations for a method.
     *
     * Method Level (Least permissive to most permissive if conflicts exist)
     * -- @DenyAll takes precedence then
     * -- @RolesAllowed then
     * -- @PermitAll
     *
     */
    private static boolean passedAuthorization(McpTransport transport, ToolMetadata tmd) {
        SecurityRequirement authData = tmd.securityRequirement();

        return switch (authData.securityAnnotation()) {
            case NONE -> AUTHORIZED;
            case DENY -> NOT_AUTHORIZED;
            case PERMIT -> AUTHORIZED;
            case ROLES -> transport.isUserInRole(authData.roleList());
        };
    }
}