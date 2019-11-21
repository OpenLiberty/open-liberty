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

import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.cdi.CDIServiceUtils;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;

import io.smallrye.reactive.messaging.MediatorFactory;
import io.smallrye.reactive.messaging.annotations.Channel;
import io.smallrye.reactive.messaging.annotations.Stream;
import io.smallrye.reactive.messaging.extension.ChannelProducer;
import io.smallrye.reactive.messaging.extension.MediatorManager;
import io.smallrye.reactive.messaging.extension.ReactiveMessagingExtension;
import io.smallrye.reactive.messaging.impl.ConfiguredChannelFactory;
import io.smallrye.reactive.messaging.impl.InternalChannelRegistry;
import io.smallrye.reactive.messaging.impl.LegacyConfiguredChannelFactory;

@Component(service = WebSphereCDIExtension.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM", "application.bdas.visible=true" })
public class OLReactiveMessagingExtension extends ReactiveMessagingExtension implements Extension, WebSphereCDIExtension {

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery discovery, BeanManager beanManager) {
        addAnnotatedType(MediatorFactory.class, discovery, beanManager);
        addAnnotatedType(MediatorManager.class, discovery, beanManager);
        addAnnotatedType(ChannelProducer.class, discovery, beanManager);
        addAnnotatedType(MediatorFactory.class, discovery, beanManager);
        addAnnotatedType(InternalChannelRegistry.class, discovery, beanManager);
        addAnnotatedType(ConfiguredChannelFactory.class, discovery, beanManager);
        addAnnotatedType(LegacyConfiguredChannelFactory.class, discovery, beanManager);

        addQualifier(Stream.class, discovery, beanManager);
        addQualifier(Channel.class, discovery, beanManager);
        addQualifier(Connector.class, discovery, beanManager);
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
