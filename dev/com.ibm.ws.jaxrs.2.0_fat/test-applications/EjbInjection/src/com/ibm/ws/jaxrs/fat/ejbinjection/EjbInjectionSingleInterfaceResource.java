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
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import com.ibm.ws.jaxrs.fat.ejbinjection.interfaces.EchoEJBInterface;

@Stateless
@Local({ EchoEJBInterface.class })
@Path("singleinterface")
public class EjbInjectionSingleInterfaceResource implements EchoEJBInterface {

    @Resource
    SessionContext ctx;

    @Override
    @POST
    @Path("echo")
    public String echo(String message) {
        if (ctx == null) {
            return "ctx is null";
        }
        return message;
    }

}
