/*
 * This is a class taken from SmallRye Reactive Messaging and slightly modified
 * https://github.com/smallrye/smallrye-reactive-messaging/blob/master/smallrye-reactive-messaging-provider/src/main/java/io/smallrye/reactive/messaging/extension/ReactiveMessagingExtension.java
 *
 * Licensed under the Apache License, Version 2.0
 * https://github.com/smallrye/smallrye-reactive-messaging/blob/master/LICENSE
 */
package io.smallrye.reactive.messaging.extension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.DeploymentException;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.ProcessManagedBean;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.reactive.messaging.StreamRegistry;
import io.smallrye.reactive.messaging.annotations.Stream;

public class ReactiveMessagingExtension implements Extension {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactiveMessagingExtension.class);

    private final List<MediatorBean<?>> mediatorBeans = new ArrayList<>();
    private final List<InjectionPoint> streamInjectionPoints = new ArrayList<>();

    <T> void processClassesContainingMediators(@Observes ProcessManagedBean<T> event) {
        AnnotatedType<?> annotatedType = event.getAnnotatedBeanClass();
        if (annotatedType.getMethods().stream().anyMatch(m -> m.isAnnotationPresent(Incoming.class) || m.isAnnotationPresent(Outgoing.class))) {
            mediatorBeans.add(new MediatorBean<>(event.getBean(), event.getAnnotatedBeanClass()));
        }
    }

    <T extends Publisher<?>> void processStreamPublisherInjectionPoint(@Observes ProcessInjectionPoint<?, T> pip) {
        Stream stream = StreamProducer.getStreamQualifier(pip.getInjectionPoint());
        if (stream != null) {
            streamInjectionPoints.add(pip.getInjectionPoint());
        }
    }

    <T extends PublisherBuilder<?>> void processStreamPublisherBuilderInjectionPoint(@Observes ProcessInjectionPoint<?, T> pip) {
        Stream stream = StreamProducer.getStreamQualifier(pip.getInjectionPoint());
        if (stream != null) {
            streamInjectionPoints.add(pip.getInjectionPoint());
        }
    }

    /**
     * In this callback, regular beans have been found, we can declare new beans.
     *
     * @param discovery   the discovery event
     * @param beanManager the bean manager
     */
    void afterBeanDiscovery(@Observes AfterBeanDiscovery discovery, BeanManager beanManager) {
        //registerVertxBeanIfNeeded(discovery, beanManager);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    void afterDeploymentValidation(@Observes AfterDeploymentValidation done, BeanManager beanManager) {
        Instance<Object> instance = beanManager.createInstance();
        StreamRegistry registry = instance.select(StreamRegistry.class).get();

        MediatorManager mediatorManager = instance.select(MediatorManager.class).get();
        for (MediatorBean mediatorBean : mediatorBeans) {
            LOGGER.info("Analyzing mediator bean:" + mediatorBean.bean);
            mediatorManager.analyze(mediatorBean.annotatedType, mediatorBean.bean);
        }
        mediatorBeans.clear();
        CompletableFuture<Void> future = mediatorManager.initializeAndRun();
        try {
            future.get();

            // NOTE: We do not validate @Stream annotations added by portable extensions
            Set<String> names = registry.getPublisherNames();
            for (InjectionPoint ip : streamInjectionPoints) {
                String name = StreamProducer.getStreamName(ip);
                if (!names.contains(name)) {
                    done.addDeploymentProblem(new DeploymentException("No stream found for name: " + name + ", injection point: " + ip));
                }
                // TODO validate the required type
            }
            streamInjectionPoints.clear();

        } catch (ExecutionException e) {
            done.addDeploymentProblem(e.getCause());
        } catch (InterruptedException e) {
            done.addDeploymentProblem(e);
        }
    }

    static class MediatorBean<T> {

        final Bean<T> bean;

        final AnnotatedType<T> annotatedType;

        MediatorBean(Bean<T> bean, AnnotatedType<T> annotatedType) {
            this.bean = bean;
            this.annotatedType = annotatedType;
        }

    }

}
