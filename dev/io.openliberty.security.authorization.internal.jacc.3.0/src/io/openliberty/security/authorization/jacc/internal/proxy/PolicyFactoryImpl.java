/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.security.authorization.jacc.PolicyConfigurationManager;
import com.ibm.ws.security.authorization.jacc.common.PolicyContextUtil;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.security.authorization.jacc.ProviderService;

import jakarta.security.jacc.Policy;
import jakarta.security.jacc.PolicyFactory;

/**
 * Liberty provided PolicyFactory implementation that conditionally could wrap a user
 * provided PolicyFactory that is specified via spec defined system property.
 */
public class PolicyFactoryImpl extends PolicyFactory {

    private static final ComponentMetaDataAccessorImpl cmdAccessor = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();

    private final PolicyFactory wrapped;

    private final Map<String, Policy> policyMap;

    private final WsLocationAdmin locationAdmin;

    private final PolicyConfigurationManager pcm;

    final ProviderService providerService;

    PolicyFactoryImpl(WsLocationAdmin locationAdmin, ProviderService providerService, PolicyConfigurationManager pcm) {
        wrapped = null;
        this.locationAdmin = locationAdmin;
        this.providerService = providerService;
        this.pcm = pcm;
        policyMap = new ConcurrentHashMap<>();
    }

    PolicyFactoryImpl(PolicyFactory wrapped, WsLocationAdmin locationAdmin, ProviderService providerService, PolicyConfigurationManager pcm) {
        super(wrapped);
        this.wrapped = wrapped;
        this.locationAdmin = locationAdmin;
        this.providerService = providerService;
        this.pcm = pcm;
        policyMap = wrapped == null ? new ConcurrentHashMap<>() : null;
    }

    private String getContextIdFromComponentMetaData(String callingMethod) {
        ComponentMetaData cmd = cmdAccessor.getComponentMetaData();
        String appName = null;
        String moduleName = null;
        if (cmd != null) {
            appName = cmd.getJ2EEName().getApplication();
            ModuleMetaData mmd = cmd.getModuleMetaData();
            if (mmd != null) {
                moduleName = mmd.getName();
            }
        }
        if (appName != null) {
            if (pcm.isApplicationRunning(appName)) {
                throw new IllegalStateException(callingMethod + " can only be called during application startup or during permission check");
            }
            if (moduleName != null) {
                return PolicyContextUtil.getContextId(locationAdmin, appName, moduleName);
            }
        }
        return null;
    }

    @Override
    public Policy getPolicy(String contextId) {
        if (contextId == null) {
            contextId = getContextIdFromComponentMetaData("PolicyFactory.getPolicy");
        }

        // If it is still null, just return a null Policy object
        Policy policy;
        if (contextId == null) {
            policy = null;
        } else if (wrapped != null) {
            policy = wrapped.getPolicy(contextId);
        } else {
            policy = policyMap.get(contextId);
            if (policy == null && !policyMap.containsKey(contextId)) {
                // get policy and set it in the map
                policy = providerService.getPolicy(contextId);
                policyMap.put(contextId, policy);
            }
        }

        return policy;
    }

    @Override
    public void setPolicy(String contextId, Policy policy) {
        if (contextId == null) {
            contextId = getContextIdFromComponentMetaData("PolicyFactory.setPolicy");
        }
        // If it is still null, do nothing
        if (contextId != null) {
            if (wrapped != null) {
                wrapped.setPolicy(contextId, policy);
            } else {
                policyMap.put(contextId, policy);
            }
        }
    }
}
