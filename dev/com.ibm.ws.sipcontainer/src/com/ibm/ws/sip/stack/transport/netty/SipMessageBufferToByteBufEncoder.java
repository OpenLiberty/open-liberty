/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.transport.netty;

import com.ibm.ws.sip.stack.transaction.transport.connections.SipMessageByteBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class SipMessageBufferToByteBufEncoder extends MessageToByteEncoder<SipMessageByteBuffer> {
	@Override
	public void encode(ChannelHandlerContext ctx, SipMessageByteBuffer msg, ByteBuf out) throws Exception {
		out.writeBytes(msg.getBytes(), 0, msg.getMarkedBytesNumber());
		msg.reset();
	}
}

