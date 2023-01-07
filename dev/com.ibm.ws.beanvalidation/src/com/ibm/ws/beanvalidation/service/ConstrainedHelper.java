/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package com.ibm.ws.beanvalidation.service;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import javax.validation.metadata.BeanDescriptor;

/**
 * Provides a service for checking whether executables are constrained via either annotation or xml. The executable types
 * set via @ValidateOnExecution on the inheritance hierarchy or from the global settings are also taken into account.
 * NOTE: This service is available regardless of whether or not the CDI feature is not enabled.
 */
public interface ConstrainedHelper {

    /**
     * Check if a method requires validation based on existing constraints and applicable executable types.
     *
     * @param method
     * @param beanDescriptor
     * @param moduleClassLoader
     * @param moduleUri
     * @return true if the method should be validated
     */
    public abstract boolean isMethodConstrained(Method method, BeanDescriptor beanDescriptor, ClassLoader moduleClassLoader, String moduleUri);

    /**
     * Check if a constructor requires validation based on existing constraints and applicable executable types.
     *
     * @param constructor
     * @param beanDescriptor
     * @param moduleClassLoader
     * @param moduleUri
     * @return true if the method should be validated
     */
    public abstract boolean isConstructorConstrained(Constructor<?> constructor, BeanDescriptor beanDescriptor, ClassLoader moduleClassLoader, String moduleUri);
}