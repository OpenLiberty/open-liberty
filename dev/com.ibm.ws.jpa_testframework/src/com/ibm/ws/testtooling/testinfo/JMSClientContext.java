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

public class JMSClientContext extends MessagingClientContext {
    private static final long serialVersionUID = 3193344939303046065L;

    private JMSClientConfig jmsClientCfg;

    public JMSClientContext(String name, JMSClientConfig jmsClientCfg) {
        super(name, MessagingClientType.JMSClient);
        this.jmsClientCfg = jmsClientCfg;
    }

    public JMSClientConfig getJmsClientCfg() {
        return jmsClientCfg;
    }

    @Override
    public MessagingClientContext clone() throws CloneNotSupportedException {
        JMSClientContext clone = (JMSClientContext) super.clone();
        clone.jmsClientCfg = this.jmsClientCfg;

        return clone;
    }

    @Override
    public String toString() {
        return "JMSClientContext [jmsClientCfg=" + jmsClientCfg
               + ", getName()=" + getName() + ", getMessagingClientType()="
               + getMessagingClientType() + "]";
    }

}