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
package com.ibm.ws.jaxrs.fat.subresource;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

public class CommentData {

    final private String commentId;

    public CommentData(String id) {
        this.commentId = id;
    }

    @GET
    @Produces("text/xml")
    public Response retrieveComment() {
        if (commentId == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        Comment existingComment = GuestbookDatabase.getGuestbook().getComment(Integer.valueOf(commentId));
        if (existingComment == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(existingComment).build();
    }

    @POST
    @Produces("text/xml")
    @Consumes("text/xml")
    public Response createComment(Comment comment, @Context UriInfo uriInfo) {
        /*
         * If no comment data was sent, then return a bad request.
         */
        if (comment == null) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        if (comment.getId() == null || comment.getMessage() == null || comment.getAuthor() == null) {
            CommentError commentError = new CommentError();
            commentError.setErrorMessage("Please include a comment ID, a message, and your name.");
            Response resp = Response.status(Response.Status.BAD_REQUEST).entity(commentError).type("application/xml").build();
            throw new WebApplicationException(resp);
        }

        GuestbookDatabase.getGuestbook().storeComment(comment);
        try {
            return Response.created(new URI(uriInfo.getAbsolutePath() + "/" + comment.getId())).entity(comment).build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new WebApplicationException(e);
        }
    }

    @PUT
    public Response updateComment(Comment comment) throws GuestbookException {
        if (comment.getId() == null || !Integer.valueOf(commentId).equals(comment.getId())) {
            throw new GuestbookException("Unexpected ID.");
        }

        Comment existingComment = GuestbookDatabase.getGuestbook().getComment(Integer.valueOf(comment.getId()));
        if (existingComment == null) {
            throw new GuestbookException("Cannot find existing comment to update.");
        }
        GuestbookDatabase.getGuestbook().storeComment(comment);
        return Response.ok(comment).build();
    }

    @DELETE
    public void deleteComment() {
        GuestbookDatabase.getGuestbook().deleteComment(Integer.valueOf(commentId));
    }

    @OPTIONS
    public Response checkOptions() {
        System.out.println("Invoked CommentData.checkOptions");
        return Response.ok("DELETE | GET | POST | PUT").build();
    }
}
