/*******************************************************************************
 * Copyright (c) 1997, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.internal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import javax.resource.ResourceException;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.HintsContext;
import javax.resource.spi.work.TransactionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkCompletedException;
import javax.resource.spi.work.WorkContext;
import javax.resource.spi.work.WorkContextErrorCodes;
import javax.resource.spi.work.WorkContextLifecycleListener;
import javax.resource.spi.work.WorkContextProvider;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkManager;
import javax.resource.spi.work.WorkRejectedException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jca.security.JCASecurityContext;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;
import com.ibm.wsspi.threadcontext.jca.JCAContextProvider;

/**
 * A wrapper for Work.
 * This wrapper takes care of execution context handling, work listener notification
 * and startTimeout.
 */
public class WorkProxy implements Callable<Void>, Runnable {
    /**
     * Constructor for WorkProxy.
     *
     * @pre theWork != null
     *
     * @param theWork the actual Work object that is passed in by the RA.
     * @param theStartTimeout abort the request after this amount of time has passed
     *            and the actual Work has not yet started.
     *            A start timeout occurs when the thread pool is to busy
     *            to allocate a thread to start the work on.
     * @param theContext controls what context information is used by the thread to
     *            establish its context
     * @param theListener send state changes to the Work to this object.
     * @param bootstrapContext the bootstrap context.
     * @param runningWork list of work that is running.
     * @param applyDefaultContext determines whether or not to apply default context for thread context types that aren't otherwise specified or configured.
     * @throws ResourceException if unable to associate with the resource adapter.
     */
    public WorkProxy(
                     Work theWork,
                     long theStartTimeout,
                     ExecutionContext theContext,
                     WorkListener theListener,
                     BootstrapContextImpl bootstrapContext,
                     ConcurrentLinkedQueue<Work> runningWork,
                     boolean applyDefaultContext) throws Exception {

        work = theWork;
        startTimeout = theStartTimeout;
        executionContext = theContext;
        lsnr = theListener;
        this.bootstrapContext = bootstrapContext;
        this.runningWork = runningWork;
        // JCA 11.4
        // If the resource adapter returns a null or an empty List when the WorkManager
        // makes a call to the getWorkContexts method, the WorkManager must treat it as if no
        // additional execution contexts are associated with that Work instance and must
        // continue with the Work processing.
        boolean isWorkContextProvider = work instanceof WorkContextProvider;
        workContexts = isWorkContextProvider ? ((WorkContextProvider) work).getWorkContexts() : null;

        // RA must not submit a Work that implements WorkContextProvider along
        // with a valid, not null, ExecutionContext -- 11.5 p.3
        if (isWorkContextProvider && executionContext != null) {
            WorkRejectedException wrex = new WorkRejectedException(Utils.getMessage("J2CA8623.execution.context.conflict",
                                                                                    bootstrapContext.resourceAdapterID), WorkException.UNDEFINED);
            if (lsnr != null)
                lsnr.workRejected(new WorkEvent(work, WorkEvent.WORK_REJECTED, work, wrex));
            throw wrex;
        }

        String workName = null;
        if (workContexts != null)
            for (WorkContext workContext : workContexts)
                if (workContext instanceof HintsContext) {
                    // JCA 11.6
                    // The WorkManager must reject the establishment of the HintsContext if the values
                    // provided for the hints are not valid.
                    Map<String, Serializable> hints = ((HintsContext) workContext).getHints();

                    // JCA 11.6.1.1 Work Name Hint
                    // The value for the hint must be a valid java.lang.String.
                    Serializable value = hints.get(HintsContext.NAME_HINT);
                    if (value == null || value instanceof String)
                        workName = (String) value;
                    else
                        hintsContextSetupFailure = new ClassCastException(Tr.formatMessage(TC, "J2CA8687.hint.datatype.invalid", "HintsContext.NAME_HINT", String.class.getName(),
                                                                                           bootstrapContext.resourceAdapterID, value, value.getClass().getName()));

                    // JCA 11.6.1.2 Long-running Work instance Hint
                    // The value of the hint must be a valid boolean value (true or false).
                    value = hints.get(HintsContext.LONGRUNNING_HINT);
                    if (value instanceof Boolean) {
                        String key = bootstrapContext.eeVersion < 9 ? "javax.enterprise.concurrent.LONGRUNNING_HINT" : "jakarta.enterprise.concurrent.LONGRUNNING_HINT";
                        executionProperties.put(key, value.toString());
                    } else if (value != null)
                        hintsContextSetupFailure = new ClassCastException(Tr.formatMessage(TC, "J2CA8687.hint.datatype.invalid", "HintsContext.LONGRUNNING_HINT",
                                                                                           Boolean.class.getName(),
                                                                                           bootstrapContext.resourceAdapterID, value, value.getClass().getName()));
                }
        String identityNameKey = bootstrapContext.eeVersion < 9 ? "javax.enterprise.concurrent.IDENTITY_NAME" : "jakarta.enterprise.concurrent.IDENTITY_NAME";
        executionProperties.put(identityNameKey, workName == null ? work == null ? null : work.getClass().getName() : workName);
        executionProperties.put(WSContextService.TASK_OWNER, bootstrapContext.resourceAdapterID);
        if (bootstrapContext.propagateThreadContext)
            if (applyDefaultContext)
                executionProperties.put(WSContextService.DEFAULT_CONTEXT, WSContextService.UNCONFIGURED_CONTEXT_TYPES);
            else
                executionProperties.put(WSContextService.SKIP_CONTEXT_PROVIDERS, "com.ibm.ws.transaction.context.provider");
        else
            executionProperties.put(WSContextService.DEFAULT_CONTEXT, WSContextService.ALL_CONTEXT_TYPES);

        // If task needs to be run with specified work context(s)
        threadContextDescriptor = bootstrapContext.contextSvc.captureThreadContext(executionProperties);

        if (work instanceof ResourceAdapterAssociation && bootstrapContext.resourceAdapter != null)
            ((ResourceAdapterAssociation) work).setResourceAdapter(bootstrapContext.resourceAdapter);

        // Send notice to listener that work has been accepted.
        // This notice should occur as soon as the work has been checked out
        // as acceptable and before the call to actually start the work occurs.
        //
        // Work is the correct thing to pass in for the first parameter.   This is because
        // EventObject provides the getSource method which returns an Object and so WorkEvent
        // must provide an Object to EventObject.

        if (lsnr != null) {
            WorkEvent event = new WorkEvent(work, WorkEvent.WORK_ACCEPTED, work, null);
            lsnr.workAccepted(event);
        }

        // Capturing the beginning time for the work should always be the last thing that
        // WorkProxy does so that the time between the creation of the proxy and the
        // call to run() is as small as possible.   Otherwise we may get more work
        // rejections due to small start time out values than we should
        timeAccepted = System.currentTimeMillis();
    }

