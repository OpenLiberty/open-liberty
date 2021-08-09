/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpRestClient12.headerPropagation;

import java.net.URI;
import java.net.URL;
import java.security.Principal;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
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
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationPath("/")
@Path("/resource")
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.TEXT_PLAIN)
public class Resource extends Application {
    private static final Logger LOG = Logger.getLogger(Resource.class.getName());

    @Context
    UriInfo uriInfo;

    @Context
    SecurityContext securityContext;

    @Context
    HttpHeaders headers;

    @Inject
    @RestClient
    Client client;

    @Inject
    @RestClient
    SecureClient secureClient;
    
    @Inject
    @RestClient
    ClientHeaderParamClient chpClient;

    @GET
    public String initial() throws Exception {
        LOG.info("auth scheme: " + securityContext.getAuthenticationScheme());
        Principal p = securityContext.getUserPrincipal();
        LOG.info("user principal name: " + (p==null?"null":p.getName()));
        LOG.info("isSecure: " + securityContext.isSecure());
        if (securityContext.isUserInRole("role1")) {
            return secureClient.useAuthorization();
        }
        return client.normalMethod();
    }

    @GET
    @Path("/clientHeaderParam")
    public String clientHeaderParamClient() throws Exception {
        LOG.info("clientHeaderParamClient()");
        return chpClient.normalMethod();
    }

    @GET
    @Path("/clientHeadersFactory")
    public String clientHeadersFactory() throws Exception {
        LOG.info("clientHeadersFactory()");
        String baseUri = InAppConfigSource.getUriForClient(Client.class);
        ClientHeadersFactoryClient c = RestClientBuilder.newBuilder()
                                                        .baseUri(URI.create(baseUri))
                                                        .build(ClientHeadersFactoryClient.class);
        return c.normalMethod();
    }

    @GET
    @Path("/cdiClientHeadersFactory")
    public String cdiClientHeadersFactory() throws Exception {
        LOG.info("cdiClientHeadersFactory()");
        String baseUri = InAppConfigSource.getUriForClient(Client.class);
        CdiClientHeadersFactoryClient c = RestClientBuilder.newBuilder()
                                                           .baseUri(URI.create(baseUri))
                                                           .build(CdiClientHeadersFactoryClient.class);
        return c.normalMethod();
    }

    @GET
    @Path("normal")
    public String normalMethod() {
        LOG.info("normalMethod");
        return allHeadersAsString();
    }

    @GET
    @Path("auth")
    @RolesAllowed("role1")
    public String securedMethod() {
        LOG.info("securedMethod");
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
