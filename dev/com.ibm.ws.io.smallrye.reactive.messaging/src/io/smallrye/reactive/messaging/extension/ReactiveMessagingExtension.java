package io.smallrye.reactive.messaging.extension;

import io.smallrye.reactive.messaging.ChannelRegistry;
import io.smallrye.reactive.messaging.annotations.Emitter;
import io.smallrye.reactive.messaging.annotations.Stream;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class ReactiveMessagingExtension implements Extension {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReactiveMessagingExtension.class);

  private List<MediatorBean<?>> mediatorBeans = new ArrayList<>();
  private List<InjectionPoint> streamInjectionPoints = new ArrayList<>();
  private List<InjectionPoint> emitterInjectionPoints = new ArrayList<>();

  <T> void processClassesContainingMediators(@Observes ProcessManagedBean<T> event) {
    AnnotatedType<?> annotatedType = event.getAnnotatedBeanClass();
    if (annotatedType.getMethods()
      .stream()
      .anyMatch(m -> m.isAnnotationPresent(Incoming.class) || m.isAnnotationPresent(Outgoing.class))) {
      mediatorBeans.add(new MediatorBean<>(event.getBean(), event.getAnnotatedBeanClass()));
    }
  }

  <T extends Publisher<?>> void processStreamPublisherInjectionPoint(@Observes ProcessInjectionPoint<?, T> pip) {
    Stream stream = StreamProducer.getStreamQualifier(pip.getInjectionPoint());
    if (stream != null) {
      streamInjectionPoints.add(pip.getInjectionPoint());
    }
  }

  <T extends Emitter<?>> void processStreamEmitterInjectionPoint(@Observes ProcessInjectionPoint<?, T> pip) {
    Stream stream = StreamProducer.getStreamQualifier(pip.getInjectionPoint());
    if (stream != null) {
      emitterInjectionPoints.add(pip.getInjectionPoint());
    }
  }

  <T extends PublisherBuilder<?>> void processStreamPublisherBuilderInjectionPoint(@Observes ProcessInjectionPoint<?, T> pip) {
    Stream stream = StreamProducer.getStreamQualifier(pip.getInjectionPoint());
    if (stream != null) {
      streamInjectionPoints.add(pip.getInjectionPoint());
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  void afterDeploymentValidation(@Observes AfterDeploymentValidation done, BeanManager beanManager) {
    Instance<Object> instance = beanManager.createInstance();
    ChannelRegistry registry = instance.select(ChannelRegistry.class)
      .get();

    List<String> emitters = emitterInjectionPoints.stream().map(StreamProducer::getStreamName).collect(Collectors.toList());
    MediatorManager mediatorManager = instance.select(MediatorManager.class)
      .get();
    mediatorManager.initializeEmitters(emitters);

    for (MediatorBean mediatorBean : mediatorBeans) {
      LOGGER.info("Analyzing mediator bean: {}", mediatorBean.bean);
      mediatorManager.analyze(mediatorBean.annotatedType, mediatorBean.bean);
    }
    mediatorBeans.clear();

    try {
      mediatorManager.initializeAndRun();

      // NOTE: We do not validate @Stream annotations added by portable extensions
      Set<String> names = registry.getIncomingNames();
      for (InjectionPoint ip : streamInjectionPoints) {
        String name = StreamProducer.getStreamName(ip);
        if (!names.contains(name)) {
          done.addDeploymentProblem(new DeploymentException("No channel found for name: " + name + ", injection point: " + ip));
        }
        // TODO validate the required type
      }
      streamInjectionPoints.clear();

      for (InjectionPoint ip : emitterInjectionPoints) {
        String name = StreamProducer.getStreamName(ip);
        EmitterImpl<?> emitter = (EmitterImpl<?>) registry.getEmitter(name);
        if (!emitter.isConnected()) {
          done.addDeploymentProblem(new DeploymentException("No channel found for name: " + name + ", injection point: " + ip));
        }
        // TODO validate the required type
      }

    } catch (Exception e) {
      if (e.getCause() == null) {
        done.addDeploymentProblem(e);
      } else {
        done.addDeploymentProblem(e.getCause());
      }

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
