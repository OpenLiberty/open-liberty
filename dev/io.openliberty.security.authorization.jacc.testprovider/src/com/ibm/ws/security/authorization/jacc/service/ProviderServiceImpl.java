/*******************************************************************************
 * Copyright (c) 2015, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.authorization.jacc.service;

import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.ws.security.authorization.jacc.provider.JaccPolicyProxyFactory;
import com.ibm.ws.security.authorization.jacc.provider.WSPolicyConfigurationFactoryImpl;
import com.ibm.ws.security.authorization.jacc.role.FileRoleMapping;

import jakarta.security.jacc.PolicyConfigurationFactory;
import jakarta.security.jacc.PolicyFactory;

@Component(immediate = true, name = "com.ibm.ws.security.authorization.jacc.provider", configurationPolicy = ConfigurationPolicy.OPTIONAL, property = { "service.vendor=IBM" })
public class ProviderServiceImpl {

    private static final String CFG_ROLE_MAPPING_FILE = "roleMappingFile";

    private PolicyFactory prevPolicyFactory;
    private PolicyConfigurationFactory prevConfigFactory;

    public ProviderServiceImpl() {
    }

    @Activate
    protected synchronized void activate(ComponentContext cc, Map<String, Object> props) {
        FileRoleMapping.initialize(getRoleMappingFile(props));

        try {
            prevPolicyFactory = PolicyFactory.getPolicyFactory();
        } catch (SecurityException se) {
        }

        try {
            prevConfigFactory = PolicyConfigurationFactory.get();
        } catch (IllegalStateException se) {
            // expected if there isn't one configured
        }

        PolicyFactory.setPolicyFactory(new JaccPolicyProxyFactory());
        PolicyConfigurationFactory.setPolicyConfigurationFactory(new WSPolicyConfigurationFactoryImpl());
    }

    @Modified
    protected synchronized void modify(Map<String, Object> props) {
        FileRoleMapping.initialize(getRoleMappingFile(props));
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        PolicyFactory currentFactory = null;
        try {
            currentFactory = PolicyFactory.getPolicyFactory();
        } catch (SecurityException se) {
        }

        if (currentFactory != null && currentFactory.getClass() == JaccPolicyProxyFactory.class) {
            PolicyFactory.setPolicyFactory(prevPolicyFactory);
        }

        PolicyConfigurationFactory currentConfigFactory = null;
        try {
            currentConfigFactory = PolicyConfigurationFactory.get();
        } catch (IllegalStateException ise) {
            // expected if it was removed
        }
        if (currentConfigFactory != null && currentConfigFactory.getClass() == WSPolicyConfigurationFactoryImpl.class) {
            PolicyConfigurationFactory.setPolicyConfigurationFactory(prevConfigFactory);
        }

    }

    private String getRoleMappingFile(Map<String, Object> props) {
        String roleMappingFile = null;
        if (props != null) {
            roleMappingFile = (String) props.get(CFG_ROLE_MAPPING_FILE);
        }
        return roleMappingFile;
    }
}
