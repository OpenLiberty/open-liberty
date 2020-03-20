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
package jaxrs21.fat.resourceinfoatstartup;

import javax.annotation.Resource;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

@Path("/1")
@RequestScoped
public class Resource1 {

    @Inject
    InjectableObject injectableObject1;
    @Resource(description = "Application Data Source", name = "jdbc/TestDataSource")
    private DataSource datasource;
    @Context
    private Application application;

    @GET
    public String getResource1() {
        int sleepTimeMillis = (int) (Math.random() * 5000);
        try {
            Thread.sleep(sleepTimeMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Resource1(getResource1) - injectableObject1 " + injectableObject1.getSomething() );
        System.out.println("Resource1(getResource1) - datasource " + datasource.toString() );
        System.out.println("Resource1(getResource1) - application " + application.toString() );

        return Resource1.class.getSimpleName();
    }
}

