/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.microprofile.reactive.streams.zerodep;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

/**
 * Stage that just captures termination signals, and redeems the given completable future when it does.
 */
public class CaptureTerminationStage<T> extends GraphStage implements InletListener, OutletListener {
  private final StageInlet<T> inlet;
  private final StageOutlet<T> outlet;
  private final CompletableFuture<Void> result;

  public CaptureTerminationStage(BuiltGraph builtGraph, StageInlet<T> inlet, StageOutlet<T> outlet, CompletableFuture<Void> result) {
    super(builtGraph);
    this.inlet = inlet;
    this.outlet = outlet;
    this.result = result;

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
    result.complete(null);
  }

  @Override
  public void onUpstreamFailure(Throwable error) {
    outlet.fail(error);
    result.completeExceptionally(error);
  }

  @Override
  public void onPull() {
    inlet.pull();
  }

  @Override
  public void onDownstreamFinish() {
    inlet.cancel();
    result.completeExceptionally(new CancellationException("Cancelled"));
  }
}
