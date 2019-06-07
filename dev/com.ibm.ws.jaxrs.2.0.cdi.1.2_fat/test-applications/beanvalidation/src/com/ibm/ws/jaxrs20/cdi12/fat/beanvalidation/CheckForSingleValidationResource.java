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
package com.ibm.ws.jaxrs20.cdi12.fat.beanvalidation;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/singleValidation")
public class CheckForSingleValidationResource {

    private final static boolean isPreBeanVal20;
    static {
        boolean b = false;
        try {
            Class.forName("javax.validation.valueextraction.Unwrapping"); // bean val 2.0-specific class
        } catch (Throwable t) {
            b = true;
        }
        isPreBeanVal20 = b;
    }
    @Context
    HttpServletRequest request;
    
    @GET
    @Path("/test")
    @Produces(MediaType.TEXT_PLAIN)
    public Response test(@QueryParam("test") String test) {
        System.out.println("test = " + test);
        SomeObject someObject;
        if ("validParamAndReturn".equals(test)) {
            if (isPreBeanVal20) { // easy out for bean-validation 1.1
                return Response.ok("VALID 1:1").build();
            }
            someObject = new SomeObject(0, "Hello");
        } else if ("invalidReturn".contentEquals(test)) {
            if (isPreBeanVal20) { // easy out for bean-validation 1.1
                return Response.ok("INVALID 1:1").build();
            }
            someObject = new SomeObject(1, "Greetings");
        } else if ("invalidParam".equals(test)) {
            if (isPreBeanVal20) { // easy out for bean-validation 1.1
                return Response.ok("INVALID 1:0").build();
            }
            someObject = new SomeObject(2, "Hi");
        } else {
            return Response.status(404).entity("Unknown test case: " + test).build();
        }
        
        StringBuilder sb = new StringBuilder();
        try {
            SomeReturnObject returnObject = ClientBuilder.newClient()
                                                         .target("http://localhost:" + request.getLocalPort() + 
                                                                 "/beanvalidation/rest/singleValidation/check")
                                                         .request(MediaType.APPLICATION_JSON)
                                                         .post(Entity.json(someObject), SomeReturnObject.class);
            sb.append("VALID ");
        } catch (Throwable t) {
            t.printStackTrace();
            sb.append("INVALID ");
        }
        
        sb.append(SomeObjectValidator.invocationCount.getAndSet(0))
          .append(":")
          .append(SomeReturnObjectValidator.invocationCount.getAndSet(0));
        
        String s = sb.toString();
        System.out.println(s);
        return Response.ok(s).build();
    }
    
    @POST
    @Path("/check")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Valid
    public SomeReturnObject checkForMoreThanOneValidationInvocation(@Valid SomeObject someObject) {
        if ("Greetings".equals(someObject.getString())) {
            return new SomeReturnObject(0, "Invalid Response");
        }
        return new SomeReturnObject(1, "Hi");
    }
}