    /**
     * Append work inflow context to the captured thread context.
     *
     * @return merged list of captured thread context and work inflow context.
     * @throws WorkCompletedException if work context is unacceptable.
     */
    private ThreadContextDescriptor appendInflowContext() throws WorkCompletedException {
        ThreadContextDescriptor merged = threadContextDescriptor.clone();
        Set<String> inflowContext = new HashSet<String>();

        // WorkContext
        if (workContexts != null)
            for (WorkContext workContext : workContexts)
                if (workContext instanceof HintsContext) {
                    if (hintsContextSetupFailure != null)
                        throw contextSetupFailure(workContext, WorkContextErrorCodes.CONTEXT_SETUP_FAILED, hintsContextSetupFailure);
                } else {
                    JCAContextProvider provider = bootstrapContext.getJCAContextProvider(workContext.getClass());
                    if (provider == null)
                        throw contextSetupFailure(workContext, WorkContextErrorCodes.UNSUPPORTED_CONTEXT_TYPE, null);
                    else {
                        ThreadContext context;
                        try {
                            context = provider.getInflowContext(workContext, executionProperties);
                        } catch (Throwable x) {
                            throw contextSetupFailure(workContext, WorkContextErrorCodes.CONTEXT_SETUP_FAILED, x);
                        }
                        String workContextProviderName = bootstrapContext.getJCAContextProviderName(workContext.getClass());
                        if (!inflowContext.add(workContextProviderName))
                            throw contextSetupFailure(workContext, WorkContextErrorCodes.DUPLICATE_CONTEXTS, null);
                        merged.set(workContextProviderName, context);
                    }
                }

        // ExecutionContext
        if (executionContext != null)
            if (inflowContext.isEmpty() && (executionContext.getXid() != null || executionContext.getTransactionTimeout() != WorkManager.UNKNOWN)) {
                JCAContextProvider provider = bootstrapContext.getJCAContextProvider(TransactionContext.class);
                if (provider == null)
                    throw contextSetupFailure(executionContext, WorkContextErrorCodes.UNSUPPORTED_CONTEXT_TYPE, null);
                ThreadContext context;
                try {
                    context = provider.getInflowContext(executionContext, executionProperties);
                } catch (Throwable x) {
                    throw contextSetupFailure(executionContext, WorkContextErrorCodes.CONTEXT_SETUP_FAILED, x);
                }
                String executionContextHandlerName = bootstrapContext.getJCAContextProviderName(TransactionContext.class);
                merged.set(executionContextHandlerName, context);
            } // else - already checked for this error condition earlier

        return merged;
    }

