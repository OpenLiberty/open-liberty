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

import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sip.stack.transaction.transport.connections.SipMessageByteBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;

public final class SipMessageBufferDatagramDecoder extends MessageToMessageDecoder<DatagramPacket> {

    private static final TraceComponent tc = Tr.register(SipMessageBufferDatagramDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket packet, List<Object> out) throws Exception {
        final ByteBuf content = packet.content();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "new packet length: " + content.readableBytes() + " for datagram: " + packet
                    + " and buffer: " + content);
        }

        if (content.readableBytes() < 20) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "ignoring message with length less than 20");
            }
            return;
        }

        final byte[] b = new byte[content.readableBytes()];
        content.getBytes(0, b);

        SipMessageByteBuffer data = SipMessageByteBuffer.fromPool();
        data.put(b, 0, b.length);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "new SipMessageByteBuffer length: " + data.getMarkedBytesNumber());
        }

        // with UDP we don't get the remote address in
        // SimpleChannelInboundHandler.channelActive()
        // so this is our chance to get it
        SipMessageEvent sipEvent = new SipMessageEvent(data, packet.sender());
        out.add(sipEvent);
    }
}