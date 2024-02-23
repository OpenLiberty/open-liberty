/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.context.custom.invalid;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;

/**
 * A bean which has an incoming channel, used to test with invalid incoming channel config
 */
@ApplicationScoped
public class KafkaInvalidContextBean {

    public static final String CHANNEL_NAME = "invalid-context-bean";

    @Incoming(CHANNEL_NAME)
    public void incoming(String data) {
    }

}
