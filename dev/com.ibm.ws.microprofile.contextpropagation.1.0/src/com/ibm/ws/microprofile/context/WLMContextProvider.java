/*******************************************************************************
 * Copyright (c) 2018,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.context;

import java.util.ArrayList;
import java.util.HashMap;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.contextpropagation.ContextOp;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Partial implementation of MicroProfile thread context provider,
 * backed by Liberty's z/OS WLM context.
 */
@Trivial
@SuppressWarnings("deprecation")
public class WLMContextProvider extends ContainerContextProvider {
    public static final String CLASSIFICATION = "Classification";

    public final AtomicServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> wlmContextProviderRef = new AtomicServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider>("WLMContextProvider");

    @Override
    public void addContextSnapshot(ContextOp op, ArrayList<com.ibm.wsspi.threadcontext.ThreadContext> contextSnapshots) {
        com.ibm.wsspi.threadcontext.ThreadContext snapshot;
        com.ibm.wsspi.threadcontext.ThreadContextProvider wlmProvider = wlmContextProviderRef.getService();

        if (wlmProvider == null) {
            snapshot = new DeferredClearedContext(wlmContextProviderRef);
        } else if (op == ContextOp.PROPAGATED) {
            /*
             * When the wlm policy is PROPAGATE, either the existing enclave is captured or no enclave will be created/added.
             * The defaultTransactionClass and daemonTransactionClass configuration properties are only used when a new
             * enclave is created, and this will never happen with a policy of PROPAGATE. This means for our purposes
             * the values of defaultTransactionClass and daemonTransactionClass don't matter.
             *
             * Also, the only execProp that is used by ThreadContextProvider.captureThreadContext() is
             * javax.enterprise.concurrent.LONGRUNNING_HINT. This execProp is used to determine which transaction class
             * to use (default vs daemon) when creating the enclave. Again, since we aren't ever creating an enclave
             * we don't need to concern ourselves with ever setting LONGRUNNING_HINT.
             */
            snapshot = wlmProvider.captureThreadContext(EMPTY_MAP, new HashMap<String, String>() {
                {
                    put("wlm", "PROPAGATE");
                    put("defaultTransactionClass", "ASYNCBN");
                    put("daemonTransactionClass", "ASYNCDMN");
                }
            });
        } else {
            snapshot = wlmProvider.createDefaultThreadContext(EMPTY_MAP);
        }
        contextSnapshots.add(snapshot);
    }

    @Override
    public final String getThreadContextType() {
        return CLASSIFICATION;
    }
}