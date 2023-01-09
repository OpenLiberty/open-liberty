/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.jaxrs.fat.standard;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("providers/standard/file")
public class FileResource {

    private static byte[] fileBytes = new byte[1000];

    @POST
    public File postFile(File f) {
        return f;
    }

    @GET
    public Response getFile() {
        return Response.ok(FileResource.fileBytes).build();
    }

    @PUT
    public void putFile(File f) throws IOException {
        try (FileInputStream fis = new FileInputStream(f)) {
            fis.read(FileResource.fileBytes);
        }
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
