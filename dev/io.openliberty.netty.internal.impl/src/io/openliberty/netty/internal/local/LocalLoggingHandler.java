/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.netty.internal.local;

import java.net.SocketAddress; // This is a marker interface rather than an actual TCP Socket

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AttributeKey;
import io.openliberty.netty.internal.impl.NettyConstants;

/**
 * Channel handler which logs TCP connection events to ensure that logging between Netty and Channelfw are 
 * as closely related as possible.
 */
@Trivial
class LocalLoggingHandler extends LoggingHandler{

	private static final TraceComponent tc = Tr.register(LocalUtils.class, NettyConstants.NETTY_TRACE_NAME,
			NettyConstants.BASE_BUNDLE);
	

	public LocalLoggingHandler(){
		super(LocalLoggingHandler.class);
	}
	
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(ctx.channel(), tc, "handler added to" + ctx + ", channel " + ctx.channel());
		}
		//ctx.channel().attr(BYTES_READ_KEY).getAndSet(0);
		super.handlerAdded(ctx);
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(ctx.channel(), tc, "registered Logging Handler for channel: " + ctx.channel());
		}
		ctx.fireChannelRegistered();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            // Note a LocalChannel can still implement the equivalent of remote/local address strings
			Tr.event(ctx.channel(), tc, "SocketChannel accepted, local: " + ctx.channel().localAddress() + " remote: " + ctx.channel().remoteAddress());
		}
		ctx.fireChannelActive();
	}


	@Override
	public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {
		// Used when binding a Channel to a port to accept connections
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(ctx.channel(), tc, "Calling bind, local: " + localAddress);
		}
		super.bind(ctx, localAddress, promise);
	}

	@Override
	public void connect(
			ChannelHandlerContext ctx,
			SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
		// Used when connecting a channel to an endpoint
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(ctx.channel(), tc, "Calling connect, local: " +localAddress + " remote: " + remoteAddress);
		}
		super.connect(ctx, remoteAddress, localAddress, promise);
	}

	@Override
	public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(ctx.channel(), tc, "Channel disconnected, local: " + ctx.channel().localAddress() + " remote: " + ctx.channel().remoteAddress());
		}
		ctx.disconnect(promise);
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(ctx.channel(), tc, "read completed. Read: " /* + ctx.channel().attr(BYTES_READ_KEY).getAndSet(0) + " bytes" */);
		}
		ctx.fireChannelReadComplete();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(ctx.channel(), tc, "read (async) called for local: " + ctx.channel().localAddress() + " remote: " + ctx.channel().remoteAddress()+ " bytes read: "+((ByteBuf) msg).readableBytes());
		}
//		ctx.channel().attr(BYTES_READ_KEY).getAndSet(ctx.channel().attr(BYTES_READ_KEY).get() + ((ByteBuf) msg).readableBytes());
		ctx.fireChannelRead(msg);
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(ctx.channel(), tc, "write (async) requested for local: " + ctx.channel().localAddress() + " remote: " + ctx.channel().remoteAddress()+ (msg instanceof ByteBuf ? " bytes to be written: "+((ByteBuf) msg).readableBytes() : " object to write: " + msg));
		}
		Integer bytesToWrite = ((ByteBuf) msg).readableBytes();
		ctx.write(msg, promise).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if(future.isSuccess() && TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
					Tr.event(ctx.channel(), tc, "write (async) finished sucessfully for local: " + ctx.channel().localAddress() + " remote: " + ctx.channel().remoteAddress()+ " wrote: " + bytesToWrite + " bytes");
				else if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
					Tr.event(ctx.channel(), tc, "write (async) FAILED for local: " + ctx.channel().localAddress() + " remote: " + ctx.channel().remoteAddress()+" did not write : "+ bytesToWrite + " bytes");
			}
		});
	}

	@Override
	public void flush(ChannelHandlerContext ctx) throws Exception {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(ctx.channel(), tc, "flush requested for local: " + ctx.channel().localAddress() + " remote: " + ctx.channel().remoteAddress());
		}
		ctx.flush();
	}
	
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(ctx.channel(), tc, "userEvent triggered for local: " + ctx.channel().localAddress() + " remote: " + ctx.channel().remoteAddress() +" event: " + evt);
		}
		super.userEventTriggered(ctx, evt);
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(ctx.channel(), tc, "SocketChannel closed, local: " + ctx.channel().localAddress() + " remote: " + ctx.channel().remoteAddress());
		}
		ctx.fireChannelInactive();
	}

}
