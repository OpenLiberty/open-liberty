/*******************************************************************************
 * Copyright (c) 2015, 2025 IBM Corporation and others.
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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authorization.jacc.provider.JaccPolicyProxy;
import com.ibm.ws.security.authorization.jacc.role.FileRoleMapping;
import com.ibm.wsspi.security.authorization.jacc.ProviderService;

import jakarta.security.jacc.Policy;
import jakarta.security.jacc.PolicyConfigurationFactory;

@Component(service = ProviderService.class, immediate = true, name = "com.ibm.ws.security.authorization.jacc.provider", configurationPolicy = ConfigurationPolicy.OPTIONAL, property = { "service.vendor=IBM",
                                                                                                                                                                                         //                        "RequestMethodArgumentsRequired=true",
                                                                                                                                                                                         "jakarta.security.jacc.PolicyConfigurationFactory.provider=com.ibm.ws.security.authorization.jacc.provider.WSPolicyConfigurationFactoryImpl"
})
public class ProviderServiceImpl implements ProviderService {
    private static final TraceComponent tc = Tr.register(ProviderServiceImpl.class);

    private static final String JACC_FACTORY = PolicyConfigurationFactory.FACTORY_NAME;
    private static final String JACC_FACTORY_IMPL = "com.ibm.ws.security.authorization.jacc.provider.WSPolicyConfigurationFactoryImpl";

    private static final String CFG_ROLE_MAPPING_FILE = "roleMappingFile";

    public ProviderServiceImpl() {
    }

    @Activate
    protected synchronized void activate(ComponentContext cc, Map<String, Object> props) {
        FileRoleMapping.initialize(getRoleMappingFile(props));

    }

    @Modified
    protected synchronized void modify(Map<String, Object> props) {
        FileRoleMapping.initialize(getRoleMappingFile(props));
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
    }

    /** {@inheritDoc} */
    @Override
    public Policy getPolicy(String contextId) {
        return new JaccPolicyProxy(contextId);
    }

    /** {@inheritDoc} */
    @Override
    public PolicyConfigurationFactory getPolicyConfigFactory() {
        ClassLoader cl = null;
        PolicyConfigurationFactory pcf = null;
        if (System.getProperty(JACC_FACTORY) == null) {
            System.setProperty(JACC_FACTORY, JACC_FACTORY_IMPL);
        }
        try {
            cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            pcf = PolicyConfigurationFactory.getPolicyConfigurationFactory();
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to instantiate PolicyConfigurationFactory class");
            }
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
        return pcf;
    }

    private String getRoleMappingFile(Map<String, Object> props) {
        String roleMappingFile = null;
        if (props != null) {
            roleMappingFile = (String) props.get(CFG_ROLE_MAPPING_FILE);
        }
        return roleMappingFile;
    }
}
