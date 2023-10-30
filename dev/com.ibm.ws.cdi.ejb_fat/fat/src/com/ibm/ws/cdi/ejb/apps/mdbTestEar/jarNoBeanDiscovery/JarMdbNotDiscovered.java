/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.mdbTestEar.jarNoBeanDiscovery;

import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import com.ibm.ws.cdi.ejb.apps.mdbTestEar.lib.EarTestMessageHolder;

@MessageDriven
public class JarMdbNotDiscovered implements MessageListener {

    @Inject
    private EarTestMessageHolder messageHolder;

    /** {@inheritDoc} */
    @Override
    public void onMessage(Message message) {

        System.out.println("JarMdbNotDiscovered Message received: " + message);

        try {
            String body = message.getBody(String.class);
            messageHolder.addMessage(body);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }

    }

}
