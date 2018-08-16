/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.streams.zerodep;

import java.util.function.Function;

class OnErrorResumeStage<T> extends GraphStage implements InletListener, OutletListener {

  private final StageInlet<T> inlet;
  private final StageOutlet<T> outlet;
  private final Function<Throwable, T> function;

  private T recoveredElement;

  public OnErrorResumeStage(BuiltGraph builtGraph, StageInlet<T> inlet, StageOutlet<T> outlet, Function<Throwable, T> function) {
    super(builtGraph);
    this.inlet = inlet;
    this.outlet = outlet;
    this.function = function;

    inlet.setListener(this);
    outlet.setListener(this);
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
      T element = function.apply(error);
      if (outlet.isAvailable()) {
        outlet.push(element);
        outlet.complete();
      } else {
        recoveredElement = element;
      }
    } catch (Exception e) {
      outlet.fail(e);
    }
  }

  @Override
  public void onPull() {
    if (recoveredElement != null) {
      outlet.push(recoveredElement);
      outlet.complete();
    } else {
      inlet.pull();
    }
  }

  @Override
  public void onDownstreamFinish() {
    inlet.cancel();
  }
}
