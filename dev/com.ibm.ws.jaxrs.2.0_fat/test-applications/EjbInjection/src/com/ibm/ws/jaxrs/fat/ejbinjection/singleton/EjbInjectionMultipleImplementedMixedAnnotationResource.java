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

import com.ibm.ws.jaxrs.fat.ejbinjection.interfaces.annotated.MixedAnnotationInterfaceA;
import com.ibm.ws.jaxrs.fat.ejbinjection.interfaces.annotated.MixedAnnotationInterfaceB;

/**
 * Test case 10: Multiple IMPLEMENTED interfaces with MIXED annotations
 * - InterfaceA has JAX-RS annotations
 * - InterfaceB has no JAX-RS annotations (implementation provides them)
 * - Bean class implements both interfaces
 */
@Stateless
@Local({MixedAnnotationInterfaceA.class, MixedAnnotationInterfaceB.class})
@Path("multipleimplementedmixed")
public class EjbInjectionMultipleImplementedMixedAnnotationResource implements MixedAnnotationInterfaceA, MixedAnnotationInterfaceB {

    /**
     * This method gets JAX-RS annotations from InterfaceA
     */
    @Override
    public String methodA() {
        return "Multiple Implemented Mixed - Method A (from interface)";
    }

    /**
     * This method gets JAX-RS annotations from the implementation class
     */
    @Override
    @GET
    @Path("methodB")
    public String methodB() {
        return "Multiple Implemented Mixed - Method B (from class)";
    }
}

