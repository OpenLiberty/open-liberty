/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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
import com.ibm.ws.security.authorization.jacc.PolicyConfigurationManager;
import com.ibm.ws.security.authorization.jacc.common.PolicyProxy;
import com.ibm.ws.security.authorization.jacc.common.ProviderServiceProxy;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.authorization.jacc.ProviderService;

import jakarta.security.jacc.PolicyConfigurationFactory;
import jakarta.security.jacc.PolicyFactory;

@Component(service = ProviderServiceProxy.class, immediate = true, name = "io.openliberty.security.authorization.jacc.provider.proxy",
           configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class ProviderServiceProxyImpl implements ProviderServiceProxy {

    private static final TraceComponent tc = Tr.register(ProviderServiceProxyImpl.class);

    static final String KEY_JACC_PROVIDER_SERVICE = "jaccProviderService";
    private static final String KEY_LOCATION_ADMIN = "locationAdmin";
    private final AtomicServiceReference<ProviderService> jaccProviderService = new AtomicServiceReference<ProviderService>(KEY_JACC_PROVIDER_SERVICE);
    private final AtomicServiceReference<WsLocationAdmin> locationAdminRef = new AtomicServiceReference<WsLocationAdmin>(KEY_LOCATION_ADMIN);

    private String policyFactoryName = null;
    private String factoryName = null;

    private String originalSystemPolicyFactoryName = null;
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

    @Reference(service = WsLocationAdmin.class, name = KEY_LOCATION_ADMIN)
    protected void setLocationAdmin(ServiceReference<WsLocationAdmin> reference) {
        locationAdminRef.setReference(reference);
    }

    protected void unsetLocationAdmin(ServiceReference<WsLocationAdmin> reference) {
        locationAdminRef.unsetReference(reference);
    }

    @Override
    public PolicyProxy getPolicyProxy(PolicyConfigurationManager pcm) {
        ProviderService providerService = jaccProviderService.getService();
        if (providerService == null) {
            return null;
        }
        WsLocationAdmin locationAdmin = locationAdminRef.getService();
        PolicyFactory policyFactory;
        if (policyFactoryName != null) {
            policyFactory = PolicyFactory.getPolicyFactory();
            if (!(policyFactory instanceof PolicyFactoryImpl)) {
                policyFactory = new PolicyFactoryImpl(policyFactory, locationAdmin, providerService, pcm);
                PolicyFactory.setPolicyFactory(policyFactory);
            } else if (((PolicyFactoryImpl) policyFactory).providerService != providerService) {
                policyFactory = new PolicyFactoryImpl(policyFactory.getWrapped(), locationAdmin, providerService, pcm);
                PolicyFactory.setPolicyFactory(policyFactory);
            }
        } else {
            policyFactory = new PolicyFactoryImpl(locationAdmin, providerService, pcm);
            PolicyFactory.setPolicyFactory(policyFactory);
        }
        return new JakartaPolicyFactoryProxyImpl();
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
        return policyFactoryName != null ? policyFactoryName : PolicyFactoryImpl.class.getName();
    }

    @Override
    public String getFactoryName() {
        return factoryName;
    }

    protected void activate(ComponentContext cc) {
        locationAdminRef.activate(cc);
        jaccProviderService.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        // unset PolicyFactory so we do not leak the user feature classloader for the ProviderService
        PolicyFactory.setPolicyFactory(null);

        jaccProviderService.deactivate(cc);
        locationAdminRef.deactivate(cc);

    }

    private void initializeSystemProperties(ServiceReference<ProviderService> reference) {
        Object obj = reference.getProperty(PolicyFactory.FACTORY_NAME);
        if (obj != null && obj instanceof String) {
            policyFactoryName = (String) obj;
        }

        obj = reference.getProperty(PolicyConfigurationFactory.FACTORY_NAME);
        if (obj != null && obj instanceof String) {
            factoryName = (String) obj;
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Meta data : policyFactoryName : " + policyFactoryName + " factoryName : " + factoryName);

        originalSystemPolicyFactoryName = null;
        originalSystemFactoryName = null;

        String systemPolicyFactoryName = System.getProperty(PolicyFactory.FACTORY_NAME);

        String systemFactoryName = System.getProperty(PolicyConfigurationFactory.FACTORY_NAME);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "System properties : policyFactoryName : " + systemPolicyFactoryName + " factoryName : " + systemFactoryName);
        }
        if (systemPolicyFactoryName == null) {
            if (policyFactoryName != null) {
                System.setProperty(PolicyFactory.FACTORY_NAME, policyFactoryName);
            }
        } else {
            if (policyFactoryName == null) {
                policyFactoryName = systemPolicyFactoryName;
            } else if (!systemPolicyFactoryName.equals(policyFactoryName)) {
                Tr.warning(tc, "JACC_INCONSISTENT_POLICY_CLASS", new Object[] { systemPolicyFactoryName, policyFactoryName });
                System.setProperty(PolicyFactory.FACTORY_NAME, policyFactoryName);
                originalSystemPolicyFactoryName = systemPolicyFactoryName;
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
        if (originalSystemPolicyFactoryName != null) {
            System.setProperty(PolicyFactory.FACTORY_NAME, originalSystemPolicyFactoryName);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "PolicyFactoryName system property is restored by : " + originalSystemPolicyFactoryName);
            }
        }
        if (originalSystemFactoryName != null) {
            System.setProperty(PolicyConfigurationFactory.FACTORY_NAME, originalSystemFactoryName);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "PolicyConfigurationFactory system property is restored by : " + originalSystemFactoryName);
            }
        }
    }
}
