/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.messaging.fat.kafka.metrics;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractDeliveryBean;

import jakarta.enterprise.context.ApplicationScoped;

/**
 *
 */
@ApplicationScoped
public class MessagingBeanOne extends AbstractDeliveryBean<String> {

    public static final String CHANNEL_IN = "consumed-channel";
    public static final String CHANNEL_OUT = "populated-channel";

    @Incoming(CHANNEL_IN)
    @Outgoing(CHANNEL_OUT)
    public String upperCaseString(String in) {
        return in.toUpperCase();
    }

}
