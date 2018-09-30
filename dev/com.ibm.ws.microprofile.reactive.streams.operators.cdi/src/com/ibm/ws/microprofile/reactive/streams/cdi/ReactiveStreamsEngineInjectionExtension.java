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
package com.ibm.ws.microprofile.reactive.streams.cdi;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;

@Component(service = WebSphereCDIExtension.class)
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
        AnnotatedType<RSBean> producer = bm.createAnnotatedType(RSBean.class);
        bbd.addAnnotatedType(producer);
    }
}

///*
// * This is CDI extension for CDI to identify there is a bean which produces ReactiveStreamsEngine
// * We need to register this extension so that CDI will identify this bundle needs scanning
// * Addition to this extension,  we need to have beens.xml file in META-INF directory for the
// * WELD implementation to scan the bean which uses @Produces
// */
//@Component(service = WebSphereCDIExtension.class, property = { "api.classes=org.eclipse.microprofile.reactive.streams.spi.ReactiveStreamsEngine" }, immediate = true)
//public class ReactiveStreamsEngineInjectionExtension implements Extension, WebSphereCDIExtension {
//    
//    private final static TraceComponent tc = Tr.register(ReactiveStreamsEngineInjectionExtension.class);
//
//    public ReactiveStreamsEngineInjectionExtension() {
//        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//            Tr.debug(tc, "ReactiveStreamsEngineInjectionExtension construction ");
//        }
//    }
//    
//    void processInjectionTarget(@Observes ProcessInjectionTarget<?> pit) {
//        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//            Tr.debug(tc, "ReactiveStreamsEngineInjectionExtension pit ");
//        }
//    }
//    
//    void afterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager bm) {
//        
//        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//            Tr.debug(tc, "ReactiveStreamsEngineInjectionExtension AfterBeanDiscoveryCalled");
//        }
//    }
//    
//    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
//        AnnotatedType<ReactiveStreamsEngineProducerBean> producer = bm.createAnnotatedType(ReactiveStreamsEngineProducerBean.class);
//        bbd.addAnnotatedType(producer);
//        
//        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//            Tr.debug(tc, "ReactiveStreamsEngineInjectionExtension BeforeBeanDiscoveryCalled added producer " + producer);
//        }
//    }
//    
//    protected Throwable processParameterizedType(InjectionPoint injectionPoint, ParameterizedType pType, ClassLoader classLoader) {
//        Throwable x = null;
//        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//            Tr.debug(tc, "ReactiveStreamsEngineInjectionExtension ppt");
//        }
//        return x;
//    }
//
//}