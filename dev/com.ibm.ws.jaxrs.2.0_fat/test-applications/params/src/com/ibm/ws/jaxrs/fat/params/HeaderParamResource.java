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
package com.ibm.ws.jaxrs.fat.params;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * Resource with<code>HeaderParam</code>.
 *
 * @see HeaderParam
 */
@Path("header")
public class HeaderParamResource {

    private final String cstrHeaderParam;

    @HeaderParam("Accept-Language")
    private String acceptLanguage;

    private String agent;

    static public class HeaderValueOf {
        private HeaderValueOf(String somevalue) {}

        public static HeaderValueOf valueOf(String someValue) {
            if ("throwex".equals(someValue)) {
                throw new WebApplicationException(499);
            } else if ("throwruntimeex".equals(someValue)) {
                throw new IllegalArgumentException();
            }
            return new HeaderValueOf(someValue);
        }
    }

    static public class HeaderConstructor {
        public HeaderConstructor(String somevalue) {
            if ("throwex".equals(somevalue)) {
                throw new WebApplicationException(499);
            } else if ("throwruntimeex".equals(somevalue)) {
                throw new IllegalArgumentException();
            }
        }
    }

    public HeaderParamResource(@HeaderParam("customHeaderParam") String cstrHeaderParam) {
        this.cstrHeaderParam = cstrHeaderParam;
    }

    @GET
    public Response getHeaderParam(@HeaderParam("Accept-Language") String methodLanguage) {
        return Response.ok("getHeaderParam:" + cstrHeaderParam
                           + ";User-Agent:"
                           + agent
                           + ";Accept-Language:"
                           + acceptLanguage
                           + ";language-method:"
                           + methodLanguage).header("custResponseHeader", "secret").build();
    }

    @POST
    public Response getHeaderParamPost(@HeaderParam("CustomHeader") HeaderValueOf customHeader,
                                       @HeaderParam("CustomConstructorHeader") HeaderConstructor customHeader2) {
        return Response.ok().entity("made successful call").build();
    }

    @HeaderParam("User-Agent")
    public void setUserAgent(String aUserAgent) {
        agent = aUserAgent;
    }

    public String getUserAgent() {
        return agent;
    }
}
