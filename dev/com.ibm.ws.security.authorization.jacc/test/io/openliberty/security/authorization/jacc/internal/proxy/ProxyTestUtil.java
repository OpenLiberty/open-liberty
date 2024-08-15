/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.authorization.jacc.internal.proxy;

import java.security.Policy;

import org.osgi.framework.ServiceReference;

import com.ibm.ws.security.authorization.jacc.common.PolicyProxy;
import com.ibm.wsspi.security.authorization.jacc.ProviderService;

/**
 * Utility to set the ProviderService ServiceReference and create a PolicyProxy
 */
public class ProxyTestUtil {
    public static void setProviderService(ProviderServiceProxyImpl providerServiceProxy, ServiceReference<ProviderService> jaccProviderServiceRef) {
        providerServiceProxy.setJaccProviderService(jaccProviderServiceRef);
    }

    public static void unsetProviderService(ProviderServiceProxyImpl providerServiceProxy, ServiceReference<ProviderService> jaccProviderServiceRef) {
        providerServiceProxy.unsetJaccProviderService(jaccProviderServiceRef);
    }

    public static PolicyProxy createPolicyProxy(Policy policy) {
        return new JavaSePolicyProxyImpl(policy);
    }

}
