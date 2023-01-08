/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package io.openliberty.restfulWS30.cdi30.fat.basic;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
