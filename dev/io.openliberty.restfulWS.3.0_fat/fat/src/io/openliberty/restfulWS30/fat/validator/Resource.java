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
package io.openliberty.restfulWS30.fat.validator;

import jakarta.ws.rs.PathParam;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;



@ApplicationPath("/app")
@Path("/path/{id}")
@Produces("text/plain")
public class Resource extends Application {    

    private int id;

    @POST
    public String foo(@Min(value = 1) @PathParam("id") int id) {
        System.out.println("foo invoked! with id: " + id);        
        this.id = id;
        return "foo " + id;
    }
    

}
