/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.authorization.jacc.internal.proxy;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.security.authorization.jacc.PolicyConfigurationManager;
import com.ibm.ws.security.authorization.jacc.common.PolicyProxy;
import com.ibm.ws.security.authorization.jacc.common.ProviderServiceProxy;

import jakarta.security.jacc.PolicyConfigurationFactory;

@Component(service = ProviderServiceProxy.class, immediate = true, name = "io.openliberty.security.authorization.jacc.provider.proxy",
           configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class ProviderServiceProxyImpl implements ProviderServiceProxy {

    @Override
    public PolicyProxy getPolicyProxy(PolicyConfigurationManager pcm) {
        return new JakartaPolicyFactoryProxyImpl();
    }

    @Override
    public PolicyConfigurationFactory getPolicyConfigFactory() {
        return JakartaPolicyConfigFactoryProxy.getInstance();
    }

    @Override
    public Object getProperty(String property) {
        return null;
    }

    @Override
    public String getPolicyName() {
        return JakartaPolicyFactoryProxyImpl.class.getName();
    }

    @Override
    public String getFactoryName() {
        return JakartaPolicyConfigFactoryProxy.class.getName();
    }
}
