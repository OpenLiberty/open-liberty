/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.concurrent.internal.cdi;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;

import com.ibm.ws.cdi.CDIServiceUtils;

import io.openliberty.concurrent.internal.cdi.interceptor.AsyncInterceptor;
import jakarta.enterprise.concurrent.Asynchronous;

public class ConcurrencyExtension implements Extension {
    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager) {
        // register the interceptor binding and the interceptor
        AnnotatedType<Asynchronous> bindingType = beanManager.createAnnotatedType(Asynchronous.class);
        beforeBeanDiscovery.addInterceptorBinding(bindingType);
        AnnotatedType<AsyncInterceptor> interceptorType = beanManager.createAnnotatedType(AsyncInterceptor.class);
        beforeBeanDiscovery.addAnnotatedType(interceptorType, CDIServiceUtils.getAnnotatedTypeIdentifier(interceptorType, this.getClass()));
    }
}