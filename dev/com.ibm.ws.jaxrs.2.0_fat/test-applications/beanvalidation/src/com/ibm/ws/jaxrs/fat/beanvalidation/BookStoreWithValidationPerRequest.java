/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.beanvalidation;

import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/bookstore/")
public class BookStoreWithValidationPerRequest {
    private final Map<String, BookWithValidation> books = new HashMap<String, BookWithValidation>();
    @NotNull
    private String id;

    public BookStoreWithValidationPerRequest() {
        books.put("123", new BookWithValidation("CXF", "123"));
        books.put("124", new BookWithValidation("124"));
    }

    @QueryParam("id")
    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    @GET
    @Path("book")
    @Valid
    @NotNull
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    public BookWithValidation book() {
        return books.get(id);
    }

    @GET
    @Path("bookResponse")
    @Valid
    @NotNull
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    public Response bookResponse() {
        return Response.ok(books.get(id)).build();
    }
}