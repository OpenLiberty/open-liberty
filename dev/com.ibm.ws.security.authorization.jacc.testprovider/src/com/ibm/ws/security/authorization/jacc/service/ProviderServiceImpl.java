/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.authorization.jacc.service;

import java.security.AccessController;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.util.Map;

import javax.security.jacc.PolicyConfigurationFactory;

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

@Component(service = ProviderService.class, immediate = true, name = "com.ibm.ws.security.authorization.jacc.provider", configurationPolicy = ConfigurationPolicy.OPTIONAL, property = { "service.vendor=IBM",
                                                                                                                                                                                         //                        "RequestMethodArgumentsRequired=true",
                                                                                                                                                                                         "javax.security.jacc.policy.provider=com.ibm.ws.security.authorization.jacc.provider.JaccPolicyProxy",
                                                                                                                                                                                         "jakarta.security.jacc.policy.provider=com.ibm.ws.security.authorization.jacc.provider.JaccPolicyProxy",
                                                                                                                                                                                         "javax.security.jacc.PolicyConfigurationFactory.provider=com.ibm.ws.security.authorization.jacc.provider.WSPolicyConfigurationFactoryImpl",
                                                                                                                                                                                         "jakarta.security.jacc.PolicyConfigurationFactory.provider=com.ibm.ws.security.authorization.jacc.provider.WSPolicyConfigurationFactoryImpl"
})
public class ProviderServiceImpl implements ProviderService {
    private static final TraceComponent tc = Tr.register(ProviderServiceImpl.class);

    private static final String JACC_FACTORY = "javax.security.jacc.PolicyConfigurationFactory.provider";
    private static final String JACC_FACTORY_EE9 = "jakarta.security.jacc.PolicyConfigurationFactory.provider";
    private static final String JACC_FACTORY_IMPL = "com.ibm.ws.security.authorization.jacc.provider.WSPolicyConfigurationFactoryImpl";

    private static final String JACC_POLICY_PROVIDER = "javax.security.jacc.policy.provider";
    private static final String JACC_POLICY_PROVIDER_EE9 = "jakarta.security.jacc.policy.provider";
    private static final String JACC_POLICY_PROVIDER_IMPL = "com.ibm.ws.security.authorization.jacc.provider.JaccPolicyProxy";
    private static final String CFG_ROLE_MAPPING_FILE = "roleMappingFile";

    public ProviderServiceImpl() {}

    @Activate
    protected synchronized void activate(ComponentContext cc, Map<String, Object> props) {
        FileRoleMapping.initialize(getRoleMappingFile(props));

    }

    @Modified
    protected synchronized void modify(Map<String, Object> props) {
        FileRoleMapping.initialize(getRoleMappingFile(props));
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {}

    /** {@inheritDoc} */
    @Override
    public Policy getPolicy() {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public java.lang.Object run() {
                if (System.getProperty(JACC_POLICY_PROVIDER) == null) {
                    System.setProperty(JACC_POLICY_PROVIDER, JACC_POLICY_PROVIDER_IMPL);
                }
                if (System.getProperty(JACC_POLICY_PROVIDER_EE9) == null) {
                    System.setProperty(JACC_POLICY_PROVIDER_EE9, JACC_POLICY_PROVIDER_IMPL);
                }
                return null;
            }
        });
        return new JaccPolicyProxy();
    }

    /** {@inheritDoc} */
    @Override
    public PolicyConfigurationFactory getPolicyConfigFactory() {
        PolicyConfigurationFactory output = AccessController.doPrivileged(new PrivilegedAction<PolicyConfigurationFactory>() {
            @Override
            public PolicyConfigurationFactory run() {
                ClassLoader cl = null;
                PolicyConfigurationFactory pcf = null;
                if (System.getProperty(JACC_FACTORY) == null) {
                    System.setProperty(JACC_FACTORY, JACC_FACTORY_IMPL);
                }
                if (System.getProperty(JACC_FACTORY_EE9) == null) {
                    System.setProperty(JACC_FACTORY_EE9, JACC_FACTORY_IMPL);
                }
                try {
                    cl = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
                    pcf = PolicyConfigurationFactory.getPolicyConfigurationFactory();
                } catch (Exception e) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Failed to instantiate PolicyConfigurationFactory class");
                    }
                    return null;
                } finally {
                    Thread.currentThread().setContextClassLoader(cl);
                }
                return pcf;
            }
        });
        return output;
    }

    private String getRoleMappingFile(Map<String, Object> props) {
        String roleMappingFile = null;
        if (props != null) {
            roleMappingFile = (String) props.get(CFG_ROLE_MAPPING_FILE);
        }
        return roleMappingFile;
    }
}
