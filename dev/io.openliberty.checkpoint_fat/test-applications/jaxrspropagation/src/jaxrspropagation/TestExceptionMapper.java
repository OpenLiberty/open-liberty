/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package jaxrspropagation;

import java.io.PrintWriter;
import java.io.StringWriter;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps an exception to an error response in a similar manner to FATServlet
 */
@Provider
public class TestExceptionMapper implements ExceptionMapper<Throwable> {

    @Context
    private UriInfo uriInfo;

    /** {@inheritDoc} */
    @Override
    public Response toResponse(Throwable t) {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            System.out.println("ERROR: " + t);
            pw.println("ERROR: Caught exception attempting to call " + uriInfo.getRequestUri().toString());
            t.printStackTrace(pw);
        }
        return Response.ok(sw.toString()).build();
    }
}
