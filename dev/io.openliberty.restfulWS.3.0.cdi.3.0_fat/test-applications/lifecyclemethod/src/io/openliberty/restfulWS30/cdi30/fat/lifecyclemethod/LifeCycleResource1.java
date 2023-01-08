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
package io.openliberty.restfulWS30.cdi30.fat.lifecyclemethod;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

@Path("/lifecycle1")
@RequestScoped
public class LifeCycleResource1 {

    /**
     * A static variable to hold a message. Note that for this sample, the field
     * is static because a new {@code HelloWorldResource} object is created
     * per request.
     */
    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Inject
    private LifeCycleSimpleBean simpleBean;

    @Context
    private UriInfo uriinfo;

    LifeCyclePerson person;

    LifeCyclePerson personForC;

    public LifeCycleSimpleBean getSimpleBean() {
        return simpleBean;
    }

    @Inject
    public void setPerson(LifeCyclePerson person) {
        this.person = person;
        System.out.println(type + " Injection successful...");
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getMessage() {
        String result = "LifeCycleResource1";
        return "Resource: " + result;
    }

    @PostConstruct
    public void method1() {
        System.out.println("postConstruct method is called on " + this.getClass().getName());
    }

    @PreDestroy
    public void method2() {
        System.out.println("preDestory method is called on " + this.getClass().getName());
    }
}