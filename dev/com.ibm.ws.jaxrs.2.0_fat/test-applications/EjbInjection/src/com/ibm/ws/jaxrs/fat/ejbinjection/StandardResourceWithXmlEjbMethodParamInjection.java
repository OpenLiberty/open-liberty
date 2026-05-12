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

import com.ibm.ws.jaxrs.fat.ejbinjection.ejbs.MessageService;

/**
 * Scenario 8: Standard non-EJB JAX-RS resource class that injects an EJB
 * (defined via ejb-jar.xml) using method injection with @Inject.
 *
 * Note: No @Stateless annotation - this is a standard JAX-RS resource
 * The injected MessageService is defined as an EJB via ejb-jar.xml
 */
@Path("standardxmlejbmethodinjection")
public class StandardResourceWithXmlEjbMethodParamInjection {

    private MessageService messageService;

    @Inject
    public void setMessageService(MessageService messageService) {
        this.messageService = messageService;
    }

    @GET
    @Path("status")
    public String getStatus() {
        if (messageService == null) {
            return "messageService is null";
        }
        return messageService.getStatus();
    }
}
