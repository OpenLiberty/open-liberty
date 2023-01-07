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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("client/early")
@ApplicationScoped
public class ClientEndpointsEarlyStart {

    public void observeInit(@Observes @Initialized(ApplicationScoped.class) Object event) {
        System.out.println(getClass() + ": " + "Initializing application context");
    }

    @Inject
    @RestClient
    private Provider<RESTclient> restClient;

    @GET
    @Path("properties")
    @Produces(MediaType.APPLICATION_JSON)
    public String produceOutput() {
        try {
            return restClient.get().getProperties();
        } catch (Exception e) {
            e.printStackTrace();
            return "Exception Thrown";
        }
    }

}