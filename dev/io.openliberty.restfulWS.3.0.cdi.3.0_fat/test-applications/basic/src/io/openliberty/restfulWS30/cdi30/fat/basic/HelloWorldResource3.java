/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.restfulWS30.cdi30.fat.basic;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/injectionInChild")
public abstract class HelloWorldResource3 {

    /**
     * A static variable to hold a message. Note that for this sample, the field
     * is static because a new {@code HelloWorldResource} object is created
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