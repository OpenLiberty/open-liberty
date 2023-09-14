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

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapter;

/**
 *
 */
public class LibertyInboundHttp2ToHttpAdapter extends InboundHttp2ToHttpAdapter {

    protected LibertyInboundHttp2ToHttpAdapter(Http2Connection connection, int maxContentLength, boolean validateHttpHeaders, boolean propagateSettings) {
        super(connection, maxContentLength, validateHttpHeaders, propagateSettings);
    }

    @Override
    @FFDCIgnore(NullPointerException.class)
    // Extended to properly get stream errors when working with header parsing with missing pesudo-headers
    protected io.netty.handler.codec.http.FullHttpMessage processHeadersBegin(ChannelHandlerContext ctx, io.netty.handler.codec.http2.Http2Stream stream,
                                                                              io.netty.handler.codec.http2.Http2Headers headers, boolean endOfStream, boolean allowAppend,
                                                                              boolean appendToTrailer) throws io.netty.handler.codec.http2.Http2Exception {
        try {
            System.out.println("Here go the headers begin!");
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

}
