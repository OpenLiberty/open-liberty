/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.misplaced.spi.test.bundle.extension;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

public class MyExtension implements Extension {

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager) {
        System.out.println("MISPLACED SPI registered extention beforeBeanDiscovery has been fired");
        AnnotatedType<ExtensionSPIRegisteredProducer> producerType = beanManager.createAnnotatedType(ExtensionSPIRegisteredProducer.class);
        beforeBeanDiscovery.addAnnotatedType(producerType, "my misplaced unique identifier");
    }

}
