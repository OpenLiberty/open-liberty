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

  private Throwable error;

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
    CompletionStage<R> future = mapper.apply(inlet.grab());
    future.whenCompleteAsync((result, error) -> {
      if (!outlet.isClosed()) {
        if (error == null) {
          outlet.push(result);
          if (inlet.isClosed()) {
            if (this.error != null) {
              outlet.fail(this.error);
            } else {
              outlet.complete();
            }
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
    if (!activeCompletionStage()) {
      outlet.complete();
    }
  }

  @Override
  public void onUpstreamFailure(Throwable error) {
    if (activeCompletionStage()) {
      this.error = error;
    } else {
      outlet.fail(error);
    }
  }

  private boolean activeCompletionStage() {
    return outlet.isAvailable() && !inlet.isPulled();
  }
}
