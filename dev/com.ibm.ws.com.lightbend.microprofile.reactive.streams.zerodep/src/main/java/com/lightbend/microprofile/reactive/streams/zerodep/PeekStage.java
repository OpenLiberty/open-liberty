/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.microprofile.reactive.streams.zerodep;

import java.util.function.Consumer;

/**
 * A map stage.
 */
class PeekStage<T> extends GraphStage implements InletListener {
  private final StageInlet<T> inlet;
  private final StageOutlet<T> outlet;
  private final Consumer<T> consumer;

  PeekStage(BuiltGraph builtGraph, StageInlet<T> inlet, StageOutlet<T> outlet, Consumer<T> consumer) {
    super(builtGraph);
    this.inlet = inlet;
    this.outlet = outlet;
    this.consumer = consumer;

    inlet.setListener(this);
    outlet.forwardTo(inlet);
  }

  @Override
  public void onPush() {
    T element = inlet.grab();
    consumer.accept(element);
    outlet.push(element);
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
