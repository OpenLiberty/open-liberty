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

import com.ibm.ejs.util.dopriv.SetContextClassLoaderPrivileged;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.beanvalidation.service.ValidationReleasableFactory;
import com.ibm.ws.beanvalidation.service.ValidatorFactoryBuilder;
import com.ibm.ws.kernel.service.util.PrivHelper;
import com.ibm.ws.util.ThreadContextAccessor;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 *
 */
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE,
           immediate = true)
public class ValidatorFactoryBuilderImpl implements ValidatorFactoryBuilder {
    private static final TraceComponent tc = Tr.register(ValidatorFactoryBuilderImpl.class);

    private static final String REFERENCE_CLASSLOADING_SERVICE = "classLoadingService";
    private static final String REFERENCE_VALIDATION_RELEASABLE_FACTORY = "ValidationReleasableFactory20";

    private final AtomicServiceReference<ClassLoadingService> classLoadingServiceSR = new AtomicServiceReference<ClassLoadingService>(REFERENCE_CLASSLOADING_SERVICE);
    private final AtomicServiceReference<ValidationReleasableFactory> validationReleasableFactorySR = new AtomicServiceReference<ValidationReleasableFactory>(REFERENCE_VALIDATION_RELEASABLE_FACTORY);

    @Override
    public void closeValidatorFactory(ValidatorFactory vf) {
        vf.close();
    }

    @Override
    public ValidatorFactory buildValidatorFactory(ClassLoader appClassLoader, String containerPath) {
        ClassLoader tcclClassLoader = null;
        SetContextClassLoaderPrivileged setClassLoader = null;
        ClassLoader oldClassLoader = null;
        try {
            tcclClassLoader = configureBvalClassloader(this.getClass().getClassLoader());
            ClassLoader bvalClassLoader = new Validation20ClassLoader(appClassLoader, containerPath);

            ThreadContextAccessor tca = System.getSecurityManager() == null ? ThreadContextAccessor.getThreadContextAccessor() : AccessController.doPrivileged(getThreadContextAccessorAction);

            // set the thread context class loader to be used, must be reset in finally block
            setClassLoader = new SetContextClassLoaderPrivileged(tca);
            oldClassLoader = setClassLoader.execute(tcclClassLoader);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Called setClassLoader with oldClassLoader of" + oldClassLoader + " and newClassLoader of " + tcclClassLoader);
            }
            Configuration<?> config = Validation.byDefaultProvider().configure();

            HibernateValidatorConfiguration hvConfig = ((HibernateValidatorConfiguration) config);
            hvConfig.externalClassLoader(bvalClassLoader);

            if (validationReleasableFactorySR.getReference() != null) {
                ValidationReleasableFactory releasableFactory = validationReleasableFactorySR.getServiceWithException();
                return releasableFactory.injectValidatorFactoryResources(config, appClassLoader);
            } else {
                return config.buildValidatorFactory();
            }
        } finally {
            if (setClassLoader != null) {
                setClassLoader.execute(oldClassLoader);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Set Class loader back to " + oldClassLoader);
                }
            }
            if (setClassLoader != null && setClassLoader.wasChanged) {
                releaseLoader(tcclClassLoader);
            }
        }

    }

    private static final PrivilegedAction<ThreadContextAccessor> getThreadContextAccessorAction = new PrivilegedAction<ThreadContextAccessor>() {
        @Override
        public ThreadContextAccessor run() {
            return ThreadContextAccessor.getThreadContextAccessor();
        }
    };

    public void releaseLoader(ClassLoader tccl) {
        classLoadingServiceSR.getServiceWithException().destroyThreadContextClassLoader(tccl);
    }

    public ClassLoader configureBvalClassloader(ClassLoader cl) {
        if (cl == null) {
            cl = PrivHelper.getContextClassLoader();
        }
        if (cl != null) {
            ClassLoadingService classLoadingService = classLoadingServiceSR.getServiceWithException();
            if (classLoadingService.isThreadContextClassLoader(cl)) {
                return cl;
            } else if (classLoadingService.isAppClassLoader(cl)) {
                return createTCCL(cl);
            }
        }
        return createTCCL(ValidatorFactoryBuilderImpl.class.getClassLoader());
    }

    private ClassLoader createTCCL(ClassLoader parentCL) {
        return classLoadingServiceSR.getServiceWithException().createThreadContextClassLoader(parentCL);
    }

    @Reference(name = REFERENCE_CLASSLOADING_SERVICE,
               service = ClassLoadingService.class)
    protected void setClassLoadingService(ServiceReference<ClassLoadingService> ref) {
        classLoadingServiceSR.setReference(ref);
    }

    protected void unsetClassLoadingService(ServiceReference<ClassLoadingService> ref) {
        classLoadingServiceSR.unsetReference(ref);
    }

    @Activate
    protected void activate(ComponentContext cc) {
        classLoadingServiceSR.activate(cc);
        validationReleasableFactorySR.activate(cc);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        classLoadingServiceSR.deactivate(cc);
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
