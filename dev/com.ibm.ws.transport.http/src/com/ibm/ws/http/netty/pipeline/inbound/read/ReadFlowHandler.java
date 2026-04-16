package com.ibm.ws.http.netty.pipeline.inbound.read;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.flow.FlowControlHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.util.AttributeKey;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.netty.internal.impl.QuiesceHandler;
import com.ibm.ws.http.netty.NettyHttpConstants;

/**
 * Netty handler that handles read gating when Netty's auto-read is disabled. It 
 * tracks the state of each connection to determine when to invoke the 
 * {@link ChannelHandlerContext#read()} to request more data. 
 * 
 * This handler is responsible of the following:
 * <ul>
 *  <li> Stream requests without depending on Netty's auto-read being enabled. </li>
 *  <li> Gate how many read() invocations are issued to the channel. </li>
 *  <li> Honor the server's Keep-Alive policy and resume reading after writing a response. </li>
 *  <li> Stop scheduling reads if a connection is closed or upgraded. </li>
 * </ul>
 */
@ChannelHandler.Sharable
public final class ReadFlowHandler extends ChannelDuplexHandler{

    public static final AttributeKey<FlowState> FLOW_KEY = AttributeKey.valueOf("httpFlowState");
    public static String NAME = "readFlowHandler";

    private static final TraceComponent tc = Tr.register(ReadFlowHandler.class);

    public static final ReadFlowHandler INSTANCE = new ReadFlowHandler();

    private ReadFlowHandler(){}

    /**
     * Returns the current state of the flow handler. The first time this method 
     * is invoked, it will initialize a new {@link FlowState} object and associate
     * it to the {@link Channel} using the {@link AttributeKey}.
     * 
     * @param context The current Netty {@link ChannelHandlerContext}.
     * @return The current flow state associated to the provided context.
     */
    public static FlowState state(ChannelHandlerContext context){
        FlowState state = context.channel().attr(FLOW_KEY).get();
        if(state == null){
            state = new FlowState();
            context.channel().attr(FLOW_KEY).set(state);
        }
        return state;
    }

    public static void markRequestConsumed(ChannelHandlerContext context){
        FlowState state = state(context);
        state.setRequestConsumed(true);
        verifyNeedRead(context, state);
    }

    public static void setBodyReadWanted(ChannelHandlerContext context, boolean want){
        FlowState state = state(context);
        state.setBodyReadWanted(want);
        if(want){
            requestRead(context);
        }
    }

    public static void setClosedOrUpgraded(ChannelHandlerContext context){
        FlowState state = state(context);
        state.setStopReading(true);
        state.setKeepAliveAllowed(false);
        state.setBodyReadWanted(false);
        state.setReadAgain(false);
        state.setReadPending(false);
    }

