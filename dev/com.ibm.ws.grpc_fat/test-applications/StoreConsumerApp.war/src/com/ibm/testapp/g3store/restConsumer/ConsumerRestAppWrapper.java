/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.testapp.g3store.restConsumer;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeIn;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.OAuthFlow;
import org.eclipse.microprofile.openapi.annotations.security.OAuthFlows;
import org.eclipse.microprofile.openapi.annotations.security.OAuthScope;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;

import com.ibm.testapp.g3store.restConsumer.api.ConsumerRestEndpoint;

/**
 * @author anupag
 *
 *         The OpenAPI annotations are used and only required to generate documentation
 *
 */
@ApplicationPath("/v1C")
@OpenAPIDefinition(
                   info = @Info(
                                title = "Consumer StoreClient App",
                                version = "1.0",
                                description = "JAX-RS based API for fetching and consuming data using gRPC services created by another StoreProducer API."))
@SecuritySchemes(
                 value = {
                           @SecurityScheme(
                                           securitySchemeName = "ApiKeyAuthorization",
                                           type = SecuritySchemeType.APIKEY,
                                           description = "authentication needed to create a new  profile for the store",
                                           apiKeyName = "createPetProfile",
                                           in = SecuritySchemeIn.HEADER),
                           @SecurityScheme(
                                           securitySchemeName = "OAuth2Authorization",
                                           type = SecuritySchemeType.OAUTH2,
                                           description = "authentication needed to delete a profile",
                                           flows = @OAuthFlows(
                                                               implicit = @OAuthFlow(
                                                                                     authorizationUrl = "https://../api/oauth/dialog",
                                                                                     scopes = @OAuthScope(name = "read:reviews")),
                                                               authorizationCode = @OAuthFlow(
                                                                                              authorizationUrl = "https://../api/oauth/dialog",
                                                                                              tokenUrl = "https://../api/oauth/token",
                                                                                              scopes = @OAuthScope(name = "read:reviews")))),
                           @SecurityScheme(
                                           securitySchemeName = "JWTAuthorization",
                                           type = SecuritySchemeType.HTTP,
                                           description = "The JWT auth",
                                           scheme = "bearer",
                                           bearerFormat = "jwt"),
                           @SecurityScheme(
                                           securitySchemeName = "ConsumerBasicHttp",
                                           type = SecuritySchemeType.HTTP,
                                           description = "Basic http authentication to access API calls",
                                           scheme = "basic")
                 })
public class ConsumerRestAppWrapper extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new HashSet<>();
        resources.add(ConsumerRestEndpoint.class);
        return resources;
    }

}
