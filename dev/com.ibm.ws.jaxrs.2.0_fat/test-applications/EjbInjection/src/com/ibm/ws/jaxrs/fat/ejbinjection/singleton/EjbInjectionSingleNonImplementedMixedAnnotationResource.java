/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.ejbinjection.singleton;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import com.ibm.ws.jaxrs.fat.ejbinjection.interfaces.annotated.MixedAnnotationInterface;

/**
 * Test case 9: Single NON-IMPLEMENTED interface with MIXED annotations
 * - Interface has some JAX-RS annotations
 * - Implementation class provides additional JAX-RS annotations
 * - Bean class does NOT implement the interface (EJB proxy handles it)
 */
@Stateless
@Local(MixedAnnotationInterface.class)
@Path("singlenonimplementedmixed")
public class EjbInjectionSingleNonImplementedMixedAnnotationResource {

    /**
     * This method gets JAX-RS annotations from the interface
     * Note: Method name and signature must match interface method
     */
    public String interfaceAnnotatedMethod() {
        return "Single Non-Implemented Mixed - Interface Annotated Method";
    }

    /**
     * This method gets JAX-RS annotations from the implementation class
     * Note: Method name and signature must match interface method
     */
    @GET
    @Path("classmethod")
    public String classAnnotatedMethod() {
        return "Single Non-Implemented Mixed - Class Annotated Method";
    }
}

// Made with Bob
