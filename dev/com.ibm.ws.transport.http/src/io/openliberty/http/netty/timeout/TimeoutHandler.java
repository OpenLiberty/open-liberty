/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.netty.timeout;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.netty.NettyHttpChannelConfig;
import com.ibm.ws.http.netty.NettyHttpConstants;
import com.ibm.ws.http.netty.NettyHttpConstants.ProtocolName;
import com.ibm.ws.http.netty.pipeline.inbound.HttpDispatcherHandler;

import io.openliberty.http.netty.timeout.exception.H2IdleTimeoutException;
import io.openliberty.http.netty.timeout.exception.PersistTimeoutException;
import io.openliberty.http.netty.timeout.exception.ReadTimeoutException;
import io.openliberty.http.netty.timeout.exception.TimeoutException;
import io.openliberty.http.options.TcpOption;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.util.AsciiString;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.ScheduledFuture;

public class TimeoutHandler extends ChannelDuplexHandler{

    private static final TraceComponent tc = Tr.register(TimeoutHandler.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    public static String NAME = "timeoutHandler";

    private enum Phase {OFF, TCP_IDLE, READ, PERSIST, H2_IDLE}
    private Phase phase = Phase.OFF;


    private static final TimeUnit LEGACY_UNIT = TimeUnit.MILLISECONDS;
    private ChannelHandlerContext parentContext;

    private  int readTimeout;
    private  int persistTimeout;
    private  int inactivityTimeout;
    private  int h2InactivityTimeout;
    private final boolean streamOnly;

    private final boolean useKeepAlive;
    private boolean clientRequestedKeepAlive = false;
    private boolean serverKeepAlive = false;
    
        private boolean firstRequest = true;
        private boolean readRetried = false;
    
        private ScheduledFuture<?> currentTimeout;
    
    
        public TimeoutHandler(NettyHttpChannelConfig config) {
            
            this(config, false);
        }
    
        public TimeoutHandler(NettyHttpChannelConfig config, boolean streamOnly){
            this.readTimeout = config.getReadTimeout();
            this.persistTimeout = config.getPersistTimeout();
            this.h2InactivityTimeout = config.getH2ConnectionIdleTimeout();
            this.inactivityTimeout = (int) config.get(TcpOption.INACTIVITY_TIMEOUT);
            this.useKeepAlive = config.isKeepAliveEnabled();
            this.streamOnly = streamOnly;
        }
    
        public static TimeoutHandler forH2Stream(NettyHttpChannelConfig config){
            return new TimeoutHandler(config, true);
        }
    
        private int timeoutForPhase(Phase p){
            switch(p){
                case TCP_IDLE:  return inactivityTimeout;
                case READ: return readTimeout > 0 ? readTimeout:inactivityTimeout;
                case PERSIST: return persistTimeout > 0 ? persistTimeout: inactivityTimeout;
                case H2_IDLE: return h2InactivityTimeout;
                default: return 0;
    
            }
        }
    
        @Override
        public void handlerAdded(ChannelHandlerContext context){
            this.parentContext = context;

            if(streamOnly){
                return;
            }

            if(getProtocol(context)==ProtocolName.HTTP2){
                    arm(context, Phase.H2_IDLE);
                }else{
                    arm(context, Phase.TCP_IDLE);
                }
            
        }
    
        @Override
        public void handlerRemoved(ChannelHandlerContext context) throws Exception {
            cancel();
            super.handlerRemoved(context);
        }
    
        @Override
        public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
            if(getProtocol(context) == ProtocolName.HTTP2 && !streamOnly){
                if(phase == Phase.H2_IDLE && h2InactivityTimeout>0){
                    arm(context, Phase.H2_IDLE);
                }
                super.channelRead(context, message);
                return;
            }

            if(isRequestStart(message)){
                cancel();
                clientRequestedKeepAlive = shouldKeepAliveRequest(context, message);
            }


            switch(phase){
                case TCP_IDLE:
                    arm(context, Phase.READ);
                    break;
                case READ:
                    resetRead(context);
                    break;
                default:
                }

            super.channelRead(context, message);
    
            if(isRequestEnd(message)){
                cancel();
                firstRequest = false;
            }
        }
    
        @Override
        public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
            
            if(message instanceof HttpResponse && ((HttpResponse)message).status().code() == 101 ){
                CharSequence upgrade = ((HttpResponse) message).headers().get(HttpHeaderNames.UPGRADE);
                if(upgrade != null){
                    AsciiString up = AsciiString.of(upgrade).toLowerCase();
                    if(AsciiString.contains(up, "websocket")){
                        markProtocol(context.pipeline(), ProtocolName.WEBSOCKET);
                        context.channel().pipeline().remove(this);
                        super.write(context, message, promise);
                        promise.addListener(future -> {
                            if(future.isSuccess()){
                                context.executor().execute(() -> {
                                    if (context.pipeline().context(this) != null){
                                        context.pipeline().remove(this);
                                    }
                                });
                            }
                        
                        });
                        return;
                    }
                    if(AsciiString.contains(up, "h2c")){
                        markProtocol(context.pipeline(), ProtocolName.HTTP2);
                        cancel();
                    }
                }
            } else if(message instanceof HttpResponse){
                serverKeepAlive = shouldKeepAliveResponse(context, message);
            }
            super.write(context, message, promise);
    
