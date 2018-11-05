/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jaxrs.fat.microProfileSessionScopedClaimInjectString;

import java.io.PrintWriter;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.Claims;

import com.ibm.ws.security.fat.common.Constants;

// http://localhost:8010/microProfileApp/rest/Injection/MicroProfileInjectionApp
// allow the same methods to invoke GET, POST, PUT, ... invocation type determines which gets invoked.

@Path("microProfileSessionScopedClaimInjectString")
@SessionScoped
public class MicroProfileApp extends Application {

    PrintWriter pw = null;

    // Raw types
    @Inject
    @Claim(standard = Claims.raw_token)
    private String rawToken;

    /**
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String myGetter(@PathParam("id") String id) {

        try {
            return doWorker(Constants.GETMETHOD);
        } catch (Exception e) {
            System.out.println("GET catch");
            return e.toString();

        }
    }

    /**
     */
    @POST
    @Path("{id}")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes("application/x-www-form-urlencoded")
    public String myPoster(@PathParam("id") String id) {
        try {
            return doWorker(Constants.POSTMETHOD);
        } catch (Exception e) {
            System.out.println("POST catch");
            return e.toString();

        }
    }

    @PUT
    @Path("{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String myPutter(@PathParam("id") String id) {
        try {
            return doWorker(Constants.PUTMETHOD);
        } catch (Exception e) {
            System.out.println("PUT catch");
            return e.toString();

        }
    }

    protected String doWorker(String requestType) {

        String returnMsg = "Executed doWorker in " + this.getClass().getCanonicalName() ;
        System.out.println(returnMsg);
        return returnMsg;

    }

}
