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
 * First interface for testing multiple interfaces with mixed annotations.
 */
public interface MixedAnnotationInterfaceA {

    /**
     * Method with JAX-RS annotations on interface
     */
    @GET
    @Path("methodA")
    public String methodA();
}

// Made with Bob
