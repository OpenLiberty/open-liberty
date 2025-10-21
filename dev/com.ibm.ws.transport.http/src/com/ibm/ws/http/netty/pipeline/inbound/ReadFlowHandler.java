package com.ibm.ws.http.netty.pipeline.inbound;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.AttributeKey;

public final class ReadFlowHandler extends ChannelDuplexHandler{

    public static final AttributeKey<FlowState> FLOW_KEY = AttributeKey.valueOf("http.flow.state");

    public ReadFlowHandler(){}

    public static final class FlowState {
        public volatile boolean requestConsumed; // set when request body is fully read or proven empty
        public volatile boolean responseInFlight; // set true at first commit; false on final write future
        public volatile boolean keepAliveAllowed; // writer decides based on response & policy
        public volatile boolean pendingRead; // someone asked to read while gated
        public volatile boolean closedOrUpgraded; // shutdown/upgrade says “never resume”
    }

    private static FlowState state(ChannelHandlerContext context){
        FlowState state = context.channel().attr(FLOW_KEY).get();
        if(state == null){
            state = new FlowState();
            context.channel().attr(FLOW_KEY).set(state);
        }
        return state;
    }

    @Override
    public void channelActive(ChannelHandlerContext context) throws Exception {
        if (context.channel().config().isAutoRead()) {
            context.channel().config().setAutoRead(false);
        }
        state(context);
        super.channelActive(context);
    }

    @Override
    public void read(ChannelHandlerContext context) throws Exception {
        FlowState st = state(context);
        if (st.closedOrUpgraded) {
            // swallow reads after close/upgrade
            return;
        }
        if (st.requestConsumed && !st.responseInFlight && st.keepAliveAllowed) {
            super.read(context);
        } else {
            st.pendingRead = true; 
        }
    }

    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
        FlowState state = state(context);

        if (message instanceof HttpResponse) {
            // Response is being committed
            state.responseInFlight = true;

            // Decide keep-alive based on response headers as they are now.
            HttpResponse res = (HttpResponse) message;
            state.keepAliveAllowed = HttpUtil.isKeepAlive(res);
        }

        if (message instanceof LastHttpContent) {
            // When the final chunk is flushed, we may allow one more read
            promise.addListener((ChannelFutureListener) f -> {
                state.responseInFlight = false;
                verifyNeedRead(context, state);
            });
        }

        super.write(context, message, promise);
    }

    private static void verifyNeedRead(ChannelHandlerContext context, FlowState state) {
        if (state.pendingRead
            && state.requestConsumed
            && !state.responseInFlight
            && state.keepAliveAllowed
            && !state.closedOrUpgraded
            && context.channel().isActive()) {
            state.pendingRead = false;
            context.read();
        }
    }

    public static FlowState getFlowState(Channel channel) {
        return channel.attr(FLOW_KEY).get();
    }

    public static void markRequestConsumed(ChannelHandlerContext context) {
        FlowState st = state(context);
        st.requestConsumed = true;
        verifyNeedRead(context, st);
    }

    public static void setClosedOrUpgraded(ChannelHandlerContext context) {
        FlowState st = state(context);
        st.closedOrUpgraded = true;
        st.keepAliveAllowed = false;
        st.pendingRead = false;
    }
}
