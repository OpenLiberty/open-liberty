/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.authorization.rest.annotations;

import java.io.PrintWriter;
import java.io.StringWriter;

import io.openliberty.security.authorization.fat.RestContextWrapper;
import io.openliberty.security.authorization.fat.TestUtil;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

@Path("/resource2")
public class RestResource2 {

    @Inject
    private jakarta.security.enterprise.SecurityContext securityContext;

    @Context
    private jakarta.ws.rs.core.SecurityContext restSecContext;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed({ "RestRole2" })
    @Path("/doTest")
    public String doTest() {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        TestUtil.printSecurityData(out, new RestContextWrapper(restSecContext), securityContext);
        out.flush();
        return sw.toString();

    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed({ "RestRole4" })
    @Path("/doTest2")
    public String doTest2() {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        TestUtil.printSecurityData(out, new RestContextWrapper(restSecContext), securityContext);
        out.flush();
        return sw.toString();

    }
}
