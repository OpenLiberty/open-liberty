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
package com.ibm.ws.jaxrs.fat.params.header;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("/headerparam/default")
public class HeaderParamDefaultResource {

    private final String customConstructorHeaderParam;

    private String customPropertyHeaderParam;

    private String agent;

    @DefaultValue("english")
    @HeaderParam("Accept-Language")
    private String acceptLanguageHeaderParam;

    public HeaderParamDefaultResource(@DefaultValue("MyCustomConstructorHeader") @HeaderParam("CustomConstructorHeader") String cstrHeaderParam) {
        this.customConstructorHeaderParam = cstrHeaderParam;
    }

    public Response info(String customMethodHeader) {
        Response r =
                        Response.status(Status.OK).header("RespCustomConstructorHeader",
                                                          customConstructorHeaderParam)
                                        .header("RespAccept-Language", acceptLanguageHeaderParam)
                                        .header("RespCustomMethodHeader", customMethodHeader)
                                        .header("RespUserAgent", agent).header("RespCustomPropertyHeader",
                                                                               customPropertyHeaderParam).build();
        return r;
    }

    @DefaultValue("MyAgent")
    @HeaderParam("User-Agent")
    public void setUserAgent(String aUserAgent) {
        agent = aUserAgent;
    }

    public String getUserAgent() {
        return agent;
    }

    @DefaultValue("MyCustomPropertyHeader")
    @HeaderParam("CustomPropertyHeader")
    public void setCustomPropertyHeader(String customProperty) {
        customPropertyHeaderParam = customProperty;
    }

    public String getCustomPropertyHeader() {
        return customPropertyHeaderParam;
    }

    @GET
    public Response getHeaderParam(@DefaultValue("MyCustomMethodHeader") @HeaderParam("CustomMethodHeader") String c) {
        return info(c);
    }

    @POST
    public Response postHeaderParam(@DefaultValue("MyCustomMethodHeader") @HeaderParam("CustomMethodHeader") String c) {
        return info(c);
    }

    @PUT
    public Response putHeaderParam(@DefaultValue("MyCustomMethodHeader") @HeaderParam("CustomMethodHeader") String c) {
        return info(c);
    }

    @DELETE
    public Response deleteHeaderParam(@DefaultValue("MyCustomMethodHeader") @HeaderParam("CustomMethodHeader") String c) {
        return info(c);
    }

    /* FIXME: Check if ResponseBuilder header values can be null */
}
