package com.ibm.ws.http.netty.pipeline.inbound.read;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.flow.FlowControlHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;

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
 *  <li>Stream requests without depending on Netty's auto-read being enabled.</li>
 *  <li>Gate how many read() invocations are issued to the channel.</li>
 *  <li>Honor the server's Keep-Alive policy and resume reading after writing a response.</li>
 *  <li>Stop scheduling reads if a connection is closed or upgraded.</li>
 * </ul>
 *
 * <h3>HTTP/1.1 exchange serialisation</h3>
 * <p>A second request (B) must not be admitted to application dispatch until the
 * first request's (A) exchange is complete. "Complete" means both:
 * <ol>
 *   <li>The inbound {@code LastHttpContent} for A has been observed (i.e. A's request
 *       body has been fully received from the wire), <em>and</em></li>
 *   <li>The outbound {@code LastHttpContent} write for A's response has succeeded.</li>
 * </ol>
 *
 * <p>The mechanism is:
 * <ul>
 *   <li>When an {@code HttpRequest} arrives, if an exchange is active
 *       ({@code responseInFlight}), the request and any following {@code HttpContent}
 *       objects are parked in {@link FlowState#enqueuePending}.</li>
 *   <li>No additional upstream {@code read()} is issued solely to obtain the next
 *       request; body reads for the <em>active</em> request continue normally.</li>
 *   <li>When the outbound {@code LastHttpContent} write for A completes, a drain
 *       loop runs on the event loop.  It forwards the queued {@code HttpRequest} (and
 *       any already-arrived content) downstream, then resumes normal read scheduling
 *       if more data is expected.</li>
 * </ul>
 */
@ChannelHandler.Sharable
public final class ReadFlowHandler extends ChannelDuplexHandler {

    public static final AttributeKey<FlowState> FLOW_KEY = AttributeKey.valueOf("httpFlowState");
    public static String NAME = "readFlowHandler";

    private static final TraceComponent tc = Tr.register(ReadFlowHandler.class);

    public static final ReadFlowHandler INSTANCE = new ReadFlowHandler();

    private ReadFlowHandler() {}

    /**
     * Returns the current state of the flow handler. The first time this method
     * is invoked, it will initialize a new {@link FlowState} object and associate
     * it to the {@link Channel} using the {@link AttributeKey}.
     *
     * @param context The current Netty {@link ChannelHandlerContext}.
     * @return The current flow state associated to the provided context.
     */
    public static FlowState state(ChannelHandlerContext context) {
        FlowState state = context.channel().attr(FLOW_KEY).get();
        if (state == null) {
            state = new FlowState();
            context.channel().attr(FLOW_KEY).set(state);
        }
        return state;
    }

    public static void markRequestConsumed(ChannelHandlerContext context) {
        FlowState state = state(context);
        state.setRequestConsumed(true);
        verifyNeedRead(context, state);
    }

    public static void setBodyReadWanted(ChannelHandlerContext context, boolean want) {
        FlowState state = state(context);
        state.setBodyReadWanted(want);
        if (want) {
            requestRead(context);
        }
    }

    public static void setClosedOrUpgraded(ChannelHandlerContext context) {
        FlowState state = state(context);
        state.setStopReading(true);
        state.setKeepAliveAllowed(false);
        state.setBodyReadWanted(false);
        state.setReadAgain(false);
        state.setReadPending(false);
        // Release any queued requests – the connection is done.
        state.releaseQueue();
    }

    /**
     * When this handler is activated, it will ensure auto-read is disabled and
     * initialize a new {@link FlowState} object. The flow state will be associated
     * to the current {@link Channel}. This method will also request the first read
     * operation.
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
        state.releaseQueue();
        super.channelInactive(context);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
        FlowState state = state(context);
        state.setReadPending(false);
        state.setReadAgain(false);
        state.releaseQueue();
        super.exceptionCaught(context, cause);
    }

    /**
     * Verifies the inbound data to determine read gating state and enforces
     * HTTP/1.1 exchange serialisation.
     *
     * <p>For {@link HttpRequest}: if an exchange is already in flight the request
     * (and any following content that belongs to it) is parked in the pending-
     * admission queue rather than forwarded downstream. The request is marked active
     * before it is forwarded, so a completion callback can safely identify it.
     *
     * <p>For {@link LastHttpContent}: the request body is marked consumed and a
     * read for the next request may be scheduled, subject to the current response
     * still being in flight.
     *
     * <p>For streaming body chunks: additional reads are requested until the request
     * body is fully consumed.
     *
     * @param context The current Netty {@link ChannelHandlerContext}.
     * @param message The inbound message.
     * @throws Exception if next handlers throw an exception.
     */
    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {

        FlowState state = state(context);
        boolean hasFlowControl = (context.pipeline().get(FlowControlHandler.class) != null);

        if (hasFlowControl && state.isReadPending()) {
            state.setReadPending(false);
            if (state.isReadAgain()) {
                state.setReadAgain(false);
                requestRead(context);
            }
        }

        if (message instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) message;

            // ----------------------------------------------------------------
            // Admission gate: if a prior exchange is still active, park this
            // request (and any content that follows) until the exchange ends.
            // ----------------------------------------------------------------
            if (state.isResponseInFlight() || state.hasPendingAdmission()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "[FLOW-PROOF] GATE_INBOUND_REQUEST ch=" + context.channel().id()
                        + " uri=" + request.uri()
                        + " queueSize=" + (state.hasPendingAdmission() ? "non-empty" : "0"));
                }
                // The pipeline delivers this message with a single ref owned by us.
                // We are parking it instead of forwarding, so no retain is needed —
                // we already own the reference.
                state.enqueuePending((HttpObject) message);
                // Do NOT issue an upstream read for the next request; body reads
                // for the currently active exchange continue via setBodyReadWanted.
                return;
            }

            // ----------------------------------------------------------------
            // Admit this request as the active exchange.
            // ----------------------------------------------------------------
            admitRequest(context, state, request, message);
            return;
        }

        // --------------------------------------------------------------------
        // HttpContent (body chunk or LastHttpContent)
        // --------------------------------------------------------------------
        if (message instanceof LastHttpContent) {
            // If the pending queue is non-empty this content belongs to a queued
            // request: park it with its request so it can be replayed together.
            // We own the reference delivered by the pipeline; no retain needed.
            if (state.hasPendingAdmission()) {
                state.enqueuePending((HttpObject) message);
                return;
            }

            state.setRequestConsumed(true);
            super.channelRead(context, message);
            verifyNeedRead(context, state);
            return;
        }

        // Plain body chunk: park if it belongs to a queued request.
        // We own the reference delivered by the pipeline; no retain needed.
        if (message instanceof HttpObject && state.hasPendingAdmission()) {
            state.enqueuePending((HttpObject) message);
            return;
        }

        super.channelRead(context, message);
    }

    /**
     * Marks the request as the active exchange, updates per-request state, and
     * forwards the {@code HttpRequest} downstream.
     */
    private static void admitRequest(ChannelHandlerContext context, FlowState state,
                                     HttpRequest request, Object message) throws Exception {
        // Mark as the active exchange BEFORE forwarding so downstream handlers
        // see a consistent responseInFlight=true state.
        state.setResponseInFlight(true);
        state.setHeadRequest(request.method() == HttpMethod.HEAD);
        state.setBodyReadWanted(false);
        state.setReadAgain(false);

        boolean requestEnd = (message instanceof LastHttpContent) || !isBodyExpected(request);
        state.setRequestConsumed(requestEnd);

        // Advance the exchange counter so completion callbacks can identify which
        // exchange they belong to.
        state.nextExchangeId();

        context.fireChannelRead(message);

        if (requestEnd && !(message instanceof LastHttpContent)) {
            // Let the codec finish emitting terminal content before granting the
            // FlowControlHandler another read. Otherwise the empty flow-control
            // queue forwards this credit as an unnecessary physical socket read.
            context.executor().execute(() -> {
                if (!context.isRemoved()
                        && context.pipeline().get(FlowControlHandler.class) != null
                        && !state.stoppedReading()
                        && context.channel().isActive()) {
                    context.read();
                }
            });
        }
    }

    /**
     * Called when the current read has finished. At this point, the
     * {@link FlowState#setReadPending(boolean)} flag is cleared. If a second
     * {@link ChannelHandlerContext#read()} was requested while the previous one was
     * still in-flight (i.e. {@link FlowState#setReadAgain(boolean)} was set), that
     * deferred read is issued now. Otherwise no action is taken — the next read will
     * be triggered by whichever code path determines that one is needed (e.g. the
     * write-promise listener in {@link #write}).
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext context) throws Exception {
        FlowState state = state(context);
        super.channelReadComplete(context);

        Tr.debug(tc, "[FLOW-PROOF] READ_COMPLETE_CLEAR_PENDING ch=" + context.channel().id()
            + " readAgain=" + state.isReadAgain());

        if (context.pipeline().get(FlowControlHandler.class) != null) {
            if (state.isReadPending()) {
                state.setReadPending(false);
                state.setReadAgain(false);
                // A queued request may already be decoded and waiting; drain it
                // before issuing another socket read.
                if (state.hasPendingAdmission() && !state.isResponseInFlight()) {
                    drainPendingAdmission(context, state);
                } else {
                    requestRead(context);
                }
            }
            return;
        }

        boolean readAgain = state.isReadAgain();
        state.setReadPending(false);
        state.setReadAgain(false);
        if (readAgain || (state.isRequestConsumed() && !state.isResponseInFlight() && state.isKeepAliveAllowed())) {
            context.executor().execute(() -> {
                // Prefer draining an already-decoded queued request over issuing
                // a new socket read.
                if (state.hasPendingAdmission() && !state.isResponseInFlight()) {
                    drainPendingAdmission(context, state);
                } else {
                    requestRead(context);
                }
            });
        }
    }

    /**
     * Intercepts write operations to monitor the response progress and update the
     * Keep-Alive state.
     *
     * <p>When a response is written, the handler sets
     * {@link FlowState#setResponseInFlight(boolean)} once the response is considered
     * committed. When the final response write completes, the handler clears the
     * {@link FlowState#setResponseInFlight(boolean)} flag, drains any pending
     * admission queue, and may issue a read for the next request if the Keep-Alive
     * policy permits it.
     *
     * <p>Write failures are propagated to the completion callback. A failed write
     * prevents admission of the next request: the connection is closed instead.
     */
    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
        FlowState state = state(context);

        if (message instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) message;
            int code = response.status().code();
            boolean informational = (code >= 100 && code < 200 && code != 101);

            if (!informational) {
                // Mark the response as in-flight for every non-informational response.
                state.setResponseInFlight(true);

                // TODO check if !(message instanceof LastHttpContent) is still valid for selfContained
                // // No body; see if we need another read
                // if(noBodyExpected && !(message instanceof LastHttpContent)){
                //     // Capture exchange id so a stale callback from a previous
                //     // exchange (e.g. reused promise) is ignored.
                boolean responseKeepAlive = HttpUtil.isKeepAlive(response);
                state.setKeepAliveAllowed(responseKeepAlive && !state.isQuiescing());

                // A FullHttpResponse (which also implements LastHttpContent) is both
                // headers and terminal content in one message — it is self-contained.
                // Complete the exchange when this single write resolves.
                //
                // An ordinary streaming HttpResponse (non-full) must NOT complete
                // here regardless of method or status code — the terminal
                // LastHttpContent write is the authoritative endpoint.  Using
                // method/status shortcuts risks releasing admission for request B
                // before A's terminal content has been flushed, which causes the
                // codec to see an out-of-order message type.
                //
                // On header-write failure, poison the exchange so even a later
                // successful terminal flush cannot reuse the connection.
                if (message instanceof FullHttpResponse) {
                    final long myExchangeId = state.getActiveExchangeId();
                    promise.addListener(f -> {
                        if (!context.executor().inEventLoop()) {
                            context.executor().execute(() -> onResponseComplete(context, state, f.isSuccess(), myExchangeId));
                        } else {
                            onResponseComplete(context, state, f.isSuccess(), myExchangeId);
                        }
                    });
                } else {
                    // Streaming header write: a failure poisons the exchange.
                    final long myExchangeId = state.getActiveExchangeId();
                    promise.addListener(f -> {
                        if (!f.isSuccess()) {
                            if (!context.executor().inEventLoop()) {
                                context.executor().execute(() -> poisonExchange(context, state, myExchangeId));
                            } else {
                                poisonExchange(context, state, myExchangeId);
                            }
                        }
                    });
                }
            }
            // Informational (1xx except 101): do not touch responseInFlight or keepAlive.
        } else if (message instanceof ByteBuf) {
            // Raw ByteBuf body write (e.g. fixed-length HTTP/1 bodies written by
            // NettyTCPWriteRequestContext for Content-Length responses).  These bypass
            // the HttpContent type but are still part of the active exchange.  A
            // failure here must poison the exchange so a later successful terminal
            // flush cannot reuse the connection.
            if (state.isResponseInFlight()) {
                final long myExchangeId = state.getActiveExchangeId();
                promise.addListener(f -> {
                    if (!f.isSuccess()) {
                        if (!context.executor().inEventLoop()) {
                            context.executor().execute(() -> poisonExchange(context, state, myExchangeId));
                        } else {
                            poisonExchange(context, state, myExchangeId);
                        }
                    }
                });
            }
        } else if (message instanceof HttpContent && !(message instanceof LastHttpContent)) {
            // Intermediate body chunk for a streaming response.  A failure here
            // poisons the exchange; the terminal write cannot rescue it.
            final long myExchangeId = state.getActiveExchangeId();
            promise.addListener(f -> {
                if (!f.isSuccess()) {
                    if (!context.executor().inEventLoop()) {
                        context.executor().execute(() -> poisonExchange(context, state, myExchangeId));
                    } else {
                        poisonExchange(context, state, myExchangeId);
                    }
                }
            });
        }

        // Separate (non-header) LastHttpContent: terminal write for a streamed body.
        // Guard against FullHttpResponse which is also a LastHttpContent but was
        // already handled above.
        if (message instanceof LastHttpContent && !(message instanceof HttpResponse)) {
            final long myExchangeId = state.getActiveExchangeId();
            promise.addListener(f -> {
                if (!context.executor().inEventLoop()) {
                    context.executor().execute(() -> onResponseComplete(context, state, f.isSuccess(), myExchangeId));
                } else {
                    onResponseComplete(context, state, f.isSuccess(), myExchangeId);
                }
            });
        }

        super.write(context, message, promise);
    }

    /**
     * Marks the active exchange as write-failed and immediately closes the
     * connection. Called when a non-terminal outbound write fails (headers or
     * body chunk) so that a later successful terminal flush cannot reuse the
     * connection. Stale-id guarded to ignore callbacks from prior exchanges.
     */
    private static void poisonExchange(ChannelHandlerContext context, FlowState state,
                                       long exchangeId) {
        if (state.getActiveExchangeId() != exchangeId) {
            return;
        }
        if (state.isExchangeWriteFailed()) {
            return; // already handled
        }
        Tr.debug(tc, "[FLOW-PROOF] POISON_EXCHANGE ch=" + context.channel().id());
        state.setExchangeWriteFailed();
        state.setKeepAliveAllowed(false);
        state.setStopReading(true);
        state.releaseQueue();
        if (context.channel().isActive()) {
            context.channel().close();
        }
    }

    /**
     * Called on the event loop when the terminal outbound write for an exchange has
     * completed (either successfully or with a failure).
     *
     * <p>If {@code succeeded} is {@code false} the connection is closed – a failed
     * write must never release the next request.
     *
     * <p>If the exchange id has changed since this listener was installed the call
     * is a stale callback (e.g. from an earlier, recycled exchange on the same
     * channel) and is silently ignored.
     */
    private static void onResponseComplete(ChannelHandlerContext context, FlowState state,
                                           boolean succeeded, long completedExchangeId) {
        // Stale callback guard.
        if (state.getActiveExchangeId() != completedExchangeId) {
            Tr.debug(tc, "[FLOW-PROOF] STALE_COMPLETION_IGNORED ch=" + context.channel().id()
                + " expected=" + state.getActiveExchangeId()
                + " got=" + completedExchangeId);
            return;
        }

        // Treat earlier-write poisoning the same as a terminal write failure:
        // a successful terminal flush does not rescue an exchange whose preceding
        // header or body write already failed.
        if (!succeeded || state.isExchangeWriteFailed()) {
            Tr.debug(tc, "[FLOW-PROOF] WRITE_FAILED_CLOSE ch=" + context.channel().id()
                + " terminalSucceeded=" + succeeded
                + " poisoned=" + state.isExchangeWriteFailed());
            state.setResponseInFlight(false);
            state.setKeepAliveAllowed(false);
            state.setStopReading(true);
            state.releaseQueue();
            if (context.channel().isActive()) {
                context.channel().close();
            }
            return;
        }

        state.setResponseInFlight(false);

        if (state.isPeerInputShutdown()) {
            context.close();
            return;
        }

        if (!state.isKeepAliveAllowed()) {
            context.close();
            return;
        }

        state.setReadPending(false);
        state.setReadAgain(false);

        // Drain the admission queue before issuing a socket read; the next
        // request may already be fully decoded and waiting.
        if (state.hasPendingAdmission()) {
            drainPendingAdmission(context, state);
        } else {
            verifyNeedRead(context, state);
        }
    }

    /**
     * Drains queued requests from the pending-admission queue onto the pipeline.
     *
     * <p>The drain loop admits exactly one {@code HttpRequest} (and all of the
     * already-arrived {@code HttpContent} objects that follow it in the queue) and
     * then stops. The next request will be drained when the newly-admitted exchange
     * completes.
     *
     * <p>If additional content is still expected (i.e. more body chunks have not
     * arrived yet) a read is scheduled so the rest of the body can arrive; this
     * does <em>not</em> count as a new physical read for the next request.
     *
     * <p>Re-entrance is guarded by {@link FlowState#isDraining()}.
     */
    static void drainPendingAdmission(ChannelHandlerContext context, FlowState state) {
        if (state.isDraining()) {
            return;
        }
        if (!context.channel().isActive() || state.stoppedReading()) {
            state.releaseQueue();
            return;
        }

        state.setDraining(true);
        try {
            HttpObject head = state.peekPending();
            if (head == null) {
                return;
            }
            if (!(head instanceof HttpRequest)) {
                // Defensive: should not happen; discard until we find a request.
                Tr.debug(tc, "[FLOW-PROOF] DRAIN_UNEXPECTED_HEAD ch=" + context.channel().id()
                    + " type=" + head.getClass().getSimpleName());
                state.pollPending();
                ReferenceCountUtil.safeRelease(head);
                return;
            }

            // Dequeue and admit the HttpRequest.
            // pollPending() transfers queue ownership; admitRequest fires it downstream.
            // The downstream HttpDispatcherHandler does NOT auto-release (super(false)),
            // so after fireChannelRead the downstream owns the ref. We must not release
            // again after the fire; hence no try/finally release here.
            HttpRequest request = (HttpRequest) state.pollPending();
            try {
                admitRequest(context, state, request, request);
            } catch (Exception e) {
                Tr.debug(tc, "[FLOW-PROOF] DRAIN_ADMIT_ERROR ch=" + context.channel().id() + " err=" + e);
                ReferenceCountUtil.safeRelease(request); // fire didn't happen; release ourselves
                state.releaseQueue();
                context.channel().close();
                return;
            }
            // admitRequest succeeded: downstream owns the reference, do NOT release here.

            // Forward any already-decoded body content that followed this request.
            // Same ownership rule: pollPending() gives us the queue's ref and we pass
            // it straight to fireChannelRead; downstream owns it after that call.
            HttpObject next;
            while ((next = state.peekPending()) != null && !(next instanceof HttpRequest)) {
                state.pollPending();
                try {
                    context.fireChannelRead(next);
                    // Downstream now owns the ref; do NOT release.
                } catch (Exception e) {
                    Tr.debug(tc, "[FLOW-PROOF] DRAIN_CONTENT_ERROR ch=" + context.channel().id() + " err=" + e);
                    // Fire did not complete; we still own the ref.
                    ReferenceCountUtil.safeRelease(next);
                    state.releaseQueue();
                    context.channel().close();
                    return;
                }
            }

            // If the active request's body is not yet fully consumed, schedule a
            // read so the remaining body chunks can arrive; do NOT issue a new
            // read merely to fetch the *next* request.
            if (!state.isRequestConsumed() && state.isBodyReadWanted() && !state.stoppedReading()
                    && context.channel().isActive()) {
                requestRead(context);
            }

        } finally {
            state.setDraining(false);
        }
    }

    /**
     * Handles events that indicate the inbound endpoint has been shutdown. When a
     * shutdown event is observed, the handler sets the
     * {@link FlowState#stoppedReading()} to true and
     * {@link FlowState#setKeepAliveAllowed(boolean)} to false, in order to disallow
     * further read scheduling. This handler will not call close on the
     * {@link ChannelHandlerContext}, allowing downstream handlers to receive the
     * event trigger.
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
                state.releaseQueue();
                context.close();
                return;
            }

            super.userEventTriggered(context, event);
            return;
        }

        if (event instanceof ChannelInputShutdownEvent || event instanceof ChannelInputShutdownReadComplete) {
            state.setPeerInputShutdown(true);

            state.setReadPending(false);
            state.setReadAgain(false);
            state.setStopReading(true);
            state.setKeepAliveAllowed(false);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Peer input shutdown: requestConsumed=" + state.isRequestConsumed() +
                                " , responseInFlight=" + state.isResponseInFlight() + " , channel=" + context.channel());
            }

            boolean quiescing = state.isQuiescing();

            if (quiescing) {
                super.userEventTriggered(context, event);
                return;
            }

            if (state.isRequestConsumed() && !state.isResponseInFlight()) {
                state.releaseQueue();
                context.close();
                return;
            }
        } else if (event == SslHandshakeCompletionEvent.SUCCESS) {
            // on handshake success, do the first read for the request if not auto reading
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Found successful SslHandshakeCompletionEvent, queueing read if auto read is disabled. AutoRead: " + context.channel().config().isAutoRead());
            }
            if (!context.channel().config().isAutoRead()) {
                requestRead(context);
            }
        }
        super.userEventTriggered(context, event);
    }

    /**
     * Request a read if, and only if, the channel is able to read and a read
     * operation is needed. This is the gate other consumers use to request
     * {@link ChannelHandlerContext#read()} scheduling. Reads are scheduled if it
     * meets the following criteria.
     * <ul>
     *  <li>The connection is active and has not been marked as closed or upgraded.</li>
     *  <li>The request is absent, being consumed, or has been fully consumed.</li>
     *  <li>No response is currently being written.</li>
     *  <li>Keep-Alive is allowed for the connection.</li>
     *  <li>There is not a read operation already pending completion.</li>
     * </ul>
     * This method will enforce execution within the event loop.
     *
     * @param context the channel handler context
     */
    public static void requestRead(ChannelHandlerContext context) {
        if (!context.executor().inEventLoop()) {
            context.executor().execute(() -> requestRead(context));
            return;
        }

        FlowState state = state(context);

        if (state.stoppedReading()) return;
        if (!context.channel().isActive()) return;

        final boolean needReadForBody = state.isBodyReadWanted() && !state.isRequestConsumed();
        final boolean needReadForNextRequest = state.isRequestConsumed() && !state.isResponseInFlight()
                                    && state.isKeepAliveAllowed()
                                    && !state.hasPendingAdmission(); // already have next request decoded

        final boolean needRead = needReadForBody || needReadForNextRequest;

        if (!needRead) {
            Tr.debug(tc, "[FLOW-PROOF] NO_READ_NEEDED ch=" + context.channel().id()
                + " bodyWanted=" + state.isBodyReadWanted()
                + " reqConsumed=" + state.isRequestConsumed()
                + " respInFlight=" + state.isResponseInFlight()
                + " keepAlive=" + state.isKeepAliveAllowed()
                + " hasPending=" + state.hasPendingAdmission()
                + " readPending=" + state.isReadPending());
            return;
        }

        if (state.isReadPending()) {
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

    private static void verifyNeedRead(ChannelHandlerContext context, FlowState state) {
        if (state.stoppedReading()) return;
        if (!context.channel().isActive()) return;

        if (state.isRequestConsumed() && !state.isResponseInFlight() && state.isKeepAliveAllowed()
                && !state.hasPendingAdmission()) {
            requestRead(context);
        }
    }

    /**
     * Utility method to determine whether a {@link HttpRequest} is expected to have
     * a body payload. This is used to decide whether the request can be flagged as
     * consumed or if the handler should schedule additional reading.
     *
     * @param request the request object
     * @return true if a body is expected; false otherwise.
     */
    private static boolean isBodyExpected(HttpRequest request) {
        if (HttpUtil.is100ContinueExpected(request)) return true;
        if (HttpUtil.isTransferEncodingChunked(request)) return true;
        return HttpUtil.getContentLength(request, -1) > 0;
    }

}
