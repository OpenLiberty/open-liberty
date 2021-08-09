/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpRestClient10.headerPropagation;

import java.net.URI;
import java.net.URL;
import java.security.Principal;
import java.util.Collections;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

@ApplicationPath("/")
@Path("/resource")
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.TEXT_PLAIN)
public class Resource extends Application {

    private HeaderPropagationFilter propagationFilter = new HeaderPropagationFilter();

    @Context
    UriInfo uriInfo;

    @Context
    SecurityContext securityContext;

    @Context
    HttpHeaders headers;

    @Override
    public Set<Object> getSingletons() {
        return Collections.singleton(propagationFilter);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return Collections.singleton(Resource.class);
    }

    @GET
    public String initial() throws Exception {
        URI uri = uriInfo.getAbsolutePath();
        String baseUrl = uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort() + "/headerPropagationApp";
        RestClient client = RestClientBuilder.newBuilder()
                                             .baseUrl(new URL(baseUrl))
                                             .register(propagationFilter)
                                             .build(RestClient.class);
        System.out.println("auth scheme: " + securityContext.getAuthenticationScheme());
        Principal p = securityContext.getUserPrincipal();
        System.out.println("user principal name: " + (p==null?"null":p.getName()));
        System.out.println("isSecure: " + securityContext.isSecure());
        if (securityContext.isUserInRole("role1")) {
            return client.useAuthorization();
        }
        return client.normalMethod();
    }

    @GET
    @Path("normal")
    public String normalMethod() {
        return allHeadersAsString();
    }

    @GET
    @Path("auth")
    @RolesAllowed("role1")
    public String securedMethod() {
        return allHeadersAsString() + ";user=" + securityContext.getUserPrincipal().getName() + ";role=role1";
    }

    private String allHeadersAsString() {
        StringBuilder allHeaders = new StringBuilder();
        headers.getRequestHeaders().forEach((key, value) -> allHeaders.append(key)
                                                                      .append("=")
                                                                      .append(value.get(0))
                                                                      .append(";"));
        return allHeaders.toString();
    }
}
