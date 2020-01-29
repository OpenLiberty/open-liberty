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
package com.ibm.ws.microprofile.faulttolerance20.state.impl;

import java.util.concurrent.atomic.AtomicReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder;
import com.ibm.ws.microprofile.faulttolerance20.impl.MethodResult;
import com.ibm.ws.microprofile.faulttolerance20.state.CircuitBreakerState;

public class CircuitBreakerStateImpl implements CircuitBreakerState {

    private final static TraceComponent tc = Tr.register(CircuitBreakerStateImpl.class);

    private final CircuitBreakerPolicy policy;
    private final MetricRecorder metricRecorder;

    /**
     * Current CB state
     */
    private final AtomicReference<State> state;

    /**
     * Cached value of policy.getDelay().toNanos()
     */
    private final long policyDelayNanos;

    /**
     * Rolling window tracking results in closed state
     * <p>
     * Access must be synchronized
     */
    private final CircuitBreakerRollingWindow rollingWindow;

    /**
     * Currently running executions in half-open state
     * <p>
     * Access must be synchronized
     */
    private int halfOpenRunningExecutions;

    /**
     * Successful executions in half-open state
     * <p>
     * Access must be synchronized
     */
    private int halfOpenSuccessfulExecutions;

    /**
     * Value of System.nanoTime() when the last execution was permitted in half-open state
     */
    private long halfOpenLastExecutionStarted;

    /**
     * Value of System.nanoTime() when the circuit breaker last transitioned to open state
     * <p>
     * Access must be synchronized
     */
    private long openStateStartTime;

    public CircuitBreakerStateImpl(CircuitBreakerPolicy policy, MetricRecorder metricRecorder) {
        this.policy = policy;
        this.metricRecorder = metricRecorder;
        this.policyDelayNanos = policy.getDelay().toNanos();

        state = new AtomicReference<>(State.CLOSED);
        rollingWindow = new CircuitBreakerRollingWindow(policy.getRequestVolumeThreshold(), policy.getFailureRatio());
        halfOpenRunningExecutions = 0;
        halfOpenSuccessfulExecutions = 0;
        halfOpenLastExecutionStarted = 0;
        openStateStartTime = 0;
    }

    @Override
    public boolean requestPermissionToExecute() {
        boolean result;
        if (state.get() == State.CLOSED) {
            // If breaker is closed then the result is simple
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Allowing execution in closed state");
            }
            result = true;
        } else {
            // Other cases are more complex and require synchronization
            result = synchronizedRequestPermissionToExecute();
        }

        if (result == false) {
            metricRecorder.incrementCircuitBreakerCallsCircuitOpenCount();
        }

