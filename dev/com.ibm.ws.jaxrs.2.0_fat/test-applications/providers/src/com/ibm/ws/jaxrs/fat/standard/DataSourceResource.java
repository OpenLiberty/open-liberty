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

import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("providers/standard/datasource")
public class DataSourceResource {

    private DataSource ds = null;

    @GET
    public Response getDataSource() {
        return Response.ok(ds).build();
    }

    @POST
    public DataSource postDataSource(DataSource ds) {
        return ds;
    }

    @PUT
    public void putDataSource(DataSource ds) {
        this.ds = ds;
    }

    @POST
    @Path("subclass/should/fail")
    public DataSource postDataSource(FileDataSource ds) {
        return ds;
    }
}
