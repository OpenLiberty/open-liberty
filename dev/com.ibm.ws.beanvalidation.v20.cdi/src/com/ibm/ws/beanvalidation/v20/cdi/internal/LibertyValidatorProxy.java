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
import java.util.concurrent.ConcurrentHashMap;

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

    private final Map<ModuleMetaData, Validator> validatorCache = new ConcurrentHashMap<ModuleMetaData, Validator>(6, 0.9f, 1);

    private Validator delegate() {
        ModuleMetaData mmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData().getModuleMetaData();
        Validator validator = validatorCache.get(mmd);
        if (validator == null) {
            validator = BeanValidationAccessor.getValidatorFactory().getValidator();
            validatorCache.put(mmd, validator);
        }
        return validator;
    }

    @Override
    public ExecutableValidator forExecutables() {
        return delegate().forExecutables();
    }

    @Override
    public BeanDescriptor getConstraintsForClass(Class<?> clazz) {
        return delegate().getConstraintsForClass(clazz);
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        return delegate().unwrap(type);
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {
        return delegate().validate(object, groups);
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName, Class<?>... groups) {
        return delegate().validateProperty(object, propertyName, groups);
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value, Class<?>... groups) {
        return delegate().validateValue(beanType, propertyName, value, groups);
    }

    @Override
    public void moduleMetaDataCreated(MetaDataEvent<ModuleMetaData> event) throws MetaDataException {
        // no-op

    }

    @Override
    public void moduleMetaDataDestroyed(MetaDataEvent<ModuleMetaData> event) {
        validatorCache.remove(event.getMetaData());
    }
}
