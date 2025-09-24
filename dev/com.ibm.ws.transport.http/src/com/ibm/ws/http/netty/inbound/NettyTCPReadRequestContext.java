/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.inbound;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.ws.netty.upgrade.NettyServletUpgradeHandler;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;

import io.netty.channel.Channel;

/**
 *
 */
public class NettyTCPReadRequestContext implements TCPReadRequestContext {
    
    private static final TraceComponent tc = Tr.register(NettyTCPReadRequestContext.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    private final NettyTCPConnectionContext connectionContext;
    private final Channel nettyChannel;

    private WsByteBuffer[] buffers;
    private ByteBuffer byteBufferArray[] = null;
    private ByteBuffer byteBufferArrayDirect[] = null;

    // define reusable arrrays of most common sizes
    private ByteBuffer byteBufferArrayOf1[] = null;
    private ByteBuffer byteBufferArrayOf2[] = null;
    private ByteBuffer byteBufferArrayOf3[] = null;
    private ByteBuffer byteBufferArrayOf4[] = null;
    private final WsByteBuffer[] defaultBuffers = new WsByteBuffer[1];

    private final boolean jitAllocateAction = false;
    private int jitAllocateSize = 0;

    private VirtualConnection vc = null;

    public NettyTCPReadRequestContext(NettyTCPConnectionContext connectionContext, Channel nettyChannel) {

        this.connectionContext = connectionContext;
        this.nettyChannel = nettyChannel;

    }

    @Override
    public void clearBuffers() {
        if (this.buffers != null) {
            for (int i = 0; i < this.buffers.length; i++) {
                this.buffers[i].clear();
            }
        }

    }

    @Override
    public TCPConnectionContext getInterface() {
        return this.connectionContext;
    }

    @Override
    public Socket getSocket() {
        throw new UnsupportedOperationException("Can not get the socket from a Netty connection!");
    }

    /**
     * Performs reads on the connection until at least the specified number of
     * bytes have been read.
     * This call is always synchronous, and will result in blocking the thread
     * until the
     * minimum number of bytes has been read. A numBytes value of 0 will cause the
     * read to return
     * immediately. Upon completion of the read, WsByteBuffer(s) position will be
     * set
     * to the end of the data. If timeout is set equal to IMMED_TIMEOUT, then an
     * attempt
     * to immediately timeout the previous read will be made, and this read will
     * return 0.
     * 
     * @param numBytes
     *            - minimum number of bytes to read. Max value for numBytes is
     *            2147483647
     * @param timeout
     *            - timeout value to associate with this request (milliseconds)
     * @return long - number of bytes read
     * @throws IOException
     */
    @Override
    public long read(long numBytes, int timeout) throws IOException {
        
        if (!nettyChannel.isActive()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Found closed connection on read for channel: " + nettyChannel);
            }
            throw new IOException("Netty channel is not active.");
        }

                
        if (nettyChannel.pipeline().get(NettyServletUpgradeHandler.class) == null) {
            NettyServletUpgradeHandler upgradeHandler = new NettyServletUpgradeHandler(nettyChannel);
            nettyChannel.pipeline().addLast("ServletUpgradeHandler", upgradeHandler);
        }

        final NettyServletUpgradeHandler upgradeHandler = nettyChannel.pipeline().get(NettyServletUpgradeHandler.class);
        upgradeHandler.setTCPReadContext(this);
        upgradeHandler.setTCPReadContext(this);
        upgradeHandler.setVC(vc);
        
        
        if (upgradeHandler == null) {
            throw new IOException("Upgrade handler not present in pipeline.");
        }

        if(timeout == IMMED_TIMEOUT) {
            // Immediate timeout hit, need to cancel all previous reads
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Calling immediate timeout on channel: " + nettyChannel);
            }
            upgradeHandler.immediateTimeout();
            return 0;
        }

