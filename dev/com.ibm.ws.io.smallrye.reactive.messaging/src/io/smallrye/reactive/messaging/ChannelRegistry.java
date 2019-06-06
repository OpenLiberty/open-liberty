package io.smallrye.reactive.messaging;

import io.smallrye.reactive.messaging.annotations.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.List;
import java.util.Set;

public interface ChannelRegistry {


  PublisherBuilder<? extends Message> register(String name, PublisherBuilder<? extends Message> stream);

  SubscriberBuilder<? extends Message, Void> register(String name, SubscriberBuilder<? extends Message, Void> subscriber);

  void register(String name, Emitter<?> emitter);

  List<PublisherBuilder<? extends Message>> getPublishers(String name);

  Emitter<?> getEmitter(String name);

  List<SubscriberBuilder<? extends Message, Void>> getSubscribers(String name);

  Set<String> getIncomingNames();

  Set<String> getOutgoingNames();

  Set<String> getEmitterNames();
}
