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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.cxf.microprofile.client.proxy.MicroProfileClientProxyImpl;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 * DS Component that should only be constructed by DS.
 * This component notifies registered listeners when a new MP Rest Client 
 * is constructed via the <code>MicroProfileClientFactoryBean.createClientProxy</code>
 * method.
 */
@Component(immediate=true)
public class RestClientNotifier {

    private static RestClientNotifier instance;

    private final List<RestClientBuildListener> listeners = new CopyOnWriteArrayList<>();

    public static RestClientNotifier getInstance() {
        return instance;
    }

    public void newRestClientProxy(MicroProfileClientProxyImpl clientProxy) {
        for (RestClientBuildListener listener : listeners) {
            listener.onNewRestClient(clientProxy);
        }
    }

    @Activate
    protected void activate() {
        instance = this;
    }

    @Deactivate
    protected void deactivate() {
        instance = null;
    }

    @Reference(cardinality=ReferenceCardinality.MULTIPLE, policyOption=ReferencePolicyOption.GREEDY, policy=ReferencePolicy.DYNAMIC)
    protected void addListener(RestClientBuildListener newListener) {
        listeners.add(newListener);
    }

    protected void removeListener(RestClientBuildListener newListener) {
        listeners.remove(newListener);
    }
}
