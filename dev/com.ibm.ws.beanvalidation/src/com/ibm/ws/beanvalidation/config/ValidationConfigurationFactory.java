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
package com.ibm.ws.beanvalidation.config;

import javax.validation.Configuration;
import javax.validation.ConstraintValidatorFactory;

import com.ibm.ws.beanvalidation.service.BeanValidationContext;
import com.ibm.ws.javaee.dd.bval.ValidationConfig;

/**
 * Simple interface to allow implementers to return implementations of {@link ValidationConfigurationInterface}.
 * This can allow the same core config object to be implemented separately for
 * splitting out the Validation API's and CDI implementation and dependencies out
 * of the core container.
 */
public interface ValidationConfigurationFactory {

    /**
     * Create a {@link ValidationConfigurationInterface} object, using the context
     * and the parsed {@link ValidationConfig} to build it.
     * 
     * @param context bean validation context for a module
     * @param config the parsed config from validation.xml if it existed, otherwise null
     * 
     * @return a {@link ValidationConfigurationInterface} object
     */
    ValidationConfigurationInterface createValidationConfiguration(BeanValidationContext context,
                                                                   ValidationConfig config);

    ConstraintValidatorFactory getConstraintValidatorFactoryOverride(Configuration<?> config);
}
