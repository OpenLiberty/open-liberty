/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.streams.zerodep;

/**
 * A signal, used by inlets and outlets.
 * <p>
 * A signal is used to wrap callbacks to inlet and outlet listeners. Rather than invoking these directly, they are
 * enqueued to be invoked in an unrolled fashion, after the current callback is finished. This ensures that stages do
 * not have to handle reentrant invocations, when the stage invokes something on a inlet our outlet, it can be sure
 * that its state before invocation hasn't changed after the invocation returns. This also solves blowing the stack
 * as it ensures there is no unbounded recursion.
 * <p>
 * Generally speaking, the implementation of inlets and outlets will ensure that a new signal is not allocated each
 * time it is needed for pulls and pushes, rather the signal is instantiated once at start up and reused each time.
 * This is safe to do since those signals don't carry state.
 */
public interface Signal {
  /**
   * Invoke the signal.
   */
  void signal();
}
