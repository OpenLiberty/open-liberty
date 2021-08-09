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

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(1)
public class ContainerRequestFilter1 implements ContainerRequestFilter {
    static final String PREEXISTING = "PreExisting";
    static final String NEW = "New";

    /* (non-Javadoc)
     * @see javax.ws.rs.container.ContainerRequestFilter#filter(javax.ws.rs.container.ContainerRequestContext)
     */
    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        MultivaluedMap<String, String> headers = ctx.getHeaders();
        if (!headers.containsKey(PREEXISTING)) {
            ctx.abortWith(Response.status(503).entity(PREEXISTING + " header not found").build());
        }
        headers.putSingle(PREEXISTING, "modified");

        headers.putSingle(NEW, "newHeader");
    }

}
