/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.microprofile.reactive.streams.zerodep;

/**
 * An inlet that a stage may interact with.
 *
 * @param <T> The type of signal this stage deals with.
 */
interface StageInlet<T> {

  /**
   * Send a pull signal to this inlet. This will allow an upstream stage to push an element.
   * <p>
   * The inlet may only be pulled if it is not closed and hasn't already been pulled since it last received an element.
   */
  void pull();

  /**
   * Whether this inlet has been pulled.
   */
  boolean isPulled();

  /**
   * Whether this inlet is available to be grabbed.
   */
  boolean isAvailable();

  /**
   * Whether this inlet has been closed, either due to it being explicitly cancelled, or due to an
   * upstream finish or failure being received.
   */
  boolean isClosed();

  /**
   * Cancel this inlet. No signals may be sent after this is invoked, and no signals will be received.
   */
  void cancel();

  /**
   * Grab the last pushed element from this inlet.
   * <p>
   * Grabbing the element will cause it to be removed from the inlet - an element cannot be grabbed twice.
   * <p>
   * This may only be invoked if a prior {@link InletListener#onPush()} signal has been received.
   *
   * @return The grabbed element.
   */
  T grab();

  /**
   * Set the listener for signals from this inlet.
   *
   * @param listener The listener.
   */
  void setListener(InletListener listener);

  /**
   * Convenience method for configuring an inlet to simply forward all signals to an outlet.
   *
   * @param outlet The outlet to forward signals to.
   */
  default void forwardTo(StageOutlet<T> outlet) {
    class ForwardingInletListener implements InletListener {
      @Override
      public void onPush() {
        outlet.push(grab());
      }

      @Override
      public void onUpstreamFinish() {
        outlet.complete();
      }

      @Override
      public void onUpstreamFailure(Throwable error) {
        outlet.fail(error);
      }
    }
    setListener(new ForwardingInletListener());
  }
}

/**
 * A listener for signals to an inlet.
 */
interface InletListener {

  /**
   * Indicates that an element has been pushed. The element can be received using {@link StageInlet#grab()}.
   * <p>
   * If this throws an exception, the error will be passed to {@link #onUpstreamFailure(Throwable)}, anything upstream
   * from this inlet will be cancelled, and the stage listening will not receive any further signals.
   */
  void onPush();

  /**
   * Indicates that upstream has completed the stream. Unless this throws an exception, no signals may be sent to the
   * inlet after this has been invoked.
   * <p>
   * If this throws an exception, the error will be passed to {@link #onUpstreamFailure(Throwable)}, anything upstream
   * from this inlet will be cancelled, and the stage will not receive any further signals. Stages should be careful
   * to ensure that if they do throw from this method, that they are ready to receive that exception from
   * {@code onUpstreamFailure}.
   */
  void onUpstreamFinish();

  /**
   * Indicates that upstream has completed the stream with a failure. Once this has been invoked, no other signals
   * will be sent to this listener.
   * <p>
   * If this throws an exception, the entire stream will be shut down, since there's no other way to guarantee that
   * the failure signal will be propagated downstream. Hence, stages should not throw exceptions from this method,
   * particularly exceptions from user supplied callbacks, as such errors will not be recoverable (eg, a recover stage
   * won't be able to resume the stream).
   */
  void onUpstreamFailure(Throwable error);
}