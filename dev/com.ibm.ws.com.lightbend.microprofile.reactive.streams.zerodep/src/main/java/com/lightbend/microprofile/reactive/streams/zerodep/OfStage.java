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
  private final Iterable<T> elements;
  private Iterator<T> iterator;

  public OfStage(BuiltGraph builtGraph, StageOutlet<T> outlet, Iterable<T> elements) {
    super(builtGraph);
    this.outlet = outlet;
    this.elements = elements;

    outlet.setListener(this);
  }

  @Override
  protected void postStart() {
    iterator = elements.iterator();
    try {
      if (!iterator.hasNext()) {
        outlet.complete();
      }
    } catch (Exception e) {
      outlet.fail(e);
    }
  }

  @Override
  public void onPull() {
    outlet.push(iterator.next());
    if (!iterator.hasNext()) {
      outlet.complete();
    }
  }

  @Override
  public void onDownstreamFinish() {
  }
}
