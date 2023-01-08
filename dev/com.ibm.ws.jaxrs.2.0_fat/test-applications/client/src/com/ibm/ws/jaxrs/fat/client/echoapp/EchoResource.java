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
package com.ibm.ws.jaxrs.fat.client.echoapp;

import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import javax.ws.rs.core.Variant.VariantListBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import com.ibm.ws.jaxrs.fat.client.jaxb.Echo;

@Path("/echoaccept")
public class EchoResource {
    private static Logger LOG = Logger.getLogger(EchoResource.class.getName());

    @Context
    HttpHeaders requestHeaders;

    @GET
    public Response getAcceptHeaderEcho(@Context Request request) throws JSONException {
        StringBuffer sb = new StringBuffer("echo: ");
        List<String> acceptHeader = requestHeaders.getRequestHeader(HttpHeaders.ACCEPT);

        if (acceptHeader != null) {
            LOG.info(() -> "Accept: " + acceptHeader);
            for (String s : acceptHeader) {
                sb.append(s);
            }
        }

        if (acceptHeader == null || acceptHeader.isEmpty()
            || MediaType.WILDCARD_TYPE.equals(requestHeaders.getAcceptableMediaTypes().get(0))) {
            LOG.info(() -> "Nothing or */* from Accept header");
            return Response.ok(sb.toString()).type(MediaType.TEXT_PLAIN_TYPE).build();
        }

        Variant variant =
                        request.selectVariant(VariantListBuilder.newInstance()
                                        .mediaTypes(MediaType.TEXT_PLAIN_TYPE,
                                                    MediaType.TEXT_XML_TYPE,
                                                    MediaType.APPLICATION_JSON_TYPE).add().build());
        if (variant != null) {
            if (MediaType.APPLICATION_JSON_TYPE.isCompatible(variant.getMediaType())) {
                LOG.info(() -> "Variant is compatible with application/json");
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("value", sb.toString());
                return Response.ok(jsonObject).type(MediaType.APPLICATION_JSON).build();
            } else if (MediaType.TEXT_XML_TYPE.isCompatible(variant.getMediaType())) {
                LOG.info(() -> "Variant is compatible with text/xml");
                Echo e = new Echo();
                e.setValue(sb.toString());
                return Response.ok(e).type(MediaType.TEXT_XML).build();
            }
            LOG.info(() -> "Variant is not null, but not compatible with application/json nor text/xml");
        }

        return Response.ok(sb.toString()).type(MediaType.TEXT_PLAIN_TYPE).build();
    }
}
