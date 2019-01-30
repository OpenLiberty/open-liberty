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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

public class AbstractRestService2 {
    /**
     * This service is focus on ResourceContext in Resource
     *
     * It includes tests: Bean, Field, Constructor, Param, NotBean
     *
     * Notice: You cannot use @GET for sub-resource, or it will return the bean, not the resource!
     */

    private ResourceContext resourceContext1;
    @Context
    private ResourceContext resourceContext2;
    private ResourceContext resourceContext3 = null;
    private ResourceContext resourceContext5;

    private final Book bookSub = new Book();

    public AbstractRestService2(@Context ResourceContext rc) {
        resourceContext3 = rc;
    }

    /**
     * ResourceContext Test
     *
     * @param rc
     */
    @Context
    public void setResourceContext1(ResourceContext rc) {
        resourceContext1 = rc;
    }

    @Context
    public void injectResourceContext(ResourceContext rc) {
        /*
         * this method does not start with "set" as its name so it is not
         * expected to be injected.
         */
        resourceContext5 = rc;
    }

    public void setResourceContext5(ResourceContext rc) {
        /*
         * this method does not have a @Context annotation so it is not expected
         * to be injected.
         */
        resourceContext5 = rc;
    }

    @Context
    public void setResourceContext5(ResourceContext rc1, ResourceContext rc2) {
        /*
         * this method is not a Java bean method (it has 2 parameters) so it
         * will not be used for injection
         */
        resourceContext5 = rc2;
    }

    @Path("/" + ContextUtil.RESOURCECONTEXT1)
    @Produces("application/xml")
    @Consumes("application/xml")
    public Book getBookSubResourceRC1() {
        return resourceContext1.getResource(Book.class);
    }

    @Path("/" + ContextUtil.RESOURCECONTEXT2)
    @Produces("application/xml")
    @Consumes("application/xml")
    public Book getBookSubResourceRC2() {
        return resourceContext2.getResource(Book.class);
    }

    @Path("/" + ContextUtil.RESOURCECONTEXT3)
    @Produces("application/xml")
    @Consumes("application/xml")
    public Book getBookSubResourceInstanceRC1() {
        return resourceContext3.initResource(bookSub);
    }

    @Path("/" + ContextUtil.RESOURCECONTEXT4)
    @Produces("application/xml")
    @Consumes("application/xml")
    public Book getBookSubResourceInstanceRC2(@Context ResourceContext rc) {
        return rc.initResource(bookSub);
    }

    @GET
    @Path("/" + ContextUtil.RESOURCECONTEXT5)
    public Response getBookSubResourceRC() {
        if (resourceContext5 == null) {
            return Response.ok("false").build();
        }
        return Response.ok("true").build();
    }

    @GET
    @Path("/jordanproviders/{id}")
    @Produces({ "text/xml" })
    public Response getJordanException(@PathParam("id") String msgId)
                    throws JordanException {
        String name = "jordan";
        if (msgId.trim() == name || msgId.trim().equals(name)) {
            throw new JordanException("JordanException: Jordan is superman, you cannot be!");
        }

        return Response.ok("true").build();
    }
}