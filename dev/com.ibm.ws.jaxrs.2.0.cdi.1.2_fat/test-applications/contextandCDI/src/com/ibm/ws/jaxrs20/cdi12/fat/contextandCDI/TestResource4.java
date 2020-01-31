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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.enterprise.context.SessionScoped;
import javax.servlet.ServletContext;

import java.io.Serializable;

@SessionScoped
@Path("resource4")
public class TestResource4 implements Serializable {
    
    @Context ServletContext servletContext;
    
    @GET
    public Response get() {
        System.out.println("TestResource4#get: servletContext.getServletContextName " + servletContext.getServletContextName() );
        return Response.ok("ok").build();
    }
}
