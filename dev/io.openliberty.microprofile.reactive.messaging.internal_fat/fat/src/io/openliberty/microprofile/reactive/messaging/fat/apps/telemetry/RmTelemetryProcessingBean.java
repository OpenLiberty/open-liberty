/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.messaging.fat.apps.telemetry;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RmTelemetryProcessingBean {

    public static final String CHANNEL_IN = "consumed-channel";
    public static final String CHANNEL_OUT = "populated-channel";

    @Incoming(CHANNEL_IN)
    @Outgoing(CHANNEL_OUT)
    public Message<String> process(Message<String> in) {
        String payload = in.getPayload().toUpperCase();
        return in.withPayload(payload);
    }

}
