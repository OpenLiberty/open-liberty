/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.microprofile.reactive.streams.zerodep;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collector;

/**
 * Stage that collects elements into a collector.
 */
class CollectStage<T, A, R> extends GraphStage implements InletListener {
  private final StageInlet<T> inlet;
  private final CompletableFuture<R> result;
  private final Collector<T, A, R> collector;
  private A container;

  public CollectStage(BuiltGraph builtGraph, StageInlet<T> inlet,
      CompletableFuture<R> result, Collector<T, A, R> collector) {
    super(builtGraph);
    this.inlet = inlet;
    this.result = result;
    this.collector = collector;

    container = collector.supplier().get();
    inlet.setListener(this);
  }

  @Override
  protected void postStart() {
    inlet.pull();
  }

  @Override
  public void onPush() {
    collector.accumulator().accept(container, inlet.grab());
    inlet.pull();
  }

  @Override
  public void onUpstreamFinish() {
    result.complete(collector.finisher().apply(container));
    container = null;
  }

  @Override
  public void onUpstreamFailure(Throwable error) {
    result.completeExceptionally(error);
    container = null;
  }
}
