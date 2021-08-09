/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
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
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.tcpchannel.internal.TCPChannelMessageConstants;
import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 * Abstract superclass for all types of asynchronous channels.
 */
public abstract class AbstractAsyncChannel implements Channel {

    private static final TraceComponent tc = Tr.register(AbstractAsyncChannel.class,
                                                         TCPChannelMessageConstants.TCP_TRACE_NAME,
                                                         TCPChannelMessageConstants.TCP_BUNDLE);

    // The provider that implements the async behaviour on behalf of the channel.
    private static IAsyncProvider provider = AsyncLibrary.getInstance();

    /*
     * Marks if ths channel has already been closed
     */
    private boolean closed = false;

    /*
     * The channelTable is used to hold references to all open AsyncChannels, indexed
     * by an int index value which is stored in the AsyncChannel and is used as an indentifier
     * through which the channel can be found - eg when returning from native code on read/write
     * completion, where a direct reference is impossible.
     * The initial size is 2000 entries, but it will grow in increments of 1000 entries if more
     * AsyncChannels are open at one time.
     */
    private static LookupTable<AbstractAsyncChannel> channelTable = new LookupTable<AbstractAsyncChannel>(2000);

    /*
     * The channelIndex is an index field that can be used to pass a reference to the channel
     * - it is an index into the channelTable for this AsyncChannel
     */
    protected final int channelIndex;

    /*
     * Reference to any outstanding read/write calls for state management.
     * Only one read call and one write call can be outstanding at any one time
     */
    protected AsyncFuture readFuture = new AsyncFuture(this);
    protected AsyncFuture writeFuture = new AsyncFuture(this);

    /*
     * Each outstanding read/write call also has a (permanent for the life of an AsyncChannel) IO
     * Completion Block assigned to it which is used to exchange data with the native code when making
     * IO calls
     */
    static final int defaultBufferCount = 10;
    protected CompletionKey readIOCB = null;
    protected CompletionKey writeIOCB = null;

    protected VirtualConnection channelVCI = null;

    /*
     * Indexes to use for the readFuture and writeFuture fields
     * Futures placed in the additionalFutures table will have indexes >= 0
     */
    protected static final int READ_FUTURE_INDEX = -1;
    protected static final long READ_INDEX = ((long) READ_FUTURE_INDEX) << 32;
    protected static final int WRITE_FUTURE_INDEX = -2;
    protected static final long WRITE_INDEX = ((long) WRITE_FUTURE_INDEX) << 32;
    protected static final int SYNC_READ_FUTURE_INDEX = -3;
    protected static final long SYNC_READ_INDEX = ((long) SYNC_READ_FUTURE_INDEX) << 32;
    protected static final int SYNC_WRITE_FUTURE_INDEX = -4;
    protected static final long SYNC_WRITE_INDEX = ((long) SYNC_WRITE_FUTURE_INDEX) << 32;

    /*
     * This field contains the actual physical address of the start of data in a ByteBuffer instance.
     */
    protected static Field addrField;

    /*
     * Direct ByteBuffers have a field called "address" which holds the physical address of the start
     * of the buffer contents in native memory. This static block obtains the value of the address field
     * through reflection.
     */
    static {
        StartPrivilegedThread privThread = new StartPrivilegedThread();
        AccessController.doPrivileged(privThread);
    }

    static class StartPrivilegedThread implements PrivilegedAction<Object> {
        /**
         * Constructor.
         */
        public StartPrivilegedThread() {
            // nothing to do
        }

        @Override
        public Object run() {
            try {
                Class<?> bufClass = Class.forName("java.nio.Buffer");
                addrField = bufClass.getDeclaredField("address");
                addrField.setAccessible(true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        }
    }

    /**
     * Returns the address of the start of the given direct byte buffer in
     * OS memory.
     *
     * @param theBuffer
     *            the direct byte buffer.
     * @return the OS address as a <code>long</code>.
     * @throws IllegalArgumentException
     *             if the buffer is not direct.
     */
    static long getBufAddress(ByteBuffer theBuffer) {
        /*
         * This only works for DIRECT byte buffers. Direct ByteBuffers have a field called "address" which
         * holds the physical address of the start of the buffer contents in native memory.
         * This method obtains the value of the address field through reflection.
         */
        if (!theBuffer.isDirect()) {
            throw new IllegalArgumentException(AsyncProperties.aio_direct_buffers_only);
        }

        try {
            return addrField.getLong(theBuffer);
        } catch (IllegalAccessException exception) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Error getting async provider instance, exception: " + exception.getMessage());
            }
            FFDCFilter.processException(exception, "com.ibm.io.async.AbstractAsyncChannel", "149");
            throw new RuntimeException(exception.getMessage());
        }
    }

