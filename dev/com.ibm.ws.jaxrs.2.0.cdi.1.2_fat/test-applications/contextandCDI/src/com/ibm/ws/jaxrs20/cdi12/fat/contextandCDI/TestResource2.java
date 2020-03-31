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
package com.ibm.ws.jaxrs20.cdi12.fat.contextandCDI;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.enterprise.context.RequestScoped;
import javax.servlet.ServletContext;
import javax.inject.Inject;

@RequestScoped
@Path("resource2")
public class TestResource2 {
    
    @Context ServletContext servletContext;
    @Inject ServletContext servletContext2;
    
    @GET    
    @Path("/{test}")
    public Response get(@PathParam("test") String testName) {
        System.out.println(testName + " TestResource2#get: servletContext.getServletContextName " + servletContext.getServletContextName() );
        System.out.println(testName + " TestResource2#get: servletContext.getServletContextName2 " + servletContext2.getServletContextName() );
        return Response.ok("ok").build();
    }
}
