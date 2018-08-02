/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.microprofile.reactive.streams.zerodep;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

class FindFirstStage<T> extends GraphStage implements InletListener {

  private final StageInlet<T> inlet;
  private final CompletableFuture<Optional<T>> result;

  FindFirstStage(BuiltGraph builtGraph, StageInlet<T> inlet, CompletableFuture<Optional<T>> result) {
    super(builtGraph);
    this.inlet = inlet;
    this.result = result;

    inlet.setListener(this);
  }

  @Override
  protected void postStart() {
    inlet.pull();
  }

  @Override
  public void onPush() {
    result.complete(Optional.of(inlet.grab()));
    inlet.cancel();
  }

  @Override
  public void onUpstreamFinish() {
    result.complete(Optional.empty());
  }

  @Override
  public void onUpstreamFailure(Throwable error) {
    result.completeExceptionally(error);
  }
}
