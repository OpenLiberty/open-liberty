package io.smallrye.reactive.messaging;

import org.apache.commons.lang3.ClassUtils;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class SubscriberMediator extends AbstractMediator {

  private PublisherBuilder<Message> source;
  private SubscriberBuilder subscriber;
  /**
   * Keep track of the subscription to cancel it once the scope is terminated.
   */
  private AtomicReference<Subscription> subscription = new AtomicReference<>();

  // Supported signatures:
  // 1. Subscriber<Message<I>> method()
  // 2. Subscriber<I> method()
  // 3. CompletionStage<?> method(Message<I> m)
  // 4. CompletionStage<?> method(I i)
  // 5. void/? method(Message<I> m) - The support of this method has been removed (CES - Reactive Hangout 2018/09/11).
  // 6. void/? method(I i)

  public SubscriberMediator(MediatorConfiguration configuration) {
    super(configuration);
    if (configuration.shape() != Shape.SUBSCRIBER) {
      throw new IllegalArgumentException("Expected a Subscriber shape, received a " + configuration.shape());
    }
  }

  @Override
  public void initialize(Object bean) {
    super.initialize(bean);
    switch (configuration.consumption()) {
      case STREAM_OF_MESSAGE: // 1
      case STREAM_OF_PAYLOAD: // 2
        processMethodReturningASubscriber();
        break;
      case MESSAGE: // 3  (5 being dropped)
      case PAYLOAD: // 4 or 6
        if (ClassUtils.isAssignable(configuration.getMethod().getReturnType(), CompletionStage.class)) {
          // Case 3, 4
          processMethodReturningACompletionStage();
        } else {
          // Case 6 (5 being dropped)
          processMethodReturningVoid();
        }
        break;
      default:
        throw new IllegalArgumentException("Unexpected consumption type: " + configuration.consumption());
    }

    assert this.subscriber != null;
  }

  @Override
  public SubscriberBuilder<Message, Void> getComputedSubscriber() {
    return subscriber;
  }

  @Override
  public boolean isConnected() {
    return source != null;
  }

  @Override
  public void connectToUpstream(PublisherBuilder<? extends Message> publisher) {
    this.source = (PublisherBuilder) publisher;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void run() {
    assert this.source != null;
    assert this.subscriber != null;
    final Logger logger = LoggerFactory.getLogger(configuration.methodAsString());
    AtomicReference<Throwable> syncErrorCatcher = new AtomicReference<>();
    Subscriber delegate = this.subscriber.build();
    Subscriber delegating = new Subscriber() {
      @Override
      public void onSubscribe(Subscription s) {
        subscription.set(s);
        delegate.onSubscribe(s);
      }

      @Override
      public void onNext(Object o) {
        delegate.onNext(o);
      }

      @Override
      public void onError(Throwable t) {
        logger.error("Error caught during the stream processing", t);
        syncErrorCatcher.set(t);
        delegate.onError(t);
      }

      @Override
      public void onComplete() {
        delegate.onComplete();
      }
    };

    this.source.to(delegating).run();
    // Check if a synchronous error has been caught
    Throwable throwable = syncErrorCatcher.get();
    if (throwable != null) {
      throw new WeavingException(configuration.getIncoming(), throwable);
    }
  }

  private void processMethodReturningVoid() {
    if (configuration.consumption() == MediatorConfiguration.Consumption.PAYLOAD) {
      this.subscriber = ReactiveStreams.<Message<?>>builder()
        .flatMapCompletionStage(managePreProcessingAck())
        .map(message -> {
          invoke(message.getPayload());
          return message;
        })
        .flatMapCompletionStage(managePostProcessingAck())
        .ignore();
    }
  }

  private void processMethodReturningACompletionStage() {
    if (configuration.consumption() == MediatorConfiguration.Consumption.PAYLOAD) {
      this.subscriber = ReactiveStreams.<Message<?>>builder()
        .flatMapCompletionStage(managePreProcessingAck())
        .flatMapCompletionStage(message -> {
          CompletionStage<?> stage = invoke(message.getPayload());
          return stage.thenApply(x -> message);
        })
        .flatMapCompletionStage(managePostProcessingAck())
        .ignore();
    } else {
      this.subscriber = ReactiveStreams.<Message<?>>builder()
        .flatMapCompletionStage(managePreProcessingAck())
        .flatMapCompletionStage(message -> {
          CompletionStage<?> completion = invoke(message);
          return completion.thenApply(x -> message);
        })
        .flatMapCompletionStage(managePostProcessingAck())
        .ignore();
    }
  }

  private void processMethodReturningASubscriber() {
    Object result = invoke();
    if (!(result instanceof Subscriber) && !(result instanceof SubscriberBuilder)) {
      throw new IllegalStateException(
        "Invalid return type: " + result + " - expected a Subscriber or a SubscriberBuilder");
    }

    Subscriber sub;
    if (result instanceof Subscriber) {
      sub = (Subscriber) result;
    } else {
      sub = ((SubscriberBuilder) result).build();
    }
    if (configuration.consumption() == MediatorConfiguration.Consumption.STREAM_OF_PAYLOAD) {
      SubscriberWrapper<Object, Message> wrapper = new SubscriberWrapper<>(sub, x -> ((Message) x).getPayload());
      this.subscriber = ReactiveStreams.<Message>builder()
        .flatMapCompletionStage(managePreProcessingAck())
        .via(wrapper)
        .ignore();
    } else {
      Subscriber<Message> casted = (Subscriber<Message>) sub;
      this.subscriber = ReactiveStreams.<Message>builder()
        .flatMapCompletionStage(managePreProcessingAck())
        .via(new SubscriberWrapper<>(casted, Function.identity()))
        .ignore();
    }
  }
}
