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
package com.ibm.ws.jaxrs.fat.params.query;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

@Path("/queryparam/exception")
public class QueryParamsExceptionResource {

    public QueryParamsExceptionResource() {
        /* do nothing */
    }

    @QueryParam("CustomStringConstructorFieldQuery")
    private ParamStringConstructor customStringConstructorFieldQuery;

    @QueryParam("CustomValueOfFieldQuery")
    private QueryValueOf customValueOfFieldQuery;

    private ParamStringConstructor customPropertyStringConstructorQuery;

    private QueryValueOf customPropertyValueOfQuery;

    @QueryParam("CustomStringConstructorPropertyHeader")
    public void setCustomPropertyStringConstructorQuery(ParamStringConstructor param) {
        customPropertyStringConstructorQuery = param;
    }

    @QueryParam("CustomValueOfPropertyHeader")
    public void setCustomValueOfPropertyHeader(QueryValueOf param) {
        customPropertyValueOfQuery = param;
    }

    @GET
    @Path("primitive")
    public Response getHeaderParam(@QueryParam("CustomNumQuery") int customNumHeader) {
        return Response.ok().header("RespCustomNumQuery", customNumHeader).build();
    }

    public static class QueryValueOf {
        String header;

        private QueryValueOf(String aHeader, int num) {
            header = aHeader;
        }

        public String getParamValue() {
            return header;
        }

        public static QueryValueOf valueOf(String v) throws Exception {
            if ("throwWeb".equals(v)) {
                throw new WebApplicationException(Response.status(498)
                                .entity("ParamValueOfWebAppEx").build());
            } else if ("throwNull".equals(v)) {
                throw new NullPointerException("ParamValueOf NPE");
            } else if ("throwEx".equals(v)) {
                throw new Exception("ParamValueOf Exception");
            }
            return new QueryValueOf(v, 100);
        }
    }

    @GET
    @Path("fieldstrcstr")
    public Response getFieldStringConstructorHeaderParam() {
        return Response.ok().entity(customStringConstructorFieldQuery.getParamValue()).build();
    }

    @GET
    @Path("fieldvalueof")
    public Response getFieldValueOfHeaderParam() {
        return Response.ok().header("RespCustomValueOfFieldHeader",
                                    customValueOfFieldQuery.getParamValue()).build();
    }

    @GET
    @Path("propertystrcstr")
    public Response getPropertyStringConstructorHeaderParam() {
        return Response.ok().header("RespCustomStringConstructorPropertyQuery",
                                    customPropertyStringConstructorQuery.getParamValue()).build();
    }

    @GET
    @Path("propertyvalueof")
    public Response getPropertyValueOfHeaderParam() {
        return Response.ok().header("RespCustomValueOfPropertyQuery",
                                    customPropertyValueOfQuery.getParamValue()).build();
    }

}
