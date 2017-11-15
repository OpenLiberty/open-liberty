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
package com.ibm.ws.beanvalidation.v20.config.internal;

import javax.validation.Configuration;
import javax.validation.ConstraintValidatorFactory;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.beanvalidation.config.ValidationConfigurationFactory;
import com.ibm.ws.beanvalidation.config.ValidationConfigurationInterface;
import com.ibm.ws.beanvalidation.service.BeanValidationContext;
import com.ibm.ws.beanvalidation.service.ValidationReleasableFactory;
import com.ibm.ws.beanvalidation.v20.config.ValidationConfiguratorV20;
import com.ibm.ws.javaee.dd.bval.ValidationConfig;

/**
 * This is the beanValidation-2.0 specific implementation. It allows us to return
 * a subclass configurator that knows about the v20 bval features. Use service.ranking
 * of 20 to represent the v.2.0 bval version.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM",
                        "service.ranking:Integer=20" })
public class ValidationConfigurationV20FactoryImpl implements ValidationConfigurationFactory {

    @Reference
    protected ValidationReleasableFactory releaseableValFactory;

    @Override
    public ValidationConfigurationInterface createValidationConfiguration(BeanValidationContext context, ValidationConfig config) {
        return new ValidationConfiguratorV20(context, config, releaseableValFactory);
    }

    @Override
    public ConstraintValidatorFactory getConstraintValidatorFactoryOverride(Configuration<?> config) {
        return new ValidationConfiguratorV20(releaseableValFactory).getConstraintValidatorFactoryOverride(config);
    }

}
