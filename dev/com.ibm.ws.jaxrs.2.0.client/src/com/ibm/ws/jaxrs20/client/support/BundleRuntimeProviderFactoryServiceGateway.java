/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.support;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.jaxrs20.api.JaxRsProviderFactoryService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * as a more reasonable method, for each bundle, it is better to use one service to import & export the service instances
 * this class works as gateway to import service, for client, no service is required to export
 */
public class BundleRuntimeProviderFactoryServiceGateway {

    private static BundleRuntimeProviderFactoryServiceGateway instance;

    public static BundleRuntimeProviderFactoryServiceGateway getInstance() {
        return instance;
    }

    public JaxRsProviderFactoryService getProviderFactory() {
        return providerFactoryServiceSR.getServiceWithException();
    }

    private final AtomicServiceReference<JaxRsProviderFactoryService> providerFactoryServiceSR =
                    new AtomicServiceReference<JaxRsProviderFactoryService>("providerFactoryService");

    protected void activate(ComponentContext cc) {
        if (instance == null) {
            instance = this;
        }
        providerFactoryServiceSR.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        providerFactoryServiceSR.deactivate(cc);
    }

    public void setProviderFactoryService(ServiceReference<JaxRsProviderFactoryService> serviceRef) {
        providerFactoryServiceSR.setReference(serviceRef);
    }

    public void unsetProviderFactoryService(ServiceReference<JaxRsProviderFactoryService> serviceRef) {
        providerFactoryServiceSR.unsetReference(serviceRef);
    }
}
