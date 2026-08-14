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
 * Test case 8: Single IMPLEMENTED interface with MIXED annotations
 * - Interface has some JAX-RS annotations
 * - Implementation class provides additional JAX-RS annotations
 * - Bean class implements the interface
 */
@Stateless
@Local(MixedAnnotationInterface.class)
@Path("singleimplementedmixed")
public class EjbInjectionSingleImplementedMixedAnnotationResource implements MixedAnnotationInterface {

    /**
     * This method gets JAX-RS annotations from the interface
     */
    @Override
    public String interfaceAnnotatedMethod() {
        return "Single Implemented Mixed - Interface Annotated Method";
    }

    /**
     * This method gets JAX-RS annotations from the implementation class
     */
    @Override
    @GET
    @Path("classmethod")
    public String classAnnotatedMethod() {
        return "Single Implemented Mixed - Class Annotated Method";
    }
}

