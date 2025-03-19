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
package com.ibm.ws.security.authorization.jacc.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.util.Map;

import javax.security.jacc.PolicyConfigurationFactory;
import javax.security.jacc.PolicyContext;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authorization.jacc.JaccService;
import com.ibm.ws.security.authorization.jacc.PolicyConfigurationManager;
import com.ibm.ws.security.authorization.jacc.common.PolicyContextUtil;
import com.ibm.ws.security.authorization.jacc.common.PolicyProxy;
import com.ibm.ws.security.authorization.jacc.common.ProviderServiceProxy;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(service = JaccService.class, name = "com.ibm.ws.security.authorization.jacc", configurationPolicy = ConfigurationPolicy.IGNORE, property = "service.vendor=IBM")
public class JaccServiceImpl implements JaccService {
    private static final TraceComponent tc = Tr.register(JaccServiceImpl.class);

    private static final String KEY_JACC_PROVIDER_SERVICE_PROXY = "jaccProviderServiceProxy";
    private static final String KEY_LOCATION_ADMIN = "locationAdmin";
    private final AtomicServiceReference<ProviderServiceProxy> jaccProviderServiceProxy = new AtomicServiceReference<ProviderServiceProxy>(KEY_JACC_PROVIDER_SERVICE_PROXY);
    private final AtomicServiceReference<WsLocationAdmin> locationAdminRef = new AtomicServiceReference<WsLocationAdmin>(KEY_LOCATION_ADMIN);
    private final PolicyConfigurationManager pcm;

    private String policyName = null;
    private String factoryName = null;

    private PolicyProxy policyProxy = null;
    private PolicyConfigurationFactory pcf = null;

    @Activate
    public JaccServiceImpl(@Reference PolicyConfigurationManager pcm) {
        this.pcm = pcm;
    }

    @Reference(service = ProviderServiceProxy.class, policy = ReferencePolicy.DYNAMIC, name = KEY_JACC_PROVIDER_SERVICE_PROXY)
    protected void setJaccProviderServiceProxy(ServiceReference<ProviderServiceProxy> reference) {
        jaccProviderServiceProxy.setReference(reference);
    }

    protected void unsetJaccProviderServiceProxy(ServiceReference<ProviderServiceProxy> reference) {
        jaccProviderServiceProxy.unsetReference(reference);
    }

    @Reference(service = WsLocationAdmin.class, name = KEY_LOCATION_ADMIN)
    protected void setLocationAdmin(ServiceReference<WsLocationAdmin> reference) {
        locationAdminRef.setReference(reference);
    }

    protected void unsetLocationAdmin(ServiceReference<WsLocationAdmin> reference) {
        locationAdminRef.unsetReference(reference);
    }

    @Activate
    protected void activate(ComponentContext cc) {
        jaccProviderServiceProxy.activate(cc);
        ProviderServiceProxy serviceProxy = jaccProviderServiceProxy.getService();
        if (serviceProxy != null) {
            policyName = serviceProxy.getPolicyName();
            factoryName = serviceProxy.getFactoryName();
        }
        Tr.info(tc, "JACC_SERVICE_STARTING", new Object[] { policyName, factoryName });
        locationAdminRef.activate(cc);
        if (loadClasses()) {
            Tr.info(tc, "JACC_SERVICE_STARTED", new Object[] { policyName, factoryName });
        } else {
            Tr.info(tc, "JACC_SERVICE_START_FAILURE", new Object[] { policyName, factoryName });
        }
    }

    @Modified
    protected void modify(Map<String, Object> props) {
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        locationAdminRef.deactivate(cc);
        jaccProviderServiceProxy.deactivate(cc);
        Tr.info(tc, "JACC_SERVICE_STOPPED", new Object[] { policyName });
    }

    /**
     * Loads the JACC Policy and Factory classes.
     *
     * @return true if the initialization was successful
     */
    public boolean loadClasses() {
        Boolean result = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {

                policyProxy = jaccProviderServiceProxy.getService().getPolicyProxy(pcm);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "policy object" + policyProxy);
                // in order to support the CTS provider, Policy object should be set prior to
                // instanciate PolicyConfigFactory class.
                if (policyProxy == null) {
                    Exception e = new Exception("Policy object is null.");
                    Tr.error(tc, "JACC_POLICY_INSTANTIATION_FAILURE", new Object[] { policyName, e });
                    return Boolean.FALSE;
                }
                try {
                    policyProxy.setPolicy();
                    policyProxy.refresh();
                } catch (ClassCastException cce) {
                    Tr.error(tc, "JACC_POLICY_INSTANTIATION_FAILURE", new Object[] { policyName, cce });
                    return Boolean.FALSE;
                }

                pcf = jaccProviderServiceProxy.getService().getPolicyConfigFactory();
                if (pcf != null) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "factory object : " + pcf);
                    pcm.initialize(policyProxy, pcf);
                } else {
                    Tr.error(tc, "JACC_FACTORY_INSTANTIATION_FAILURE", new Object[] { factoryName });
                    return Boolean.FALSE;
                }
                return Boolean.TRUE;
            }
        });
        return result.booleanValue();
    }

    @Override
    public String getContextId(String applicationName, String moduleName) {
        WsLocationAdmin locationAdmin = locationAdminRef.getService();
        return PolicyContextUtil.getContextId(locationAdmin, applicationName, moduleName);
    }

    @Override
    public PolicyConfigurationFactory getPolicyConfigurationFactory() {
        return pcf;
    }

    @Override
    public PolicyConfigurationManager getPolicyConfigurationManager() {
        return pcm;
    }

    @Override
    public PolicyProxy getPolicyProxy() {
        return policyProxy;
    }

    @Override
    public String getProviderServiceProperty(String propertyName) {
        ProviderServiceProxy reference = jaccProviderServiceProxy.getService();
        String value = null;
        if (reference != null) {
            Object obj = reference.getProperty(propertyName);
            if (obj instanceof String) {
                value = (String) obj;
            }
        }
        return value;
    }

    @Override
    public void resetPolicyContextHandlerInfo() {
        try {
            AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() {
                    // resetting the handler info as per spec..
                    PolicyContext.setHandlerData(null);
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception when resetting setHandlerData. Ignoring.. " + e.getException());
        }
    }
}
