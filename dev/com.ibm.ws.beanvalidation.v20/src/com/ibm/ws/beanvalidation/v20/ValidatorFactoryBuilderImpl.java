/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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

import com.ibm.ws.beanvalidation.service.BvalManagedObjectBuilder;
import com.ibm.ws.beanvalidation.service.Validation20ClassLoader;
import com.ibm.ws.beanvalidation.service.ValidatorFactoryBuilder;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE,
           immediate = true)
public class ValidatorFactoryBuilderImpl implements ValidatorFactoryBuilder {

    private static final String REFERENCE_BVAL_MANAGED_OBJECT_BUILDER = "BvalManagedObjectBuilder";
    private final AtomicServiceReference<BvalManagedObjectBuilder> bvalManagedObjectBuilderSR = new AtomicServiceReference<BvalManagedObjectBuilder>(REFERENCE_BVAL_MANAGED_OBJECT_BUILDER);

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

        if (bvalManagedObjectBuilderSR.getReference() != null) {
            BvalManagedObjectBuilder bvalManagedObjectBuilder = bvalManagedObjectBuilderSR.getServiceWithException();
            return bvalManagedObjectBuilder.injectValidatorFactoryResources(config, appClassLoader);
        } else {
            return config.buildValidatorFactory();
        }
    }

    @Activate
    protected void activate(ComponentContext cc) {
        bvalManagedObjectBuilderSR.activate(cc);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        bvalManagedObjectBuilderSR.deactivate(cc);
    }

    @Reference(name = REFERENCE_BVAL_MANAGED_OBJECT_BUILDER,
               service = BvalManagedObjectBuilder.class,
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.STATIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setBvalManagedObjectBuilder(ServiceReference<BvalManagedObjectBuilder> builderRef) {
        bvalManagedObjectBuilderSR.setReference(builderRef);
    }

    protected void unsetBvalManagedObjectBuilder(ServiceReference<BvalManagedObjectBuilder> builderRef) {
        bvalManagedObjectBuilderSR.unsetReference(builderRef);
    }
}
