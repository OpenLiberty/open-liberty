/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.liberty_login;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

@ApplicationScoped
public class AesTestBean {

    public static final String CHANNEL_IN = "aes-test-in";
    public static final String CHANNEL_OUT = "aes-test-out";

    @Incoming(CHANNEL_IN)
    @Outgoing(CHANNEL_OUT)
    public String processMessage(String input) {
        return "aes-" + input;
    }

}
