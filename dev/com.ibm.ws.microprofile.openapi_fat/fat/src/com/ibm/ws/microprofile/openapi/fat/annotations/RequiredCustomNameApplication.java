/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.fat.annotations;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@ApplicationPath("/")
@Path("/")
public class RequiredCustomNameApplication extends Application {

    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public RequiredCustomNameDataObject testGet() {
        return new RequiredCustomNameDataObject();
    }

    public static class RequiredCustomNameDataObject {

        @Schema(name = "test_a", required = true)
        public String testA;

        @Schema(required = true)
        public String testB;
    }
}
