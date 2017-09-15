package com.ibm.ws.jaxrs20.component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.jaxrs20.JaxRsConstants;
import com.ibm.ws.jaxrs20.api.JaxRsProviderFactoryService;
import com.ibm.ws.jaxrs20.providers.api.JaxRsProviderRegister;
import com.ibm.ws.kernel.feature.FeatureProvisioner;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
@Component(immediate = true, property = { "service.vendor=IBM" }, configurationPolicy = ConfigurationPolicy.IGNORE)
public class JaxRsProviderFactory implements JaxRsProviderFactoryService {

    private static JaxRsProviderFactory serviceInstance = null;



    @Override
    public void bindProviders(boolean clientSide, List<Object> providers) {
        if (serviceInstance != null && providers != null) {
            serviceInstance.bindDefaultProviders(clientSide, providers);
        }
    }

    private final List<JaxRsProviderRegister> providerRegisterList = new CopyOnWriteArrayList<JaxRsProviderRegister>();

    private final AtomicServiceReference<FeatureProvisioner> _featureProvisioner = new AtomicServiceReference<FeatureProvisioner>(JaxRsConstants.FEATUREPROVISIONER_REFERENCE_NAME);

    @Activate
    protected void activate(ComponentContext cc) {
        _featureProvisioner.activate(cc);
        serviceInstance = this;
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        _featureProvisioner.deactivate(cc);
        serviceInstance = null;
    }

    @Reference(name = JaxRsConstants.FEATUREPROVISIONER_REFERENCE_NAME, service = FeatureProvisioner.class, cardinality = ReferenceCardinality.MANDATORY)
    protected void setFeatureProvisioner(ServiceReference<FeatureProvisioner> ref) {
        _featureProvisioner.setReference(ref);
    }

    protected void unsetFeatureProvisioner(FeatureProvisioner featureProvisioner) {

    }

    @Reference(name = JaxRsConstants.JAXRS_PROVIDER_REGISTER_REFERENCE_NAME, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    public void addProviderRegister(JaxRsProviderRegister pr) {
        providerRegisterList.add(pr);
    }

    public void removeProviderRegister(JaxRsProviderRegister pr) {
        providerRegisterList.remove(pr);
    }

    public void bindDefaultProviders(boolean clientSide, List<Object> providers) {

        if (providerRegisterList.size() == 0)
            return;

        if (_featureProvisioner.getService() == null)
            return;

        Set<String> features = _featureProvisioner.getService().getInstalledFeatures();

        for (JaxRsProviderRegister pr : providerRegisterList) {
            pr.installProvider(clientSide, providers, features);
        }
    }
}
