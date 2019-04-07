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
package com.ibm.ws.jaxrs20.fat.bookstore;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;

@Path("/bookstore2")
public class BookStore2 {
    private final List<Book> books = new ArrayList<Book>();

    public BookStore2() {
        books.add(new Book("Book1", 1));
        books.add(new Book("Book2", 1));
        books.add(new Book("Book3", 1));
    }

    @GET
    @Path("/asyncget1")
    public void getasyncBook(@Suspended AsyncResponse async) {
        async.resume("Good book");
    }

    @GET
    @Path("/asyncget2")
    public void getasyncBookObject(@Suspended AsyncResponse async) {
        async.resume(new Book("Good book", 100));
    }

//    @GET
//    @Path("/asyncget3")
//    public List<Book> getBookList() {
//        return books;
//    }

    @GET
    @Path("/asyncget3")
    public void getasyncBookList(@Suspended AsyncResponse async) {
        async.resume(books);

    }

    @POST
    @Path("/asyncpost1")
    public void postasyncBook(@Suspended AsyncResponse async, Book newBook) {
        books.add(newBook);
        async.resume(books.get(books.size() - 1).getName());
    }

    @POST
    @Path("/asyncpost2")
    public void postasyncBookObject(@Suspended AsyncResponse async, Book newBook) {
        books.add(newBook);
        async.resume(books.get(books.size() - 1));
    }

    @POST
    @Path("/asyncpost3")
    public void postasyncBookList(@Suspended AsyncResponse async, Book newBook) {
        books.add(newBook);
        async.resume(books);
    }

    @GET
    @Path("/get1")
    public Response getBook() {
        return Response.ok("Good book").build();
    }

    @GET
    @Path("/get2")
    public Book getBookObject() {
        return new Book("Good book", 100);
    }

    @GET
    @Path("/getBadBook")
    public Book getBadBookObject() {
        return new Book("Bad book", 123);
    }

    @GET
    @Path("/get3")
    public List<Book> getBookList() {
        return books;
    }

    @POST
    @Path("/post1")
    public Response postBook(Book newBook) {
        books.add(newBook);
        return Response.ok(books.get(books.size() - 1).getName()).build();
    }

    @POST
    @Path("/post2")
    public Book postBookObject(Book newBook) {
        books.add(newBook);
        return books.get(books.size() - 1);
    }

    @POST
    @Path("/post3")
    public List<Book> postBookList(Book newBook) {
        books.add(newBook);
        return books;
    }

    @GET
    @Path("/{sleepTime}")
    public Response getSleep(@PathParam("sleepTime") @DefaultValue("30000") long sleepTime) {
        try {
            Thread.sleep(sleepTime);
            return Response.ok("Slept " + sleepTime + "ms").build();
        } catch (InterruptedException ex) {
            return Response.serverError().entity(ex).build();
        }
    }

    @POST
    @Path("/post/{postSleepTime}")
    public Response postSleep(@PathParam("postSleepTime") @DefaultValue("30000") long sleepTime) {
        try {
            Thread.sleep(sleepTime);
            return Response.ok("Slept " + sleepTime + "ms").build();
        } catch (InterruptedException ex) {
            return Response.serverError().entity(ex).build();
        }
    }
}
