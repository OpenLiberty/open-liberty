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
package io.openliberty.restfulWS30.fat.exception;

import jakarta.ejb.EJBException;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response.Status;

@ApplicationPath("/app")
@Path("/path")
@Produces("text/plain")
public class Resource extends Application {

    @GET
    public String foo() {
        System.out.println("foo invoked!");
        return "foo";
    }
    
    @Path("exception")
    @GET
    public String throwException() {
      throw new EJBException(new WebApplicationException(Status.CREATED));
    }
    
}
