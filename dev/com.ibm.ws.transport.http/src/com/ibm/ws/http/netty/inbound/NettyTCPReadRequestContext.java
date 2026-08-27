/*******************************************************************************
 * Copyright (c) 2023, 2026 IBM Corporation and others.
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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.http.channel.internal.AsyncReadDispatchState;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.inbound.HttpInputStreamImpl;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.ws.http.netty.NettyHttpChannelConfig;
import com.ibm.ws.http.netty.NettyHttpConstants;
import com.ibm.ws.http.netty.pipeline.inbound.read.ReadFlowHandler;
import com.ibm.ws.http.netty.pipeline.inbound.HttpDispatcherHandler;
import com.ibm.ws.netty.upgrade.NettyServletUpgradeHandler;
import com.ibm.ws.transport.access.TransportConstants;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.http.HttpInputStream;
import com.ibm.wsspi.http.ee7.HttpInputStreamEE7;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;

import io.openliberty.http.options.TcpOption;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.EventExecutor;

//autoread design, will organize imports later
import java.util.concurrent.CompletableFuture;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import io.openliberty.http.netty.channel.ReadOnlySocket;
import io.openliberty.http.options.TcpOption;



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
    private int channelTimeout;

    private volatile Socket cachedSocket;
    private NettyHttpChannelConfig config;

    public NettyTCPReadRequestContext(NettyTCPConnectionContext connectionContext, Channel nettyChannel, NettyHttpChannelConfig config) {
        this.connectionContext = connectionContext;
        this.nettyChannel = nettyChannel;
        this.config = config;
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
        if(cachedSocket == null){
            Optional<Socket> socket = Optional.ofNullable(this.nettyChannel.attr(NettyHttpConstants.SOCKET_HANDLE).get());
            cachedSocket = socket.orElse(new ReadOnlySocket(nettyChannel));     
        }
        return cachedSocket;
    }

    private HttpInputStreamImpl input() throws IOException {
    
        HttpInputStreamImpl in = null;

        if (vc != null) {
            Object candidate = vc.getStateMap().get(NettyHttpConstants.VC_HTTP_INPUT_STREAM);
            if (candidate instanceof HttpInputStreamImpl) {
                in = (HttpInputStreamImpl) candidate;
            }
            if (in == null) {
                Object sid = vc.getStateMap().get(NettyHttpConstants.VC_HTTP2_STREAM_ID);
                if (sid instanceof String) {
                    HttpDispatcherHandler disp =
                            nettyChannel.pipeline().get(HttpDispatcherHandler.class);
                    if (disp != null) {
                        HttpInputStream s = disp.getStream((String) sid);
                        if (s instanceof HttpInputStreamImpl) {
                            in = (HttpInputStreamImpl) s;
                            // Optionally cache it on VC for next time:
                            vc.getStateMap().put(NettyHttpConstants.VC_HTTP_INPUT_STREAM, in);
                        }
                    }
                }
            }
        }

        if (in == null) {
            throw new IOException("HTTP input stream not initialized for channel " + nettyChannel);
        }
        return in;
    }


    @Override
    public long read(long numBytes, int timeout) throws IOException {
        if(nettyChannel.eventLoop().inEventLoop()){
            throw new IllegalStateException("Blocking read cannot be done in Netty thread");
        }

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

        final boolean logicalUpg = isLogicallyUpgraded();
        final boolean handlerReady = hasUpgradeHandler();

        // If we're logically upgraded but the upgrade handler is not yet in place,
        // DO NOT touch HttpInputStreamImpl. Just report "no data" for now.
        if (logicalUpg && !handlerReady) {
            //return 0L;

            final int effectiveTimeout = normalizeTimeout(timeout);
            if(effectiveTimeout != IMMED_TIMEOUT && effectiveTimeout != ABORT_TIMEOUT){
                awaitUpgradePipeline(effectiveTimeout);
                if(!hasUpgradeHandler()){
                    return 0L;
                }
            }

        }

        if (timeout == IMMED_TIMEOUT) {
            if (hasUpgradeHandler())
                ensureUpgradeHandler().immediateTimeout();
            return 0L;
        }

        if (timeout == ABORT_TIMEOUT) {
            aborted = true;
            if (hasUpgradeHandler())
                ensureUpgradeHandler().immediateTimeout();
            return 0L;
        }

        ensureBuffersOrJIT(numBytes, false);

        if(numBytes == 0){
            return hasUpgradeHandler() ? upgradedImmediateDrain() : nonUpgradedImmediateDrain();
        }

        return hasUpgradeHandler() ? upgradedSyncRead(numBytes, timeout) : nonUpgradedSyncRead(numBytes, timeout);
    }

    private long nonUpgradedSyncRead(long numBytes, int timeout) throws IOException {
        final HttpInputStreamImpl in = input();
        if (buffers == null || buffers.length == 0 || buffers[0] == null)
            throw new IOException("Buffers not set for read()");

        if (numBytes <= 0) {
            int available;
            try {
                available = Math.max(0, in.available());
            } catch (IOException ioe) {
                available = 0;
            }
            if (available <= 0)
                return 0L;

            int capacity = 0;
            for (WsByteBuffer buffer : buffers) {
                if (buffer == null)
                    break;
                capacity += buffer.remaining();
            }
            final int chunk = Math.min(8192, Math.min(available, capacity));
            final byte[] temp = new byte[chunk];
            final int n = in.read(temp, 0, chunk);
            if (n > 0) {
                int off = 0;
                for (WsByteBuffer buffer : buffers) {
                    if (buffer == null || off >= n)
                        break;
                    off += copyInto(buffer, temp, off, n - off);
                }
                return n;
            }
            return 0L;
        }

        final int effectiveTimeout = normalizeTimeout(timeout);
        final long deadlineNs = (effectiveTimeout == NO_TIMEOUT) ? Long.MAX_VALUE : System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(effectiveTimeout);

        
        requestRead();

        final byte[] scratch = new byte[8192];
        long delivered = 0L;

        while (true) {
            int target = 0;
            for (WsByteBuffer b : buffers) {
                if (b == null)
                    break;
                target += b.remaining();
            }
            if (numBytes > 0) {
                    target = (int) Math.min(target, Math.max(0, numBytes - delivered));
            }
            if (target == 0)
                return delivered;

            final int chunk = Math.min(target, scratch.length);
            final int n = in.read(scratch, 0, chunk);

            if (n > 0) {
                int off = 0;
                for (WsByteBuffer b : buffers) {
                    if (b == null || off >= n)
                        break;
                    off += copyInto(b, scratch, off, n - off);
                }
                delivered += n;
                if (numBytes > 0 && delivered >= numBytes){
                    return delivered;
                }
                    
                continue;
            }
            if (n == -1){
                return delivered;
            }

            requestRead();

            if (deadlineNs != Long.MAX_VALUE && System.nanoTime() > deadlineNs) {
                throw new SocketTimeoutException("sync timeout; delivered=" + delivered
                                                     + " need=" + numBytes + " bufRemain=" + remaining(buffers));
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));            
        } 
    }

    private long upgradedSyncRead(long numBytes, int timeout) throws IOException {
        final NettyServletUpgradeHandler h = ensureUpgradeHandler();
        h.setTCPReadContext(this);
        h.setVC(vc);

        if (numBytes <= 0) {
            requestRead();
            return h.containsQueuedData() ? h.setToBuffer() : 0L;
        }

        final int t = normalizeTimeout(timeout);
        final long need = Math.max(1L, numBytes);
        final long deadlineNs = (t == NO_TIMEOUT) ? Long.MAX_VALUE : System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(t);

         
      
            requestRead();

    
            if (h.containsQueuedData() && h.queuedDataSize() >= need) {
                long copied = h.setToBuffer();
                return copied;
            }

            while (nettyChannel.isActive()) {
                final long now = System.nanoTime();
                if (deadlineNs != Long.MAX_VALUE && now >= deadlineNs) {
                    throw new SocketTimeoutException("Failed to read data within the specified timeout.");
                }
                final long remainingMs = (deadlineNs == Long.MAX_VALUE) ? 250L : Math.max(1L, TimeUnit.NANOSECONDS.toMillis(deadlineNs - now));
                try {
                    h.waitForDataRead(Math.min(remainingMs, 250L)); 
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for upgraded read data.", ie);
                }

                if (h.containsQueuedData() && h.queuedDataSize() >= need) {
                    long copied = h.setToBuffer();
                    Tr.debug(tc, "(Fast path) UPG sync read: need=" + need + " copied=" + copied + " queuedAfter=" + h.queuedDataSize());
                    return copied;
                }

                requestRead();
            }
            throw new EOFException("Channel inactive during read");
        
    }

    @Override
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

        boolean logicalUpg = isLogicallyUpgraded();
        boolean handlerReady = hasUpgradeHandler();
        final int effectiveTimeout = normalizeTimeout(timeout);

        if (logicalUpg && handlerReady) {

            
            if (effectiveTimeout != IMMED_TIMEOUT && effectiveTimeout != ABORT_TIMEOUT) {
                ensureBuffersOrJIT(numBytes, true);
            }
            return upgradedAsyncRead(numBytes, callback, forceQueue, timeout);
        }

        if (logicalUpg && !handlerReady) {
            installAsyncHttpReadCallbacks(numBytes, callback, effectiveTimeout);


            if (effectiveTimeout != IMMED_TIMEOUT && effectiveTimeout != ABORT_TIMEOUT) {
                awaitUpgradePipeline(effectiveTimeout);
            }
            if(hasUpgradeHandler()){
                signalAndDispatchAsyncRead();
            }
            return null;
        }

        if (effectiveTimeout == IMMED_TIMEOUT)
            return null;
        if (effectiveTimeout == ABORT_TIMEOUT) {
            aborted = true;
            return null;
        }

        ensureBuffersOrJIT(numBytes, true);

        installAsyncHttpReadCallbacks(numBytes, callback, effectiveTimeout);

        if(!isLogicallyUpgraded()){
            try {
                HttpInputStreamImpl in2 = input();
                boolean isEE7 = (in2 instanceof HttpInputStreamEE7);
                if (in2.available() > 0 || (isEE7 && ((HttpInputStreamEE7) in2).isFinished())) {
                    signalAndDispatchAsyncRead();
                }
            } catch (IOException ignore) { }

            requestRead();
            return null;
        }
        return null;
    }

    private void signalAndDispatchAsyncRead() {
        AsyncReadDispatchState.forChannel(nettyChannel).signal();
    }

    public VirtualConnection upgradedAsyncRead(long numBytes, TCPReadCompletedCallback callback, boolean forceQueue, int timeout) {
        final NettyServletUpgradeHandler h = ensureUpgradeHandler();
        h.setTCPReadContext(this);
        h.setVC(vc);

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

        final io.netty.util.concurrent.EventExecutor el = nettyChannel.eventLoop();
        final java.util.concurrent.atomic.AtomicReference<io.netty.util.concurrent.ScheduledFuture<?>> toRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        final AtomicBoolean delivered = new AtomicBoolean(false);

        final TCPReadCompletedCallback wrapped = new TCPReadCompletedCallback() {
            
            @Override
            public void complete(VirtualConnection v, TCPReadRequestContext ctx) {
                if (!delivered.compareAndSet(false, true)) {
                    return;
                }

                io.netty.util.concurrent.ScheduledFuture<?> f = toRef.getAndSet(null);
                if (f != null){
                    try {
                        f.cancel(false);
                    } catch (Throwable ignore) {
                    }
                }
                    
                h.setToBuffer();
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
                if (!delivered.compareAndSet(false, true)) {
                    return;
                }
                io.netty.util.concurrent.ScheduledFuture<?> f = toRef.getAndSet(null);
                if (f != null)
                    try {
                        f.cancel(false);
                    } catch (Throwable ignore) {
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

        h.setReadListener((callback!=null) ? wrapped : null);


        h.queueAsyncRead(need);

        final int t = normalizeTimeout(timeout);
        if (t != NO_TIMEOUT) {
            toRef.set(el.schedule(() -> {
                if (h.isAsyncReadArmed() && nettyChannel.isActive()) {
                    try {
                        wrapped.error(vc, this, new SocketTimeoutException("Read operation timed out"));
                    } catch (Throwable ignore) {
                    }
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

    private boolean isLogicallyUpgraded() {

        if (hasUpgradeHandler()) {
            return true;
        }

        if(vc == null){
            return false;
        }
        
        Object flag = vc.getStateMap().get(TransportConstants.UPGRADED_CONNECTION);
        if ("true".equalsIgnoreCase(String.valueOf(flag))) {
            return true;
        }
        flag = vc.getStateMap().get(TransportConstants.UPGRADED_LISTENER);
        if ("true".equalsIgnoreCase(String.valueOf(flag))) {
            return true;
        }

        flag = vc.getStateMap().get(TransportConstants.CLOSE_NON_UPGRADED_STREAMS);
        if (flag != null && !"false".equalsIgnoreCase(String.valueOf(flag))) {
            // Values like "true" or "CLOSED_NON_UPGRADED_STREAMS" mean this VC
            // is in an upgrade scenario where the normal HTTP request is done.
            return true;
        }

        return false;
    }

    private boolean hasReadFlowHandler() {
        return nettyChannel.pipeline().get(ReadFlowHandler.class) != null;
    }

    private boolean hasUpgradeHandler() {
        return nettyChannel.pipeline().get(NettyServletUpgradeHandler.class) != null;
    }

    private NettyServletUpgradeHandler ensureUpgradeHandler() {
        NettyServletUpgradeHandler h = nettyChannel.pipeline().get(NettyServletUpgradeHandler.class);
        if(h != null) return h;

        // //TODO lazy initialization due to wsoc not triggering upgrade event. Find missing location to throw event .
        // if(isWsocUpgrade()){
        //     Tr.debug(tc, "Installing upgrade handler for WSOC upgrade");
        //     h = new NettyServletUpgradeHandler(nettyChannel);
        //     h.setTCPReadContext(this);
        //     h.setVC(vc);
        //     if(nettyChannel.pipeline().get("ServletUpgradeHandler") == null){
        //         nettyChannel.pipeline().addLast("ServletUpgradeHandler", h);
        //     }
        //     return h;
        //}
        //if (h == null) {

            //Dispatcher must install it, this is a bad state meaning we did not get upgrade signal in 
            //the dispatcher.
            throw new IllegalStateException("Channel marked upgraded but no NettyServletUpgradeHandler in pipeline");
       // }
        //return h;
    }

    private boolean isWsocUpgrade(){
        if(vc == null){
            return false;
        }
        Object upgradeConn = vc.getStateMap().get(com.ibm.ws.transport.access.TransportConstants.UPGRADED_CONNECTION);
        Object webConn = vc.getStateMap().get(com.ibm.ws.transport.access.TransportConstants.UPGRADED_WEB_CONNECTION_OBJECT);
        return "true".equalsIgnoreCase(String.valueOf(upgradeConn)) && webConn != null;
    }

    private long nonUpgradedImmediateDrain() throws IOException {
        requestRead();

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
        ByteBuffer bb;
        for (WsByteBuffer buffer: buffers){
            if (buffer == null || off>=n) break;
            off += copyInto(buffer, scratch, off, n - off);
        }

        return n;
    }

    private long upgradedImmediateDrain(){
        requestRead();
        final NettyServletUpgradeHandler h = ensureUpgradeHandler();
        h.setTCPReadContext(this);
        h.setVC(vc);
        if (!h.containsQueuedData()) return 0L;
        long copied = h.setToBuffer();

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

    private static int copyInto(WsByteBuffer buf, byte[] src, int off, int len) {
        final java.nio.ByteBuffer bb = buf.getWrappedByteBuffer();
        final int can = Math.min(bb.remaining(), len);
        if (can > 0) {
            bb.put(src, off, can);
            buf.position(bb.position()); 
        }
        return can;
    }

    private static long remaining(WsByteBuffer[] bufs) {
        long tot = 0;
        if (bufs == null)
            return 0;
        for (WsByteBuffer b : bufs) {
            if (b == null)
                break;
            tot += Math.max(0, b.remaining());
        }
        return tot;
    }  

    private ChannelHandlerContext readFlowContext(){
        return nettyChannel.pipeline().context(ReadFlowHandler.class);
    }

    private void requestRead(){
        ChannelHandlerContext context = readFlowContext();
        if (context != null){
            Tr.debug(tc, "[READGATE] requestRead via ReadFlowHandler");
            ReadFlowHandler.requestRead(context);
            return;
        }

        NettyServletUpgradeHandler upgradeHandler = nettyChannel.pipeline().get(NettyServletUpgradeHandler.class);
        if(upgradeHandler != null){
            Tr.debug(tc, "[READGATE] requestRead via NettyServletUpgradeHandler");
            upgradeHandler.requestReadIfNeeded();
            return;
        }

        if(!nettyChannel.config().isAutoRead() && nettyChannel.isActive()){
            nettyChannel.eventLoop().execute(nettyChannel::read);
        }
    }

    private boolean awaitUpgradePipeline(int timeout){
        CompletableFuture<Void> promise = nettyChannel.attr(NettyHttpConstants.UPGRADE_READY_PROMISE).get();
        if(promise == null){
            return false;
        }
        if(promise.isDone()){
            return true;
        }
        if(nettyChannel.eventLoop().inEventLoop()){
            Tr.debug(tc," CRITICAL ERROR: waiting on upgrade on netty thread");
            return false;
        }

        //TODO -> find a way to cleanly handle timing for now block for 5s, this should be 
        //more than enough time to handle it or report the issue. 
        try {
            promise.get(5, TimeUnit.SECONDS);
            return true;
        } catch (Exception e){
            return false;
        }
    }

    private void installAsyncHttpReadCallbacks(long numBytes,
                                               TCPReadCompletedCallback callback,
                                               int effectiveTimeout) {
        final AtomicBoolean delivered = new AtomicBoolean(false);
        final AsyncReadDispatchState state = AsyncReadDispatchState.forChannel(nettyChannel);
        Runnable success = () -> {
            if (!delivered.compareAndSet(false, true)) {
                return;
            }
            nettyChannel.attr(NettyHttpConstants.ASYNC_STREAM_READ).set(Boolean.TRUE);
            try {
                boolean inputShutdownPending;
                try {
                    read(numBytes, effectiveTimeout);
                    inputShutdownPending = Boolean.TRUE.equals(nettyChannel.attr(NettyHttpConstants.INPUT_SHUTDOWN_PENDING).get());
                } catch (Throwable t) {
                    if (callback != null) {
                        callback.error(
                            vc,
                            this,
                            (t instanceof EOFException) ? (EOFException) t : new EOFException(t.toString())
                        );
                    }
                    return;
                }
                if (callback != null) {
                    if(inputShutdownPending){
                        callback.error(vc, this, new EOFException("Peer input shutdown before request body completed. local="
                                    + nettyChannel.localAddress() + " remote=" + nettyChannel.remoteAddress()));
                    } else{
                        callback.complete(vc, this);
                    }
                    
                }
            } finally {
                nettyChannel.attr(NettyHttpConstants.ASYNC_STREAM_READ).set(Boolean.FALSE);
                nettyChannel.attr(NettyHttpConstants.INPUT_SHUTDOWN_PENDING).set(Boolean.FALSE);
            }
        };

        Runnable error = () -> {
            if (!delivered.compareAndSet(false, true)) {
                return;
            }
            try {
                if (callback != null) {
                    callback.error(
                        vc,
                        this,
                        new EOFException(
                            "Peer input shutdown before request body completed. local="
                                + nettyChannel.localAddress()
                                + " remote="
                                + nettyChannel.remoteAddress()
                        )
                    );
                }
            } finally {
                nettyChannel.attr(NettyHttpConstants.ASYNC_STREAM_READ).set(Boolean.FALSE);
            }
        };

        state.arm(success, error);
    }

}