    /**
     * When this handler is activated, it will ensure auto-read is disabled and initialize 
     * a new {@link FlowState} object. The flow state will be associated to the current
     * {@link Channel}. This method will also request the first read operation. 
     * 
     * @param context The current Netty {@link ChannelHandlerContext}.
     * @throws Exception if next handlers throw an exception.
     */
    @Override
    public void channelActive(ChannelHandlerContext context) throws Exception {
        if (context.channel().config().isAutoRead()) {
            context.channel().config().setAutoRead(false);
        }
        state(context);
        super.channelActive(context);
        requestRead(context);
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception {
        FlowState state = state(context);
        state.setReadPending(false);
        state.setReadAgain(false);
        state.setStopReading(true);
        super.channelInactive(context);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
        FlowState state = state(context);
        state.setReadPending(false);
        state.setReadAgain(false);
        super.exceptionCaught(context, cause);
    }

    /**
     * Verifies the inbound data to determine read gating state.
     * 
     * For {@link HttpRequest}, the handler determines if a body is expected and updates the 
     * {@link FlowState#requestConsumed} flag accordingly. For {@link LastHttpContent}, the handler 
     * will mark the request as consumed and potentially request a read if:
     *  <ul>
     *   <li> the server Keep-Alive policy allows for it </li>
     *   <li> there is not a response currently in flight </li>
     *  </ul>
     * 
     * For streaming request bodies, the handler will request additional reads until the request has 
     * been marked as fully consumed. However, no further reads will be requested if the connection 
     * has been closed or upgraded.
     * 
     * @param context The current Netty {@link ChannelHandlerContext}.
     * @param message The inbound message.
     * @throws Exception if next handlers throw an exception.
     */
    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {

        FlowState state = state(context);
        boolean hasFlowControl = (context.pipeline().get(FlowControlHandler.class) != null);

        if (hasFlowControl && state.isReadPending()){
            state.setReadPending(false);
            if (state.isReadAgain()){
                state.setReadAgain(false);
                requestRead(context);
            }
        }

        if(message instanceof HttpRequest){
            state.setResponseInFlight(true);
            HttpRequest request = (HttpRequest) message;
            state.setHeadRequest(request.method() == HttpMethod.HEAD);
            state.setBodyReadWanted(false);
            state.setReadAgain(false);
            boolean requestEnd = (message instanceof LastHttpContent) || !isBodyExpected(request);
            
            state.setRequestConsumed(requestEnd);
            super.channelRead(context, message);

            if(requestEnd && !(message instanceof LastHttpContent)){
                context.channel().read();
            }
            return;
        }

        if(message instanceof LastHttpContent){
            state.setRequestConsumed(true);
            super.channelRead(context, message);
            verifyNeedRead(context, state);
            return;
        }

        super.channelRead(context, message);
    }


    /**
     * Called when the current read has finished. At this point, the {@link FlowState#setReadPending(boolean)}
     * flag is cleared and a decision is made to determine whether another {@link ChannelHandlerContext#read()} 
     * should be requested for additional body payload. 
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext context) throws Exception {
        FlowState state = state(context);
        super.channelReadComplete(context);

        Tr.debug(tc, "[FLOW-PROOF] READ_COMPLETE_CLEAR_PENDING ch=" + context.channel().id()
            + " readAgain=" + state.isReadAgain());

        if (context.pipeline().get(FlowControlHandler.class) != null){
            return;
        }
        
        state.setReadPending(false);
        if(state.isReadAgain()){
            state.setReadAgain(false);
            context.executor().execute(()->requestRead(context));
        }
        
    }

    /**
     * Intercepts write operations to monitor the response progress and update the Keep-Alive state.
     * When a response is written, the handler sets {@link FlowState#setResponseInFlight(boolean)}
     * once the response is considered committed. When the final response write completes, the handler
     * clears the {@link FlowState#setResponseInFlight(boolean)} flag and may issue a read for the 
     * next request if the Keep-Alive policy permits it. 
     */
    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
        FlowState state = state(context);

        if (message instanceof HttpResponse) {
            state.setResponseInFlight(true);
        
            HttpResponse response = (HttpResponse) message;
            int code = response.status().code();
            boolean informational = (code >= 100 && code < 200 && code != 101);

            boolean responseKeepAlive = HttpUtil.isKeepAlive(response);
            state.setKeepAliveAllowed(responseKeepAlive && !state.isQuiescing());

            boolean noBodyExpected = state.isHeadRequest() || !isResponseBodyPermitted(code)
                || (HttpUtil.getContentLength(response,-1) == 0 && !HttpUtil.isTransferEncodingChunked(response));

            if (!informational) {
                state.setResponseInFlight(true);

                // No body; see if we need another read
                if(noBodyExpected){
                    promise.addListener(f -> {
                        state.setResponseInFlight(false);

                        if(state.isPeerInputShutdown()){
                            context.close();
                            return;
                        }

                        if (!state.isKeepAliveAllowed()) {
                            context.close();
                            return;
                        }

                        verifyNeedRead(context, state);
                    });
                }
            }
        }
        
        if (message instanceof LastHttpContent) {
            promise.addListener(f -> {
                state.setResponseInFlight(false);

                if(state.isPeerInputShutdown()){
                    context.close();
                    return;
                }

                if (!state.isKeepAliveAllowed()) {
                    context.close();
                    return;
                }

                verifyNeedRead(context, state);
            });
        }

        super.write(context, message, promise);
    }

