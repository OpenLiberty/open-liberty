/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation.config;

import java.io.IOException;
import java.util.List;

import javax.validation.Configuration;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.ValidationException;
import javax.validation.ValidatorFactory;
import javax.validation.spi.ValidationProvider;

import com.ibm.ws.javaee.dd.bval.Property;
import com.ibm.ws.javaee.dd.bval.ValidationConfig;

/**
 * 
 */
public interface ValidationConfigurationInterface {

    @Override
    public abstract String toString();

    /**
     * Gets the class for the defaultProvider property.
     */
    public abstract Class<ValidationProvider<?>> getDefaultProviderClass();

    /**
     * The driver to configure a {@link Configuration} that the caller can use to
     * create the {@link ValidatorFactory} with.
     * 
     * @return a {@link Configuration} for the validation provider
     * @throws IOException
     */
    public abstract Configuration<?> configure() throws IOException;

    /**
     * Clean up anything that was done during the {@link #configure()} call.
     * 
     * @param vf the ValidatorFactory that needs to get released
     */
    public abstract void release(ValidatorFactory vf);

    /**
     * Creates and sets the {@link MessageInterpolator} on the {@link Configuration} passed in.
     * 
     * @param apiConfig the Validation API configuration object used to
     *            bootstrap the {@link ValidatorFactory}
     * @throws ValidationException
     */
    public abstract void setMessageInterpolator(Configuration<?> apiConfig);

    /**
     * Creates and sets the {@link TraversableResolver} on the {@link Configuration} passed in.
     * 
     * @param apiConfig the Validation API configuration object used to
     *            bootstrap the {@link ValidatorFactory}
     * @throws ValidationException
     */
    public abstract void setTraversableResolver(Configuration<?> apiConfig);

    /**
     * Creates and sets the {@link ConstraintValidatorFactory} on the {@link Configuration} passed in.
     * 
     * @param apiConfig the Validation API configuration object used to
     *            bootstrap the {@link ValidatorFactory}
     * @throws ValidationException
     */
    public abstract void setConstraintValidatorFactory(Configuration<?> apiConfig);

    /**
     * Gets the list of values for the constraintMapping property.
     * 
     * <p>
     * This accessor method returns a reference to the live list, not a snapshot.
     * Therefore any modification that is made the returned list will be present
     * inside the actual parsed {@link ValidationConfig} object.
     */
    public abstract List<String> getConstraintMapping();

    /**
     * Gets the list of values for the property property.
     * 
     * <p>
     * This accessor method returns a reference to the live list, not a snapshot.
     * Therefore any modification that is made to the returned list will be present
     * inside the actual parsed {@link ValidationConfig} object.
     */
    public abstract List<Property> getProperty();

    /**
     * Sets the value of properties in the configuration
     * 
     * @param config
     *            is the configuration object that needs properties to be set.
     * 
     */
    public abstract void setProperties(Configuration<?> config);

    /**
     * Sets the configuration mapping information into the configuration. This method
     * requires the current classloader context to be appropriately set before called.
     * 
     * @param config
     *            is the configuration object that needs to be set.
     * @throws IOException
     */
    public abstract void setConstraintMappings(Configuration<?> config) throws IOException;

    /**
     * Go through each mapping InputStreams and close them
     */
    public abstract void closeMappingFiles();

    /*
     * This method will clear out the reference to the classloader to help ensure the reference is not held on for ever. However, we cannot guarantee
     * the method will be called so those paths are an issue. Today we think they will only be in the application path and when the app dies so do all
     * references to the WASProxyValidatorFactory so it will be garbarge collected.
     */
    public abstract void clearClassLoader();

    /**
     * This method will return the classloader
     * 
     * @return appClassLoader will return the application module's class loader This will never be null
     * 
     */
    public abstract ClassLoader getAppClassLoader();

}
