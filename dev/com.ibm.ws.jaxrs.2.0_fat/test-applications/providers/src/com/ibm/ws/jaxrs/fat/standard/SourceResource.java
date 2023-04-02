/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.standard;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

@Path("providers/standard/source")
public class SourceResource {

    private static Source source = null;

    @GET
    @Produces("text/xml")
    public Response getSource() {
        return Response.ok(source).type("text/xml").build();
    }

    @POST
    public Source postSource(Source src) {
        return src;
    }

    public static class UnsupportedSourceSubclass implements Source {

        @Override
        public String getSystemId() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setSystemId(String systemId) {
            // TODO Auto-generated method stub

        }

    }

    @POST
    @Path("/subclasses/shouldfail")
    public UnsupportedSourceSubclass postReader(UnsupportedSourceSubclass saxSource) {
        return saxSource;
    }

    @PUT
    public void putSource(Document doc) throws IOException {
        SourceResource.source = new DOMSource(doc);
    }

    @POST
    @Path("/empty")
    public Response postReader(Source source) throws IOException {
        if (source != null) {
            if (source instanceof SAXSource) {
                SAXSource s = (SAXSource) source;
                if (s.getInputSource().getByteStream() == null) {
                    return Response.ok("expected").build();
                }
            } else if (source instanceof StreamSource) {
                if (-1 == ((StreamSource)source).getInputStream().read()) {
                    return Response.ok("expected").build();
                }
            }
        }
        return Response.serverError().build();
    }
}
