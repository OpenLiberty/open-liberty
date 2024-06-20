/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.local.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.ibm.ws.zos.channel.local.LocalCommClientConnHandle;
import com.ibm.ws.zos.channel.local.LocalCommReadCompletedCallback;
import com.ibm.ws.zos.channel.local.LocalCommServiceContext;
import com.ibm.ws.zos.channel.local.queuing.BlackQueueReadyCallback;
import com.ibm.ws.zos.channel.local.queuing.NativeWorkRequest;
import com.ibm.ws.zos.channel.local.queuing.NativeWorkRequestType;

/**
 * Read/Write interface for the LocalCommChannel.
 * 
 * There are 1 of these per LocalCommConnLink (i.e. one per connection).
 */
public class LocalCommServiceContextImpl implements LocalCommServiceContext {

    /**
     * Reference to the LocalCommConnLink who created this service context.
     */
    private LocalCommConnLink localCommConnLink = null;

    /**
     * CTOR.
     * 
     * @param localCommConnLink - The guy who created this object.
     */
    public LocalCommServiceContextImpl(LocalCommConnLink localCommConnLink) {
        this.localCommConnLink = localCommConnLink;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void read(final LocalCommReadCompletedCallback upstreamCallback) {
        try {

            ByteBuffer data = readHelper(upstreamCallback, false);

            if (data != null) {
                // The read completed synchronously.  Invoke the callback inline.
                upstreamCallback.ready(this, data);
            }

        } catch (IOException ioe) {
            // The read failed synchronously.  Invoke the callback inline.
            upstreamCallback.error(this, ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void asyncRead(final LocalCommReadCompletedCallback upstreamCallback) {
        try {

            final ByteBuffer data = readHelper(upstreamCallback, true);

        	if (data != null) {
        		// The read completed synchronously.  This is an error.
        		throw new IOException("Async read completed synchronously");
        	}

    	} catch (final IOException ioe) {
            // The read failed synchronously.  Force async by invoking the callback 
            // on a separate thread.
            localCommConnLink.getChannel().getExecutorService().execute(new Runnable() {
                @Override
                public void run() {
                    upstreamCallback.error(LocalCommServiceContextImpl.this, ioe);
                }
            });
        }
    }

    /**
     * 
     * @return The data read, if data was immediately available. Otherwise, this method
     *         returns null, indicating the read went async.
     */
    protected ByteBuffer readHelper(final LocalCommReadCompletedCallback upstreamCallback, boolean forceAsync) throws IOException {

        // Neat way to simulate a closure (although there is a perf cost of creating
        // the anonymous object every time...).
        BlackQueueReadyCallback anon = new BlackQueueReadyCallback() {
            @Override
            public void blackQueueReady(NativeWorkRequest nativeWorkRequest) {
                // Just call right back into the read() method.
                // This time, however, there should be data ready.
                // Note: we do not call into asyncRead, because we're already on an async path.
           	read(upstreamCallback);
            }

            @Override
            public void cancel(Exception e) {
                upstreamCallback.error(LocalCommServiceContextImpl.this, e);
            }
        };

        // Remember the exception we caught (if any), for the finally block.
        IOException caughtException = null;

        // The read data.
        ByteBuffer retMe = null;

        try {
            // Register with BlackQueueDemultiplexor to be notified when the read has completed.
            localCommConnLink.getChannel().getLocalChannelProvider()
                            .getBlackQueueDemultiplexor()
                            .registerCallback(anon,
                                              NativeWorkRequestType.REQUESTTYPE_READREADY,
                                              getClientConnectionHandle());
            // Read the data.
            // This call shall either:
            //  1) return the data that was read.
            //  2) return null, indicating it went async
            //  3) throw an exception for anything else
            retMe = localCommConnLink.getNativeRequestHandler().read(getConnectWorkRequest(), forceAsync);

        } catch (IOException ioe) {
            caughtException = ioe;

        } finally {

            if (retMe != null || caughtException != null) {
                // Either the read completed synchronously (retMe != null), or an exception occurred
                // (either the callback registration failed or the read failed).
                // Either way, make sure the registered callback is removed.
                localCommConnLink.getChannel().getLocalChannelProvider()
                                .getBlackQueueDemultiplexor()
                                .removeCallback(anon,
                                                NativeWorkRequestType.REQUESTTYPE_READREADY,
                                                getClientConnectionHandle());
            }
        }

        if (caughtException != null) {
            throw caughtException;
        } else {
            return retMe;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer syncRead() throws IOException {

        SyncReadCompletedCallback syncReadCompletedCallback = new SyncReadCompletedCallback();

        read(syncReadCompletedCallback);

        try {
            return syncReadCompletedCallback.get();
        } catch (ExecutionException ee) {
            if (ee.getCause() instanceof IOException) {
                throw (IOException) ee.getCause();
            } else {
                throw new IOException(ee);
            }
        } catch (InterruptedException ie) {
            throw new IOException(ie);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void syncWrite(ByteBuffer buffer) throws IOException {
        localCommConnLink.getNativeRequestHandler().write(getConnectWorkRequest(), buffer);
    }

    /**
     * Return the client's connection handle. This information may be useful
     * in tying together server and client connection information, and is used
     * by WOLA to find the server side of a local comm connection during an
     * outbound service call (outbound from Liberty to a service hosted by a
     * native language program).
     */
    @Override
    public LocalCommClientConnHandle getClientConnectionHandle() {
        return localCommConnLink.getClientConnectionHandle();
    }

    /**
     * @return the connectWorkRequest associated with this connection.
     */
    private NativeWorkRequest getConnectWorkRequest() {
        return localCommConnLink.getConnectWorkRequest();
    }

    /**
     * Indicate to the local comm connection that it's safe to disconnect.  
     * This method is called by the upstream channel under their read-completed 
     * callback after they've read off the data.
     */
    @Override
    public void releaseDisconnectLock() {
        localCommConnLink.releaseDisconnectLock();
    }

    /**
     * Tells the caller that they can or cannot dispatch a request on this thread.
     */
	@Override
	public boolean isDispatchThread() {
		return (localCommConnLink.getChannel().getLocalChannelProvider().isBlackQueueListenerThread() == false);
	}
}

/**
 * Callback object for sync reads. syncRead() calls into the normal read() path, which may
 * or may not go async, depending on whether data is available. Whether or not it goes
 * async, once the read is complete it will invoke this callback object with the data
 * that was read.
 * 
 * The callback object also implements Future. syncRead() waits on the result of the
 * (possibly async) operation by calling Future.get(), which blocks until the callback
 * has been called for read complete.
 * 
 * The synchronization details are all handled by the BlockingQueue, which is used to pass the
 * read-complete data from the LocalCommReadCompletedCallback side of this class to the Future side.
 * 
 */
class SyncReadCompletedCallback implements LocalCommReadCompletedCallback, Future<ByteBuffer> {

    /**
     * Handles all the messy synchronization details between the read-complete callback
     * and the Future.get.
     */
    private final BlockingQueue<ByteBuffer> readCompleteData = new ArrayBlockingQueue<ByteBuffer>(1);

    /**
     * Indicates whether the operation has completed (for Future.isDone).
     */
    private boolean isItDone = false;

    /**
     * If the read fails with an exception it is cached here. The Future.get call is then
     * woken up with an empty ByteBuffer. Future.get then checks this field and if it is
     * not null, raises the exception.
     */
    private Exception readException = null;

    /**
     * @return the data that was read.
     * 
     * @throws ExecutionException - if the read failed. The cause of the failure can be found
     *             via ExecutionException.getCause().
     */
    @Override
    public ByteBuffer get() throws InterruptedException, ExecutionException {
        ByteBuffer retMe = readCompleteData.take();
        if (readException != null) {
            throw new ExecutionException(readException);
        }
        return retMe;
    }

    /**
     * Push the read data onto the blocking queue and mark the operation 'done'.
     */
    @Override
    public void ready(LocalCommServiceContext context, ByteBuffer data) {
        readCompleteData.add(data);
        isItDone = true;
    }

    /**
     * Cache the exception, set an empty ByteBuffer onto the blocking queue (to wake
     * up Future.get), and mark the operation 'done'.
     */
    @Override
    public void error(LocalCommServiceContext context, Exception e) {
        readException = e;
        readCompleteData.add(ByteBuffer.allocate(0)); // Need to put something here to wake up Future.get.
        isItDone = true;
    }

    @Override
    public boolean isDone() {
        return isItDone;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public ByteBuffer get(long timeout, TimeUnit unit)
                    throws InterruptedException, ExecutionException, TimeoutException {
        throw new UnsupportedOperationException();
    }
}
