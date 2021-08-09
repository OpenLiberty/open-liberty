/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation.service;

import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import com.ibm.ws.runtime.metadata.ModuleMetaData;

/**
 * The BeanValidation container integration service to be used by components (e.g. JCA)
 * to get information for JavaBean Validation with additional classloader attribute. <p>
 * 
 * Primarily, this service provides access to container managed
 * ValidatorFactory instances. <p>
 */
public interface BeanValidationUsingClassLoader {

    /**
     * Returns the container managed ValidatorFactory that has been configured
     * for the specified Java EE application module. <p>
     * 
     * @param mmd module level metadata; must not be null.
     * @param loader classloader used for locating bean validation artifacts.
     */
    public ValidatorFactory getValidatorFactory(ModuleMetaData mmd, ClassLoader loader);

    /**
     * Returns the container managed Validator that has been configured
     * for the specified Java EE application component. <p>
     * 
     * This is a convenience method that will obtain the Validator for
     * the module corresponding to the specified component metadata, but is
     * also intended to throw a consistent exception when there is no component
     * meta data. It is suggested that this method is used in conjunction with
     * obtaining the current component metadata for the thread, as follows: <p>
     * 
     * cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
     * vf = bv.getValidator( cmd, classloader );
     * 
     * @param cmd component level metadata.
     * @param loader classloader used for locating bean validation artifacts.
     * 
     * @throws ValidationException if the input parameter is null or a
     *             failure occurs obtaining the Validator instance.
     */
    public Validator getValidator(ModuleMetaData mmd, ClassLoader loader);

}
