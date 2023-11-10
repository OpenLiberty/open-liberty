/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.rest.client.component;

import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.List;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * DS component used to notify any registered {@link CxfRestClientBeanBuilderListener} services of a new builder.
 */
@Component(immediate = true)
public class CxfRestClientBeanBuilderNotifier {
    
    private static CxfRestClientBeanBuilderNotifier instance;

    @Reference(cardinality=MULTIPLE, policyOption=GREEDY, policy=DYNAMIC)
    private volatile List<CxfRestClientBeanBuilderListener> listeners;
    
    public static CxfRestClientBeanBuilderNotifier getInstance() {
        return instance;
    }
    
    public void newBuilder(RestClientBuilder builder) {
        for (CxfRestClientBeanBuilderListener listener : listeners) {
            listener.onNewBuilder(builder);
        }
    }
    
    @Activate
    protected void activate() {
        instance = this;
    }
    
    @Deactivate
    protected void deactivate() {
        if (instance == this) {
            instance = null;
        }
    }
}
