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

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;

@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter, ClientRequestFilter, ClientResponseFilter {

    @Override
    public void filter(ClientRequestContext req, ClientResponseContext res) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void filter(ClientRequestContext req) throws IOException {
        System.out.println("-> Client sending - Accept: " + req.getHeaderString(HttpHeaders.ACCEPT)
                           + " - Content-Type: " + req.getHeaderString(HttpHeaders.CONTENT_TYPE));
    }

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void filter(ContainerRequestContext req) throws IOException {
        System.out.println("-> Server received - Accept: " + req.getHeaderString(HttpHeaders.ACCEPT)
        + " - Content-Type: " + req.getHeaderString(HttpHeaders.CONTENT_TYPE));

    }

}
