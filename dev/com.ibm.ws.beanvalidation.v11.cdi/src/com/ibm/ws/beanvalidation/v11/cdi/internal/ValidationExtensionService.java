/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation.v11.cdi.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.apache.bval.cdi.BValExtension;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.util.Util;
import com.ibm.ejs.util.dopriv.SetContextClassLoaderPrivileged;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.beanvalidation.AbstractBeanValidation.ClassLoaderTuple;
import com.ibm.ws.beanvalidation.service.BeanValidation;
import com.ibm.ws.beanvalidation.service.ValidationReleasable;
import com.ibm.ws.javaee.dd.bval.ValidationConfig;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.util.ThreadContextAccessor;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * DS to provide bean validation and class loading services to the bean validation CDI extension.
 * This class is meant to be extended by an OSGi Component class that implements javax.enterprise.inject.spi.Extension
 */
public abstract class ValidationExtensionService {

    private static final TraceComponent tc = Tr.register(ValidationExtensionService.class);

    private static final String REFERENCE_BEANVALIDATION_SERVICE = "beanValidation";
    private static final String REFERENCE_CLASSLOADING_SERVICE = "classLoadingService";

    protected final AtomicServiceReference<BeanValidation> beanValidation = new AtomicServiceReference<BeanValidation>(REFERENCE_BEANVALIDATION_SERVICE);
    final AtomicServiceReference<ClassLoadingService> classLoadingServiceSR = new AtomicServiceReference<ClassLoadingService>(REFERENCE_CLASSLOADING_SERVICE);

    protected ValidationConfig validationConfig;

    private static volatile ValidationExtensionService svInstance = null;

    protected static final PrivilegedAction<ThreadContextAccessor> getThreadContextAccessorAction = new PrivilegedAction<ThreadContextAccessor>() {
        @Override
        public ThreadContextAccessor run() {
            return ThreadContextAccessor.getThreadContextAccessor();
        }
    };

    static ValidationExtensionService instance() {
        return svInstance;
    }

