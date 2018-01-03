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
package com.ibm.ws.beanvalidation.v20;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.validation.Configuration;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

import org.hibernate.validator.HibernateValidatorConfiguration;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.beanvalidation.service.Validation20ClassLoader;
import com.ibm.ws.beanvalidation.service.ValidationReleasableFactory;
import com.ibm.ws.beanvalidation.service.ValidatorFactoryBuilder;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE,
           immediate = true)
public class ValidatorFactoryBuilderImpl implements ValidatorFactoryBuilder {

    private static final String REFERENCE_VALIDATION_RELEASABLE_FACTORY = "ValidationReleasableFactory";
    private final AtomicServiceReference<ValidationReleasableFactory> validationReleasableFactorySR = new AtomicServiceReference<ValidationReleasableFactory>(REFERENCE_VALIDATION_RELEASABLE_FACTORY);

    @Override
    public void closeValidatorFactory(ValidatorFactory vf) {
        if (vf != null) {
            vf.close();
        }
    }

    @Override
    public ValidatorFactory buildValidatorFactory(final ClassLoader appClassLoader, final String containerPath) {

        ClassLoader bvalClassLoader = AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> new Validation20ClassLoader(appClassLoader, containerPath));

        Configuration<?> config = Validation.byDefaultProvider().configure();

        if (config instanceof HibernateValidatorConfiguration) {
            HibernateValidatorConfiguration hvConfig = ((HibernateValidatorConfiguration) config);
            hvConfig.externalClassLoader(bvalClassLoader);
        }

        if (validationReleasableFactorySR.getReference() != null) {
            ValidationReleasableFactory releasableFactory = validationReleasableFactorySR.getServiceWithException();
            return releasableFactory.injectValidatorFactoryResources(config, appClassLoader);
        } else {
            return config.buildValidatorFactory();
        }
    }

    @Activate
    protected void activate(ComponentContext cc) {
        validationReleasableFactorySR.activate(cc);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        validationReleasableFactorySR.deactivate(cc);
    }

    @Reference(name = REFERENCE_VALIDATION_RELEASABLE_FACTORY,
               service = ValidationReleasableFactory.class,
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.STATIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setValidationReleasableFactory(ServiceReference<ValidationReleasableFactory> factoryRef) {
        validationReleasableFactorySR.setReference(factoryRef);
    }

    protected void unsetValidationReleasableFactory(ServiceReference<ValidationReleasableFactory> factoryRef) {
        validationReleasableFactorySR.unsetReference(factoryRef);
    }
}
