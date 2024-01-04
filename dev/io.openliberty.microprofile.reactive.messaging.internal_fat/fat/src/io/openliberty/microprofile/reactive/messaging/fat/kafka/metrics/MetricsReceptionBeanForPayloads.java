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

import static org.eclipse.microprofile.reactive.messaging.Acknowledgment.Strategy.MANUAL;

import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractReceptionBean;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MetricsReceptionBeanForPayloads extends AbstractReceptionBean<String> {

    public static final String CHANNEL_IN = "metrics-incoming";

    @Incoming(CHANNEL_IN)
    @Acknowledgment(MANUAL)
    public CompletionStage<Void> emitterMessage(Message<String> msg) {
        System.out.println(msg.getPayload());
        return super.receiveMessage(msg);
    }

}
