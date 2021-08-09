/*******************************************************************************
 * Copyright (c) 2014, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation.config.internal;

import javax.validation.Configuration;
import javax.validation.ConstraintValidatorFactory;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.beanvalidation.config.ValidationConfigurationFactory;
import com.ibm.ws.beanvalidation.config.ValidationConfigurationInterface;
import com.ibm.ws.beanvalidation.config.ValidationConfigurator;
import com.ibm.ws.beanvalidation.service.BeanValidationContext;
import com.ibm.ws.javaee.dd.bval.ValidationConfig;

/**
 * Core implementation of the {@link ValidationConfigurationFactory}. The base case
 * is to return the root {@link ValidationConfigurator} object with no additional
 * behavior. This satisfies the case when beanValidation-1.0 is enabled.
 */
@Component(service = ValidationConfigurationFactory.class)
public class ValidationConfigurationFactoryImpl implements ValidationConfigurationFactory {

    @Override
    public ValidationConfigurationInterface createValidationConfiguration(BeanValidationContext context,
                                                                          ValidationConfig config) {

        return new ValidationConfigurator(context, config);
    }

    @Override
    public ConstraintValidatorFactory getConstraintValidatorFactoryOverride(Configuration<?> config) {
        return null;
    }

}