    static void setInstance(ValidationExtensionService instance) {
        if (svInstance != null && instance != null) {
            throw new IllegalStateException("instance already set");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setInstance : " + Util.identity(instance));
        svInstance = instance;
    }

    /**
     * Called by DS to activate this service
     *
     * @param compcontext the context of this component
     */
    protected void activate(ComponentContext compcontext) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Activating " + this.getClass().getName());
        }
        setInstance(this);
        this.beanValidation.activate(compcontext);
        this.classLoadingServiceSR.activate(compcontext);
    }

    /**
     * Called by DS to deactivate this service
     *
     * @param compcontext the context of this component
     */
    protected void deactivate(ComponentContext compcontext) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Deactivating " + this.getClass().getName());
        }
        setInstance(null);
        this.beanValidation.deactivate(compcontext);
        this.classLoadingServiceSR.deactivate(compcontext);
    }

    /**
     * Called by DS to set the service reference
     *
     * @param ref the reference from DS
     */
    @Reference(name = REFERENCE_BEANVALIDATION_SERVICE, service = BeanValidation.class)
    protected void setBeanValidation(ServiceReference<BeanValidation> ref) {
        this.beanValidation.setReference(ref);
    }

    /**
     * Called by DS to remove the service reference
     *
     * @param ref the reference from DS
     */
    protected void unsetBeanValidation(ServiceReference<BeanValidation> ref) {
        this.beanValidation.unsetReference(ref);
    }

    /**
     * Called by DS to set the service reference
     *
     * @param ref the reference from DS
     */
    @Reference(name = REFERENCE_CLASSLOADING_SERVICE,
               service = ClassLoadingService.class)
    protected void setClassLoadingService(ServiceReference<ClassLoadingService> ref) {
        classLoadingServiceSR.setReference(ref);
    }

    /**
     * Called by DS to remove the service reference
     *
     * @param ref the reference from DS
     */
    protected void unsetClassLoadingService(ServiceReference<ClassLoadingService> ref) {
        classLoadingServiceSR.unsetReference(ref);
    }

    public Validator getDefaultValidator() {
        BeanValidation beanValidation = instance().beanValidation.getService();
        if (beanValidation == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Returning a null Validator because the BeanValidation service is not currently unavailable. Is the bean validation feature enabled?");
            }
            return null;
        }
        try {
            ComponentMetaData componentMetaData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
            return beanValidation.getValidator(componentMetaData);
        } catch (ValidationException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Returning a null Validator: " + e.getMessage());
            }
            return null;
        }
    }

    public ValidatorFactory getDefaultValidatorFactory() {
        BeanValidation beanValidation = instance().beanValidation.getService();
        if (beanValidation == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Returning a null ValidatorFactory because the BeanValidation service is not currently available.  Is the bean validation feature enabled?");
            }
            return null;
        }
        try {
            ComponentMetaData componentMetaData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
            return beanValidation.getValidatorFactory(componentMetaData);
        } catch (ValidationException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Returning a null ValidatorFactory: " + e.getMessage());
            }
            return null;
        }
    }

    public BValExtension createBValExtension(BeanManager beanManager) {
        ValidationExtensionService validatorService = instance();
        if (validatorService == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Returning a null BValExtension object because the validatorService service is not currently unavailable.");
            }
            return null;
        }
        SetContextClassLoaderPrivileged setClassLoader = null;
        ClassLoader oldClassLoader = null;
        ClassLoaderTuple tuple = null;
        BValExtension bValExtension;
        try {
            tuple = configureBvalClassloader(null);
            ThreadContextAccessor tca = System.getSecurityManager() == null ? ThreadContextAccessor.getThreadContextAccessor() : AccessController.doPrivileged(getThreadContextAccessorAction);

            // set the thread context class loader to be used, must be reset in finally block
            setClassLoader = new SetContextClassLoaderPrivileged(tca);
            oldClassLoader = setClassLoader.execute(tuple.classLoader);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Called setClassLoader with oldClassLoader of" + oldClassLoader + " and newClassLoader of " + tuple.classLoader);
            }

            //create a BValExtension Bean since the BValInterceptor injects one.
            bValExtension = createValidationReleasable(beanManager, BValExtension.class).getInstance();
        } catch (ValidationException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Returning a null Configuration: " + e.getMessage());
            }
            bValExtension = null;
        } finally {
            if (setClassLoader != null) {
                setClassLoader.execute(oldClassLoader);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Set Class loader back to " + oldClassLoader);
                }
            }
            releaseLoader(tuple);
        }
        return bValExtension;
    }

    private ClassLoader createTCCL(ClassLoader parentCL) {
        return classLoadingServiceSR.getServiceWithException().createThreadContextClassLoader(parentCL);
    }

    protected void releaseLoader(ClassLoaderTuple tuple) {
        if (tuple != null && tuple.wasCreatedViaClassLoadingService) {
            classLoadingServiceSR.getServiceWithException().destroyThreadContextClassLoader(tuple.classLoader);
        }
    }

    private static PrivilegedAction<ClassLoader> getContextClassLoaderAction = new PrivilegedAction<ClassLoader>() {
        @Override
        public ClassLoader run() {
            return Thread.currentThread().getContextClassLoader();
        }
    };

    private static ClassLoader getContextClassLoader() {
        if (System.getSecurityManager() == null)
            return Thread.currentThread().getContextClassLoader();
        else
            return AccessController.doPrivileged(getContextClassLoaderAction);
    }

    protected ClassLoaderTuple configureBvalClassloader(ClassLoader cl) {
        if (cl == null) {
            cl = getContextClassLoader();
        }
        if (cl != null) {
            ClassLoadingService classLoadingService = classLoadingServiceSR.getServiceWithException();
            if (classLoadingService.isThreadContextClassLoader(cl)) {
                return ClassLoaderTuple.of(cl, false);
            } else if (classLoadingService.isAppClassLoader(cl)) {
                return ClassLoaderTuple.of(createTCCL(cl), true);
            }
        }
        return ClassLoaderTuple.of(createTCCL(BeanValidation.class.getClassLoader()), true);
    }

    private <T> ValidationReleasable<T> createValidationReleasable(BeanManager beanManager, Class<T> clazz) {
        // If the bean manger isn't null, this indicates that the module that is
        // invoking this code path has CDI enabled.
        if (beanManager != null) {
            final AnnotatedType<T> annotatedType = beanManager.createAnnotatedType(clazz);
            final InjectionTarget<T> it = beanManager.createInjectionTarget(annotatedType);
            final CreationalContext<T> context = beanManager.createCreationalContext(null);
            final T instance = it.produce(context);
            it.inject(instance, context);
            it.postConstruct(instance);

            return new ValidationReleasableImpl<T>(context, it, instance);
        }
        return null;
    }
}
