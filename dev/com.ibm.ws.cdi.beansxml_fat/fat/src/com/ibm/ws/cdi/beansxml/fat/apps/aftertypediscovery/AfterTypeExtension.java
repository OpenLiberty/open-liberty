/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.beansxml.fat.apps.aftertypediscovery;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterTypeDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

public class AfterTypeExtension implements Extension {

    void afterTypeDiscovery(@Observes AfterTypeDiscovery event, BeanManager bm) {

        event.addAnnotatedType(bm.createAnnotatedType(AfterTypeInterface.class), AfterTypeInterface.class.getName());
        event.addAnnotatedType(bm.createAnnotatedType(AfterTypeBean.class), AfterTypeBean.class.getName());
        event.addAnnotatedType(bm.createAnnotatedType(AfterTypeBeanDecorator.class), AfterTypeBeanDecorator.class.getName());
        event.getDecorators().add(AfterTypeBeanDecorator.class);

        event.addAnnotatedType(bm.createAnnotatedType(AfterTypeInterceptorImpl.class), AfterTypeInterceptorImpl.class.getName());
        event.addAnnotatedType(bm.createAnnotatedType(InterceptedAfterType.class), InterceptedAfterType.class.getName());
        event.addAnnotatedType(bm.createAnnotatedType(InterceptedBean.class), InterceptedBean.class.getName());
        event.getInterceptors().add(AfterTypeInterceptorImpl.class);

        event.addAnnotatedType(bm.createAnnotatedType(AfterTypeAlternativeInterface.class), AfterTypeAlternativeInterface.class.getName());
        event.addAnnotatedType(bm.createAnnotatedType(AfterTypeAlternativeOne.class), AfterTypeAlternativeOne.class.getName());
        event.addAnnotatedType(bm.createAnnotatedType(AfterTypeAlternativeTwo.class), AfterTypeAlternativeTwo.class.getName());
        event.addAnnotatedType(bm.createAnnotatedType(AfterTypeNotAlternative.class), AfterTypeNotAlternative.class.getName());
        event.addAnnotatedType(bm.createAnnotatedType(UseAlternative.class), UseAlternative.class.getName());
        event.getAlternatives().add(AfterTypeAlternativeOne.class);
        event.getAlternatives().add(AfterTypeAlternativeTwo.class);

    }

}
