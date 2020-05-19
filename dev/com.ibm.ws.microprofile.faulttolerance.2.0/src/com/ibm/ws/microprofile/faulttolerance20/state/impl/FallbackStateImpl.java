/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance20.state.impl;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance20.impl.MethodResult;
import com.ibm.ws.microprofile.faulttolerance20.impl.SyncExecutionContextImpl;
import com.ibm.ws.microprofile.faulttolerance20.state.FallbackState;

public class FallbackStateImpl implements FallbackState {

    private final static TraceComponent tc = Tr.register(FallbackStateImpl.class);

    private final FallbackPolicy policy;

    public FallbackStateImpl(FallbackPolicy policy) {
        this.policy = policy;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> MethodResult<R> runFallback(MethodResult<R> result, SyncExecutionContextImpl executionContext) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Execution {0} calling fallback", executionContext.getId());
        }

        executionContext.setFailure(result.getFailure());
        try {
            result = MethodResult.success((R) policy.getFallbackFunction().execute(executionContext));
        } catch (Throwable ex) {
            result = MethodResult.failure(ex);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Execution {0} fallback result: {1}", executionContext.getId(), result);
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldApplyFallback(MethodResult<?> result) {
        boolean apply;

        if (!result.isFailure()) {
            apply = false;
        } else if (isSkipOn(result.getFailure())) {
            apply = false;
        } else if (isApplyOn(result.getFailure())) {
            apply = true;
        } else {
            apply = false;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Fallback considers {0} to be {1}", result, apply);
        }

        return apply;
    }

    private boolean isApplyOn(Throwable methodException) {
        for (Class<?> applyExClazz : policy.getApplyOn()) {
            if (applyExClazz.isInstance(methodException)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSkipOn(Throwable methodException) {
        for (Class<?> skipExClazz : policy.getSkipOn()) {
            if (skipExClazz.isInstance(methodException)) {
                return true;
            }
        }
        return false;
    }

}