    /**
     * @pre work != null
     * @see java.util.concurrent.Callable#call()
     */
    @FFDCIgnore(Throwable.class)
    @Override
    public Void call() throws WorkException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && TC.isEntryEnabled())
            Tr.entry(this, TC, "call", work, lsnr);

        // 1. Capture the current time before run does anything else
        long currentTime = System.currentTimeMillis();
        long startupDuration = currentTime - timeAccepted;

        // 2. Check to see if time is up before actually doing the work
        // There can be a delay between requesting that the ThreadPool execute the work
        // and the work actually beginning.  If this delay exceeds the startTimeout value,
        // then the work must be rejected with a WorkRejectedException
        if (startTimeout != WorkManager.INDEFINITE && startupDuration > startTimeout && startTimeout >= 0)
            if (startupDuration < fudgeFactor)
                startupDuration = WorkManager.UNKNOWN;
            else {
                WorkRejectedException wrex = new WorkRejectedException(Utils.getMessage("J2CA8600.work.start.timeout", work, bootstrapContext.resourceAdapterID,
                                                                                        startTimeout), WorkException.START_TIMED_OUT);
                if (lsnr != null) {
                    WorkEvent event = new WorkEvent(work, WorkEvent.WORK_REJECTED, work, wrex, startupDuration);
                    lsnr.workRejected(event);
                }

                startupDurationQueue.add(wrex);
                if (trace && TC.isEntryEnabled())
                    Tr.exit(this, TC, "call", wrex);
                throw wrex;
            }

        startupDurationQueue.add(startupDuration); // Signal source source thread that work started

        // 3. Send notice to listener that work has been started
        if (lsnr != null) {
            WorkEvent event = new WorkEvent(work, WorkEvent.WORK_STARTED, work, null, startupDuration);
            lsnr.workStarted(event);
        }

        // 4. Apply context
        // 5. Call run on the actual Work and then dissociate the handlers when done,
        //    whether an exception occurs or not on the call to run()
        WorkCompletedException wcex = null;
        try {
            ThreadContextDescriptor mergedThreadContextDescriptor = work instanceof WorkContextProvider
                                                                    || executionContext != null ? appendInflowContext() : threadContextDescriptor;

            Runnable contextualWork = bootstrapContext.contextSvc.createContextualProxy(mergedThreadContextDescriptor, this, Runnable.class);
            runningWork.add(work);

            if (trace && TC.isDebugEnabled())
                Tr.debug(this, TC, "Actual call to work.run");

            contextualWork.run();
        } catch (Throwable ex) {
            // Catch exception from the run method and wrap it in a
            // WorkCompletedException with the message sent to UNDEFINED
            // since this is from the application and so we don't know the
            // real cause.   Since this exception is passed in the
            // WorkCompletedException, the RA can try to get the real exception
            // if it needs to.
            ex = ex instanceof RejectedExecutionException ? ex.getCause() : ex;
            String errorCode = ex instanceof ResourceException ? ((ResourceException) ex).getErrorCode() : WorkException.UNDEFINED;
            wcex = (WorkCompletedException) (ex instanceof WorkCompletedException ? ex : new WorkCompletedException(ex.getMessage(), errorCode).initCause(ex));
            if (!runningWork.contains(work)) {
                // Didn't make it far enough to run the work, so we can log the error knowing that it's unexpected.
                FFDCFilter.processException(ex, getClass().getName(), "326", this);
                Tr.error(TC, "J2CA8688.work.setup.failed", ex.getMessage() == null ? ex.getClass().getName() : ex.getMessage());
            }
            if (trace && TC.isEntryEnabled())
                Tr.exit(this, TC, "call", Utils.toString(ex));
            throw wcex;
        } finally {
            runningWork.remove(work);
            if (lsnr != null) {
                WorkEvent event = new WorkEvent(work, WorkEvent.WORK_COMPLETED, work, wcex, startupDuration);
                lsnr.workCompleted(event);
            }
        }

        if (trace && TC.isEntryEnabled())
            Tr.exit(this, TC, "call");
        return null;
    }

    /**
     * Handle a work context setup failure.
     *
     * @param workContext the work context.
     * @param errorCode error code from javax.resource.spi.work.WorkContextErrorCodes
     * @param cause Throwable to chain as the cause. Can be null.
     * @return WorkCompletedException to raise the the invoker.
     */
    private WorkCompletedException contextSetupFailure(Object context, String errorCode, Throwable cause) {

        if (context instanceof WorkContextLifecycleListener)
            ((WorkContextLifecycleListener) context).contextSetupFailed(errorCode);

        String message = null;

        if (WorkContextErrorCodes.DUPLICATE_CONTEXTS.equals(errorCode))
            message = Utils.getMessage("J2CA8624.work.context.duplicate", bootstrapContext.resourceAdapterID, context.getClass().getName());
        else if (WorkContextErrorCodes.UNSUPPORTED_CONTEXT_TYPE.equals(errorCode))
            message = Utils.getMessage("J2CA8625.work.context.unavailable", bootstrapContext.resourceAdapterID, context.getClass().getName());
        else if (cause != null)
            message = cause.getMessage();

        WorkCompletedException wcex = new WorkCompletedException(message, errorCode);
        if (cause != null)
            wcex.initCause(cause);
        return wcex;
    }

    /**
     * When this is invoked, the work is running with context on the thread.
     *
     * @see java.lang.Runnable.run()
     */
    @Override
    public void run() {
        JCASecurityContext ctx = bootstrapContext.getJCASecurityContext();
        if (ctx != null)
            ctx.runInInboundSecurityContext(work);
        else
            work.run();
    }

    /**
     * Waits for the work to start.
     *
     * @return the duration that it took for the work to start. Null if we timed out waiting.
     * @throws InterruptedException if interrupted while waiting.
     */
    Long waitForStart() throws InterruptedException, WorkRejectedException {
        long timeout = startTimeout == WorkManager.UNKNOWN ? WorkManager.INDEFINITE : startTimeout < fudgeFactor ? fudgeFactor : startTimeout;
        Object o = startupDurationQueue.poll(timeout, TimeUnit.MILLISECONDS);
        if (o instanceof WorkRejectedException)
            throw (WorkRejectedException) o;
        else
            return (Long) o;
    }

    private static final TraceComponent TC = Tr.register(WorkProxy.class);

    /**
     * The bootstrap context.
     */
    private final BootstrapContextImpl bootstrapContext;

    /**
     * The execution context, if any is specified.
     */
    private final ExecutionContext executionContext;

    /**
     * The execution properties.
     */
    private final Map<String, String> executionProperties = new HashMap<String, String>();

    /**
     * Failure that occurred when processing the HintsContext, which per spec cannot be reported until the accepted and started work events are sent.
     */
    private Throwable hintsContextSetupFailure;

    /**
     * List of work that is running. This is useful so that we know what to release.
     */
    private final ConcurrentLinkedQueue<Work> runningWork;

    /**
     * The work that is wrapped by <code>WorkProxy</code>.
     * This method is protected because the WorkScheduler may need to
     * obtain the work.
     */
    protected Work work;

    /**
     * The amount of time to wait before canceling the work if it hasn't started.
     */
    private final long startTimeout;

    /**
     * Single-element queue of startup duration. This is used to wait for the work to start.
     * When the work starts, the startup time is placed on the queue.
     * If start is rejected, a WorkRejectedException is placed on the queue.
     */
    private final BlockingQueue<Object> startupDurationQueue = new LinkedBlockingQueue<Object>();

    /**
     * Captured thread context as of when the work was submitted.
     */
    private final ThreadContextDescriptor threadContextDescriptor;

    /**
     * Time work request accepted.
     */
    private final long timeAccepted;

    /**
     * Work inflow contexts
     */
    private final List<WorkContext> workContexts;

    /**
     * Amount of time system seems to occasionally need between creation
     * of WorkProxy and call to execute. Allow this much time additionally
     * for an IMMEDIATE timeout request.
     *
     * 100 was chosen as it reduced the number of immediate exceptions
     * under load from 39 percent of the work requests to .6 percent of
     * the work requests. This analysis was done on Windows.
     */
    // TODO I am going to leave the code in for
    // now just in case someone else runs into this.
    private static final long fudgeFactor = 100;

    /**
     * Work listener for the work.
     * This method is protected because the WorkScheduler may need to
     * obtain the listener.
     */
    protected WorkListener lsnr;
}