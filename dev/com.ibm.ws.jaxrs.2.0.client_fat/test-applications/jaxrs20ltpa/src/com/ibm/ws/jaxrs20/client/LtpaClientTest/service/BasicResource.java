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
package com.ibm.ws.jaxrs20.client.LtpaClientTest.service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * basic resource to test jaxrs20 client API
 */
@Path("ltpa")
public class BasicResource {

    private final String prefix = "Hello LTPA Resource";

    @GET
    public String echo() {
        return prefix;
    }
}
