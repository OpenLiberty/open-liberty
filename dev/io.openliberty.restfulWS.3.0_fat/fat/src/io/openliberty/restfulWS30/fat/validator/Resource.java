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

    private String id;

    @POST
    public String foo(@NotNull @PathParam("id") String id) {
        System.out.println("foo invoked! with id: " + id);         
        new Exception("foo is running on " + Thread.currentThread().getName()).printStackTrace(System.out);
        this.id = id;
        return "foo " + id;
    }
    

}
