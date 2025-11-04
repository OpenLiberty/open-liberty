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
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.inbound.HttpInputStreamImpl;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.ws.http.netty.NettyHttpChannelConfig;
import com.ibm.ws.netty.upgrade.NettyServletUpgradeHandler;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.http.ee7.HttpInputStreamEE7;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;

import io.netty.channel.Channel;
import io.openliberty.http.options.TcpOption;
import com.ibm.ws.http.netty.NettyHttpConstants;

import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;

/**
 *
 */
public class NettyTCPReadRequestContext implements TCPReadRequestContext {

    private static final TraceComponent tc = Tr.register(NettyTCPReadRequestContext.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    private int channelDefaultTimeout = (int) TcpOption.INACTIVITY_TIMEOUT.getDefaultValue();

    private final NettyTCPConnectionContext connectionContext;
    private final Channel nettyChannel;

    private WsByteBuffer[] buffers;
    private final WsByteBuffer[] defaultBuffers = new WsByteBuffer[1];
    
    private VirtualConnection vc = null;
    private int jitAllocateSize = 0;
    private boolean jitAllocateAction = false;

    private volatile boolean aborted = false;

    public NettyTCPReadRequestContext(NettyTCPConnectionContext connectionContext, Channel nettyChannel) {
        this.connectionContext = connectionContext;
        this.nettyChannel = nettyChannel;

        HttpChannelConfig config = nettyChannel.attr(NettyHttpConstants.HTTP_CONFIG).get();
        if(config != null && config instanceof NettyHttpChannelConfig){
            this.channelDefaultTimeout = (int) ((NettyHttpChannelConfig) config).get(TcpOption.INACTIVITY_TIMEOUT);
        }
    }

    @Override
    public void clearBuffers() {
        if(this.buffers != null){
            for(WsByteBuffer buffer: this.buffers){
                if(buffer == null) break;
                buffer.clear();
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

    private HttpInputStreamImpl input() throws IOException {
        HttpInputStreamImpl in = nettyChannel.attr(NettyHttpConstants.HTTP_INPUT_STREAM).get();
        if (in == null) {
            throw new IOException("HTTP input stream not initialized for channel " + nettyChannel);
        }
        return in;
    }

    /**
     * Performs reads on the connection until at least the specified number of bytes have been read.
     * This call is always synchronous, and will result in blocking the thread until the minimum
     * number of bytes has been read. A numBytes value of 0 will cause the read to return immediately.
     * Upon completion of the read, WsByteBuffer(s) position will be set to the end of the data.
     * If timeout is set equal to IMMED_TIMEOUT, then an attempt to immediately timeout the previous
     * read will be made, and this read will return 0.
     *
     * @param numBytes - minimum number of bytes to read. Max value for numBytes is 2147483647
     * @param timeout  - timeout value to associate with this request (milliseconds)
     * @return long - number of bytes read
     * @throws IOException
     */
    @Override
    public long read(long numBytes, int timeout) throws IOException {
        if(aborted) throw new IOException("I/O Aborted");


        if (!nettyChannel.isActive()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Found closed connection on read for channel: " + nettyChannel);
            }
            throw new EOFException("Connection closed: Read failed. Possible end of stream encountered. local="
                                   + nettyChannel.localAddress()
                                   + " remote="
                                   + nettyChannel.remoteAddress());
        }

        if (timeout == IMMED_TIMEOUT) {
            if (isUpgraded())
                ensureUpgradeHandler().immediateTimeout();
            return 0L;
        }

        if (timeout == ABORT_TIMEOUT) {
            aborted = true;
            if (isUpgraded())
                ensureUpgradeHandler().immediateTimeout();
            return 0L;
        }

        ensureBuffersOrJIT(numBytes, false);

        if(numBytes == 0){
            return isUpgraded() ? upgradedImmediateDrain() : nonUpgradedImmediateDrain();
        }

        return isUpgraded() ? upgradedSyncRead(numBytes, timeout) : nonUpgradedSyncRead(numBytes, timeout);
    }

    private long nonUpgradedSyncRead(long numBytes, int timeout) throws IOException {
        final HttpInputStreamImpl in = input();
        if (buffers == null || buffers.length == 0 || buffers[0] == null)
            throw new IOException("Buffers not set for read()");
        
        if(numBytes <= 0){
            int available;
            try{
                available = Math.max(0, in.available());
            } catch (IOException ioe){
                available = 0;
            }
            if(available <= 0) return 0L;

            int capacity = 0;
            for (WsByteBuffer buffer: buffers){
                if (buffer == null) break;
                capacity += buffer.remaining();
            }
            final int chunk = Math.min(8192, Math.min(available, capacity));
            final byte[] temp = new byte[chunk];
            final int n = in.read(temp, 0, chunk);
            if (n > 0){
                int off = 0;
                for (WsByteBuffer buffer: buffers){
                    if (buffer == null || off >= n) break;
                    final int can = Math.min(buffer.remaining(), n - off);
                    if (can > 0){
                        buffer.getWrappedByteBuffer().put(temp, off, can);
                        buffer.position(buffer.position()+can);
                        off += can;
                    }
                }
                return n;
            }
            return 0L;
        }

        final int effectiveTimeout = normalizeTimeout(timeout);
        final long deadlineNs = (effectiveTimeout == NO_TIMEOUT) ? Long.MAX_VALUE : System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(effectiveTimeout);

        ensureReadIfManual();

        final byte[] scratch = new byte[8192];
        long delivered = 0L;

        while (true) {
            int target = 0;
            for (WsByteBuffer b : buffers) {
                if (b == null)
                    break;
                target += b.remaining();
            }
            if (numBytes > 0)
                target = (int) Math.min(target, Math.max(0, numBytes - delivered));
            if (target == 0)
                return delivered;

            final int chunk = Math.min(target, scratch.length);
            final int n = in.read(scratch, 0, chunk);

            if (n > 0) {
                int off = 0;
                for (WsByteBuffer b : buffers) {
                    if (b == null || off >= n)
                        break;
                    final int can = Math.min(b.remaining(), n - off);
                    if (can > 0) {
                        b.getWrappedByteBuffer().put(scratch, off, can);
                        b.position(b.position()+can);
                        off += can;
                    }
                }
                delivered += n;
                if (numBytes > 0 && delivered >= numBytes)
                    return delivered;
                continue;
            }

            if (n == -1)
                return delivered; // EOF

            // cooperative small wait and nudge read if manual
            if (!nettyChannel.config().isAutoRead())
                nettyChannel.eventLoop().execute(nettyChannel::read);

            if (deadlineNs != Long.MAX_VALUE && System.nanoTime() > deadlineNs)
                throw new SocketTimeoutException("Failed to read data within the specified timeout.");

            // wake on next EL tick
            final Object lock = new Object();
            nettyChannel.eventLoop().execute(() -> {
                synchronized (lock) {
                    lock.notify();
                }
            });
            try {
                synchronized (lock) {
                    lock.wait(1L);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IOException("Thread interrupted while reading.", ie);
            }
        }
    }

    private long upgradedSyncRead(long numBytes, int timeout) throws IOException {
        final NettyServletUpgradeHandler h = ensureUpgradeHandler();
        h.setTCPReadContext(this);
        h.setVC(vc);

        if (numBytes <= 0) {
            // Inspect only; do not wait
            ensureReadIfManual();
            Tr.debug(tc, "numBytes<= 0  containsQueuedData="+h.containsQueuedData());
            return h.containsQueuedData() ? h.setToBuffer() : 0L;
        }

        final int t = normalizeTimeout(timeout);
        final long need = Math.max(1L, numBytes);
        final long deadlineNs = (t == NO_TIMEOUT) ? Long.MAX_VALUE
                                                  : System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(t);

        ensureReadIfManual();
        // Fast path
        if (h.containsQueuedData() && h.queuedDataSize() >= need) {
            long copied = h.setToBuffer();
            Tr.debug(tc, "(Fast path) UPG sync read: need="+need+" copied="+copied+ " queuedAfter="+h.queuedDataSize());
            return copied;
        }
        // Cooperative wait loop with read nudges; timeout enforced here.
        while (nettyChannel.isActive()) {
            final long now = System.nanoTime();
            if (deadlineNs != Long.MAX_VALUE && now >= deadlineNs) {
                throw new SocketTimeoutException("Failed to read data within the specified timeout.");
            }
            final long remainingMs = (deadlineNs == Long.MAX_VALUE) ? 250L
                                    : Math.max(1L, TimeUnit.NANOSECONDS.toMillis(deadlineNs - now));
           try {
                h.waitForDataRead(Math.min(remainingMs, 250L)); // slice wait
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for upgraded read data.", ie);
            }
            if (h.containsQueuedData() && h.queuedDataSize() >= need) {
                long copied = h.setToBuffer();
                Tr.debug(tc, "(Fast path) UPG sync read: need=" + need + " copied=" + copied + " queuedAfter=" + h.queuedDataSize());
                return copied;
            }
            ensureReadIfManual();
        }
        throw new EOFException("Channel inactive during read");
    }

    @Override
    //@FFDCIgnore(EOFException.class)
    public VirtualConnection read(long numBytes, TCPReadCompletedCallback callback, boolean forceQueue, int timeout) {
        if (aborted) {
            if (callback != null) {
                HttpDispatcher.getExecutorService().execute(
                                                            () -> callback.error(vc, this, new EOFException("I/O aborted")));
            }
            return null;
        }

        if (!nettyChannel.isActive()) {
            if (callback != null) {
                HttpDispatcher.getExecutorService().execute(
                                                            () -> callback.error(vc, this, new EOFException("Channel closed.")));
            }
            return null;
        }

        if (isUpgraded()) {

            final int effectiveTimeout = normalizeTimeout(timeout);
            // For IMMED/ABORT we won't actually read; skip buffer ensure in those two cases.
            if (effectiveTimeout != IMMED_TIMEOUT && effectiveTimeout != ABORT_TIMEOUT) {
                ensureBuffersOrJIT(numBytes, /* isAsync = */ true);
            }
            return upgradedAsyncRead(numBytes, callback, forceQueue, timeout);
        }

        // Non-upgrade async: arm a one-shot runnable that re-enters the sync path when data arrives.
        final int effectiveTimeout = normalizeTimeout(timeout);

        // IMMED cancels any prior wait and returns immediately
        if (effectiveTimeout == IMMED_TIMEOUT)
            return null;
        if (effectiveTimeout == ABORT_TIMEOUT) {
            aborted = true;
            return null;
        }

        ensureBuffersOrJIT(numBytes, true);

        Runnable r = () -> {
            try {
                read(numBytes, effectiveTimeout);
                if (callback != null)
                    callback.complete(vc, this);
            } catch (Throwable t) {
                if (callback != null) {
                    callback.error(vc, this, (t instanceof EOFException) ? (EOFException) t : new EOFException(t.toString()));
                }
            }
        };

        nettyChannel.attr(com.ibm.ws.http.netty.NettyHttpConstants.ASYNC_READ_CALLBACK).set(r);

        // If bytes already available (or finished), run now
        try {
            HttpInputStreamImpl in = input();
            boolean isEE7 = (in instanceof HttpInputStreamEE7);
            if (in.available() > 0 || (isEE7 && ((HttpInputStreamEE7) in).isFinished())) {
                Runnable pending = nettyChannel.attr(com.ibm.ws.http.netty.NettyHttpConstants.ASYNC_READ_CALLBACK).getAndSet(null);
                if (pending != null)
                    HttpDispatcher.getExecutorService().execute(pending);
            }
        } catch (IOException ignore) { /* dispatcher close handles this */}

        ensureReadIfManual();
        return null;
    }

    public VirtualConnection upgradedAsyncRead(long numBytes, TCPReadCompletedCallback callback, boolean forceQueue, int timeout) {
        final NettyServletUpgradeHandler h = ensureUpgradeHandler();
        h.setTCPReadContext(this);
        h.setVC(vc);

        // IMMED cancels prior async wait
        if (timeout == IMMED_TIMEOUT) {
            h.immediateTimeout();
            return null;
        }
        if (timeout == ABORT_TIMEOUT) {
            aborted = true;
            h.immediateTimeout();
            return null;
        }

        final long need = Math.max(1L, numBytes);
        ensureReadIfManual();

        if (h.containsQueuedData() && h.queuedDataSize() >= need) {
            //if (!forceQueue) {
                long copied = h.setToBuffer();
                Tr.debug(tc, "(Fast path) UPG async read (sync complete): need=" + need
                             + " copied=" + copied + " queuedAfter=" + h.queuedDataSize());
            //    return vc; // completed synchronously
            //} else {
                // Forced async: offload copy+callback to Dispatcher, and return null
                if (callback != null) {
                    HttpDispatcher.getExecutorService().execute(() -> {
                        //long copied = h.setToBuffer();
                        Tr.debug(tc, "(Fast path) UPG async read (forced async): need=" + need
                                     + " copied=" + copied + " queuedAfter=" + h.queuedDataSize());
                        
                        try {
                            callback.complete(vc, this);
                        } catch (Exception e) {/* keep async semantic */}             
                    });
                }
                return null; // indicates async completion
            //}
        }
        




        // Wrap the callback to cancel the scheduled timeout on either complete() or error()
        final io.netty.util.concurrent.EventExecutor el = nettyChannel.eventLoop();
        final java.util.concurrent.atomic.AtomicReference<io.netty.util.concurrent.ScheduledFuture<?>> toRef =
                new java.util.concurrent.atomic.AtomicReference<>(null);

        final TCPReadCompletedCallback wrapped = new TCPReadCompletedCallback() {
            @Override
            public void complete(VirtualConnection v, TCPReadRequestContext ctx) {
                io.netty.util.concurrent.ScheduledFuture<?> f = toRef.getAndSet(null);
                if (f != null) {
                    try {
                        f.cancel(false);
                    } catch (Throwable ignore) {
                    }
                }
                if (callback != null) {
                    HttpDispatcher.getExecutorService().execute(() -> {
                        try {
                            callback.complete(v, ctx);
                        } catch (Throwable ignore) {
                        }
                    });
                }
            }

            @Override
            public void error(VirtualConnection v, TCPReadRequestContext ctx, java.io.IOException e) {
                io.netty.util.concurrent.ScheduledFuture<?> f = toRef.getAndSet(null);
                if (f != null) {
                    try {
                        f.cancel(false);
                    } catch (Throwable ignore) {
                    }
                }
                if (callback != null) {
                    HttpDispatcher.getExecutorService().execute(() -> {
                        try {
                            callback.error(v, ctx, e);
                        } catch (Throwable ignore) {
                        }
                    });
                }
            }
        };


        if (callback != null) h.setReadListener(wrapped);
        h.queueAsyncRead(need);                  
        ensureReadIfManual();

        final int t = normalizeTimeout(timeout);
        if (t != NO_TIMEOUT) {
            toRef.set(el.schedule(() -> {
                // If still armed, fail the read
                if (h.isAsyncReadArmed() && nettyChannel.isActive()) {
                    try {
                        wrapped.error(vc, this, new SocketTimeoutException("Read operation timed out"));
                    } catch (Throwable ignore) {}
                }
            }, t, TimeUnit.MILLISECONDS));
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
    }

    @Override
    public WsByteBuffer getBuffer() {
        return (this.buffers == null) ? null : this.buffers[0];
    }

    @Override
    public void setBuffer(WsByteBuffer buf) {
        this.defaultBuffers[0] = null;
        if (buf != null) {
            this.buffers = this.defaultBuffers;
            this.buffers[0] = buf;
        } else {
            this.buffers = null;
        }
    }

    public void setVC(VirtualConnection vc) {
        this.vc = vc;
    }

    private int normalizeTimeout(int timeout) {
        if (timeout == NO_TIMEOUT)
            return NO_TIMEOUT;
        if (timeout == USE_CHANNEL_TIMEOUT)
            return channelDefaultTimeout;
        if (timeout == IMMED_TIMEOUT)
            return IMMED_TIMEOUT;
        if (timeout == ABORT_TIMEOUT)
            return ABORT_TIMEOUT;
        return (timeout <= 0) ? channelDefaultTimeout : timeout;
    }

    private void ensureReadIfManual() {
        if (!nettyChannel.config().isAutoRead())
            nettyChannel.eventLoop().execute(nettyChannel::read);
    }

    private boolean isUpgraded() {
        return nettyChannel.pipeline().get(NettyServletUpgradeHandler.class) != null;
    }

    private NettyServletUpgradeHandler ensureUpgradeHandler() {
        NettyServletUpgradeHandler h = nettyChannel.pipeline().get(NettyServletUpgradeHandler.class);
        if (h == null) {
            throw new IllegalStateException("Channel marked upgraded but no NettyServletUpgradeHandler in pipeline");
        }
        return h;
    }

    private long nonUpgradedImmediateDrain() throws IOException {
        ensureReadIfManual();
        final HttpInputStreamImpl in = input();
        if (buffers == null || buffers.length == 0 || buffers[0] == null) return 0L;

        final int available = Math.max(0, in.available());
        if (available <= 0) return 0L;
        
        int want = 0;
        for(WsByteBuffer buffer: buffers){
            if (buffer == null) break;

            want += buffer.remaining();
        }
        if (want <= 0) return 0L;

        final int toRead = Math.min(available, want);
        final byte[] scratch = new byte[toRead];
        final int n = in.read(scratch, 0, toRead);
        if (n <= 0) return 0L;

        int off = 0;
        for (WsByteBuffer buffer: buffers){
            if (buffer == null || off>=n) break;
            final int can = Math.min(buffer.remaining(), n - off);
            if (can > 0){
                buffer.getWrappedByteBuffer().put(scratch, off, can);
                buffer.position(buffer.position()+can);
                off += can;
            }
        }
        return n;
    }

    private long upgradedImmediateDrain(){
        ensureReadIfManual();
        final NettyServletUpgradeHandler h = ensureUpgradeHandler();
        h.setTCPReadContext(this);
        h.setVC(vc);
        if (!h.containsQueuedData()) return 0L;
        long copied = h.setToBuffer();
        Tr.debug(tc, "UPG sync drain (numBytes==0): copied="+copied);


        return copied;
    }

    private void ensureBuffersOrJIT(long numBytes, boolean isAsync) {
        if (isAsync && numBytes < 1) {
            throw new IllegalArgumentException("Number of bytes requested to read: " + numBytes
                    + " is less than minimum allowed (1 for asynch)");
        }
        if (!isAsync && numBytes < 0) {
            throw new IllegalArgumentException("Number of bytes requested to read: " + numBytes
                    + " is less than minimum allowed (0 for sync)");
        }
        if (buffers == null || buffers.length == 0 || buffers[0] == null) {
            if (jitAllocateSize > 0) {
                WsByteBuffer buf = ChannelFrameworkFactory.getBufferManager().allocateDirect(jitAllocateSize);
                setBuffer(buf);
                jitAllocateAction = true;
            } else {
                throw new IllegalArgumentException("No buffer(s) provided for reading data into");
            }
        } else {
            jitAllocateAction = false;
        }
        long bytesAvail = 0;
        for (WsByteBuffer b : buffers) {
            if (b == null) break;
            bytesAvail += Math.max(0, b.limit() - b.position());
        }
        if (isAsync) {
            long need = Math.max(1L, numBytes);
            if (bytesAvail < need) {
                throw new IllegalArgumentException("Number of bytes requested: " + numBytes
                        + " exceeds space remaining in the buffers provided: " + bytesAvail);
            }
        } else {
            if (bytesAvail == 0) {
                throw new IllegalArgumentException("Number of bytes requested: " + numBytes
                        + " exceeds space remaining in the buffers provided: 0");
            }
            if (numBytes > 0 && numBytes > bytesAvail) {
                throw new IllegalArgumentException("Number of bytes requested: " + numBytes
                        + " exceeds space remaining in the buffers provided: " + bytesAvail);
            }
        }
    }
}