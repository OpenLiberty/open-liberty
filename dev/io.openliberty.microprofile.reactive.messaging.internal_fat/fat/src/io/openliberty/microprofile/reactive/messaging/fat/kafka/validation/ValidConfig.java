/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
import org.eclipse.microprofile.reactive.messaging.Incoming;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ValidConfig {

    public final static String INCOMING_CHANNEL = "ValidIncomingChannel";
    public final static String OUTGOING_CHANNEL = "ValidOutgoingChannel";

    @Inject
    @Channel(OUTGOING_CHANNEL)
    Emitter<String> emitter;

    @Incoming(INCOMING_CHANNEL)
    public void testMethod(String message) {
        // do nothing
    }
}