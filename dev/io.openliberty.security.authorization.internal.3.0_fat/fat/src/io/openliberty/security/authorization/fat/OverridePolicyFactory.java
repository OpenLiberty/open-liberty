/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.authorization.fat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.security.jacc.Policy;
import jakarta.security.jacc.PolicyFactory;

public class OverridePolicyFactory extends PolicyFactory {

    private final Map<String, Policy> policyMap = new ConcurrentHashMap<>();

    private volatile Policy globalPolicy = null;

    private final PolicyFactory delegate;

    public OverridePolicyFactory(PolicyFactory delegate) {
        this.delegate = delegate;
        globalPolicy = new OverridePolicy(delegate, null);
    }

    @Override
    public Policy getPolicy(String contextId) {
        if (contextId == null) {
            return globalPolicy;
        }

        Policy policy = policyMap.get(contextId);
        if (policy == null) {
            // get policy and set it in the map
            policy = new OverridePolicy(delegate, contextId);
            policyMap.put(contextId, policy);
        }

        return policy;
    }

    @Override
    public void setPolicy(String contextId, Policy policy) {
        if (contextId != null) {
            policyMap.put(contextId, policy);
        } else {
            globalPolicy = policy;
        }
    }
}
