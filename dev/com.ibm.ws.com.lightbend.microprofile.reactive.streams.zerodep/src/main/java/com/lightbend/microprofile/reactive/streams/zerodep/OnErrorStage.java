/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.streams.zerodep;

import java.util.function.Consumer;

class OnErrorStage<T> extends GraphStage implements InletListener {
  private final StageInlet<T> inlet;
  private final StageOutlet<T> outlet;
  private final Consumer<Throwable> consumer;

  OnErrorStage(BuiltGraph builtGraph, StageInlet<T> inlet, StageOutlet<T> outlet, Consumer<Throwable> consumer) {
    super(builtGraph);
    this.inlet = inlet;
    this.outlet = outlet;
    this.consumer = consumer;

    inlet.setListener(this);
    outlet.forwardTo(inlet);
  }

  @Override
  public void onPush() {
    outlet.push(inlet.grab());
  }

  @Override
  public void onUpstreamFinish() {
    outlet.complete();
  }

  @Override
  public void onUpstreamFailure(Throwable error) {
    try {
      outlet.fail(error);
    } catch (RuntimeException e) {
      error = e;
    }
    consumer.accept(error);
  }
}
