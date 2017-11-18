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
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.executable.ValidateOnExecution;

import org.hibernate.validator.cdi.ValidationExtension;
import org.hibernate.validator.cdi.internal.ValidatorBean;
import org.hibernate.validator.cdi.internal.ValidatorFactoryBean;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.beanvalidation.service.BeanValidation;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

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

    @Reference
    BeanValidation beanValidation;

    public static BeanValidation getBeanVal() {
        BundleContext bctx = FrameworkUtil.getBundle(LibertyHibernateValidatorExtension.class).getBundleContext();
        return bctx.getService(bctx.getServiceReference(BeanValidation.class));
    }

    public static Validator getDefaultValidator() {
        try {
            ComponentMetaData componentMetaData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
            return getBeanVal().getValidator(componentMetaData);
        } catch (ValidationException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ValidatorFactory getDefaultValidatorFactory() {
        try {
            ComponentMetaData componentMetaData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
            return getBeanVal().getValidatorFactory(componentMetaData);
        } catch (ValidationException e) {
            e.printStackTrace();
            return null;
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

    private ValidationExtension delegate() {
        if (extDelegate == null) {
            extDelegate = new ValidationExtension();
        }
        return extDelegate;
    }

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscoveryEvent,
                                    final BeanManager beanManager) {
        delegate().beforeBeanDiscovery(beforeBeanDiscoveryEvent, beanManager);
    }

    public void processBean(@Observes ProcessBean<?> processBeanEvent) {
        delegate().processBean(processBeanEvent);
    }

    public <T> void processAnnotatedType(@Observes @WithAnnotations({
                                                                      Constraint.class,
                                                                      Valid.class,
                                                                      ValidateOnExecution.class
    }) ProcessAnnotatedType<T> processAnnotatedTypeEvent) {
        delegate().processAnnotatedType(processAnnotatedTypeEvent);
    }
}
