/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.streams.zerodep;

import org.eclipse.microprofile.reactive.streams.spi.Graph;

import java.util.function.Function;

class OnErrorResumeWithStage<T> extends GraphStage implements InletListener {

  private final StageInlet<T> inlet;
  private final StageOutlet<T> outlet;
  private final Function<Throwable, Graph> function;

  public OnErrorResumeWithStage(BuiltGraph builtGraph, StageInlet<T> inlet, StageOutlet<T> outlet, Function<Throwable, Graph> function) {
    super(builtGraph);
    this.inlet = inlet;
    this.outlet = outlet;
    this.function = function;

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
      Graph graph = function.apply(error);

      BuiltGraph.SubStageInlet<T> newInlet = createSubInlet(graph);

      // Wire the new inlet directly to/from the outlet, this stage no longer has any involvement in the stream.
      newInlet.forwardTo(outlet);
      outlet.forwardTo(newInlet);

      newInlet.start();
      if (outlet.isAvailable()) {
        newInlet.pull();
      }
    } catch (Exception e) {
      outlet.fail(e);
    }
  }
}
