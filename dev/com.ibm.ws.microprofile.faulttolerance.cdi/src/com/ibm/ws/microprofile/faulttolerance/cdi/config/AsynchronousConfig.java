/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.cdi.config;

/**
 * Annotation config for classes and methods annotated with {@code @Asynchronous}
 */
public interface AsynchronousConfig {

    /**
     * Validate Asynchronous annotation to make sure all methods with this annotation specified returns a Future.
     * If placed on class-level, all declared methods in this class will need to return a Future.
     */
    void validate();

}