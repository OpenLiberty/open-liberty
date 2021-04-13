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
package com.ibm.ws.jaxrs21.fat.JAXRS21bookstore;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;

/**
 *
 */
@Path("/JAXRS21bookstore2")
public class JAXRS21BookStore2 {
    private final List<JAXRS21Book> books = new ArrayList<JAXRS21Book>();

    public JAXRS21BookStore2() {
        books.add(new JAXRS21Book("Book1", 1));
        books.add(new JAXRS21Book("Book2", 1));
        books.add(new JAXRS21Book("Book3", 1));
    }

    @GET
    @Path("/rxget1")
    public void getasyncBook(@Suspended AsyncResponse async) {
        async.resume("Good book");
    }

    @GET
    @Path("/rxget2")
    public void getasyncBookObject(@Suspended AsyncResponse async) {
        System.out.println("JAXRS21BookStore2.getasyncBookObject ");
        async.resume(new JAXRS21Book("Good book", 100));
    }

    // @GET
    // @Path("/asyncget3")
    // public List<Book> getBookList() {
    // return books;
    // }

    @GET
    @Path("/rxget3")
    public void getasyncBookList(@Suspended AsyncResponse async) {
        GenericEntity<List<JAXRS21Book>> genericEntity = (new GenericEntity<List<JAXRS21Book>>(books){
        });
        async.resume(genericEntity);
    }

    @POST
    @Path("/rxpost1")
    public void postasyncBook(@Suspended AsyncResponse async, JAXRS21Book newBook) {
        books.add(newBook);
        async.resume(books.get(books.size() - 1).getName());
    }

    @POST
    @Path("/rxpost2")
    public void postasyncBookObject(@Suspended AsyncResponse async, JAXRS21Book newBook) {
        books.add(newBook);
        async.resume(books.get(books.size() - 1));
    }

    @POST
    @Path("/rxpost3")
    public void postasyncBookList(@Suspended AsyncResponse async, JAXRS21Book newBook) {
        books.add(newBook);
        GenericEntity<List<JAXRS21Book>> genericEntity = (new GenericEntity<List<JAXRS21Book>>(books){
        });
        async.resume(genericEntity);
    }

    @GET
    @Path("/get1")
    public Response getBook() {
        return Response.ok("Good book").build();
    }

    @GET
    @Path("/get2")
    public JAXRS21Book getBookObject() {
        return new JAXRS21Book("Good book", 100);
    }

    @GET
    @Path("/getBadBook")
    public JAXRS21Book getBadBookObject() {
        return new JAXRS21Book("Bad book", 123);
    }

    @GET
    @Path("/get3")
    public List<JAXRS21Book> getBookList() {
        return books;
    }

    @POST
    @Path("/post1")
    public Response postBook(JAXRS21Book newBook) {
        books.add(newBook);
        return Response.ok(books.get(books.size() - 1).getName()).build();
    }

    @POST
    @Path("/post2")
    public JAXRS21Book postBookObject(JAXRS21Book newBook) {
        books.add(newBook);
        return books.get(books.size() - 1);
    }

    @POST
    @Path("/post3")
    public List<JAXRS21Book> postBookList(JAXRS21Book newBook) {
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
