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
package io.openliberty.opentracing.internal.test.helloworld;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hello World JAXRS service.
 */
@ApplicationPath("rest")
@Path("ws")
public class HelloWorldJAXRS extends Application {
    
    private final static Logger LOGGER = Logger.getLogger(HelloWorldJAXRS.class.getName());
    /**
     * Return Hello World OK response.
     * @return Hello World text/plain.
     */
    @GET
    @Path("helloWorld")
    @Produces(MediaType.TEXT_PLAIN)
    public String helloWorld() {
        LOGGER.log(Level.INFO, "helloWorld web service called");
        return "Hello World";
    }

    /**
     * List classes of providers.
     */
    @Override
    public Set<Class<?>> getClasses() {
        return new HashSet<>(Arrays.asList(HelloWorldJAXRS.class));
    }
}
