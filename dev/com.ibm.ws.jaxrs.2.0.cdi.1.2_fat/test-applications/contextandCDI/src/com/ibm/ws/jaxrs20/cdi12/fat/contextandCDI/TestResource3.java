/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package com.ibm.ws.jaxrs20.cdi12.fat.contextandCDI;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.enterprise.context.Dependent;
import javax.servlet.ServletContext;
import javax.inject.Inject;

@Dependent
@Path("resource3")
public class TestResource3 {
    
    @Context ServletContext servletContext;
    @Inject ServletContext servletContext2;

    @GET
    @Path("/{test}")
    public Response get(@PathParam("test") String testName) {
        System.out.println(testName + " TestResource3#get: servletContext.getServletContextName " + servletContext.getServletContextName() );
        System.out.println(testName + " TestResource3#get: servletContext.getServletContextName2 " + servletContext2.getServletContextName() );
        return Response.ok("ok").build();
    }
}
