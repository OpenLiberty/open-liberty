/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.testtooling.testinfo;

import com.ibm.ws.testtooling.msgcli.jms.JMSClientConfig;

public class StatefulMessengerClientContext extends MessagingClientContext {
    private static final long serialVersionUID = 1716114584482714320L;

    private String beanName; // JNDI name of the StatefulMessengerClient SFSB
    private boolean fullDuplexMode; // If true, then transmitterClient = receiverClient
    private JMSClientConfig transmitterClient = null;
    private JMSClientConfig receiverClient = null;

    public StatefulMessengerClientContext(String name, String beanName, JMSClientConfig fullDuplexConfig) {
        super(name, MessagingClientType.StatefulMessengerClient);

        this.beanName = beanName;
        this.fullDuplexMode = true;
        this.transmitterClient = fullDuplexConfig;
        this.receiverClient = fullDuplexConfig;
    }

    public StatefulMessengerClientContext(String name, String beanName, JMSClientConfig receiver, JMSClientConfig sender) {
        super(name, MessagingClientType.StatefulMessengerClient);

        this.beanName = beanName;
        this.fullDuplexMode = false;
        this.transmitterClient = sender;
        this.receiverClient = receiver;
    }

    public String getBeanName() {
        return beanName;
    }

    public boolean isFullDuplexMode() {
        return fullDuplexMode;
    }

    public JMSClientConfig getTransmitterClient() {
        return transmitterClient;
    }

    public JMSClientConfig getReceiverClient() {
        return receiverClient;
    }

    @Override
    public MessagingClientContext clone() throws CloneNotSupportedException {
        StatefulMessengerClientContext clone = (StatefulMessengerClientContext) super.clone();

        clone.beanName = this.beanName;
        clone.fullDuplexMode = this.fullDuplexMode;
        clone.transmitterClient = this.transmitterClient;
        clone.receiverClient = this.receiverClient;

        return clone;
    }

    @Override
    public String toString() {
        return "StatefulMessengerClientContext [beanName=" + beanName
               + ", fullDuplexMode=" + fullDuplexMode + ", transmitterClient="
               + transmitterClient + ", receiverClient=" + receiverClient
               + ", getName()=" + getName() + ", getMessagingClientType()="
               + getMessagingClientType() + "]";
    }
}