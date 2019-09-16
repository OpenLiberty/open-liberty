/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.streams.test.jaxrs;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

/**
 * This class will Subscribe to a data stream and request a set number of elements
 * to respond with
 */
@Path("/output")
public class ReactiveOutput {

    @Inject
    private SessionScopedStateBean messages;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> get(@QueryParam("count") int count) throws InterruptedException, ExecutionException {

        EndpointSubscriber<? super String> rs = new EndpointSubscriber<>();

        ReactiveStreams.fromIterable(messages).to(rs).run();

        List<String> response = rs.getResponse(count);

        return response;

    }
}
