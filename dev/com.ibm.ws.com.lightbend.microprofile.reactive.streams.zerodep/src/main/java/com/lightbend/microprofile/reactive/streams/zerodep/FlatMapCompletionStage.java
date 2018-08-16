/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.microprofile.reactive.streams.zerodep;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Flat maps to completion stages of elements.
 */
class FlatMapCompletionStage<T, R> extends GraphStage implements InletListener {
  private final StageInlet<T> inlet;
  private final StageOutlet<R> outlet;
  private final Function<T, CompletionStage<R>> mapper;

  private CompletionStage<R> activeCompletionStage;

  FlatMapCompletionStage(BuiltGraph builtGraph, StageInlet<T> inlet, StageOutlet<R> outlet, Function<T, CompletionStage<R>> mapper) {
    super(builtGraph);
    this.inlet = inlet;
    this.outlet = outlet;
    this.mapper = mapper;

    inlet.setListener(this);
    outlet.forwardTo(inlet);
  }

  @Override
  public void onPush() {
    activeCompletionStage = mapper.apply(inlet.grab());
    activeCompletionStage.whenCompleteAsync((result, error) -> {
      activeCompletionStage = null;
      if (!outlet.isClosed()) {
        if (error == null) {
          outlet.push(result);
          if (inlet.isClosed()) {
            outlet.complete();
          }
        } else {
          outlet.fail(error);
          if (!inlet.isClosed()) {
            inlet.cancel();
          }
        }
      }
    }, executor());
  }

  @Override
  public void onUpstreamFinish() {
    if (activeCompletionStage == null) {
      outlet.complete();
    }
  }

  @Override
  public void onUpstreamFailure(Throwable error) {
    outlet.fail(error);
  }
}
