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
package com.ibm.testapp.g3store.restConsumer.client;

import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;

import com.ibm.testapp.g3store.utilsConsumer.ConsumerUtils;

/**
 * @author anupag
 *
 *         This class is MicroProfile REST Client interface of consumer APIs.
 *         It uses the JAX-RS annotations.
 *
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/consumer")
public interface ConsumerServiceRestClient {

    @GET
    @Path("/appInfo/{appName}")
    @ClientHeaderParam(name = "Authorization", value = "{getAuthValue}")
    public Response getAppInfo(@PathParam("appName") String appName) throws Exception;

    @GET
    @Path("/appInfo_BadAuth/{appName}")
    @ClientHeaderParam(name = "Authorization", value = "{getBadAuthValue}")
    public Response getAppInfoBadAuth(@PathParam("appName") String appName) throws Exception;

    @GET
    @Path("/priceQuery")
    public Response getPrices(@QueryParam("appName") List<String> appNames) throws Exception;

    @GET
    @Path("/appNames/{testMethodName}")
    public Response getAllAppNames(@PathParam("testMethodName") String testMethodName) throws Exception;

    @GET
    @Path("/appNames_CC")
    public Response getAllAppNames_Auth_CallCred() throws Exception;

    default String getAuthValue() throws UnsupportedEncodingException {
        return ConsumerUtils.createBasicAuthHeaderValue("dev", "hello");
    }

    default String getBadAuthValue() throws UnsupportedEncodingException {
        return ConsumerUtils.createBasicAuthHeaderValue("dev2", "hello2");
    }

}
