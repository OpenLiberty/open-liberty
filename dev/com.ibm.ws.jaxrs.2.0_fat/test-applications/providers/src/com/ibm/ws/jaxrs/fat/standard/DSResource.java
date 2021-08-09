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
import java.io.OutputStream;

import javax.activation.DataSource;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

@Path(value = "/dstest")
public class DSResource {

    @POST
    public Response post(DataSource dataSource) {
        Response resp = null;
        try {
            InputStream inputStream = dataSource.getInputStream();
            byte[] inputBytes = new byte[1024];
            int next = inputStream.read();
            int i = 0;
            while (next != -1) {
                if (i == inputBytes.length) {
                    inputBytes = ArrayUtils.copyOf(inputBytes, 2 * i);
                }
                inputBytes[i] = (byte) next;
                next = inputStream.read();
                i++;
            }
            TestDataSource entity =
                            new TestDataSource(inputBytes, dataSource.getName(), dataSource.getContentType());
            ResponseBuilder rb = Response.ok();
            rb.entity(entity);
            resp = rb.build();
        } catch (Exception e) {
            ResponseBuilder rb = Response.serverError();
            resp = rb.build();
        }
        return resp;
    }

    public class TestDataSource implements DataSource {

        private final byte[] inputBytes;

        private final String name;

        private final String contentType;

        public TestDataSource(byte[] inputBytes, String name, String contentType) {
            this.inputBytes = inputBytes;
            this.name = name;
            this.contentType = contentType;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(inputBytes);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return null;
        }

    }

    @POST
    @Path("/empty")
    public Response postEmpty(DataSource dataSource) {
        InputStream inputStream;
        try {
            inputStream = dataSource.getInputStream();
            if (inputStream.read() == -1) {
                return Response.ok("expected").build();
            }
        } catch (IOException e) {
            throw new WebApplicationException(e);
        }
        return Response.serverError().build();
    }

}
