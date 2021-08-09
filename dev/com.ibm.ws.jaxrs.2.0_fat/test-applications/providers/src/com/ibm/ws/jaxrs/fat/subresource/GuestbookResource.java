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

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("guestbooksubresources")
public class GuestbookResource {
    public static class Page {
        static Map<Double, Page> pages = new HashMap<Double, Page>();
        private final double pageNum;
        private String pageData;

        private synchronized static Page lookup(double num) {
            Page p = pages.get(num);
            if (p == null) {
                p = new Page(num, "");
                pages.put(num, p);
            }
            return p;
        }

        public Page(double pageNum, String data) {
            this.pageNum = pageNum;
            this.pageData = data;
        }

        @GET
        public Response get() {
            return Response.ok(pageData).build();
        }

        @PUT
        public Response put(@QueryParam("data") String data) {
            this.pageData = data;
            return Response.ok().build();
        }

        @DELETE
        public Response delete() {
            pages.remove(pageNum);
            return Response.ok().build();
        }

    }

    @Path("/page/{pageNum}")
    public Page page(@PathParam("pageNum") double pageNum) {
        return Page.lookup(pageNum);
    }

    @GET
    @Path("/page/list")
    public Response listPages() {
        String pages = "";
        for (Double d : Page.pages.keySet()) {
            pages += d + " ";
        }
        return Response.ok(pages).build();
    }
    //
    // @Path("commentdata/{commentid}")
    // @Produces(value = { "text/xml" })
    // public CommentData resolveComment() {
    // return new CommentData(aCommentId, null);
    // }

    @Path("commentdata/{commentid}")
    public Object resolveComment(@PathParam("commentid") String aCommentId) {
        return new CommentData(aCommentId);
    }

    @Path("commentdata")
    public Object resolveComment() {
        return new CommentData(null);
    }

    @GET
    @Path("somecomment")
    @Produces("text/xml")
    public Object getComment(Comment c2) {
        Comment c = new Comment();
        c.setAuthor("Anonymous");
        c.setId(10);
        c.setMessage("Hello World!");
        return c;
    }

}
