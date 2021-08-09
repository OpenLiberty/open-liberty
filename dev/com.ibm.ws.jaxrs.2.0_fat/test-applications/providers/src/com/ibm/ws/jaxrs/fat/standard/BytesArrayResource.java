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

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("providers/standard/bytesarray")
public class BytesArrayResource {

    private byte[] barr = null;

    @GET
    public Response getByteArray() {
        return Response.ok(barr).build();
    }

    @POST
    public byte[] postByteArray(byte[] bytearray) {
        return bytearray;
    }

    @PUT
    public void putByteArray(byte[] bytearray) {
        barr = bytearray;
    }

    @POST
    @Path("/empty")
    @Produces("text/plain")
    public Response postEmptyByteArray(byte[] bytearray) {
        if (bytearray != null && bytearray.length == 0) {
            return Response.ok("expected").build();
        }
        return Response.serverError().build();
    }

}
