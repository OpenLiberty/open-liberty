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

import com.ibm.ws.jaxrs.fat.ejbinjection.ejbs.GreetingService;

/**
 * Scenario 3: Resource class defined as an EJB via ejb-jar.xml file
 * (with no EJB annotations) with EJB field injection using @Inject.
 * Tests that EJBs can be injected using @Inject in XML-defined EJBs.
 *
 * Note: No @Stateless annotation - EJB definition comes from ejb-jar.xml
 */
@Path("ejbxmlfield")
public class EjbXmlFieldInjectionResource {

    @Inject
    private GreetingService greetingService;

    @GET
    @Path("greet")
    public String hello() {
        if (greetingService == null) {
            return "greetingService is null";
        }
        return "Hello from XML-defined EJB!";
    }
}
