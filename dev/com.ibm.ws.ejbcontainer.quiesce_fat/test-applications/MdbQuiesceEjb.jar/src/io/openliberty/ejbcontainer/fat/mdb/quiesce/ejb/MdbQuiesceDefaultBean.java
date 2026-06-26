/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.ejbcontainer.fat.mdb.quiesce.ejb;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;

@MessageDriven(name = "MdbQuiesceDefault",
               activationConfig = {
                   @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue")
               })
public class MdbQuiesceDefaultBean implements MessageListener {

    @PostConstruct
    public void postConstruct() {
        System.out.println("PostConstruct:MdbQuiesceApp:MdbQuiesceEjb:MdbQuiesceDefault:");
    }

    @PreDestroy
    public void preDestroy() {
        System.out.println("PreDestroy:MdbQuiesceApp:MdbQuiesceEjb:MdbQuiesceDefault:");
    }

    @Override
    public void onMessage(Message message) {
        // Simple message listener - no processing needed for this test
        System.out.println("MdbQuiesceDefault received message: " + message);
    }
}
