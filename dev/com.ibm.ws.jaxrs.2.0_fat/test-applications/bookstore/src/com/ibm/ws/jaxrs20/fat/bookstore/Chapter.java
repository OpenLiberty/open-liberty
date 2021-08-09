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

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Chapter")
public class Chapter {
    private String title;
    private long id;

    public Chapter() {}

    public void setTitle(String n) {
        title = n;
    }

    public String getTitle() {
        return title;
    }

    public void setId(long i) {
        id = i;
    }

    public long getId() {
        return id;
    }

    @GET
    @Path("/recurse")
    @Produces("application/xml")
    public Chapter getItself() {
        return this;
    }

    @Path("/recurse2")
    public Chapter getItself2() {
        return this;
    }

    @GET
    @Produces("application/xml;charset=ISO-8859-1")
    public Chapter get() {
        return this;
    }

    @GET
    @Path("/ids")
    @Produces("application/xml;charset=ISO-8859-1")
    public Chapter getWithBookId(@PathParam("bookId") int bookId,
                                 @PathParam("chapterid") int chapterId) {
        if (bookId != 123 || chapterId != 1) {
            throw new RuntimeException();
        }
        return this;
    }

    @GET
    @Path("/matched-resources")
    @Produces("text/plain")
    public String getMatchedResources(@Context UriInfo ui) {
        List<String> list = new ArrayList<String>();
        for (Object obj : ui.getMatchedResources()) {
            list.add(obj.toString());
        }
        return list.toString();
    }

    @GET
    @Path("/matched%21uris")
    @Produces("text/plain")
    public String getMatchedUris(@Context UriInfo ui,
                                 @QueryParam("decode") String decode) {
        return ui.getMatchedURIs(Boolean.parseBoolean(decode)).toString();
    }
}
