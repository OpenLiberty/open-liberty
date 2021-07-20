/*******************************************************************************
 * Copyright (c) 2005, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.io.async;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.kernel.service.util.CpuInfo;
import com.ibm.ws.tcpchannel.internal.TCPChannelMessageConstants;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;

final class ResultHandler {

    private static final TraceComponent tc = Tr.register(ResultHandler.class,
                                                         TCPChannelMessageConstants.TCP_TRACE_NAME,
                                                         TCPChannelMessageConstants.TCP_BUNDLE);

    /** The number of handler threads currently 'active' in the system. */
    private final AtomicNumHandlersInFlight numHandlersInFlight;
    /** Total number of IO events received from native library */
    private long numItemsFromNative = 0;
    /** Number of handlers currently waiting for an event. */
    private final AtomicInteger handlersWaiting = new AtomicInteger(0);
    /** Port number used in the native library */
    private final long completionPort;
    /** Group id for tcp channels sharing this handler */
    private String myGroupID = null;
    /** IO event batch size */
    private int batchSize = 1;

    /**
     * Creates a result handler for the completion port provided, working with
     * 0..n other handlers in the given group ID.
     *
     * @param groupID
     * @param port
     */
    public ResultHandler(String groupID, long port) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "ResultHandler");
        }

        // Based on num processors (or explicit config)
        Integer maxHandlers = AsyncProperties.maxThreadsWaitingForEvents;

        if (AsyncLibrary.getInstance().hasCapability(IAsyncProvider.CAP_BATCH_IO)) {
            this.batchSize = AsyncProperties.maximumBatchedEvents;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Batch size set to: " + this.batchSize);
            }
        } else {
            this.batchSize = 1;
        }

        this.completionPort = port;
        this.myGroupID = groupID;
        this.numHandlersInFlight = new AtomicNumHandlersInFlight(maxHandlers);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "completionPort=" + port + " group=" + groupID + " maxHandlers=" + (maxHandlers == null ? CpuInfo.getAvailableProcessors().get() : maxHandlers));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "ResultHandler");
        }
    }

    /**
     * Activate the result handler when the channel starts.
     */
    public void activate() {
        // if no handlers are currently running, start one now
        if (this.numHandlersInFlight.getInt() == 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Activating result handler: " + this.completionPort);
            }
            startHandler();
        }
    }

    /**
     * Access the grouping ID field.
     *
     * @return String
     */
    protected String getGroupID() {
        return this.myGroupID;
    }

    private final void startHandler() {

        // There is a limit to the number of handlers that we will start.
        // incNumIfNotMax will either increment and return the new value,
        // or will return 0 if at max
        int updatedInFlightNum = this.numHandlersInFlight.incNumIfNotMax();

        // if we updated the inflight num we should be starting a new thread
        if (updatedInFlightNum > 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "starting another handler for completion port: " + this.completionPort);
                Tr.debug(tc, "numHandlersInFlight = " + this.numHandlersInFlight.getInt() + ", handlersWaiting = " + this.handlersWaiting.get());
            }
            AccessController.doPrivileged(new PrivilegedThreadStarter2());
        }
    }

    /**
     * Primary method that will continually check for new IO events
     * to process.
     *
     */
    public void runEventProcessingLoop() {
        final long port = this.completionPort;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "runEventProcessingLoop: " + port + " " + Thread.currentThread());
        }

        final IAsyncProvider provider = AsyncLibrary.getInstance();
        final int size = this.batchSize;
        final int jitSize = 8192;
        final int timeout = AsyncProperties.completionTimeout;
        final WsByteBufferPoolManager wsByteBufferManager = ChannelFrameworkFactory.getBufferManager();
        final boolean jitCapable = provider.hasCapability(IAsyncProvider.CAP_JIT_BUFFERS);
        final boolean batchCapable = (1 < size);

        long[] compKeyAddrs = new long[size];
        long[] jitBufferAddressBatch = new long[size];
        WsByteBuffer[] wsBBBatch = new WsByteBuffer[size];

        int keysReady = 0;
        CompletionKey completionKey = null;

        AsyncFuture future;
        int rc = 0;
        long numBytes = 0L;

        CompletionKey[] compKeys = new CompletionKey[size];
        for (int i = 0; i < size; i++) {
            compKeys[i] = (CompletionKey) AsyncLibrary.completionKeyPool.get();
            if (compKeys[i] != null) {
                // Initialize the IOCB obtained from the pool
                compKeys[i].initializePoolEntry(0, 0);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Handler got CompletionKey from Pool:" + compKeys[i]);
                }
            } else {
                compKeys[i] = new CompletionKey();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Handler newed up CompletionKey:" + compKeys[i]);
                }
            }
            compKeyAddrs[i] = compKeys[i].getAddress();
        }

        try {
            // loop until we should not do any more
            while (true) {
                // see if we are shutting down, access directly for better performance
                if (AsyncLibrary.aioInitialized == AsyncLibrary.AIO_SHUTDOWN) {
                    // leave immediately if we are shutting down
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "AIO library is shutdown, ending loop");
                    }
                    break;
                }

                future = null;
                keysReady = 0;

                this.handlersWaiting.getAndIncrement();

                // get the completion keys ready for use
                if (jitCapable) {
                    for (int i = 0; i < size; i++) {
                        // reset the completionKey for next request.  Commenting this out
                        // for performance reasons.  It is driving 4 put calls per IOCD.
                        // The native code will now reset() between calls.
                        //compKeys[i].reset();

                        // see if we need to allocate a jit buffer
                        if (jitBufferAddressBatch[i] == 0) {
                            wsBBBatch[i] = wsByteBufferManager.allocateDirect(jitSize);
                            ByteBuffer bb = wsBBBatch[i].getWrappedByteBufferNonSafe();
                            jitBufferAddressBatch[i] = AbstractAsyncChannel.getBufAddress(bb);

                            compKeys[i].setJITBufferUsed();
                            compKeys[i].setBuffer(jitBufferAddressBatch[i], jitSize, 0);
                        }
                    }
                }

                // Blocks until an async call completes, or the timeout occurs
                try {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "waiting for native event port=" + port);
                    }

                    if (batchCapable) {
                        keysReady = provider.getCompletionData3(compKeyAddrs, size, timeout, port);

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "(batch) events: " + keysReady);
                        }
                    } else {
                        boolean gotData = provider.getCompletionData2(compKeyAddrs[0], timeout, port);

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "(no batch) events: " + gotData);
                        }

                        if (gotData) {
                            keysReady = 1;

                            // Not used below...Commenting out for performance
                            //compKeys[0].setReturnStatus(1);
                        } else {
                            keysReady = 0;

                            // Not used below...Commenting out for performance
                            //compKeys[0].setReturnStatus(0);
                        }
                    }

                } catch (AsyncException exception) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Error getting IO completion event: " + exception);
                    }
                    FFDCFilter.processException(exception, getClass().getName() + ".runEventProcessingLoop", "331", this);
                } finally {
                    this.handlersWaiting.getAndDecrement();
                }

                // if we didn't get data, go back to the top of the loop and try again
                if (keysReady == 0) {
                    // make sure this completion port didn't get closed, and then AIO
                    // restarted while this thread was in the native AIO code
                    if ((AsyncLibrary.aioInitialized != AsyncLibrary.AIO_SHUTDOWN)) {
                        if (!provider.isCompletionPortValid(port)) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Completion port not valid");
                            }
                            break;
                        }
                    } else {
                        // AIO shutdown, so break out now
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "AIO Shutdown detected, break out");
                        }
                        break;
                    }

                    // Timeout path. Just loop back.
                    continue;
                } // end-no-events-received
                else if (this.handlersWaiting.get() == 0) {
                    // if we have work to process and nobody else is currently
                    // calling into the native code, start an extra handler now
                    // (if allowed)
                    startHandler();
                }

                this.numItemsFromNative += keysReady;

                for (int j = keysReady - 1; j >= 0; j--) {
                    completionKey = compKeys[j];

                    // if we get here (no timeout and no exception), we should have a
                    // valid completion key
                    completionKey.postNativePrep();
                    long callid = completionKey.getCallIdentifier();

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "batch index: " + j + " call id: " + Long.toHexString(callid) + " channel id: " + completionKey.getChannelIdentifier());
                        Tr.debug(tc, "completionKey: " + completionKey);
                    }

                    int channelIndex = (int) (callid & 0x00000000FFFFFFFF);
                    int futureIndex = (int) (callid >> 32);
                    AbstractAsyncChannel theChannel = AbstractAsyncChannel.getChannelFromIndex(channelIndex);

                    if (theChannel == null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Could not find channel, possibly closed " + completionKey);
                        }
                        continue;
                    }

                    future = theChannel.getFutureFromIndex(futureIndex);

                    if (future == null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Completion event could not find future " + completionKey);
                        }
                        AsyncException exception = new AsyncException("Future not found");
                        FFDCFilter.processException(exception, getClass().getName() + ".runEventProcessingLoop", "142", this);
                        continue;
                    }

                    if (jitCapable && completionKey.wasJITBufferUsed()) {
                        // set the JIT buffer value in the future
                        future.setJITBuffer(wsBBBatch[j]);
                        jitBufferAddressBatch[j] = 0;
                        // reset so new jit buffer will be allocated for next call
                    } else {
                        future.setJITBuffer(null);
                    }
                    rc = completionKey.getReturnCode();
                    numBytes = completionKey.getBytesAffected();

                    // check the event to see if it is for synchronous request
                    if (futureIndex == AbstractAsyncChannel.SYNC_READ_FUTURE_INDEX || futureIndex == AbstractAsyncChannel.SYNC_WRITE_FUTURE_INDEX) {
                        // notify waiting sync request

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Processing Sync Request rc: " + rc);
                        }

                        if (rc == 0) {
                            // Defend against erroneous combination of values returned from
                            // native
                            if (numBytes == 0L) {
                                IOException ioe = new IOException("Async IO operation failed, internal error");
                                future.completed(ioe);
                                continue;
                            }
                            // Mark the future as completed
                            future.setCancelInProgress(0);
                            future.completed(numBytes);
                        } else {
                            if (future.getCancelInProgress() == 1) {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "Cancel is in progress, ignoring");
                                }
                                future.setCancelInProgress(0);
                            } else {
                                future.completed(AsyncLibrary.getIOException("Async IO operation failed (3), reason: ", rc));
                            }
                        }
                        continue; // sync response has been processed
                    }

                    // signal the IO completion
                    // Check to see if the IO operation succeeded
                    if (rc == 0) {
                        future.setCancelInProgress(0);
                        future.completed(numBytes);
                    } else {
                        if (future.getCancelInProgress() == 1) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Cancel is in progress, ignoring");
                            }
                            future.setCancelInProgress(0);
                        } else {
                            future.completed(AsyncLibrary.getIOException("Async IO operation failed (2), reason: ", rc));
                        }
                    }

                } // end the batch loop
            } // end of "while (true) {"
        } catch (Throwable t) {
            if (AsyncLibrary.aioInitialized != AsyncLibrary.AIO_SHUTDOWN) {
                FFDCFilter.processException(t, getClass().getName() + ".runEventProcessingLoop", "792", this);
                throw new RuntimeException(t);
            }
        } finally {

            // Decrement the number of in flight handlers, since this one is exiting
            this.numHandlersInFlight.decrementInt();

            // The completion processing thread is being shut down or failed
            // if jit buffer was allocated, release it
            for (int k = 0; k < size; k++) {
                if (jitBufferAddressBatch[k] != 0) {
                    wsBBBatch[k].release();
                }
            }

            // Put the CompletionKeys into the pool for reuse.   They contain a DirectByteBuffer (WsByteBuffer).
            for (int i = 0; i < size; i++) {
                if (compKeys[i] != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Handler ending, pooling CompletionKey:\n" + compKeys[i]);
                    }
                    AsyncLibrary.completionKeyPool.put(compKeys[i]);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "runEventProcessingLoop: " + port + " " + Thread.currentThread());
        }
    }

    protected void dumpStatistics() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "   Number of handlers: " + this.numHandlersInFlight.getInt());
            Tr.event(tc, "   Number of IO events received: " + this.numItemsFromNative);
        }
    }

    /**
     * Wrapper class for the event processing loop. Creating a runnable
     * like this results in multiple threads sharing the same actual
     * handler instance and variables.
     */
    Runnable getProcessCompletionEventTask() {
        return new Runnable() {
            @Override
            public void run() {
                runEventProcessingLoop();
            }

        };
    }

    class PrivilegedThreadStarter2 implements PrivilegedAction<Object> {
        /** Constructor */
        public PrivilegedThreadStarter2() {
            // do nothing
        }

        @Override
        public Object run() {
            String threadName = "Completion Processing Thread for group: " + getGroupID();
            Thread newThread = new Thread(getProcessCompletionEventTask());
            newThread.setName(threadName);

            // all TCPChannel Thread should be daemon threads
            newThread.setDaemon(true);
            newThread.start();
            return null;
        }
    }

    /**
     * This class implements update with saturation by using java.util.concurrent.
     */
    final static class AtomicNumHandlersInFlight {
        private final AtomicInteger myNumHandlersInFlight;
        private int myMaxHandlers;
        private final boolean recalculate;

        /**
         * Constructor.
         *
         * @param max
         */
        AtomicNumHandlersInFlight(Integer max) {
            this.myNumHandlersInFlight = new AtomicInteger(0);
            if (max != null) {
                this.myMaxHandlers = max.intValue();
            }
            recalculate = (max == null ? true : false);
        }

        /**
         * Query the current number of handlers in use.
         *
         * @return int
         */
        int getInt() {
            return this.myNumHandlersInFlight.get();
        }

        /**
         * Atomically increment the number of handlers being used, only if
         * that count is under the configurated maximum allowed.
         *
         * @return int (0 means no change, 1 means value was updated)
         */
        int incNumIfNotMax() {
            int ret = 0;
            boolean succ = false;
            do {
                int val = this.myNumHandlersInFlight.get();
                int check = recalculate ? CpuInfo.getAvailableProcessors().get() : myMaxHandlers;
                if (val < check) {
                    int newval = val + 1;
                    succ = this.myNumHandlersInFlight.weakCompareAndSet(val, newval);
                    if (succ) {
                        ret = newval;
                    }
                } else {
                    succ = true; // numHandlersInFlight >= maxHandlers
                }
            } while (!succ);
            return ret;
        }

        /**
         * Atomically decrement numHandlersInFlight.
         */
        void decrementInt() {
            boolean succ = false;
            do {
                int val = this.myNumHandlersInFlight.get();
                int newval = val - 1;
                succ = this.myNumHandlersInFlight.weakCompareAndSet(val, newval);
            } while (!succ);
        }

    }

}