    /*
     * The channelIdentifier is guaranteed to be unique amongst all open channels. this is used to denote a unique reference to the
     * underlying resource in the OS. As such, many operations (like read/write) cannot be called until the channel identifier is set.
     */
    long channelIdentifier;

    // This is the object that handles completed calls for this provider.
    protected ResultHandler resultHandler;

    // The AsyncChannelGroup that this channel belongs to
    protected AsyncChannelGroup asyncChannelGroup;

    /**
     * Protected constructor ensures async channels are only created via
     * the static open() method.
     *
     * @param asyncChannelGroup
     */
    protected AbstractAsyncChannel(AsyncChannelGroup asyncChannelGroup) {
        super();

        // Create an entry in the channelTable for this AsyncChannel and store the
        // index value
        this.channelIndex = channelTable.addElement(this);
        this.asyncChannelGroup = asyncChannelGroup;
        this.resultHandler = asyncChannelGroup.getResultHandler();
        this.readFuture.setRead(true);
        this.writeFuture.setRead(false);
    }

    /**
     * Method which gets the AsyncChannel which corresponds to a supplied
     * Channel Index number.
     *
     * @param theIndex the Index number of the AsyncChannel to find.
     * @return the AsyncChannel or null if there is no Channel which matches the supplied Index
     */
    public static AbstractAsyncChannel getChannelFromIndex(int theIndex) {
        return channelTable.lookupElement(theIndex);
    }

    /**
     * Gets the Future corresponding to a supplied index value.
     *
     * @param theIndex the index value of the Future
     * @return the Future corresponding to the index number. null if there is no Future which
     *         corresponds to the supplied Index
     */
    public AsyncFuture getFutureFromIndex(int theIndex) {
        if (theIndex == READ_FUTURE_INDEX || theIndex == SYNC_READ_FUTURE_INDEX) {
            return this.readFuture;
        }
        if (theIndex == WRITE_FUTURE_INDEX || theIndex == SYNC_WRITE_FUTURE_INDEX) {
            return this.writeFuture;
        }
        return null;
    }

