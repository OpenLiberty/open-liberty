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

/**
 * Second interface for testing multiple interfaces with mixed annotations.
 * This interface has no JAX-RS annotations - they will be on the implementation.
 */
public interface MixedAnnotationInterfaceB {

    /**
     * Method with no JAX-RS annotations on interface (implementation will provide them)
     */
    public String methodB();
}

// Made with Bob
