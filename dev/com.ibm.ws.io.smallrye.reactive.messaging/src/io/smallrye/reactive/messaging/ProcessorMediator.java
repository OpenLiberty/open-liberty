package io.smallrye.reactive.messaging;

import org.apache.commons.lang3.ClassUtils;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;


public class ProcessorMediator extends AbstractMediator {

  private Processor<Message, Message> processor;
  private PublisherBuilder<? extends Message> publisher;

  public ProcessorMediator(MediatorConfiguration configuration) {
    super(configuration);
    if (configuration.shape() != Shape.PROCESSOR) {
      throw new IllegalArgumentException("Expected a Processor shape, received a " + configuration.shape());
    }
  }

  public void connectToUpstream(PublisherBuilder<? extends Message> publisher) {
    assert processor != null;
    this.publisher = decorate(publisher.via(processor));
  }

  @Override
  public PublisherBuilder<? extends Message> getStream() {
    return Objects.requireNonNull(publisher);
  }

  @Override
  public boolean isConnected() {
    return publisher != null;
  }

  @Override
  public void initialize(Object bean) {
    super.initialize(bean);
    // Supported signatures:
    // 1.  Processor<Message<I>, Message<O>> method()
    // 2.  Processor<I, O> method()
    // 3.  ProcessorBuilder<Message<I>, Message<O>> method()
    // 4.  ProcessorBuilder<I, O> method()

    // 5.  Publisher<Message<O>> method(Message<I> msg)
    // 6.  Publisher<O> method(I payload)
    // 7.  PublisherBuilder<Message<O>> method(Message<I> msg)
    // 8.  PublisherBuilder<O> method(I payload)

    // 9. Message<O> method(Message<I> msg)
    // 10. O method(I payload)
    // 11. CompletionStage<O> method(I payload)
    // 12. CompletionStage<Message<O>> method(Message<I> msg)

    switch (configuration.production()) {
      case STREAM_OF_MESSAGE:
        // Case 1, 3, 5, 7
        if (isReturningAProcessorOrAProcessorBuilder()) {
          if (configuration.usesBuilderTypes()) {
            // Case 3
            processMethodReturningAProcessorBuilderOfMessages();
          } else {
            // Case 1
            processMethodReturningAProcessorOfMessages();
          }
        } else if (isReturningAPublisherOrAPublisherBuilder()) {
          if (configuration.usesBuilderTypes()) {
            // Case 7
            processMethodReturningAPublisherBuilderOfMessageAndConsumingMessages();
          } else {
            // Case 5
            processMethodReturningAPublisherOfMessageAndConsumingMessages();
          }
        } else {
          throw new IllegalArgumentException("Invalid Processor - unsupported signature for " + configuration.methodAsString());
        }
        break;
      case STREAM_OF_PAYLOAD:
        // Case 2, 4, 6, 8
        if (isReturningAProcessorOrAProcessorBuilder()) {
          // Case 2, 4
          if (configuration.usesBuilderTypes()) {
            // Case 4
            processMethodReturningAProcessorBuilderOfPayloads();
          } else {
            // Case 2
            processMethodReturningAProcessorOfPayloads();
          }
        } else if (isReturningAPublisherOrAPublisherBuilder()) {
          // Case 6, 8
          if (configuration.usesBuilderTypes()) {
            // Case 8
            processMethodReturningAPublisherBuilderOfPayloadsAndConsumingPayloads();
          } else {
            // Case 6
            processMethodReturningAPublisherOfPayloadsAndConsumingPayloads();
          }
        } else {
          throw new IllegalArgumentException("Invalid Processor - unsupported signature for " + configuration.methodAsString());
        }
        break;
      case INDIVIDUAL_MESSAGE:
        // Case 9
        processMethodReturningIndividualMessageAndConsumingIndividualItem();
        break;
      case INDIVIDUAL_PAYLOAD:
        // Case 10
        processMethodReturningIndividualPayloadAndConsumingIndividualItem();
        break;
      case COMPLETION_STAGE_OF_MESSAGE:
        // Case 11
        processMethodReturningACompletionStageOfMessageAndConsumingIndividualMessage();
        break;
      case COMPLETION_STAGE_OF_PAYLOAD:
        // Case 12
        processMethodReturningACompletionStageOfPayloadAndConsumingIndividualPayload();
        break;
      default:
        throw new IllegalArgumentException("Unexpected production type: " + configuration.production());
    }
  }

  private void processMethodReturningAPublisherBuilderOfMessageAndConsumingMessages() {
    this.processor = ReactiveStreams.<Message>builder()
      .flatMapCompletionStage(managePreProcessingAck())
      .map(msg -> (PublisherBuilder<Message>) invoke(msg))
      .flatMap(Function.identity())
      .buildRs();
  }

  private void processMethodReturningAPublisherOfMessageAndConsumingMessages() {
    this.processor = ReactiveStreams.<Message>builder()
      .flatMapCompletionStage(managePreProcessingAck())
      .map(msg -> (Publisher<Message>) invoke(msg))
      .flatMapRsPublisher(Function.identity())
      .buildRs();
  }

  private void processMethodReturningAProcessorBuilderOfMessages() {
    ProcessorBuilder<Message, Message> builder = Objects.requireNonNull(invoke(),
      "The method " + configuration.methodAsString() + " returned `null`");

    this.processor = ReactiveStreams.<Message>builder()
      .flatMapCompletionStage(managePreProcessingAck())
      .via(builder)
      .buildRs();
  }

