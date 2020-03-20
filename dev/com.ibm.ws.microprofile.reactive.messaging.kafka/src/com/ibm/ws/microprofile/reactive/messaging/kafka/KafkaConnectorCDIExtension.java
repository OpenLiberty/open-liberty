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
package com.ibm.ws.microprofile.reactive.messaging.kafka;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.cdi.CDIServiceUtils;
import com.ibm.wsspi.cdi.extension.WebSphereCDIExtension;

@Component(service = WebSphereCDIExtension.class, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE)
public class KafkaConnectorCDIExtension implements Extension, WebSphereCDIExtension {

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager) {
        AnnotatedType<AdapterFactoryImpl> adapterFactoryImplType = beanManager.createAnnotatedType(AdapterFactoryImpl.class);
        beforeBeanDiscovery.addAnnotatedType(adapterFactoryImplType, CDIServiceUtils.getAnnotatedTypeIdentifier(adapterFactoryImplType, this.getClass()));

        AnnotatedType<KafkaIncomingConnector> kafkaIncomingConnectorType = beanManager.createAnnotatedType(KafkaIncomingConnector.class);
        beforeBeanDiscovery.addAnnotatedType(kafkaIncomingConnectorType, CDIServiceUtils.getAnnotatedTypeIdentifier(kafkaIncomingConnectorType, this.getClass()));

        AnnotatedType<KafkaOutgoingConnector> kafkaOutgoingConnectorType = beanManager.createAnnotatedType(KafkaOutgoingConnector.class);
        beforeBeanDiscovery.addAnnotatedType(kafkaOutgoingConnectorType, CDIServiceUtils.getAnnotatedTypeIdentifier(kafkaOutgoingConnectorType, this.getClass()));
    }

}
