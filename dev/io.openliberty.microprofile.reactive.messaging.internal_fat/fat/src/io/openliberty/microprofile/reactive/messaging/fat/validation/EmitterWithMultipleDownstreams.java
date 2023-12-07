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
package io.openliberty.microprofile.reactive.messaging.fat.validation;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class EmitterWithMultipleDownstreams {

    public final static String CHANNEL = "EmitterWithMultipleDownstreams";

    @Inject
    @Channel(CHANNEL)
    Emitter<String> emitter1;

    @Incoming(CHANNEL)
    public void badMethod(String message) {
        // do nothing
    }

    @Incoming(CHANNEL)
    public void anotherBadMethod(String message) {
        // do nothing
    }
}