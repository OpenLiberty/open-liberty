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
 * <h3>Purpose</h3>
 * <p>An HTTP/1 reusable exchange is complete — and the next request (B) may be
 * admitted — only when ALL of the following have occurred for the current
 * exchange (A):
 * <ol>
 *   <li>A's terminal response write has succeeded (tracked by
 *       {@link ReadFlowHandler} via {@code responseInFlight}).</li>
 *   <li>A's request body has reached protocol completion
 *       ({@link #signalBodyDone} has been called).</li>
 *   <li>The application lifecycle has reached cleanup readiness
 *       ({@link #signalAppDone} has been called).</li>
 *   <li>A's queued-body purge and the {@link CleanupAction} have returned
 *       ({@link #isCleanupComplete} becomes {@code true}).</li>
 * </ol>
 *
 * <h3>Cleanup resource binding</h3>
 * <p>The {@link CleanupAction} is bound to this lifecycle via
 * {@link #bindCleanupAction(CleanupAction)} after construction but <em>before</em>
 * any signal can arrive.  In production, {@code HttpDispatcherLink.init()} calls
 * {@code bindCleanupAction} synchronously on the event loop (after setting up the
 * ISC but before application dispatch), so any delayed worker signal always executes
 * the cleanup action that belongs to this exchange — not whatever is current when
 * the task eventually runs.
 *
 * <p>Both production code and tests use the same {@link CleanupAction} mechanism.
 * Production passes a real {@code isc.clear()} lambda; tests can inject a counting
 * or throwing action to verify the cleanup contract.
 *
 * <h3>Thread model</h3>
 * <p>All fields and methods of this class are <em>event-loop-owned</em>.
 * Neither {@link #signalBodyDone} nor {@link #signalAppDone} may be called
 * from a worker thread; callers on worker threads must submit a task to the
 * channel's event loop first and call these methods from within that task.
 *
 * <p>This invariant is checked at runtime in assert-enabled builds and is
 * enforced by the callers in {@link ReadFlowHandler} and
 * {@code HttpDispatcherLink}.
 *
 * <h3>Lifecycle state machine</h3>
 * <pre>
 *   PENDING ──(both signals + cleanup ok)──► COMPLETE
 *           ──(cleanup throws)             ──► FAILED
 * </pre>
 * A fresh {@code ExchangeLifecycle} always starts in {@code PENDING}; it is
 * NEVER considered complete until both signals have been delivered and the
 * cleanup action has returned successfully.  There is no bypass for newly
 * created instances.
 *
 * <h3>Failure isolation</h3>
 * <p>The cleanup-failure catch boundary wraps <em>only</em> the
 * {@link CleanupAction} invocation.  Once the action returns successfully,
 * {@link CleanupState#COMPLETE} is set and {@link ReadFlowHandler#onCleanupComplete}
 * is called <em>outside</em> the catch boundary.  An exception from the admission
 * or notification path is therefore never misclassified as a cleanup failure; it is
 * routed through {@link ReadFlowHandler#onCleanupNotificationFailed} while A's
 * state remains {@code COMPLETE}.
 *
 * <h3>Lifecycle allocation</h3>
 * A fresh {@code ExchangeLifecycle} is allocated by {@link FlowState#nextExchangeId}
 * each time an {@code HttpRequest} is admitted.  It is discarded when the next
 * exchange begins.  Stale callbacks for a prior exchange must not use the
 * lifecycle object of the current exchange; callers capture the instance at
 * registration time.
 */
public final class ExchangeLifecycle {

    private static final TraceComponent tc = Tr.register(ExchangeLifecycle.class);

    /**
     * Abstraction for the cleanup work bound to this exchange.
     *
     * <p>Implementations are expected to be lightweight and non-blocking when
     * called on the event loop.  Production uses an {@code isc.clear()} lambda.
     * Tests use counting or throwing actions to verify the cleanup contract.
     *
     * <p>A {@code null} action is never permitted; use an explicit no-op
     * ({@code () -> {}}) for resource-free paths.
     */
    @FunctionalInterface
    public interface CleanupAction {
        /**
         * Performs the exchange cleanup.  Called exactly once, on the event loop,
         * when both body-done and app-done signals have been received.
         *
         * @throws Exception if cleanup fails; the lifecycle will be set to FAILED
         *                   and the connection will be closed.
         */
        void execute() throws Exception;
    }

    /** Explicit cleanup state to replace boolean flags. Event-loop-owned. */
    private enum CleanupState {
        /** Both signals not yet received; cleanup has not started. */
        PENDING,
        /** Both signals received; cleanup is executing right now (re-entrance guard). */
        RUNNING,
        /** Cleanup completed successfully. Next-request admission is now allowed. */
        COMPLETE,
        /** Cleanup threw; connection should be closed, B must never be admitted. */
        FAILED
    }

    /** Binding state for the cleanup action. Event-loop-owned. */
    private enum BindState {
        /** No action bound yet. Signals are rejected in this state. */
        UNBOUND,
        /** Action has been bound; signals are accepted. */
        BOUND
    }

    /** Set to {@code true} once {@link #signalBodyDone} is called. Event-loop-owned. */
    private boolean bodyDone;

    /** Set to {@code true} once {@link #signalAppDone} is called. Event-loop-owned. */
    private boolean appDone;

    /** Current cleanup phase. Starts at PENDING; transitions exactly once. Event-loop-owned. */
    private CleanupState cleanupState = CleanupState.PENDING;

    /** Binding state. Starts UNBOUND; transitions to BOUND exactly once. Event-loop-owned. */
    private BindState bindState = BindState.UNBOUND;

    /**
     * The cleanup action bound to this lifecycle.  Bound via
     * {@link #bindCleanupAction(CleanupAction)} before any signal arrives.
     * Never {@code null} after binding; use an explicit no-op for resource-free paths.
     * Event-loop-owned: written once on the event loop before dispatch,
     * read only from the event loop during cleanup.
     */
    private CleanupAction cleanupAction;

    /**
     * Constructs an {@code ExchangeLifecycle} in the UNBOUND state.
     *
     * <p>Call {@link #bindCleanupAction(CleanupAction)} on the event loop after
     * construction but before application dispatch.  Signals delivered before
     * binding throw {@link IllegalStateException}.
     */
    public ExchangeLifecycle() {
        this.cleanupAction = null;
    }

    /**
     * Binds the cleanup action to this lifecycle.
     *
     * <p>Must be called on the event loop, after the lifecycle has been installed by
     * {@link FlowState#nextExchangeId()} but before any worker thread can deliver
     * body or app-done signals.  May only be called once; subsequent calls throw
     * {@link IllegalStateException}.
     *
     * <p>Binding is rejected if:
     * <ul>
     *   <li>{@code action} is {@code null} — use an explicit no-op instead.</li>
     *   <li>an action has already been bound (duplicate binding).</li>
     *   <li>lifecycle signalling has already started (late binding).</li>
     * </ul>
     *
     * <p>In production, called from {@code HttpDispatcherLink.init()} /
     * {@code initStreaming()} as part of the synchronous request-init sequence that
     * runs on the event loop before application dispatch.  This guarantees that the
     * ISC captured in the action belongs to exchange A even if a delayed worker
     * callback fires after the link has been recycled for exchange B.
     *
     * @param action the cleanup action to bind; must not be {@code null}.
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     * @throws IllegalStateException    if already bound or if signalling has started.
     */
    public void bindCleanupAction(CleanupAction action) {
        if (action == null) {
            throw new IllegalArgumentException(
                "bindCleanupAction: action must not be null; use a no-op for resource-free paths");
        }
        if (bindState == BindState.BOUND) {
            throw new IllegalStateException(
                "bindCleanupAction called more than once on lifecycle " + this);
        }
        if (cleanupState != CleanupState.PENDING || bodyDone || appDone) {
            throw new IllegalStateException(
                "bindCleanupAction called after lifecycle signalling has started: " + this);
        }
        this.cleanupAction = action;
        this.bindState = BindState.BOUND;
    }

    /**
     * Records that the wire body for this exchange has reached protocol
     * completion ({@code LastHttpContent} observed).
     *
     * <p>Must be called on the event loop.  If {@link #signalAppDone} has
     * already been called, cleanup is performed now.
     *
     * @param context the channel handler context, used to trigger admission
     *                after cleanup.
     */
    public void signalBodyDone(ChannelHandlerContext context) {
        assert context.executor().inEventLoop()
            : "signalBodyDone must be called on the event loop";

        if (bindState == BindState.UNBOUND) {
            // Cleanup action not yet bound — this is an initialization error.
            // Treat as a cleanup failure so the connection is closed safely.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "[LIFECYCLE] signalBodyDone: UNBOUND — treating as failure ch="
                        + context.channel().id());
            }
            cleanupState = CleanupState.FAILED;
            ReadFlowHandler.onCleanupFailed(context,
                new IllegalStateException("signalBodyDone: cleanup action not bound"));
            return;
        }

        if (bodyDone) {
            // Duplicate notification — safe to ignore.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "[LIFECYCLE] signalBodyDone: already set, ignoring ch="
                        + context.channel().id());
            }
            return;
        }
        bodyDone = true;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "[LIFECYCLE] signalBodyDone ch=" + context.channel().id()
                    + " appDone=" + appDone);
        }
        tryCleanup(context);
    }

    /**
     * Records that the application/response lifecycle has reached cleanup
     * readiness (i.e. {@code nettyClose()} has determined this is a reusable
     * keep-alive exchange and the application has made its final use of the
     * context).
     *
     * <p>Must be called on the event loop.  If {@link #signalBodyDone} has
     * already been called, cleanup is performed now.
     *
     * @param context the channel handler context.
     */
    public void signalAppDone(ChannelHandlerContext context) {
        assert context.executor().inEventLoop()
            : "signalAppDone must be called on the event loop";

        if (bindState == BindState.UNBOUND) {
            // Cleanup action not yet bound — treat as a cleanup failure.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "[LIFECYCLE] signalAppDone: UNBOUND — treating as failure ch="
                        + context.channel().id());
            }
            cleanupState = CleanupState.FAILED;
            ReadFlowHandler.onCleanupFailed(context,
                new IllegalStateException("signalAppDone: cleanup action not bound"));
            return;
        }

        if (appDone) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "[LIFECYCLE] signalAppDone: already set, ignoring ch="
                        + context.channel().id());
            }
            return;
        }
        appDone = true;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "[LIFECYCLE] signalAppDone ch=" + context.channel().id()
                    + " bodyDone=" + bodyDone);
        }
        tryCleanup(context);
    }

    /**
     * Returns {@code true} if and only if cleanup has successfully completed.
     *
     * <p>A freshly created {@code ExchangeLifecycle} always returns {@code false}
     * until both {@link #signalBodyDone} and {@link #signalAppDone} have been
     * delivered and the cleanup action has returned successfully.  There is no
     * bypass for unarmed or newly-created instances.
     *
     * <p>Code paths that do not participate in the lifecycle (e.g. the first
     * admitted request before any exchange is active) must use
     * {@link FlowState#isAdmissionEligible()} instead of calling this method
     * directly, so that the "no prior exchange" case is handled separately.
     */
    public boolean isCleanupComplete() {
        return cleanupState == CleanupState.COMPLETE;
    }

    /**
     * Returns {@code true} if cleanup has failed.  When true, B must never be
     * admitted and the connection should be closed.
     */
    public boolean isCleanupFailed() {
        return cleanupState == CleanupState.FAILED;
    }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    /**
     * Performs cleanup exactly once when both {@link #bodyDone} and
     * {@link #appDone} are {@code true}.
     *
     * <p>Sets {@link CleanupState#RUNNING} before invoking any downstream code
     * that could re-enter the pipeline, to guard against recursive calls.
     *
     * <p><strong>Failure isolation</strong>: only the {@link CleanupAction}
     * invocation is wrapped in the cleanup-failure catch.  Once the action
     * returns successfully, {@link CleanupState#COMPLETE} is set and
     * {@link ReadFlowHandler#onCleanupComplete} is called <em>outside</em> the
     * catch boundary.  An exception from the admission/notification path is
     * therefore never misclassified as a cleanup failure.
     *
     * <ul>
     *   <li>Cleanup failure: {@code cleanupState = FAILED}; {@link ReadFlowHandler#onCleanupFailed}
     *       is called; return without admission.</li>
     *   <li>Notification/admission failure after successful cleanup: {@code cleanupState}
     *       remains {@code COMPLETE}; the error is routed through
     *       {@link ReadFlowHandler#onCleanupNotificationFailed}.</li>
     * </ul>
     *
     * Duplicate signals cannot repeat a RUNNING, COMPLETE, or FAILED cleanup.
     */
    private void tryCleanup(ChannelHandlerContext context) {
        if (!bodyDone || !appDone) {
            return; // one condition still outstanding
        }
        if (cleanupState != CleanupState.PENDING) {
            // Already RUNNING, COMPLETE, or FAILED — duplicate signal; ignore.
            return;
        }

        cleanupState = CleanupState.RUNNING;

        // --- Cleanup failure catch boundary: wraps only the cleanup action ---
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "[LIFECYCLE] tryCleanup: executing cleanup action ch="
                        + context.channel().id());
            }
            // cleanupAction is non-null: bindCleanupAction rejected null.
            cleanupAction.execute();
        } catch (Throwable t) {
            // Cleanup failed: mark FAILED (not COMPLETE) so B is never admitted.
            cleanupState = CleanupState.FAILED;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "[LIFECYCLE] tryCleanup: FAILED ch=" + context.channel().id()
                        + " cause=" + t);
            }
            // Delegate failure handling to the established connection-failure policy.
            // Return without calling onCleanupComplete.
            ReadFlowHandler.onCleanupFailed(context, t);
            return;
        }
        // --- End cleanup failure catch boundary ---

        // Mark complete only after all release work finishes successfully.
        // This assignment is outside the catch so admission-path exceptions
        // cannot rewrite COMPLETE to FAILED.
        cleanupState = CleanupState.COMPLETE;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "[LIFECYCLE] tryCleanup: complete; triggering admission ch="
                    + context.channel().id());
        }

        // Notify the flow handler so it can drain or schedule the next read.
        // If this notification throws, A remains COMPLETE and the error is
        // routed through the connection-error path.
        try {
            ReadFlowHandler.onCleanupComplete(context);
        } catch (Throwable t) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "[LIFECYCLE] tryCleanup: notification failed (cleanup was COMPLETE) ch="
                        + context.channel().id() + " cause=" + t);
            }
            // cleanupState remains COMPLETE — cleanup succeeded.
            // Route the notification/admission error through the connection-error path.
            ReadFlowHandler.onCleanupNotificationFailed(context, t);
        }
    }
}
