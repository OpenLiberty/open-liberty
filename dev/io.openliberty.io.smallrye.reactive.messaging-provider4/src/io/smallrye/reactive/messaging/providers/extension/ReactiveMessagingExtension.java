/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package io.smallrye.reactive.messaging.providers.extension;

import static io.smallrye.reactive.messaging.providers.i18n.ProviderLogging.log;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;

import jakarta.annotation.Priority;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.reactivestreams.Publisher;

import io.smallrye.reactive.messaging.EmitterConfiguration;
import io.smallrye.reactive.messaging.EmitterFactory;
import io.smallrye.reactive.messaging.EmitterType;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.smallrye.reactive.messaging.annotations.Broadcast;
import io.smallrye.reactive.messaging.annotations.EmitterFactoryFor;
import io.smallrye.reactive.messaging.annotations.Incomings;
import io.smallrye.reactive.messaging.providers.DefaultEmitterConfiguration;
import io.smallrye.reactive.messaging.providers.connectors.WorkerPoolRegistry;
import io.smallrye.reactive.messaging.providers.i18n.ProviderExceptions;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.ProcessInjectionPoint;
import jakarta.enterprise.inject.spi.ProcessManagedBean;
import jakarta.enterprise.inject.spi.WithAnnotations;
import jakarta.inject.Inject;

/**
 *
 */
public class ReactiveMessagingExtension implements Extension {

    private final List<MediatorBean<?>> mediatorBeans = new ArrayList<>();
    private final List<InjectionPoint> streamInjectionPoints = new ArrayList<>();
    private final Map<InjectionPoint, EmitterFactoryFor> emitterInjectionPoints = new HashMap<InjectionPoint, EmitterFactoryFor>();
    private final List<EmitterFactoryBean<?>> emitterFactoryBeans = new ArrayList<>();
    private final List<WorkerPoolBean<?>> workerPoolBeans = new ArrayList<>();

    //IBM Modification: store this so we can access it elsewhere
    protected MediatorManager mediatorManager = null;

    @Inject
    HealthCenter health;

    <T> void processClassesContainingMediators(@Observes ProcessManagedBean<T> event) {
        AnnotatedType<?> annotatedType = event.getAnnotatedBeanClass();
        if (annotatedType.getMethods().stream().anyMatch(m -> m.isAnnotationPresent(Incomings.class) || m.isAnnotationPresent(Incoming.class)
                || m.isAnnotationPresent(Outgoing.class))) {
            mediatorBeans.add(new MediatorBean<>(event.getBean(), event.getAnnotatedBeanClass()));
        }
    }

    <T extends EmitterFactory<?>> void processEmitterFactories(
            @Observes @WithAnnotations({ EmitterFactoryFor.class }) ProcessAnnotatedType<T> event) {
        AnnotatedType<?> annotatedType = event.getAnnotatedType();
        emitterFactoryBeans.add(new EmitterFactoryBean<>(annotatedType));
    }

    <T> void processBlockingAnnotation(@Observes @WithAnnotations({ Blocking.class }) ProcessAnnotatedType<T> event) {
        AnnotatedType<?> annotatedType = event.getAnnotatedType();
        workerPoolBeans.add(new WorkerPoolBean<>(annotatedType));
    }

    <T extends Flow.Publisher<?>> void processStreamPublisherInjectionPoint(@Observes ProcessInjectionPoint<?, T> pip) {
        Channel stream = ChannelProducer.getChannelQualifier(pip.getInjectionPoint());
        if (stream != null) {
            streamInjectionPoints.add(pip.getInjectionPoint());
        }
    }

    <T extends Publisher<?>> void processStreamReactiveStreamPublisherInjectionPoint(
            @Observes ProcessInjectionPoint<?, T> pip) {
        Channel stream = ChannelProducer.getChannelQualifier(pip.getInjectionPoint());
        if (stream != null) {
            streamInjectionPoints.add(pip.getInjectionPoint());
        }
    }

    void processStreamSpecEmitterInjectionPoint(@Observes ProcessInjectionPoint<?, Emitter<?>> pip) {
        Channel stream = ChannelProducer.getChannelQualifier(pip.getInjectionPoint());
        if (stream != null) {
            EmitterFactoryFor emitterType = emitterType(pip.getInjectionPoint(), emitterFactoryBeans);
            if (emitterType != null) {
                emitterInjectionPoints.put(pip.getInjectionPoint(), emitterType);
            }
        }
    }

    <T extends EmitterType> void processStreamEmitterInjectionPoint(@Observes ProcessInjectionPoint<?, T> pip) {
        Channel stream = ChannelProducer.getChannelQualifier(pip.getInjectionPoint());
        if (stream != null) {
            EmitterFactoryFor emitterType = emitterType(pip.getInjectionPoint(), emitterFactoryBeans);
            if (emitterType != null) {
                emitterInjectionPoints.put(pip.getInjectionPoint(), emitterType);
            }
        }
    }

