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

import io.openliberty.security.authorization.jacc.internal.proxy.AuthzModuleTracker.ModuleType;
import jakarta.security.jacc.PolicyConfigurationFactory;
import jakarta.security.jacc.PolicyContextException;

/**
 * This implementation wraps an existing PolicyConfigurationFactory and just delegates
 * to the wrapped implementation and doesn't modify the PolicyConfigurations at all.
 */
public class DelegatingPolicyConfigFactoryImpl extends PolicyConfigurationFactory {

    private final PolicyConfigFactoryImpl delegate;

    DelegatingPolicyConfigFactoryImpl() {
        PolicyConfigurationFactory pcf = PolicyConfigurationFactory.get();
        if (!(pcf instanceof PolicyConfigFactoryImpl)) {
            throw new IllegalStateException(pcf + " is not a PolicyConfigFactoryImpl");
        }
        delegate = (PolicyConfigFactoryImpl) pcf;
    }

    DelegatingPolicyConfigFactoryImpl(PolicyConfigFactoryImpl configFactory) {
        delegate = configFactory;
    }

    @Override
    public PolicyConfigImpl getPolicyConfiguration() {
        AuthzModuleTracker.addOperation(null, ModuleType.DELEGATING_POLICY_CONFIG_FACTORY, "getPolicyConfiguration");
        return delegate.getPolicyConfiguration();
    }

    @Override
    public PolicyConfigImpl getPolicyConfiguration(String contextID) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.DELEGATING_POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String)");
        return delegate.getPolicyConfiguration(contextID);
    }

    @Override
    public PolicyConfigImpl getPolicyConfiguration(String contextID, boolean remove) throws PolicyContextException {
        AuthzModuleTracker.addOperation(contextID, ModuleType.DELEGATING_POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String, " + remove + ")");
        return delegate.getPolicyConfiguration(contextID, remove);
    }

    @Override
    public boolean inService(String contextID) throws PolicyContextException {
        AuthzModuleTracker.addOperation(contextID, ModuleType.DELEGATING_POLICY_CONFIG_FACTORY, "inService");
        return delegate.inService(contextID);
    }
}
