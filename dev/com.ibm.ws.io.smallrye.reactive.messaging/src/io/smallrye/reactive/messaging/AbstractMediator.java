package io.smallrye.reactive.messaging;

import io.reactivex.Flowable;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public abstract class AbstractMediator {

  protected final MediatorConfiguration configuration;
  private Invoker invoker;

  public AbstractMediator(MediatorConfiguration configuration) {
    this.configuration = configuration;
  }

  public synchronized void setInvoker(Invoker invoker) {
    this.invoker = invoker;
  }

  public void run() {
    // Do nothing by default.
  }

  public void connectToUpstream(PublisherBuilder<? extends Message> publisher) {
    // Do nothing by default.
  }

  public MediatorConfiguration configuration() {
    return configuration;
  }

  public void initialize(Object bean) {
    // Method overriding initialize MUST call super(bean).
    synchronized (this) {
      if (this.invoker == null) {
        this.invoker = args -> {
          try {
            return this.configuration.getMethod().invoke(bean, args);
          } catch (Exception e) {
            throw new ProcessingException(configuration.methodAsString(), e);
          }
        };
      }
    }
  }

  @SuppressWarnings("unchecked")
  protected <T> T invoke(Object... args) {
    try {
      Objects.requireNonNull(this.invoker, "Invoker not initialized");
      return (T) this.invoker.invoke(args);
    } catch (RuntimeException e) {
      LoggerFactory.getLogger(configuration().methodAsString())
        .error("The method " + configuration().methodAsString() + " has thrown an exception", e);
      throw e;
    }
  }

  protected CompletionStage<? extends Void> getAckOrCompletion(Message<?> message) {
    CompletionStage<Void> ack = message.ack();
    if (ack != null) {
      return ack;
    } else {
      return CompletableFuture.completedFuture(null);
    }
  }

  public PublisherBuilder<? extends Message> getStream() {
    return null;
  }

  public MediatorConfiguration getConfiguration() {
    return configuration;
  }

  public String getMethodAsString() {
    return configuration.methodAsString();
  }

  public SubscriberBuilder<Message, Void> getComputedSubscriber() {
    return null;
  }

  public abstract boolean isConnected();

  protected Function<Message, ? extends CompletionStage<? extends Message>> managePostProcessingAck() {
    return message -> {
      if (configuration.getAcknowledgment() == Acknowledgment.Strategy.POST_PROCESSING) {
        return getAckOrCompletion(message).thenApply(x -> message);
      } else {
        return CompletableFuture.completedFuture(message);
      }
    };
  }

  protected Function<Message, ? extends CompletionStage<? extends Message>> managePreProcessingAck() {
    return message -> {
      if (configuration.getAcknowledgment() == Acknowledgment.Strategy.PRE_PROCESSING) {
        return getAckOrCompletion(message).thenApply(x -> message);
      }
      return CompletableFuture.completedFuture(message);
    };
  }

  public PublisherBuilder<? extends Message> decorate(PublisherBuilder<? extends Message> input) {
    if (input == null) {
      return null;
    }
    if (configuration.getBroadcast()) {
      Flowable<Message> flow = Flowable.fromPublisher(input.buildRs());
      if (configuration.getNumberOfSubscriberBeforeConnecting() != 0) {
        return ReactiveStreams.fromPublisher(Flowable.fromPublisher(flow).publish().autoConnect(configuration.getNumberOfSubscriberBeforeConnecting()));
      } else {
        return ReactiveStreams.fromPublisher(Flowable.fromPublisher(flow).publish().autoConnect());
      }
    } else {
      return input;
    }
  }

}
