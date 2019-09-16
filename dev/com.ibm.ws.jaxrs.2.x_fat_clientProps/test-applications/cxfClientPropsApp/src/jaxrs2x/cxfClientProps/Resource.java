/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxrs2x.cxfClientProps;

import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

@ApplicationPath("/")
@Path("/resource")
public class Resource extends Application {
    private final static Logger _log = Logger.getLogger(Resource.class.getName());

    @Context
    private HttpHeaders httpHeaders;
    
    @Context
    HttpServletRequest req;

    @GET
    @Path("/{sleepTime}")
    public Response sleep(@PathParam("sleepTime") @DefaultValue("30000") long sleepTime) {
        try {
            Thread.sleep(sleepTime);
            return Response.ok("Slept " + sleepTime + "ms").build();
        } catch (InterruptedException ex) {
            return Response.serverError().entity(ex).build();
        }
    }

    @GET
    @Path("/header")
    public String getHeader(@QueryParam("h") String headerName) {
        return httpHeaders.getHeaderString(headerName);
    }
    
    @POST
    @Path("/chunking")
    public String chunking(String entity) {
        String contentLength = httpHeaders.getHeaderString("Content-Length");
        int actualLength = entity.length();
        _log.info("chunking contentLength(from header)=" + contentLength + " actualLength=" + actualLength);
        return contentLength == null ? "CHUNKING" : contentLength + ":" + actualLength;
    }
    
    @GET
    @Path("echo/{param}")
    public String echo(@PathParam("param") String param) {        
        return  param;
    }
    
    
    @GET
    @Path("redirect/{param}/{status}")
    public Response redirect(@PathParam("param") String param,
                             @PathParam("status") String status) { 
        _log.info("redirect/{param}/{status} testing status = " + status);
        return Response.status(Integer.valueOf(status)).header("Location", "http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/redirectecho?param=" + param).build();
    }
        
    @GET
    @Path("redirectecho")
    public String redirectEcho(@QueryParam("param") String param) {        
        return  param;
    }
    
    @GET
    @Path("redirecthop1/{param}/{status}")
    public Response redirectHop1(@PathParam("param") String param,
                                 @PathParam("status") String status) { 
        _log.info("redirecthop1/{param}/{status} testing status = " + status);
        return Response.status(Integer.valueOf(status)).header("Location", "http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/redirecthop2?param=" + param + "&status=" + status).build();
    }
    
    @GET
    @Path("redirecthop2")
    public Response redirectHop2(@QueryParam("param") String param,
                                 @QueryParam("status") String status) { 
        _log.info("redirecthop2 testing status = " + status);
        return Response.status(Integer.valueOf(status)).header("Location", "http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/redirecthop3?param=" + param + "&status=" + status).build();
    }
    
    @GET
    @Path("redirecthop3")
    public Response redirectHop3(@QueryParam("param") String param,
                                 @QueryParam("status") String status) { 
        _log.info("redirecthop3 testing status = " + status);
        return Response.status(Integer.valueOf(status)).header("Location", "http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/redirectecho?param=" + param).build();
    }
}
