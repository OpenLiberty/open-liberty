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
package com.ibm.ws.jaxrs21.fat.JAXRS21ReactiveSample.server;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;

public class Customer {
    public static final String BEST_SERVICE_LEVEL = "Best";
    public static final String BETTER_SERVICE_LEVEL = "Better";
    public static final String FREE_SERVICE_LEVEL = "Free";
    public static final int BEST_MAX = 2048;
    public static final int BETTER_MAX = 1024;
    public static final int FREE_MAX = 512;
    private String name;
    private String serviceLevel;

    public Customer() {

    }

    public Customer(String name, String serviceLevel) {
        this.name = name;
        this.serviceLevel = serviceLevel;
    }

    public void setName(String n) {
        name = n;
    }

    public String getName() {
        return name;
    }

    public void setServiceLevel(String sl) {
        serviceLevel = sl;
    }

    public String getServiceLevel() {
        return serviceLevel;
    }

    @PUT
    public void cloneState(Customer customer) {
        serviceLevel = customer.getServiceLevel();
        name = customer.getName();
    }

    @GET
    public Customer retrieveState() {
        return this;
    }
}
