/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.authorization.jacc.provider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.security.jacc.Policy;
import jakarta.security.jacc.PolicyFactory;

public class PolicyFactoryImpl extends PolicyFactory {

    private final Map<String, Policy> policyMap = new ConcurrentHashMap<>();

    @Override
    public Policy getPolicy(String contextId) {
        if (contextId == null) {
            return null;
        }

        Policy policy = policyMap.get(contextId);
        if (policy == null) {
            // get policy and set it in the map
            policy = new JaccPolicyProxy(contextId);
            policyMap.put(contextId, policy);
        }

        return policy;
    }

    @Override
    public void setPolicy(String contextId, Policy policy) {
        if (contextId != null) {
            policyMap.put(contextId, policy);
        }
    }

}
