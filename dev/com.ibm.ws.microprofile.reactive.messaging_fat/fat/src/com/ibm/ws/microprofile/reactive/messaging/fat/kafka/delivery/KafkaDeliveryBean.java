/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.delivery;

import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractDeliveryBean;

@ApplicationScoped
public class KafkaDeliveryBean extends AbstractDeliveryBean<String> {

    public final static String CHANNEL_NAME = "delivery-test-output";

    // Overridden to add @Outgoing
    @Override
    @Outgoing(CHANNEL_NAME)
    public CompletionStage<Message<String>> getMessage() {
        return super.getMessage();
    }

}
