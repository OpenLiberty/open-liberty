/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxrs21.fat.mutableHeaders;

import static jaxrs21.fat.mutableHeaders.ContainerRequestFilter1.NEW;
import static jaxrs21.fat.mutableHeaders.ContainerRequestFilter1.PREEXISTING;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(2)
public class ContainerRequestFilter2 implements ContainerRequestFilter {

    /* (non-Javadoc)
     * @see javax.ws.rs.container.ContainerRequestFilter#filter(javax.ws.rs.container.ContainerRequestContext)
     */
    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        MultivaluedMap<String, String> headers = ctx.getHeaders();
        String preexisting = headers.getFirst(PREEXISTING);
        if (preexisting == null) {
            ctx.abortWith(Response.status(503).entity(PREEXISTING + " header not found 2").build());
        }
        if (!"modified".equals(preexisting)) {
            ctx.abortWith(Response.status(503).entity(PREEXISTING + " header found but not modified").build());
        }

        String newHeader = headers.getFirst(NEW);
        if (newHeader == null) {
            ctx.abortWith(Response.status(503).entity(NEW + " header not found 2").build());
        }
        if (!"newHeader".equals(newHeader)) {
            ctx.abortWith(Response.status(503).entity(NEW + " header has unexpected value: " + newHeader).build());
        }
        ctx.abortWith(Response.ok("SUCCESS").build());
    }

}
