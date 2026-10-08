/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.pipeline.inbound.read;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.netty.channel.ChannelHandlerContext;

/**
 * Per-exchange lifecycle coordinator for HTTP/1 keep-alive connections.
 *
 * <p>Request B may be admitted only after request A has: (1) completed its
 * response write ({@code responseInFlight}, tracked by {@link ReadFlowHandler});
 * (2) drained its request body ({@link #signalBodyDone}); (3) reached application
 * cleanup readiness ({@link #signalAppDone}); and (4) successfully executed its
 * {@link CleanupAction}.
 *
 * <p><strong>Binding:</strong> call {@link #bindCleanupAction} on the event loop
 * after construction but before application dispatch. In production,
 * {@code HttpDispatcherLink.init()} does this synchronously, ensuring the captured
 * ISC belongs to exchange A even if a worker callback fires after the link recycles.
 *
 * <p><strong>Thread model:</strong> all fields are event-loop-owned. Both signal
 * methods assert {@code inEventLoop()}; worker callers must post a task first.
 *
 * <p><strong>State machine:</strong> unbound → cleanup-started (re-entrance guard) →
 * complete (B may be admitted) or failed (connection must close).
 *
 * <p><strong>Failure isolation:</strong> only the {@link CleanupAction} call is inside
 * the cleanup-failure catch, so admission exceptions are never misclassified as cleanup
 * failures.
 */
public final class ExchangeLifecycle {

    private static final TraceComponent tc = Tr.register(ExchangeLifecycle.class);

    /**
     * Cleanup work bound to this exchange. Called exactly once on the event loop
     * when both body-done and app-done signals have been received. Use an explicit
     * no-op ({@code () -> {}}) for resource-free paths; {@code null} is rejected.
     */
    @FunctionalInterface
    public interface CleanupAction {
        /** Executes cleanup; if this throws, the connection will be closed. */
        void execute() throws Exception;
    }

    private boolean bodyDone;
    private boolean appDone;
    /** Set before {@link CleanupAction#execute()} to block re-entrance and duplicates. */
    private boolean cleanupStarted;
    private boolean cleanupComplete;
    private boolean cleanupFailed;
    /** {@code null} when unbound; written exactly once by {@link #bindCleanupAction}. */
    private CleanupAction cleanupAction;

    /**
     * Binds the cleanup action. Must be called on the event loop before any signal.
     * Rejects {@code null}, duplicate bindings, and late bindings (after a signal).
     */
    public void bindCleanupAction(CleanupAction action) {
        if (action == null) {
            throw new IllegalArgumentException(
                "bindCleanupAction: action must not be null; use a no-op for resource-free paths");
        }
        if (cleanupAction != null) {
            throw new IllegalStateException(
                "bindCleanupAction called more than once on lifecycle " + this);
        }
        if (bodyDone || appDone) {
            throw new IllegalStateException(
                "bindCleanupAction called after lifecycle signalling has started: " + this);
        }
        this.cleanupAction = action;
    }

    /**
     * Records that the wire body has reached protocol completion
     * ({@code LastHttpContent} observed). Triggers cleanup if app-done is also set.
     * Must be called on the event loop.
     */
    public void signalBodyDone(ChannelHandlerContext context) {
        assert context.executor().inEventLoop() : "signalBodyDone must be called on the event loop";
        if (!guardSignal(context, "signalBodyDone") || bodyDone) return;
        bodyDone = true;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "signalBodyDone ch=" + context.channel().id() + " appDone=" + appDone);
        }
        tryCleanup(context);
    }

    /**
     * Records that the application has reached cleanup readiness ({@code nettyClose()}
     * confirmed reusable keep-alive). Triggers cleanup if body-done is also set.
     * Must be called on the event loop.
     */
    public void signalAppDone(ChannelHandlerContext context) {
        assert context.executor().inEventLoop() : "signalAppDone must be called on the event loop";
        if (!guardSignal(context, "signalAppDone") || appDone) return;
        appDone = true;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "signalAppDone ch=" + context.channel().id() + " bodyDone=" + bodyDone);
        }
        tryCleanup(context);
    }

    /**
     * Returns false and routes to failure if the lifecycle is unbound.
     * Used as a pre-check in signal methods to share the unbound-guard logic.
     */
    private boolean guardSignal(ChannelHandlerContext context, String signal) {
        if (cleanupAction == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, signal + ": UNBOUND ch=" + context.channel().id());
            }
            cleanupFailed = true;
            ReadFlowHandler.onCleanupFailed(context.channel(),
                new IllegalStateException(signal + ": cleanup action not bound"));
            return false;
        }
        return true;
    }

    /** Returns {@code true} only after both signals have been delivered and the cleanup
     *  action has returned successfully. Returns {@code false} for unbound instances. */
    public boolean isCleanupComplete() {
        return cleanupComplete;
    }

    /** Returns {@code true} if cleanup threw. B must never be admitted; connection closes. */
    public boolean isCleanupFailed() {
        return cleanupFailed;
    }

    /**
     * Runs the cleanup action exactly once when both bodyDone and appDone are set.
     * Only the CleanupAction call is inside the failure catch so that a subsequent
     * admission exception cannot be misclassified as a cleanup failure.
     */
    private void tryCleanup(ChannelHandlerContext context) {
        if (!bodyDone || !appDone || cleanupStarted) {
            return;
        }
        cleanupStarted = true;

        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "tryCleanup: executing ch=" + context.channel().id());
            }
            cleanupAction.execute();
        } catch (Throwable t) {
            cleanupFailed = true;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "tryCleanup: FAILED ch=" + context.channel().id() + " cause=" + t);
            }
            ReadFlowHandler.onCleanupFailed(context.channel(), t);
            return;
        }

        cleanupComplete = true;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "tryCleanup: COMPLETE ch=" + context.channel().id());
        }
        try {
            ReadFlowHandler.onCleanupComplete(context.channel());
        } catch (Throwable t) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "tryCleanup: notification failed (cleanup was COMPLETE) ch="
                        + context.channel().id() + " cause=" + t);
            }
            ReadFlowHandler.onCleanupNotificationFailed(context.channel(), t);
        }
    }
}
