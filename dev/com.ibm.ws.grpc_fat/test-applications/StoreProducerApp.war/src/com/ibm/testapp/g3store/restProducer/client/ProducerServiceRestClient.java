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
package com.ibm.testapp.g3store.restProducer.client;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.ibm.testapp.g3store.restProducer.model.AppStructure;
import com.ibm.testapp.g3store.restProducer.model.MultiAppStructues;

/**
 * @author anupag
 *
 *         This class is MicroProfile REST Client interface of producer APIs.
 *         It uses the JAX-RS annotations.
 *
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/producer")
public interface ProducerServiceRestClient {

    @PUT
    @Path("/create")
    public Response createApp(AppStructure reqPOJO) throws Exception;

    @PUT
    @Path("/create/multi")
    public Response createMultiApps(MultiAppStructues reqPOJO) throws Exception;

    @Path("/remove/{name}")
    @DELETE
    public Response deleteApp(@PathParam("name") String name) throws Exception;

    @Path("/remove/all")
    @DELETE
    public Response deleteAllApps() throws Exception;

}
