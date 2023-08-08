/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.messaging30.internal;

import java.lang.annotation.Annotation;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.cdi.CDIServiceUtils;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;

import io.smallrye.reactive.messaging.providers.MediatorFactory;
import io.smallrye.reactive.messaging.providers.OutgoingInterceptorDecorator;
import io.smallrye.reactive.messaging.providers.connectors.WorkerPoolRegistry;
import io.smallrye.reactive.messaging.providers.extension.ChannelProducer;
import io.smallrye.reactive.messaging.providers.extension.EmitterFactoryImpl;
import io.smallrye.reactive.messaging.providers.extension.HealthCenter;
import io.smallrye.reactive.messaging.providers.extension.LegacyEmitterFactoryImpl;
import io.smallrye.reactive.messaging.providers.extension.MediatorManager;
import io.smallrye.reactive.messaging.providers.extension.MutinyEmitterFactoryImpl;
import io.smallrye.reactive.messaging.providers.extension.ReactiveMessagingExtension;
import io.smallrye.reactive.messaging.providers.impl.ConfiguredChannelFactory;
import io.smallrye.reactive.messaging.providers.impl.ConnectorFactories;
import io.smallrye.reactive.messaging.providers.impl.InternalChannelRegistry;
import io.smallrye.reactive.messaging.providers.wiring.Wiring;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;

@Component(service = WebSphereCDIExtension.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "application.bdas.visible=true" })
public class OLReactiveMessaging30Extension extends ReactiveMessagingExtension implements Extension, WebSphereCDIExtension {

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery discovery, BeanManager beanManager) {
        //This bundle has bean-discovery-mode=none so we have to add anything with a CDI annotation here

        //io.smallrye.reactive.messaging.providers
        addAnnotatedType(MediatorFactory.class, discovery, beanManager);
        addAnnotatedType(OutgoingInterceptorDecorator.class, discovery, beanManager);

        //io.smallrye.reactive.messaging.providers.connectors - These depend on vertx so we're excluding them
//        addAnnotatedType(ExecutionHolder.class, discovery, beanManager);
        //WorkerPoolRegistry is injected by some key SmallRye internals so we have a dummy version of the class
        addAnnotatedType(WorkerPoolRegistry.class, discovery, beanManager);

        //io.smallrye.reactive.messaging.providers.extension
        addAnnotatedType(ChannelProducer.class, discovery, beanManager);
        addAnnotatedType(EmitterFactoryImpl.class, discovery, beanManager);
        addAnnotatedType(HealthCenter.class, discovery, beanManager);
        addAnnotatedType(LegacyEmitterFactoryImpl.class, discovery, beanManager);
        addAnnotatedType(MediatorManager.class, discovery, beanManager);
        addAnnotatedType(MutinyEmitterFactoryImpl.class, discovery, beanManager);

        //io.smallrye.reactive.messaging.providers.impl
        addAnnotatedType(ConfiguredChannelFactory.class, discovery, beanManager);
        addAnnotatedType(ConnectorFactories.class, discovery, beanManager);
        addAnnotatedType(InternalChannelRegistry.class, discovery, beanManager);

        //io.smallrye.reactive.messaging.providers.locals - Decorator to dispatch messages on the Vert.x context attached to the message
        //addAnnotatedType(ContextDecorator.class, discovery, beanManager);

        //io.smallrye.reactive.messaging.providers.metrics - These depend on Micrometer so we're excluding them
//        addAnnotatedType(MetricDecorator.class, discovery, beanManager);
//        addAnnotatedType(MicrometerDecorator.class, discovery, beanManager);

        //io.smallrye.reactive.messaging.providers.wiring
        addAnnotatedType(Wiring.class, discovery, beanManager);

        //org.eclipse.microprofile.reactive.messaging
        addQualifier(Channel.class, discovery, beanManager);

        //org.eclipse.microprofile.reactive.messaging.spi
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
