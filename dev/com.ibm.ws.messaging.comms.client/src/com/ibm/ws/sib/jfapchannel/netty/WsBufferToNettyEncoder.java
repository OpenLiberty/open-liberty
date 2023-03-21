/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.netty;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;
import com.ibm.ws.sib.jfapchannel.richclient.buffer.impl.RichByteBufferImpl;
import com.ibm.ws.sib.utils.ras.SibTr;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Netty encoder for transforming outgoing WsByteBuffer objects to ByteBuf objects
 * that the Netty framework uses to send data.
 * 
 */
public class WsBufferToNettyEncoder extends MessageToByteEncoder<WsByteBuffer> {

	/** Trace */
	private static final TraceComponent tc = SibTr.register(WsBufferToNettyEncoder.class,
			JFapChannelConstants.MSG_GROUP,
			JFapChannelConstants.MSG_BUNDLE);

	/** Log class info on load */
	static
	{
		if (tc.isDebugEnabled())
			SibTr.debug(tc,
					"@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/sib/netty/jfapchannel/WsBufferToNettyEncoder.java, SIB.comms, WASX.SIB, uu1215.01 1.1");
	}

	@Override
	public void encode(ChannelHandlerContext ctx, WsByteBuffer msg, ByteBuf out) throws Exception {
		if (tc.isEntryEnabled())
			SibTr.entry(this, tc, "encode", ctx.channel());
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			SibTr.debug(this, tc, "encode", ctx.channel().remoteAddress() + " encoding message [ " + msg.toString() + " ] from WSByteBuffer to Netty ByteBuf");
		}
		com.ibm.wsspi.bytebuffer.WsByteBuffer wrappedBuffer = (com.ibm.wsspi.bytebuffer.WsByteBuffer)((RichByteBufferImpl)msg).getUnderlyingBuffer();
		out.writeBytes(wrappedBuffer.getWrappedByteBuffer());

		if (tc.isEntryEnabled())
			SibTr.exit(this, tc, "encode", ctx.channel());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (tc.isEntryEnabled())
			SibTr.entry(this, tc, "exceptionCaught", new Object[] {cause, ctx.channel()});
		super.exceptionCaught(ctx, cause);
		if (tc.isEntryEnabled())
			SibTr.exit(this, tc, "exceptionCaught", new Object[] {cause, ctx.channel()});
	}

}
