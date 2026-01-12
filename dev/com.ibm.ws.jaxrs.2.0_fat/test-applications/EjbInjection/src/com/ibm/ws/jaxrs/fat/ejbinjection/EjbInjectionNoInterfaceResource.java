/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.ejbinjection;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Stateless
@Path("nointerface")
public class EjbInjectionNoInterfaceResource {

    @Resource
    SessionContext ctx;

    @GET
    @Path("greet")
    public String hello() {
        if (ctx == null) {
            return "ctx is null";
        }
        return "Hello, World!";
    }

}
