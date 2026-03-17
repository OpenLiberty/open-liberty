/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package inmemory.identity.store;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Protected REST API resource that requires authentication and authorization.
 * This is the REST API endpoint that the tests actually call to verify authentication and authorization work correctly with the InMemoryIdentityStore.
 * It provides the required protected resources.
 * Access is restricted to users with "caller" or "user" roles.
 * It uses `@RolesAllowed` annotations to enforce role-based access control, which tests that:
 * - Users with correct credentials AND correct groups can access the API (200 OK)
 * - Users with correct credentials but WRONG groups get denied (403 Forbidden)
 * - Users with incorrect credentials get rejected (401 Unauthorized)
 */
@Path("/resource")
public class InMemoryIdentityStoreProtectedResource {

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
}
