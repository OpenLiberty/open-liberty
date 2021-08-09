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
package com.ibm.ws.jaxrs.fat.context;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;

public class AbstractRestService {

    /**
     * This service is focus on UriInfo, HttpHeaders and Request test in Resource
     *
     * It includes tests: Bean, Field, Constructor, Param, NotBean
     */

    private UriInfo uriInfo1;
    @Context
    private UriInfo uriInfo2;
    private UriInfo uriInfo3 = null;
    private UriInfo uriInfo5;

    private HttpHeaders httpheaders1;
    @Context
    private HttpHeaders httpheaders2;
    private HttpHeaders httpheaders3 = null;
    private HttpHeaders httpheaders5;

    private Request request1;
    @Context
    private Request request2;
    private Request request3 = null;
    private Request request5;

    private Providers providers1;
    @Context
    private Providers providers2;
    private Providers providers3 = null;
    private Providers providers5;

    private Configuration config1;
    @Context
    private Configuration config2;
    private Configuration config3 = null;
    private Configuration config5;

    public AbstractRestService(@Context UriInfo ui, @Context HttpHeaders hh, @Context Request r, @Context Providers p, @Context Configuration c) {
        this.uriInfo3 = ui;
        this.httpheaders3 = hh;
        this.request3 = r;
        this.providers3 = p;
        this.config3 = c;
    }

    /**
     * UriInfo Test
     *
     * @param ui
     */
    @Context
    public void setUriInfo1(UriInfo ui) {
        uriInfo1 = ui;
    }

    @Context
    public void injectUriInfo(UriInfo ui) {
        /*
         * this method does not start with "set" as its name so it is not
         * expected to be injected.
         */
        uriInfo5 = ui;
    }

    public void setUriInfo5(UriInfo ui) {
        /*
         * this method does not have a @Context annotation so it is not expected
         * to be injected.
         */
        uriInfo5 = ui;
    }

    @Context
    public void setUriInfo5(UriInfo ui1, UriInfo ui2) {
        /*
         * this method is not a Java bean method (it has 2 parameters) so it
         * will not be used for injection
         */
        uriInfo5 = ui1;
    }

    @GET
    @Path("/" + ContextUtil.URIINFONAME1)
    @Produces("text/plain")
    public String listQueryParamNames1() {
        return ContextUtil.URIINFONAME1 + ": " + ContextUtil.testUriInfo(uriInfo1);
    }

    @GET
    @Path("/" + ContextUtil.URIINFONAME2)
    @Produces("text/plain")
    public String listQueryParamNames2() {
        return ContextUtil.URIINFONAME2 + ": " + ContextUtil.testUriInfo(uriInfo2);
    }

    @GET
    @Path("/" + ContextUtil.URIINFONAME3)
    @Produces("text/plain")
    public String listQueryParamNames3() {
        return ContextUtil.URIINFONAME3 + ": " + ContextUtil.testUriInfo(uriInfo3);
    }

    @GET
    @Path("/" + ContextUtil.URIINFONAME4)
    @Produces("text/plain")
    public String listQueryParamNames4(@Context UriInfo uriInfo4) {
        return ContextUtil.URIINFONAME4 + ": " + ContextUtil.testUriInfo(uriInfo4);
    }

    @GET
    @Path("/" + ContextUtil.URIINFONAME5)
    public Response listQueryParamNames5() {
        if (uriInfo5 == null) {
            return Response.ok("false").build();
        }
        return Response.ok("true").build();
    }

    /**
     * HttpHeaders and Providers Test
     *
     * @return
     */
    @Context
    public void setHttpHeaders1(HttpHeaders hh) {
        httpheaders1 = hh;
    }

    @Context
    public void injectHttpHeaders(HttpHeaders hh) {
        /*
         * this method does not start with "set" as its name so it is not
         * expected to be injected.
         */
        httpheaders5 = hh;
    }

    public void setHttpHeaders5(HttpHeaders hh) {
        /*
         * this method does not have a @Context annotation so it is not expected
         * to be injected.
         */
        httpheaders5 = hh;
    }

    @Context
    public void setHttpHeaders5(HttpHeaders hh1, HttpHeaders hh2) {
        /*
         * this method is not a Java bean method (it has 2 parameters) so it
         * will not be used for injection
         */
        httpheaders5 = hh1;
    }

    @Context
    public void setProviders1(Providers pr) {
        providers1 = pr;
    }

    @Context
    public void injectProviders(Providers pr) {
        /*
         * this method does not start with "set" as its name so it is not
         * expected to be injected.
         */
        providers5 = pr;
    }

    public void setProviders5(Providers pr) {
        /*
         * this method does not have a @Context annotation so it is not expected
         * to be injected.
         */
        providers5 = pr;
    }

    @Context
    public void setProviders5(Providers pr1, Providers pr2) {
        /*
         * this method is not a Java bean method (it has 2 parameters) so it
         * will not be used for injection
         */
        providers5 = pr1;
    }

    @GET
    @Path("/" + ContextUtil.HTTPHEADERSNAME1)
    @Produces("text/plain")
    public String listHeaderNames1() {
        if (providers1 == null) {
            throw new RuntimeException();
        }
        return ContextUtil.HTTPHEADERSNAME1 + ": "
               + ContextUtil.findHttpHeadersValue(httpheaders1, ContextUtil.ADDHEADERNAME);
    }

