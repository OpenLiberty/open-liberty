/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.monitor.fat.restApp;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * Fail Resource
 */
@ApplicationScoped
@Path("/fail")
public class FailResource {

    @GET
    @Path("/zero")
    public String failGet() throws Exception {
        int x = 5 / 0;
        return "I tried to divide by 0!";
    }

    @GET
    @Path("/io")
    public String throwIO() throws Exception {
        throw new IOException("throw IO");
    }

    @GET
    @Path("/iae")
    public String throwIAE() throws Exception {
        throw new IllegalArgumentException("throw IAE");
    }

}
