/*
 * Copyright 2019 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.grpc.servlet;

import static io.grpc.servlet.ServletServerStream.toHexString;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.SEVERE;

import com.ibm.websphere.ras.annotation.Trivial;

import io.grpc.InternalLogId;
import io.grpc.Status;
import io.grpc.servlet.ServletServerStream.ServletTransportState;
import java.io.IOException;
import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.logging.Logger;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;

/** Handles write actions from the container thread and the application thread. */
@Trivial
final class AsyncServletOutputStreamWriter {

  private static final Logger logger =
      Logger.getLogger(AsyncServletOutputStreamWriter.class.getName());

  /**
   * Memory boundary for write actions.
   *
   * <pre>
   * WriteState curState = writeState.get();  // mark a boundary
   * doSomething();  // do something within the boundary
   * boolean successful = writeState.compareAndSet(curState, newState); // try to mark a boundary
   * if (successful) {
   *   // state has not changed since
   *   return;
   * } else {
   *   // state is changed by another thread while doSomething(), need recompute
   * }
   * </pre>
   *
   * <p>There are two threads, the container thread (calling {@code onWritePossible()}) and the
   * application thread (calling {@code runOrBuffer()}) that read and update the
   * writeState. Only onWritePossible() may turn {@code readyAndDrained} from false to true, and
   * only runOrBuffer() may turn it from true to false.
   */
  private final AtomicReference<WriteState> writeState = new AtomicReference<>(WriteState.DEFAULT);

  private final ServletOutputStream outputStream;
  private final ServletTransportState transportState;
  private final InternalLogId logId;
  private final BiFunction<byte[], Integer, ActionItem> writeAction;
  private final ActionItem flushAction;
  private final ActionItem completeAction;
  private final BooleanSupplier isReady;

  /**
   * New write actions will be buffered into this queue if the servlet output stream is not ready or
   * the queue is not drained.
   */
  // SPSC queue would do
  private final Queue<ActionItem> writeChain = new ConcurrentLinkedQueue<>();
  // for a theoretical race condition that onWritePossible() is called immediately after isReady()
  // returns false and before writeState.compareAndSet()
  @Nullable
  private volatile Thread parkingThread;

  AsyncServletOutputStreamWriter(
      AsyncContext asyncContext,
      ServletOutputStream outputStream,
      ServletTransportState transportState,
      InternalLogId logId) {

    this.transportState = transportState;
    this.logId = logId;
    this.outputStream = outputStream;
    
    Logger logger = Logger.getLogger(AsyncServletOutputStreamWriter.class.getName());
   
    this.writeAction = (byte[] bytes, Integer numBytes) -> () -> {
        outputStream.write(bytes, 0, numBytes);
        transportState.runOnTransportThread(() -> transportState.onSentBytes(numBytes));
        if (logger.isLoggable(FINEST)) {
            logger.log(
                FINEST,
                "[{0}] outbound data: length = {1}, bytes = {2}",
                new Object[]{logId, numBytes, toHexString(bytes, numBytes)});
        }
      };
      
    this.flushAction = () -> {
      logger.log(FINEST, "[{0}] flushBuffer", logId);
      asyncContext.getResponse().flushBuffer();
    };
    this.completeAction = () -> {
      logger.log(FINE, "[{0}] call is completing", logId);
      transportState.runOnTransportThread(
          () -> {
            transportState.complete();
            asyncContext.complete();
            logger.log(FINE, "[{0}] call completed", logId);
          });
    };
    
    this.isReady = () -> outputStream.isReady();

  };
    
  /** Called from application thread. */
  void writeBytes(byte[] bytes, int numBytes) throws IOException {
	runOrBuffer(writeAction.apply(bytes, numBytes));
  }

  /** Called from application thread. */
  void flush() throws IOException {
    runOrBuffer(flushAction);
  }

  /** Called from application thread. */
  void complete() {
    try {
      runOrBuffer(completeAction);
    } catch (IOException e) {
      // actually completeAction does not throw
      throw Status.fromThrowable(e).asRuntimeException();
    }
  }

