/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jaxrs.fat.microProfileApp.PropagationClient;

import java.io.PrintWriter;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;

// http://localhost:<nonSecurePort>/microProfileApp/rest/microProfileNoLoginConfig/MicroProfileApp
// allow the same methods to invoke GET, POST, PUT, ... invocation type determines which is invoked.

@Path("propagationClient")
@RequestScoped
public class MicroProfileApp {

    PrintWriter pw = null;

    /**
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String myGetter(@Context HttpHeaders headers,
            @PathParam("id") String id,
            @DefaultValue("notSet") @FormParam("targetApp") String appToCall,
            @DefaultValue("notSet") @FormParam("where") String where) {

        try {
            System.out.println("token in header: " + headers.getRequestHeader("Authorization"));
            return doWorker(Constants.GETMETHOD, appToCall, where);
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
    public String myPoster(@Context HttpHeaders headers,
            @PathParam("id") String id,
            @DefaultValue("notSet") @FormParam("targetApp") String appToCall,
            @DefaultValue("notSet") @FormParam("where") String where) {
        try {
            System.out.println("token in header: " + headers.getRequestHeader("Authorization"));
            return doWorker(Constants.POSTMETHOD, appToCall, where);
        } catch (Exception e) {
            System.out.println("POST catch");
            return e.toString();

        }
    }

    @PUT
    @Path("{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String myPutter(@Context HttpHeaders headers,
            @PathParam("id") String id,
            @DefaultValue("notSet") @FormParam("targetApp") String appToCall,
            @DefaultValue("notSet") @FormParam("where") String where) {
        try {
            System.out.println("token in header: " + headers.getRequestHeader("Authorization"));
            return doWorker(Constants.PUTMETHOD, appToCall, where);
        } catch (Exception e) {
            System.out.println("PUT catch");
            return e.toString();

        }
    }

    protected String doWorker(String requestType, String appToCall, String where) {

        String returnMsg = "Executed doWorker in " + this.getClass().getCanonicalName() + " special case";
        String localResponse = null;
        System.out.println("Where is set to: " + where);

        try {
            Client client = ClientBuilder.newClient();

            /*
             * check if caller requested that com.ibm.ws.jaxrs.client.mpjwt.sendToken be set and
             * if set, how it should be set
             * The client property can take either the boolean or string versions of true/false
             */
            if (where.equals(MpJwtFatConstants.PROPAGATE_TOKEN_BOOLEAN_TRUE)) {
                System.out.println("Setting client property: " + where);
                client.property(MpJwtFatConstants.CLIENT_SEND_TOKEN_PROPERTY, true);
            }
            if (where.equals(MpJwtFatConstants.PROPAGATE_TOKEN_STRING_TRUE)) {
                System.out.println("Setting client property: " + where);
                client.property(MpJwtFatConstants.CLIENT_SEND_TOKEN_PROPERTY, "true");
            }
            if (where.equals(MpJwtFatConstants.PROPAGATE_TOKEN_BOOLEAN_FALSE)) {
                System.out.println("Setting client property: " + where);
                client.property(MpJwtFatConstants.CLIENT_SEND_TOKEN_PROPERTY, false);
            }
            if (where.equals(MpJwtFatConstants.PROPAGATE_TOKEN_STRING_FALSE)) {
                System.out.println("Setting client property: " + where);
                client.property(MpJwtFatConstants.CLIENT_SEND_TOKEN_PROPERTY, "false");
            }

            /* invoke the requested app */
            returnMsg = returnMsg + getNewLine() + "Calling: " + appToCall;
            WebTarget myResource = client.target(appToCall);
            localResponse = myResource.request(MediaType.TEXT_PLAIN).get(String.class);

        } catch (Exception e) {
            e.printStackTrace();
            localResponse = "Caught an exception invoking remote appr : " + e.getLocalizedMessage();

        }

        returnMsg = returnMsg + getNewLine() + localResponse;
        System.out.println(returnMsg);
        return returnMsg;
    }

    protected String getNewLine() {
        return System.getProperty("line.separator");
    }
}
