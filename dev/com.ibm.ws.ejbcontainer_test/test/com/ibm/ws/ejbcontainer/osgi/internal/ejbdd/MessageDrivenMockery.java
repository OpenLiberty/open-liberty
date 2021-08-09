/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal.ejbdd;

import java.util.ArrayList;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;

import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.ejb.ActivationConfig;
import com.ibm.ws.javaee.dd.ejb.ActivationConfigProperty;
import com.ibm.ws.javaee.dd.ejb.EnterpriseBean;
import com.ibm.ws.javaee.dd.ejb.MessageDriven;
import com.ibm.ws.javaee.dd.ejb.NamedMethod;
import com.ibm.ws.javaee.dd.ejb.Timer;
import com.ibm.ws.javaee.dd.ejb.TransactionalBean;

public class MessageDrivenMockery extends EnterpriseBeanMockery<MessageDrivenMockery> {
    private String messagingType;
    private List<ActivationConfigProperty> activationConfigProperties;
    private int transactionType = TransactionalBean.TRANSACTION_TYPE_UNSPECIFIED;
    private NamedMethod timeoutMethod;
    private final List<Timer> timers = new ArrayList<Timer>();

    MessageDrivenMockery(Mockery mockery, String name) {
        super(mockery, name, EnterpriseBean.KIND_MESSAGE_DRIVEN);
    }

    public MessageDrivenMockery messagingType(String messagingType) {
        this.messagingType = messagingType;
        return this;
    }

    public MessageDrivenMockery transactionType(int type) {
        this.transactionType = type;
        return this;
    }

    public MessageDrivenMockery activationConfigProperty(final String name, final String value) {
        if (activationConfigProperties == null) {
            activationConfigProperties = new ArrayList<ActivationConfigProperty>();
        }
        activationConfigProperties.add(new ActivationConfigProperty() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getValue() {
                return value;
            }
        });
        return this;
    }

    public MessageDrivenMockery timeoutMethod(String name, String... params) {
        this.timeoutMethod = new NamedMethodImpl(name, params);
        return this;
    }

    public MessageDrivenMockery timer() {
        this.timers.add(null);
        return this;
    }

    public MessageDriven mock() {
        final MessageDriven bean = mockEnterpriseBean(MessageDriven.class);
        mockery.checking(new Expectations() {
            {
                allowing(bean).getMessagingTypeName();
                will(returnValue(messagingType));

                allowing(bean).getActivationConfigValue();
                if (activationConfigProperties == null) {
                    will(returnValue(null));
                } else {
                    will(returnValue(new ActivationConfig() {
                        @Override
                        public List<Description> getDescriptions() {
                            throw new UnsupportedOperationException();
                        }

                        @Override
                        public List<ActivationConfigProperty> getConfigProperties() {
                            return activationConfigProperties;
                        }
                    }));
                }

                allowing(bean).getTransactionTypeValue();
                will(returnValue(transactionType));

                allowing(bean).getTimeoutMethod();
                will(returnValue(timeoutMethod));

                allowing(bean).getTimers();
                will(returnValue(timers));
            }
        });
        return bean;
    }
}
