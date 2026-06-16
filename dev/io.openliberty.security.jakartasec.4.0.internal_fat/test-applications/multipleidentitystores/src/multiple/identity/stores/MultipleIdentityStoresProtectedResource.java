/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package multiple.identity.stores;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Protected REST API resource for testing multiple identity stores.
 * This resource requires authentication and authorization to test that:
 * - Users from the in-memory store can access with correct credentials
 * - Users from the database store can access with correct credentials
 * - Priority is respected when a user exists in both stores
 */
@Path("/resource")
public class MultipleIdentityStoresProtectedResource {

    @GET
    @Path("/test")
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed({ "caller", "user" })
    public String getProtectedResource(@Context SecurityContext securityContext) {
        String username = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : "anonymous";
        return "SUCCESS: Access granted to user: " + username;
    }

    @GET
    @Path("/caller")
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed("caller")
    public String getCallerResource(@Context SecurityContext securityContext) {
        String username = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : "anonymous";
        return "SUCCESS: Caller access granted to user: " + username;
    }

    @GET
    @Path("/user")
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed("user")
    public String getUserResource(@Context SecurityContext securityContext) {
        String username = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : "anonymous";
        return "SUCCESS: User access granted to user: " + username;
    }

    @GET
    @Path("/dbuser")
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed("dbuser")
    public String getDbUserResource(@Context SecurityContext securityContext) {
        String username = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : "anonymous";
        return "SUCCESS: DB User access granted to user: " + username;
    }

    @GET
    @Path("/memoryuser")
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed("memoryuser")
    public String getMemoryUserResource(@Context SecurityContext securityContext) {
        String username = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : "anonymous";
        return "SUCCESS: Memory User access granted to user: " + username;
    }
}