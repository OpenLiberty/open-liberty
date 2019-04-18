/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging;

import java.lang.annotation.Annotation;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.cdi.CDIServiceUtils;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;

import io.smallrye.reactive.messaging.MediatorFactory;
import io.smallrye.reactive.messaging.annotations.Stream;
import io.smallrye.reactive.messaging.extension.MediatorManager;
import io.smallrye.reactive.messaging.extension.ReactiveMessagingExtension;
import io.smallrye.reactive.messaging.extension.StreamProducer;
import io.smallrye.reactive.messaging.impl.ConfiguredStreamFactory;
import io.smallrye.reactive.messaging.impl.InternalStreamRegistry;
import io.smallrye.reactive.messaging.impl.StreamFactoryImpl;

@Component(service = WebSphereCDIExtension.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class OLReactiveMessagingExtension extends ReactiveMessagingExtension implements Extension, WebSphereCDIExtension {

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery discovery, BeanManager beanManager) {
        addAnnotatedType(MediatorFactory.class, discovery, beanManager);
        addAnnotatedType(MediatorManager.class, discovery, beanManager);
        addAnnotatedType(StreamProducer.class, discovery, beanManager);
        addAnnotatedType(ConfiguredStreamFactory.class, discovery, beanManager);
        addAnnotatedType(InternalStreamRegistry.class, discovery, beanManager);
        addAnnotatedType(StreamFactoryImpl.class, discovery, beanManager);
        addAnnotatedType(MediatorFactory.class, discovery, beanManager);
        addQualifier(Stream.class, discovery, beanManager);
    }

    private <T> void addAnnotatedType(Class<T> clazz, BeforeBeanDiscovery discovery, BeanManager beanManager) {
        AnnotatedType<T> annotatedType = beanManager.createAnnotatedType(clazz);
        String id = CDIServiceUtils.getAnnotatedTypeIdentifier(annotatedType, this.getClass());
        discovery.addAnnotatedType(annotatedType, id);
    }

    private <T extends Annotation> void addQualifier(Class<T> clazz, BeforeBeanDiscovery discovery, BeanManager beanManager) {
        AnnotatedType<T> annotatedType = beanManager.createAnnotatedType(clazz);
        discovery.addQualifier(annotatedType);
    }

}
