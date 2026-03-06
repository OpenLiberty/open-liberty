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

        if (newProxyAdded.get()) {
            return existingConfig;
        }

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

    public void ensurePolicyConfigInitialized() {
        for (JakartaPolicyConfigProxy policyConfig : configMap.values()) {
            if (policyConfig != null) {
                policyConfig.ensureInitialized();
            }
        }
    }
}
