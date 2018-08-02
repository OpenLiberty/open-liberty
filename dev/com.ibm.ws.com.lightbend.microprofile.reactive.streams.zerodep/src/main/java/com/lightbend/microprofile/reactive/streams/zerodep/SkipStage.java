/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.microprofile.reactive.streams.zerodep;

/**
 * A filter stage.
 */
class SkipStage<T> extends GraphStage implements InletListener {
  private final StageInlet<T> inlet;
  private final StageOutlet<T> outlet;
  private final long toSkip;
  private long count = 0;

  SkipStage(BuiltGraph builtGraph, StageInlet<T> inlet, StageOutlet<T> outlet, long toSkip) {
    super(builtGraph);
    this.inlet = inlet;
    this.outlet = outlet;
    this.toSkip = toSkip;

    if (toSkip == 0) {
      inlet.forwardTo(outlet);
    } else {
      inlet.setListener(this);
    }
    outlet.forwardTo(inlet);
  }

  @Override
  public void onPush() {
    count++;
    inlet.grab();
    if (count == toSkip) {
      inlet.forwardTo(outlet);
    }
    inlet.pull();
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
