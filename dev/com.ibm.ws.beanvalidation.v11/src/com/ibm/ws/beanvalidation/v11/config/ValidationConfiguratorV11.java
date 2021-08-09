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
package com.ibm.ws.beanvalidation.v11.config;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.validation.Configuration;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.ParameterNameProvider;
import javax.validation.ValidationException;
import javax.validation.ValidatorFactory;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.beanvalidation.config.ValidationConfigurator;
import com.ibm.ws.beanvalidation.service.BeanValidationContext;
import com.ibm.ws.beanvalidation.service.ValidationReleasable;
import com.ibm.ws.beanvalidation.service.ValidationReleasableFactory;
import com.ibm.ws.javaee.dd.bval.ValidationConfig;
import com.ibm.ws.managedobject.ManagedObject;

/**
 * Subclass of the base {@link ValidationConfigurator} implementation so that
 * the dependency on API's from 1.1 can be held in this bundle and not the core
 * bval container bundle.
 */
public class ValidationConfiguratorV11 extends ValidationConfigurator {

    protected String parameterNameProvider;

    private final ValidationReleasableFactory releasableFactory;
    private List<ValidationReleasable<?>> releasables;

    public ValidationConfiguratorV11(BeanValidationContext bvContext,
                                     ValidationConfig config,
                                     ValidationReleasableFactory releasableFactory) {
        super(bvContext, config);

        if (config != null) {
            parameterNameProvider = config.getParameterNameProvider();
            parameterNameProvider = parameterNameProvider != null ? parameterNameProvider.trim() : null;
        }

        this.releasableFactory = releasableFactory;
    }

    /**
     * This constructor should only be used to get an instance of ValidationConfigurator to call
     * getConstraintValidatorFactoryOverride(). It sets the bare minimum, and isn't guaranteed to work
     * for other methods.
     */
    public ValidationConfiguratorV11(ValidationReleasableFactory releasableFactory) {
        super();
        this.releasableFactory = releasableFactory;
    }

    @Override
    public Configuration<?> configure() throws IOException {
        Configuration<?> configuration = super.configure();

        // Set the parameter name provider after the super does any common configuration
        // setup.
        setParameterNameProvider(configuration);

        return configuration;

    }

    @Override
    public void release(ValidatorFactory vf) {
        super.release(vf);

        if (releasables != null) {
            for (ValidationReleasable<?> releasable : releasables) {
                releasable.release();
            }
        }

        if (vf != null) {
            vf.close();
        }
    }

    @Override
    public void setConstraintValidatorFactory(Configuration<?> apiConfig) {
        ValidationReleasable<ConstraintValidatorFactory> releasable = null;

        // If the cdi feature is enabled AND a custom constraint validator wasn't
        // specified we can try to create our own implementation of it.
        if (constraintValidatorFactory == null && releasableFactory != null) {
            releasable = releasableFactory.createConstraintValidatorFactory();
        }

        if (releasable != null) {
            if (releasables == null) {
                releasables = new LinkedList<ValidationReleasable<?>>();
            }

            releasables.add(releasable);

            apiConfig.constraintValidatorFactory(releasable.getInstance());
        } else {
            // Either the cdi feature isn't enabled or the application isn't enabled
            // for cdi, which means it should be set the default way.
            super.setConstraintValidatorFactory(apiConfig);
        }
    }

    /**
     * Creates and sets the {@link ParameterNameProvider} on the {@link Configuration} passed in.
     * 
     * @param apiConfig the Validation API configuration object used to
     *            bootstrap the {@link ValidatorFactory}
     * @throws ValidationException
     */
    public void setParameterNameProvider(Configuration<?> apiConfig) {
        Class<? extends ParameterNameProvider> clazz = null;
        ParameterNameProvider pnp = null;
        if (parameterNameProvider != null && versionID >= 11) {
            try {
                clazz = loadClass(parameterNameProvider).asSubclass(ParameterNameProvider.class);
                pnp = instantiateClass(clazz);
            } catch (Throwable t) {
                ValidationException e = new ValidationException(t);
                throw e;
            }
        }

        if (pnp != null) {
            apiConfig.parameterNameProvider(pnp);
        }
    }

    /**
     * Override the base implementation so that when a class is instantiated
     * it is done so as a CDI managed bean.
     */
    @Override
    protected <T> T instantiateClass(Class<T> clazz) throws Throwable {
        // Get the current BeanManager for this module. If one doesn't exist it is because 
        // the module isn't CDI enabled and we fall back to the original behavior.

        ManagedObject<T> mo = null;
        if (releasableFactory != null) {
            mo = releasableFactory.createValidationReleasable(clazz);
        }

        if (mo != null) {
            if (releasables == null) {
                releasables = new LinkedList<ValidationReleasable<?>>();
            }

            // Save off the context under which the class was created as a CDI managed
            // bean so it can be cleaned up upon stopping the module.
            //releasables.add(mo);

            return mo.getObject();
        } else {
            return super.instantiateClass(clazz);
        }
    }

    @Trivial
    @Override
    protected void dump(StringBuffer sb) {
        super.dump(sb);
        sb.append(NEW_LINE);
        sb.append(" ### Added in Bean Validation 1.1 ###").append(NEW_LINE).append(NEW_LINE);
        sb.append(" parameterNameProvider: " + parameterNameProvider).append(NEW_LINE);
    }

    /**
     * Get the default WebSphere ConstraintValidatorFactory as a managed object.
     * 
     * @return a managed object instance of ReleasableConstraintValidatorFactory {@link ConstraintValidatorFactory }
     * 
     * @throws ValidationException
     */
    public ConstraintValidatorFactory getConstraintValidatorFactoryOverride(Configuration<?> config) {
        ValidationReleasable<ConstraintValidatorFactory> releasable = null;

        String cvfClassName = config.getBootstrapConfiguration().getConstraintValidatorFactoryClassName();
        // If the validation.xml ConstraintValidatorFactory is null AND the CDI feature is enabled
        // we can try to create our own implementation of it.
        if (cvfClassName == null && releasableFactory != null) {
            releasable = releasableFactory.createConstraintValidatorFactory();
        }

        if (releasable != null) {
            if (releasables == null) {
                releasables = new LinkedList<ValidationReleasable<?>>();
            }

            releasables.add(releasable);
            return releasable.getInstance();
        }
        return null;
    }
}
