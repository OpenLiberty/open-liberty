/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.microprofile.reactive.streams.zerodep;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * An inlet that is a subscriber.
 *
 * This is either the first inlet for a graph that has an inlet, or is used to connect a Processor or Subscriber stage
 * in a graph.
 */
final class SubscriberInlet<T> implements StageInlet<T>, Subscriber<T>, Port {
  private final BuiltGraph builtGraph;
  private final int bufferHighWatermark;
  private final int bufferLowWatermark;

  private final Deque<T> elements = new ArrayDeque<>();
  private Subscription subscription;
  private int outstandingDemand;
  private InletListener listener;
  private boolean upstreamFinished;
  private boolean downstreamFinished;
  private Throwable error;
  private boolean pulled;
  private boolean pushed;

  SubscriberInlet(BuiltGraph builtGraph, int bufferHighWatermark, int bufferLowWatermark) {
    this.builtGraph = builtGraph;
    this.bufferHighWatermark = bufferHighWatermark;
    this.bufferLowWatermark = bufferLowWatermark;
  }

  @Override
  public void onStreamFailure(Throwable reason) {
    if (!downstreamFinished) {
      downstreamFinished = true;
      if (listener != null) {
        listener.onUpstreamFailure(reason);
      }
    }
    if (!upstreamFinished) {
      if (subscription != null) {
        upstreamFinished = true;
        subscription.cancel();
        subscription = null;
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
  public void onSubscribe(Subscription subscription) {
    Objects.requireNonNull(subscription, "Subscription must not be null");
    builtGraph.execute(() -> {
      if (upstreamFinished || downstreamFinished || this.subscription != null) {
        subscription.cancel();
      } else {
        this.subscription = subscription;
        maybeRequest();
      }
    });
  }

  private void maybeRequest() {
    if (!upstreamFinished) {
      int bufferSize = outstandingDemand + elements.size();
      if (bufferSize <= bufferLowWatermark) {
        int toRequest = bufferHighWatermark - bufferSize;
        outstandingDemand += toRequest;
        subscription.request(toRequest);
      }
    }
  }

  @Override
  public void onNext(T item) {
    Objects.requireNonNull(item, "Elements passed to onNext must not be null");
    // possible optimization: place item in a queue, and dispatch singleton runnable to executor, to save an
    // allocation
    builtGraph.execute(() -> {
      if (downstreamFinished || upstreamFinished) {
        // Ignore events after cancellation or complete
      } else if (outstandingDemand == 0) {
        onStreamFailure(new IllegalStateException("Element signalled without demand for it"));
      } else {
        outstandingDemand -= 1;
        elements.add(item);
        if (pulled) {
          pushed = true;
          builtGraph.enqueueSignal(onPushSignal);
        }
      }
    });
  }

  @Override
  public void onError(Throwable throwable) {
    Objects.requireNonNull(throwable, "Error passed to onError must not be null");
    builtGraph.execute(() -> {
      if (downstreamFinished || upstreamFinished) {
        // Ignore
      } else {
        subscription = null;
        upstreamFinished = true;
        error = throwable;
        if (elements.isEmpty()) {
          builtGraph.enqueueSignal(onUpstreamFailureSignal);
        }
      }
    });
  }

  @Override
  public void onComplete() {
    builtGraph.execute(() -> {
      if (downstreamFinished || upstreamFinished) {
        // Ignore
      } else {
        subscription = null;
        upstreamFinished = true;
        if (elements.isEmpty()) {
          builtGraph.enqueueSignal(onUpstreamFinishSignal);
        }
      }
    });
  }

  @Override
  public void pull() {
    if (downstreamFinished) {
      throw new IllegalStateException("Can't pull when finished");
    } else if (pulled) {
      throw new IllegalStateException("Can't pull twice");
    }
    pulled = true;
    if (!elements.isEmpty()) {
      pushed = true;
      builtGraph.enqueueSignal(onPushSignal);
    }
  }

  @Override
  public boolean isPulled() {
    return pulled;
  }

  @Override
  public boolean isAvailable() {
    return !elements.isEmpty();
  }

  @Override
  public boolean isClosed() {
    return downstreamFinished;
  }

  @Override
  public void cancel() {
    if (downstreamFinished) {
      throw new IllegalStateException("Can't cancel twice");
    } else {
      downstreamFinished = true;
      error = null;
      elements.clear();
      if (subscription != null) {
        upstreamFinished = true;
        subscription.cancel();
      }
    }
  }

  @Override
  public T grab() {
    if (downstreamFinished) {
      throw new IllegalStateException("Can't grab when finished");
    } else if (!pulled) {
      throw new IllegalStateException("Can't grab when not pulled");
    } else if (!pushed) {
      throw new IllegalStateException("Grab without onPush");
    } else {
      pushed = false;
      pulled = false;
      T element = elements.removeFirst();
      // Signal another signal so that we can notify downstream complete after
      // it gets the element without pulling first.
      if (elements.isEmpty() && upstreamFinished) {
        if (error != null) {
          builtGraph.enqueueSignal(onUpstreamFailureSignal);
        } else {
          builtGraph.enqueueSignal(onUpstreamFinishSignal);
        }
      } else {
        maybeRequest();
      }
      return element;
    }
  }

  @Override
  public void setListener(InletListener listener) {
    this.listener = Objects.requireNonNull(listener, "Listener must not be null");
  }

  private final Signal onPushSignal = () -> {
    try {
      if (!downstreamFinished) {
        listener.onPush();
      }
    } catch (Exception e) {
      onStreamFailure(e);
    }
  };

  private final Signal onUpstreamFinishSignal = () -> {
    if (!downstreamFinished) {
      downstreamFinished = true;
      try {
        listener.onUpstreamFinish();
      } catch (Exception e) {
        listener.onUpstreamFailure(e);
        if (!upstreamFinished) {
          upstreamFinished = true;
          subscription.cancel();
        }
      }
    }
  };

  private final Signal onUpstreamFailureSignal = () -> {
    if (!downstreamFinished) {
      downstreamFinished = true;
      Throwable theFailure = error;
      error = null;
      listener.onUpstreamFailure(theFailure);
    }
  };
}
