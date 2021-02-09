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
package com.ibm.ws.jaxrs21.client.JAXRS21ClientLTPA.service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * basic resource to test jaxrs21 client API
 */
@Path("BasicResource")
public class BasicResource {

    private final String prefix = "[Basic Resource]:";

    @GET
    @Path("echo/{param}")
    public String echo(@PathParam("param") String param) {
        return prefix + param;
    }
}
