/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
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
package io.openliberty.microprofile.reactive.messaging.fat.kafka.validation;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

@ApplicationScoped
public class OutgoingConnectorMultipleUpstreams {

    public final static String OUTGOING_CHANNEL = "OutgoingChannel";

    @Inject
    @Channel(OUTGOING_CHANNEL)
    Emitter<String> emitter1;

    @Outgoing(OUTGOING_CHANNEL)
    public String test(){
        return "test";
    }
}