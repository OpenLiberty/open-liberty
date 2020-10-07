/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxrs21.fat.resourceinfoatstartup;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
@ApplicationScoped
public class Filter1 implements ContainerRequestFilter, ContainerResponseFilter {

    @Context
    ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext reqCtx) throws IOException {
        Class<?> resourceClass = resourceInfo.getResourceClass();
        System.out.println("Filter1(request) - resourceClass=" + resourceClass);
        if (resourceClass == null) {
            reqCtx.abortWith(Response.serverError().build());
        }
    }

    @Override
    public void filter(ContainerRequestContext reqCtx, ContainerResponseContext respCtx) throws IOException {
        Class<?> resourceClass = resourceInfo.getResourceClass();
        System.out.println("Filter1(response) - resourceClass=" + resourceClass);
        if (resourceClass == null) {
            respCtx.setStatus(500);
        }
    }
}

