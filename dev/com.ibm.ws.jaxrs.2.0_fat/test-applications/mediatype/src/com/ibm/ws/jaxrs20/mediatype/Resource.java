/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.mediatype;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

@Path("resource")
@Consumes("text/plain")
@Produces("text/plain")
public class Resource {

    @Context
    HttpHeaders headers;

    @POST
    @Path("class")
    public String consumesTextPlainProducesTextPlain(String content) {
        return headers(content);
    }

    @POST
    @Path("consumeXml")
    @Consumes({"text/xml", "application/xml"})
    public String consumesTextXmlProducesTextPlain(String content) {
        return headers(content);
    }

    @POST
    @Path("produceXml")
    @Produces("text/xml")
    public String consumesTextPlainProducesTextXml(String content) {
        return "<text>" + headers(content) + "</text>";
    }

    private String headers(String content) {
        System.out.println("Content: " + content);
        String accept = headers.getHeaderString(HttpHeaders.ACCEPT);
        String contenttype = headers.getHeaderString(HttpHeaders.CONTENT_TYPE);
        return String.format("Accept: %s - Content-Type: %s", accept, contenttype);
    }
}
