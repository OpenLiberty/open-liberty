/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.context;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

/**
 * Test bean that appends the app name to a string message
 */
@ApplicationScoped
public class KafkaDefaultContextTestMessageBean {

    public static final String INPUT_CHANNEL = "context-test-in";
    public static final String OUTPUT_CHANNEL = "context-test-out";

    @Resource(lookup = "java:app/AppName")
    private String appName;

    @Incoming(INPUT_CHANNEL)
    @Outgoing(OUTPUT_CHANNEL)
    public String addAppName(String input) {
        return input + "-" + appName;
    }
}
