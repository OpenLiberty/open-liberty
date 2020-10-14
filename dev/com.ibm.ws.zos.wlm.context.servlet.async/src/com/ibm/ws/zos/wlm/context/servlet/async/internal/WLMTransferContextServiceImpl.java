/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.wlm.context.servlet.async.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.zos.wlm.AlreadyClassifiedException;
import com.ibm.ws.zos.wlm.Enclave;
import com.ibm.ws.zos.wlm.EnclaveManager;
import com.ibm.wsspi.webcontainer.servlet.ITransferContextService;
import com.ibm.wsspi.webcontainer.servlet.ITransferContextServiceExt;

/**
 * Implementation of ITransferContextServiceExt for WLM context propagation when using
 * servlet 3.*'s asynchronous support.
 */
@Component(service = { ITransferContextService.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM" })
public class WLMTransferContextServiceImpl implements ITransferContextServiceExt {

    /** Trace component registration. */
    private static final TraceComponent tc = Tr.register(WLMTransferContextServiceImpl.class);

    /** WLM context context propagation key */
    private final String WLM_CONTEXT_SERVLET_ASYNC_KEY = "com.ibm.ws.zos.wlm.context.servlet.async.enclave";

    /** Enclave manager reference. */
    private EnclaveManager enclaveManager;

    /** Map holding joined enclaves for all participating threads. Needed for cleanup during deactivation and sanity checks during restore. */
    private final Map<String, Enclave> enclaveMap = new ConcurrentHashMap<String, Enclave>();

    /** ThreadLocal to keep track of enclaves that were joined as part of work associated to this class. */
    private final ThreadLocal<Enclave> threadLocalJoinedEnclave = new ThreadLocal<Enclave>();

    /**
     * DS method to activate this component.
     *
     * @param properties The associated properties.
     */
    @Activate
    protected void activate(Map<String, Object> properties) {
    }

    /**
     * DS method to deactivate this component.
     */
    @Deactivate
    protected void deactivate() {
        // See if there are any outstanding enclaves. If there are, we will proceed to delete them.
        // Any failures coming out of DELETE/LEAVE should be explainable along the message
        // this service will issue prior to deletion. One of the failures that can be issued is this:
        // CWWKB0152E: Native routine (DeleteWorkUnit) received a failing reason code (1,041) from WLM,
        // where RC=4 REASON=1041 on delete means: "Input enclave had 1 or more SRBs scheduled or
        // running, or 1 or more TCBs joined to the enclave.".
        //
        // NOTES:
        // 1. Deleting the enclave is a best effort approach to prevent leaking enclaves in cases where
        // the servlet or wlm features are removed in error while work is still in progress.
        //
        // 2. There are 3 cases in which enclaves are physically deleted despite having
        // outstanding joins.
        // a. When the thread terminates.
        // b. When WAS disconnects from WLM.
        // c. When a call to delete (IWMEDELE, DeleteWorkUnit) is issued.
        //
        // 3. The case in which an enclave is only logically deleted after a delete call (IWMEDELE, DeleteWorkUnit)
        // is when the IWMEREG is called prior to the delete call.
        try {
            if (!enclaveMap.isEmpty()) {
                for (Map.Entry<String, Enclave> entry : enclaveMap.entrySet()) {
                    String enclaveToken = entry.getKey();
                    Enclave enclave = entry.getValue();
                    try {
                        synchronized (enclave) {
                            enclaveManager.deleteEnclave(enclave, true);
                            Tr.warning(tc, "DEACTIVATE_ENCLAVE_DELETED", enclave.getStringToken());
                        }
                    } catch (Throwable t) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc,
                                     "The support for async servlet context propagation is deactivating while there is active asynchronous work is still in progress. Enclave delete failed. Enclave: "
                                         + enclave,
                                     t.getCause());
                        }
                    }
                    enclaveMap.remove(enclaveToken);
                }
            }
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Enclave map size: " + enclaveMap.size() + ". Enclave map: " + enclaveMap);
            }
        }
    }

    /**
     * Set a reference to the enclave manager.
     *
     * @param em The enclave manager to set.
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setWlmEnclaveManager(EnclaveManager em) {
        enclaveManager = em;
    }

    /**
     * Unset a reference to the enclave manager.
     *
     * @param em The enclave manager to unset.
     */
    protected void unsetWlmEnclaveManager(EnclaveManager em) {
        if (enclaveManager == em) {
            enclaveManager = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void storeState(Map<String, Object> m) {
        // This method is called "once" per request during pushContext on the original (web container) thread.
        Enclave enclave = enclaveManager.getCurrentEnclave();

        // Set the enclave's autoDelete bit to false to prevent the enclave from being deleted if
        // the work submitted under a single request is intermittent. In other words, when a batch of work is
        // started (AsyncContext.start(work)) and completes, but the request is not really done issuing work,
        // the WLM work manager will attempt to delete the enclave when its counts reach zero.
        if (enclave != null) {
            enclave.setAutoDelete(false);

            // Store the enclave to be used by restoreState() and completeState().
            m.put(WLM_CONTEXT_SERVLET_ASYNC_KEY, enclave);
            enclaveMap.put(enclave.getStringToken(), enclave);
        } else {
            // If there is no enclave on entry, something happened around the call to
            // WlmClassification>WlmExecutor.wlmPreInvoke(), which prevented a
            // new enclave to be created/joined.
            // If this is the case, allow this request to run without an enclave, but log
            // an FFDC to leave a trail of what has happened. Note that printing a
            // warning message is not appropriate because there is really
            // nothing that the user could do with the message to fix the issue.
            // Furthermore, it is hard to explain an internal issue in a message.

            // We hit this path if no default classification was defined...its normal...
            // Its a way to say NO enclave please.  So, we removed the FFDC and replaced
            // with debug trace to highlite that you may want to look into this.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "An enclave was not found on the current thread. Unable to save enclave state information for asynchronous processing");
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void preProcessWorkState(Map<String, Object> m) {
        // This method is called prior to contextual work being queued for execution. Note that, in some cases
        // (i.e. servlet-3.1), this method is called when contextual CompleteRunable work is issued as result of
        // the application making a complete call on the async context.
        // This call is used to pre-join the enclave to prevent its deletion in some cases:
        // For example: Work execution sequence: w1, w2, w3, complete, w4, w5.
        // If complete finished execution prior to w4 and w5 starting execution, the enclave will be deleted
        // because the work completed (AsyncContext.complete()) and the enclave is not in use
        // (w1, w2, w3, also completed). In order to prevent join errors for w4 and w5, we need to know ahead
        // of time about all the work that has been started before the HTTP request exits.
        Enclave enclave = (Enclave) m.get(WLM_CONTEXT_SERVLET_ASYNC_KEY);

        // If there is no enclave, an FFDC should already be available.
        if (enclave != null) {
            enclaveManager.preJoinEnclave(enclave);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void restoreState(Map<String, Object> m) {
        // This method is called to set the context stored during popContext on the current thread executing the work.
        Enclave enclave = (Enclave) m.get(WLM_CONTEXT_SERVLET_ASYNC_KEY);

        // If there was no enclave set during the storeState call, this request is
        // running without an enclave. An FFDC has already been logged.
        if (enclave != null) {
            try {
                // There is an enclave, join and save it.
                enclaveManager.joinEnclave(enclave);
                threadLocalJoinedEnclave.set(enclave);
            } catch (AlreadyClassifiedException ace) {
                // If we are here, someone else has illegally joined our enclave or we did not
                // leave/delete a previous enclave.
                // At this point, the enclave manager has already attempted to delete it. However, since
                // we pre-joined the enclave, the enclave manager will not delete it because
                // it is "in use". Having said that, print an FFDC to record this error
                // and move on. The logs should contain messages from WLM indicating the error:
                // CWWKB0152E: Native routine (JoinWorkUnit) received a failing reason code (2,106) from WLM.
                // CWWKB0152E: Native routine (DeleteWorkUnit) received a failing reason code (2,106) from WLM.
                Exception e = new IllegalStateException("The request to join enclave with the token of " + enclave.getStringToken() + " failed.");
                FFDCFilter.processException(e, getClass().getName(), "216", this, new Object[] { enclave, m, enclaveManager, enclaveMap });
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void resetState() {
        // This method is called while on the async thread and after the work is done.
        Enclave enclave = enclaveManager.getCurrentEnclave();

        // If the current enclave is null, it is because we never joined it. We never joined it because
        // something happened during join or there was no enclave for this request to begin with.
        // In either case, there should already be an FFDC and an error message in the logs.
        if (enclave != null) {
            // If join failed because there was a foreign enclave, make sure that we do
            // not leave that enclave.
            if (threadLocalJoinedEnclave.get() == enclave) {
                enclaveManager.leaveEnclave(enclave);
            } else {
                Exception e = new IllegalStateException("Enclave " + enclave.getStringToken() + " was not joined. The request to leave the enclave is ignored.");
                FFDCFilter.processException(e, getClass().getName(), "202", this, new Object[] { enclave, enclaveManager, enclaveMap });
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void completeState(Map<String, Object> m) {
        // This method is called after the request was marked completed either by the application
        // or by the web container.
        Enclave enclave = (Enclave) m.get(WLM_CONTEXT_SERVLET_ASYNC_KEY);

        // If there was no enclave set during the storeState call, this request is
        // running without an enclave. An FFDC has already been logged.
        if (enclave != null) {
            // We found an enclave in the context map. In the normal case and if the
            // the join call failed, we will proceed to delete the enclave.
            // Note that some threads might still be finishing up enclave leaves by the time this call
            // completes or maybe joins might be happening later. We really do not know what the application
            // might decide to do. All we know is that processing work after AsyncContext.complete()
            // is not something that is done in normal executions because driving complete() signals that
            // the asynchronous work under the HTTP request dispatch is complete and that the HTTP response
            // should be processed.
            // To handle the failure cases where work is driven after complete has been driven, we will do
            // 2 things. First, if we detect that the enclave is in-use, we will simply mark the enclave for
            // auto delete, so that when the last leave is invoked, the enclave will deleted. Second, if the enclave
            // is not in use, we will assume that the work has completed and will proceed to delete the enclave.

            // Deleting the enclave and not tracking the work after the application or the
            // webcontainer (error/timeout cases) drove complete() is explainable because we were told the work
            // completed. Any work beyond the point where complete is processed is not tracked. This behavior
            // is consistent with the managed executor's wlm context join (propagate = true) support.
            boolean inUse = false;
            synchronized (enclave) {
                inUse = enclave.isInUse();
                if (inUse) {
                    enclave.setAutoDelete(true);

                    // Proactively remove the enclave from our enclaveMap because the request has completed
                    // and more importantly because there are cases where the HTTP postInvoke might be the
                    // last method calling leave(). Since that call is not part of this support, it cannot
                    // tell us to remove the enclave from our maps.
                    enclaveMap.remove(enclave.getStringToken());
                }
            }

            if (!inUse) {
                // If we are here all of the work for the request is very much completed.
                // No join calls are expected. We can delete the enclave.
                enclaveManager.deleteEnclave(enclave, false);
                enclaveMap.remove(enclave.getStringToken());
            }
        }
    }
}
