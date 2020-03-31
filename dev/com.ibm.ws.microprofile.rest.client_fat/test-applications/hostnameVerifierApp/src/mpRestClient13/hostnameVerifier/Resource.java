/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package mpRestClient13.hostnameVerifier;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

@ApplicationPath("/")
@Path("/resource")
@Produces("text/plain")
public class Resource extends Application {

    @Path("/canAccessDespiteWrongHostname")
    @GET
    public Response canAccessDespiteWrongHostname() {
        return Response.ok("you made it here!").build();
    }
}