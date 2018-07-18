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
package com.ibm.ws.jaxrs20.cdi12.fat.basic;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/injectionInChild")
public abstract class HelloWorldResource3 {

    /**
     * A static variable to hold a message. Note that for this sample, the field
     * is static because a new <code>HelloWorldResource</code> object is created
     * per request.
     */
    protected String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    abstract public String getAbstractMessage();

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getMessage() {
        return getAbstractMessage();
    }

    abstract public String getAbstractUriinfo();

    @GET
    @Path("/uriinfo")
    @Produces(MediaType.TEXT_PLAIN)
    public String getUriinfo() {
        return getAbstractUriinfo();
    }

    abstract public String getAbstractSimpleBeanMessage();

    @GET
    @Path("/simplebean")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSimpleBeanMessage() {
        return getAbstractSimpleBeanMessage();
    }

    abstract public String getAbstractPerson();

    @GET
    @Path("/person")
    @Produces(MediaType.TEXT_PLAIN)
    public String getPerson() {

        return getAbstractPerson();
    }

    abstract public String getAbstractJordanException(String msgId) throws JordanException;

    @GET
    @Path("/provider/{id}")
    public String getJordanException(@PathParam("id") String msgId)
                    throws JordanException {

        return getAbstractJordanException(msgId);
    }
}