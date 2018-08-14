/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.microprofile.reactive.streams.zerodep;

import java.util.HashSet;
import java.util.Set;

/**
 * A filter stage.
 */
class DistinctStage<T> extends GraphStage implements InletListener {
  private final StageInlet<T> inlet;
  private final StageOutlet<T> outlet;
  private final Set<T> seen = new HashSet<>();

  DistinctStage(BuiltGraph builtGraph, StageInlet<T> inlet, StageOutlet<T> outlet) {
    super(builtGraph);
    this.inlet = inlet;
    this.outlet = outlet;

    inlet.setListener(this);
    outlet.forwardTo(inlet);
  }

  @Override
  public void onPush() {
    T element = inlet.grab();
    if (seen.add(element)) {
      outlet.push(element);
    } else {
      inlet.pull();
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
