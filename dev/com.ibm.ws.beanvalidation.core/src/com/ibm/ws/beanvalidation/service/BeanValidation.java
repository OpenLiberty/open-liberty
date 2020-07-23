/*******************************************************************************
 * Copyright (c) 2012,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation.service;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;

/**
 * The BeanValidation container integration service to be used by containers
 * to get information for JavaBean Validation. <p>
 *
 * Primarily, this service provides access to container managed
 * ValidatorFactory instances. <p>
 */
public interface BeanValidation {
    /**
     * Returns the container managed ValidatorFactory that has been configured
     * for the specified Java EE application module. <p>
     *
     * @param mmd module level metadata; must not be null.
     */
    ValidatorFactory getValidatorFactory(ModuleMetaData mmd);

    /**
     * Returns the container managed ValidatorFactory that has been configured
     * for the specified Java EE application component. <p>
     *
     * This is a convenience method that will obtain the ValidatorFactory for
     * the module corresponding to the specified component metadata, but is
     * also intended to throw a consistent exception when there is no component
     * meta data. It is suggested that this method is used in conjunction with
     * obtaining the current component metadata for the thread, as follows: <p>
     *
     * cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
     * vf = bv.getValidatorFactory( cmd );
     *
     * @param cmd component level metadata.
     *
     * @throws ValidationException if the input parameter is null or a
     *                                 failure occurs obtaining the ValidatorFactory instance.
     */
    ValidatorFactory getValidatorFactory(ComponentMetaData cmd) throws ValidationException;

    /**
     * Returns the container managed ValidatorFactory that has been configured
     * for the specified Java EE application component. If there is no component
     * or one is not available, a default ValidatorFactory is returned. <p>
     *
     * This is a convenience method that will obtain the ValidatorFactory for
     * the module corresponding to the specified component metadata. It is
     * suggested that this method is used in conjunction with
     * obtaining the current component metadata for the thread, as follows: <p>
     *
     * cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
     * vf = bv.getValidatorFactory( cmd );
     *
     * @param cmd component level metadata.
     *
     * @throws ValidationException if a failure occurs obtaining the
     *                                 ValidatorFactory instance.
     */
    ValidatorFactory getValidatorFactoryOrDefault(ComponentMetaData cmd) throws ValidationException;

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
     * vf = bv.getValidator( cmd );
     *
     * @param cmd component level metadata.
     *
     * @throws ValidationException if the input parameter is null or a
     *                                 failure occurs obtaining the Validator instance.
     */
    Validator getValidator(ComponentMetaData cmd) throws ValidationException;

    /**
     * Check if a method requires validation based on existing constraints and applicable executable types.
     *
     * @param method to check whether or not it requires validation
     * @return true if the method should be validated
     * @throws ValidationException
     */
    boolean isMethodConstrained(Method method) throws ValidationException;

    /**
     * Check if a constructor requires validation based on existing constraints and applicable executable types.
     *
     * @param constructor to check whether or not it requires validation
     * @return true if the method should be validated
     * @throws ValidationException
     */
    boolean isConstructorConstrained(Constructor<?> constructor) throws ValidationException;

    /**
     * Register an already create ValidatorFactory with the liberty runtime for the specified Java EE application module.
     * This is mainly useful when a ValidatorFactory had to be created during application statup,
     * but the required module meta data to register it wasn't available on the thread yet. <p>
     *
     * @param mmd              module level metadata; must not be null.
     * @param cl               ClassLoader that contains the module classes of the Validator Factory. These are needed when the factory
     *                             gets configured in the Liberty runtime.
     * @param validatorFactory ValidatorFactory object to register with the Liberty bean validation runtime
     */
    void registerValidatorFactory(ModuleMetaData mmd, ClassLoader cl, ValidatorFactory validatorFactory);
}
