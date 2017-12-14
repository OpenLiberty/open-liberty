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
import java.util.WeakHashMap;

import javax.validation.ClockProvider;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.TraversableResolver;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;

import com.ibm.ws.beanvalidation.accessor.BeanValidationAccessor;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.metadata.ModuleMetaDataListener;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

/**
 * Proxy class for getting the real ValidatorFactory object for a module.
 */
public class LibertyValidatorFactoryProxy implements ValidatorFactory, ModuleMetaDataListener {

    private final Map<ModuleMetaData, ValidatorFactory> validatorFactoryCache = new WeakHashMap<ModuleMetaData, ValidatorFactory>();

    private ValidatorFactory delegate() {
        ModuleMetaData mmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData().getModuleMetaData();
        ValidatorFactory vf = validatorFactoryCache.get(mmd);
        if (vf == null) {
            vf = BeanValidationAccessor.getValidatorFactory();
            validatorFactoryCache.put(mmd, vf);
        }
        return vf;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.validation.ValidatorFactory#close()
     */
    @Override
    public void close() {
        for (ValidatorFactory vf : validatorFactoryCache.values()) {
            vf.close();
        }
        validatorFactoryCache.clear();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.validation.ValidatorFactory#getClockProvider()
     */
    @Override
    public ClockProvider getClockProvider() {
        return delegate().getClockProvider();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.validation.ValidatorFactory#getConstraintValidatorFactory()
     */
    @Override
    public ConstraintValidatorFactory getConstraintValidatorFactory() {
        return delegate().getConstraintValidatorFactory();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.validation.ValidatorFactory#getMessageInterpolator()
     */
    @Override
    public MessageInterpolator getMessageInterpolator() {
        return delegate().getMessageInterpolator();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.validation.ValidatorFactory#getParameterNameProvider()
     */
    @Override
    public ParameterNameProvider getParameterNameProvider() {
        return delegate().getParameterNameProvider();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.validation.ValidatorFactory#getTraversableResolver()
     */
    @Override
    public TraversableResolver getTraversableResolver() {
        return delegate().getTraversableResolver();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.validation.ValidatorFactory#getValidator()
     */
    @Override
    public Validator getValidator() {
        return delegate().getValidator();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.validation.ValidatorFactory#unwrap(java.lang.Class)
     */
    @Override
    public <T> T unwrap(Class<T> type) {
        return delegate().unwrap(type);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.validation.ValidatorFactory#usingContext()
     */
    @Override
    public ValidatorContext usingContext() {
        return delegate().usingContext();
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
        validatorFactoryCache.remove(event.getMetaData());

    }

}
