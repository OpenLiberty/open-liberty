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

import javax.validation.ClockProvider;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.TraversableResolver;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;

import com.ibm.ws.beanvalidation.accessor.BeanValidationAccessor;

/**
 * Proxy class for getting the real ValidatorFactory object for a module.
 */
public class LibertyValidatorFactoryProxy implements ValidatorFactory {

    private ValidatorFactory delegate() {
        return BeanValidationAccessor.getValidatorFactory();
    }

    @Override
    public void close() {
        //Make sure the VF wasn't already destroyed during a module shutdown.
        ValidatorFactory vf = delegate();
        if (vf != null) {
            vf.close();
        }
    }

    @Override
    public ClockProvider getClockProvider() {
        return delegate().getClockProvider();
    }

    @Override
    public ConstraintValidatorFactory getConstraintValidatorFactory() {
        return delegate().getConstraintValidatorFactory();
    }

    @Override
    public MessageInterpolator getMessageInterpolator() {
        return delegate().getMessageInterpolator();
    }

    @Override
    public ParameterNameProvider getParameterNameProvider() {
        return delegate().getParameterNameProvider();
    }

    @Override
    public TraversableResolver getTraversableResolver() {
        return delegate().getTraversableResolver();
    }

    @Override
    public Validator getValidator() {
        return delegate().getValidator();
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        return delegate().unwrap(type);
    }

    @Override
    public ValidatorContext usingContext() {
        return delegate().usingContext();
    }
}