    <T extends PublisherBuilder<?>> void processStreamPublisherBuilderInjectionPoint(
            @Observes ProcessInjectionPoint<?, T> pip) {
        Channel stream = ChannelProducer.getChannelQualifier(pip.getInjectionPoint());
        if (stream != null) {
            streamInjectionPoints.add(pip.getInjectionPoint());
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    void afterDeploymentValidation(@Observes AfterDeploymentValidation done, BeanManager beanManager) {
        Instance<Object> instance = beanManager.createInstance();
        mediatorManager = instance.select(MediatorManager.class).get();
        WorkerPoolRegistry workerPoolRegistry = instance.select(WorkerPoolRegistry.class).get();

        List<EmitterConfiguration> emitters = createEmitterConfigurations();
        for (EmitterConfiguration emitter : emitters) {
            mediatorManager.addEmitter(emitter);
        }

        List<ChannelConfiguration> channels = createChannelConfigurations();
        for (ChannelConfiguration channel : channels) {
            mediatorManager.addChannel(channel);
        }

        for (MediatorBean mediatorBean : mediatorBeans) {
            log.analyzingMediatorBean(mediatorBean.bean);
            mediatorManager.analyze(mediatorBean.annotatedType, mediatorBean.bean);
        }

        //IBM Modifications begin here.

        //See comments in WorkerPoolRegistry. We don't expect for loop to ever have
        //a workerPoolBean actually commenting out workerPoolRegistry.analyzeWorker
        //is needed for compiling.

        //for (WorkerPoolBean workerPoolBean : workerPoolBeans) {
        //    workerPoolRegistry.analyzeWorker(workerPoolBean.annotatedType);
        //}

        //This is moved to later in the lifecycle to allow for instantOn
        //mediatorManager.start();

        //IBM Modifications end here.
    }

    private List<ChannelConfiguration> createChannelConfigurations() {
        List<ChannelConfiguration> channels = new ArrayList<>();
        for (InjectionPoint ip : streamInjectionPoints) {
            String name = ChannelProducer.getChannelName(ip);
            channels.add(new ChannelConfiguration(name));
        }
        return channels;
    }

    private List<EmitterConfiguration> createEmitterConfigurations() {
        List<EmitterConfiguration> emitters = new ArrayList<>();
        createEmitterConfiguration(emitterInjectionPoints, emitters);
        return emitters;
    }

    private void createEmitterConfiguration(Map<InjectionPoint, EmitterFactoryFor> emitterInjectionPoints,
                                            List<EmitterConfiguration> emitters) {
        for (Map.Entry<InjectionPoint, EmitterFactoryFor> entry : emitterInjectionPoints.entrySet()) {
            InjectionPoint point = entry.getKey();
            EmitterFactoryFor emitterType = entry.getValue();
            String name = ChannelProducer.getChannelName(point);
            OnOverflow onOverflow = point.getAnnotated().getAnnotation(OnOverflow.class);
            if (onOverflow == null) {
                onOverflow = createOnOverflowForLegacyAnnotation(point);
            }
            Broadcast broadcast = point.getAnnotated().getAnnotation(Broadcast.class);
            emitters.add(new DefaultEmitterConfiguration(name, emitterType, onOverflow, broadcast));
        }
    }

    @SuppressWarnings("deprecation")
    private OnOverflow createOnOverflowForLegacyAnnotation(InjectionPoint point) {
        io.smallrye.reactive.messaging.annotations.OnOverflow legacy = point.getAnnotated().getAnnotation(io.smallrye.reactive.messaging.annotations.OnOverflow.class);
        if (legacy != null) {
            return new OnOverflow() {

                @Override
                public Class<? extends Annotation> annotationType() {
                    return OnOverflow.class;
                }

                @Override
                public Strategy value() {
                    return Strategy.valueOf(legacy.value().name());
                }

                @Override
                public long bufferSize() {
                    return legacy.bufferSize();
                }
            };
        }
        return null;
    }

    private EmitterFactoryFor emitterType(InjectionPoint point, List<EmitterFactoryBean<?>> emitterFactoryBeans) {
        for (EmitterFactoryBean<?> emitterFactoryBean : emitterFactoryBeans) {
            EmitterFactoryFor annotation = emitterFactoryBean.emitterFactoryType.getAnnotation(EmitterFactoryFor.class);
            Type type = point.getType();
            if (type instanceof ParameterizedType && ((ParameterizedType) type).getActualTypeArguments().length > 0) {
                if (((ParameterizedType) type).getRawType().equals(annotation.value())) {
                    return annotation;
                }
            } else {
                throw ProviderExceptions.ex.invalidRawEmitter(point);
            }
        }
        return null;
    }

    static class MediatorBean<T> {

        final Bean<T> bean;

        final AnnotatedType<T> annotatedType;

        MediatorBean(Bean<T> bean, AnnotatedType<T> annotatedType) {
            this.bean = bean;
            this.annotatedType = annotatedType;
        }

    }

    static class WorkerPoolBean<T> {
        final AnnotatedType<T> annotatedType;

        WorkerPoolBean(AnnotatedType<T> annotatedType) {
            this.annotatedType = annotatedType;
        }
    }

    static class EmitterFactoryBean<T> {
        final AnnotatedType<T> emitterFactoryType;

        EmitterFactoryBean(AnnotatedType<T> emitterFactoryType) {
            this.emitterFactoryType = emitterFactoryType;
        }
    }
}
