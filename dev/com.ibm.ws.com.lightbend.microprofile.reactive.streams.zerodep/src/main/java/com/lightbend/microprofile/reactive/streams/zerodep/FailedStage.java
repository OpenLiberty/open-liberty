/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.microprofile.reactive.streams.zerodep;

/**
 * A failed stage. Does nothing but fails the stream when the graph starts.
 */
class FailedStage extends GraphStage implements OutletListener {
  private final Throwable error;
  private final StageOutlet<?> outlet;

  public FailedStage(BuiltGraph builtGraph, StageOutlet<?> outlet, Throwable error) {
    super(builtGraph);
    this.outlet = outlet;
    this.error = error;

    outlet.setListener(this);
  }

  @Override
  protected void postStart() {
    outlet.fail(error);
  }

  @Override
  public void onPull() {
  }

  @Override
  public void onDownstreamFinish() {
  }
}
