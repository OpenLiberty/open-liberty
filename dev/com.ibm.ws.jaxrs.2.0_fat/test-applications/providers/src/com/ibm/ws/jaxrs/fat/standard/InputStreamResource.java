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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("providers/standard/inputstream")
public class InputStreamResource {

    private byte[] barr = null;

    @GET
    public Response getInputStream() {
        return Response.ok(new ByteArrayInputStream(barr)).build();
    }

    @POST
    public InputStream postInputStream(InputStream is) {
        return is;
    }

    @POST
    @Path("/subclasses/shouldfail")
    public ByteArrayInputStream postInputStream(ByteArrayInputStream is) {
        return is;
    }

    @PUT
    public void putInputStream(InputStream is) throws IOException {
        byte[] buffer = new byte[(is.available() <= 0) ? 1 : is.available()];
        int read = 0;
        int offset = 0;
        while ((read = is.read(buffer, offset, buffer.length - offset)) != -1) {
            offset += read;
            if (offset >= buffer.length) {
                buffer = ArrayUtils.copyOf(buffer, buffer.length * 2);
            }
        }
        barr = ArrayUtils.copyOf(buffer, offset);
    }

    @POST
    @Path("/empty")
    public Response postEmptyInputStream(InputStream is) throws IOException {
        if (is != null && is.read() == -1) {
            return Response.ok("expected").build();
        }
        return Response.serverError().build();
    }

}
