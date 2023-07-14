/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package io.openliberty.microprofile.openapi.ui.internal.fat.app;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.OAuthFlow;
import org.eclipse.microprofile.openapi.annotations.security.OAuthFlows;
import org.eclipse.microprofile.openapi.annotations.security.OAuthScope;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;


@SecurityScheme(securitySchemeName = "oauth", type = SecuritySchemeType.OAUTH2,
        flows = @OAuthFlows(authorizationCode = @OAuthFlow(authorizationUrl = "https://host.testcontainers.internal:8020/oauth2/endpoint/TestProvider/authorize",
                tokenUrl = "https://host.testcontainers.internal:8020/oauth2/endpoint/TestProvider/token",
                scopes = @OAuthScope(name = "test"))))
@SecurityRequirement(name = "oauth", scopes = "test")
@RolesAllowed("restricted")
@ApplicationPath("/")
@Path("/test")
public class SecureTestResource {

    @GET
    public String testGet() {
        return "OK";
    }
}
