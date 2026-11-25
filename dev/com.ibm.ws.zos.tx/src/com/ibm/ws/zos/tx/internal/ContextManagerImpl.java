/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.tx.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ibm.tx.util.ByteArray;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.Transaction.JTA.Util;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.zos.core.thread.ThreadLifecycleEventListener;
import com.ibm.ws.zos.thread.term.ThreadOptimizedObjectPool;
import com.ibm.ws.zos.thread.term.ThreadOptimizedObjectPool.ObjectDestroyer;
import com.ibm.ws.zos.thread.term.ThreadOptimizedObjectPool.ObjectFactory;
import com.ibm.ws.zos.tx.internal.rrs.BeginContextReturnType;
import com.ibm.ws.zos.tx.internal.rrs.RRSServices;
import com.ibm.ws.zos.tx.internal.rrs.RetrieveCurrentContextTokenReturnType;
import com.ibm.ws.zos.tx.internal.rrs.RetrieveSideInformationFastReturnType;
import com.ibm.ws.zos.tx.internal.rrs.SwitchContextReturnType;
import com.ibm.wsspi.logging.IntrospectableService;

/**
 * Manages a context pool for use by the native transaction manager.
 */
public class ContextManagerImpl implements ContextManager, IntrospectableService, ThreadLifecycleEventListener {
    private static final TraceComponent tc = Tr.register(ContextManagerImpl.class);

    /** Number of contexts to keep in the thread-level context cache. */
    private static final int THREAD_CONTEXT_POOL_MAX = 5;

    /** Number of contexts to keep in the global (for all threads) pool. */
    private static final int GLOBAL_CONTEXT_POOL_MAX = 100;

    /** Number of milliseconds to sleep between iterations of checking for all contexts to end. */
    private static final int CONTEXT_END_WAIT_TIME_MILLIS = 5000;

    /** Current context on this thread. */
    private ThreadLocalContext currentContext;

    /** Pool of contexts. */
    private ThreadOptimizedObjectPool<ContextImpl> contextPool;

    /** Map of suspended contexts to their UOW. */
    private Map<UOWCoordinator, ContextImpl> suspendedContextMap;

    /** Reference to the RRS services. */
    private RRSServices rrsServices;

    /** Destroyer for contexts. */
    private ContextDestroyer contextDestroyer;

    /** Lock used to serialize the state of the context manager. */
    private ReentrantReadWriteLock stateLock;

    /** Flag which is set when we are active and can create contexts. */
    private boolean active = false;

    /** Indicates that the the context manager destroy method was called and the it completed. */
    private boolean ctxMgrDestroyComplete;

    /** Declarative services activation call. */
    protected void activate() {
        stateLock = new ReentrantReadWriteLock(true);
        suspendedContextMap = new ConcurrentHashMap<UOWCoordinator, ContextImpl>();
        contextDestroyer = new ContextDestroyer(rrsServices);
        currentContext = new ThreadLocalContext(rrsServices, contextDestroyer);

        active = true;
    }

    /**
     * Declarative services deactivation call.
     */
    protected void deactivate() {
        destroyContextManager(0L);
    }

    /**
     * Sets the RRSServices object reference.
     */
    protected void setRRSServices(RRSServices rrsServices) {
        this.rrsServices = rrsServices;
    }

    /**
     * Clears the RRSServices object reference.
     */
    protected void unsetRRSServices(RRSServices rrsServices) {
        this.rrsServices = null;
    }

