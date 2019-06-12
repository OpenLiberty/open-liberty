package io.smallrye.reactive.messaging;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.reactivestreams.Publisher;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class PublisherMediator extends AbstractMediator {

  private PublisherBuilder<Message> publisher;

  // Supported signatures:
  // 1. Publisher<Message<O>> method()
  // 2. Publisher<O> method()
  // 3. PublisherBuilder<Message<O>> method()
  // 4. PublisherBuilder<O> method()
  // 5. O method() O cannot be Void
  // 6. Message<O> method()
  // 7. CompletionStage<O> method()
  // 8. CompletionStage<Message<O>> method()

  public PublisherMediator(MediatorConfiguration configuration) {
    super(configuration);
    if (configuration.shape() != Shape.PUBLISHER) {
      throw new IllegalArgumentException("Expected a Publisher shape, received a " + configuration.shape());
    }
  }

  @Override
  public PublisherBuilder<Message> getStream() {
    return Objects.requireNonNull(publisher);
  }

  @Override
  public boolean isConnected() {
    // Easy, not expecting anything
    return true;
  }

  @Override
  public void initialize(Object bean) {
    super.initialize(bean);
    switch (configuration.production()) {
      case STREAM_OF_MESSAGE: // 1, 3
        if (configuration.usesBuilderTypes()) {
          produceAPublisherBuilderOfMessages();
        } else {
          produceAPublisherOfMessages();
        }
        break;
      case STREAM_OF_PAYLOAD: // 2, 4
        if (configuration.usesBuilderTypes()) {
          produceAPublisherBuilderOfPayloads();
        } else {
          produceAPublisherOfPayloads();
        }
        break;
      case INDIVIDUAL_PAYLOAD: // 5
        produceIndividualPayloads();
        break;
      case INDIVIDUAL_MESSAGE:  // 6
        produceIndividualMessages();
        break;
      case COMPLETION_STAGE_OF_MESSAGE: // 8
        produceIndividualCompletionStageOfMessages();
        break;
      case COMPLETION_STAGE_OF_PAYLOAD: // 7
        produceIndividualCompletionStageOfPayloads();
        break;
      default: throw new IllegalArgumentException("Unexpected production type: " + configuration.production());
    }

    assert this.publisher != null;
  }

  private void produceAPublisherBuilderOfMessages() {
    PublisherBuilder<Message> builder = invoke();
    setPublisher(builder);
  }

  private void setPublisher(PublisherBuilder publisher) {
    this.publisher = decorate(publisher);
  }

  private <P> void produceAPublisherBuilderOfPayloads() {
    PublisherBuilder<P> builder = invoke();
    setPublisher(builder.map(Message::of));
  }

  private void produceAPublisherOfMessages() {
    setPublisher(ReactiveStreams.fromPublisher(invoke()));
  }

  private <P> void produceAPublisherOfPayloads() {
    Publisher<P> pub = invoke();
    setPublisher(ReactiveStreams.fromPublisher(pub).map(Message::of));
  }

  private void produceIndividualMessages() {
    setPublisher(ReactiveStreams.generate(() -> {
      Message message = invoke();
      Objects.requireNonNull(message, "The method " + configuration.methodAsString() + " returned an invalid value: null");
      return message;
    }));
  }

  private <T> void produceIndividualPayloads() {
    setPublisher(ReactiveStreams.<T>generate(() -> invoke())
      .map(Message::of));
  }

  private void produceIndividualCompletionStageOfMessages() {
    setPublisher(ReactiveStreams.<CompletionStage<Message>>generate(() -> invoke())
      .flatMapCompletionStage(Function.identity()));
  }

  private <P> void produceIndividualCompletionStageOfPayloads() {
    setPublisher(ReactiveStreams.<CompletionStage<P>>generate(() -> invoke())
      .flatMapCompletionStage(Function.identity())
      .map(Message::of));
  }
}
