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
package com.ibm.ws.testtooling.msgcli.jms;

import java.io.Serializable;

public class JMSClientConfig implements Serializable {
    private static final long serialVersionUID = 5437041853848147854L;

    public enum JMSType {
        QUEUE,
        TOPIC;
    }

    private JMSType jmsType;
    private String connectionFactoryName;
    private String receiverName;
    private String senderName;

    private String username;
    private String password;

    public JMSClientConfig() {
        super();
    }

    public JMSClientConfig(JMSType jmsType, String connectionFactoryName,
                           String receiverName, String senderName, String username,
                           String password) {
        super();
        this.jmsType = jmsType;
        this.connectionFactoryName = connectionFactoryName;
        this.receiverName = receiverName;
        this.senderName = senderName;
        this.username = username;
        this.password = password;
    }

    public JMSType getJmsType() {
        return jmsType;
    }

    public void setJmsType(JMSType jmsType) {
        this.jmsType = jmsType;
    }

    public String getConnectionFactoryName() {
        return connectionFactoryName;
    }

    public void setConnectionFactoryName(String connectionFactoryName) {
        this.connectionFactoryName = connectionFactoryName;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "JMSClientConfig [jmsType=" + jmsType
               + ", connectionFactoryName=" + connectionFactoryName
               + ", receiverName=" + receiverName + ", senderName="
               + senderName + ", username=" + username + ", password="
               + "xxxxx ]";
    }
}