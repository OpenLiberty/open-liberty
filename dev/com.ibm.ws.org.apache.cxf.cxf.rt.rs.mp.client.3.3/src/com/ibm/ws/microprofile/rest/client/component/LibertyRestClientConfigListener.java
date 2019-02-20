/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.rest.client.component;


import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.client.spec.TLSConfiguration;
import org.apache.cxf.microprofile.client.proxy.MicroProfileClientProxyImpl;
import org.apache.cxf.phase.Phase;
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.jaxrs20.client.configuration.LibertyJaxRsClientConfigInterceptor;
import com.ibm.ws.microprofile.rest.client.component.RestClientBuildListener;

/**
 * This DS service registers a listener for new MP Rest Client proxy instances,
 * and will register the client config out interceptor with it.
 */
@Component(immediate=true)
public class LibertyRestClientConfigListener implements RestClientBuildListener {

    /** {@inheritDoc} */
    @Override
    public void onNewRestClient(MicroProfileClientProxyImpl clientProxy) {
        // install outbound Client Config handler
        LibertyJaxRsClientConfigInterceptor configInterceptor = new LibertyJaxRsClientConfigInterceptor(Phase.PRE_LOGICAL);
        ClientConfiguration ccfg = WebClient.getConfig(clientProxy);
        ccfg.getOutInterceptors().add(configInterceptor);
    }

}
