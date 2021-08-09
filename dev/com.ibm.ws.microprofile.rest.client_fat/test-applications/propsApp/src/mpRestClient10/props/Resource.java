/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpRestClient10.props;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

@ApplicationPath("/")
@Path("/resource")
public class Resource extends Application implements PropChecker {

    @Context
    HttpHeaders headers;
    
    /* (non-Javadoc)
     * @see mpRestClient10.props.PropChecker#checkKeepAliveProp()
     */
    @Override
    @Path("/checkKeepAliveProp")
    @GET
    public Response checkKeepAliveProp() {
        return Response.ok(headers.getHeaderString("Connection")).build();
    }

}
