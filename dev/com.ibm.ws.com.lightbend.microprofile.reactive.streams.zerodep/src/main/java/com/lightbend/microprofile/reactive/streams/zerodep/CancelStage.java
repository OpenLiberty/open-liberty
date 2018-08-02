/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.microprofile.reactive.streams.zerodep;

import java.util.concurrent.CompletableFuture;

/**
 * A cancel stage.
 */
class CancelStage extends GraphStage implements InletListener {
  private final StageInlet<?> inlet;
  private final CompletableFuture<Void> result;

  CancelStage(BuiltGraph builtGraph, StageInlet<?> inlet, CompletableFuture<Void> result) {
    super(builtGraph);
    this.inlet = inlet;
    this.result = result;

    inlet.setListener(this);
  }

  @Override
  protected void postStart() {
    inlet.cancel();
    result.complete(null);
  }

  @Override
  public void onPush() {
  }

  @Override
  public void onUpstreamFinish() {
  }

  @Override
  public void onUpstreamFailure(Throwable error) {
  }
}
