/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.microprofile.reactive.streams.zerodep;

import java.util.Iterator;

/**
 * Of stage.
 */
class OfStage<T> extends GraphStage implements OutletListener {
  private final StageOutlet<T> outlet;
  private Iterator<T> elements;

  public OfStage(BuiltGraph builtGraph, StageOutlet<T> outlet, Iterable<T> elements) {
    super(builtGraph);
    this.outlet = outlet;
    this.elements = elements.iterator();

    outlet.setListener(this);
  }

  @Override
  protected void postStart() {
    if (!elements.hasNext()) {
      outlet.complete();
    }
  }

  @Override
  public void onPull() {
    outlet.push(elements.next());
    if (!elements.hasNext()) {
      outlet.complete();
    }
  }

  @Override
  public void onDownstreamFinish() {
  }
}
