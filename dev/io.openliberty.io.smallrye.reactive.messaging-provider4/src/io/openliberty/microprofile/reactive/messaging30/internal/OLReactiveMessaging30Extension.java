/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.messaging30.internal;

import java.lang.annotation.Annotation;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.DeploymentException;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.cdi.CDIServiceUtils;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.ContextBeginnerEnder;
import com.ibm.ws.kernel.service.util.ServiceCaller;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.openliberty.checkpoint.spi.CheckpointHook;
import io.openliberty.checkpoint.spi.CheckpointPhase;
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
import io.smallrye.reactive.messaging.providers.metrics.MetricDecorator;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.enterprise.inject.spi.Extension;

@Component(service = WebSphereCDIExtension.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM", "application.bdas.visible=true" })
public class OLReactiveMessaging30Extension extends ReactiveMessagingExtension implements Extension, WebSphereCDIExtension {

    private static final TraceComponent tc = Tr.register(OLReactiveMessaging30Extension.class);

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

        // io.smallrye.reactive.messaging.providers.metrics
        // Metrics Decorator to handle Reactive Messaging and MP Metrics integration
        addAnnotatedType(MetricDecorator.class, discovery, beanManager);
        // Micrometer Decorator to handle Smallrye Reactive Messaging and Micrometer integration - This uses micrometer so excluding
        // addAnnotatedType(MicrometerDecorator.class, discovery, beanManager);

        //io.smallrye.reactive.messaging.providers.wiring
        addAnnotatedType(Wiring.class, discovery, beanManager);

        //org.eclipse.microprofile.reactive.messaging
        addQualifier(Channel.class, discovery, beanManager);

        //org.eclipse.microprofile.reactive.messaging.spi
        addQualifier(Connector.class, discovery, beanManager);
    }

    @Override
    protected void afterDeploymentValidation(@Observes AfterDeploymentValidation done, BeanManager beanManager) {
        final ContextBeginnerEnder contextBeginnerEnder = createContextBeginnerEnderIfCheckpointEnabled();
        final MediatorManager mediatorManager = configureMediatorManager(beanManager);

        CheckpointHook checkpointHook = new CheckpointHook() {

            @Override
            public void restore() {
                try (ContextBeginnerEnder cbe = contextBeginnerEnder.beginContext()) {
                    startMediatorManager(mediatorManager);
                }
            }
        };

        // If checkpoint is enabled this if statement will register the hook we created above, so startMediatorManager() will be called after restore. Then it returns true.
        // If checkpoint is disabled this if statement will return false, so startMediatorManager() will be called right away.
        if (!CheckpointPhase.getPhase().addMultiThreadedHook(checkpointHook)) {
            startMediatorManager(mediatorManager);
        }
    }

    @Override
    protected void startMediatorManager(MediatorManager mediatorManager) {
        // Catch "jakarta.enterprise.inject.spi.DeploymentException: Wiring error(s) detected in application"
        // that is thrown from MediatorManager.start() there are any validation errors with the Reactive Messaging Configuration
        try {
            super.startMediatorManager(mediatorManager);
        } catch (DeploymentException de) {
            StringBuilder exceptionBuilder = new StringBuilder(de.getLocalizedMessage());
            for (Throwable t : de.getSuppressed()) {
                exceptionBuilder.append("\n" + t.getLocalizedMessage());
            }
            String appName = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData().getJ2EEName().getApplication();
            Tr.error(tc, Tr.formatMessage(tc, "reactive.messaging.validation.error.CWMRX1100E", appName, exceptionBuilder.toString()));
            throw de; // rethrow DeploymentException to make sure we stop the application correctly. Do it here so the compiler doesn't ask for a return statement 
        }
    }

    private ContextBeginnerEnder createContextBeginnerEnderIfCheckpointEnabled() {
        if (CheckpointPhase.getPhase() != CheckpointPhase.INACTIVE) {
            return ServiceCaller.callOnce(OLReactiveMessaging30Extension.class, CDIService.class, 
                (cdiService) -> { return ((CDIRuntime) cdiService).cloneActiveContextBeginnerEnder(); }).get(); //Call get because if we can't get context from CDIservice inside a CDI extension throwing an exception is better than covering this up.
        }
        return null;
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
