/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.microprofile.reactive.streams.zerodep;

import java.util.Iterator;
import java.util.function.Function;

/**
 * A flatmap to iterable stage.
 */
class FlatMapIterableStage<T, R> extends GraphStage implements InletListener, OutletListener {
  private final StageInlet<T> inlet;
  private final StageOutlet<R> outlet;
  private final Function<T, Iterable<R>> mapper;

  private Iterator<R> iterator;

  FlatMapIterableStage(BuiltGraph builtGraph, StageInlet<T> inlet, StageOutlet<R> outlet, Function<T, Iterable<R>> mapper) {
    super(builtGraph);
    this.inlet = inlet;
    this.outlet = outlet;
    this.mapper = mapper;

    inlet.setListener(this);
    outlet.setListener(this);
  }

  @Override
  public void onPush() {
    iterator = mapper.apply(inlet.grab()).iterator();

    if (iterator.hasNext()) {
      outlet.push(iterator.next());
      if (!iterator.hasNext()) {
        iterator = null;
      }
    } else {
      iterator = null;
      inlet.pull();
    }
  }

  @Override
  public void onUpstreamFinish() {
    if (iterator == null) {
      outlet.complete();
    }
  }

  @Override
  public void onUpstreamFailure(Throwable error) {
    // Allow failures to overtake elements here
    outlet.fail(error);
  }

  @Override
  public void onPull() {
    if (iterator == null) {
      inlet.pull();
    } else {
      outlet.push(iterator.next());
      if (!iterator.hasNext()) {
        iterator = null;
        if (inlet.isClosed()) {
          outlet.complete();
        }
      }
    }
  }

  @Override
  public void onDownstreamFinish() {
    inlet.cancel();
  }
}
