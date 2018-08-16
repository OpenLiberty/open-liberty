/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.microprofile.reactive.streams.zerodep;

public class ConcatStage<T> extends GraphStage implements InletListener, OutletListener {

  private final BuiltGraph.SubStageInlet<T> first;
  private final BuiltGraph.SubStageInlet<T> second;
  private final StageOutlet<T> outlet;

  public ConcatStage(BuiltGraph builtGraph, BuiltGraph.SubStageInlet<T> first, BuiltGraph.SubStageInlet<T> second, StageOutlet<T> outlet) {
    super(builtGraph);
    this.first = first;
    this.second = second;
    this.outlet = outlet;

    first.setListener(this);
    outlet.setListener(this);
  }

  @Override
  protected void postStart() {
    first.start();
  }

  @Override
  public void onPull() {
    first.pull();
  }

  @Override
  public void onDownstreamFinish() {
    first.cancel();
    // Start up second so we can shut it down, in case it holds any resources.
    startAndCancelSecond();
  }

  @Override
  public void onPush() {
    outlet.push(first.grab());
  }

  @Override
  public void onUpstreamFinish() {
    second.forwardTo(outlet);
    outlet.forwardTo(second);
    second.start();
    if (outlet.isAvailable()) {
      second.pull();
    }
  }

  @Override
  public void onUpstreamFailure(Throwable error) {
    outlet.fail(error);
    startAndCancelSecond();
  }

  private void startAndCancelSecond() {
    try {
      second.setListener(new InletListener() {
        @Override
        public void onPush() {
        }

        @Override
        public void onUpstreamFinish() {
        }

        @Override
        public void onUpstreamFailure(Throwable error) {
        }
      });
      second.start();
      second.cancel();
    } catch (Exception e) {
      // Ignore exceptions
    }
  }
}
