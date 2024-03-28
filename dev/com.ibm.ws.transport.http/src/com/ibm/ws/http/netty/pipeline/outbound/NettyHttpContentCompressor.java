/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.pipeline.outbound;

import com.ibm.ws.http.netty.MSP;
import com.ibm.ws.http.netty.NettyHttpConstants;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpResponse;

/**
 *
 */
public class NettyHttpContentCompressor extends HttpContentCompressor {

    private ChannelHandlerContext context;

    @Override
    public void handlerAdded(ChannelHandlerContext context) throws Exception {
        this.context = context;
    }

    @Override
    protected Result beginEncode(HttpResponse httpResponse, String acceptEncoding) throws Exception {

        MSP.log("Netty compression encode begin");

        Result result = null;

        if (context.channel().hasAttr(NettyHttpConstants.COMPRESSION_ENCODING)) {

            String targetContentEncoding = context.channel().attr(NettyHttpConstants.COMPRESSION_ENCODING).get();

            MSP.log("targetEncoding: " + targetContentEncoding);

            ZlibWrapper wrapper;

            switch (targetContentEncoding) {
                case "gzip":
                    wrapper = ZlibWrapper.GZIP;
                    break;
                case "deflate":
                    wrapper = ZlibWrapper.ZLIB;
                    break;
                default:
                    throw new Error();
            }

            result = new Result(targetContentEncoding, new EmbeddedChannel(context.channel().id(), context.channel().metadata().hasDisconnect(), context.channel().config(), ZlibCodecFactory.newZlibEncoder(
                                                                                                                                                                                                             wrapper,
                                                                                                                                                                                                             -1,
                                                                                                                                                                                                             -1,
                                                                                                                                                                                                             -1)));
            MSP.log("Netty compression encode end");
        }
        return result;
    }

}
