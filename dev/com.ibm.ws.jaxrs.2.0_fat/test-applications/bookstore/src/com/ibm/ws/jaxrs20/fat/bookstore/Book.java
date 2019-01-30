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
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.CLASS, include = As.PROPERTY, property = "class")
@XmlRootElement(name = "Book")
@XmlSeeAlso(SuperBook.class)
public class Book {
    private String name;
    private long id;
    private final Map<Long, Chapter> chapters = new HashMap<Long, Chapter>();

    public Book() {
        init();
        //System.out.println("----chapters: " + chapters.size());
    }

    public Book(String name, long id) {
        this.name = name;
        this.id = id;
    }

    public void setName(String n) {
        name = n;
    }

    public String getName() {
        return name;
    }

    public void setId(long i) {
        id = i;
    }

    public long getId() {
        return id;
    }

    @PUT
    public void cloneState(Book book) {
        id = book.getId();
        name = book.getName();
    }

    @GET
    public Book retrieveState() {
        return this;
    }

    @GET
    @Path("chapters/{chapterid}/")
    @Produces("application/xml;charset=ISO-8859-1")
    public Chapter getChapter(@PathParam("chapterid") int chapterid) {
        return chapters.get(new Long(chapterid));
    }

    @GET
    @Path("chapters/acceptencoding/{chapterid}/")
    @Produces("application/xml")
    public Chapter getChapterAcceptEncoding(@PathParam("chapterid") int chapterid) {
        return chapters.get(new Long(chapterid));
    }

    @GET
    @Path("chapters/badencoding/{chapterid}/")
    @Produces("application/xml;charset=UTF-48")
    public Chapter getChapterBadEncoding(@PathParam("chapterid") int chapterid) {
        return chapters.get(new Long(chapterid));
    }

    @Path("chapters/sub/{chapterid}/")
    public Chapter getSubChapter(@PathParam("chapterid") int chapterid) {
        return chapters.get(new Long(chapterid));
    }

    @Path("chaptersobject/sub/{chapterid}/")
    public Object getSubChapterObject(@PathParam("chapterid") int chapterid) {
        return getSubChapter(chapterid);
    }

    final void init() {
        Chapter c1 = new Chapter();
        c1.setId(1);
        c1.setTitle("chapter 1");
        chapters.put(c1.getId(), c1);
        Chapter c2 = new Chapter();
        c2.setId(2);
        c2.setTitle("chapter 2");
        chapters.put(c2.getId(), c2);
    }

}
