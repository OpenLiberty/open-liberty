/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.ejbinjection.interfaces.annotated;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * Interface with JAX-RS annotations for testing mixed annotation scenarios.
 * Some methods have JAX-RS annotations on the interface, others will have them on the implementation.
 */
public interface MixedAnnotationInterface {

    /**
     * Method with JAX-RS annotations on interface
     */
    @GET
    @Path("interfaceAnnotated")
    public String interfaceAnnotatedMethod();

    /**
     * Method without JAX-RS annotations - implementation will have them
     */
    public String classAnnotatedMethod();
}

// Made with Bob
