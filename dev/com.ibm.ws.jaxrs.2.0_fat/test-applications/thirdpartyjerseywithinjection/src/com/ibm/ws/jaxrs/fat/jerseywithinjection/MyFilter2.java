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

import java.io.IOException;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.sql.DataSource;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

@Provider
@ApplicationScoped
public class MyFilter2 implements ContainerRequestFilter, ContainerResponseFilter {

   @Resource(description = "Application Data Source", name = "jdbc/TestDataSource")
   private DataSource datasource;

    @Override
    public void filter(ContainerRequestContext reqCtx) throws IOException {
        System.out.println("MyFilter2(request) - datasource=" + datasource.toString());
    }

    @Override
    public void filter(ContainerRequestContext reqCtx, ContainerResponseContext respCtx) throws IOException {
        System.out.println("MyFilter2(response) - datasource=" + datasource.toString());
    }
}

