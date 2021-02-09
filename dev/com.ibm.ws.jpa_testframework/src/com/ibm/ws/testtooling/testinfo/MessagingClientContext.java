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

public abstract class MessagingClientContext implements java.io.Serializable, Cloneable {
    private static final long serialVersionUID = -2625905278102737773L;

    public enum MessagingClientType {
        JMSClient, // Raw JMS client
        StatefulMessengerClient // JMS client fronted by a Stateful Session Bean
    }

    private String name;
    private MessagingClientType clientType;

    protected MessagingClientContext(String name, MessagingClientType clientType) {
        this.name = name;
        this.clientType = clientType;
    }

    public final String getName() {
        return name;
    }

    public final MessagingClientType getMessagingClientType() {
        return clientType;
    }

    @Override
    public MessagingClientContext clone() throws CloneNotSupportedException {
        MessagingClientContext clone = (MessagingClientContext) super.clone();
        clone.name = this.name;
        clone.clientType = this.clientType;

        return clone;
    }
}
