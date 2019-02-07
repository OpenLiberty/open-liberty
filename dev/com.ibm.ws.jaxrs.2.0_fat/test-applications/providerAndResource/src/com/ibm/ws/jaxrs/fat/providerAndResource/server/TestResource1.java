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
package com.ibm.ws.jaxrs.fat.providerAndResource.server;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

@Provider
@Consumes(value = { "application/json" })
@Path("res1")
public class TestResource1 implements MessageBodyWriter<String> {

    private static volatile int id = 0;
    private int pid = 0;

    /**
     * per request instance
     */
    public TestResource1() {
        id++;
    }

    @GET
    @Path("testUseTheSameInstanceForProviderAndResource")
    @Produces(value = { "application/json" })
    public String testUseTheSameInstanceForProviderAndResource() {
        return "200";
    }

    @Override
    public long getSize(String arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {

        return 0;
    }

    @Override
    public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
        pid = 1;
        return true;
    }

    @Override
    public void writeTo(String arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4, MultivaluedMap<String, Object> arg5, OutputStream arg6) throws IOException, WebApplicationException {
        if (id == 1 && pid == 1) {
            arg6.write("OK".getBytes());
        }
        else {
            arg6.write(("FAIL:pid=" + pid + ",id=" + id).getBytes());
        }
    }
}
