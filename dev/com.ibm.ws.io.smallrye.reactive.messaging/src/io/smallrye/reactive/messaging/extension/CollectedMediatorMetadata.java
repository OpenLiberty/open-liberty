package io.smallrye.reactive.messaging.extension;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.spi.Bean;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.reactive.messaging.MediatorConfiguration;

class CollectedMediatorMetadata {

  private final List<MediatorConfiguration> mediators = new ArrayList<>();

  void add(Method method, Bean<?> bean) {
    mediators.add(createMediatorConfiguration(method, bean));
  }

  private MediatorConfiguration createMediatorConfiguration(Method met, Bean<?> bean) {
    MediatorConfiguration configuration = new MediatorConfiguration(met, bean);
    configuration.compute(met.getAnnotation(Incoming.class), met.getAnnotation(Outgoing.class));
    return configuration;
  }

  List<MediatorConfiguration> mediators() {
    return mediators;
  }
}
