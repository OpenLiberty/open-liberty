package io.smallrye.reactive.messaging.extension;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.smallrye.reactive.messaging.annotations.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.reactivestreams.Publisher;

import java.util.concurrent.atomic.AtomicReference;

public class EmitterImpl<T> implements Emitter<T> {

  private final AtomicReference<FlowableEmitter<Message<? extends T>>> internal = new AtomicReference<>();
  private final Flowable<Message<? extends T>> publisher;

  EmitterImpl() {
    publisher = Flowable.create(x -> {
      if (!internal.compareAndSet(null, x)) {
        x.onError(new Exception("Emitter already created"));
      }
    }, BackpressureStrategy.BUFFER);
  }

  public Publisher<Message<? extends T>> getPublisher() {
    return publisher;
  }

  boolean isConnected() {
    return internal.get() != null;
  }

  @Override
  public synchronized Emitter<T> send(T msg) {
    if (msg == null) {
      throw new IllegalArgumentException("`null` is not a valid value");
    }
    FlowableEmitter<Message<? extends T>> emitter = verify();
    if (emitter.requested() == 0) {
      throw new IllegalStateException("Unable to send a message to the stream using the emitter - no data requested");
    }
    if (msg instanceof Message) {
      //noinspection unchecked
      emitter.onNext((Message) msg);
    } else {
      emitter.onNext(Message.of(msg));
    }
    return this;
  }

  private synchronized FlowableEmitter<Message<? extends T>> verify() {
    FlowableEmitter<Message<? extends T>> emitter = internal.get();
    if (emitter == null) {
      throw new IllegalStateException("Stream not yet connected");
    }
    if (emitter.isCancelled()) {
      throw new IllegalStateException("Stream has been terminated");
    }
    return emitter;
  }

  @Override
  public synchronized void complete() {
    verify().onComplete();
  }

  @Override
  public synchronized void error(Exception e) {
    if (e == null) {
      throw new IllegalArgumentException("`null` is not a valid exception");
    }
    verify().onError(e);
  }

  @Override
  public synchronized boolean isCancelled() {
    FlowableEmitter<Message<? extends T>> emitter = internal.get();
    return emitter == null || emitter.isCancelled();
  }

  @Override
  public boolean isRequested() {
    FlowableEmitter<Message<? extends T>> emitter = internal.get();
    return !isCancelled() && emitter.requested() > 0;
  }
}
