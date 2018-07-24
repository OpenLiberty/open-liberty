/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.streams.zerodep;

public class OnTerminateStage<T> extends GraphStage implements InletListener, OutletListener {
  private final StageInlet<T> inlet;
  private final StageOutlet<T> outlet;
  private final Runnable action;

  public OnTerminateStage(BuiltGraph builtGraph, StageInlet<T> inlet, StageOutlet<T> outlet, Runnable action) {
    super(builtGraph);
    this.inlet = inlet;
    this.outlet = outlet;
    this.action = action;

    inlet.setListener(this);
    outlet.setListener(this);
  }

  @Override
  public void onPush() {
    outlet.push(inlet.grab());
  }

  @Override
  public void onUpstreamFinish() {
    action.run();
    outlet.complete();
  }

  @Override
  public void onUpstreamFailure(Throwable error) {
    try {
      action.run();
    } catch (Exception e) {
      error = e;
    }
    outlet.fail(error);
  }

  @Override
  public void onPull() {
    inlet.pull();
  }

  @Override
  public void onDownstreamFinish() {
    try {
      action.run();
    } catch (Exception e) {
      // Ignore??
    } finally {
      inlet.cancel();
    }
  }
}
