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

import com.ibm.ws.jaxrs.fat.ejbinjection.ejbs.GreetingService;

/**
 * Scenario 4: Resource class defined as an EJB via ejb-jar.xml file
 * (with no EJB annotations) with EJB method injection using @Inject.
 * Tests that EJBs can be injected using @Inject via setter in XML-defined EJBs.
 *
 * Note: No @Stateless annotation - EJB definition comes from ejb-jar.xml
 */
@Path("ejbxmlmethodparam")
public class EjbXmlMethodParamInjectionResource {

    private GreetingService greetingService;

    @Inject
    public void setGreetingService(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @GET
    @Path("greet")
    public String greet(@QueryParam("name") String name) {
        if (greetingService == null) {
            return "greetingService is null";
        }
        String userName = (name != null && !name.isEmpty()) ? name : "World";
        return "Hello from XML-defined EJB, " + userName + "!";
    }
}