  /** Called from the container thread {@link javax.servlet.WriteListener#onWritePossible()}. */
  void onWritePossible() throws IOException {
    logger.log(
        FINEST, "[{0}] onWritePossible: ENTRY. The servlet output stream becomes ready", logId);
    assureReadyAndDrainedTurnsFalse();
    while (isReady.getAsBoolean()) {
      WriteState curState = writeState.get();

      ActionItem actionItem = writeChain.poll();
      if (actionItem != null) {
        actionItem.run();
        continue;
      }

      if (writeState.compareAndSet(curState, curState.withReadyAndDrained(true))) {
        // state has not changed since.
        logger.log(
            FINEST,
            "[{0}] onWritePossible: EXIT. All data available now is sent out and the servlet output"
                + " stream is still ready",
            logId);
        return;
      }
      // else, state changed by another thread (runOrBuffer), need to drain the writeChain
      // again
    }
    logger.log(
        FINEST, "[{0}] onWritePossible: EXIT. The servlet output stream becomes not ready", logId);
  }
  
  private void assureReadyAndDrainedTurnsFalse() {
	    // readyAndDrained should have been set to false already.
	    // Just in case due to a race condition readyAndDrained is still true at this moment and is
	    // being set to false by runOrBuffer() concurrently.
	    while (writeState.get().readyAndDrained) {
	      parkingThread = Thread.currentThread();
	      // Try to sleep for an extremely long time to avoid writeState being changed at exactly
	      // the time when sleep time expires (in extreme scenario, such as #9917).
	      LockSupport.parkNanos(Duration.ofHours(1).toNanos()); // should return immediately
	    }
	    parkingThread = null;
  }
  
  /**
   * Either execute the write action directly, or buffer the action and let the container thread
   * drain it.
   *
   * <p>Called from application thread.
   */
  private void runOrBuffer(ActionItem actionItem) throws IOException {
    WriteState curState = writeState.get();
    // TODO: Liberty currently needs to check isReady() BEFORE doing the write, as it puts context on the
    // thread during that call.  We should get rid of that requirement.
    if (curState.readyAndDrained && outputStream.isReady()) { // write to the outputStream directly
      actionItem.run();
      if (!outputStream.isReady()) {
        logger.log(FINEST, "[{0}] the servlet output stream becomes not ready", logId);
        boolean successful = writeState.compareAndSet(curState, curState.withReadyAndDrained(false));
        assert successful;
        LockSupport.unpark(parkingThread);
      }
    } else { // buffer to the writeChain
      writeChain.offer(actionItem);
      if (!writeState.compareAndSet(curState, curState.withReadyAndDrained(false))) {
        checkstate(
            writeState.get().readyAndDrained,
            "Bug: onWritePossible() should have changed readyAndDrained to true, but not");
        ActionItem lastItem = writeChain.poll();
        if (lastItem != null) {
          checkstate(lastItem == actionItem, "Bug: lastItem != actionItem");
          runOrBuffer(lastItem);
        }
      } // state has not changed since
    }
  }

  private void checkstate(Boolean condition, String msg) {
	  if(! condition ) {
		  logger.log(SEVERE, "assert failed: " +  msg);
	  }
	  assert( condition );
  }

  /** Write actions, e.g. writeBytes, flush, complete. */
  @FunctionalInterface
  private interface ActionItem {
    void run() throws IOException;
  }

  interface Log {
    default void fine(String str, Object...params) {}
    default void finest(String str, Object...params) {}
  }

  private static final class WriteState {

    static final WriteState DEFAULT = new WriteState(false);

    /**
     * The servlet output stream is ready and the writeChain is empty.
     *
     * <p>readyAndDrained turns from false to true when:
     * {@code onWritePossible()} exits while currently there is no more data to write, but the last
     * check of {@link javax.servlet.ServletOutputStream#isReady()} is true.
     *
     * <p>readyAndDrained turns from true to false when:
     * {@code runOrBuffer()} exits while either the action item is written directly to the
     * servlet output stream and the check of {@link javax.servlet.ServletOutputStream#isReady()}
     * right after that returns false, or the action item is buffered into the writeChain.
     */
    final boolean readyAndDrained;

    WriteState(boolean readyAndDrained) {
      this.readyAndDrained = readyAndDrained;
    }

    /**
     * Only {@code onWritePossible()} can set readyAndDrained to true, and only {@code
     * runOrBuffer()} can set it to false.
     */
    @CheckReturnValue
    WriteState withReadyAndDrained(boolean readyAndDrained) {
      return new WriteState(readyAndDrained);
    }
  }
}
