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
package com.ibm.ws.jaxrs21.fat.uriInfo;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

public class TestClientRequestResponseFilter implements ClientRequestFilter, ClientResponseFilter {

    @Context
    UriInfo uriInfo;

    private static String pre;
    private static String preUri;

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        pre = requestContext.getUri().toString();
        preUri = uriInfo.getBaseUriBuilder().build().toString();
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        responseContext.getHeaders().putSingle("pre", pre);
        responseContext.getHeaders().putSingle("preUri", preUri);

        responseContext.getHeaders().putSingle("post", requestContext.getUri().toString());
        responseContext.getHeaders().putSingle("postUri", uriInfo.getBaseUriBuilder().build().toString());
    }

}
