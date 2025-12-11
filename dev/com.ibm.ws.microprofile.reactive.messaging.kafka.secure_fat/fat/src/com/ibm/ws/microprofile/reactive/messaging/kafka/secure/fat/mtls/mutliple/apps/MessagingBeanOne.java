/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.mtls.mutliple.apps;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MessagingBeanOne {

    public static final String CHANNEL_IN = "test-bean1-in";
    public static final String CHANNEL_OUT = "test-bean1-out";

    @Incoming(CHANNEL_IN)
    @Outgoing(CHANNEL_OUT)
    public String reverseString(String in) {
        StringBuilder sb = new StringBuilder(in);
        System.out.println("MessagingBeanOne Processing message " + in);
        sb.reverse();
        return sb.toString();
    }

}
