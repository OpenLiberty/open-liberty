/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.monitor.fat.restApp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

/**
 *
 */
@ApplicationScoped
@Path("/")
public class MetricsResource {

    @GET
    @Path("/normalPathGet")
    public String normalPathGet() throws InterruptedException {
        return "Hello, do you GET it?";
    }

    @POST
    @Path("/normalPathPost")
    public String normalPathPost() throws InterruptedException {
        return "Hello, mister POST man";
    }

}
