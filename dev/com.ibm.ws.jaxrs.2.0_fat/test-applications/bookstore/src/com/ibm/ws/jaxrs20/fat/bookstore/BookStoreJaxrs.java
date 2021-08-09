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
package com.ibm.ws.jaxrs20.fat.bookstore;

import javax.jws.WebParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/bookstore")
@Consumes("application/xml")
@Produces("application/xml")
public interface BookStoreJaxrs {

    @GET
    @Path("/{id}")
    @Consumes("*/*")
    Book getBook(@PathParam("id") Long id) throws BookNotFoundFault;

    @POST
    @Path("/books")
    Book addBook(@WebParam(name = "book") Book book);

    @Path("/books/{id}")
    BookSubresource getBookSubresource(@PathParam("id") String id);

    @Path("/thestore/{id}")
    BookStoreJaxrs getBookStore(@PathParam("id") String id);

    @POST
    @Path("/fastinfoset")
    @Consumes({ "application/fastinfoset", "text/xml" })
    @Produces({ "application/fastinfoset", "text/xml", "application/xml" })
    Book addFastinfoBook(Book book);

    @GET
    @Path("/fastinfoset2")
    @Produces({ "application/fastinfoset", "text/xml", "application/xml" })
    Book getFastinfoBook();

    @GET
    @Path("/check/{id}")
    Response checkBook(@PathParam("id") Long id);
}
