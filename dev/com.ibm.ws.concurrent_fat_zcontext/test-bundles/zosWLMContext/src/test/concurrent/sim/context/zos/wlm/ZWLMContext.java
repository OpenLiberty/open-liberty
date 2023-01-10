/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.concurrent.sim.context.zos.wlm;

import java.util.concurrent.RejectedExecutionException;

import com.ibm.wsspi.threadcontext.ThreadContext;

/**
 * This a fake thread context that we made up for testing purposes.
 */
public class ZWLMContext implements ThreadContext {
    private static final long serialVersionUID = 1L;

    private String txClass;
    private transient String txClassToRestore;

    ZWLMContext(String txClass) {
        this.txClass = txClass;
    }

    ZWLMContext(String defaultTxClass, String action) {
        if ("New".equals(action))
            txClass = defaultTxClass;
        else {
            txClass = Enclave.getTransactionClass();
            if (txClass == null && "PropagateOrNew".equals(action))
                txClass = defaultTxClass;
        }
    }

    @Override
    public ZWLMContext clone() {
        return new ZWLMContext(txClass);
    }

    /**
     * Establishes context on the current thread.
     */
    @Override
    public void taskStarting() throws RejectedExecutionException {
        txClassToRestore = Enclave.getTransactionClass();
        Enclave.setTransactionClass(txClass);
    }

    /**
     * Restore the thread to its previous state from before the most recently applied context.
     */
    @Override
    public void taskStopping() {
        Enclave.setTransactionClass(txClassToRestore);
    }

    @Override
    public String toString() {
        return "ZWLMContext propagate [" + txClass + "] restore [" + txClassToRestore + "]";
    }
}