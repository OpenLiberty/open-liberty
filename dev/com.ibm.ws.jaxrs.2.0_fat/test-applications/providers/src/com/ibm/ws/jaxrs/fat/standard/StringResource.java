/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.standard;

import java.io.IOException;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("providers/standard/string")
public class StringResource {

    @POST
    @Path("/empty")
    public Response postString(String str) throws IOException {
        if (str != null && str.length() == 0) {
            return Response.ok("expected").build();
        }
        return Response.serverError().build();
    }
}
