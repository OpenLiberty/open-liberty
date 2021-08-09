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
package com.ibm.ws.jaxrs.fat.params;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * To test that JAX-RS runtime can match request to
 * subresource methods with the same regular expression,
 * but different capture variable names. None of the parameters
 * should have null values.
 */
@Path("diffvarnames")
public class DiffCaptureVariablesParamsResource {

    // See https://issues.apache.org/jira/browse/WINK-344 for history
    @GET
    @Path("{id1}")
    public String getMethod(@PathParam("id1") String id) {
        return "id1_" + id;
    }

    @POST
    @Path("{id2}/post")
    public String doSomething(@PathParam("id2") String id) {
        return "id2_" + id;
    }

    @DELETE
    @Path("{id3}")
    public String deleteMethod(@PathParam("id3") String id) {
        return "id3_" + id;
    }
}