    /** {@inheritDoc} */
    @Override
    public void initialize(byte[] rmToken) {
        if (contextPool == null) {
            if (rmToken != null) {
                byte[] localRmToken = new byte[rmToken.length];
                System.arraycopy(rmToken, 0, localRmToken, 0, rmToken.length);

                contextPool = new ThreadOptimizedObjectPool<ContextImpl>(THREAD_CONTEXT_POOL_MAX, GLOBAL_CONTEXT_POOL_MAX, new ContextFactory(rrsServices, localRmToken), contextDestroyer, null);
            } else {
                throw new IllegalArgumentException("The context manager cannot be initialized with a null resource manager token");
            }
        } else {
            throw new IllegalStateException("The resource manager token has already been set on this ContextManager instance");
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInitialized() {
        return (contextPool != null);
    }

    /** {@inheritDoc} */
    @Override
    public void begin(UOWCoordinator coord) {
        Lock readLock = stateLock.readLock();
        readLock.lock();
        try {
            // ---------------------------------------------------------------
            // There should always be a context switched on the thread.
            // ---------------------------------------------------------------
            ContextImpl ctx = currentContext.get();
            if (ctx == null) {
                throw new IllegalStateException("A thread must be associated with a context to begin a new one");
            }

            // ---------------------------------------------------------------
            // If we are no longer active, we need to prevent a new context
            // from being created, and also try to remove any context on our
            // thread.
            // ---------------------------------------------------------------
            if (active == false) {
                if (ctx.equals(ContextImpl.NATIVE_CONTEXT) == false) {
                    if (ctx.getState() != ContextImpl.DESTROYED) {
                        switchContextOntoThisThread(ContextImpl.NATIVE_CONTEXT, ctx);
                        contextDestroyer.destroy(ctx);
                    }

                    currentContext.set(ContextImpl.NATIVE_CONTEXT);
                }

                throw new IllegalStateException("The context manager has been deactivated.");
            }

            // ---------------------------------------------------------------
            // Make sure that we were told what our RM name is and made a pool.
            // ---------------------------------------------------------------
            if (contextPool == null) {
                throw new IllegalStateException("The context manager has not been initialized by the native transaction manager");
            }

            // ------------------------------------------------------------------
            // If the current context is the native context, we'll replace it with a
            // privately managed context from our pool.  In the future, there should
            // be a way to switch the context on first-work.  This may happen here
            // in the context manager, or it might happen a level higher, in which
            // case begin() is called when the first work occurs.
            // ------------------------------------------------------------------
            if (ctx.equals(ContextImpl.NATIVE_CONTEXT)) {
                ContextImpl newCtx = contextPool.get();
                if (newCtx != null) {
                    switchContextOntoThisThread(newCtx, ctx);
                    currentContext.set(newCtx);
                    ctx = newCtx;
                } else {
                    throw new IllegalStateException("A context could not be obtained from the context pool");
                }
            } else {
                if (ctx.getState() != ContextImpl.CLEAN) {
                    throw new IllegalStateException("This thread is currently associated with another context: " + ctx);
                }
            }

            ctx.setState(ContextImpl.ACTIVE);
            ctx.setUnitOfWork(coord);
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void end(UOWCoordinator coord) {
        final Lock readLock = stateLock.readLock();
        readLock.lock();
        try {
            // ----------------------------------------------------------------
            // Make sure we initialized the context pool.  During deactivation,
            // we won't have a context pool.
            // ----------------------------------------------------------------
            if ((contextPool == null) && (active)) {
                throw new IllegalStateException("The context manager has not been initialized by the native transaction manager");
            }

            boolean isNativeContext = false;

            // --------------------------------------------------------------------
            // Check for a suspended transaction first.
            // --------------------------------------------------------------------
            ContextImpl ctx = suspendedContextMap.get(coord);
            if (ctx != null) {
                if (ctx.getState() != ContextImpl.SUSPENDED) {
                    throw new IllegalStateException("Suspended context [" + ctx + "] is in the wrong state to end");
                }

                isNativeContext = ctx.equals(ContextImpl.NATIVE_CONTEXT);
            } else {
                // ----------------------------------------------------------------
                // Check if the current transaction is the right one.  Note that
                // it's possible that end could be the first call that we get, since
                // our service can be activated at any time.
                // ----------------------------------------------------------------
                ctx = currentContext.get();
                if (ctx == null) {
                    throw new IllegalStateException("Could not find a suspended or active context for this work: " + coord);
                }

                isNativeContext = ctx.equals(ContextImpl.NATIVE_CONTEXT);

                if ((ctx.getState() != ContextImpl.ACTIVE) && (isNativeContext == false)) {
                    throw new IllegalStateException("Current context [" + ctx + "] is in the wrong state to end");
                }
            }

            // Make sure we are finished with the work that we think we're finished with.
            if ((coord.equals(ctx.getUnitOfWork()) == false) && (isNativeContext == false)) {
                throw new IllegalStateException("Context [" + ctx + "] is not associated with work: " + coord);
            }

            // --------------------------------------------------------------------
            // Make sure RRS thinks the context is clean.  If the context we are
            // ending is the native context, we need to get the context token from
            // RRS before calling RUSF, because RRS does not let us specify binary
            // zeros to represent the native context.
            // --------------------------------------------------------------------
            byte[] ctxToken;
            if (isNativeContext == false) {
                ctxToken = ctx.getContextToken();
            } else {
                RetrieveCurrentContextTokenReturnType rcctrt = rrsServices.retrieveCurrentContextToken();
                if ((rcctrt == null) || (rcctrt.getReturnCode() != 0)) {
                    throw new RuntimeException("Could not get context token for the native context on this thread");
                }
                ctxToken = rcctrt.getContextToken();
            }

            RetrieveSideInformationFastReturnType rusfRt = rrsServices.retrieveSideInformationFast(ctxToken, 0);
            if ((rusfRt != null) && (rusfRt.getReturnCode() == 0)) {
                if (rusfRt.isURStateInReset() == false) {
                    ContextImpl oldCtx = null;

                    try {
                        // If the context we are looking for has been suspended, resume it.
                        if (ctx.getState() == ContextImpl.SUSPENDED) {
                            oldCtx = currentContext.get();
                            switchContextOntoThisThread(ctx, oldCtx);
                            suspendedContextMap.remove(coord);
                            ctx.setState(ContextImpl.ACTIVE);
                            currentContext.set(ctx);
                        }
                        // End the UR on the current thread.
                        int endUrRt = rrsServices.endUR(RRSServices.ATR_ROLLBACK_ACTION, null);
                        if (endUrRt == 0) {
                            String ctxTokenString = (ctxToken != null) ? new ByteArray(ctxToken).toString() : "null";
                            Tr.warning(tc, "CTXMGR_DIRTY_UR_ON_END_ROLLBACK", new Object[] { coord, ctxTokenString });
                        } else {
                            throw new RuntimeException("EndUR did not complete successfully for context [" + currentContext.get() + "], " + endUrRt);
                        }
                    } finally {
                        // Return the old context back
                        if (oldCtx != null) {
                            switchContextOntoThisThread(oldCtx, ctx);
                            suspendedContextMap.put(coord, ctx);
                            ctx.setState(ContextImpl.SUSPENDED);
                            currentContext.set(oldCtx);
                        }
                    }
                }
            } else {
                throw new RuntimeException("RetrieveSideInformationFast did not complete successfully for context [" + ctx + "], " + rusfRt);
            }

            // -------------------------------------------------------------------
            // Do the right thing based on the context state.  Active contexts
            // can be re-used by the next request.  Suspended contexts need to
            // be returned to the context pool.  Note that if the thread ending
            // the context is not the one that ran the work, the context is going
            // to get associated with a different thread's local cache.  If we're
            // using the native context, take this opportunity to replace it with
            // a privately managed context.
            // -------------------------------------------------------------------
            int oldState = ctx.getState();
            ctx.setState(ContextImpl.CLEAN);
            ctx.setUnitOfWork(null);

            if (oldState == ContextImpl.SUSPENDED) {
                suspendedContextMap.remove(coord);

                // ------------------------------------------------------------
                // If we're deactivating, end the context instead of pooling it.
                // ------------------------------------------------------------
                if (isNativeContext == false) {
                    if ((active) && (contextPool != null)) {
                        contextPool.put(ctx);
                    } else {
                        contextDestroyer.destroy(ctx);
                    }
                }
            } else {
                // ------------------------------------------------------------
                // If we're deactivating, and we have a privately managed
                // context, we need to end it.
                // ------------------------------------------------------------
                if (isNativeContext) {
                    if ((active) && (contextPool != null)) {
                        ContextImpl newCtx = contextPool.get();
                        if (newCtx != null) {
                            switchContextOntoThisThread(newCtx, ctx);
                            currentContext.set(newCtx);
                        }
                    }
                } else {
                    if (active == false) {
                        switchContextOntoThisThread(ContextImpl.NATIVE_CONTEXT, ctx);
                        currentContext.set(ContextImpl.NATIVE_CONTEXT);
                        contextDestroyer.destroy(ctx);
                    }
                }
            }
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void suspend(UOWCoordinator coord) {
        final Lock readLock = stateLock.readLock();
        readLock.lock();
        try {
            ContextImpl ctx = currentContext.get();

            if (ctx == null) {
                throw new IllegalStateException("There is no current work unit on this thread");
            }

            // ----------------------------------------------------------------
            // Make sure we initialized the context pool.  During deactivation,
            // we won't have a context pool.
            // ----------------------------------------------------------------
            if ((contextPool == null) && (active)) {
                throw new IllegalStateException("The context manager has not been initialized by the native transaction manager");
            }

            boolean isNativeContext = ctx.equals(ContextImpl.NATIVE_CONTEXT);

            if (suspendedContextMap.containsKey(coord)) {
                ContextImpl suspendedCtx = suspendedContextMap.get(coord);
                throw new IllegalStateException("There is already a suspended context [" + suspendedCtx + "] associated with this UOW: " + coord);
            }

            if ((ctx.getState() != ContextImpl.ACTIVE) && (isNativeContext == false)) {
                throw new IllegalStateException("Context [" + ctx + "] is in an invalid state to be suspended");
            }

            if ((coord.equals(ctx.getUnitOfWork()) == false) && (isNativeContext == false)) {
                throw new IllegalStateException("Context [" + ctx + "] is not associated with the unit of work trying to suspend it: " + coord);
            }

            // ----------------------------------------------------------------
            // Get another context to put on the thread.  If we're
            // deactivating, we need to put the native context on the thread.
            // ----------------------------------------------------------------
            ContextImpl newCtx = ((active) && (contextPool != null)) ? contextPool.get() : ContextImpl.NATIVE_CONTEXT;
            if (newCtx == null) {
                throw new RuntimeException("Could not get a new context from the pool");
            }

            switchContextOntoThisThread(newCtx, ctx);
            suspendedContextMap.put(coord, ctx);
            ctx.setState(ContextImpl.SUSPENDED);
            currentContext.set(newCtx);
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void resume(UOWCoordinator coord) {
        Lock readLock = stateLock.readLock();
        readLock.lock();
        try {
            ContextImpl currentCtx = currentContext.get();

            if (currentCtx == null) {
                throw new IllegalStateException("There is no current work unit on this thread");
            }

            // ----------------------------------------------------------------
            // Make sure we initialized the context pool.  During deactivation,
            // we won't have a context pool.
            // ----------------------------------------------------------------
            if ((active) && (contextPool == null)) {
                throw new IllegalStateException("The context manager has not been initialized by the native transaction manager");
            }

            boolean isCurrentNativeContext = currentCtx.equals(ContextImpl.NATIVE_CONTEXT);

            if ((currentCtx.getState() != ContextImpl.CLEAN) && (isCurrentNativeContext == false)) {
                throw new IllegalStateException("The current context [" + currentCtx + "] is not clean and cannot be replaced");
            }

            ContextImpl ctx = suspendedContextMap.get(coord);
            if (ctx == null) {
                throw new IllegalStateException("There is no suspended context for this UOW: " + coord);
            }

            boolean isNativeContext = ctx.equals(ContextImpl.NATIVE_CONTEXT);

            if (ctx.getState() != ContextImpl.SUSPENDED) {
                throw new IllegalStateException("The context [" + ctx + "] associated with this UOW is not suspended: " + coord);
            }

            if ((coord.equals(ctx.getUnitOfWork()) == false) && (isNativeContext == false)) {
                throw new IllegalStateException("Context [" + ctx + "] is not associated with the unit of work trying to resume it: " + coord);
            }

            switchContextOntoThisThread(ctx, currentCtx);
            suspendedContextMap.remove(coord);
            ctx.setState(ContextImpl.ACTIVE);
            currentContext.set(ctx);

            // ----------------------------------------------------------------
            // If we are not deactivating, we can put the clean context that
            // was previously on the thread into the context pool.
            // ----------------------------------------------------------------
            if (isCurrentNativeContext == false) {
                if ((active) && (contextPool != null)) {
                    contextPool.put(currentCtx);
                } else {
                    contextDestroyer.destroy(currentCtx);
                }
            }
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Switches a context onto this thread.
     *
     * @param newContext         The context to switch on to this thread. If null, the
     *                               native context is switched on to this thread.
     * @param expectedOldContext The context which we expected to be on this
     *                               thread originally. If null, we expect the
     *                               native context to be on the thread.
     */
    private void switchContextOntoThisThread(ContextImpl newContext, ContextImpl expectedOldContext) {
        // If we weren't given a context to use, switch the native context back on the thread.
        byte[] token = (newContext == null) ? null : newContext.getContextRegistryToken();
        SwitchContextReturnType scrt = rrsServices.contextSwitch(token);
        if ((scrt == null) || (scrt.getReturnCode() != 0)) {
            throw new RuntimeException("Could not switch context onto thread, " + scrt);
        }

        // Make sure that the context we switched off was the one we expected.
        token = (expectedOldContext == null) ? ContextImpl.NATIVE_CONTEXT.getContextToken() : expectedOldContext.getContextToken();
        byte[] oldToken = scrt.getOldCtxToken();

        // The context was not what we expected. Switch the old context back on the thread.
        if (Arrays.equals(token, oldToken) == false) {
            String message = "";
            byte[] oldCtxRegistryToken = findCtxRegistryToken(oldToken);
            if (oldCtxRegistryToken != null) {
                scrt = rrsServices.contextSwitch(oldToken);
                if ((scrt == null) || (scrt.getReturnCode() != 0)) {
                    message = ". Unable to switch context [" + Util.toHexString(oldToken) + "] back on the thread. " + scrt;
                }
            } else {
                message = ". Registry token for context [" + Util.toHexString(oldToken) + "] not found. Attempt to put context back on thread failed." + scrt;
            }

            throw new IllegalStateException("Previous context [" + new ContextImpl(oldToken, null) + "] was not expected [" + expectedOldContext + "]" + message);
        }
    }

    /**
     * Finds the registry token associated to the input context token.
     * It iterates over all active and suspended contexts.
     *
     * @param ctxToken The context token whose associated registry is to be retrieved.
     *
     * @return The context registry token associated with the input context token.
     */
    protected byte[] findCtxRegistryToken(byte[] ctxToken) {
        byte[] registryToken = null;

        // Check all active contexts.
        Map<Thread, ContextImpl> activeCtxMap = currentContext.participatingThreads;
        if (activeCtxMap != null && activeCtxMap.size() > 0) {
            for (ContextImpl ctx : activeCtxMap.values()) {
                byte[] knownToken = ctx.getContextToken();
                if (Arrays.equals(ctxToken, knownToken)) {
                    return ctx.getContextInterestRegistryToken();
                }
            }
        }

        // Check all suspended contexts.
        for (ContextImpl ctx : suspendedContextMap.values()) {
            byte[] knownToken = ctx.getContextToken();
            if (Arrays.equals(ctxToken, knownToken)) {
                return ctx.getContextInterestRegistryToken();
            }
        }

        return registryToken;
    }

    /** {@inheritDoc} */
    @Override
    public void destroyContextManager(long timeoutMillis) {
        // -------------------------------------------------------------------
        // Get the write lock so that we can update our state.  From this
        // point on we should not be creating new contexts, and we will clean
        // up any contexts that we can.
        // -------------------------------------------------------------------
        final Lock writeLock = stateLock.writeLock();
        writeLock.lock();
        try {
            if (active == false) {
                return;
            }

            active = false;
            contextDestroyer.setTerminating(true);

            // ---------------------------------------------------------------
            // Get rid of the context pool first -- these contexts are clean
            // and are not set current on any thread, so they should go away
            // without trouble.
            // ---------------------------------------------------------------
            if (contextPool != null) {
                contextPool.destroyAllObjects();
                contextPool = null;
            }

            // ---------------------------------------------------------------
            // Now we need to get rid of the contexts that are suspended, or
            // are current on an active thread.  We are only going to wait for
            // contexts which are dirty (have outstanding work).  We treat any
            // suspended context as dirty.
            // ---------------------------------------------------------------
            boolean waitForDirtyThreads = ((suspendedContextMap.size() > 0) || (currentContext.isAnyThreadDirty()));
            long timeLeftToWaitMillis = timeoutMillis;
            while (waitForDirtyThreads == true) {
                long waitMillisThisRound = Math.min(timeLeftToWaitMillis, CONTEXT_END_WAIT_TIME_MILLIS);

                // No need to release the write lock, if we know we know we are not waiting.
                if (waitMillisThisRound > 0) {
                    writeLock.unlock();
                    try {
                        Thread.sleep(waitMillisThisRound);
                    } catch (InterruptedException iex) {
                        /* Don't do anything, we're not going to wait forever. */
                    } finally {
                        writeLock.lock();
                    }
                }

                // -----------------------------------------------------------
                // If still dirty threads, issue messages describing what
                // we're waiting for.
                // -----------------------------------------------------------
                waitForDirtyThreads = ((suspendedContextMap.size() > 0) || (currentContext.isAnyThreadDirty()));
                if (waitForDirtyThreads == true) {
                    timeLeftToWaitMillis -= waitMillisThisRound;
                    for (Map.Entry<UOWCoordinator, ContextImpl> entry : suspendedContextMap.entrySet()) {
                        UOWCoordinator coord = entry.getKey();
                        ContextImpl ctx = entry.getValue();
                        byte[] ctxToken = ctx.getContextToken();
                        // ctxToken known to be non-null (getContextToken always constructs a new one
                        String ctxTokenString = new ByteArray(ctxToken).toString();
                        String messageId = (timeLeftToWaitMillis > 0) ? "CTXMGR_WAIT_FOR_SUSPENDED_CTX" : "CTXMGR_TERMINATING_CTX_WITH_WORK";
                        Tr.info(tc, messageId, new Object[] { ctxTokenString, coord });
                    }

                    Map<Thread, ContextImpl> dirtyThreadMap = currentContext.getDirtyThreads();
                    for (Map.Entry<Thread, ContextImpl> entry : dirtyThreadMap.entrySet()) {
                        Thread thd = entry.getKey();
                        ContextImpl ctx = entry.getValue();
                        UOWCoordinator coord = ctx.getUnitOfWork();
                        byte[] ctxToken = ctx.getContextToken();
                        // ctxToken known to be non-null (getContextToken always constructs a new one
                        String ctxTokenString = new ByteArray(ctxToken).toString();
                        if (timeLeftToWaitMillis > 0) {
                            Tr.info(tc, "CTXMGR_WAIT_FOR_ACTIVE_CTX", new Object[] { ctxTokenString, thd.getName(), coord });
                        } else {
                            Tr.info(tc, "CTXMGR_TERMINATING_CTX_WITH_WORK", new Object[] { ctxTokenString, coord });
                        }
                    }

                    if (timeLeftToWaitMillis <= 0) {
                        waitForDirtyThreads = false;
                    }
                }
            }

            // ---------------------------------------------------------------
            // Finally, end any contexts that are left.  This will include
            // clean contexts in the current context map, and any active
            // contexts left over from our waiting period above.  We will
            // only end the context if ATR4RUSF reports that it's in-reset.
            // ---------------------------------------------------------------
            for (ContextImpl ctx : suspendedContextMap.values()) {
                try {
                    byte[] ctxToken = ctx.getContextToken();
                    RetrieveSideInformationFastReturnType rusfRt = rrsServices.retrieveSideInformationFast(ctxToken, 0);
                    if ((rusfRt != null) && (rusfRt.getReturnCode() == RRSServices.ATR_OK) && (rusfRt.isURStateInReset())) {
                        contextDestroyer.destroy(ctx);
                    }
                } catch (Throwable t) {
                    // Continue with the next context. Let FFDC do the logging.
                }
            }
            currentContext.endAllCurrentContexts();
            ctxMgrDestroyComplete = true;
        } finally {
            writeLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void threadStarted() {
        // Nothing to do here.
    }

    /** {@inheritDoc} */
    @Override
    public void threadTerminating() {

        // Acquire a reader lock before we process thread termination. The intent here
        // is to make thread termination and context manager termination mutually exclusive
        // if thread termination is called first or if context manager termination gets called
        // first with no time to wait for pending transactions to complete.
        Lock readLock = stateLock.readLock();
        readLock.lock();
        try {
            // If the context manager is no longer active and we are here, two things have happened:
            // 1. Context manager termination completed. All contexts went through termination (or not).
            // 2. Context termination started, but the writer lock was released to wait for a x amount of time
            //    for transactions to resolve themselves.
            // In both of the above cases the context pool has already been invalidated.
            // In case 1, switch the native context on the thread (medium weight thread issue), and exit.
            // In case 2, preempt context termination by going through the thread termination logic.
            // We can do this because the context manager destroy path will not be able to get the
            // lock until we are done.
            if (ctxMgrDestroyComplete) {
                SwitchContextReturnType scrt = rrsServices.contextSwitch(null);
                if ((scrt == null) || (scrt.getReturnCode() != 0)) {
                    // The native context is already on the thread. Nothing to do.
                    if (scrt != null && scrt.getReturnCode() == RRSServices.CTX_CURRENT_WU_NATIVE) {
                        return;
                    }

                    throw new RuntimeException("The attempt to switch a native context on terminating thread: " + Thread.currentThread() + " with ID: "
                                               + Thread.currentThread().getId() + " failed. " + scrt);
                }
                return;
            }

            // Get the current context.
            ContextImpl currentCtx = currentContext.get();

            if (currentCtx == null) {
                throw new IllegalStateException("Failed to obtain context on terminating thread " + getCurrentThreadInfo() + ".");
            }

            // If the current context is not a native one ...
            if (!currentCtx.equals(ContextImpl.NATIVE_CONTEXT)) {
                // Make sure that the UR under the current context is clean.
                byte[] ctxToken = currentCtx.getContextToken();

                RetrieveSideInformationFastReturnType rusfRt = rrsServices.retrieveSideInformationFast(ctxToken, 0);
                if (rusfRt == null || rusfRt.getReturnCode() != 0) {
                    throw new IllegalStateException("Thread " + getCurrentThreadInfo() + " is terminating. Failed to retrieve side " +
                                                    "information using context token " + Util.toHexString(ctxToken) + ". " + rusfRt);
                }

                // If the UR under the current context is not clean, try to end the work.
                boolean endedDirtyUr = false;
                boolean terminateCtx = false;
                if (rusfRt.isURStateInReset() == false) {
                    int endUrRt = rrsServices.endUR(RRSServices.ATR_ROLLBACK_ACTION, null);
                    if (endUrRt == 0) {
                        endedDirtyUr = true;
                    } else {
                        terminateCtx = true;
                    }
                }

                // Switch a native context onto the current thread.
                switchContextOntoThisThread(ContextImpl.NATIVE_CONTEXT, currentCtx);

                // Terminate the context or put it back in the local thread pool.
                if (terminateCtx) {
                    try {
                        Tr.warning(tc, "THREAD_TERM_TERMINATING_CTX_WITH_DIRTY_UR", new Object[] { getCurrentThreadInfo(), Util.toHexString(ctxToken) });
                        contextDestroyer.destroy(currentCtx);
                    } catch (Throwable t) {

                        IllegalStateException ise = new IllegalStateException("Failed to terminate context " + Util.toHexString(ctxToken) +
                                                                              ". The context is associated with a dirty UR on terminatig thread: " + getCurrentThreadInfo());
                        ise.initCause(t);
                        throw ise;
                    }
                } else {
                    // If the context pool is null, we are going through context manager termination. Destroy the context now.
                    if (contextPool == null) {
                        try {
                            contextDestroyer.destroy(currentCtx);
                        } catch (Throwable t) {
                            IllegalStateException ise = new IllegalStateException("Failed to terminate context " + Util.toHexString(ctxToken) +
                                                                                  ". The contex is associated with a clean UR on termianting thread: " + getCurrentThreadInfo());
                            ise.initCause(t);
                            throw ise;
                        }
                    } else {
                        currentCtx.setState(ContextImpl.CLEAN);
                        currentCtx.setUnitOfWork(null);
                        contextPool.put(currentCtx);
                        if (endedDirtyUr) {
                            Tr.warning(tc, "THREAD_TERM_DIRTY_UR_ENDED_POOLED", new Object[] { getCurrentThreadInfo(), Util.toHexString(ctxToken) });
                        }
                    }
                }
            }

            // -------------------------------------------------------------------
            // Tell the context pool that the thread is terminating. The pool will
            // move the contexts in the thread's local cache to the global cache.
            // Note that if there is a context on the thread we assume it is
            // clean. After all, we try very hard to make sure it is .
            // -------------------------------------------------------------------
            ThreadOptimizedObjectPool<ContextImpl> pool = contextPool;
            if (pool != null) {
                pool.threadTerminated(Thread.currentThread());
            }

            // -------------------------------------------------------------------
            // Tell the current context ThreadLocal that the thread is terminating
            // so that it removes the thread from the map of active threads.
            // -------------------------------------------------------------------
            currentContext.threadTerminated(Thread.currentThread());
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Returns some information about the current executing thread.
     *
     * @return The current thread's information.
     */
    private String getCurrentThreadInfo() {
        StringBuilder threadInfo = new StringBuilder();
        Thread currentThread = Thread.currentThread();
        ThreadGroup group = currentThread.getThreadGroup();
        threadInfo.append(currentThread.getName());
        threadInfo.append("[Id: ");
        threadInfo.append(currentThread.getId());
        threadInfo.append(", Group: ");
        threadInfo.append((group == null) ? "null" : group.getName());
        threadInfo.append("]");
        return threadInfo.toString();
    }

    /** {@inheritDoc} */
    @Override
    public ContextImpl getCurrentContext() {
        return currentContext.get();
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return this.getClass().getCanonicalName() + " data introspection.";
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    /** {@inheritDoc} */
    @Override
    public void introspect(OutputStream out) throws IOException {
        StringBuilder sb = new StringBuilder();

        sb.append("\nContextManager Data.");
        sb.append("\n\n1. Summary. ");
        sb.append("\nIsBundleActive                 : ");
        sb.append(active);
        sb.append("\n\n2. Object References. ");
        sb.append("\nThreadLocalContext             : ");
        sb.append(currentContext);
        sb.append("\nThreadOptimizedObjectPool      : ");
        sb.append(contextPool);
        sb.append("\nRRSServices                    : ");
        sb.append(rrsServices);
        sb.append("\nContextDestroyer               : ");
        sb.append(contextDestroyer);
        sb.append("\nReentrantReadWriteLock         : ");
        sb.append(stateLock);

        sb.append("\n\n3. Context Pool Data.");
        sb.append(getPoolData());

        // Write to the stream and flush the data. The stream is closed by the caller.
        String introspectionData = sb.toString();
        out.write(introspectionData.getBytes());
        out.flush();
    }

    /**
     * Collects pool data from the active, suspended, and free pools.
     *
     * @return Context pool data.
     */
    @Trivial
    @FFDCIgnore(Throwable.class)
    public String getPoolData() {
        StringBuilder sb = new StringBuilder();
        Lock readLock = stateLock.readLock();
        readLock.lock();
        try {
            // Gather active context data.
            Map<Thread, ContextImpl> activeCtxMap = currentContext.participatingThreads;
            sb.append("\n\nA. Active Contexts.");
            if (activeCtxMap != null && activeCtxMap.size() > 0) {
                for (Map.Entry<Thread, ContextImpl> entry : activeCtxMap.entrySet()) {
                    sb.append("\nThread: " + entry.getKey().getId());
                    sb.append(" [");
                    sb.append(entry.getValue().toString());
                    sb.append("]");
                }
            } else {
                sb.append("\nNo active contexts found.");
            }

            // Gather suspended context data.
            Collection<ContextImpl> ctxs = suspendedContextMap.values();
            sb.append("\n\nB. Suspended Contexts.");
            if (ctxs != null && ctxs.size() > 0) {
                for (ContextImpl ctx : ctxs) {
                    sb.append("\n" + ctx.toString());
                }
            } else {
                sb.append("\nNo contexts found in suspended context pool.");
            }

            // Gather free (available) context data.
            sb.append("\n\nC. Free context pool Data.");
            String freeContextPoolData = contextPool.getPoolData();
            if (freeContextPoolData != null && !freeContextPoolData.equals("")) {
                sb.append("\n\n" + freeContextPoolData);
            } else {
                sb.append("\nNo contexts found in Free context pools.");
            }
        } catch (Throwable t) {
            sb.append("ContextManagerImpl.getPoolData. Error: " + t.toString());
        } finally {
            readLock.unlock();
        }

        return sb.toString();
    }

    /** Context factory. */
    public static class ContextFactory implements ObjectFactory<ContextImpl> {
        /** Reference to RRS services. */
        private final RRSServices rrsServices;

        /** Reference to the resource manager registry token. */
        private final byte[] rmRegistryToken;

        /**
         * Constructor
         *
         * @param rrsServices     The RRS services service.
         * @param rmRegistryToken The RM registry token to use when creating contexts.
         */
        ContextFactory(RRSServices rrsServices, byte[] rmRegistryToken) {
            this.rrsServices = rrsServices;
            this.rmRegistryToken = rmRegistryToken;
        }

        /**
         * Creates an RRS context
         *
         * @return A new RRS context, or null if a context could not be created.
         */
        @Override
        public ContextImpl create() {
            ContextImpl ctx = null;
            BeginContextReturnType bcrt = rrsServices.beginContext(rmRegistryToken);
            if ((bcrt != null) && (bcrt.getReturnCode() == 0)) {
                byte[] ctxToken = bcrt.getContextToken();
                byte[] ctxRegistryToken = bcrt.getContextRegistryToken();
                ctx = new ContextImpl(ctxToken, ctxRegistryToken);
            }

            return ctx;
        }
    }

    /** Context destroyer. */
    public static class ContextDestroyer implements ObjectDestroyer<ContextImpl> {
        /** Reference to RRS services. */
        private final RRSServices rrsServices;

        /** Flag set when we are going through termination and should ignore service failures. */
        private boolean terminating = false;

        /**
         * Constructor
         *
         * @param rrsServices The RRS services service.
         */
        ContextDestroyer(RRSServices rrsServices) {
            this.rrsServices = rrsServices;
        }

        /**
         * Sets the terminating flag
         *
         * @param terminating Set to true if the context manager is terminating
         *                        and we should ignore service failures.
         */
        public void setTerminating(boolean terminating) {
            this.terminating = terminating;
        }

        /**
         * Destroys a context object
         *
         * @param ctx The context to destroy.
         */
        @Override
        public void destroy(ContextImpl ctx) {
            ctx.setState(ContextImpl.DESTROYED);
            byte[] ctxRegistryToken = ctx.getContextRegistryToken();
            // If we are terminating, always force termination. The registry entry for the token
            // we are trying to end will be cleaned up right after ctx4end is called.
            int terminationType = (terminating) ? RRSServices.CTX_FORCED_END_OF_CONTEXT : RRSServices.CTX_NORMAL_TERMINATION;
            int rc = rrsServices.endContext(ctxRegistryToken, terminationType);
            if (rc != 0) {
                if (terminating == false) {
                    throw new RuntimeException("MVS Context could not be ended, RC = " + rc);
                }
            }
        }
    }

    /**
     * ThreadLocal extension to help us determine when all threads which had set
     * a current context have terminated, or have cleared their current context.
     */
    public static class ThreadLocalContext extends ThreadLocal<ContextImpl> {

        /** Set of threads which are participating. */
        private final Map<Thread, ContextImpl> participatingThreads = new ConcurrentHashMap<Thread, ContextImpl>();

        /** Context destroyer reference. */
        private final ContextDestroyer destroyer;

        /** RRS Services, for ATR4RUSF. */
        private final RRSServices rrsServices;

        /** Constructor */
        public ThreadLocalContext(RRSServices rrsServices, ContextDestroyer destroyer) {
            super();
            this.rrsServices = rrsServices;
            this.destroyer = destroyer;
        }

        @Override
        public ContextImpl get() {
            return super.get();
        }

        @Override
        protected ContextImpl initialValue() {
            return ContextImpl.NATIVE_CONTEXT;
        }

        @Override
        public void remove() {
            super.set(initialValue());
            participatingThreads.remove(Thread.currentThread());
        }

        @Override
        public void set(ContextImpl value) {
            if ((value == null) || (value.equals(initialValue()))) {
                remove();
            } else {
                super.set(value);
                participatingThreads.put(Thread.currentThread(), value);
            }
        }

        /**
         * Tells that a thread has terminated. If this thread had a current
         * context, it is now destroyed or pooled.
         */
        public void threadTerminated(Thread thd) {
            participatingThreads.remove(thd);
        }

        /**
         * Tells the caller if any threads have a dirty context.
         *
         * @return true if any threads have a ContextImpl object which is not
         *         in the clean state, and false if all threads have a
         *         ContextImpl in the clean state.
         */
        public boolean isAnyThreadDirty() {
            boolean clean = true;
            for (Map.Entry<Thread, ContextImpl> entry : participatingThreads.entrySet()) {
                ContextImpl ctx = entry.getValue();
                if (ctx.getState() != ContextImpl.CLEAN) {
                    clean = false;
                }
            }

            return (clean == false);
        }

        /**
         * Gets a map containing all threads which have a dirty context on
         * them, and the associated ContextImpl.
         *
         * @return A map of threads to contexts. Each thread in the map has a
         *         ContextImpl which is dirty.
         */
        public Map<Thread, ContextImpl> getDirtyThreads() {
            Map<Thread, ContextImpl> dirtyThreadMap = new HashMap<Thread, ContextImpl>();
            for (Map.Entry<Thread, ContextImpl> entry : participatingThreads.entrySet()) {
                Thread thd = entry.getKey();
                ContextImpl ctx = entry.getValue();
                if (ctx.getState() != ContextImpl.CLEAN) {
                    dirtyThreadMap.put(thd, ctx);
                }
            }
            return dirtyThreadMap;
        }

        /**
         * Tries to end the context on any thread which has a current context
         * set. The context will only be ended if ATR4RUSF reports that the
         * UR attached to the context is IN-RESET.
         */
        public void endAllCurrentContexts() {
            for (Map.Entry<Thread, ContextImpl> entry : participatingThreads.entrySet()) {
                Thread thd = entry.getKey();
                ContextImpl ctx = entry.getValue();
                try {
                    // ----------------------------------------------------
                    // If we can destroy the context, it's still going to
                    // be set in the ThreadLocal, and there's nothing we
                    // can do about that.
                    // ----------------------------------------------------
                    byte[] ctxToken = ctx.getContextToken();
                    RetrieveSideInformationFastReturnType rusfRt = rrsServices.retrieveSideInformationFast(ctxToken, 0);
                    if ((rusfRt != null) && (rusfRt.getReturnCode() == RRSServices.ATR_OK) && (rusfRt.isURStateInReset())) {
                        destroyer.destroy(ctx);
                    }
                    participatingThreads.remove(thd);
                } catch (Throwable t) {
                    /* Just absorb and try the next context. */
                }
            }
        }
    }

    /** Class representing a context. */
    public static class ContextImpl implements Context {
        /** State when the context is not associated with any work. */
        public static final int CLEAN = 0;

        /** State when the context is involved in a transaction. */
        public static final int ACTIVE = 1;

        /** State when the context is suspended. */
        public static final int SUSPENDED = 2;

        /** State when the context has been destroyed asynchronously. */
        public static final int DESTROYED = 3;

        /** Context representing the native RRS context. */
        public static final ContextImpl NATIVE_CONTEXT = new ContextImpl(new byte[16], null);

        /** The RRS context identifier. */
        private final byte[] ctxToken;

        /** The token for the context in the native registry. */
        private final byte[] ctxRegistryToken;

        /** The context interest token, if one was obtained. */
        private byte[] contextInterestRegistryToken;

        /** The internal state of the context (not the RRS state). */
        private int ctxState;

        /** The unit of work that is currently associated with this context. */
        private UOWCoordinator coord;

        /**
         * Wrap an RRS context.
         *
         * @param token         The RRS context token.
         * @param registryToken The registry token where the RRS context token
         *                          is stored. The registry token will be used
         *                          on future switch and end calls.
         */
        ContextImpl(byte[] token, byte[] registryToken) {
            ctxToken = token;
            ctxState = CLEAN;
            coord = null;
            ctxRegistryToken = registryToken;
        }

        /**
         * Gets the context token.
         *
         * @return The RRS context token.
         */
        @Override
        public byte[] getContextToken() {
            byte[] findbugsFriendlyContextToken = new byte[ctxToken.length];
            System.arraycopy(ctxToken, 0, findbugsFriendlyContextToken, 0, ctxToken.length);
            return findbugsFriendlyContextToken;
        }

        /**
         * Gets the registry token.
         *
         * The registry token used to obtain the RRS context token from the
         * registry.
         */
        @Override
        public byte[] getContextRegistryToken() {
            byte[] findbugsFriendlyContextRegistryToken = null;
            if (ctxRegistryToken != null) {
                findbugsFriendlyContextRegistryToken = new byte[ctxRegistryToken.length];
                System.arraycopy(ctxRegistryToken, 0, findbugsFriendlyContextRegistryToken, 0, ctxRegistryToken.length);
            }
            return findbugsFriendlyContextRegistryToken;
        }

        /**
         * Gets the context state.
         *
         * @return The internal state of the context (not the context services
         *         state).
         */
        public int getState() {
            return ctxState;
        }

        /**
         * Sets the current context state.
         *
         * @param newState The new internal state for the context.
         */
        public void setState(int newState) {
            ctxState = newState;
        }

        /**
         * Gets the current unit of work currently associated with this context.
         *
         * @return The UOWCoordinator which is associated with this context.
         */
        public UOWCoordinator getUnitOfWork() {
            return coord;
        }

        /**
         * Sets the current unit of work.
         *
         * @param coord The UOWCoordinator which is to be associated with this
         *                  context.
         */
        public void setUnitOfWork(UOWCoordinator coord) {
            this.coord = coord;
        }

        /**
         * Converts the context state to a string.
         *
         * @return A string representing the current internal state of this
         *         context.
         */
        @Trivial
        private String getContextStateString() {
            switch (ctxState) {
                case CLEAN:
                    return "CLEAN";
                case ACTIVE:
                    return "ACTIVE";
                case SUSPENDED:
                    return "SUSPENDED";
                case DESTROYED:
                    return "DESTROYED";
                default:
                    return "UNKNOWN";
            }
        }

        /**
         * Prints context information.
         *
         * @return A string representation of this object.
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ContextImpl [Token: ");
            sb.append((ctxToken == null) ? "NULL" : Util.toHexString(ctxToken));
            sb.append(", RegistryToken: ");
            sb.append((ctxRegistryToken == null) ? "NULL" : Util.toHexString(ctxRegistryToken));
            sb.append(", State: ");
            sb.append(getContextStateString());
            sb.append(", UOWCoordinator: ");
            sb.append((coord == null) ? "NULL" : coord);
            sb.append("]");
            return sb.toString();
        }

        /** {@inheritDoc} */
        @Override
        public byte[] getContextInterestRegistryToken() {
            byte[] contextInterestRegistryToken = null;
            if (this.contextInterestRegistryToken != null) {
                contextInterestRegistryToken = new byte[this.contextInterestRegistryToken.length];
                System.arraycopy(this.contextInterestRegistryToken, 0, contextInterestRegistryToken, 0, contextInterestRegistryToken.length);
            }
            return contextInterestRegistryToken;
        }

        /** {@inheritDoc} */
        @Override
        public void setContextInterestRegistryToken(byte[] token) {
            if (token != null) {
                contextInterestRegistryToken = new byte[token.length];
                System.arraycopy(token, 0, contextInterestRegistryToken, 0, token.length);
            } else {
                contextInterestRegistryToken = null;
            }
        }
    }
}
