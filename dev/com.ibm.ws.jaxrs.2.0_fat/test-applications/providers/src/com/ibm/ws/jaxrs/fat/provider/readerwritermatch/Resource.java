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
package com.ibm.ws.jaxrs.fat.provider.readerwritermatch;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBElement;
import javax.xml.transform.Source;

@Path("resource")
public class Resource {

    public static final String HEADERNAME = "FILTER_HEADER";
    public static final String DIRECTION = "FROM_RESOURCE";

    @Path("source")
    @POST
    public Source source(Source source) {
        return source;
    }

    @Path("jaxb")
    @POST
    public JAXBElement<String> jaxb(JAXBElement<String> jaxb) {
        return jaxb;
    }

    @Path("map")
    @POST
    public MultivaluedMap<String, String> map(MultivaluedMap<String, String> map) {
        return map;
    }

    @Path("character")
    @POST
    public Character character(Character character) {
        if (character != 'a')
            throw new WebApplicationException(Status.NOT_ACCEPTABLE);
        return character;
    }

    @GET
    @Path("text")
    @Produces(value = "text/*")
    public String geText() {
        return "text/* is ok";
    }

    @Path("boolean")
    @POST
    public Boolean bool(Boolean bool) {
        if (bool)
            throw new WebApplicationException(Status.NOT_ACCEPTABLE);
        return false;
    }

    @POST
    @Path("postbytearray")
    public Response postByteArray(byte[] array) {
        return buildResponse(new String(array));
    }

    @GET
    @Path("getboolean")
    public Response getBoolean() {
        Boolean b = false;
        return buildResponse(b, MediaType.TEXT_PLAIN_TYPE);
    }

    @POST
    @Path("boolean")
    public Object postBoolean(Boolean bool) {
        if (bool) {
            System.out.println("=================406=============");
            throw new WebApplicationException(Status.NOT_ACCEPTABLE);
        }
        System.out.println("=================200=============");
        return false;
    }

    @GET
    @Path("getchar")
    public Response getChar() {
        Character c = 'R';
        return buildResponse(c, MediaType.TEXT_PLAIN_TYPE);
    }

    @POST
    @Path("postchar")
    public Response postChar(char c) {
        String text = String.valueOf(c);
        return buildResponse(text);
    }

    // ///////////////////////////////////////////////////////////////////////////
    // Send header that would have the power to enable filter / interceptor
    // The header is passed from client request
    @Context
    private HttpHeaders headers;

    private Response buildResponse(Object content) {
        return buildResponse(content, MediaType.WILDCARD_TYPE);
    }

    private Response buildResponse(Object content, MediaType type) {
        List<String> list = headers.getRequestHeader(HEADERNAME);
        String name = null;
        if (list != null && list.size() != 0)
            name = list.iterator().next();
        ResponseBuilder builder = Response.ok(content, type).type(type);
        if (name != null)
            builder.header(HEADERNAME, name + DIRECTION);
        return builder.build();
    }

}
