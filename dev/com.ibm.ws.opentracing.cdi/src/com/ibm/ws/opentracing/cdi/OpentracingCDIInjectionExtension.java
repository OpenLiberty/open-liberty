/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.opentracing.cdi;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.cdi.extension.WebSphereCDIExtension;

@Component(service = WebSphereCDIExtension.class, configurationPolicy = ConfigurationPolicy.IGNORE)
public class OpentracingCDIInjectionExtension implements WebSphereCDIExtension, Extension {
    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
        AnnotatedType<OpentracingProducerBean> at = bm.createAnnotatedType(OpentracingProducerBean.class);
        bbd.addAnnotatedType(at);
        AnnotatedType<Traced> bindingType = bm.createAnnotatedType(Traced.class);
        bbd.addInterceptorBinding(bindingType);
        AnnotatedType<TracedInterceptor> interceptorType = bm.createAnnotatedType(TracedInterceptor.class);
        bbd.addAnnotatedType(interceptorType);
    }
}
