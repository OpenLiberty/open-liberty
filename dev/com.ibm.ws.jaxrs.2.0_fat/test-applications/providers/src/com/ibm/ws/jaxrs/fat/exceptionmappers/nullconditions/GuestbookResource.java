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
package com.ibm.ws.jaxrs.fat.exceptionmappers.nullconditions;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("guestbooknullconditions")
public class GuestbookResource {

    @GET
    @Path("emptywebappexception")
    public String exception() {
        throw new WebApplicationException();
    }

    @GET
    @Path("webappexceptionwithcause")
    public String exceptionWithCause() {
        throw new WebApplicationException(new GuestbookException("Threw checked exception"));
    }

    @POST
    @Path("webappexceptionwithcauseandstatus")
    public String exceptionWithCauseAndStatus() {
        throw new WebApplicationException(new GuestbookException("Threw checked exception"), 499);
    }

    @PUT
    @Path("webappexceptionwithcauseandresponse")
    public String exceptionWithCauseAndResponse() {
        Response resp =
                        Response.status(Status.NOT_ACCEPTABLE).entity("Entity inside response").build();
        throw new WebApplicationException(new GuestbookException("Threw checked exception"), resp);
    }

    @DELETE
    @Path("webappexceptionwithcauseandresponsestatus")
    public String exceptionWithCauseAndResponseStatus() {
        throw new WebApplicationException(new GuestbookException("Threw checked exception"),
                                          Response.Status.BAD_REQUEST);
    }

    @GET
    @Path("exceptionmappernull")
    public String exceptionMapperReturnNull() {
        throw new GuestbookNullException("Should not see me");
    }

    @POST
    @Path("exceptionmapperthrowsexception")
    public String exceptionMapperThrowsException() throws GuestbookThrowException {
        throw new GuestbookThrowException("Re-throw an exception");
    }

    @POST
    @Path("exceptionmapperthrowserror")
    public String exceptionMapperThrowsError() throws GuestbookThrowException {
        throw new GuestbookThrowException("Re-throw an error");
    }

    @PUT
    @Path("throwableexceptionmapper")
    public String throwableExceptionMapper() throws GuestbookThrowable {
        throw new GuestbookThrowable();
    }

    @DELETE
    @Path("throwsthrowable")
    public String throwThrowable() throws Throwable {
        throw new Throwable("Throwable was thrown") {

            private static final long serialVersionUID = 1L;

        };
    }
}
