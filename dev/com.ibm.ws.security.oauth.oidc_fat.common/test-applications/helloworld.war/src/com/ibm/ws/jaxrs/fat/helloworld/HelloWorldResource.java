/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.helloworld;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * <code>HelloWorldResource</code> is a simple POJO which is annotated with
 * JAX-RS annotations to turn it into a JAX-RS resource.
 * <p/>
 * This class has a {@link Path} annotation with the value "helloworld" which means the resource will be available at:
 * <code>http://&lt;hostname&gt;:&lt;port&gt/&lt;context root&gt;/&lt;servlet path&gt;/helloworld</code>
 * <p/>
 * Remember to add this resource class to the {@link HelloWorldApplication#getClasses()} method.
 *
 * @param <E>
 */

@Path("/{app: helloworld.*}")
public class HelloWorldResource<E> {

    String ATinHeader = "Accessed Hello World! Access Token in the header";
    String ATasParm = "Accessed Hello World! Access Token as Parm";
    String noAT = "Accessed Hello World! Access Token is notSet";
    String notSet = "notSet";
    String global_access_token;
    String global_targetApp;
    String global_where;
    String global_tokenContent;
    String global_contextSet;
    String global_headerName;
    String global_jwtBuilder;
    /**
     * A static variable to hold a message. Note that for this sample, the field
     * is static because a new <code>HelloWorldResource</code> object is created
     * per request.
     */
    private static String message;

    /**
     * Processes a GET request and returns the stored message.
     *
     * @return the stored message
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    //    public String getMessage(@Context HttpHeaders headers, @DefaultValue("notSet") @QueryParam("access_token") String accessToken) {
    public String getMessage(@Context HttpHeaders headers,
            @DefaultValue("notSet") @QueryParam("access_token") String accessToken,
            @DefaultValue("notSet") @QueryParam("targetApp") String appToCall,
            @DefaultValue("notSet") @QueryParam("where") String where,
            @DefaultValue("notSet") @QueryParam("tokenContent") String tokenContent,
            @DefaultValue("notSet") @QueryParam("contextSet") String contextSet,
            @DefaultValue("Authorization") @QueryParam("headerName") String headerName,
            @DefaultValue("notSet") @QueryParam("jwtBuilder") String jwtBuilder) {
        global_access_token = accessToken;
        global_targetApp = appToCall;
        global_where = where;
        global_tokenContent = tokenContent;
        global_contextSet = contextSet;
        global_headerName = headerName;
        global_jwtBuilder = jwtBuilder;
        return processMessage(headers, accessToken);
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes("application/x-www-form-urlencoded")
    //    public String postMessage(@Context HttpHeaders headers, @DefaultValue("notSet") @QueryParam("access_token") String accessToken) {
    public String postMessage(@Context HttpHeaders headers,
            @DefaultValue("notSet") @FormParam("access_token") String accessToken,
            @DefaultValue("notSet") @FormParam("targetApp") String appToCall,
            @DefaultValue("notSet") @FormParam("where") String where,
            @DefaultValue("notSet") @FormParam("tokenContent") String tokenContent,
            @DefaultValue("notSet") @FormParam("contextSet") String contextSet,
            @DefaultValue("Authorization") @QueryParam("headerName") String headerName,
            @DefaultValue("notSet") @QueryParam("jwtBuilder") String jwtBuilder) {
        //    public String postMessage(@Context HttpHeaders headers, HttpServletRequest request)  {

        //    	Map<String, String[]> parmMap = request.getParameterMap();
        //    	Set set = parmMap.entrySet() ;
        //    	Iterator it = set.iterator() ;
        //    	while (it.hasNext()) {
        //			Map.Entry<String, String[]> entry = (Entry<String, String[]>) it.next();
        //            String paramName = entry.getKey();
        //    	}
        //    	String accessToken = request.getParameter("access_token") ;

        global_access_token = accessToken;
        global_targetApp = appToCall;
        global_where = where;
        global_tokenContent = tokenContent;
        global_contextSet = contextSet;
        global_headerName = headerName;
        global_jwtBuilder = jwtBuilder;
        return processMessage(headers, accessToken);
    }

    private String processMessage(HttpHeaders headers, String param_at) {
        HelloWorldResource.message = "Hello World!";
        StringBuffer sb = new StringBuffer();

        System.out.println("Entering Helloworld");
        MultivaluedMap<String, String> theHeaders = headers.getRequestHeaders();
        if (theHeaders != null) {
            for (String key : theHeaders.keySet()) {
                CallerUtil.writeLine(sb, "Header key: " + key + " value: " + theHeaders.getFirst(key));
            }
        }

        String atoken = null;
        List<String> tokenFromHeader = headers.getRequestHeader(global_headerName);
        if (tokenFromHeader == null || tokenFromHeader.isEmpty()) {
            if (param_at.equals(notSet)) {
                System.out.println(noAT);
                HelloWorldResource.message = noAT;
                atoken = notSet;
            } else {
                System.out.println(ATasParm);
                HelloWorldResource.message = ATasParm;
                atoken = param_at;
            }
        } else {
            System.out.println(ATinHeader);
            HelloWorldResource.message = ATinHeader;
            atoken = tokenFromHeader.get(0);
        }

        // Note that if null is returned from a resource method, a HTTP 204 (No
        // Content) status code response is sent.

        CallerUtil.writeLine(sb, "\n");
        CallerUtil.writeLine(sb, "*******************  Start of HelloWorld output  ******************* \r\n");
        CallerUtil.writeLine(sb, HelloWorldResource.message);
        CallerUtil.writeLine(sb, "access_token: " + atoken);
        CallerUtil.writeLine(sb, "Param: access_token with value: " + global_access_token);
        CallerUtil.writeLine(sb, "Param: targetApp with value: " + global_targetApp);
        CallerUtil.writeLine(sb, "Param: where with value: " + global_where);
        CallerUtil.writeLine(sb, "Param: tokenContent with value: " + global_tokenContent);
        CallerUtil.writeLine(sb, "Param: contextSet with value: " + global_contextSet);
        CallerUtil.writeLine(sb, "Param: headerName with value: " + global_headerName);
        CallerUtil.writeLine(sb, "Param: jwtBuilder with value: " + global_jwtBuilder);
        CallerUtil.writeLine(sb, "\n");

        CallerUtil.writeLine(sb, "RealmName: " + CallerUtil.getRealmName());
        CallerUtil.writeLine(sb, "Principal ID: " + CallerUtil.getPrincipalUserID());
        CallerUtil.writeLine(sb, "Access ID: " + CallerUtil.getAccessId());
        CallerUtil.writeLine(sb, "Access Token: " + CallerUtil.getAccessTokenFromSubject());
        CallerUtil.writeLine(sb, "Security name: " + CallerUtil.getSecurityName());

        CallerUtil.printProgrammaticApiValues(sb);
        CallerUtil.writeLine(sb, "*******************  End of HelloWorld output  ******************* \r\n");

        System.out.println(sb.toString());

        return sb.toString();

    }

    //    /**
    //     * Processes a POST request and returns the incoming request message.
    //     *
    //     * @param incomingMessage the request body is mapped to the String by the
    //     *            JAX-RS runtime using a built-in entity provider
    //     * @return the original request body
    //     */
    //    @POST
    //    @Consumes(MediaType.TEXT_PLAIN)
    //    @Produces(MediaType.TEXT_PLAIN)
    //    public String postMessage(String incomingMessage) {
    //        // A plain Java parameter is used to represent the request body. The
    //        // JAX-RS runtime will map the request body to a String.
    //        HelloWorldResource.message = incomingMessage;
    //        return incomingMessage;
    //    }

    /**
     * Processes a PUT request and returns the incoming request message.
     *
     * @param incomingMessage
     *            the request body is mapped to the byte[] by the
     *            JAX-RS runtime using a built-in entity provider
     * @return the original request body in a JAX-RS Response object
     */
    @PUT
    public Response putMessage(byte[] incomingMessage) {
        // Note that different Java types can be used to map the
        // incoming request body to a Java type.
        HelloWorldResource.message = new String(incomingMessage);

        // Note that a javax.ws.rs.core.Response object is returned. A Response
        // object can be built which contains additional HTTP headers, a status
        // code, and the entity body.
        return Response.ok(incomingMessage).type(MediaType.TEXT_PLAIN).build();
    }

    /**
     * Processes a DELETE request.
     *
     * @return an empty response with a 204 status code
     */
    @DELETE
    public Response deleteMessage() {
        HelloWorldResource.message = null;
        // Note that a javax.ws.rs.core.Response object is returned. In this
        // method a HTTP 204 status code (No Content) is returned.
        return Response.noContent().build();
    }
}
