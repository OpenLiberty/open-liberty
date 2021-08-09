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
package com.ibm.ws.jaxrs.fat.exceptionmappers.nomapper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

/**
 * The main JAX-RS resource.
 */
@Path("guestbooknomap")
public class Guestbook {

    private static class MyWebAppException extends WebApplicationException {

        private static final long serialVersionUID = -2022185988670037226L;

        final private Response resp;

        public MyWebAppException(int status) {
            CommentError error = new CommentError();
            error.setErrorMessage("Cannot post an invalid message.");
            resp = Response.status(status).type("text/xml").entity(error).build();
        }

        @Override
        public Response getResponse() {
            return resp;
        }
    }

    /**
     * Adds a new message to the database.
     *
     * @return HTTP status 200
     */
    @POST
    @Consumes({ "text/xml" })
    @Produces({ "text/xml" })
    public Response createMessage(Comment aMessage, @Context UriInfo uriInfo) {
        if (aMessage == null) {
            WebApplicationException webAppException =
                            new WebApplicationException(Status.BAD_REQUEST);
            throw webAppException;
        }

        if (aMessage.getMessage() == null && aMessage.getAuthor() == null) {
            throw new WebApplicationException();
        }

        if (aMessage.getMessage() == null) {
            CommentError error = new CommentError();
            error.setErrorMessage("Missing the message in the comment.");
            Response malformedCommentResponse =
                            Response.status(Status.BAD_REQUEST).entity(error).type("text/xml").build();
            WebApplicationException webAppException =
                            new WebApplicationException(malformedCommentResponse);
            throw webAppException;
        }

        if (aMessage.getAuthor() == null) {
            WebApplicationException webAppException = new WebApplicationException(499);
            throw webAppException;
        }

        if ("".equals(aMessage.getMessage())) {
            throw new MyWebAppException(498);
        }

        /*
         * Set the message id to a server decided message, even if the client
         * set it.
         */
        int id = GuestbookDatabase.getGuestbook().getAndIncrementCounter();
        aMessage.setId(id);

        GuestbookDatabase.getGuestbook().storeComment(aMessage);
        try {
            return Response.created(new URI(uriInfo.getAbsolutePath() + "/" + aMessage.getId()))
                            .entity(aMessage).type(MediaType.TEXT_XML).build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @PUT
    @Path("{id}")
    public Response updateMessage(Comment aMessage, @PathParam("id") String msgId)
                    throws GuestbookException {
        /*
         * If no message data was sent, then return the null request.
         */
        if (aMessage == null) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        if (aMessage.getId() == null || !aMessage.getId().equals(msgId)) {
            throw new GuestbookException("Unexpected ID.");
        }

        Comment existingComment =
                        GuestbookDatabase.getGuestbook().getComment(Integer.valueOf(msgId));
        if (existingComment == null) {
            throw new GuestbookException("Cannot find existing comment to update.");
        }
        GuestbookDatabase.getGuestbook().storeComment(aMessage);
        return Response.ok(aMessage).build();
    }

    @GET
    @Path("/{id}")
    @Produces({ "text/xml" })
    public Response readMessage(@PathParam("id") String msgId) {
        Comment msg = GuestbookDatabase.getGuestbook().getComment(Integer.valueOf(msgId));
        if (msg == null) {
            return Response.status(404).build();
        }

        return Response.ok(msg).build();
    }

    @DELETE
    @Path("{id}")
    public Response deleteMessage(@PathParam("id") String msgId) {
        // NumberFormatException thrown here, most likely, when
        // ExceptionMappersTest.testRuntimeExceptionNoMappingProvider() runs
        GuestbookDatabase.getGuestbook().deleteComment(Integer.valueOf(msgId));
        return Response.noContent().build();
    }

    @POST
    @Path("/clear")
    public void clearMessages() {
        Collection<Integer> keys = GuestbookDatabase.getGuestbook().getCommentKeys();
        for (Integer k : keys) {
            GuestbookDatabase.getGuestbook().deleteComment(k);
        }
        GuestbookDatabase.getGuestbook().resetCounter();
    }
}