            promise.addListener(future -> {
                if(future.isSuccess() && isResponseEnd(message) && !streamOnly){
                    if(!serverKeepAlive){
                        context.close();
                    }else{
                        armPersistIfNeeded(context);
                    }
                }
            });
            
        }
    
        private void arm(ChannelHandlerContext context, Phase newPhase){
            int timeout = timeoutForPhase(newPhase);
            if(timeout <=0){
                phase = Phase.OFF;
                return;
            }
            cancel();
            phase = newPhase;
            currentTimeout = context.executor().schedule(() -> onTimeout(context), timeout, TimeUnit.MILLISECONDS);
        }
    
    
        private void resetRead(ChannelHandlerContext context){
            if(phase == Phase.READ){
                arm(context, Phase.READ);
            }
        }
    
        private void armPersistIfNeeded(ChannelHandlerContext context){
            if(getProtocol(context) != ProtocolName.WEBSOCKET){
                arm(context, Phase.PERSIST);
            }
        }

        private void cancel(){
            if(currentTimeout != null){
                currentTimeout.cancel(false);
                currentTimeout = null;
            }
            phase = Phase.OFF;
        }
    
        private void onTimeout(ChannelHandlerContext context){
            switch (phase) {
                case TCP_IDLE:
                    
                case READ:
                    if (firstRequest && !readRetried) {
                        readRetried = true;
                        arm(context, Phase.READ);
                        return;
                    }
                    if(firstRequest){
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "The connection closed due to idle timeout");
                        }
                        context.close();
                    }else{
                        context.fireExceptionCaught(new ReadTimeoutException(readTimeout, LEGACY_UNIT));
                    }
                    break;
                    
                case PERSIST:
                    context.fireExceptionCaught(new PersistTimeoutException(persistTimeout, LEGACY_UNIT));
                    //context.close();
                    break;
                case H2_IDLE:
                    context.fireExceptionCaught(new H2IdleTimeoutException(h2InactivityTimeout, LEGACY_UNIT));
                    break;
                default:
            }
        }
    
        private static boolean isRequestStart(Object message){
                return (message instanceof HttpRequest) || (message instanceof Http2HeadersFrame); 
        }
    
        private static boolean isRequestEnd(Object message){
            if(message instanceof HttpRequest){
                HttpRequest req = (HttpRequest) message;
                boolean hasBody = HttpUtil.isTransferEncodingChunked(req) || HttpUtil.isContentLengthSet(req);
                return !hasBody;
            }
            if(message instanceof LastHttpContent){
                return true;
            }
            if(message instanceof Http2DataFrame){
                return ((Http2DataFrame)message).isEndStream();
            }
            if(message instanceof Http2HeadersFrame){
                return ((Http2HeadersFrame)message).isEndStream();
            }
            return false;
        } 
    
        private static boolean isResponseEnd(Object message){
            return message instanceof LastHttpContent
                || (message instanceof Http2DataFrame && ((Http2DataFrame)message).isEndStream())
                || (message instanceof Http2HeadersFrame && ((Http2HeadersFrame)message).isEndStream());
        }
    
        private boolean shouldKeepAliveRequest(ChannelHandlerContext context, Object request){
            ProtocolName proto = NettyHttpConstants.ProtocolName.from(context.channel().attr(NettyHttpConstants.PROTOCOL).get());
            if(proto == ProtocolName.HTTP2){
                return false;
            }
            if(request instanceof HttpRequest){
                return HttpUtil.isKeepAlive((HttpRequest)request);
            }
            return false;
        }
    
        private boolean shouldKeepAliveResponse(ChannelHandlerContext context, Object response){
            ProtocolName proto = getProtocol(context);
            if(proto == ProtocolName.WEBSOCKET){
                return true;
            }
            if(proto == ProtocolName.HTTP2){
                return true;
            }

            if (!clientRequestedKeepAlive) {
                return false;
            }
            if(response instanceof HttpResponse){
                HttpResponse r = (HttpResponse) response;
                if(HttpHeaderValues.CLOSE.contentEqualsIgnoreCase(r.headers().get(HttpHeaderNames.CONNECTION))){
                    return false;
                }
                return useKeepAlive;
                
        }
        return false;
    }

    private void switchToH2Idle(){
        if(streamOnly || parentContext == null){
            return;
        }
        if(h2InactivityTimeout == 0){
            cancel();
            return;
        }
        cancel();
        arm(parentContext, Phase.H2_IDLE);
    }


    public void markProtocol(ChannelPipeline p, ProtocolName proto){
        p.channel().attr(NettyHttpConstants.PROTOCOL).set(proto.name());
        if(proto == ProtocolName.HTTP2){
            switchToH2Idle();
        } else if(proto == ProtocolName.WEBSOCKET){
            cancel();
        }
    }

    private static ProtocolName getProtocol(ChannelHandlerContext context){
        String protocol = context.channel().attr(NettyHttpConstants.PROTOCOL).get();
        return ProtocolName.from(protocol);
    }
}
