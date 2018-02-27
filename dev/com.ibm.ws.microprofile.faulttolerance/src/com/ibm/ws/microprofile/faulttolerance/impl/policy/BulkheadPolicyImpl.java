/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.impl.policy;

import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;

/**
 *
 */
public class BulkheadPolicyImpl implements BulkheadPolicy {

    private static final TraceComponent tc = Tr.register(BulkheadPolicyImpl.class);

    private int maxThreads;
    private int queueSize;

    /**
     *
     */
    public BulkheadPolicyImpl() {
        try {
            maxThreads = (int) Bulkhead.class.getMethod("value").getDefaultValue();
            queueSize = (int) Bulkhead.class.getMethod("waitingTaskQueue").getDefaultValue();
        } catch (NoSuchMethodException | SecurityException e) {
            throw new FaultToleranceException(Tr.formatMessage(tc, "internal.error.CWMFT4998E", e), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxThreads() {
        return maxThreads;
    }

    /** {@inheritDoc} */
    @Override
    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    /** {@inheritDoc} */
    @Override
    public int getQueueSize() {
        return queueSize;
    }

    /** {@inheritDoc} */
    @Override
    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

}
