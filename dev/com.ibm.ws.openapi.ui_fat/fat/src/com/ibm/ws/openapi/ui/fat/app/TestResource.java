/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.openapi.ui.fat.app;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;

/**
 * Very basic application so that we have something to look at in the OpenAPI UI
 */
@OpenAPIDefinition(info = @Info(title = "UI Test App", version = "0.1"))
@ApplicationPath("/")
@Path("/test")
public class TestResource {

    @GET
    public String testGet() {
        return "OK";
    }
}
