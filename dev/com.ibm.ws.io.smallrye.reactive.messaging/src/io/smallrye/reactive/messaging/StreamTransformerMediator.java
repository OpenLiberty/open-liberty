package io.smallrye.reactive.messaging;

import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import org.apache.commons.lang3.ClassUtils;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.reactivestreams.Publisher;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class StreamTransformerMediator extends AbstractMediator {

  Function<PublisherBuilder<? extends Message>, PublisherBuilder<? extends Message>> function;

  private PublisherBuilder<? extends Message> publisher;

  public StreamTransformerMediator(MediatorConfiguration configuration) {
    super(configuration);
  }

  @Override
  public void connectToUpstream(PublisherBuilder<? extends Message> publisher) {
    Objects.requireNonNull(function);
    this.publisher = decorate(function.apply(publisher));
  }

  @Override
  public PublisherBuilder<? extends Message> getStream() {
    Objects.requireNonNull(publisher);
    return publisher;
  }

  @Override
  public boolean isConnected() {
    return publisher != null;
  }

  @Override
  public void initialize(Object bean) {
    super.initialize(bean);
    // 1. Publisher<Message<O>> method(Publisher<Message<I>> publisher)
    // 2. Publisher<O> method(Publisher<I> publisher)
    // 3. PublisherBuilder<Message<O>> method(PublisherBuilder<Message<I>> publisher)
    // 4. PublisherBuilder<O> method(PublisherBuilder<I> publisher)

    switch (configuration.consumption()) {
      case STREAM_OF_MESSAGE:
        if (configuration.usesBuilderTypes()) {
          // Case 3
          processMethodConsumingAPublisherBuilderOfMessages();
        } else {
          // Case 1
          processMethodConsumingAPublisherOfMessages();
        }
        break;
      case STREAM_OF_PAYLOAD:
        if (configuration.usesBuilderTypes()) {
          // Case 4
          processMethodConsumingAPublisherBuilderOfPayload();
        } else {
          // Case 2
          processMethodConsumingAPublisherOfPayload();
        }
        break;
      default:
        throw new IllegalArgumentException("Unexpected consumption type: " + configuration.consumption());
    }

    assert function != null;
  }

  private void processMethodConsumingAPublisherBuilderOfMessages() {
    function = publisher -> {
      PublisherBuilder<Message> prependedWithAck = publisher
        .flatMapCompletionStage(managePreProcessingAck());

      PublisherBuilder<Message> builder = invoke(prependedWithAck);
      Objects.requireNonNull(builder, "The method " + configuration.methodAsString() + " has returned an invalid value: `null`");
      return builder;
    };
  }

  private void processMethodConsumingAPublisherOfMessages() {
    function = publisher -> {
      Publisher<Message> prependedWithAck = publisher
        .flatMapCompletionStage(managePreProcessingAck())
        .buildRs();
      Class<?> parameterType = configuration.getMethod().getParameterTypes()[0];
      Optional<? extends ReactiveTypeConverter<?>> converter = Registry.lookup(parameterType);
      if (converter.isPresent()) {
        prependedWithAck = (Publisher) converter.get().fromPublisher(prependedWithAck);
      }
      Publisher<Message> result = invoke(prependedWithAck);
      Objects.requireNonNull(result, "The method " + configuration.methodAsString() + " has returned an invalid value: `null`");
      return ReactiveStreams.fromPublisher(result);
    };
  }

  private void processMethodConsumingAPublisherBuilderOfPayload() {
    function = builder -> {
        PublisherBuilder<Object> unwrapped = builder
        .flatMapCompletionStage(managePreProcessingAck())
        .map(Message::getPayload);
      PublisherBuilder<Object> result = invoke(unwrapped);
      Objects.requireNonNull(result, "The method " + configuration.methodAsString() + " has returned an invalid value: `null`");
      return result.map(o -> (Message) Message.of(o));
    };
  }

  private void processMethodConsumingAPublisherOfPayload() {
    function = builder -> {
      Publisher<Object> stream = builder
        .flatMapCompletionStage(managePreProcessingAck())
        .map(Message::getPayload).buildRs();
      // Ability to inject Publisher implementation in method getting a Publisher.
      Class<?> parameterType = configuration.getMethod().getParameterTypes()[0];
      Optional<? extends ReactiveTypeConverter<?>> converter = Registry.lookup(parameterType);
      if (converter.isPresent()) {
        stream = (Publisher) converter.get().fromPublisher(stream);
      }
      Publisher<Object> result = invoke(stream);
      Objects.requireNonNull(result, "The method " + configuration.methodAsString() + " has returned an invalid value: `null`");
      return ReactiveStreams.fromPublisher(result)
        .map(o -> (Message) Message.of(o));
    };
  }

}
