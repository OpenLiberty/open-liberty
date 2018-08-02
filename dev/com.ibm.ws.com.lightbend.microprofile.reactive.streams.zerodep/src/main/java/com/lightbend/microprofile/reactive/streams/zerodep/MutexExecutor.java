/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.microprofile.reactive.streams.zerodep;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Executor that provides mutual exclusion between the operations submitted to it.
 *
 * All operations are delegated to the wrapped executor, however only one operation
 * at a time will be submitted to that executor. The queuing of operations is done
 * in a non blocking fashion.
 */
final class MutexExecutor implements Executor {
  private final Executor delegate;
  private final AtomicReference<RunNode> last = new AtomicReference<>();

  MutexExecutor(Executor delegate) {
    this.delegate = delegate;
  }

  @Override
  public void execute(final Runnable command) {
    final RunNode newNode = new RunNode(Objects.requireNonNull(command, "Runnable must not be null"));
    final RunNode prevLast = last.getAndSet(newNode);
    if (prevLast != null)
      prevLast.lazySet(newNode);
    else
      delegate.execute(() -> runAll(newNode));
  }

  protected void reportFailure(final Thread runner, final Runnable thrower, final Throwable thrown) {
    if (thrown instanceof InterruptedException) {
      // TODO: Current task was interrupted, set interrupted flag and proceed is a valid strategy?
      runner.interrupt();
    } else { // TODO: complement the most appropriate way of dealing with fatal Throwables
      final Thread.UncaughtExceptionHandler ueh = runner.getUncaughtExceptionHandler();
      if (ueh != null)
        ueh.uncaughtException(runner, thrown);
      else thrown.printStackTrace();
      // TODO: Rethrow or something else? Is there a sensible fallback here?
    }
  }

  // Runs a single RunNode and deals with any Throwables it throws
  private final void run(final RunNode current) {
    try { current.runnable.run(); } catch (final Throwable thrown) {
      reportFailure(Thread.currentThread(), current.runnable, thrown);
    }
  }

  // Runs all the RunNodes starting with `next`
  private final void runAll(RunNode next) {
    for(;;) {
      final RunNode current = next;
      run(current);
      if ((next = current.get()) == null) { // try advance, if we get null test
        if (last.compareAndSet(current, null)) return; // end-of-queue: we're done.
        else while((next = current.get()) == null)/* Thread.onSpinWait() */; // try advance until next is visible.
      }
    }
  }

  private static class RunNode extends AtomicReference<RunNode> {
    final Runnable runnable;
    RunNode(final Runnable runnable) {
      this.runnable = runnable;
    }
  }
}