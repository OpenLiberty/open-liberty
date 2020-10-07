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
package com.ibm.ws.jaxrs.fat.exceptionMappingWithOT;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

@ApplicationPath("/")
@Path("/exceptionMappingWithOT")
@Produces(MediaType.TEXT_PLAIN)
public class MyAppAndResource extends Application {

    @GET
    public String get() throws Exception {
        System.out.println("MyAppAndResource.get()");
        throw new Exception("Expected Exception");
    }
}