    @GET
    @Path("/" + ContextUtil.HTTPHEADERSNAME2)
    @Produces("text/plain")
    public String listHeaderNames2() {
        if (providers2 == null) {
            throw new RuntimeException();
        }
        return ContextUtil.HTTPHEADERSNAME2 + ": "
               + ContextUtil.findHttpHeadersValue(httpheaders2, ContextUtil.ADDHEADERNAME);
    }

    @GET
    @Path("/" + ContextUtil.HTTPHEADERSNAME3)
    @Produces("text/plain")
    public String listHeaderNames3() {
        if (providers3 == null) {
            throw new RuntimeException();
        }
        return ContextUtil.HTTPHEADERSNAME3 + ": "
               + ContextUtil.findHttpHeadersValue(httpheaders3, ContextUtil.ADDHEADERNAME);
    }

    @GET
    @Path("/" + ContextUtil.HTTPHEADERSNAME4)
    @Produces("text/plain")
    public String listHeaderNames4(@Context HttpHeaders httpheaders4, @Context Providers providers4) {
        if (providers4 == null) {
            throw new RuntimeException();
        }
        return ContextUtil.HTTPHEADERSNAME4 + ": "
               + ContextUtil.findHttpHeadersValue(httpheaders4, ContextUtil.ADDHEADERNAME);
    }

    @GET
    @Path("/" + ContextUtil.HTTPHEADERSNAME5)
    public Response listHeaderNames5() {
        if (httpheaders5 == null && providers5 == null) {
            return Response.ok("false").build();
        }
        return Response.ok("true").build();
    }

    /**
     * Request Test
     */
    @Context
    public void setRequest1(Request r) {
        request1 = r;
    }

    @Context
    public void injectRequest(Request r) {
        /*
         * this method does not start with "set" as its name so it is not
         * expected to be injected.
         */
        request5 = r;
    }

    public void setRequest5(Request r) {
        /*
         * this method does not have a @Context annotation so it is not expected
         * to be injected.
         */
        request5 = r;
    }

    @Context
    public void setRequest5(Request r1, Request r2) {
        /*
         * this method is not a Java bean method (it has 2 parameters) so it
         * will not be used for injection
         */
        request5 = r1;
    }

    @GET
    @Path("/" + ContextUtil.REQUESTNAME1)
    @Produces("text/plain")
    public String listRequest1() {
        return ContextUtil.REQUESTNAME1 + ": " + request1.getMethod();
    }

    @GET
    @Path("/" + ContextUtil.REQUESTNAME2)
    @Produces("text/plain")
    public String listRequest2() {
        return ContextUtil.REQUESTNAME2 + ": " + request2.getMethod();
    }

    @GET
    @Path("/" + ContextUtil.REQUESTNAME3)
    @Produces("text/plain")
    public String listRequest3() {
        return ContextUtil.REQUESTNAME3 + ": " + request3.getMethod();
    }

    @GET
    @Path("/" + ContextUtil.REQUESTNAME4)
    @Produces("text/plain")
    public String listRequest4(@Context Request request4) {
        return ContextUtil.REQUESTNAME4 + ": " + request4.getMethod();
    }

    @GET
    @Path("/" + ContextUtil.REQUESTNAME5)
    public Response listRequest5() {
        if (request5 == null) {
            return Response.ok("false").build();
        }
        return Response.ok("true").build();
    }

    /**
     * Configuration Test
     *
     * @param c
     */
    @Context
    public void setConfig1(Configuration c) {
        config1 = c;
    }

    @Context
    public void injectConfig1(Configuration c) {
        /*
         * this method does not start with "set" as its name so it is not
         * expected to be injected.
         */
        config5 = c;
    }

    public void setConfig5(Configuration c) {
        /*
         * this method does not have a @Context annotation so it is not expected
         * to be injected.
         */
        config5 = c;
    }

    @Context
    public void setConfig5(Configuration c1, Configuration c2) {
        /*
         * this method is not a Java bean method (it has 2 parameters) so it
         * will not be used for injection
         */
        config5 = c1;
    }

    @GET
    @Path("/" + ContextUtil.CONFIGNAME1)
    @Produces("text/plain")
    public String listConfiguration1() {
        return ContextUtil.CONFIGNAME1 + ": " + config1.getRuntimeType().toString();
    }

    @GET
    @Path("/" + ContextUtil.CONFIGNAME2)
    @Produces("text/plain")
    public String listConfiguration2() {
        return ContextUtil.CONFIGNAME2 + ": " + config2.getRuntimeType().toString();
    }

    @GET
    @Path("/" + ContextUtil.CONFIGNAME3)
    @Produces("text/plain")
    public String listConfiguration3() {
        return ContextUtil.CONFIGNAME3 + ": " + config3.getRuntimeType().toString();
    }

    @GET
    @Path("/" + ContextUtil.CONFIGNAME4)
    @Produces("text/plain")
    public String listConfiguration4(@Context Configuration config4) {
        return ContextUtil.CONFIGNAME4 + ": " + config4.getRuntimeType().toString();
    }

    @GET
    @Path("/" + ContextUtil.CONFIGNAME5)
    public Response listConfiguration5() {
        if (config5 == null) {
            return Response.ok("false").build();
        }
        return Response.ok("true").build();
    }
}