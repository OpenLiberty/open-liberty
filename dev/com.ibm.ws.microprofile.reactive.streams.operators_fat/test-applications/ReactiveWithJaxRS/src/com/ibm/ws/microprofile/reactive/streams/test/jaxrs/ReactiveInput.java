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

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import componenttest.topology.utils.FATServletClient;

@Path("/input")
public class ReactiveInput {

    @Inject
    StorageService storage;

    @Inject
    SimpleAsyncPublisher publisher;

    @GET
    public String sendMessage(@QueryParam("message") String message) {
        storage.toString();
        publisher.publish(message);
        return FATServletClient.SUCCESS;
    }

}
