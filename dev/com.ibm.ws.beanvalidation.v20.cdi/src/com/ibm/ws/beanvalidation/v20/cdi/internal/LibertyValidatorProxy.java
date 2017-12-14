/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation.v20.cdi.internal;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.executable.ExecutableValidator;
import javax.validation.metadata.BeanDescriptor;

import com.ibm.ws.beanvalidation.accessor.BeanValidationAccessor;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.metadata.ModuleMetaDataListener;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

/**
 * Proxy class for getting the real Validator object for a module.
 */
public class LibertyValidatorProxy implements Validator, ModuleMetaDataListener {

    private final Map<ModuleMetaData, Validator> validatorCache = new WeakHashMap<ModuleMetaData, Validator>();

    private Validator delegate() {
        ModuleMetaData mmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData().getModuleMetaData();
        Validator validator = validatorCache.get(mmd);
        if (validator == null) {
            validator = BeanValidationAccessor.getValidatorFactory().getValidator();
            validatorCache.put(mmd, validator);
        }
        return validator;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.validation.Validator#forExecutables()
     */
    @Override
    public ExecutableValidator forExecutables() {
        return delegate().forExecutables();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.validation.Validator#getConstraintsForClass(java.lang.Class)
     */
    @Override
    public BeanDescriptor getConstraintsForClass(Class<?> clazz) {
        return delegate().getConstraintsForClass(clazz);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.validation.Validator#unwrap(java.lang.Class)
     */
    @Override
    public <T> T unwrap(Class<T> type) {
        return delegate().unwrap(type);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.validation.Validator#validate(java.lang.Object, java.lang.Class[])
     */
    @Override
    public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {
        return delegate().validate(object, groups);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.validation.Validator#validateProperty(java.lang.Object, java.lang.String, java.lang.Class[])
     */
    @Override
    public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName, Class<?>... groups) {
        return delegate().validateProperty(object, propertyName, groups);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.validation.Validator#validateValue(java.lang.Class, java.lang.String, java.lang.Object, java.lang.Class[])
     */
    @Override
    public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value, Class<?>... groups) {
        return delegate().validateValue(beanType, propertyName, value, groups);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.container.service.metadata.ModuleMetaDataListener#moduleMetaDataCreated(com.ibm.ws.container.service.metadata.MetaDataEvent)
     */
    @Override
    public void moduleMetaDataCreated(MetaDataEvent<ModuleMetaData> event) throws MetaDataException {
        // no-op

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.container.service.metadata.ModuleMetaDataListener#moduleMetaDataDestroyed(com.ibm.ws.container.service.metadata.MetaDataEvent)
     */
    @Override
    public void moduleMetaDataDestroyed(MetaDataEvent<ModuleMetaData> event) {
        validatorCache.remove(event.getMetaData());

    }

}
