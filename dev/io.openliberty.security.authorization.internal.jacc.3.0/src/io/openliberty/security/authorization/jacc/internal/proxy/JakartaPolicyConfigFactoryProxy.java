/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.authorization.jacc.internal.proxy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import jakarta.security.jacc.PolicyConfiguration;
import jakarta.security.jacc.PolicyConfigurationFactory;
import jakarta.security.jacc.PolicyContext;
import jakarta.security.jacc.PolicyContextException;

/**
 * Stores the PolicyConfigurationFactory state so that as new PolicyConfigurationFactory instances
 * are configured dynamically, they can be populated and committed based off of the state of each application
 * that has started already.
 *
 * Only this class accesses the actual PolicyConfigurationFactory instances. All other Liberty runtime function interacts
 * with this class when using Jakarta Authorization 3.0. The actual PolicyConfigurationFactory is accessed
 * by the user's PolicyFactory instance to be able to get the state that this class propagates.
 */
public class JakartaPolicyConfigFactoryProxy extends PolicyConfigurationFactory {

    private final Map<String, JakartaPolicyConfigProxy> configMap = new ConcurrentHashMap<>();

    private static final JakartaPolicyConfigFactoryProxy configFactoryProxy = new JakartaPolicyConfigFactoryProxy();

    public static JakartaPolicyConfigFactoryProxy getInstance() {
        return configFactoryProxy;
    }

    private JakartaPolicyConfigFactoryProxy() {
    }

    @Override
    public PolicyConfiguration getPolicyConfiguration() {
        String currentContextID = PolicyContext.getContextID();
        if (currentContextID == null) {
            return null;
        }
        return configMap.get(currentContextID);
    }

    @Override
    public PolicyConfiguration getPolicyConfiguration(String contextId) {
        return configMap.get(contextId);
    }

    @Override
    public PolicyConfiguration getPolicyConfiguration(String contextId, boolean remove) throws PolicyContextException {

        // Do the fast check first for if it doesn't exist
        JakartaPolicyConfigProxy existingConfig = configMap.get(contextId);
        final AtomicBoolean newProxyAdded = new AtomicBoolean(false);
        if (existingConfig == null) {
            existingConfig = configMap.computeIfAbsent(contextId, new Function<String, JakartaPolicyConfigProxy>() {

                @Override
                public JakartaPolicyConfigProxy apply(String contextId) {
                    try {
                        JakartaPolicyConfigProxy newProxy = new JakartaPolicyConfigProxy(JakartaPolicyConfigFactoryProxy.this, contextId, remove);
                        newProxyAdded.set(true);
                        return newProxy;
                    } catch (PolicyContextException pce) {
                        sneakyThrow(pce);
                        return null;
                    }
                }
            });
        }

        // If we added a new PolicyConfiguration, there is no need to call reset to remove any config because it will
        // already be an empty PolicyConfiguration in open state
        if (newProxyAdded.get()) {
            return existingConfig;
        }

        // Call the actual PolicyConfigurationFactory.getPolicyConfiguration() method to drive any logic that it needs to run.
        // Namely this will make sure that the remove flag is handled and the state is back to open state
        existingConfig.resetDelegatePolicyConfig(remove);
        return existingConfig;
    }

    @SuppressWarnings("unchecked")
    static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

    @Override
    public boolean inService(String contextId) {
        JakartaPolicyConfigProxy policyConfig = configMap.get(contextId);
        return policyConfig == null ? false : policyConfig.inService();
    }

    @FFDCIgnore(IllegalStateException.class)
    PolicyConfigurationFactory getFactory() {
        PolicyConfigurationFactory factory = null;
        try {
            factory = PolicyConfigurationFactory.get();
        } catch (IllegalStateException ise) {
            // expected if nothing was set up
        }
        return factory;
    }

    /**
     * When adding a new PolicyConfigurationFactory from a web.xml config, this method populates the new factory with the
     * existing state and gets the PolicyConfiguration instances into the correct state.
     */
    public void ensurePolicyConfigInitialized() {
        for (JakartaPolicyConfigProxy policyConfig : configMap.values()) {
            if (policyConfig != null) {
                policyConfig.ensureInitialized();
            }
        }
    }
}
