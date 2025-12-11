/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi40.fat.versionwarning.app;

import org.eclipse.microprofile.openapi.annotations.callbacks.Callback;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/")
public class VersionWarningResource {

    @GET
    @Callback(name = "test", callbackUrlExpression = "/callback/test", pathItemRef = "testPathItem")
    public String getTest() {
        return "test";
    }
}
