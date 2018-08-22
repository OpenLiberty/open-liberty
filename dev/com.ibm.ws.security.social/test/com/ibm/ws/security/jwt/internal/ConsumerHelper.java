/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.internal;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.security.jwt.Consumer;
import com.ibm.ws.security.jwt.config.ConsumerUtils;
import com.ibm.ws.security.jwt.config.JwtConsumerConfig;

public class ConsumerHelper {
    static final String idTokenConsumer = "facebookLogin";

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static ServiceReference<Consumer> getActivatedConsumerServiceReference(Mockery mockery, final ComponentContext cc, final JwtConsumerConfig jwtConsumerConfig) {
        final ConsumerImpl consumerImpl = new ConsumerImpl();
        final ConsumerUtils consumerUtil = new ConsumerUtils(null);
        final ServiceReference<JwtConsumerConfig> jwtConsumerConfigServiceRef = mockery.mock(ServiceReference.class, "jwtConsumerConfigServiceRef");
        mockery.checking(new Expectations() {
            {
                allowing(jwtConsumerConfigServiceRef).getProperty("id");
                will(returnValue(idTokenConsumer));
                allowing(jwtConsumerConfigServiceRef).getProperty("service.id");
                will(returnValue(123L));
                allowing(jwtConsumerConfigServiceRef).getProperty("service.ranking");
                will(returnValue(1L));
                allowing(cc).locateService("jwtConsumerConfig", jwtConsumerConfigServiceRef);
                will(returnValue(jwtConsumerConfig));
                allowing(jwtConsumerConfig).getConsumerUtils();
                will(returnValue(consumerUtil));
                allowing(jwtConsumerConfig).getId();
                will(returnValue(idTokenConsumer));
                allowing(jwtConsumerConfig).getClockSkew();
                will(returnValue(300000L));
                allowing(jwtConsumerConfig).isValidationRequired();
                will(returnValue(false));
            }
        });
        consumerImpl.setJwtConsumerConfig(jwtConsumerConfigServiceRef);
        consumerImpl.activate(cc);

        final ServiceReference consumerServiceRef = mockery.mock(ServiceReference.class, "consumerServiceRef");
        mockery.checking(new Expectations() {
            {
                allowing(cc).locateService("consumer", consumerServiceRef);
                will(returnValue(consumerImpl));
            }
        });

        return consumerServiceRef;
    }

}
