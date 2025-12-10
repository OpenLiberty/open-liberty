/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.jaxrs21.client.threadleak.server;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST resource for vehicle (voertuigen) operations.
 * This is the entry point that receives HTTP requests.
 * Path parameter {voertuignummer} varies per request, which is key to reproducing the thread leak.
 */
@Path("rest")
public class JAXRS21MyResource {

    @Inject
    private ComputerService computerService;

    /**
     * Get data for students
     * Each student has a different IP address, causing varying URLs in the JAX-RS client.
     * 
     * @param vnumber Vehicle number (path parameter)
     * @return HTTP 204 No Content on success
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}/data")
    public Response getDataForStudent(@PathParam(value = "id") String id) {
     
        String studentIp = "192.168.1." + id;
        int port = 8080;
        
        try {
            computerService.getStudentData(id, studentIp, port);
            return Response.noContent().build();
        } catch (Exception e) {
            return Response.noContent().build();
        }
    }
}

