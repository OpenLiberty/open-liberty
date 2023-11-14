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

import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractReceptionBean;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RmTelemetryReceptionBean extends AbstractReceptionBean<String> {

    public static final String CHANNEL_IN = RmTelemetryProcessingBean.CHANNEL_OUT;

    @Incoming(CHANNEL_IN)
    @Override
    public CompletionStage<Void> receiveMessage(Message<String> msg) {
        return super.receiveMessage(msg);
    }

}
