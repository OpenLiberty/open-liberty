/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.microprofile.reactive.streams.zerodep;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Processor that wraps a subscriber and publisher.
 */
public class WrappedProcessor<T, R> implements Processor<T, R> {
  private final Subscriber<T> subscriber;
  private final Publisher<R> publisher;

  public WrappedProcessor(Subscriber<T> subscriber, Publisher<R> publisher) {
    this.subscriber = subscriber;
    this.publisher = publisher;
  }

  @Override
  public void subscribe(Subscriber<? super R> subscriber) {
    publisher.subscribe(subscriber);
  }

  @Override
  public void onSubscribe(Subscription subscription) {
    subscriber.onSubscribe(subscription);
  }

  @Override
  public void onNext(T item) {
    subscriber.onNext(item);
  }

  @Override
  public void onError(Throwable throwable) {
    subscriber.onError(throwable);
  }

  @Override
  public void onComplete() {
    subscriber.onComplete();
  }
}

