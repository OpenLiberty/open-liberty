/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.microprofile.reactive.streams.zerodep;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Objects;

/**
 * An outlet that is a publisher.
 *
 * This is either the last outlet for a graph that has an outlet, or is used to connect a Processor or Publisher stage
 * in a graph.
 */
final class PublisherOutlet<T> implements StageOutlet<T>, Publisher<T>, Subscription, Port {

  private final BuiltGraph builtGraph;

  private Subscriber<? super T> subscriber;
  private boolean pulled;
  private long demand;
  private boolean upstreamFinished;
  private boolean downstreamFinished;
  private Throwable failure;
  private OutletListener listener;

  PublisherOutlet(BuiltGraph builtGraph) {
    this.builtGraph = builtGraph;
  }

  @Override
  public void onStreamFailure(Throwable reason) {
    if (!upstreamFinished) {
      upstreamFinished = true;
      if (listener != null) {
        listener.onDownstreamFinish();
      }
    }
    if (!downstreamFinished) {
      if (subscriber != null) {
        downstreamFinished = true;
        subscriber.onError(reason);
      } else {
        failure = reason;
      }
    }
  }

  @Override
  public void verifyReady() {
    if (listener == null) {
      throw new IllegalStateException("Cannot start stream without inlet listener set");
    }
  }

  @Override
  public void subscribe(Subscriber<? super T> subscriber) {
    Objects.requireNonNull(subscriber, "Subscriber must not be null");
    builtGraph.execute(() -> {
      if (this.subscriber != null) {
        subscriber.onSubscribe(new Subscription() {
          @Override
          public void request(long n) {
          }

          @Override
          public void cancel() {
          }
        });
        subscriber.onError(new IllegalStateException("This publisher only supports one subscriber"));
      } else {
        this.subscriber = subscriber;
        subscriber.onSubscribe(this);
        if (upstreamFinished) {
          downstreamFinished = true;
          if (failure != null) {
            subscriber.onError(failure);
            failure = null;
            this.subscriber = null;
          } else {
            subscriber.onComplete();
            this.subscriber = null;
          }
        }
      }
    });
  }

  @Override
  public void request(long n) {
    // possible optimization: place n in a queue, and dispatch singleton runnable to executor, to save an allocation
    builtGraph.execute(() -> {
      if (!upstreamFinished) {
        if (n <= 0) {
          onStreamFailure(new IllegalArgumentException("Request demand must be greater than zero"));
        } else {
          boolean existingDemand = demand > 0;
          demand = demand + n;
          if (demand <= 0) {
            demand = Long.MAX_VALUE;
          }
          if (!existingDemand) {
            doPull();
          }
        }
      }
    });
  }

  private void doPull() {
    builtGraph.enqueueSignal(onPullSignal);
  }

  @Override
  public void cancel() {
    builtGraph.execute(() -> {
      subscriber = null;
      demand = 0;
      builtGraph.enqueueSignal(onDownstreamFinishSignal);
    });
  }

  @Override
  public void push(T element) {
    Objects.requireNonNull(element, "Elements cannot be null");
    if (upstreamFinished) {
      throw new IllegalStateException("Can't push after publisher is finished");
    } else if (demand <= 0) {
      throw new IllegalStateException("Push without pull");
    }
    pulled = false;
    if (!downstreamFinished) {
      if (demand != Long.MAX_VALUE) {
        demand -= 1;
      }
      subscriber.onNext(element);
      if (demand > 0) {
        doPull();
      }

    }

  }

  @Override
  public boolean isAvailable() {
    return !upstreamFinished && pulled;
  }

  @Override
  public void complete() {
    if (upstreamFinished) {
      throw new IllegalStateException("Can't complete twice");
    } else {
      upstreamFinished = true;
      demand = 0;
      if (subscriber != null && !downstreamFinished) {
        downstreamFinished = true;
        subscriber.onComplete();
        subscriber = null;
      }
    }
  }

  @Override
  public boolean isClosed() {
    return upstreamFinished;
  }

  @Override
  public void fail(Throwable error) {
    Objects.requireNonNull(error, "Error must not be null");
    if (upstreamFinished) {
      throw new IllegalStateException("Can't complete twice");
    } else {
      upstreamFinished = true;
      demand = 0;
      if (subscriber != null && !downstreamFinished) {
        downstreamFinished = true;
        subscriber.onError(error);
        subscriber = null;
      } else {
        failure = error;
      }
    }
  }

  @Override
  public void setListener(OutletListener listener) {
    this.listener = Objects.requireNonNull(listener, "Listener must not be null");
  }

  private final Signal onPullSignal = () -> {
    if (!upstreamFinished) {
      pulled = true;
      try {
        listener.onPull();
      } catch (Exception e) {
        onStreamFailure(e);
      }
    }
  };

  private final Signal onDownstreamFinishSignal = () -> {
    if (!upstreamFinished) {
      upstreamFinished = true;
      listener.onDownstreamFinish();
    }
  };
}
