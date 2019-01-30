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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

@Path("providers/standard/streamingoutput")
public class StreamingOutputResource {

    private byte[] barr = null;

    @GET
    public Response getStreamingOutputStream() {
        return Response.ok(new StreamingOutput() {

            @Override
            public void write(OutputStream arg0) throws IOException, WebApplicationException {
                arg0.write(barr);
            }

        }).build();
    }

    @POST
    public StreamingOutput postInputStream(final InputStream is) {
        return new StreamingOutput() {

            @Override
            public void write(OutputStream arg0) throws IOException, WebApplicationException {
                int read = 0;
                while ((read = is.read()) != -1) {
                    arg0.write(read);
                }
            }

        };
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
}
