/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.context.internal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.transaction.Transaction;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.tx.UOWEventEmitter;
import com.ibm.wsspi.tx.UOWEventListener;

/**
 * TODO Remove the following temporary code once the transaction manager provides a proper mechanism to prevent a transaction on multiple threads at once.
 * This data structure will accumulate transaction references if the application leaves transactions unresolved.
 */
@Trivial
public class SuspendCount extends ConcurrentHashMap<Transaction, AtomicInteger> implements UOWEventListener {
    private static final TraceComponent tc = Tr.register(SuspendCount.class);
    private static final long serialVersionUID = 1L;

    /**
     * @see com.ibm.wsspi.tx.UOWEventListener#UOWEvent(com.ibm.wsspi.tx.UOWEventEmitter, int, java.lang.Object)
     */
    @Override
    public void UOWEvent(UOWEventEmitter uow, int event, Object data) {
        if (event == UOWEventListener.SUSPEND) {
            AtomicInteger count = get(uow);
            if (count != null) {
                int c = count.incrementAndGet();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "SUSPEND UOWEvent, count=" + c + " for " + uow);
            }
        } else if (event == UOWEventListener.RESUME) {
            AtomicInteger count = get(uow);
            if (count != null) {
                int c = count.decrementAndGet();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "RESUME UOWEvent, count=" + c + " for " + uow);
            }
        } else if (event == UOWEventListener.POST_END) {
            AtomicInteger count = remove(uow);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "END UOWEvent, count=" + count + " for " + uow);
        }
    }
}
