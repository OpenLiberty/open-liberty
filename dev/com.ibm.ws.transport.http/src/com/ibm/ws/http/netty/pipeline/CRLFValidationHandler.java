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

import com.ibm.ws.http.netty.NettyHttpConstants;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

public class CRLFValidationHandler extends ChannelInboundHandlerAdapter {

    private static final int MAX_CRLF_ALLOWED = 2;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof ByteBuf){
            ByteBuf buffer = (ByteBuf) msg;
            buffer.markReaderIndex();

            int leadingCRFLCount = 0;
            boolean nonCRLFFound = false;
            byte b;
            byte nextByte;

            while(buffer.isReadable() && !nonCRLFFound){
                b = buffer.readByte();

                if(b == '\r'){
                    if(buffer.isReadable()){
                        nextByte = buffer.readByte();
                        if(nextByte == '\n'){
                            leadingCRFLCount++;
                            if (leadingCRFLCount > MAX_CRLF_ALLOWED){
                                ctx.channel().attr(NettyHttpConstants.THROW_FFDC).set(true);
                                throw new IllegalArgumentException("Too many leading CRLF characters");
                            }
                        } else {
                            nonCRLFFound = true;
                            buffer.readerIndex(buffer.readerIndex() -1);
                        }
                    } else{
                        nonCRLFFound = true;
                    }
                } else{
                    nonCRLFFound = true;
                }
            }

            buffer.resetReaderIndex();
            super.channelRead(ctx, msg);
        } else {
            super.channelRead(ctx, msg);
        }
    }


}
