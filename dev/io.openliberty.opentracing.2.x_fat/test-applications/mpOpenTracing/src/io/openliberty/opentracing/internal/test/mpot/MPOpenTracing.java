/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.opentracing.internal.test.mpot;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import io.opentracing.Tracer;

/**
 * JAXRS service.
 */
@ApplicationPath("rest")
@Path("ws")
public class MPOpenTracing extends Application {
    /**
     * <p>The open tracing tracer. Injected.</p>
     */
    @Inject
    public Tracer tracer;

    /**
     * Injected class with Traced annotation on the class.
     */
    @Inject
    private POJO pojo;

    /**
     * Return Hello World OK response.
     * @return Hello World text/plain.
     */
    @GET
    @Path("helloWorld")
    @Produces(MediaType.TEXT_PLAIN)
    public String helloWorld() {
        pojo.annotatedClassMethodImplicitlyTraced();
        return "Hello World";
    }

    @GET
    @Path("notFound")
    @Produces(MediaType.TEXT_PLAIN)
    public String notFound() {
        throw new NotFoundException("This is an expected exception.  Do not open a defect.");
    }

    /**
     * List classes of providers.
     */
    @Override
    public Set<Class<?>> getClasses() {
        return new HashSet<>(Arrays.asList(MPOpenTracing.class));
    }
    
    /**
     * <p>Service API used to peek at the tracer state.</p>
     *
     * <p>Produces the print string of the injected tracer as
     * plain text.</p>
     *
     * @return The print string of the injected tracer as plain
     *    text.
     */
    @GET
    @Path("getTracerState")
    @Produces(MediaType.TEXT_PLAIN)
    public String getTracerState() {
        if ( tracer == null ) {
            return "*** TRACER INJECTION FAILURE ***";
        } else {
            return tracer.toString();
        }
    }
    
    @DELETE
    @Path("reset")
    public void clearTracer() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        tracer.getClass().getMethod("reset").invoke(tracer);
    }
}
