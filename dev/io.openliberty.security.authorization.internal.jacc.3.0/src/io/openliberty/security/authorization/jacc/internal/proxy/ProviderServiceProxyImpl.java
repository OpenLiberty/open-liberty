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

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authorization.jacc.common.PolicyProxy;
import com.ibm.ws.security.authorization.jacc.common.ProviderServiceProxy;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.authorization.jacc.ProviderService;

import jakarta.security.jacc.PolicyConfigurationFactory;
import jakarta.security.jacc.PolicyFactory;

@Component(service = ProviderServiceProxy.class, immediate = true, name = "io.openliberty.security.authorization.jacc.provider.proxy",
           configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class ProviderServiceProxyImpl implements ProviderServiceProxy {

    private static final TraceComponent tc = Tr.register(ProviderServiceProxyImpl.class);

    static final String KEY_JACC_PROVIDER_SERVICE = "jaccProviderService";
    private final AtomicServiceReference<ProviderService> jaccProviderService = new AtomicServiceReference<ProviderService>(KEY_JACC_PROVIDER_SERVICE);

    private String policyName = null;
    private String factoryName = null;

    private String originalSystemPolicyName = null;
    private String originalSystemFactoryName = null;

    @Reference(service = ProviderService.class, policy = ReferencePolicy.DYNAMIC, name = KEY_JACC_PROVIDER_SERVICE)
    protected void setJaccProviderService(ServiceReference<ProviderService> reference) {
        jaccProviderService.setReference(reference);
        initializeSystemProperties(reference);
    }

    protected void unsetJaccProviderService(ServiceReference<ProviderService> reference) {
        jaccProviderService.unsetReference(reference);
        restoreSystemProperties();
    }

    @Override
    public PolicyProxy getPolicyProxy() {
        ProviderService providerService = jaccProviderService.getService();
        if (providerService == null) {
            return null;
        }
        PolicyFactory policyFactory = providerService.getPolicyFactory();
        return policyFactory == null ? null : new JakartaPolicyFactoryProxyImpl(policyFactory);
    }

    @Override
    public PolicyConfigurationFactory getPolicyConfigFactory() {
        ProviderService providerService = jaccProviderService.getService();
        return providerService == null ? null : providerService.getPolicyConfigFactory();
    }

    @Override
    public Object getProperty(String property) {
        ServiceReference<ProviderService> serviceRef = jaccProviderService.getReference();
        return serviceRef == null ? null : serviceRef.getProperty(property);
    }

    @Override
    public String getPolicyName() {
        return policyName;
    }

    @Override
    public String getFactoryName() {
        return factoryName;
    }

    protected void activate(ComponentContext cc) {
        jaccProviderService.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        jaccProviderService.deactivate(cc);
    }

    private void initializeSystemProperties(ServiceReference<ProviderService> reference) {
        Object obj = reference.getProperty(PolicyFactory.FACTORY_NAME);
        if (obj != null && obj instanceof String) {
            policyName = (String) obj;
        }

        obj = reference.getProperty(PolicyConfigurationFactory.FACTORY_NAME);
        if (obj != null && obj instanceof String) {
            factoryName = (String) obj;
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Meta data : policyName : " + policyName + " factoryName : " + factoryName);

        originalSystemPolicyName = null;
        originalSystemFactoryName = null;

        String systemPolicyName = System.getProperty(PolicyFactory.FACTORY_NAME);

        String systemFactoryName = System.getProperty(PolicyConfigurationFactory.FACTORY_NAME);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "System properties : policyName : " + systemPolicyName + " factoryName : " + systemFactoryName);
        }
        if (systemPolicyName == null) {
            if (policyName != null) {
                System.setProperty(PolicyFactory.FACTORY_NAME, policyName);
            } else if (policyName == null) {
                Tr.error(tc, "JACC_POLICY_IS_NOT_SET");
                return;
            }
        } else {
            if (policyName == null) {
                policyName = systemPolicyName;
            } else if (!systemPolicyName.equals(policyName)) {
                Tr.warning(tc, "JACC_INCONSISTENT_POLICY_CLASS", new Object[] { systemPolicyName, policyName });
                System.setProperty(PolicyFactory.FACTORY_NAME, policyName);
                originalSystemPolicyName = systemPolicyName;
            }
        }
        if (systemFactoryName == null) {
            if (factoryName != null) {
                System.setProperty(PolicyConfigurationFactory.FACTORY_NAME, factoryName);
            } else if (factoryName == null) {
                Tr.error(tc, "JACC_FACTORY_IS_NOT_SET");
                return;
            }
        } else {
            if (factoryName == null) {
                factoryName = systemFactoryName;
            } else if (!systemFactoryName.equals(factoryName)) {
                Tr.warning(tc, "JACC_INCONSISTENT_FACTORY_CLASS", new Object[] { systemFactoryName, factoryName });
                System.setProperty(PolicyConfigurationFactory.FACTORY_NAME, factoryName);
                originalSystemFactoryName = systemFactoryName;
            }
        }
    }

    private void restoreSystemProperties() {
        if (originalSystemPolicyName != null) {
            System.setProperty(PolicyFactory.FACTORY_NAME, originalSystemPolicyName);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "PolicyName system property is restored by : " + originalSystemPolicyName);
            }
        }
        if (originalSystemFactoryName != null) {
            System.setProperty(PolicyConfigurationFactory.FACTORY_NAME, originalSystemFactoryName);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "PolicyName system property is restored by : " + originalSystemFactoryName);
            }
        }
    }
}
