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
package com.ibm.ws.jaxrs.fat.jerseywithinjection;

import javax.annotation.Resource;
import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/helloworld2")
public class MyResource2 {

    @Resource(description = "Application Data Source", name = "jdbc/TestDataSource")
    private DataSource datasource;

    @GET
    public String get() {
        System.out.println("MyResource2(get) - datasource " + datasource.toString());
        return "Hello World2!";
    }

    @POST
    @Path("post1")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TestEntity2 post1(TestEntity2 entity) {
        System.out.println("MyResource2(post1)");
        return entity;
    }
}
