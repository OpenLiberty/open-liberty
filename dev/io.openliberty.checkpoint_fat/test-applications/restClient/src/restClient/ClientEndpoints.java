/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package restClient;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("client")
@ApplicationScoped
public class ClientEndpoints {

    @Inject
    @RestClient
    private RESTclient restClient;

    @Inject
    @ConfigProperty(name = "default.http.port")
    private String port;

    @GET
    @Path("properties")
    @Produces(MediaType.APPLICATION_JSON)
    public String produceOutput() {
        try {
            return restClient.getProperties();
        } catch (Exception e) {
            e.printStackTrace();
            return "Exception Thrown";
        }
    }

    @POST
    @Path("setHost/{host}")
    public void setHost(@PathParam(value = "host") String baseURI) {
        String customURIString = "http://localhost:" + port + "/webappWAR/" + baseURI;
        URI customURI = URI.create(customURIString);
        RESTclient customRestClient = RestClientBuilder.newBuilder()
                        .baseUri(customURI)
                        .build(RESTclient.class);
        restClient = customRestClient;
    }

}