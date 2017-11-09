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

import java.util.ServiceLoader;

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
import javax.validation.spi.ValidationProvider;

import org.hibernate.validator.cdi.ValidationExtension;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.cdi.extension.WebSphereCDIExtension;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "api.classes=" +
                        "javax.validation.Validator;" +
                        "javax.validation.ValidatorFactory;" +
                        "org.hibernate.validator.cdi.HibernateValidator;" +
                        "org.hibernate.validator.cdi.ValidationExtension;" +
                        "org.hibernate.validator.internal.engine.ValidatorImpl;" +
                        "org.hibernate.validator.cdi.internal.interceptor.ValidationInterceptor",
                        "service.vendor=IBM"
           })
public class LibertyHibernateValidatorExtension implements Extension, WebSphereCDIExtension {

    private static final boolean DEBUG = true;

    private ValidationExtension delegate;

    @Activate
    protected void activate() {
        if (DEBUG)
            System.out.println("@AGG activating " + getClass().getSimpleName());
        try {
            System.out.println("@AGG getting providers w/ default CL");
            for (ValidationProvider p : ServiceLoader.load(ValidationProvider.class)) {
                System.out.println("@AGG found provider: " + p);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            System.out.println("@AGG getting providers w/ TCCL");
            for (ValidationProvider p : ServiceLoader.load(ValidationProvider.class, Thread.currentThread().getContextClassLoader())) {
                System.out.println("@AGG found provider: " + p);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initHibernateValidator() {
        if (delegate == null) {
            if (DEBUG)
                System.out.println("@AGG initializing hibernate ValidationExtension");
            delegate = new ValidationExtension();
        }
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscoveryEvent, BeanManager beanManager) {
        initHibernateValidator();
        delegate.afterBeanDiscovery(afterBeanDiscoveryEvent, beanManager);
    }

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscoveryEvent, BeanManager beanManager) {
        initHibernateValidator();
        if (DEBUG)
            System.out.println("@AGG beforeBeanDiscovery:  " + beforeBeanDiscoveryEvent);
        delegate.beforeBeanDiscovery(beforeBeanDiscoveryEvent, beanManager);
    }

    public <T> void processAnnotatedType(@Observes @WithAnnotations({ Constraint.class, Valid.class,
                                                                      ValidateOnExecution.class }) ProcessAnnotatedType<T> processAnnotatedTypeEvent) {
        initHibernateValidator();
        if (DEBUG)
            System.out.println("@AGG processAnnotatedType: " + processAnnotatedTypeEvent.getAnnotatedType());
        delegate.processAnnotatedType(processAnnotatedTypeEvent);
    }

    public void processBean(@Observes ProcessBean<?> processBeanEvent) {
        initHibernateValidator();
        if (DEBUG)
            System.out.println("@AGG processBean:          " + processBeanEvent.getBean());
        delegate.processBean(processBeanEvent);
    }
}
