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
import jakarta.security.jacc.PolicyConfigurationFactory;
import jakarta.security.jacc.PolicyContext;

class PolicyConfigFactoryImpl extends PolicyConfigurationFactory {

    Map<String, PolicyConfigImpl> configs = new HashMap<>();

    @Override
    public PolicyConfigImpl getPolicyConfiguration() {
        AuthzModuleTracker.addOperation(null, ModuleType.POLICY_CONFIG_FACTORY, "getPolicyConfiguration");
        String contextID = PolicyContext.getContextID();
        if (contextID == null) {
            return null;
        }
        return configs.get(contextID);
    }

    @Override
    public PolicyConfigImpl getPolicyConfiguration(String contextID) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String)");
        if (contextID == null) {
            throw new IllegalArgumentException("contextID is required to be non-null");
        }
        return configs.get(contextID);
    }

    @Override
    public PolicyConfigImpl getPolicyConfiguration(String contextID, boolean remove) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String, " + remove + ")");
        if (contextID == null) {
            throw new IllegalArgumentException("contextID is required to be non-null");
        }
        PolicyConfigImpl config = configs.get(contextID);
        if (config == null) {
            config = new PolicyConfigImpl(contextID);
            configs.put(contextID, config);
        } else {
            if (remove) {
                config.delete();
            }
            config.reOpen();
        }

        return config;
    }

    @Override
    public boolean inService(String contextID) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY_CONFIG_FACTORY, "inService");
        PolicyConfigImpl config = configs.get(contextID);
        return config == null ? false : config.inService();
    }
}