/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
import java.nio.channels.ClosedChannelException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.tcpchannel.internal.TCPChannelMessageConstants;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;

/**
 * AsyncFuture provides a concrete implementation of the IAsyncFuture interface for asynchronous operations involving multiple
 * steps, such as Scatter/Gather operations.
 */
final class AsyncFuture extends AsyncChannelFuture implements IAsyncFuture {
    private static final TraceComponent tc = Tr.register(AsyncFuture.class,
                                                         TCPChannelMessageConstants.TCP_TRACE_NAME,
                                                         TCPChannelMessageConstants.TCP_BUNDLE);

    // The buffer array associated with this class.
    private ByteBuffer[] buffers;
    // Number of bytes affected by this operation, -1L if the call is incomplete.
    protected volatile long byteCount = -1L;
    // is future for a read request
    protected boolean isRead;
    protected WsByteBuffer jitBuffer = null;
    protected ByteBuffer[] jitBufferArray = new ByteBuffer[1];

    /**
     * Constructor for an AsyncFuture with a Channel.
     * 
     * @param channel
     */
    AsyncFuture(AbstractAsyncChannel channel) {
        super(channel);
    }

    void setBuffers(ByteBuffer[] buffers) {
        this.buffers = buffers;
    }

    void setJITBuffer(WsByteBuffer jitBuffer) {
        this.jitBuffer = jitBuffer;
        if (jitBuffer != null) {
            this.jitBufferArray[0] = jitBuffer.getWrappedByteBufferNonSafe();
            setBuffers(this.jitBufferArray);
        }
    }

    public WsByteBuffer getJITBuffer() {
        return this.jitBuffer;
    }

    protected void setRead(boolean isRead) {
        this.isRead = isRead;
    }

    protected boolean isRead() {
        return this.isRead;
    }

    /**
     * Marks this result as successfully complete, wakes waiting threads
     * and runs the callback. Note that the thread that invokes this
     * method runs the code in the callback function, which is arbitrarily
     * long (as it is user code).
     * 
     * @param bytesAffected a count of the number of bytes read/written
     *            by the operation
     */
    void completed(long bytesAffected) {
        boolean needToFire = true;
        synchronized (this.completedSemaphore) {
            // If it's already completed, do nothing
            if (this.completed || !this.channel.isOpen()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Future completed after already cancelled or socket was closed");
                }
                return;
            }
            // mark it completed, release sync lock, then process
            this.completed = true;

            // new timeout code - cancel timeout request if active
            if (getTimeoutWorkItem() != null) {
                getTimeoutWorkItem().state = TimerWorkItem.ENTRY_CANCELLED;
            }

            this.byteCount = bytesAffected;
            // Loop over the buffers, updating the positions until
            // we've exhausted the number of bytes affected
            long numbytes = bytesAffected;
            for (ByteBuffer buffer : this.buffers) {
                int bufspace = buffer.remaining();
                if (bytesAffected > bufspace) {
                    buffer.position(buffer.limit());
                    numbytes -= bufspace;
                } else {
                    buffer.position(buffer.position() + (int) numbytes);
                    numbytes = 0;
                    break;
                }
            } // end for

            if (this.firstListener == null) {
                // Sync Read/Write request.
                // must do this inside the sync, or else Sync Read/Write could complete
                // before we get here, and we would be doing this on the next
                // Read/Write request!
                needToFire = false;
                this.completedSemaphore.notifyAll();
            }
        }

        if (needToFire) {
            // ASync Read/Write request.
            // need to do this outside the sync, or else we will hold the sync
            // for the user's callback.
            fireCompletionActions();
        }
    } // end method completed( long )

    /**
     * Gets the byte buffer array associated with the IO operation.
     * 
     * @return The buffer array associated with the IO operation.
     */
    public ByteBuffer[] getBuffers() {
        return this.buffers;
    }

    /**
     * Returns the number of bytes read/written in the operation.
     * <p>
     * This method is intended to provide the number of bytes affected by
     * the multiple read/write operation in the case where the operation
     * failed with some type of exception. The caller should use one of
     * the getByteCount(...) methods first - if the method returns with an
     * exception, the caller can invoke readByteCount() to get the number
     * of bytes read or written before the failure occurs.
     * <p>
     * This method does not wait for the completion of the operation.
     * 
     * @return - the number of Bytes read/written in the operation. Will
     *         return -1 for an operation that is not complete.
     */
    public long readByteCount() {
        return this.byteCount;
    }

    /**
     * Returns the number of bytes read/written in the operation, or zero
     * if none. If the future is completed, the call returns immediately,
     * otherwise the call blocks until the operation completes.
     * 
     * @return The number of bytes read/written in this operation.
     * @throws InterruptedException
     *             if the calling thread was interrupted during it's
     *             wait for the operation to complete.
     * @throws IOException
     *             if the operation failed
     * @throws ClosedChannelException
     *             if the channel was closed for IO when the operation
     *             was attempted
     * @throws AsyncTimeoutException
     *             if the underlying operation timed out. (Note that
     *             this is distinct from waiting for a limited time
     *             on the future to complete).
     */
    final public long getByteCount() throws InterruptedException, IOException {
        try {
            return getByteCount(0L);
        } catch (AsyncTimeoutException e) {
            // Should only happen if the future itself is marked with an AsyncTimeoutException
            if ((this.exception != null)
                    && (this.exception instanceof AsyncTimeoutException)) {
                throw (AsyncTimeoutException) this.exception;
            } // end if
              // Cannot happen since arg of 0L means wait forever
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unexpected timeout on blocking getByteCount: "
                             + this.exception);
            }
            FFDCFilter.processException(e, getClass().getName(), "145", this);
            IOException ie = new IOException("Unexpected timeout");
            ie.initCause(e);
            throw ie;
        }
    }

    /**
     * Returns the number of bytes read/written in the operation, or zero
     * if none. If the future is completed, the call returns immediately,
     * otherwise the call blocks for at least the given number of
     * milliseconds. If the call has not completed within the timeout
     * period an <code>AsyncTimeoutException</code> is thrown.
     * 
     * @param timeout
     *            the maximum time to wait in milliseconds.
     *            Specifying 0L means wait forever.
     * @return The number of bytes read/written in this operation.
     * @throws InterruptedException
     *             if the calling thread was interrupted during
     *             it's wait for the operation to complete.
     * @throws AsyncTimeoutException
     *             if the call did not complete within the given
     *             timeout period.
     * @throws IOException
     *             if the operation failed
     * @throws ClosedChannelException
     *             if the channel was closed for IO when the
     *             operation was attempted
     */
    public long getByteCount(long timeout) throws InterruptedException, IOException {
        waitForCompletion(timeout);
        if (this.exception != null) {
            throwException();
        }
        return this.byteCount;
    }

    public void resetFuture() {
        super.resetFuture();
        // reset the JIT buffer value
        setJITBuffer(null);
    }

}
