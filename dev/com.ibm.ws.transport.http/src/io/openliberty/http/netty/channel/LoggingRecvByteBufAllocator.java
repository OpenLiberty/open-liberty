/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.RecvByteBufAllocator;

/**
 * This class is a custom {@link RecvByteBufAllocator} that wraps an existing allocator to intercept buffer
 * allocations in order to log when a read operation is requested.
 */
 public class LoggingRecvByteBufAllocator implements RecvByteBufAllocator{

    private static final TraceComponent tc = Tr.register(LoggingRecvByteBufAllocator.class, "TCPChannel", "Netty");

    private final RecvByteBufAllocator delegate;

    /**
     * Constructor that wraps an existing allocator to add logging in {@code allocate(...)}
     * 
     * @param delegate
     */
    public LoggingRecvByteBufAllocator(RecvByteBufAllocator delegate){
        this.delegate = delegate;
    }

    /**
     * Returns a handle that intercepts the buffer allocation call to log read operations.
     *
     * @return a new {@link Handle}
     */
    @Override
    @SuppressWarnings("deprecation")
    public Handle newHandle() {
        return new LoggingHandle(delegate.newHandle());
    }

    /**
     * A handle that {@link #allocate(ByteBufAllocator)} call in order to log read operations.
     * Delegates all other operations to the wrapped {@link ExtendedHandle}.
     * 
     * Netty has not provided a non-depracated alternative to some calls. In the
     * meantime, the deprecation warnings are supressed.
     */
    @SuppressWarnings("deprecation")
    public class LoggingHandle implements Handle{
        private final Handle delegateHandle;
        private ChannelHandlerContext context;

        @SuppressWarnings("deprecation")
        LoggingHandle(Handle handle){
            this.delegateHandle = handle;
        }

        /**
         * Sets the context for logging addresses. This is expected
         * to be called from the pipeline to ensure the handle can see addresses.
         *
         * @param context the channel handler context
         */
        public void setChannelHandlerContext(ChannelHandlerContext context){
            this.context = context;
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
            if (context != null) {
                SocketAddress localAddress = context.channel().localAddress();
                SocketAddress remoteAddress = context.channel().remoteAddress();

                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(context.channel(), tc, "read (async) requested for local: " + localAddress + " remote: " + remoteAddress);
                }
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
    } 
}
