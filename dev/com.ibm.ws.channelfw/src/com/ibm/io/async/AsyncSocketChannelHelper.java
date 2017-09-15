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

import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.tcpchannel.internal.TCPChannelMessageConstants;
import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 * A class that provides utility functions for an {@link com.ibm.io.async.AsyncSocketChannel}.
 * The <code>AsyncSocketChannelHelper</code>
 * wraps an existing <code>AsyncSocketChannel</code>.
 * <p>
 * One class of functions provided are scatter/gather functions (also
 * called "multi IO") for reading/writing data held in arrays of buffers.
 * On some operating systems the scatter/gather routines are implemented
 * directly by the operating system, whereas on others the routines are
 * emulated by the Java implementation making multiple OS calls.
 * </p>
 * <p>
 * Another class of functions provided are versions of the channel read/write
 * operations that have timeouts. At the operating system level, there are
 * no timeout functions available for asynchronous I/O operations and it is
 * possible for asynchronous read or write operations to wait forever.
 * The <code>AsyncSocketChannelHelper</code> provides timeout capabilities
 * for these operations, which are achieved by layering a timeout
 * mechanism on top of the underlying I/O functions.
 * </p>
 * <p>Operations which timeout are cancelled. Cancellation leaves the
 * channel in an indeterminate state, since it is impossible to know
 * whether the operation had already started or not when the cancellation
 * happened. As a result, the only practical thing to do with a channel
 * which has had a timeout is to <code>Close</code> the channel.
 * </p>
 */
public class AsyncSocketChannelHelper {

    protected static final TraceComponent tc = Tr.register(AsyncSocketChannelHelper.class,
                                                           TCPChannelMessageConstants.TCP_TRACE_NAME,
                                                           TCPChannelMessageConstants.TCP_BUNDLE);

    protected static Timer timer = null;
    protected static AsyncTimeoutException timeoutException = null;

    private TimerCallback callback = new tCallback();
    final protected AsyncSocketChannel schannel;
    final protected boolean providerIsMultiCapable;

    static {
        AccessController.doPrivileged(new PrivStartTimer());
    }

    static class PrivStartTimer implements PrivilegedAction<Object> {
        /** Constructor */
        public PrivStartTimer() {
            // nothing to do
        }

        public Object run() {
            timer = new Timer();
            return null;
        }
    }

    private static class tCallback implements TimerCallback {
        protected tCallback() {
            // nothing to do
        }

