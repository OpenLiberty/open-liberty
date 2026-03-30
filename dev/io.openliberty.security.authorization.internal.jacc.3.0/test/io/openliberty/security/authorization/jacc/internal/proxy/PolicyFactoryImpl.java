/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.authorization.jacc.internal.proxy;

import java.util.HashMap;
import java.util.Map;

import io.openliberty.security.authorization.jacc.internal.proxy.AuthzModuleTracker.ModuleType;
import jakarta.security.jacc.Policy;
import jakarta.security.jacc.PolicyContext;
import jakarta.security.jacc.PolicyFactory;

/**
 *
 */
public class PolicyFactoryImpl extends PolicyFactory {

    Map<String, Policy> policies = new HashMap<>();

    @Override
    public Policy getPolicy() {
        AuthzModuleTracker.addOperation(null, ModuleType.POLICY_FACTORY, "getPolicy");
        String contextID = PolicyContext.getContextID();
        Policy policy = policies.get(contextID);
        if (policy == null) {
            policy = new PolicyImpl(contextID);
            policies.put(contextID, policy);
        }
        return policy;
    }

    @Override
    public Policy getPolicy(String contextID) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY_FACTORY, "getPolicy(String)");
        Policy policy = policies.get(contextID);
        if (policy == null) {
            policy = new PolicyImpl(contextID);
            policies.put(contextID, policy);
        }
        return policy;
    }

    @Override
    public void setPolicy(String contextID, Policy policy) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY_FACTORY, "setPolicy");
        policies.put(contextID, policy);
    }

}
