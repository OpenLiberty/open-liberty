/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package typeAndCType;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;

@ApplicationPath("/")
@Path("/path")
public class AppAndResource extends Application {

    @Path("/accept")
    @GET
    public String getAcceptHeader(@HeaderParam(HttpHeaders.ACCEPT)String accept) {
        return accept;
    }

    @Path("/contentType")
    @POST
    public String getContentTypeHeader(@HeaderParam(HttpHeaders.CONTENT_TYPE)String contentType, String data) {
        return contentType;
    }
}
