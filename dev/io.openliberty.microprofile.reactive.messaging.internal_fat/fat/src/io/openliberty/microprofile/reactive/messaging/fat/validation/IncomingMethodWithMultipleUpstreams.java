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
package io.openliberty.microprofile.reactive.messaging.fat.validation;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class IncomingMethodWithMultipleUpstreams {

    public final static String CHANNEL = "IncomingMethodWithMultipleUpstreams";

    @Inject
    @Channel(CHANNEL)
    Emitter<String> emitter1;


    @Outgoing(CHANNEL)
    public String outgoing(){
        return "hello";
    }

    @Incoming(CHANNEL)
    public void badMethod(String message) {
        // do nothing
    }

}