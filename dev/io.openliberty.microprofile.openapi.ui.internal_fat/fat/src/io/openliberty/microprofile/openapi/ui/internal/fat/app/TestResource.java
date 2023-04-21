/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi.ui.internal.fat.app;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * Very basic application so that we have something to look at in the OpenAPI UI
 */
@ApplicationPath("/")
@Path("/test")
public class TestResource {

    @GET
    public String testGet() {
        return "OK";
    }
}
