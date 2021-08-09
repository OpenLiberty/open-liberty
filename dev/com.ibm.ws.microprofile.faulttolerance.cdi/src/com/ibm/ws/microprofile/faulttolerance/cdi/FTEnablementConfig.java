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
package com.ibm.ws.microprofile.faulttolerance.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Provides methods for determining whether a given annotation has been disabled via config.
 * <p>
 * Different versions of the Fault Tolerance specification provide different levels of support for enabling and disabling fault tolerance functionality.
 */
public interface FTEnablementConfig {

    /**
     * Checks if a fault tolerance annotation is enabled or disabled via configuration options.
     *
     * @param ann The annotation
     * @param clazz The class containing the annotation.
     * @return Is the annotation enabled
     */
    public boolean isAnnotationEnabled(Annotation ann, Class<?> clazz);

    /**
     * Checks if a fault tolerance annotation is enabled or disabled via configuration options.
     *
     * @param ann The annotation
     * @param clazz The class containing the annotation.
     * @param method The method annotated with the annotation. If {@code null} only class and global scope properties will be checked.
     * @throws IllegalArgumentException If passed a non-fault-tolerance annotation
     * @return Is the annotation enabled
     */
    public boolean isAnnotationEnabled(Annotation ann, Class<?> clazz, Method method);

    /**
     * Checks whether the given annotation is a fault tolerance annotation
     *
     * @param ann the annotation
     * @return true if the annotation is a fault tolerance annotation, false otherwise
     */
    public boolean isFaultTolerance(Annotation ann);
}
