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
package com.ibm.ws.microprofile.faulttolerance.cdi20;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.microprofile.faulttolerance.cdi.FaultToleranceInterceptor;
import com.ibm.wsspi.cdi.extension.WebSphereCDIExtension;

/**
 * Changes the way the FT Interceptor is registered when running with FT 2.0+ on CDI 2.0+
 */
@Component(service = WebSphereCDIExtension.class, immediate = true)
public class FaultToleranceCDI20Extension implements WebSphereCDIExtension, Extension {

    public void removeInterceptorType(@Observes ProcessAnnotatedType<FaultToleranceInterceptor> event) {
        // Veto the type, so that CDI doesn't discover that it's a bean
        event.veto();
    }

    public void registerInterceptor(@Observes AfterBeanDiscovery event, BeanManager bm) {
        // Register our Interceptor implementation
        // This allows us to dynamically set the priority
        event.addBean(new FaultToleranceCDI20Interceptor(bm));
    }

}
