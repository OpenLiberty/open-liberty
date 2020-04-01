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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.executable.ExecutableType;
import javax.validation.executable.ValidateOnExecution;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/bookstore/")
public class BookStoreWithValidation extends AbstractBookStoreWithValidation implements BookStoreValidatable {
    private final Map<String, BookWithValidation> books = new HashMap<String, BookWithValidation>();

    public BookStoreWithValidation() {}

    @GET
    @Path("/books/{bookId}")
    @Override
    @NotNull
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    public BookWithValidation getBook(@PathParam("bookId") String id) {
        return books.get(id);
    }

    @GET
    @Path("/booksResponse/{bookId}")
    @Valid
    @NotNull
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    public Response getBookResponse(@PathParam("bookId") String id) {
        return Response.ok(books.get(id)).build();
    }

    @GET
    @Path("/booksResponseNoValidation/{bookId}")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    public Response getBookResponseNoValidation(@PathParam("bookId") String id) {
        return Response.ok(books.get(id)).build();
    }

    @POST
    @Path("/books")
    public Response addBook(@Context final UriInfo uriInfo,
                            @NotNull @Size(min = 1, max = 50) @FormParam("id") String id,
                            @FormParam("name") String name) {
        books.put(id, new BookWithValidation(name, id));
        return Response.created(uriInfo.getRequestUriBuilder().path(id).build()).build();
    }

    @POST
    @Path("/books/direct")
    @Consumes("text/xml")
    public Response addBookDirect(@Valid BookWithValidation book, @Context final UriInfo uriInfo) {
        books.put(book.getId(), book);
        return Response.created(uriInfo.getRequestUriBuilder().path(book.getId()).build()).build();
    }

    @POST
    @Path("/booksNoValidate")
    @ValidateOnExecution(type = ExecutableType.NONE)
    public Response addBookNoValidation(@NotNull @FormParam("id") String id) {
        return Response.ok().build();
    }
    @POST
    @Path("/booksValidate")
    @ValidateOnExecution(type = ExecutableType.IMPLICIT)
    public Response addBookValidate(@NotNull @FormParam("id") String id) {
        return Response.ok().build();
    }

    @POST
    @Path("/books/directmany")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    public Response addBooksDirect(@Valid List<BookWithValidation> list, @Context final UriInfo uriInfo) {
        books.put(list.get(0).getId(), list.get(0));
        return Response.created(uriInfo.getRequestUriBuilder().path(list.get(0).getId()).build()).build();
    }

    @Override
    @GET
    @Path("/books")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    public Collection<BookWithValidation> list(@DefaultValue("1") @QueryParam("page") int page) {
        return books.values();
    }

    @DELETE
    @Path("/books")
    public Response clear() {
        books.clear();
        return Response.ok().build();
    }
}