  private void processMethodReturningAProcessorOfMessages() {
    Processor<Message, Message> result = Objects.requireNonNull(invoke(), "The method " + configuration.methodAsString() + " returned `null`");
    this.processor = ReactiveStreams.<Message>builder()
      .flatMapCompletionStage(managePreProcessingAck())
      .via(result)
      .buildRs();
  }

  private void processMethodReturningAProcessorOfPayloads() {
    Processor returnedProcessor = invoke();

    this.processor = ReactiveStreams.<Message>builder()
      .flatMapCompletionStage(managePreProcessingAck())
      .map(m -> m.getPayload())
      .via(returnedProcessor)
      .map(Message::of)
      .buildRs();
  }

  private void processMethodReturningAProcessorBuilderOfPayloads() {
    ProcessorBuilder returnedProcessorBuilder = invoke();
    Objects.requireNonNull(returnedProcessorBuilder, "The method " + configuration.methodAsString()
      + " has returned an invalid value: null");

    this.processor = ReactiveStreams.<Message>builder()
      .flatMapCompletionStage(managePreProcessingAck())
      .map(m -> m.getPayload())
      .via(returnedProcessorBuilder)
      .map(Message::of)
      .buildRs();
  }

  private void processMethodReturningAPublisherBuilderOfPayloadsAndConsumingPayloads() {
    this.processor = ReactiveStreams.<Message>builder()
      .flatMapCompletionStage(managePreProcessingAck())
      .flatMap(message -> invoke(message.getPayload()))
      .map(p -> (Message) Message.of(p))
      .buildRs();
  }

  private void processMethodReturningAPublisherOfPayloadsAndConsumingPayloads() {
    this.processor = ReactiveStreams.<Message>builder()
      .flatMapCompletionStage(managePreProcessingAck())
      .flatMapRsPublisher(message -> invoke(message.getPayload()))
      .map(p -> (Message) Message.of(p))
      .buildRs();
  }

  private void processMethodReturningIndividualMessageAndConsumingIndividualItem() {
    // Item can be message or payload
    if (configuration.consumption() == MediatorConfiguration.Consumption.PAYLOAD) {
      this.processor = ReactiveStreams.<Message>builder()
        .flatMapCompletionStage(managePreProcessingAck())
        .flatMapCompletionStage(input -> {
          Message output = invoke(input.getPayload());
          return managePostProcessingAck().apply(input).thenApply(x -> output);
        })
        .buildRs();
    } else {
      this.processor = ReactiveStreams.<Message>builder()
        .flatMapCompletionStage(managePreProcessingAck())
        .flatMapCompletionStage(input -> {
          Message output = invoke(input);
          return managePostProcessingAck().apply(input).thenApply(x -> output);
        })
        .buildRs();
    }
  }

  private void processMethodReturningIndividualPayloadAndConsumingIndividualItem() {
    // Item can be message or payload.
    if (configuration.consumption() == MediatorConfiguration.Consumption.PAYLOAD) {
      this.processor = ReactiveStreams.<Message>builder()
        .flatMapCompletionStage(managePreProcessingAck())
        .flatMapCompletionStage(input -> {
          Object result = invoke(input.getPayload());
          return managePostProcessingAck().apply(input)
            .thenApply(x -> (Message) Message.of(result));
        })
        .buildRs();
    } else {
      // Message
      this.processor = ReactiveStreams.<Message>builder()
        .flatMapCompletionStage(managePreProcessingAck())
        .flatMapCompletionStage(input -> {
          Object result = invoke(input);
          return managePostProcessingAck().apply(input).thenApply(x -> (Message) Message.of(result));
        })
        .buildRs();
    }
  }

  private void processMethodReturningACompletionStageOfMessageAndConsumingIndividualMessage() {
    this.processor = ReactiveStreams.<Message>builder()
      .flatMapCompletionStage(managePreProcessingAck())
      .flatMapCompletionStage(input -> {
        CompletionStage<Message> cs = invoke(input);
        return cs.thenCompose(res -> managePostProcessingAck().apply(input).thenApply(x -> res));
      })
      .buildRs();
  }

  private void processMethodReturningACompletionStageOfPayloadAndConsumingIndividualPayload() {
    this.processor = ReactiveStreams.<Message>builder()
      .flatMapCompletionStage(managePreProcessingAck())
      .flatMapCompletionStage(input -> {
        CompletionStage<Object> cs = invoke(input.getPayload());
        return cs
          .thenCompose(res -> managePostProcessingAck().apply(input).thenApply(p -> (Message) Message.of(res)));
      })
      .buildRs();
  }

  private boolean isReturningAPublisherOrAPublisherBuilder() {
    Class<?> returnType = configuration.getMethod().getReturnType();
    return ClassUtils.isAssignable(returnType, Publisher.class) || ClassUtils.isAssignable(returnType, PublisherBuilder.class);
  }

  private boolean isReturningAProcessorOrAProcessorBuilder() {
    Class<?> returnType = configuration.getMethod().getReturnType();
    return ClassUtils.isAssignable(returnType, Processor.class) || ClassUtils.isAssignable(returnType, ProcessorBuilder.class);
  }
}
