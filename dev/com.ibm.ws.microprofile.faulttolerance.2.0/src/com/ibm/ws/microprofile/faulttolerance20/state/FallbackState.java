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
package com.ibm.ws.microprofile.faulttolerance20.state;

import com.ibm.ws.microprofile.faulttolerance20.impl.MethodResult;
import com.ibm.ws.microprofile.faulttolerance20.impl.SyncExecutionContextImpl;

/**
 * Implements the state and logic for a Fault Tolerance Fallback
 * <p>
 * Scope:
 * <p>
 * Example usage:
 *
 * <pre>
 * <code>
 * if (fallback.shouldApplyFallback(result)) {
 *      result = fallback.runFallback(result, executionContext);
 * }
 * </code>
 * </pre>
 */
public interface FallbackState {

    /**
     * Decides whether or not to apply Fallback
     * <p>
     * Considers the list of exceptions in applyOn() and skipOn()
     * <p>
     * Gives priority to the exceptions in skipOn
     *
     * @param result the result of the execution of the method Fallback was applied to
     * @return a decision on whether Fallback should be applied or not
     */
    public boolean shouldApplyFallback(MethodResult<?> result);

    /**
     * Executes the fallback function based on the executionContext
     *
     * @param <R>              the return type of the method
     * @param result           the result of the execution of the method Fallback was applied to
     * @param executionContext context for the execution of the method Fallback was applied to
     * @return an updated MethodResult with the new result obtained by running fallback
     */
    public <R> MethodResult<R> runFallback(MethodResult<R> result, SyncExecutionContextImpl executionContext);

}
