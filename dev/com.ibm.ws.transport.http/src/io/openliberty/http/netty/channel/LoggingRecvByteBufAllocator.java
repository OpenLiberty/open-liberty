/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.netty.channel;

import java.net.SocketAddress;
import java.util.Objects;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpMessages;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.RecvByteBufAllocator;

import io.netty.util.UncheckedBooleanSupplier;
/**
 * This class is a custom {@link RecvByteBufAllocator} that wraps an existing allocator to intercept buffer
 * allocations in order to log when a read operation is requested.
 */
 public class LoggingRecvByteBufAllocator implements RecvByteBufAllocator{

    private static final TraceComponent tc = Tr.register(LoggingRecvByteBufAllocator.class, "TCPChannel", "Netty");

    private final RecvByteBufAllocator delegate;
    private final Channel channel;

    /**
     * Constructor that wraps an existing allocator to add logging in {@code allocate(...)}
     * 
     * @param delegate
     */
    public LoggingRecvByteBufAllocator(RecvByteBufAllocator delegate, Channel channel){
        Objects.requireNonNull(delegate, "RecvByteBufAllocator must not be null");
        Objects.requireNonNull(channel, "Channel must not be null");
        this.delegate = delegate;
        this.channel = channel;
    }

    /**
     * Returns a handle that intercepts the buffer allocation call to log read operations.
     *
     * @return a new {@link Handle}
     */
    @Override
    @SuppressWarnings("deprecation")
    public ExtendedHandle newHandle() {
        Handle handle = delegate.newHandle();
        return new LoggingHandle((ExtendedHandle) handle);
    }

    /**
     * A handle that {@link #allocate(ByteBufAllocator)} call in order to log read operations.
     * Delegates all other operations to the wrapped {@link ExtendedHandle}.
     * 
     * Netty has not provided a non-deprecated alternative to some calls. In the
     * meantime, the deprecation warnings are supressed.
     */
    @SuppressWarnings("deprecation")
    public class LoggingHandle implements ExtendedHandle {
        private final ExtendedHandle delegateHandle;
        private ChannelHandlerContext context;

        @SuppressWarnings("deprecation")
        LoggingHandle(ExtendedHandle handle){
            this.delegateHandle = handle;
        }

        /**
         * Logs the read request, then delegates allocation.
         *
         * @param allocator the {@link ByteBufAllocator}
         * @return the allocated {@link ByteBuf}
         */
        @Override
        @SuppressWarnings("deprecation")
        public ByteBuf allocate(ByteBufAllocator allocator) {
            SocketAddress localAddress = channel.localAddress();
            SocketAddress remoteAddress = channel.remoteAddress();

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(channel, tc, "read (async) requested for local: " + localAddress + " remote: " + remoteAddress);
            }
            return delegateHandle.allocate(allocator);
        }

        @Override
        @SuppressWarnings("deprecation")
        public void reset(ChannelConfig config){
            delegateHandle.reset(config);
        }

        @Override
        @SuppressWarnings("deprecation")
        public int guess(){
            return delegateHandle.guess();
        }

        @Override
        @SuppressWarnings("deprecation")
        public void incMessagesRead(int numMessages){
            delegateHandle.incMessagesRead(numMessages);
        }

        @Override
        @SuppressWarnings("deprecation")
        public void lastBytesRead(int bytes){
            delegateHandle.lastBytesRead(bytes);
        }

        @Override
        @SuppressWarnings("deprecation")
        public int lastBytesRead(){
            return delegateHandle.lastBytesRead();
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean continueReading(){
            return delegateHandle.continueReading();
        }

        @Override
        @SuppressWarnings("deprecation")
        public void readComplete(){
            delegateHandle.readComplete();
        }

        @Override
        @SuppressWarnings("deprecation")
        public void attemptedBytesRead(int bytes){
            delegateHandle.attemptedBytesRead(bytes);
        }

        @Override
        @SuppressWarnings("deprecation")
        public int attemptedBytesRead(){
            return delegateHandle.attemptedBytesRead();
        }

        @Override
        @SuppressWarnings("deprecation") // is SuppressWarnings needed?
        public boolean continueReading(UncheckedBooleanSupplier maybeMoreDataSupplier) {
            return delegateHandle.continueReading(maybeMoreDataSupplier);
        }

    } 
}
