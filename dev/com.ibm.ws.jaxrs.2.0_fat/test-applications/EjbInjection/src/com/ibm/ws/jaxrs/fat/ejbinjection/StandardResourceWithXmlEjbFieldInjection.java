/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.jaxrs.fat.ejbinjection;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import com.ibm.ws.jaxrs.fat.ejbinjection.ejbs.MessageService;

/**
 * Scenario 7: Standard non-EJB JAX-RS resource class that injects an EJB
 * (defined via ejb-jar.xml) using field injection with @Inject.
 *
 * Note: No @Stateless annotation - this is a standard JAX-RS resource
 * The injected MessageService is defined as an EJB via ejb-jar.xml
 */
@Path("standardxmlejbfield")
public class StandardResourceWithXmlEjbFieldInjection {

    @Inject
    private MessageService messageService;

    @GET
    @Path("message")
    public String getMessage(@QueryParam("name") String name) {
        if (messageService == null) {
            return "messageService is null";
        }
        String userName = (name != null && !name.isEmpty()) ? name : "World";
        return messageService.getMessage(userName);
    }
}