    /**
     * Package private version of cancel, which takes an exception as an
     * additional parameter. The exception is applied to the future on
     * cancellation.
     *
     * @param future
     * @param reason
     * @throws ClosedChannelException
     * @throws IOException
     */
    void cancel(AsyncChannelFuture future, Exception reason) throws ClosedChannelException, IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "cancel");
        }
        if (!isOpen()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Cancel request on closed connection");
            }
            future.setCancelInProgress(0);
            throw new ClosedChannelException();
        }

        // Cannot cancel a completed call.
        if (future.isCompleted()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Cancel request on completed future");
            }
            future.setCancelInProgress(0);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "cancel");
            }
            return;
        }

        long callid;
        if (((AsyncFuture) future).isRead()) {
            callid = this.readIOCB.getCallIdentifier();
        } else {
            callid = this.writeIOCB.getCallIdentifier();
        }

        int rc = provider.cancel2(this.channelIdentifier, callid);
        if (rc == 0) {
            // Mark the future completed with an Exception
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Cancel Successful, resetting CancelInProgress state to 0");
            }
            future.setCancelInProgress(0);
            future.completed(reason);
        } else {
            // could not cancel
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Cancel could not be completed");
            }
            future.setCancelInProgress(0);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "cancel");
        }
    }

    /**
     * Close the AsyncChannel.
     *
     * @throws IOException
     */
    @Override
    public synchronized void close() throws IOException {
        if (this.closed) {
            return;
        }
        // Remove the channel from the channelTable
        channelTable.removeElement(this.channelIndex);
        this.closed = true;
    }

    /**
     * Gets the async channel group used by this channel.
     *
     * @return the AsyncChannelGroup defined for this channel
     * @see AsyncChannelGroup
     */
    protected AsyncChannelGroup getAsyncChannelGroup() {
        return this.asyncChannelGroup;
    }

    /**
     * Returns whether the provider has specific capabilities.
     *
     * @param capability
     * @return boolean
     */
    final boolean isCapable(int capability) {
        return provider.hasCapability(capability);
    }

    /**
     * Performs a read or write operation.
     *
     * @param buffers -
     *            a Direct ByteBuffer array for the operation
     * @param position -
     *            a position in a file for the operation.
     * @param isRead -
     *            true for a read operation, false for a write operation
     * @return a future representing the IO operation underway
     */
    IAsyncFuture multiIO(ByteBuffer[] buffers, long position, boolean isRead, boolean forceQueue, long bytesRequested, boolean useJITBuffer, VirtualConnection vci,
                         boolean asyncIO) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "multiIO(.," + position + "," + isRead + "," + forceQueue + "," + bytesRequested + "," + useJITBuffer + ",.," + asyncIO);
        }

        // Sanity check on the arguments
        if (buffers == null) {
            throw new IllegalArgumentException();
        }

        // Allocate a future representing this operation
        AsyncFuture theFuture = null;
        CompletionKey iocb = null;

        // set it everytime, faster than doing and "if (channelVCI == null)"
        this.channelVCI = vci;

        if (isRead) {
            theFuture = this.readFuture;
            iocb = this.readIOCB;
            if (asyncIO) {
                iocb.setCallIdentifier(this.channelIndex | READ_INDEX);
            } else {
                iocb.setCallIdentifier(this.channelIndex | SYNC_READ_INDEX);
            }
        } else {
            theFuture = this.writeFuture;
            iocb = this.writeIOCB;
            if (asyncIO) {
                iocb.setCallIdentifier(this.channelIndex | WRITE_INDEX);
            } else {
                iocb.setCallIdentifier(this.channelIndex | SYNC_WRITE_INDEX);
            }
        }
        theFuture.resetFuture();
        theFuture.setBuffers(buffers);
        if (!isOpen()) {
            theFuture.completed(new ClosedChannelException());
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "multiIO", theFuture);
            }
            return theFuture;
        }

        // Prepare the buffer arrays
        int count = 0;
        for (int i = 0; i < buffers.length; i++) {
            // If the buffer array entry is null, stop processing the array
            if (buffers[i] == null)
                break;
            count++;
        }
        if (count == 0) {
            // Should always have at least one buffer, if not, its an error.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "no buffers passed on I/O call");
            }
            AsyncException exception = new AsyncException("no buffers passed on I/O call");
            FFDCFilter.processException(exception, getClass().getName(), "384", this);
            // We were asked to do no work
            theFuture.completed(exception);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "multiIO", theFuture);
            }
            return theFuture;
        } else if (count > iocb.getBufferCount()) {
            // too many buffers to fit in Completion Key, so expand it
            iocb.expandBuffers(count);
        }

        for (int i = 0; i < count; i++) {
            // Set the start address of the data in each buffer
            iocb.setBuffer((getBufAddress(buffers[i]) + buffers[i].position()),
                           buffers[i].remaining(), i);
        }

        // Make the OS call and see if it completes immediately or asyncronously.
        boolean pending = false;
        iocb.setBytesAffected(0);
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Requesting IO from native for "
                             + " call id: " + Long.toHexString(iocb.getCallIdentifier())
                             + " channel id: " + iocb.getChannelIdentifier());
            }

            iocb.preNativePrep();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "CompleteKey in AbstractAsyncChannel.multiIO before preNativePrep() call", iocb);
            }
            pending = provider.multiIO3(iocb.getAddress(), position, count, isRead, forceQueue, bytesRequested, useJITBuffer);
            iocb.postNativePrep();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "CompleteKey in AbstractAsyncChannel.multiIO after postNativePrep() call", iocb);
            }

            // For pending operations, ensure that someone is listening...
            if (pending) {
                if (vci.isInputStateTrackingOperational()) {
                    synchronized (vci.getLockObject()) {
                        if (asyncIO) {
                            // only allow closes of outstanding async reads/writes
                            if (isRead) {
                                vci.setReadStatetoCloseAllowedNoSync();
                            } else {
                                vci.setWriteStatetoCloseAllowedNoSync();
                            }
                        }

                        if (vci.getCloseWaiting()) {
                            vci.getLockObject().notify();
                        }
                    } // end-sync
                }
            } else {
                // For calls that complete immediately, complete the future
                // according to the returned data
                int rc = iocb.getReturnCode();
                if (rc == 0) {
                    // Defend against erroneous combination of values returned from native
                    if (iocb.getBytesAffected() == 0 && bytesRequested > 0) {
                        IOException ioe = new IOException("Async IO operation failed, internal error");
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Error processing multiIO request, exception: " + ioe.getMessage());
                        }
                        theFuture.completed(ioe);
                    } else {
                        // The IO operation succeeded, set the buffer position to reflect the number of bytes read/written.
                        theFuture.completed(iocb.getBytesAffected());
                    }
                } else {
                    // The operation completed immediately with an IO error.
                    theFuture.completed(AsyncLibrary.getIOException("Async IO operation failed (1), reason: ", rc));
                }
            }
        } catch (AsyncException exception) {
            // If a problem occurs invoking the async call, then the operation
            // completes immediately, with an Exception.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Error processing multiIO request, exception: " + exception.getMessage());
            }
            FFDCFilter.processException(exception, getClass().getName(), "420", this);
            theFuture.completed(exception);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "multiIO", theFuture);
        }
        return theFuture;
    }

}