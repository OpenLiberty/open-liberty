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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

/**
 * REST endpoint that deliberately calls getSession() from application code.
 * Used to verify that when generateNewSession=false, the audit service still
 * records the session ID when a session already exists — because the session
 * was created by the application, not by the audit code.
 */
@Path("/session")
public class SessionResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String createSession(@Context HttpServletRequest request) {
        // The application itself creates the session — this is the normal,
        // intentional case that generateNewSession=false must not break.
        HttpSession session = request.getSession(true);
        return session.getId();
    }
}
