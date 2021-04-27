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
package io.openliberty.netty.internal.codec;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Convert WsByteBuffers to ByteBufs 
 */
public class WsByteBufferToByteBufEncoder extends MessageToByteEncoder<WsByteBuffer> {
    @Override
    public void encode(ChannelHandlerContext ctx, WsByteBuffer msg, ByteBuf out) throws Exception {
        out.writeBytes(msg.getWrappedByteBuffer());
    }
}