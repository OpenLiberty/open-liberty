/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.microprofile.reactive.streams.zerodep;

import java.util.Objects;

/**
 * A stage outlet and inlet. Elements passed in to the outlet are forwarded to the inlet, and backpressure from the
 * inlet flows to the outlet.
 *
 * This port is for use between two stages of a graph.
 */
final class StageOutletInlet<T> implements Port {
  private final BuiltGraph builtGraph;

  private InletListener inletListener;
  private OutletListener outletListener;
  private boolean outletPulled;
  private T pushedElement;
  private boolean inletPushed;
  private boolean outletFinished;
  private boolean inletFinished;
  private Throwable failure;

  StageOutletInlet(BuiltGraph builtGraph) {
    this.builtGraph = builtGraph;
  }

  @Override
  public void onStreamFailure(Throwable reason) {
    if (!outletFinished) {
      outletFinished = true;
      if (outletListener != null) {
        outletListener.onDownstreamFinish();
      }
    }
    if (!inletFinished) {
      inletFinished = true;
      if (inletListener != null) {
        inletListener.onUpstreamFailure(reason);
      }
    }
  }

  @Override
  public void verifyReady() {
    if (inletListener == null) {
      throw new IllegalStateException("Cannot start stream without inlet listener set");
    }
    if (outletListener == null) {
      throw new IllegalStateException("Cannot start stream without outlet listener set");
    }
  }

  final class Outlet implements StageOutlet<T> {
    @Override
    public void push(T element) {
      Objects.requireNonNull(element, "Elements cannot be null");
      if (outletFinished) {
        throw new IllegalStateException("Can't push element after complete");
      } else if (!outletPulled) {
        throw new IllegalStateException("Can't push element to outlet when it hasn't pulled");
      } else {
        outletPulled = false;
        pushedElement = element;
        builtGraph.enqueueSignal(onPushSignal);
      }
    }

    @Override
    public boolean isAvailable() {
      return !outletFinished && outletPulled;
    }

    @Override
    public void complete() {
      if (outletFinished) {
        throw new IllegalStateException("Can't complete twice.");
      }
      outletFinished = true;
      builtGraph.enqueueSignal(onUpstreamFinishSignal);
    }

    @Override
    public boolean isClosed() {
      return outletFinished;
    }

    @Override
    public void fail(Throwable error) {
      Objects.requireNonNull(error, "Error must not be null");
      if (outletFinished) {
        throw new IllegalStateException("Can't complete twice.");
      }
      outletFinished = true;
      failure = error;
      builtGraph.enqueueSignal(onUpstreamErrorSignal);
    }

    @Override
    public void setListener(OutletListener listener) {
      outletListener = Objects.requireNonNull(listener, "Cannot register null listener");
    }
  }

  final class Inlet implements StageInlet<T> {
    boolean inletPulled;

    @Override
    public void pull() {
      if (inletFinished) {
        throw new IllegalStateException("Can't pull after complete");
      } else if (inletPulled) {
        throw new IllegalStateException("Can't pull twice");
      } else if (pushedElement != null) {
        throw new IllegalStateException("Can't pull without having grabbed the previous element");
      }
      if (!outletFinished) {
        inletPulled = true;
        builtGraph.enqueueSignal(onPullSignal);
      }
    }

    @Override
    public boolean isPulled() {
      return inletPulled;
    }

    @Override
    public boolean isAvailable() {
      return inletPushed;
    }

    @Override
    public boolean isClosed() {
      return inletFinished;
    }

    @Override
    public void cancel() {
      if (inletFinished) {
        throw new IllegalStateException("Stage already finished");
      }
      inletFinished = true;
      pushedElement = null;
      builtGraph.enqueueSignal(onDownstreamFinishSignal);
    }

    @Override
    public T grab() {
      if (!inletPushed) {
        throw new IllegalStateException("Grab without onPush notification");
      }
      T grabbed = pushedElement;
      inletPushed = false;
      inletPulled = false;
      pushedElement = null;
      return grabbed;
    }

    @Override
    public void setListener(InletListener listener) {
      inletListener = Objects.requireNonNull(listener, "Cannot register null listener");
    }
  }

  private abstract class RecoverableSignal implements Signal {
    @Override
    public final void signal() {
      try {
        doSignal();
      } catch (Exception e) {
        onStreamFailure(e);
      }
    }

    protected abstract void doSignal();
  }

  private final Signal onPullSignal = new RecoverableSignal() {
    @Override
    protected void doSignal() {
      if (!outletFinished) {
        outletPulled = true;
        outletListener.onPull();
      }
    }
  };

  private final Signal onDownstreamFinishSignal = () -> {
    if (!outletFinished) {
      outletFinished = true;
      outletListener.onDownstreamFinish();
    }
  };

  private final Signal onPushSignal = new RecoverableSignal() {
    @Override
    protected void doSignal() {
      if (!inletFinished) {
        inletPushed = true;
        inletListener.onPush();
      }
    }
  };

  private final Signal onUpstreamFinishSignal = () -> {
    if (!inletFinished) {
      inletFinished = true;
      try {
        inletListener.onUpstreamFinish();
      } catch (Exception e) {
        inletListener.onUpstreamFailure(e);
        if (!outletFinished) {
          outletFinished = true;
          outletListener.onDownstreamFinish();
        }
      }
    }
  };

  private final Signal onUpstreamErrorSignal = () -> {
    if (!inletFinished) {
      inletFinished = true;
      Throwable theFailure = failure;
      failure = null;
      inletListener.onUpstreamFailure(theFailure);
    }
  };
}
