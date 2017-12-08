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

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.validation.Constraint;
import javax.validation.Valid;
import javax.validation.executable.ValidateOnExecution;

import org.hibernate.validator.cdi.ValidationExtension;
import org.hibernate.validator.cdi.internal.ValidatorBean;
import org.hibernate.validator.cdi.internal.ValidatorFactoryBean;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ejs.util.dopriv.SetContextClassLoaderPrivileged;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.beanvalidation.service.Validation20ClassLoader;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;
import com.ibm.ws.util.ThreadContextAccessor;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "api.classes=" +
                        "javax.validation.Validator;" +
                        "javax.validation.ValidatorFactory;" +
                        "org.hibernate.validator.HibernateValidatorFactory;" +
                        "org.hibernate.validator.cdi.internal.ValidatorFactoryBean;" +
                        "org.hibernate.validator.cdi.HibernateValidator;" +
                        "org.hibernate.validator.cdi.ValidationExtension;" +
                        "org.hibernate.validator.cdi.internal.interceptor.ValidationInterceptor;" +
                        "org.hibernate.validator.internal.engine.ValidatorImpl;" +
                        "org.hibernate.validator.internal.engine.ValidatorFactoryImpl;" +
                        "org.hibernate.validator.cdi.internal.InjectingConstraintValidatorFactory;" +
                        "com.ibm.ws.beanvalidation.v20.cdi.internal.LibertyValidatorBean;" +
                        "com.ibm.ws.beanvalidation.v20.cdi.internal.LibertyValidatorFactoryBean",
                        "service.vendor=IBM"
           })
public class LibertyHibernateValidatorExtension implements Extension, WebSphereCDIExtension {
    private static final TraceComponent tc = Tr.register(LibertyHibernateValidatorExtension.class);

    private static final PrivilegedAction<ThreadContextAccessor> getThreadContextAccessorAction = new PrivilegedAction<ThreadContextAccessor>() {
        @Override
        public ThreadContextAccessor run() {
            return ThreadContextAccessor.getThreadContextAccessor();
        }
    };

    /**
     * Privileged action for creating a Validation20ClassLoader.
     */
    private class CreateValidation20ClassLoaderAction implements PrivilegedAction<Validation20ClassLoader> {
        private final ClassLoader parentCL;
        private final String moduleHint;

        private CreateValidation20ClassLoaderAction(ClassLoader parentCL, String moduleHint) {
            this.parentCL = parentCL;
            this.moduleHint = moduleHint;
        }

        @Override
        public Validation20ClassLoader run() {
            return new Validation20ClassLoader(parentCL, moduleHint);
        }
    }

    private ValidatorFactoryBean vfBean;
    private ValidatorFactoryBean hibernateValidatorFactoryBean;
    private ValidatorBean vBean;
    private ValidatorBean hibernateValidatorBean;

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscoveryEvent, BeanManager beanManager) {
        if (vfBean == null) {
            vfBean = hibernateValidatorFactoryBean = new LibertyValidatorFactoryBean();
            afterBeanDiscoveryEvent.addBean(vfBean);
            afterBeanDiscoveryEvent.addBean(hibernateValidatorFactoryBean);
        }

        if (vBean == null) {
            vBean = hibernateValidatorBean = new LibertyValidatorBean();
            afterBeanDiscoveryEvent.addBean(vBean);
            afterBeanDiscoveryEvent.addBean(hibernateValidatorBean);
        }
    }

    private ValidationExtension extDelegate;
    private String currentClassloaderHint = "";

    private ValidationExtension delegate(String newClassloaderHint) {
        if (newClassloaderHint != null && !newClassloaderHint.equals(currentClassloaderHint)) {

            SetContextClassLoaderPrivileged setClassLoader = null;
            ClassLoader oldClassLoader = null;
            try {
                ClassLoader tccl = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {

                    @Override
                    public ClassLoader run() {
                        return Thread.currentThread().getContextClassLoader();
                    }

                });

                ClassLoader bvalClassLoader = AccessController.doPrivileged(new CreateValidation20ClassLoaderAction(tccl, newClassloaderHint));
                ThreadContextAccessor tca = System.getSecurityManager() == null ? ThreadContextAccessor.getThreadContextAccessor() : AccessController.doPrivileged(getThreadContextAccessorAction);

                // set the thread context class loader to be used, must be reset in finally block
                setClassLoader = new SetContextClassLoaderPrivileged(tca);
                oldClassLoader = setClassLoader.execute(bvalClassLoader);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Called setClassLoader with oldClassLoader of" + oldClassLoader + " and newClassLoader of " + bvalClassLoader);
                }
                extDelegate = new ValidationExtension();
                currentClassloaderHint = newClassloaderHint;
            } finally {
                if (setClassLoader != null) {
                    setClassLoader.execute(oldClassLoader);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Set Class loader back to " + oldClassLoader);
                    }
                }
            }
        } else if (extDelegate == null) {
            extDelegate = new ValidationExtension();
        }
        return extDelegate;
    }

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscoveryEvent,
                                    final BeanManager beanManager) {
        delegate(null).beforeBeanDiscovery(beforeBeanDiscoveryEvent, beanManager);
    }

    public void processBean(@Observes ProcessBean<?> processBeanEvent) {
        delegate(null).processBean(processBeanEvent);
    }

    public <T> void processAnnotatedType(@Observes @WithAnnotations({
                                                                      Constraint.class,
                                                                      Valid.class,
                                                                      ValidateOnExecution.class
    }) ProcessAnnotatedType<T> processAnnotatedTypeEvent) {
        Class<?> javaClass = processAnnotatedTypeEvent.getAnnotatedType().getJavaClass();
        String moduleName = getModuleName(javaClass);
        delegate(moduleName).processAnnotatedType(processAnnotatedTypeEvent);
    }

    private String getModuleName(Class<?> javaClass) {
        String moduleName = javaClass.getProtectionDomain().getCodeSource().getLocation().getPath();

        //Handle war module
        if (moduleName.endsWith("/WEB-INF/classes/")) {
            moduleName = moduleName.substring(0, moduleName.length() - 17);
        }

        int index = moduleName.lastIndexOf('/');
        if (index > -1) {
            moduleName = moduleName.substring(index);
        }
        return moduleName;
    }
}
