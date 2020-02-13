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
package jaxrs21.fat.contextandclient;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path("/resource")
@Produces(MediaType.TEXT_PLAIN)
public class LocalResource {

    @Context
    HttpServletRequest req;

    @Inject
    Client client;

    @Path("/invokeClient")
    @GET
    public String invokeClient() {
        String response = "PASS";
        System.out.println(req);
        String serverNamePre = req.getServerName();
        String remoteUri = "http://" + serverNamePre + ":" + req.getServerPort() + "/contextandclient/resource/remote";
        String s = client.get(remoteUri);
        System.out.println("Received from remote: " + s);
        if (s == null) {
            response = "FAIL";
        }
        String serverNamePost = req.getServerName();
        System.out.println("From LocalResource(" + serverNamePost + ")");
        if (serverNamePost == null || !serverNamePre.equals(serverNamePost)) {
            response = "FAIL";
        }
        return response;
    }

    @GET
    @Path("remote")
    public String remote() {
        String s = "From RemoteResource(" + req.getServerName() + ")";
        System.out.println(s);
        return s;
    }
}