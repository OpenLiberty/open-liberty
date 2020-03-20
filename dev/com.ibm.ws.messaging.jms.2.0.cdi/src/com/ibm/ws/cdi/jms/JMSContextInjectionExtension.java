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
package com.ibm.ws.cdi.jms;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.cdi.CDIServiceUtils;
import com.ibm.wsspi.cdi.extension.WebSphereCDIExtension;

/*
 * This is CDI extension for CDI to identify there is a bean which produces JMSContext
 * We need to register this extension so that CDI will identify this bundle needs scanning
 * Addition to this extension,  we need to have beens.xml file in META-INF directory for the
 * WELD implementation to scan the bean which uses @Produces
 */

@Component(service = WebSphereCDIExtension.class)
public class JMSContextInjectionExtension implements Extension, WebSphereCDIExtension {

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
        AnnotatedType<JMSContextInjectionBean> producer = bm.createAnnotatedType(JMSContextInjectionBean.class);
        bbd.addAnnotatedType(producer, CDIServiceUtils.getAnnotatedTypeIdentifier(producer, this.getClass()));
    }
}
