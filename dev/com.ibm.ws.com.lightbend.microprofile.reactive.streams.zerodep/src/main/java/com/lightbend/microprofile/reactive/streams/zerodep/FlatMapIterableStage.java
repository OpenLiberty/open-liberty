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

  private Throwable error;
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
    Iterator<R> iterator = mapper.apply(inlet.grab()).iterator();

    if (iterator.hasNext()) {
      this.iterator = iterator;

      outlet.push(iterator.next());
      // Make sure we're still on the same iterator in case a recursive call changed things
      if (!iterator.hasNext() && this.iterator == iterator) {
        this.iterator = null;
      }
    } else {
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
    if (iterator == null) {
      outlet.fail(error);
    } else {
      this.error = error;
    }
  }

  @Override
  public void onPull() {
    if (iterator == null) {
      inlet.pull();
    } else {
      Iterator<R> iterator = this.iterator;
      outlet.push(iterator.next());
      if (!iterator.hasNext() && this.iterator == iterator) {
        this.iterator = null;
        if (inlet.isClosed()) {
          if (error != null) {
            outlet.fail(error);
          } else {
            outlet.complete();
          }
        }
      }
    }
  }

  @Override
  public void onDownstreamFinish() {
    inlet.cancel();
  }
}
