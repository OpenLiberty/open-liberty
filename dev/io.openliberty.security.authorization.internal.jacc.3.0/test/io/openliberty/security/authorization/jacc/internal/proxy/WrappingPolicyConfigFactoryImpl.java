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

/**
 * This implementation wraps an existing PolicyConfigurationFactory and wraps the
 * PolicyConfigurations from the wrapped implementation.
 */
public class WrappingPolicyConfigFactoryImpl extends PolicyConfigurationFactory {

    private final PolicyConfigFactoryImpl wrappee;

    Map<String, WrappedPolicyConfigImpl> configs = new HashMap<>();

    WrappingPolicyConfigFactoryImpl() {
        PolicyConfigurationFactory pcf = PolicyConfigurationFactory.get();
        if (!(pcf instanceof PolicyConfigFactoryImpl)) {
            throw new IllegalStateException(pcf + " is not a PolicyConfigFactoryImpl");
        }
        wrappee = (PolicyConfigFactoryImpl) pcf;
    }

    WrappingPolicyConfigFactoryImpl(PolicyConfigFactoryImpl configFactory) {
        wrappee = configFactory;
    }

    private WrappedPolicyConfigImpl getAndPopulateConfigIfExistsinWrappedFactory(String contextID) {
        WrappedPolicyConfigImpl config = configs.get(contextID);
        if (config == null) {
            PolicyConfigImpl wrappedConfig = wrappee.getPolicyConfiguration(contextID);
            if (wrappedConfig != null) {
                config = new WrappedPolicyConfigImpl(contextID, wrappedConfig);
                configs.put(contextID, config);
            }
        }
        return config;

    }

    @Override
    public WrappedPolicyConfigImpl getPolicyConfiguration() {
        AuthzModuleTracker.addOperation(null, ModuleType.WRAPPING_POLICY_CONFIG_FACTORY, "getPolicyConfiguration");
        String contextID = PolicyContext.getContextID();
        if (contextID == null) {
            return null;
        }
        return getAndPopulateConfigIfExistsinWrappedFactory(contextID);
    }

    @Override
    public WrappedPolicyConfigImpl getPolicyConfiguration(String contextID) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.WRAPPING_POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String)");
        if (contextID == null) {
            throw new IllegalArgumentException("contextID is required to be non-null");
        }
        return getAndPopulateConfigIfExistsinWrappedFactory(contextID);
    }

    @Override
    public WrappedPolicyConfigImpl getPolicyConfiguration(String contextID, boolean remove) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.WRAPPING_POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String, " + remove + ")");
        if (contextID == null) {
            throw new IllegalArgumentException("contextID is required to be non-null");
        }
        WrappedPolicyConfigImpl config = configs.get(contextID);
        if (config == null) {
            config = new WrappedPolicyConfigImpl(contextID, wrappee.getPolicyConfiguration(contextID, remove));
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
        AuthzModuleTracker.addOperation(contextID, ModuleType.WRAPPING_POLICY_CONFIG_FACTORY, "inService");
        if (contextID == null) {
            throw new IllegalArgumentException("contextID is required to be non-null");
        }
        WrappedPolicyConfigImpl config = getAndPopulateConfigIfExistsinWrappedFactory(contextID);
        return config == null ? false : config.inService();
    }
}
