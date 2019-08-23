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
package com.ibm.ws.security.jwt.fat.mpjwt;

import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * A JAX-RS application marked as requiring MP-JWT authentication
 *
 * (There should only be one LoginConfig annotation per module, or processing
 * will be indeterminate.)
 */
public class CommonMicroProfileApp {

    PrintWriter pw = null;
    
    @Context
    HttpServletRequest request;

    /**
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String myGetter(@PathParam("id") String id) {

        try {
            String result = doWorker(MpJwtFatConstants.GETMETHOD);
            if(id.equals("logout")) {
                System.out.println("*** logging out, request is "+ request);
                try {
                    request.logout();
                } catch (ServletException se) {
                    System.out.println("Caught exception: " + se);
                    se.printStackTrace();
                }
            }
            return result;
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
            return doWorker(MpJwtFatConstants.POSTMETHOD);
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
            return doWorker(MpJwtFatConstants.PUTMETHOD);
        } catch (Exception e) {
            System.out.println("PUT catch");
            return e.toString();

        }
    }

    protected String doWorker(String requestType) {

        String returnMsg = "Executed doWorker in " + this.getClass().getCanonicalName();
        System.out.println(returnMsg);
        return returnMsg;

    }
}
