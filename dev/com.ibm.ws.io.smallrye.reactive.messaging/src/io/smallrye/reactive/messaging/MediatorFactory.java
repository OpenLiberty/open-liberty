package io.smallrye.reactive.messaging;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class MediatorFactory {

  public AbstractMediator create(MediatorConfiguration configuration) {
    switch (configuration.shape()) {
      case PROCESSOR: return new ProcessorMediator(configuration);
      case SUBSCRIBER: return new SubscriberMediator(configuration);
      case PUBLISHER: return new PublisherMediator(configuration);
      case STREAM_TRANSFORMER: return new StreamTransformerMediator(configuration);
      default: throw new IllegalArgumentException("Unsupported shape " + configuration.shape()
        + " for method " + configuration.methodAsString());
    }
  }

}
