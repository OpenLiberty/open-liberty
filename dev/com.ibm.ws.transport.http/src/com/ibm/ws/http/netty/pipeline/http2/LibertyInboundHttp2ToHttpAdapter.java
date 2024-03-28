/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.pipeline.http2;

import java.util.Objects;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapter;

/**
 *
 */
public class LibertyInboundHttp2ToHttpAdapter extends InboundHttp2ToHttpAdapter {

    private final Channel channel;

    protected LibertyInboundHttp2ToHttpAdapter(Http2Connection connection, int maxContentLength, boolean validateHttpHeaders, boolean propagateSettings, Channel channel) {
        super(connection, maxContentLength, validateHttpHeaders, propagateSettings);
        this.channel = channel;
    }

    @Override
    @FFDCIgnore(NullPointerException.class)
    // Extended to properly get stream errors when working with header parsing with missing pesudo-headers
    protected io.netty.handler.codec.http.FullHttpMessage processHeadersBegin(ChannelHandlerContext ctx, io.netty.handler.codec.http2.Http2Stream stream,
                                                                              io.netty.handler.codec.http2.Http2Headers headers, boolean endOfStream, boolean allowAppend,
                                                                              boolean appendToTrailer) throws io.netty.handler.codec.http2.Http2Exception {
        try {
            System.out.println("Here go the headers begin!");
            System.out.println(headers.method());
            System.out.println(HttpMethod.CONNECT.asciiName());
            boolean containsPath = Objects.nonNull(headers.path()) && !headers.path().toString().isEmpty();
            boolean containsScheme = Objects.nonNull(headers.scheme()) && !headers.scheme().toString().isEmpty();
            if (headers.method().toString().equalsIgnoreCase(HttpMethod.CONNECT.asciiName().toString())) {
                System.out.println("Found connect method!");
                if (containsPath || containsScheme || Objects.isNull(headers.authority()))
                    throw new NullPointerException("Connect method request must omit path and scheme values!");
            } else {
                if (!containsPath)
                    throw new NullPointerException("Request path must have a value!");
                if (!containsScheme)
                    throw new NullPointerException("Request scheme must have a value!");
            }
            return super.processHeadersBegin(ctx, stream, headers, endOfStream, allowAppend, appendToTrailer);
        } catch (NullPointerException e) {
            System.out.println("Got null processing headers! Sending streamError");
            throw Http2Exception.streamError(stream.id(), Http2Error.PROTOCOL_ERROR, e.getMessage());
        } catch (Exception e2) {
            System.out.println("Uncatched Issue with processing headers");
            e2.printStackTrace();
            throw e2;
        }
    }

    @Override
    public void onGoAwayReceived(int lastStreamId, long errorCode, ByteBuf debugData) {
        // TODO Auto-generated method stub
        super.onGoAwayReceived(lastStreamId, errorCode, debugData);
        channel.close();
    }

    @Override
    public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode) throws Http2Exception {
        // TODO Auto-generated method stub
//        super.onRstStreamRead(ctx, streamId, errorCode);
        Http2Stream stream = connection.stream(streamId);
        FullHttpMessage msg = getMessage(stream);
        if (msg != null) {
            onRstStreamRead(stream, msg);
        }
        Http2Error code = Http2Error.valueOf(errorCode);
        if (Objects.isNull(code)) {
            System.out.println("Found NULL code, treating as internal error!");
            code = Http2Error.INTERNAL_ERROR;
        }
        ctx.fireExceptionCaught(Http2Exception.streamError(streamId, code,
                                                           "HTTP/2 to HTTP layer caught stream reset"));

    }

}
