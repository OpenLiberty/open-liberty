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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

@Path("/headerparam/exception")
public class HeaderParamExceptionResource {

    public HeaderParamExceptionResource() {
    /* do nothing */
    }

    @HeaderParam("CustomStringConstructorFieldHeader")
    private HeaderStringConstructor customStringConstructorFieldHeader;

    @HeaderParam("CustomValueOfFieldHeader")
    private HeaderValueOf customValueOfFieldHeader;

    private HeaderValueOf customPropertyValueOfHeader;

    private HeaderStringConstructor customPropertyStringConstructorHeader;

    @HeaderParam("CustomValueOfPropertyHeader")
    public void setCustomValueOfPropertyHeader(HeaderValueOf param) {
        customPropertyValueOfHeader = param;
    }

    @HeaderParam("CustomStringConstructorPropertyHeader")
    public void setCustomConstructorPropertyHeader(HeaderStringConstructor param) {
        customPropertyStringConstructorHeader = param;
    }

    @GET
    @Path("primitive")
    public Response getHeaderParam(@HeaderParam("CustomNumHeader") int customNumHeader) {
        return Response.ok().header("RespCustomNumHeader", customNumHeader).build();
    }

    @GET
    @Path("constructor")
    public Response getStringConstructorHeaderParam(@HeaderParam("CustomStringHeader") HeaderStringConstructor customStringHeader) {
        return Response.ok().header("RespCustomStringHeader", customStringHeader.getHeader())
                        .build();
    }

    public static class HeaderValueOf implements Comparable<HeaderValueOf> {
        String header;

        private HeaderValueOf(String aHeader, int num) {
            header = aHeader;
        }

        public String getHeader() {
            return header;
        }

        public static HeaderValueOf valueOf(String v) throws Exception {
            if ("throwWeb".equals(v)) {
                throw new WebApplicationException(Response.status(498)
                                .entity("HeaderValueOfWebAppEx").build());
            } else if ("throwNull".equals(v)) {
                throw new NullPointerException("HeaderValueOf NPE");
            } else if ("throwEx".equals(v)) {
                throw new Exception("HeaderValueOf Exception");
            }
            return new HeaderValueOf(v, 100);
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        @Override
        //Defect 68201: Implenent camparable interface. There is only one useful variable header in the class, so return the compare to result of header
        public int compareTo(HeaderValueOf o) {
            // TODO Auto-generated method stub
            return this.header.compareTo(o.header);
        }
    }

    @GET
    @Path("valueof")
    public Response getValueOfHeaderParam(@HeaderParam("CustomValueOfHeader") HeaderValueOf customValueOfHeader) {
        return Response.ok().header("RespCustomValueOfHeader", customValueOfHeader.getHeader())
                        .build();
    }

    @GET
    @Path("listvalueof")
    public Response getValueOfHeaderParam(@HeaderParam("CustomListValueOfHeader") List<HeaderValueOf> customValueOfHeader) {
        if (customValueOfHeader.size() != 1) {
            throw new IllegalArgumentException();
        }
        return Response.ok().header("RespCustomListValueOfHeader",
                                    customValueOfHeader.get(0).getHeader()).build();
    }

    @GET
    @Path("setvalueof")
    public Response getValueOfHeaderParam(@HeaderParam("CustomSetValueOfHeader") Set<HeaderValueOf> customValueOfHeader) {
        if (customValueOfHeader.size() != 1) {
            throw new IllegalArgumentException();
        }
        return Response.ok().header("RespCustomSetValueOfHeader",
                                    new ArrayList<HeaderValueOf>(customValueOfHeader).get(0)
                                                    .getHeader()).build();
    }

    @GET
    @Path("sortedsetvalueof")
    public Response getValueOfHeaderParam(@HeaderParam("CustomSortedSetValueOfHeader") SortedSet<HeaderValueOf> customValueOfHeader) {
        if (customValueOfHeader.size() != 1) {
            throw new IllegalArgumentException();
        }
        return Response.ok().header("RespCustomSortedSetValueOfHeader",
                                    customValueOfHeader.first().getHeader()).build();
    }

    @GET
    @Path("fieldstrcstr")
    public Response getFieldStringConstructorHeaderParam() {
        return Response.ok().header("RespCustomStringConstructorFieldHeader",
                                    customStringConstructorFieldHeader.getHeader()).build();
    }

    @GET
    @Path("fieldvalueof")
    public Response getFieldValueOfHeaderParam() {
        return Response.ok().header("RespCustomValueOfFieldHeader",
                                    customValueOfFieldHeader.getHeader()).build();
    }

    @GET
    @Path("propertystrcstr")
    public Response getPropertyStringConstructorHeaderParam() {
        return Response.ok().header("RespCustomStringConstructorPropertyHeader",
                                    customPropertyStringConstructorHeader.getHeader()).build();
    }

    @GET
    @Path("propertyvalueof")
    public Response getPropertyValueOfHeaderParam() {
        return Response.ok().header("RespCustomValueOfPropertyHeader",
                                    customPropertyValueOfHeader.getHeader()).build();
    }

}
