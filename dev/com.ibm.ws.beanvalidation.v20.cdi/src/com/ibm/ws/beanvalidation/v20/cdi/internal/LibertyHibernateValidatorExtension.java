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
package com.ibm.ws.beanvalidation.v20.cdi.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;

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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.util.dopriv.SetContextClassLoaderPrivileged;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.beanvalidation.service.Validation20ClassLoader;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.util.ThreadContextAccessor;
import com.ibm.wsspi.cdi.extension.WebSphereCDIExtension;
import com.ibm.wsspi.classloading.ClassLoadingService;

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

    @Reference
    protected ClassLoadingService classLoadingService;

    private ValidatorFactoryBean vfBean;
    private ValidatorBean vBean;
    private ValidationExtension extDelegate;
    private String currentClassloaderHint = "";
    private boolean delegateFailed;

    private ValidationExtension delegate(String newClassloaderHint) {
        if (extDelegate == null || newClassloaderHint != null && !newClassloaderHint.equals(currentClassloaderHint)) {

            ClassLoader tcclClassLoader = null;
            SetContextClassLoaderPrivileged setClassLoader = null;
            ClassLoader oldClassLoader = null;
            try {
                final ClassLoader tcclClassLoaderTmp = tcclClassLoader = configureBvalClassloader(null);

                ClassLoader bvalClassLoader;
                if (newClassloaderHint != null) {
                    bvalClassLoader = AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> new Validation20ClassLoader(tcclClassLoaderTmp, newClassloaderHint));
                } else {
                    bvalClassLoader = tcclClassLoaderTmp;
                }

                ThreadContextAccessor tca = AccessController.doPrivileged((PrivilegedAction<ThreadContextAccessor>) () -> ThreadContextAccessor.getThreadContextAccessor());

                // set the thread context class loader to be used, must be reset in finally block
                setClassLoader = new SetContextClassLoaderPrivileged(tca);
                oldClassLoader = setClassLoader.execute(bvalClassLoader);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Called setClassLoader with oldClassLoader of " + oldClassLoader + " and newClassLoader of " + bvalClassLoader);
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
                if (setClassLoader != null && setClassLoader.wasChanged) {
                    releaseLoader(tcclClassLoader);
                }
            }
        }
        return extDelegate;
    }

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscoveryEvent, final BeanManager beanManager) {
        if (!delegateFailed)
            try {
                delegate(null).beforeBeanDiscovery(beforeBeanDiscoveryEvent, beanManager);
            } catch (Exception e) {
                delegateFailed = true;
                String appName = getAppName();
                if (tc.isWarningEnabled())
                    Tr.warning(tc, "UNABLE_TO_REGISTER_WITH_CDI", appName == null ? beanManager.toString() : appName, e);
            }
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscoveryEvent, BeanManager beanManager) {
        if (delegateFailed)
            return;

        if (vfBean == null) {
            vfBean = new LibertyValidatorFactoryBean();
            afterBeanDiscoveryEvent.addBean(vfBean);
        }

        if (vBean == null) {
            vBean = new LibertyValidatorBean();
            afterBeanDiscoveryEvent.addBean(vBean);
        }
    }

    public void processBean(@Observes ProcessBean<?> processBeanEvent) {
        if (delegateFailed)
            return;

        delegate(null).processBean(processBeanEvent);
    }

    public <T> void processAnnotatedType(@Observes @WithAnnotations({
                                                                      Constraint.class,
                                                                      Valid.class,
                                                                      ValidateOnExecution.class
    }) ProcessAnnotatedType<T> processAnnotatedTypeEvent) {
        if (delegateFailed)
            return;

        Class<?> javaClass = processAnnotatedTypeEvent.getAnnotatedType().getJavaClass();
        String moduleName = getModuleName(javaClass);
        delegate(moduleName).processAnnotatedType(processAnnotatedTypeEvent);
    }

    private String getModuleName(final Class<?> javaClass) {
        ProtectionDomain protectionDomain = AccessController.doPrivileged((PrivilegedAction<ProtectionDomain>) () -> javaClass.getProtectionDomain());

        String moduleName = protectionDomain.getCodeSource().getLocation().getPath();

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

    private ClassLoadingService getClassLoadingService() {
        Bundle bundle = FrameworkUtil.getBundle(ClassLoadingService.class);
        ClassLoadingService classLoadingService = AccessController.doPrivileged((PrivilegedAction<ClassLoadingService>) () -> {
            BundleContext bCtx = bundle.getBundleContext();
            ServiceReference<ClassLoadingService> svcRef = bCtx.getServiceReference(ClassLoadingService.class);
            return svcRef == null ? null : bCtx.getService(svcRef);
        });
        if (classLoadingService == null) {
            throw new IllegalStateException("Failed to get the ClassLoadingService.");
        }
        return classLoadingService;
    }

    private String getAppName() {
        // Get the CDIService
        Bundle bundle = FrameworkUtil.getBundle(CDIService.class);
        CDIService cdiService = AccessController.doPrivileged((PrivilegedAction<CDIService>) () -> {
            BundleContext bCtx = bundle.getBundleContext();
            ServiceReference<CDIService> svcRef = bCtx.getServiceReference(CDIService.class);
            return svcRef == null ? null : bCtx.getService(svcRef);
        });
        if (cdiService == null) {
            return null;
        }
        return cdiService.getCurrentApplicationContextID();
    }

    private void releaseLoader(ClassLoader tccl) {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            getClassLoadingService().destroyThreadContextClassLoader(tccl);
            return null;
        });
    }

    private ClassLoader configureBvalClassloader(ClassLoader cl) {
        if (cl == null) {
            cl = AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> Thread.currentThread().getContextClassLoader());
        }
        if (cl != null) {
            if (getClassLoadingService().isThreadContextClassLoader(cl)) {
                return cl;
            } else if (getClassLoadingService().isAppClassLoader(cl)) {
                return createTCCL(cl);
            }
        }
        return createTCCL(LibertyHibernateValidatorExtension.class.getClassLoader());
    }

    private ClassLoader createTCCL(ClassLoader parentCL) {
        return AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> getClassLoadingService().createThreadContextClassLoader(parentCL));
    }
}
