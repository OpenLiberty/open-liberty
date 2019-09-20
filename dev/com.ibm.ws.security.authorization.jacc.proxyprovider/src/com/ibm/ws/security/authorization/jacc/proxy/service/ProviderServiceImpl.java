/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authorization.jacc.proxy.service;

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
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.library.Library;
import com.ibm.wsspi.security.authorization.jacc.ProviderService;

@Component(service = ProviderService.class,
           immediate = true,
           name = "com.ibm.ws.security.authorization.jacc.proxyprovider",
           configurationPolicy = ConfigurationPolicy.OPTIONAL,
           property = { "service.vendor=IBM" })
public class ProviderServiceImpl implements ProviderService {
    private static final TraceComponent tc = Tr.register(ProviderServiceImpl.class);
    private static final String JACC_FACTORY = "javax.security.jacc.PolicyConfigurationFactory.provider";
    private static final String JACC_POLICY_PROVIDER = "javax.security.jacc.policy.provider";
    private Map<String, Object> props = null;
    private static final String POLICY_PROVIDER = "policyProviderClass";
    private static final String POLICY_CONFIGURATION_FACTORY = "policyConfigurationFactoryClass";
    private String policyProviderClass = null;
    private String policyConfigurationFactoryClass = null;

    private volatile Library sharedLibrary = null;

    public ProviderServiceImpl() {
    }

    @Reference(service = Library.class,
               name = "sharedLibrary",
               target = "(id=global)",
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setSharedLibrary(Library svc) {
        sharedLibrary = svc;
    }

    /** This is a required service, unset will be called after deactivate */
    protected void unsetSharedLibrary(Library ref) {
    }

    @Activate
    protected synchronized void activate(ComponentContext cc, Map<String, Object> props) {
        this.props = props;
        initialize(props);
    }

    @Modified
    protected synchronized void modify(Map<String, Object> props) {
        this.props = props;
        initialize(props);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        this.props = null;
    }

    private void initialize(Map<String, Object> props) {
        if (props != null) {
            policyProviderClass = (String) props.get(POLICY_PROVIDER);
            policyConfigurationFactoryClass = (String) props.get(POLICY_CONFIGURATION_FACTORY);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "config properties : policyName : " + policyProviderClass + " factoryName : " + policyConfigurationFactoryClass);
            }
        }
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public java.lang.Object run() {
                String systemPolicyProviderClass = System.getProperty(JACC_POLICY_PROVIDER);
                if (policyProviderClass == null) {
                    policyProviderClass = systemPolicyProviderClass;
                } else if (systemPolicyProviderClass == null) {
                    System.setProperty(JACC_POLICY_PROVIDER, policyProviderClass);
                }
                String systemPolicyConfigurationFactoryClass = System.getProperty(JACC_FACTORY);
                if (policyConfigurationFactoryClass == null) {
                    policyConfigurationFactoryClass = systemPolicyConfigurationFactoryClass;
                } else if (systemPolicyConfigurationFactoryClass == null) {
                    System.setProperty(JACC_FACTORY, policyConfigurationFactoryClass);
                }
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "System properties : policyName : " + systemPolicyProviderClass + " factoryName : " + systemPolicyConfigurationFactoryClass);
                }

                return null;
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public Policy getPolicy() {
        // load the code via the global shared library.
        ClassLoader sharedLibClassLoader = sharedLibrary.getClassLoader();
        Policy policy = null;
        try {
            Class<?> myClass = sharedLibClassLoader.loadClass(policyProviderClass);
            policy = (Policy) myClass.newInstance();
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Fail to load Policy Object : " + e);
            }
        }
        return policy;
    }

    /** {@inheritDoc} */
    @Override
    public PolicyConfigurationFactory getPolicyConfigFactory() {
        PolicyConfigurationFactory output = AccessController.doPrivileged(new PrivilegedAction<PolicyConfigurationFactory>() {
            @Override
            public PolicyConfigurationFactory run() {
                ClassLoader cl = null;
                PolicyConfigurationFactory pcf = null;
                try {
                    cl = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(sharedLibrary.getClassLoader());
                    pcf = PolicyConfigurationFactory.getPolicyConfigurationFactory();
                } catch (Exception e) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Failed to instanciate PolicyConfigurationFactory class");
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
}
