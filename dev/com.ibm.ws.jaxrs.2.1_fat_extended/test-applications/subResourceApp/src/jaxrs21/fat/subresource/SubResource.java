/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxrs21.fat.subresource;

import java.util.HashMap;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

@RequestScoped
public class SubResource {

    @Context
    UriInfo uriInfo;
    
    @Inject
    MyBean myBean;
    
    @GET
    @Path("id/{id}")
    public HashMap<String, String> getId(@PathParam("id") String id) {
        HashMap<String, String> map = new HashMap<>();
        map.put("subId", id);
        return map;
    }
    
    @GET
    @Path("context")
    public String getUriFromContextInjectedField() {
        return uriInfo.getRequestUri().toString();
    }
    
    @GET
    @Path("cdi")
    public String getStringFromCdiInjectedField() {
        return myBean.getSomeString();
    }
}
