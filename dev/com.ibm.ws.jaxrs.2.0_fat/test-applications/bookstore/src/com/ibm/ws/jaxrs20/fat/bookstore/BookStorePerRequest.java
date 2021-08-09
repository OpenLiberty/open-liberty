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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

@Path("/bookstore2")
public class BookStorePerRequest {

    private HttpHeaders httpHeaders;
    private final Map<Long, Book> books = new HashMap<Long, Book>();
    private List<String> bookIds;
    private List<String> setterBookIds;

    public BookStorePerRequest() {
        throw new RuntimeException();
    }

    public BookStorePerRequest(@Context HttpHeaders headers) {
        throw new RuntimeException();
    }

    public BookStorePerRequest(@Context HttpHeaders headers, Long bar) {
        throw new RuntimeException();
    }

    public BookStorePerRequest(@Context HttpHeaders headers,
                               @HeaderParam("BOOK") List<String> bookIds) {
        if (!bookIds.contains("3")) {
            throw new ClientErrorException(Response.status(400).type("text/plain")
                                           .entity("Constructor: Header value 3 is required").build());
        }
        httpHeaders = headers;
        this.bookIds = bookIds;
        init();
    }

    @HeaderParam("Book")
    public void setBook(List<String> ids) {
        if (!ids.equals(bookIds) || ids.size() != 3) {
            throw new ClientErrorException(Response.status(400).type("text/plain")
                                           .entity("Param setter: 3 header values are required").build());
        }
        setterBookIds = ids;
    }

    @Context
    public void setHttpHeaders(HttpHeaders headers) {
        List<String> ids = httpHeaders.getRequestHeader("BOOK");
        if (ids.contains("4")) {
            throw new ClientErrorException(Response.status(400).type("text/plain")
                                           .entity("Context setter: unexpected header value").build());
        }
    }

    @GET
    @Path("/bookheaders/")
    public Book getBookByHeader() throws Exception {

        List<String> ids = httpHeaders.getRequestHeader("BOOK");
        if (!ids.equals(bookIds)) {
            throw new RuntimeException();
        }
        return doGetBook(ids.get(0) + ids.get(1) + ids.get(2));
    }

    @GET
    @Path("/bookheaders/injected")
    public Book getBookByHeaderInjected() throws Exception {

        return doGetBook(setterBookIds.get(0) + setterBookIds.get(1) + setterBookIds.get(2));
    }

    private Book doGetBook(String id) throws BookNotFoundFault {
        Book book = books.get(Long.parseLong(id));
        if (book != null) {
            return book;
        } else {
            BookNotFoundDetails details = new BookNotFoundDetails();
            details.setId(Long.parseLong(id));
            throw new BookNotFoundFault(details);
        }
    }

    final void init() {
        Book book = new Book();
        book.setId(123);
        book.setName("CXF in Action");
        books.put(book.getId(), book);
    }

}