        public void timerTriggered(TimerWorkItem twi) {
            AbstractAsyncFuture theFuture = (AbstractAsyncFuture) twi.attachment;
            // Note: the JNI cancel call is on this Timer thread but the
            // final "completed" call into user code is not
            try {
                synchronized (theFuture.getCompletedSemaphore()) {
                    if (!theFuture.isCompleted()) {

                        // If the future just completed, and then a new operation
                        // started before we could grab the completed lock, then
                        // we would errantly time out the new operation here. So
                        // check here that we are not doing that.
                        if (theFuture.getReuseCount() != twi.futureCount) {
                            return;
                        }

                        // init/use a static exception here, it will have the
                        // same stack every time anyway
                        if (timeoutException == null) {
                            timeoutException = new AsyncTimeoutException();
                        }
                        // Cancel call will mark the future completed
                        // Attempt to cancel operation at the native layer

                        theFuture.setCancelInProgress(1);
                        theFuture.cancel(timeoutException);
                    }
                }
            } catch (Exception e) {
                // Any exceptions generated can only be thrown away
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Error in timerTriggered while trying to timeout an operation. exception: " + e.getMessage());
                }
                FFDCFilter.processException(e, getClass().getName(), "98", this);
            }
        }
    }

    /**
     * Creates an AsyncChannelHelper for an AsyncSocketChannel
     * 
     * @param channel
     */
    public AsyncSocketChannelHelper(AsyncSocketChannel channel) {
        super();
        if (channel == null) {
            throw new IllegalArgumentException();
        }

        this.schannel = channel;
        this.providerIsMultiCapable = channel.isCapable(IAsyncProvider.CAP_MULTI_SOCKET);
    }

    /**
     * Requests an asynchronous read from an <code>AsyncSocketChannel</code>
     * to an array of direct ByteBuffers.
     * <p>
     * The byte buffers in the array must be direct byte buffers created using {@link java.nio.ByteBuffer#allocateDirect(int)}. The array of byte
     * buffers can contain <code>null</code> entries. Processing of the
     * array will terminate at the first <code>null</code> entry.
     * Any entries after the <code>null</code> entry are ignored.
     * </p>
     * <p>
     * The method returns immediately with a <em>future</em> object.
     * Applications can invoke methods on the future object to determine the
     * completion status of the read, including registering a listener to be
     * called back when the operation completes. The completed future
     * also reports the total number of bytes read, or if the read failed,
     * the reason for failure as an <code>Exception</code>.
     * </p>
     * <p>
     * The asynchronous IO subsystem retains logical control of the array of
     * byte buffers until the read completes. Clients should not attempt to
     * read or modify the byte buffers until the system indicates that the
     * operation is finished by marking the future completed.
     * </p>
     * <p>
     * Only one read operation can be in progress on this channel at a time.
     * No further reads will succeed on this channel until this read
     * completes. Attempts to read on the channel before an outstanding read
     * has completed will return a completed future with an
     * <code>IOPendingException</code>.
     * </p>
     * <p>
     * The read operation uses the array of byte buffers as follows:
     * <ul>
     * <li>Starting with the first byte buffer in the array, data is read
     * into the buffer starting at the buffer's initial <code>buffer.position()
     * </code>.</li>
     * <li>A maximum of <code>buffer.remaining()</code> bytes are read into
     * the buffer.</li>
     * <li>The buffer's <code>buffer.position()</code> is incremented by the
     * number of bytes read.</li>
     * <li>The buffer's limit is left unchanged.</li>
     * <li>This is repeated for each buffer in turn, either until all the
     * data available from the channel has been read or until all the buffers
     * have been filled.</li>
     * </ul>
     * </p>
     * <p>
     * If the operation fails in some way, the future will return an
     * Exception. Some of the byte buffers may get modified in these
     * circumstances. It is not possible to be sure that a failing operation
     * will not change the contents of some of the buffers.
     * </p>
     * <p>
     * Example usage is:
     * 
     * <pre>
     * ...
     * ByteBuffer[] bufarray = new ByteBuffer[MAXBUFS];
     * for( int i = 0, i < MAXBUFS; i++ ) {
     * bufarray[i] = ByteBuffer.allocateDirect(1024);
     * }
     * IAsyncFuture readFuture = channel.read(bufarray);
     * ... &lt;do some useful work&gt;...
     * try {
     * // bytesRead is the total number of bytes read across all buffers
     * long bytesRead = readFuture.getByteCount();
     * ...
     * } catch (AsyncException ex) {
     * ... the IO operation failed...
     * }
     * </pre>
     * 
     * </p>
     * 
     * @param bufs
     *            the array of <code>ByteBuffers</code> to read into.
     *            The array must be non-null.
     *            The first entry in the array must be non-null.
     *            The second or subsequent entries in the array may be null. The read operation uses all of the entries in the
     *            array before the first null entry. Any entries after the first null entry are ignored.
     * @param forceQueue
     * @param bytesRequested
     * @param useJITBuffer
     * @param vci
     * @param asyncIO
     * @return an <code>IAsyncFuture</code> which acts as a placeholder for the asynchronous operation
     * @throws IllegalArgumentException if the preconditions on the bufs parameter are not met
     * @see IAsyncFuture
     */
    public IAsyncFuture read(ByteBuffer[] bufs, boolean forceQueue,
                             long bytesRequested, boolean useJITBuffer, VirtualConnection vci,
                             boolean asyncIO) {
        return this.schannel.multiIO(bufs, 0, true, forceQueue,
                                     bytesRequested, useJITBuffer, vci, asyncIO);
    }

    /**
     * Requests an asynchronous read from an <code>AsyncSocketChannel</code>
     * to an array of direct ByteBuffers with a specified timeout.
     * <p>
     * The byte buffers in the array must be direct byte buffers created
     * using {@link java.nio.ByteBuffer#allocateDirect(int)}. The array
     * of byte buffers can contain <code>null</code> entries. Processing
     * of the array will terminate at the first <code>null</code> entry.
     * Any entries after the <code>null</code> entry are ignored.
     * </p>
     * <p>
     * The method returns immediately with a <em>future</em> object.
     * Applications can invoke methods on the future object to determine the
     * completion status of the read, including registering a listener to be
     * called back when the operation completes. The completed future
     * also reports the total number of bytes read, or if the read failed,
     * the reason for failure as an <code>Exception</code>. If the read does
     * not complete before the specified timeout period, then the future is
     * marked as complete with an <code>AsyncTimeoutException</code>
     * and the operation is cancelled.
     * </p>
     * <p>
     * The asynchronous IO subsystem retains logical control of the array
     * of byte buffers until the read completes. Clients should not attempt
     * to read or modify the byte buffers until the system indicates that
     * the operation is finished by marking the future completed.
     * </p>
     * <p>
     * Only one read operation can be in progress on this channel at a time.
     * No further reads will succeed on this channel until this read
     * completes. Attempts to read on the channel before an outstanding
     * read has completed will return a completed future with an <code>
     * IOPendingException</code>.
     * </p>
     * <p>
     * The read operation uses the array of byte buffers as follows:
     * <ul>
     * <li>Starting with the first byte buffer in the array, data is read
     * into the buffer starting at the buffer's initial <code>buffer.position()
     * </code>.</li>
     * <li>A maximum of <code>buffer.remaining()</code> bytes are read into
     * the buffer.</li>
     * <li>The buffer's <code>buffer.position()</code> is incremented by the
     * number of bytes read.</li>
     * <li>The buffer's limit is left unchanged.</li>
     * <li>This is repeated for each buffer in turn, either until all the
     * data available from the channel has been read or until all the
     * buffers have been filled.</li>
     * </ul>
     * </p>
     * <p>
     * If the operation fails the future will return an <code>Exception
     * </code>. Some of the byte buffers may get modified in these
     * circumstances.
     * </p>
     * <p>
     * Example usage is:
     * 
     * <pre>
     * ...
     * ByteBuffer[] bufarray = new ByteBuffer[MAXBUFS];
     * for( int i = 0, i < MAXBUFS; i++ ) {
     * bufarray[i] = ByteBuffer.allocateDirect(1024);
     * }
     * // Request that the multi read completes in 10 seconds
     * IAsyncFuture readFuture = channel.read(bufarray, 10000);
     * ... &lt;do some useful work&gt;...
     * try {
     * // bytesRead is the total number of bytes read across all buffers
     * long bytesRead = readFuture.getByteCount();
     * ...
     * } catch (AsyncTimeoutException ex) {
     * ... the IO operation timed out...
     * } catch (AsyncException ex) {
     * ... the IO operation failed...
     * }
     * </pre>
     * 
     * </p>
     * 
     * @param bufs the array of <code>ByteBuffers</code> to read into.
     *            The array must be non-null.
     *            The first entry in the array must be non-null.
     *            The second or subsequent entries in the array may be null. The read operation uses all of the entries in the
     *            array before the first null entry. Any entries after the first null entry are ignored.
     * @param timeout
     *            The timeout in milliseconds.
     * @param forceQueue
     * @param bytesRequested
     * @param useJITBuffer
     * @param vci
     * @param asyncIO
     * @return an <code>IAsyncFuture</code> representing the requested read operation.
     * @throws IllegalArgumentException if the preconditions on the bufs parameter are not met.
     * 
     * @see IAsyncFuture
     */
    public IAsyncFuture read(ByteBuffer[] bufs, long timeout,
                             boolean forceQueue, long bytesRequested, boolean useJITBuffer,
                             VirtualConnection vci, boolean asyncIO) {
        if (timeout < 0) {
            throw new IllegalArgumentException();
        }
        IAsyncFuture future = read(bufs, forceQueue, bytesRequested,
                                   useJITBuffer, vci, asyncIO);

        // don't lock the getCompletedSemaphore here, wait till later
        if (future != null && !future.isCompleted() && timeout > 0) {
            createTimeout(future, timeout, true);
        }
        return future;
    }

    /**
     * Requests an asynchronous write from an array of direct ByteBuffers to
     * the <code>AsyncSocketChannel</code> with a specified timeout.
     * <p>
     * The byte buffers in the array must be direct byte buffers created
     * using {@link java.nio.ByteBuffer#allocateDirect(int)}. The array
     * of byte buffers can contain <code>null</code> entries. Processing
     * of the array will terminate at the first <code>null</code> entry.
     * Any entries after the <code>null</code> entry are ignored.
     * </p>
     * <p>
     * The method returns immediately with a <em>future</em> object.
     * Applications can invoke methods on the future object to determine the
     * completion status of the write, including registering a listener to
     * be called back when the operation completes. The completed future
     * also reports the total number of bytes written, or if the write
     * failed, the reason for failure as an <code>Exception</code>. If the
     * write does not complete before the specified timeout period, then
     * the future is marked as complete with an <code>AsyncTimeoutException</code>
     * and the operation is cancelled.
     * </p>
     * <p>
     * The asynchronous IO subsystem retains logical control of the array of
     * byte buffers until the write completes. Clients should not attempt to
     * read or modify the byte buffers until the system indicates that the
     * operation is finished by marking the future completed.
     * </p>
     * <p>
     * Only one write operation can be in progress on this channel at a time.
     * No further writes will succeed on this channel until this write
     * completes. Attempts to write on the channel before an outstanding write
     * has completed will return a completed future with an <code>IOPendingException</code>.
     * </p>
     * <p>
     * The write operation uses the array of byte buffers as follows:
     * <ul>
     * <li>Starting with the first byte buffer in the array, data is written
     * from the buffer starting at the buffer's initial <code>buffer.position()
     * </code>.</li>
     * <li>A maximum of <code>buffer.remaining()</code> bytes are written to
     * the channel.</li>
     * <li>The buffer's <code>buffer.position()</code> is incremented by the
     * number of bytes successfully written.</li>
     * <li>The buffer's limit is left unchanged.</li>
     * <li>This is repeated for each buffer in turn, either until the channel
     * is unable to take any more data or until the data in all the buffers
     * has been written to the channel.</li>
     * </ul>
     * </p>
     * <p>
     * If the operation fails in some way, the future will return an
     * Exception. Some of the byte buffers may get modified in these
     * circumstances. It is not possible to be sure that a failing operation
     * will not change the contents of some of the buffers.
     * </p>
     * <p>
     * Example usage:
     * 
     * <pre>
     * ...
     * ByteBuffer[] bufarray = new ByteBuffer[MAXBUFS];
     * for( int i = 0, i < MAXBUFS; i++ ) {
     * bufarray[i] = ByteBuffer.allocateDirect(1024);
     * buffer.put("Some data....".getBytes());
     * buffer.flip();
     * }
     * // Request that the multi write completes in 10 seconds
     * IAsyncFuture writeFuture = channel.write(bufarray, 10000);
     * ... &lt;do some useful work&gt;...
     * try {
     * // bytesWritten is the total number of bytes written across all buffers
     * long bytesWritten = writeFuture.getByteCount();
     * ...
     * } catch (AsyncTimeoutException ex) {
     * ... the IO operation timed out...
     * } catch (AsyncException ex) {
     * ... the IO operation failed...
     * }
     * </pre>
     * 
     * </p>
     * 
     * @param bufs
     *            the array of <code>ByteBuffers</code> to write from.
     *            The array must be non-null.
     *            The first entry in the array must be non-null.
     *            The second or subsequent entries in the array may be null. The write operation uses all of the entries in the
     *            array before the first null entry. Any entries after the first null entry are ignored.
     * @param timeout
     *            The timeout in milliseconds.
     * @param forceQueue
     * @param bytesRequested
     * @param vci
     * @param asyncIO
     * @return an <code>IAsyncFuture</code> which acts as a placeholder for the asynchronous operation.
     * @throws IllegalArgumentException if the preconditions on the bufs parameter are not met.
     * @see IAsyncFuture
     */
    public IAsyncFuture write(ByteBuffer[] bufs, long timeout,
                              boolean forceQueue, long bytesRequested, VirtualConnection vci,
                              boolean asyncIO) {
        if (timeout < 0) {
            throw new IllegalArgumentException();
        }

        IAsyncFuture future = write(bufs, forceQueue, bytesRequested,
                                    vci, asyncIO);

        // don't lock the getCompletedSemaphore here, wait till later
        if (future != null && !future.isCompleted() && timeout > 0) {
            createTimeout(future, timeout, false);
        }
        return future;
    }

    /**
     * Requests an asynchronous write from an array of direct ByteBuffers to
     * the <code>AsyncSocketChannel</code>.
     * <p>
     * The byte buffers in the array must be direct byte buffers created
     * using {@link java.nio.ByteBuffer#allocateDirect(int)}. The array
     * of byte buffers can contain <code>null</code> entries. Processing
     * of the array will terminate at the first <code>null</code> entry.
     * Any entries after the <code>null</code> entry are ignored.
     * </p>
     * <p>
     * The method returns immediately with a <em>future</em> object.
     * Applications can invoke methods on the future object to determine the
     * completion status of the write, including registering a listener to
     * be called back when the operation completes. The completed future
     * also reports the total number of bytes written, or if the write
     * failed, the reason for failure as an <code>Exception</code>.
     * </p>
     * <p>
     * The asynchronous IO subsystem retains logical control of the array
     * of byte buffers until the write completes. Clients should not
     * attempt to read or modify the byte buffers until the system
     * indicates that the operation is finished by marking the future
     * completed.
     * </p>
     * <p>
     * Only one write operation can be in progress on this channel at a
     * time. No further writes will succeed on this channel until this write
     * completes. Attempts to write on the channel before an outstanding
     * write has completed will return a completed future with an <code>
     * IOPendingException</code>.
     * </p>
     * <p>
     * The write operation uses the array of byte buffers as follows:
     * <ul>
     * <li>Starting with the first byte buffer in the array, data is written
     * from the buffer starting at the buffer's initial <code>buffer.position()
     * </code>.</li>
     * <li>A maximum of <code>buffer.remaining()</code> bytes are written to
     * the channel.</li>
     * <li>The buffer's <code>buffer.position()</code> is incremented by the
     * number of bytes successfully written.</li>
     * <li>The buffer's limit is left unchanged.</li>
     * <li>This is repeated for each buffer in turn, either until the channel
     * is unable to take any more data or until the data in all the buffers
     * has been written to the channel.</li>
     * </ul>
     * </p>
     * <p>
     * If the operation fails the future will return an <code>Exception
     * </code>. Some of the byte buffers may get modified in these
     * circumstances.
     * </p>
     * <p>
     * Example usage is:
     * 
     * <pre>
     * ...
     * ByteBuffer[] bufarray = new ByteBuffer[MAXBUFS];
     * for( int i = 0, i < MAXBUFS; i++ ) {
     * bufarray[i] = ByteBuffer.allocateDirect(1024);
     * buffer.put("Some data....".getBytes());
     * buffer.flip();
     * }
     * IAsyncFuture writeFuture = channel.write(bufarray);
     * ... do some useful work...
     * try {
     * // bytesWritten is the total number of bytes written across all buffers
     * long bytesWritten = writeFuture.getByteCount();
     * ...
     * } catch (AsyncException ex) {
     * ... the IO operation failed...
     * }
     * </pre>
     * 
     * </p>
     * 
     * @param bufs
     *            the array of <code>ByteBuffers</code> to write from.
     *            The array must be non-null.
     *            The first entry in the array must be non-null.
     *            The second or subsequent entries in the array may be null. The write operation uses all of the entries in the
     *            array before the first null entry. Any entries after the first null entry are ignored.
     * @param forceQueue
     * @param bytesRequested
     * @param vci
     * @param asyncIO
     * @return an <code>IAsyncFuture</code> which acts as a placeholder for the asynchronous operation.
     * @throws IllegalArgumentException if the preconditions on the bufs parameter are not met.
     * @see IAsyncFuture
     */
    public IAsyncFuture write(ByteBuffer[] bufs, boolean forceQueue,
                              long bytesRequested, VirtualConnection vci, boolean asyncIO) {
        return this.schannel.multiIO(bufs, 0, false, forceQueue,
                                     bytesRequested, false, vci, asyncIO);
    }

    /**
     * Returns the socket channel that this instance is wrappering.
     * 
     * @return the wrapped <code>AsyncSocketChannel</code>.
     */
    public AsyncSocketChannel getChannel() {
        return this.schannel;
    }

    /**
     * Create the delayed timeout work item for this request.
     * 
     * @param future
     * @param delay
     * @param isRead
     */
    private void createTimeout(IAbstractAsyncFuture future, long delay, boolean isRead) {
        if (AsyncProperties.disableTimeouts) {
            return;
        }
        // create the timeout time, while not holding the lock
        long timeoutTime =
                        (System.currentTimeMillis() + delay + Timer.timeoutRoundup)
                                        & Timer.timeoutResolution;

        synchronized (future.getCompletedSemaphore()) {
            // make sure it didn't complete while we were getting here
            if (!future.isCompleted()) {
                timer.createTimeoutRequest(timeoutTime, this.callback, future);
            }
        }
    }

}
