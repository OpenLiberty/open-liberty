/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.audit.internal.fat.app;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Simple stateless REST endpoint. It deliberately does not use HttpSession.
 * When the audit feature is enabled with generateNewSession=true (default),
 * the audit code creates a new session and the response will contain a
 * Set-Cookie: JSESSIONID header. With generateNewSession=false no new session
 * is created and no JSESSIONID cookie is returned.
 */
@Path("/hello")
public class HelloResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "SUCCESS";
    }
}
