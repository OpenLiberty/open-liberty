/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.microprofile.reactive.streams.zerodep;

/**
 * Take while stage.
 */
class LimitStage<T> extends GraphStage implements InletListener {
  private final StageInlet<T> inlet;
  private final StageOutlet<T> outlet;
  private final long maxSize;
  private long count = 0;

  LimitStage(BuiltGraph builtGraph, StageInlet<T> inlet, StageOutlet<T> outlet, long maxSize) {
    super(builtGraph);
    this.inlet = inlet;
    this.outlet = outlet;
    this.maxSize = maxSize;

    inlet.setListener(this);
    outlet.forwardTo(inlet);
  }

  @Override
  protected void postStart() {
    if (maxSize == 0) {
      closeAll();
    }
  }

  @Override
  public void onPush() {
    T element = inlet.grab();
    outlet.push(element);
    if (++count == maxSize) {
      closeAll();
    }
  }

  private void closeAll() {
    outlet.complete();
    inlet.cancel();
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
