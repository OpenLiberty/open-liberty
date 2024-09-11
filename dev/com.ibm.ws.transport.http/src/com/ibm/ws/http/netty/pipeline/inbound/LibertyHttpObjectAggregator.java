package com.ibm.ws.http.netty.pipeline.inbound;

import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;

public class LibertyHttpObjectAggregator extends SimpleChannelInboundHandler<HttpObject> {

    private long maxContentLength = Long.MAX_VALUE;

    // AttributeKey to store the current HttpRequest in progress
    private static final AttributeKey<HttpRequest> CURRENT_REQUEST = AttributeKey.valueOf("currentRequest");

    // AttributeKey to store the current composite content
    private static final AttributeKey<CompositeByteBuf> COMPOSITE_CONTENT = AttributeKey.valueOf("compositeContent");

    public LibertyHttpObjectAggregator(long maxContentLength) {
        if (maxContentLength <= 0) {
            throw new IllegalArgumentException("maxContentLength must be a positive integer.");
        }
        this.maxContentLength = maxContentLength;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg.decoderResult().isFinished() && msg.decoderResult().isFailure()) {
            exceptionCaught(ctx, msg.decoderResult().cause());
        }
        if (msg instanceof FullHttpRequest) {
            // Already have a Full HTTP Request so just need to forward here
            ctx.fireChannelRead(ReferenceCountUtil.retain(msg, 1));

            ctx.channel().attr(COMPOSITE_CONTENT).set(null);
            ctx.channel().attr(CURRENT_REQUEST).set(null);
        }
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            ctx.channel().attr(CURRENT_REQUEST).set(request);

            CompositeByteBuf content = ctx.alloc().compositeBuffer();
            ctx.channel().attr(COMPOSITE_CONTENT).set(content);
        } else if (msg instanceof HttpContent) {
            CompositeByteBuf content = ctx.channel().attr(COMPOSITE_CONTENT).get();
            if (content != null) {
                HttpContent httpContent = (HttpContent) msg;
                int sizeOfCurrentChunk = httpContent.content().readableBytes();

                if (sizeOfCurrentChunk > maxContentLength ||
                    (content.readableBytes() + sizeOfCurrentChunk) > maxContentLength) {
                    ReferenceCountUtil.release(msg);
                    throw new TooLongFrameException("Content length exceeded max of " + maxContentLength + " bytes.");
                }

                content.addComponent(true, httpContent.content().retain());

                if (msg instanceof LastHttpContent) {
                    HttpRequest request = ctx.channel().attr(CURRENT_REQUEST).get();

                    FullHttpRequest fullRequest = new DefaultFullHttpRequest(request.protocolVersion(), request.method(), request.uri(), content);
                    fullRequest.headers().set(request.headers());
                    fullRequest.trailingHeaders().set(((LastHttpContent) msg).trailingHeaders());

                    ctx.fireChannelRead(fullRequest);

                    ctx.channel().attr(COMPOSITE_CONTENT).set(null);
                    ctx.channel().attr(CURRENT_REQUEST).set(null);
                }
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.channel().attr(COMPOSITE_CONTENT).set(null);
        ctx.channel().attr(CURRENT_REQUEST).set(null);
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().attr(COMPOSITE_CONTENT).set(null);
        ctx.channel().attr(CURRENT_REQUEST).set(null);
        super.channelInactive(ctx);
    }
}
