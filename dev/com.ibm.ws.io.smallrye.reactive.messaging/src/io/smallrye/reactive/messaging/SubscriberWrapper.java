package io.smallrye.reactive.messaging;

import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class SubscriberWrapper<I, T> implements Processor<T, T> {

  private final Subscriber<I> delegate;
  AtomicReference<Subscriber<? super T>> subscriber = new AtomicReference<>();
  private Function<T, I> mapper;

  public SubscriberWrapper(Subscriber<I> subscriber, Function<T, I> mapper) {
    this.delegate = Objects.requireNonNull(subscriber);
    this.mapper = Objects.requireNonNull(mapper);
  }

  @Override
  public void subscribe(Subscriber<? super T> s) {
    if (!this.subscriber.compareAndSet(null, s)) {
      s.onSubscribe(new Subscription() {
        @Override
        public void request(long n) {
          // Ignored.
        }

        @Override
        public void cancel() {
          // Ignored.
        }
      });
      s.onError(new IllegalStateException("Broadcast not supported"));
    }
  }

  @Override
  public void onSubscribe(Subscription s) {
    delegate.onSubscribe(s);
  }

  @Override
  public void onNext(T item) {
    try {
      delegate.onNext(mapper.apply(item));
      subscriber.get().onNext(item);
    } catch (Exception e) {
      subscriber.get().onError(e);
    }
  }

  @Override
  public void onError(Throwable error) {
    delegate.onError(error);
  }

  @Override
  public void onComplete() {
    delegate.onComplete();
  }
}