        ExecutorService blockingTaskExecutor = HttpDispatcher.getExecutorService();
        Future<Long> readFuture = blockingTaskExecutor.submit(() -> {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Starting read in synchronous thread for channel: " + nettyChannel);
            }
            boolean dataAvailable = upgradeHandler.containsQueuedData() || upgradeHandler.awaitReadReady(numBytes, timeout, TimeUnit.MILLISECONDS);
            if (!dataAvailable || !nettyChannel.isActive()) {
                throw new SocketTimeoutException("Failed to read data within the specified timeout.");
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Finished read in synchronous thread for channel: " + nettyChannel);
            }
            return upgradeHandler.setToBuffer();
        });

        try {
            // Wait for the read operation to complete or timeout
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Waiting on read for channel: " + nettyChannel);
            }
            if (timeout == NO_TIMEOUT)
                return readFuture.get();
            else if (timeout == USE_CHANNEL_TIMEOUT)
                return readFuture.get(60000, TimeUnit.MILLISECONDS);
            else
                return readFuture.get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Interrupted exception on channel: " + nettyChannel);
            }
            Thread.currentThread().interrupt();
            throw new IOException("Thread interrupted while reading.", e);
        } catch (ExecutionException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Exection exception on channel: " + nettyChannel);
            }
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new IOException("Error occurred during read operation.", cause);
        } catch (TimeoutException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Timeout exception on channel: " + nettyChannel);
            }
            throw new SocketTimeoutException("Read operation timed out.");
        }
    }

    @Override
    @FFDCIgnore(EOFException.class)
    public VirtualConnection read(long numBytes, TCPReadCompletedCallback callback, boolean forceQueue, int timeout) {
        if (!nettyChannel.isActive()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Channel became inactive, not queueing async read! " + nettyChannel);
            }
            // Channel is not active, do not proceed with the callback
             return vc; // Return
        }

        if (nettyChannel.pipeline().get(NettyServletUpgradeHandler.class) == null) {
            NettyServletUpgradeHandler upgradeHandler = new NettyServletUpgradeHandler(nettyChannel);
            nettyChannel.pipeline().addLast("ServletUpgradeHandler", upgradeHandler);
        }

        NettyServletUpgradeHandler upgrade = this.nettyChannel.pipeline().get(NettyServletUpgradeHandler.class);
        
        if(timeout == IMMED_TIMEOUT) {
            // Immediate timeout hit, need to cancel all previous reads
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Calling immediate timeout on channel: " + nettyChannel);
            }
            upgrade.immediateTimeout();
            return null;
        }

        if (Objects.nonNull(callback)) {
            upgrade.setReadListener(callback);
        }
        upgrade.setTCPReadContext(this);
        upgrade.setVC(vc);

        ExecutorService blockingTaskExecutor = HttpDispatcher.getExecutorService();
        if (upgrade.containsQueuedData() && upgrade.queuedDataSize() >= numBytes) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Found queued data on channel: " + nettyChannel);
            }
            if(!forceQueue) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Not force queue so setting to buffer and returning...");
                }
                upgrade.setToBuffer();
                return vc;
            }
            blockingTaskExecutor.submit(() -> {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Running async read callback for channel: " + nettyChannel);
                }
                upgrade.setToBuffer();
                callback.complete(vc, this);
            });
            return null;
        }
        // If no data, then we queue the callback to run once data has been received
        if (!nettyChannel.isActive()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Channel became inactive, not queueing read! " + nettyChannel);
            }
            // Channel is not active, call on error
            HttpDispatcher.getExecutorService().execute(() -> {
                try {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Running async read callback error channel closed for channel: " + nettyChannel);
                    }
                    upgrade.getReadListener().error(vc, this, new EOFException("Connection closed: Read failed.  Possible end of stream encountered. local=" + nettyChannel.localAddress() + " remote=" + nettyChannel.remoteAddress()));
                } catch (Exception e2) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Exception calling error on read listener for channel: " + nettyChannel + " error: " + e2);
                    }
                }
            });
            return null; // Return
        }
        try {
            upgrade.queueAsyncRead(timeout, numBytes);
        } catch(EOFException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "EOF exception caught on channel: " + nettyChannel);
            }
            HttpDispatcher.getExecutorService().execute(() -> {
                try {
                    upgrade.getReadListener().error(vc, this, e);
                } catch (Exception e2) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Exception calling EOF error on read listener for channel: " + nettyChannel + " error: " + e2);
                    }
                }
            });
        }
        return null;
    }


    @Override
    public void setJITAllocateSize(int numBytes) {
        this.jitAllocateSize = numBytes;
    }

    @Override
    public boolean getJITAllocateAction() {
        return this.jitAllocateAction;
    }

    @Override
    public WsByteBuffer[] getBuffers() {
        return this.buffers;
    }

    @Override
    public void setBuffers(WsByteBuffer[] bufs) {
        this.buffers = bufs;

        // reset arrays to free memory quicker. defect 457362
        if (this.byteBufferArray != null) {
            // reset references
            for (int i = 0; i < this.byteBufferArray.length; i++) {
                this.byteBufferArray[i] = null;
            }
        }

        if (this.byteBufferArrayDirect != null) {
            // reset references
            for (int i = 0; i < this.byteBufferArrayDirect.length; i++) {
                this.byteBufferArrayDirect[i] = null;
            }
            this.byteBufferArrayDirect = null;
        }

        if (bufs != null) {
            int numBufs;
            // reuse an existing byteBufferArray if one was already created
            // kind of hokey, but this allows us to avoid construction of a
            // new array object unless absolutely neccessary

            // following loop will count the number of buffers in
            // the input array rather than relying on the array length
            for (numBufs = 0; numBufs < bufs.length; numBufs++) {
                if (bufs[numBufs] == null) {
                    break;
                }
            }

            if (numBufs == 1) {
                if (this.byteBufferArrayOf1 == null) {
                    this.byteBufferArrayOf1 = new ByteBuffer[1];
                }
                this.byteBufferArray = this.byteBufferArrayOf1;
            } else if (numBufs == 2) {
                if (this.byteBufferArrayOf2 == null) {
                    this.byteBufferArrayOf2 = new ByteBuffer[2];
                }
                this.byteBufferArray = this.byteBufferArrayOf2;
            } else if (numBufs == 3) {
                if (this.byteBufferArrayOf3 == null) {
                    this.byteBufferArrayOf3 = new ByteBuffer[3];
                }
                this.byteBufferArray = this.byteBufferArrayOf3;
            } else if (numBufs == 4) {
                if (this.byteBufferArrayOf4 == null) {
                    this.byteBufferArrayOf4 = new ByteBuffer[4];
                }
                this.byteBufferArray = this.byteBufferArrayOf4;

            } else {
                // more than 4 buffers in request, allocate array as needed
                this.byteBufferArray = new ByteBuffer[numBufs];
            }

            if (numBufs > 1) {
                for (int i = 0; i < numBufs; i++) {
                    this.byteBufferArray[i] = bufs[i].getWrappedByteBufferNonSafe();
                }
            } else if (numBufs == 1) {
                this.byteBufferArray[0] = bufs[0].getWrappedByteBufferNonSafe();
            }

        } else {
            // buffers == null, so set byteBufferArray to null also
            this.byteBufferArray = null;
        }
    }

    @Override
    public WsByteBuffer getBuffer() {
        if (this.buffers == null) {
            return null;
        }
        return this.buffers[0];
    }

    @Override
    public void setBuffer(WsByteBuffer buf) {

        // reset arrays to free memory quicker. defect 457362
        if (this.byteBufferArray != null) {
            // reset references
            for (int i = 0; i < this.byteBufferArray.length; i++) {
                this.byteBufferArray[i] = null;
            }
        }
        if (this.byteBufferArrayDirect != null) {
            // reset references
            for (int i = 0; i < this.byteBufferArrayDirect.length; i++) {
                this.byteBufferArrayDirect[i] = null;
            }
            this.byteBufferArrayDirect = null;
        }
        this.defaultBuffers[0] = null; // reset reference

        if (buf != null) {
            this.buffers = this.defaultBuffers;
            this.buffers[0] = buf;

            if (this.byteBufferArrayOf1 == null) {
                this.byteBufferArrayOf1 = new ByteBuffer[1];
            }
            this.byteBufferArray = this.byteBufferArrayOf1;
            this.byteBufferArray[0] = buf.getWrappedByteBufferNonSafe();
        } else {
            this.buffers = null;
            this.byteBufferArray = null;
        }
    }

    public void setVC(VirtualConnection vc) {
        this.vc = vc;
    }

}
