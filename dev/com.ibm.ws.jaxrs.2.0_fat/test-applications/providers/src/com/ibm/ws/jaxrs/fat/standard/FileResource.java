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
package com.ibm.ws.jaxrs.fat.standard;

import java.io.File;
import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("providers/standard/file")
public class FileResource {

    private static File f;

    @POST
    public File postFile(File f) {
        return f;
    }

    @GET
    public Response getFile() {
        return Response.ok(FileResource.f).build();
    }

    @PUT
    public void putFile(File f) throws IOException {
        FileResource.f = f;
    }

    @POST
    @Path("/empty")
    public Response postEmptyFile(File f) {
        if (f.exists() && f.length() == 0) {
            return Response.ok("expected").build();
        }
        return Response.serverError().build();
    }
}
