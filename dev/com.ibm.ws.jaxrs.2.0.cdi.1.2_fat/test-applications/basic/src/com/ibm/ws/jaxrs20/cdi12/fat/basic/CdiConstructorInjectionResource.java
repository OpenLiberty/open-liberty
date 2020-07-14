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
package com.ibm.ws.jaxrs20.cdi12.fat.basic;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.inject.Inject;
import javax.ws.rs.GET;

@Path("cdiresource")
public class CdiConstructorInjectionResource {

    private InjectedClass injectedClass;
    
    public CdiConstructorInjectionResource() {}
    
    @Inject
    public CdiConstructorInjectionResource(InjectedClass injectedClass) {
        this.injectedClass = injectedClass;
    }
    
    @GET
    @Path("get")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get() {
        System.out.println(injectedClass.message());
        return Response.ok().entity(injectedClass.message()).build();
    }
}
