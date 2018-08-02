/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.microprofile.reactive.streams.zerodep;

import java.util.function.Predicate;

/**
 * Take while stage.
 */
class TakeWhileStage<T> extends GraphStage implements InletListener {
  private final StageInlet<T> inlet;
  private final StageOutlet<T> outlet;
  private final Predicate<T> predicate;

  TakeWhileStage(BuiltGraph builtGraph, StageInlet<T> inlet, StageOutlet<T> outlet, Predicate<T> predicate) {
    super(builtGraph);
    this.inlet = inlet;
    this.outlet = outlet;
    this.predicate = predicate;

    inlet.setListener(this);
    outlet.forwardTo(inlet);
  }

  @Override
  public void onPush() {
    T element = inlet.grab();
    if (predicate.test(element)) {
      outlet.push(element);
    } else {
      if (!outlet.isClosed()) outlet.complete();
      if (!inlet.isClosed()) inlet.cancel();
    }
  }

  @Override
  public void onUpstreamFinish() {
    outlet.complete();
  }

  @Override
  public void onUpstreamFailure(Throwable error) {
    outlet.fail(error);
  }
}
