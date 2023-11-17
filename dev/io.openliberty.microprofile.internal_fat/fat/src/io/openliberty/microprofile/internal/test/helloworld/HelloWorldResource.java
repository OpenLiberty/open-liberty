/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package io.openliberty.microprofile.internal.test.helloworld;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/helloworld")
public class HelloWorldResource {

    @Inject
    HelloWorldBean bean;

    /**
     * Processes a GET request and returns the stored message.
     *
     * @return the stored message
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getMessage() {
        return bean.getMessage();
    }

    @Path("servlettest")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String isServletFound() {
        try {
            Class.forName("javax.servlet.http.HttpServlet");
            return "FOUND";
        } catch (Exception e) {
            return "NOTFOUND";
        }
    }

    @Path("opentracingtest")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String isOpenTracingSPIFound() {
        try {
            Class.forName("com.ibm.ws.opentracing.tracer.OpentracingTracerFactory");
            return "FOUND";
        } catch (Exception e) {
        }
        try {
            Class.forName("io.openliberty.opentracing.spi.tracer.OpentracingTracerFactory");
            return "FOUND";
        } catch (Exception e) {
        }
        return "NOTFOUND";
    }
}
