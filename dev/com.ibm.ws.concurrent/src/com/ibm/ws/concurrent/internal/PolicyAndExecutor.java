/*******************************************************************************
 * Copyright (c) 2024,2025 IBM Corporation and others.
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
package com.ibm.ws.concurrent.internal;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrency.policy.ConcurrencyPolicy;
import com.ibm.ws.threading.PolicyExecutor;

/**
 * Pair of ConcurrencyPolicy and PolicyExecutor.
 */
@Trivial
class PolicyAndExecutor {
    final PolicyExecutor executor;
    final ConcurrencyPolicy policy;

    PolicyAndExecutor(ConcurrencyPolicy policy, PolicyExecutor executor) {
        this.policy = policy;
        this.executor = executor;
    }

    @Override
    public boolean equals(Object other) {
        PolicyAndExecutor o;
        return other instanceof PolicyAndExecutor && //
               (o = (PolicyAndExecutor) other).policy == policy && //
               o.executor == executor;
    }

    @Override
    public int hashCode() {
        return (executor == null ? 0 : executor.hashCode()) + //
               (policy == null ? 0 : policy.hashCode());
    }

    /**
     * Returns a String of the form:
     * [ConcurrencyPolicyImpl@87654321, PolicyExecutorImpl@12345678]
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(61).append('[');
        if (policy == null)
            b.append("null");
        else
            b.append(policy.getClass().getName()).append('@') //
                            .append(Integer.toHexString(policy.hashCode()));
        b.append(", ");
        if (executor == null)
            b.append("null");
        else
            b.append(executor.getClass().getName()).append('@')//
                            .append(Integer.toHexString(executor.hashCode()));
        b.append("]");
        return b.toString();
    }
}