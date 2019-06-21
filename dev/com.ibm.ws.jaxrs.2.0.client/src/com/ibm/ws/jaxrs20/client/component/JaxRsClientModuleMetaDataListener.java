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
package com.ibm.ws.jaxrs20.client.component;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.ModuleMetaDataListener;
import com.ibm.ws.jaxrs20.client.JAXRSClientImpl;
import com.ibm.ws.runtime.metadata.ModuleMetaData;

/**
 * Listening a Web/EJB module metadata events, and clean up JAXRSClientImpl objects on destroy.
 */
@Component(immediate = true, property = { "service.vendor=IBM" }, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class JaxRsClientModuleMetaDataListener implements ModuleMetaDataListener {

    private static final TraceComponent tc = Tr.register(JaxRsClientModuleMetaDataListener.class);

    @Override
    public void moduleMetaDataCreated(MetaDataEvent<ModuleMetaData> event) {
        //NO-OP
    }

    @Activate
    protected void activate(ComponentContext cc) {
        //NO-OP
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        //NO-OP
    }

    @Override
    public void moduleMetaDataDestroyed(MetaDataEvent<ModuleMetaData> event) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "moduleMetaDataDestroyed(" + event.getMetaData().getName() + ") : " + event.getMetaData());
        }

        JAXRSClientImpl.closeClients(event.getMetaData());
    }
}
