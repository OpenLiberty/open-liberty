/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation.service;

import java.io.InputStream;

import javax.validation.ValidationException;

/**
 * Provides BeanValidation module context data for creating the
 * module specific ValidatorFactory. <p>
 * 
 * This interface also allows different implementations for
 * traditional WebSphere and the Liberty profile. <p>
 */
public interface BeanValidationContext
{

    /**
     * Returns the module (i.e. application) ClassLoader.
     */
    ClassLoader getClassLoader();

    /**
     * Returns the module path.
     */
    String getPath();

    /**
     * Opens an InputStream for the requested file in the module.
     * 
     * @param fileName name of the file within the module
     * 
     * @throws ValidationException if the file cannot be found or accessed.
     */
    InputStream getInputStream(final String fileName) throws ValidationException;

}
