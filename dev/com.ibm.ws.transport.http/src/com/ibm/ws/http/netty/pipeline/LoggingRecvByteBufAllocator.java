/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.pipeline;

import java.net.SocketAddress;
import java.util.Objects;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpMessages;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.RecvByteBufAllocator;

 import io.netty.buffer.ByteBufAllocator;

/**
 * This class is a custom {@link RecvByteBufAllocator} that wraps an existing allocator to intercept buffer
 * allocations in order to log when a read operation is requested.
 */
 public class LoggingRecvByteBufAllocator implements RecvByteBufAllocator{

    private static final TraceComponent tc = Tr.register(LoggingRecvByteBufAllocator.class, "TCPChannel", "Netty");

    private final RecvByteBufAllocator delegate;

    /**
     * Constructor that wraps an existing allocator
     * @param delegate
     */
    public LoggingRecvByteBufAllocator(RecvByteBufAllocator delegate){
        this.delegate = delegate;
    }

    @Override
    public Handle newHandle() {
        return new LoggingHandle(delegate.newHandle());
    }

    public class LoggingHandle implements Handle{
        private final Handle delegateHandle;
        private ChannelHandlerContext context;

        LoggingHandle(Handle delegateHandle){
            this.delegateHandle = delegateHandle;
        }

        public void setChannelHandlerContext(ChannelHandlerContext context){
            this.context = context;
        }

        @Override
        public void reset(ChannelConfig config){
            delegateHandle.reset(config);
        }

        @Override
        public ByteBuf allocate(ByteBufAllocator allocator){
            if (Objects.nonNull(context)){
                SocketAddress localAddress = context.channel().localAddress();
                SocketAddress remoteAddress = context.channel().remoteAddress();

                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(context.channel(), tc, "read (async) requested for local: " + localAddress + " remote: " + remoteAddress);
                }
                System.out.println("read (async) requested for local: " + localAddress + " remote: " + remoteAddress);
            }
            return delegateHandle.allocate(allocator);
        }

        @Override
        public int guess(){
            return delegateHandle.guess();
        }

        @Override
        public void incMessagesRead(int numMessages){
            delegateHandle.incMessagesRead(numMessages);
        }

        @Override
        public void lastBytesRead(int bytes){
            delegateHandle.lastBytesRead(bytes);
        }

        @Override
        public int lastBytesRead(){
            return delegateHandle.lastBytesRead();
        }

        @Override
        public boolean continueReading(){
            return delegateHandle.continueReading();
        }

        @Override
        public void readComplete(){
            delegateHandle.readComplete();
        }

        @Override
        public void attemptedBytesRead(int bytes){
            delegateHandle.attemptedBytesRead(bytes);
        }

        @Override
        public int attemptedBytesRead(){
            return delegateHandle.attemptedBytesRead();
        }
    }
    
}
