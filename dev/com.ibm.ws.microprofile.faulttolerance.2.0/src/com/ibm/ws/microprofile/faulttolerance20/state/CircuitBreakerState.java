/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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

/**
 * Implements the state and logic for a Fault Tolerance Circuit Breaker
 * <p>
 * Scope: one method for the lifetime of the application
 * <p>
 * Example usage:
 *
 * <pre>
 * <code>
 * if (circuitBreaker.requestPermissionToExecute()) {
 *   MethodResult<?> result;
 *   try {
 *     result = MethodResult.success(codeToRun());
 *   } catch (Exception e) {
 *     result = MethodResult.failure(e);
 *   } finally {
 *     circuitBreaker.recordResult(result);
 *   }
 * } else {
 *   throw new CircuitBreakerOpenException();
 * }
 * </code>
 * </pre>
 */
public interface CircuitBreakerState {

    /**
     * Request permission from the circuit breaker to run an execution
     * <p>
     * This may result in the circuit breaker changing its state.
     * <p>
     * If this method returns true, an execution must be attempted and the result recorded with a call to {@link #recordResult(MethodResult)}.
     *
     * @return {@code true} if the circuit breaker permits an execution at this time, {@code false} otherwise.
     */
    public boolean requestPermissionToExecute();

    /**
     * Record the result of an execution on the circuit breaker
     *
     * @param result the result the execution
     */
    public void recordResult(MethodResult<?> result);

}
