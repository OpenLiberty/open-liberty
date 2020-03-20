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
package com.ibm.ws.microprofile.reactive.streams.cdi;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIServiceUtils;
import com.ibm.wsspi.cdi.extension.WebSphereCDIExtension;

@Component(service = WebSphereCDIExtension.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class ReactiveStreamsEngineInjectionExtension implements Extension, WebSphereCDIExtension {

    private final static TraceComponent tc = Tr.register(ReactiveStreamsEngineInjectionExtension.class);

    public ReactiveStreamsEngineInjectionExtension() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "ReactiveStreamsEngineInjectionExtension construction ");
        }
    }

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "ReactiveStreamsEngineInjectionExtension bbd ");
        }
        AnnotatedType<ReactiveStreamsEngineProducer> producer = bm.createAnnotatedType(ReactiveStreamsEngineProducer.class);
        bbd.addAnnotatedType(producer, CDIServiceUtils.getAnnotatedTypeIdentifier(producer, this.getClass()));
    }
}