        return result;
    }

    @Override
    public void recordResult(MethodResult<?> result) {
        CircuitBreakerResult cbResult = getCircuitBreakerResult(result);
        if (state.get() == State.OPEN) {
            // Nothing to do
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Recording result {0} in open state", cbResult);
            }
        } else {
            // Other cases are more complex and require synchronization
            synchronizedRecordResult(cbResult);
        }

        if (cbResult == CircuitBreakerResult.SUCCESS) {
            metricRecorder.incrementCircuitBreakerCallsSuccessCount();
        } else {
            metricRecorder.incrementCircuitBreakerCallsFailureCount();
        }
    }

    /**
     * Implements the logic for requestPermissionToExecute for the cases where synchronization is required
     *
     * @return whether execution is permitted
     */
    private boolean synchronizedRequestPermissionToExecute() {
        boolean result = false;
        synchronized (this) {
            switch (state.get()) {
                case CLOSED:
                    result = true;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Allowing execution in closed state");
                    }
                    break;
                case HALF_OPEN:
                    // Limit the number of concurrent executions when we're half-open
                    if (halfOpenRunningExecutions < policy.getSuccessThreshold()) {
                        halfOpenRunningExecutions++;
                        halfOpenLastExecutionStarted = System.nanoTime();
                        result = true;
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Allowing execution in half-open state. Now running ({0}/{1})", halfOpenRunningExecutions, policy.getSuccessThreshold());
                        }
                    } else {
                        // If the user runs an operation which never returns (possible using CompletionStage), we might end up stuck in half-open state
                        // denying all executions forever. For this reason, if we stay in half-open state for longer than the open state delay time,
                        // allow another execution
                        if (System.nanoTime() - halfOpenLastExecutionStarted > policyDelayNanos) {
                            halfOpenLastExecutionStarted = System.nanoTime();
                            result = true;
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Allowing execution in half-open state because enough time has passed without a trial executions completing");
                            }
                        } else {
                            result = false;
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Denying execution in half-open state, trial execution limit reached");
                            }
                        }
                    }
                    break;
                case OPEN:
                    if (System.nanoTime() - openStateStartTime > policyDelayNanos) {
                        // We've been in open state for long enough, transition to half-open
                        stateHalfOpen();
                        halfOpenRunningExecutions++;
                        halfOpenLastExecutionStarted = System.nanoTime();
                        result = true;
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Allowing execution because we just changed to half-open state");
                        }
                    } else {
                        result = false;
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Denying execution in open state");
                        }
                    }
                    break;
            }
        }
        return result;
    }

    /**
     * Implements the logic for recordResult for the cases where synchronization is required
     */
    private void synchronizedRecordResult(CircuitBreakerResult result) {
        synchronized (this) {
            switch (state.get()) {
                case CLOSED:
                    rollingWindow.record(result);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Recording result {0} in closed state: {1}", result, rollingWindow);
                    }
                    if (rollingWindow.isOverThreshold()) {
                        stateOpen();
                    }
                    break;
                case HALF_OPEN:
                    if (result == CircuitBreakerResult.FAILURE) {
                        stateOpen();
                    } else {
                        halfOpenRunningExecutions--;
                        halfOpenSuccessfulExecutions++;
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Recording result {0} in half-open state. Running executions: {1}, Current results: ({2}/{3})",
                                     result, halfOpenRunningExecutions, halfOpenSuccessfulExecutions, policy.getSuccessThreshold());
                        }
                        if (halfOpenSuccessfulExecutions >= policy.getSuccessThreshold()) {
                            stateClosed();
                        }
                    }
                    break;
                case OPEN:
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Recording result {0} in open state", result);
                    }
                    // Nothing else to do
                    break;
            }
        }
    }

    /**
     * Transition to closed state
     */
    @Trivial
    private void stateClosed() {
        rollingWindow.clear();
        state.set(State.CLOSED);
        metricRecorder.reportCircuitClosed();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Transitioned to Closed state");
        }
    }

    /**
     * Transition to half open state
     */
    @Trivial
    private void stateHalfOpen() {
        halfOpenRunningExecutions = 0;
        halfOpenSuccessfulExecutions = 0;
        state.set(State.HALF_OPEN);
        metricRecorder.reportCircuitHalfOpen();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Transitioned to Half Open state");
        }
    }

    /**
     * Transition to open state
     */
    @Trivial
    private void stateOpen() {
        openStateStartTime = System.nanoTime();
        state.set(State.OPEN);
        metricRecorder.reportCircuitOpen();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Transitioned to Open state");
        }
    }

    @Trivial
    private CircuitBreakerResult getCircuitBreakerResult(MethodResult<?> result) {
        CircuitBreakerResult cbResult;

        if (!result.isFailure()) {
            cbResult = CircuitBreakerResult.SUCCESS;
        } else if (isSkipOn(result.getFailure())) {
            cbResult = CircuitBreakerResult.SUCCESS;
        } else if (isFailOn(result.getFailure())) {
            cbResult = CircuitBreakerResult.FAILURE;
        } else {
            cbResult = CircuitBreakerResult.SUCCESS;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Circuit breaker considers {0} to be {1}", result, cbResult);
        }

        return cbResult;
    }

    private boolean isSkipOn(Throwable methodException) {
        for (Class<?> skipExClazz : policy.getSkipOn()) {
            if (skipExClazz.isInstance(methodException)) {
                return true;
            }
        }
        return false;
    }

    private boolean isFailOn(Throwable methodException) {
        for (Class<?> failExClazz : policy.getFailOn()) {
            if (failExClazz.isInstance(methodException)) {
                return true;
            }
        }
        return false;
    }

    public enum CircuitBreakerResult {
        SUCCESS,
        FAILURE
    }

    private enum State {
        OPEN,
        HALF_OPEN,
        CLOSED
    }

}