    /**
     * Handles events that indicate the inbound endpoint has been shutdown. When
     * a shutdown event is observed, the handler sets the {@link FlowState#stopReading(boolean)} 
     * to true and {@link FlowState#setKeepAlive(boolean)} to false, in order to disallow further 
     * read scheduling. This handler will not call close on the {@link ChannelHandlerContext}, 
     * allowing downstream handlers to receive the event trigger.
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext context, Object event) throws Exception {
        FlowState state = state(context);

        if (event == QuiesceHandler.QUIESCE_EVENT) {
            state.setQuiescing(true);
            context.channel().attr(NettyHttpConstants.QUIESCING).set(Boolean.TRUE);

            state.setKeepAliveAllowed(false);


            // Idle keep-alive connection: close it now.
            if (state.isRequestConsumed() && !state.isResponseInFlight()) {
                state.setReadPending(false);
                state.setReadAgain(false);
                state.setStopReading(true);
                state.setBodyReadWanted(false);
                context.close();
                return;
            }

            super.userEventTriggered(context, event);
            return;
        }
        
        if (event instanceof ChannelInputShutdownEvent || event instanceof ChannelInputShutdownReadComplete) {
            //FlowState state = state(context);
            state.setPeerInputShutdown(true);

            state.setReadPending(false);
            state.setReadAgain(false);
            state.setStopReading(true);
            state.setKeepAliveAllowed(false);

            if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
                Tr.debug(tc, "Peer input shutdown: requestConsumed=" + state.isRequestConsumed() +
                                " , responseInFlight=" + state.isResponseInFlight() + " , channel="+ context.channel());
            }

            boolean quiescing = state.isQuiescing();

            if (quiescing){
                super.userEventTriggered(context, event);
                return;
            }

            if(state.isRequestConsumed() && !state.isResponseInFlight()){
                context.close();
                return;

            } 
        } else if (event == SslHandshakeCompletionEvent.SUCCESS) {
                // on handshake success, do the first read for the request if not auto reading
                if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
                    Tr.debug(tc, "Found successful SslHandshakeCompletionEvent, queueing read if auto read is disabled. AutoRead: " + context.channel().config().isAutoRead());
                }
                if (!context.channel().config().isAutoRead()) {
                    context.read();
                }
        }
        super.userEventTriggered(context, event);
    }

    /**
     * Request a read if, and only if, the channel is able to read and a read operation is 
     * needed. This is the gate other consumers use to request {@link ChannelHandlerContext#read()} 
     * scheduling. Reads are scheduled if it meets the following criteria.
     *  <ul> 
     *   <li> The connection is active and has not been marked as closed or upgraded. </li>
     *   <li> The request is absent, being consumed, or has been fully consumed. </li>
     *   <li> No response is currently being written. </li>
     *   <li> Keep-Alive is allowed for the connection. </li>
     *   <li> There is not read operation already pending completion. </li>
     *  <ul>
     * This method will enforce execution within the event loop. 
     * 
     * @param context the channel handler context
     */
    public static void requestRead(ChannelHandlerContext context) {
        if(!context.executor().inEventLoop()){
            context.executor().execute(() -> requestRead(context));
            return;
        }

        FlowState state = state(context);

        if(state.stoppedReading()) return;
        if(!context.channel().isActive()) return;

        final boolean needReadForBody = state.isBodyReadWanted() && !state.isRequestConsumed();
        final boolean needReadForNextRequest = state.isRequestConsumed() && !state.isResponseInFlight()
                                    && state.isKeepAliveAllowed();

        final boolean needRead = needReadForBody || needReadForNextRequest;


        if(!needRead){
            Tr.debug(tc, "[FLOW-PROOF] NO_READ_NEEDED ch=" + context.channel().id()
                + " bodyWanted=" + state.isBodyReadWanted()
                + " reqConsumed=" + state.isRequestConsumed()
                + " respInFlight=" + state.isResponseInFlight()
                + " keepAlive=" + state.isKeepAliveAllowed()
                + " readPending=" + state.isReadPending());
            return;
        }

        

        if(state.isReadPending()){
            state.setReadAgain(true);
            Tr.debug(tc, "[FLOW-PROOF] READ_SUPPRESSED_PENDING ch=" + context.channel().id()
                + " bodyWanted=" + state.isBodyReadWanted()
                + " reqConsumed=" + state.isRequestConsumed()
                + " respInFlight=" + state.isResponseInFlight()
                + " keepAlive=" + state.isKeepAliveAllowed()
                + " readPending=" + state.isReadPending());
            return;
        }

        state.setReadPending(true);
        Tr.debug(tc, "[FLOW-PROOF] ISSUING_READ ch=" + context.channel().id()
            + " bodyWanted=" + state.isBodyReadWanted()
            + " reqConsumed=" + state.isRequestConsumed()
            + " respInFlight=" + state.isResponseInFlight()
            + " keepAlive=" + state.isKeepAliveAllowed());
        context.read();
    }

    private static void verifyNeedRead(ChannelHandlerContext context, FlowState state){
        if(state.stoppedReading()) return;
        if(!context.channel().isActive()) return;

        if(state.isRequestConsumed() && !state.isResponseInFlight() && state.isKeepAliveAllowed()){
            requestRead(context);
        }
    }

    /**
     * Utility method to determine whether a {@link HttpRequest} is expected to have
     * body payload. This is used to decide whether the request can be flagged as 
     * consumed or if the handler should schedule additional reading.
     * 
     * @param request the request object
     * @return true if a body is expected; false otherwise. 
     */
    private static boolean isBodyExpected(HttpRequest request){
      
        if(HttpUtil.is100ContinueExpected(request)) return true;
        if (HttpUtil.isTransferEncodingChunked(request)) return true;
        return HttpUtil.getContentLength(request, -1) > 0;
    }

    /** 
     * Utility method to inspect the response status to confirm if a body is permitted. 
     * As detailed in RFC 9110, the following status codes do not provide body payloads:
     *  <ul>
     *   <li> 204 No Content </li>
     *   <li> 304 Not Modified </li>
     *   <li> 1xx Informational </li> 
     *  </ul>
     * This is utilized to change the flow status during writing so that the 
     * {@link FlowState#isResponseInFlight()} is flaged as false (response sent).
     */
    private static boolean isResponseBodyPermitted(int status) {
        if (status == 101 || status == 204 || status == 304) return false;
        return !(status >= 100 && status < 200);
    }

    
}
