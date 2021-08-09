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

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("providers/standard/reader")
public class ReaderResource {

    private char[] carr = null;

    @GET
    public Response getReader() {
        return Response.ok(new BufferedReader(new CharArrayReader(carr))).build();
    }

    @POST
    public Reader postReader(Reader reader) {
        return reader;
    }

    @POST
    @Path("/subclasses/shouldfail")
    public BufferedReader postReader(BufferedReader br) {
        return br;
    }

    @PUT
    public void putReader(Reader is) throws IOException {
        char[] buffer = new char[1];
        int read = 0;
        int offset = 0;
        while ((read = is.read(buffer, offset, buffer.length - offset)) != -1) {
            offset += read;
            if (offset >= buffer.length) {
                buffer = ArrayUtils.copyOf(buffer, buffer.length * 2);
            }
        }
        carr = ArrayUtils.copyOf(buffer, offset);
    }

    @POST
    @Path("empty")
    public Response postEmptyReader(Reader reader) throws IOException {
        if (reader != null && reader.read() == -1) {
            return Response.ok("expected").build();
        }
        return Response.serverError().build();
    }

}
